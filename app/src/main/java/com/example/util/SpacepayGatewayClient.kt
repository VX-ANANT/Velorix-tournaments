package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Spacepay (spacepay.in) Automated UPI Gateway Integration Client.
 * Handles server-side/app-level transaction creation and status inquiry.
 */
object SpacepayGatewayClient {

    private const val TAG = "SpacepayGateway"
    private const val BASE_URL = "https://spacepay.in/api/payment/v1"

    // Configured live credentials provided for VeloRix Esports
    val PUBLIC_KEY: String
        get() = try {
            val key = com.example.BuildConfig.SPACEPAY_PUBLIC_KEY
            if (key.isNotBlank() && key != "DEFAULT_SPACEPAY_PUBLIC_KEY") key else "pk_36e628d10fa848ab67cfb003e2b08fddd4a03210b5cf2b8633537ab41ab5decb"
        } catch (e: Throwable) {
            "pk_36e628d10fa848ab67cfb003e2b08fddd4a03210b5cf2b8633537ab41ab5decb"
        }

    val SECRET_KEY: String
        get() = try {
            val sec = com.example.BuildConfig.SPACEPAY_SECRET_KEY
            if (sec.isNotBlank() && sec != "DEFAULT_SPACEPAY_SECRET_KEY") sec else "c1ce7bfaa54a8ade10e1ba15cf83451bea2be225011fc3e0000a9701c389f7b2"
        } catch (e: Throwable) {
            "c1ce7bfaa54a8ade10e1ba15cf83451bea2be225011fc3e0000a9701c389f7b2"
        }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    sealed class CreateOrderResult {
        data class Success(
            val orderId: String,
            val paymentUrl: String,
            val rawResponse: String
        ) : CreateOrderResult()

        data class Error(val errorMessage: String) : CreateOrderResult()
    }

    sealed class OrderStatusResult {
        data class Success(
            val orderId: String,
            val status: String, // e.g. "SUCCESS", "PENDING", "FAILED"
            val bankTxnId: String?,
            val amount: String?,
            val customerMobile: String?
        ) : OrderStatusResult()

        data class Error(val errorMessage: String) : OrderStatusResult()
    }

    /**
     * Initiates a payment order on Spacepay.
     * POST https://spacepay.in/api/payment/v1/pay
     */
    suspend fun createPaymentOrder(
        amount: Double,
        orderId: String,
        customerMobile: String = "9876543210",
        redirectUrl: String = "https://velorix.esports/payment-success",
        note: String = "VeloRix Token Topup"
    ): CreateOrderResult = withContext(Dispatchers.IO) {
        try {
            val amountFormatted = if (amount % 1.0 == 0.0) amount.toInt().toString() else "%.2f".format(amount)
            val jsonPayload = JSONObject().apply {
                put("public_key", PUBLIC_KEY)
                put("secret_key", SECRET_KEY)
                put("customer_mobile", customerMobile.ifBlank { "9876543210" })
                put("amount", amountFormatted)
                put("order_id", orderId)
                put("redirect_url", redirectUrl)
                put("note", note)
            }

            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$BASE_URL/pay")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            Log.d(TAG, "createPaymentOrder response code=${response.code}, body=$responseBody")

            if (!response.isSuccessful) {
                return@withContext CreateOrderResult.Error("HTTP ${response.code}: $responseBody")
            }

            val jsonResp = JSONObject(responseBody)
            val status = jsonResp.optBoolean("status", false)
            if (status) {
                val resultObj = jsonResp.optJSONObject("result")
                val returnedOrderId = resultObj?.optString("orderId") ?: orderId
                val paymentUrl = resultObj?.optString("payment_url") ?: ""
                CreateOrderResult.Success(
                    orderId = returnedOrderId,
                    paymentUrl = paymentUrl,
                    rawResponse = responseBody
                )
            } else {
                val msg = jsonResp.optString("message", "Failed to create payment order on Spacepay")
                CreateOrderResult.Error(msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createPaymentOrder", e)
            CreateOrderResult.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    /**
     * Checks the transaction status of an order on Spacepay.
     * POST https://spacepay.in/api/payment/v1/order-status
     */
    suspend fun checkOrderStatus(orderId: String): OrderStatusResult = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("public_key", PUBLIC_KEY)
                put("secret_key", SECRET_KEY)
                put("order_id", orderId)
            }

            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$BASE_URL/order-status")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            Log.d(TAG, "checkOrderStatus response code=${response.code}, body=$responseBody")

            if (!response.isSuccessful) {
                return@withContext OrderStatusResult.Error("HTTP ${response.code}: $responseBody")
            }

            val jsonResp = JSONObject(responseBody)
            val statusBool = jsonResp.optBoolean("status", false)
            if (statusBool) {
                val details = jsonResp.optJSONObject("order_details")
                val statusStr = details?.optString("STATUS") ?: "PENDING"
                val bankTxnId = details?.optString("BANKTXNID")?.takeIf { it != "null" && it.isNotBlank() }
                val amount = details?.optString("AMOUNT")
                val mobile = details?.optString("CUSTOMER_MOBILE")

                OrderStatusResult.Success(
                    orderId = orderId,
                    status = statusStr,
                    bankTxnId = bankTxnId,
                    amount = amount,
                    customerMobile = mobile
                )
            } else {
                val msg = jsonResp.optString("message", "Order not found or pending")
                OrderStatusResult.Error(msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkOrderStatus", e)
            OrderStatusResult.Error(e.localizedMessage ?: "Failed to query order status")
        }
    }
}
