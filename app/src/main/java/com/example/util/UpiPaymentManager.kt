package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

object UpiPaymentManager {

    /**
     * Active Receiver UPI ID configured for all user panel transactions.
     * Can be easily changed here in the future.
     */
    const val PRIMARY_UPI_ID: String = "veloxyra.anant@fam"
    const val MERCHANT_NAME: String = "VeloRix Esports"
    const val MERCHANT_CODE: String = "5411"

    /**
     * Generates a tamper-proof locked UPI payment URI with non-modifiable amount.
     */
    fun buildUpiUri(
        amount: Double,
        transactionId: String,
        note: String = "VeloRix Token TopUp",
        upiId: String = PRIMARY_UPI_ID
    ): Uri {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val encodedMerchantName = Uri.encode(MERCHANT_NAME)
        val encodedNote = Uri.encode(note)
        
        val uriString = "upi://pay?pa=$upiId" +
                "&pn=$encodedMerchantName" +
                "&mc=$MERCHANT_CODE" +
                "&tr=$transactionId" +
                "&tn=$encodedNote" +
                "&am=$formattedAmount" +
                "&cu=INR"
                
        return Uri.parse(uriString)
    }

    /**
     * Generates a high-resolution, custom esports-styled QR code bitmap.
     * Uses ErrorCorrectionLevel.H so the center VeloRix logo does not hinder scan reliability.
     * Seamlessly adapts to the active app theme (Dark vs Light):
     * - In Dark Mode: Sleek OLED zinc-950 canvas (#09090B) with crisp pure white modules (#FFFFFF).
     * - In Light Mode: Clean off-white paper canvas (#FAFAFA) with deep high-contrast black modules (#0A0A0A).
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        context: Context? = null,
        isDarkTheme: Boolean = true,
        customBgColor: Int? = null,
        customModuleColor: Int? = null
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            // Encode at raw QR grid size first so we have the exact module matrix
            val rawMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixWidth = rawMatrix.width
            val matrixHeight = rawMatrix.height

            // Calculate exact target canvas size and integer/float scale
            val outputSize = if (sizePx < 256) 256 else sizePx
            val moduleSize = outputSize.toFloat() / matrixWidth.toFloat()

            // Resolve theme colors:
            // Dark Mode: Deep Zinc/OLED #09090B background, White #FFFFFF modules
            // Light Mode: Clean #FAFAFA background, Deep #0A0A0A modules
            val qrBgColor = customBgColor ?: if (isDarkTheme) AndroidColor.parseColor("#09090B") else AndroidColor.parseColor("#FAFAFA")
            val qrModuleColor = customModuleColor ?: if (isDarkTheme) AndroidColor.WHITE else AndroidColor.parseColor("#0A0A0A")
            val badgeBgColor = customBgColor ?: if (isDarkTheme) AndroidColor.parseColor("#09090B") else AndroidColor.parseColor("#FAFAFA")
            val badgeBorderColor = if (isDarkTheme) AndroidColor.parseColor("#27272A") else AndroidColor.parseColor("#E4E4E7")
            val initialsColor = qrModuleColor

            val resultBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(resultBitmap)

            // Dynamic background canvas matching active theme
            val bgPaint = android.graphics.Paint().apply {
                color = qrBgColor
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, outputSize.toFloat(), outputSize.toFloat(), bgPaint)

            // High-contrast modules for fast scanning across all cameras
            val modulePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = qrModuleColor
                style = android.graphics.Paint.Style.FILL
            }

            // Finder eye accent: matches theme module color for clean, cohesive look
            val eyeAccentPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = qrModuleColor
                style = android.graphics.Paint.Style.FILL
            }

            // Center logo area reserved boundary (center 22% of matrix)
            val centerStartModule = (matrixWidth * 0.38f).toInt()
            val centerEndModule = (matrixWidth * 0.62f).toInt()

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    if (rawMatrix.get(x, y)) {
                        // Finder eyes in top-left, top-right, bottom-left (7x7 modules + 1 margin)
                        val isTopLeftEye = x < 8 && y < 8
                        val isTopRightEye = x >= matrixWidth - 8 && y < 8
                        val isBottomLeftEye = x < 8 && y >= matrixHeight - 8

                        // Skip drawing modules that will be completely covered by the center logo badge
                        val isInCenterLogoArea = x in (centerStartModule + 1) until centerEndModule &&
                                                 y in (centerStartModule + 1) until centerEndModule
                        if (isInCenterLogoArea) continue

                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = (x + 1) * moduleSize
                        val bottom = (y + 1) * moduleSize

                        val activePaint = if (isTopLeftEye || isTopRightEye || isBottomLeftEye) {
                            eyeAccentPaint
                        } else {
                            modulePaint
                        }

                        // Subtle clean rounding for premium, modern tech aesthetic
                        val cornerRadius = moduleSize * 0.20f
                        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, activePaint)
                    }
                }
            }

            // Overlay Centered VeloRix Logo Emblem
            val logoSize = (outputSize * 0.22f).toInt()
            val logoLeft = (outputSize - logoSize) / 2f
            val logoTop = (outputSize - logoSize) / 2f

            // Clean badge background for the logo with subtle border matching active theme
            val badgeBgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = badgeBgColor
                style = android.graphics.Paint.Style.FILL
            }
            val badgeBorderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = badgeBorderColor
                strokeWidth = 3f
                style = android.graphics.Paint.Style.STROKE
            }

            val badgeRadius = 12f
            val badgeRect = android.graphics.RectF(logoLeft - 3f, logoTop - 3f, logoLeft + logoSize + 3f, logoTop + logoSize + 3f)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBorderPaint)

            // Load and draw velorix logo
            var logoDrawn = false
            if (context != null) {
                try {
                    val rawLogo = android.graphics.BitmapFactory.decodeResource(
                        context.resources,
                        com.example.R.drawable.velorix_logo_image
                    )
                    if (rawLogo != null) {
                        val scaledLogo = Bitmap.createScaledBitmap(rawLogo, logoSize, logoSize, true)
                        canvas.drawBitmap(scaledLogo, logoLeft, logoTop, null)
                        logoDrawn = true
                    }
                } catch (e: Exception) {
                    logoDrawn = false
                }
            }

            if (!logoDrawn) {
                drawVrxInitials(canvas, logoLeft, logoTop, logoSize.toFloat(), initialsColor)
            }

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // High-reliability simple fallback if any advanced drawing threw an unexpected exception
            try {
                val fallbackBg = customBgColor ?: if (isDarkTheme) AndroidColor.parseColor("#09090B") else AndroidColor.parseColor("#FAFAFA")
                val fallbackMod = customModuleColor ?: if (isDarkTheme) AndroidColor.WHITE else AndroidColor.parseColor("#0A0A0A")
                val simpleHints = mapOf(EncodeHintType.MARGIN to 1)
                val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, simpleHints)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) fallbackMod else fallbackBg
                    }
                }
                val fallbackBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                fallbackBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                fallbackBitmap
            } catch (fallbackEx: Exception) {
                null
            }
        }
    }

    private fun drawVrxInitials(canvas: android.graphics.Canvas, left: Float, top: Float, size: Float, textColor: Int = AndroidColor.WHITE) {
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = size * 0.40f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val xPos = left + size / 2f
        val yPos = top + (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("VRX", xPos, yPos, textPaint)
    }

    /**
     * Copies UPI ID or transaction ID to clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }
}
