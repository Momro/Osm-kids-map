package com.momro.weltentdecker.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momro.weltentdecker.R
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos

private const val GLOBE_SIZE = 1024

@Composable
fun GlobeOverlay(texture: Bitmap, modifier: Modifier = Modifier) {
    val globe = remember(texture) { renderGlobeBitmap(texture) }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color(0xFF050A18))
            val d = minOf(size.width, size.height) * 0.9f
            drawImage(
                globe.asImageBitmap(),
                dstOffset = Offset((size.width - d) / 2f, (size.height - d) / 2f),
                dstSize = IntSize(d.toInt(), d.toInt())
            )
        }
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

private fun renderGlobeBitmap(texture: Bitmap, outSize: Int = GLOBE_SIZE): Bitmap {
    val out = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val r = outSize / 2f
    val texW = texture.width.toFloat()
    val texH = texture.height.toFloat()

    val glowPaint = Paint().apply {
        shader = RadialGradient(
            r, r, r * 1.18f,
            AndroidColor.argb(90, 70, 130, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r * 1.18f, glowPaint)

    canvas.save()
    canvas.clipPath(Path().apply { addCircle(r, r, r, Path.Direction.CW) })

    val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    val src = RectF()
    val dst = RectF()
    for (i in 0 until outSize) {
        val u = (i + 0.5f - r) / r
        if (u <= -1f || u >= 1f) continue
        val lon = asin(u.toDouble())
        val texX = ((lon + PI) / (2.0 * PI)).toFloat() * texW
        val cosLon = cos(lon).toFloat()
        val dTex = (texW / (2.0 * PI * r * cosLon)).toFloat() * 1.5f
        src.set(
            (texX - dTex / 2).coerceIn(0f, texW),
            0f,
            (texX + dTex / 2).coerceIn(0f, texW),
            texH
        )
        dst.set(i.toFloat(), 0f, i + 1f, outSize.toFloat())
        canvas.drawBitmap(texture, src, dst, slicePaint)
    }

    val shadePaint = Paint().apply {
        shader = RadialGradient(
            r, r, r,
            AndroidColor.TRANSPARENT,
            AndroidColor.argb(150, 0, 0, 10),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r, shadePaint)

    val highlightPaint = Paint().apply {
        shader = RadialGradient(
            r * 0.72f, r * 0.68f, r * 0.85f,
            AndroidColor.argb(70, 255, 255, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(r, r, r, highlightPaint)

    canvas.restore()
    return out
}
