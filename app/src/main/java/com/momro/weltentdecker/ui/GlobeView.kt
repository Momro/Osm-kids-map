package com.momro.weltentdecker.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momro.weltentdecker.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor

private const val GLOBE_SIZE = 768

@Composable
fun GlobeOverlay(texture: Bitmap, onTap: () -> Unit = {}, modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(0f) }
    val texW = texture.width

    LaunchedEffect(texture) {
        while (true) {
            delay(66)
            phase = (phase + 2f) % texW
        }
    }

    val globe = remember(texture, phase) { renderGlobeBitmap(texture, phase) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF050A18))) {
        Image(
            bitmap = globe.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(texture) {
                    detectTapGestures(onTap = { onTap() })
                    detectDragGestures { change, drag ->
                        change.consume()
                        val d = minOf(size.width, size.height)
                        val v = phase + drag.x * texW / (PI * d)
                        phase = ((v % texW) + texW) % texW
                    }
                }
        )
        Text(
            text = stringResource(R.string.globe_hint),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }
}

private fun renderGlobeBitmap(texture: Bitmap, phaseTex: Float, outSize: Int = GLOBE_SIZE): Bitmap {
    val out = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val r = outSize / 2f
    val texW = texture.width
    val texH = texture.height

    val glowPaint = Paint().apply {
        shader = RadialGradient(
            r, r, r * 1.15f,
            AndroidColor.argb(80, 80, 150, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r * 1.15f, glowPaint)

    canvas.save()
    canvas.clipPath(Path().apply { addCircle(r, r, r, Path.Direction.CW) })

    val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    val src = Rect()
    val dst = RectF()
    val step = 2
    var i = 0
    while (i < outSize) {
        val u = (i + step / 2f - r) / r
        if (u > -1f && u < 1f) {
            val lon = asin(u.toDouble())
            val raw = ((lon + PI) / (2.0 * PI)).toFloat() * texW + phaseTex
            val texCenter = ((raw % texW) + texW) % texW
            val cosLon = cos(lon).toFloat().coerceAtLeast(0.05f)
            val dTex = (texW / (2.0 * PI * r * cosLon) * step).toFloat()
            val left = floor((texCenter - dTex / 2).toDouble()).toInt().coerceIn(0, texW - 1)
            var right = ceil((texCenter + dTex / 2).toDouble()).toInt().coerceIn(1, texW)
            if (right <= left) right = left + 1
            src.set(left, 0, right, texH)
            dst.set(i.toFloat(), 0f, (i + step).toFloat(), outSize.toFloat())
            canvas.drawBitmap(texture, src, dst, slicePaint)
        }
        i += step
    }

    val shadePaint = Paint().apply {
        shader = RadialGradient(
            r, r, r,
            AndroidColor.TRANSPARENT,
            AndroidColor.argb(90, 0, 0, 20),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r, shadePaint)

    val highlightPaint = Paint().apply {
        shader = RadialGradient(
            r * 0.7f, r * 0.65f, r * 0.8f,
            AndroidColor.argb(80, 255, 255, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r, highlightPaint)

    canvas.restore()

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = outSize / 128f
        color = AndroidColor.argb(110, 120, 180, 255)
    }
    canvas.drawCircle(r, r, r - ringPaint.strokeWidth / 2, ringPaint)
    return out
}
