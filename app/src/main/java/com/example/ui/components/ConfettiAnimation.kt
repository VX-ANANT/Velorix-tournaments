package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants

@Composable
fun ConfettiAnimation(modifier: Modifier = Modifier, isVisible: Boolean) {
    if (!isVisible) return
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.example.R.raw.confetti))
    LottieAnimation(
        composition = composition,
        iterations = 1,
        modifier = modifier
    )
}
