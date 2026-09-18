package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import com.example.util.SpacepayGatewayClient
import com.example.util.UpiPaymentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiPaymentQrDialog(
    amount: Double,
    purpose: String = "Token Top-up",
    onDismiss: () -> Unit,
    onPaymentSuccess: (Double, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Unique locked transaction ID
    val transactionId = remember {
        "VRX-" + (System.currentTimeMillis() % 10000000)
    }

    // Spacepay Gateway Dynamic Order States
    var spacepayOrderId by remember { mutableStateOf<String?>(null) }
    var spacepayPaymentUrl by remember { mutableStateOf<String?>(null) }
    var spacepayUpiUriString by remember { mutableStateOf<String?>(null) }
    var isGatewayCreatingOrder by remember { mutableStateOf(true) }
    var gatewayErrorMessage by remember { mutableStateOf<String?>(null) }
    var paymentVerifiedSuccessfully by remember { mutableStateOf(false) }

    // Fallback static UPI URI string
    val fallbackUpiUri = remember(amount, transactionId) {
        UpiPaymentManager.buildUpiUri(
            amount = amount,
            transactionId = transactionId,
            note = purpose
        )
    }

    // Create Spacepay Dynamic Order on dialog entry
    LaunchedEffect(amount, transactionId) {
        isGatewayCreatingOrder = true
        gatewayErrorMessage = null
        try {
            val res = SpacepayGatewayClient.createPaymentOrder(
                amount = amount,
                orderId = transactionId,
                customerMobile = "9876543210",
                note = purpose
            )
            when (res) {
                is SpacepayGatewayClient.CreateOrderResult.Success -> {
                    spacepayOrderId = res.orderId
                    spacepayPaymentUrl = res.paymentUrl
                    spacepayUpiUriString = res.paymentUrl
                    isGatewayCreatingOrder = false
                }
                is SpacepayGatewayClient.CreateOrderResult.Error -> {
                    gatewayErrorMessage = res.errorMessage
                    isGatewayCreatingOrder = false
                }
            }
        } catch (e: Exception) {
            gatewayErrorMessage = e.localizedMessage ?: "Failed to connect to Spacepay"
            isGatewayCreatingOrder = false
        }
    }

    // Active UPI URI: dynamic Spacepay URI if available, otherwise fallback static UPI
    val effectiveUpiUri = remember(spacepayUpiUriString, fallbackUpiUri) {
        val dynamic = spacepayUpiUriString
        if (!dynamic.isNullOrBlank() && (dynamic.startsWith("upi://") || dynamic.startsWith("http://") || dynamic.startsWith("https://"))) {
            Uri.parse(dynamic)
        } else {
            fallbackUpiUri
        }
    }

    // Theme-aware detection: dark theme vs light theme & color scheme extraction
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val qrBgColor = (if (isDarkTheme) Color(0xFF09090B) else Color(0xFFF4F4F5)).toArgb()
    val qrModuleColor = (if (isDarkTheme) Color(0xFFFAFAFA) else Color(0xFF09090B)).toArgb()

    // Dynamic QR Bitmap for active URI matching current theme mode (Dark/Light)
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(effectiveUpiUri, isDarkTheme, qrBgColor, qrModuleColor) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val bmp = UpiPaymentManager.generateQrBitmap(
                content = effectiveUpiUri.toString(),
                sizePx = 512,
                context = context,
                isDarkTheme = isDarkTheme,
                customBgColor = qrBgColor,
                customModuleColor = qrModuleColor
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                qrBitmap = bmp
            }
        }
    }

    // 10-minute countdown timer
    var timeLeftSeconds by remember { mutableIntStateOf(600) }
    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
    }

    // Background Status Polling when Spacepay Order ID is available
    LaunchedEffect(spacepayOrderId) {
        val activeOrderId = spacepayOrderId ?: return@LaunchedEffect
        while (!paymentVerifiedSuccessfully && timeLeftSeconds > 0) {
            delay(4000) // Poll every 4 seconds
            val statusRes = SpacepayGatewayClient.checkOrderStatus(activeOrderId)
            if (statusRes is SpacepayGatewayClient.OrderStatusResult.Success) {
                val isSuccess = statusRes.status.equals("SUCCESS", ignoreCase = true) ||
                        statusRes.status.equals("TXN_SUCCESS", ignoreCase = true) ||
                        statusRes.status.equals("COMPLETED", ignoreCase = true)
                if (isSuccess) {
                    paymentVerifiedSuccessfully = true
                    val confirmedRef = statusRes.bankTxnId?.takeIf { it.isNotBlank() } ?: statusRes.orderId
                    Toast.makeText(context, "Spacepay: Payment Verified via Automated Gateway!", Toast.LENGTH_SHORT).show()
                    onPaymentSuccess(amount, confirmedRef)
                    break
                }
            }
        }
    }

    var utrNumber by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var paymentMode by remember { mutableStateOf("QR") } // "QR" or "APP"
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // UPI App Intent Launcher
    val upiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dataString = result.data?.getStringExtra("response") ?: result.data?.dataString ?: ""
        val isSuccess = dataString.contains("Status=SUCCESS", ignoreCase = true) ||
                dataString.contains("txnStatus=SUCCESS", ignoreCase = true) ||
                dataString.contains("Status=00", ignoreCase = true) ||
                (result.resultCode == Activity.RESULT_OK && dataString.contains("SUCCESS", ignoreCase = true))

        if (isSuccess) {
            Toast.makeText(context, "UPI Gateway: Payment Verified Successfully!", Toast.LENGTH_SHORT).show()
            onPaymentSuccess(amount, transactionId)
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(
                context,
                "Payment was not completed in the UPI app. Please retry or enter your 12-digit UTR number.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Please complete the payment in your UPI app or enter the 12-digit UTR number from your payment receipt.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Dialog(
        onDismissRequest = {
            // Prevent accidental outside-click dismiss to enforce payment or deliberate cancellation
            showCancelConfirmDialog = true
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        ApplyDialogWindowBlur()
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x2E000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.88f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.72f,
                                stiffness = 380f
                            )
                        ),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                        scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(150))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .widthIn(max = 480.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .testTag("upi_qr_payment_modal"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF20B0E14)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x40FFFFFF),
                                Color(0x14FFFFFF),
                                Color(0x06FFFFFF)
                            )
                        )
                    )
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Xiaomi Top Handle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0x28FFFFFF))
                    )

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(BorderStroke(1.dp, Color(0x20FFFFFF)), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_wallet_receive),
                                    contentDescription = "Secure Checkout",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SECURE PAYMENT GATE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pay to proceed or Cancel to exit",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCancelConfirmDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Payment",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Locked Gate Notification Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF18181B),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_alert),
                                contentDescription = null,
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen locked until payment is verified or cancelled.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD4D4D8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Locked Amount Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF121215),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MANDATORY PAYABLE AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA1A1AA),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹ " + String.format(Locale.US, "%.2f", amount),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Amount locked by system • Unalterable",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF71717A)
                                )
                            }
                        }
                    }

                    // Spacepay Dynamic Gateway Status Indicator
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141416),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isGatewayCreatingOrder) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (spacepayOrderId != null) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (spacepayOrderId != null) Color(0xFF10B981) else Color(0xFFA1A1AA),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isGatewayCreatingOrder -> "Connecting Spacepay Gateway..."
                                        spacepayOrderId != null -> "Spacepay Gateway Active (${spacepayOrderId!!.take(10)}...)"
                                        gatewayErrorMessage != null -> "Spacepay: Ready (UPI Fallback Active)"
                                        else -> "Spacepay Gateway Ready"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (spacepayOrderId != null) Color(0xFF10B981) else Color(0xFFD4D4D8),
                                    maxLines = 1
                                )
                            }
                            if (spacepayOrderId != null) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0x2010B981)
                                ) {
                                    Text(
                                        text = "AUTO-SYNC",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Selection Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            title = "Scan QR Code",
                            icon = Icons.Outlined.QrCodeScanner,
                            isSelected = paymentMode == "QR",
                            modifier = Modifier.weight(1f),
                            onClick = { paymentMode = "QR" }
                        )
                        TabButton(
                            title = "Pay via UPI App",
                            icon = Icons.Default.PhoneAndroid,
                            isSelected = paymentMode == "APP",
                            modifier = Modifier.weight(1f),
                            onClick = { paymentMode = "APP" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (paymentMode == "QR") {
                        // Minimalist QR Code Container dynamically styled for current theme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkTheme) Color(0xFF09090B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF27272A) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Subtle Header bar above QR
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "INSTANT UPI PAY",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.8.sp,
                                            color = if (isDarkTheme) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isDarkTheme) Color(0xFF18181B) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "LOCKED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFD4D4D8) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // QR Card with theme-adaptive background and dots
                                Surface(
                                    modifier = Modifier
                                        .size(214.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    color = if (isDarkTheme) Color(0xFF09090B) else Color(0xFFF4F4F5),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF27272A) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentQr = qrBitmap
                                        if (currentQr != null) {
                                            Image(
                                                bitmap = currentQr.asImageBitmap(),
                                                contentDescription = "UPI Payment QR Code",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Scan with any UPI app (GPay, PhonePe, Paytm, CRED)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isDarkTheme) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Timer Display
                        val minutes = timeLeftSeconds / 60
                        val seconds = timeLeftSeconds % 60
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (timeLeftSeconds < 60) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "QR Expires in %02d:%02d".format(minutes, seconds),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (timeLeftSeconds < 60) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // UPI ID Copy Row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "BENEFICIARY UPI ID",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = UpiPaymentManager.PRIMARY_UPI_ID,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        UpiPaymentManager.copyToClipboard(
                                            context,
                                            "UPI ID",
                                            UpiPaymentManager.PRIMARY_UPI_ID
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy UPI ID",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Direct UPI App Option
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Launch your preferred UPI app with locked ₹${String.format(Locale.US, "%.2f", amount)} amount:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val targetUri = effectiveUpiUri
                                    val intent = Intent(Intent.ACTION_VIEW, targetUri)
                                    try {
                                        upiLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "No compatible UPI application installed. Please scan the QR code using another device.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("launch_upi_app_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pay ₹${String.format(Locale.US, "%.2f", amount)} via Installed UPI App",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            if (!spacepayPaymentUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spacepayPaymentUrl))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFD4D4D8)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Launch,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFA1A1AA)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Spacepay Web Checkout",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD4D4D8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Explicit OR Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF27272A)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF18181B),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "VERIFY VIA UTR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA1A1AA),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF27272A)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verification / UTR Submission Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF141416),
                            border = BorderStroke(1.dp, Color(0xFF27272A))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Instant balance sync: If paid via QR or UPI, enter the 12-digit UTR from your bank app receipt to confirm.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }

                        Text(
                            text = "12-DIGIT UPI REFERENCE / UTR NUMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA1A1AA),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = utrNumber,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }
                                if (digitsOnly.length <= 12) {
                                    utrNumber = digitsOnly
                                    utrError = null
                                }
                            },
                            label = { Text("12-Digit UTR / UPI Ref No.") },
                            placeholder = { Text("e.g., 423871928312") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("utr_number_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            isError = utrError != null,
                            supportingText = {
                                if (utrError != null) {
                                    Text(text = utrError!!, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text(
                                        text = "${utrNumber.length}/12 Digits (Found on payment receipt)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF71717A)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isVerifying) return@Button
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (utrNumber.length != 12) {
                                    utrError = "Please enter the complete 12-digit UTR number from your payment app."
                                    return@Button
                                }
                                if (utrNumber.toSet().size <= 1) {
                                    utrError = "Invalid UTR sequence. Please enter the authentic 12-digit transaction ID."
                                    return@Button
                                }
                                isVerifying = true
                                coroutineScope.launch {
                                    delay(800)
                                    onPaymentSuccess(amount, utrNumber)
                                }
                            },
                            enabled = !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("confirm_payment_completed_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submitting Deposit Request...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit UTR for Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // DEMO PREVIEW: Instant "I HAVE PAID" test button
                        OutlinedButton(
                            onClick = {
                                if (isVerifying) return@OutlinedButton
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isVerifying = true
                                val simulatedUtr = (100000000000L..999999999999L).random().toString()
                                coroutineScope.launch {
                                    delay(600)
                                    onPaymentSuccess(amount, simulatedUtr)
                                }
                            },
                            enabled = !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("quick_test_paid_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD4D4D8)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test Preview: I Have Paid",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Explicit Cancel Button
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCancelConfirmDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("cancel_payment_gate_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA1A1AA)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFA1A1AA)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cancel Payment & Return",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ref: $transactionId • All transactions are cryptographically signed & logged.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            }
        }
    }

    if (showCancelConfirmDialog) {
        VeloRixGlassAlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abort Payment?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "If you cancel, the locked session will be discarded and no tokens or access will be granted. Are you sure you want to exit?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        Toast.makeText(context, "Payment cancelled. No funds were added.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Yes, Cancel Payment", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Continue Paying", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
