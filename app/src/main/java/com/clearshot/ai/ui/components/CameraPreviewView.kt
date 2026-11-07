package com.clearshot.ai.ui.components

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Camera preview surface using CameraX PreviewView with tap-to-focus, pinch-to-zoom,
 * and seamless real-time digital viewfinder magnification up to 100X.
 *
 * Uses PreviewView.ImplementationMode.COMPATIBLE (TextureView) to allow smooth hardware-accelerated
 * Compose graphics scaling (graphicsLayer) beyond the camera sensor's native hardware zoom.
 */
@Composable
fun CameraPreviewView(
    zoomRatio: Float = 1.0f,
    nativeMaxZoom: Float = 8.0f,
    panOffset: Offset = Offset.Zero,
    onPreviewReady: (PreviewView) -> Unit,
    onTapToFocus: (x: Float, y: Float, width: Int, height: Int) -> Unit,
    onPinchZoom: (zoomFactor: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize = IntSize.Zero

    // Calculate viewfinder digital magnification beyond sensor's native optical/digital limit
    val digitalScale = if (zoomRatio > nativeMaxZoom && nativeMaxZoom > 0f) {
        (zoomRatio / nativeMaxZoom).coerceIn(1.0f, 25.0f)
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                    var isZooming = false

                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val pointerCount = event.changes.size

                        if (pointerCount >= 2) {
                            isZooming = true
                            val zoomFactor = event.calculateZoom()
                            if (zoomFactor != 1.0f) {
                                onPinchZoom(zoomFactor)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    // If single tap and was not pinching, trigger focus & metering with unscaled coordinates
                    if (!isZooming && down.pressed.not() && viewSize.width > 0 && viewSize.height > 0) {
                        val centerX = viewSize.width / 2f
                        val centerY = viewSize.height / 2f
                        val unscaledX = ((down.position.x - centerX) / digitalScale + centerX)
                            .coerceIn(0f, viewSize.width.toFloat())
                        val unscaledY = ((down.position.y - centerY) / digitalScale + centerY)
                            .coerceIn(0f, viewSize.height.toFloat())
                        onTapToFocus(unscaledX, unscaledY, viewSize.width, viewSize.height)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    // COMPATIBLE uses TextureView, enabling graphicsLayer scaling for 100X viewfinder
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onPreviewReady(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = digitalScale
                    scaleY = digitalScale
                    if (digitalScale > 1.0f) {
                        val maxTransX = size.width * (digitalScale - 1f) / 2f
                        val maxTransY = size.height * (digitalScale - 1f) / 2f
                        translationX = -panOffset.x * 2f * maxTransX
                        translationY = -panOffset.y * 2f * maxTransY
                    } else {
                        translationX = 0f
                        translationY = 0f
                    }
                }
        )
    }
}
