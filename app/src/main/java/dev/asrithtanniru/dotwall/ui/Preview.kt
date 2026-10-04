package dev.asrithtanniru.dotwall.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private val Ghost = Color(0x59B0B0B0)

/** Rendered wallpaper inside a phone outline, optionally with ghost shapes for the Pixel lock-screen UI. */
@Composable
fun Preview(bitmap: Bitmap?, aspect: Float, showGhost: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(shape)
            .background(DotsColors.Surface)
            .border(3.dp, Color(0xFF3A3A38), shape),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Wallpaper preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
            )
        }
        if (showGhost) GhostOverlay(Modifier.fillMaxSize().padding(0.dp))
    }
}

/** Coordinates follow render.py's lockscreen() on a 1080x2400 canvas. */
@Composable
private fun GhostOverlay(modifier: Modifier) {
    Canvas(modifier) {
        val sx = size.width / 1080f
        val sy = size.height / 2400f
        fun bar(x: Float, top: Float, w: Float, h: Float) = drawRoundRect(
            Ghost, Offset(x * sx, top * sy), Size(w * sx, h * sy), CornerRadius(h * sy / 2),
        )
        // Clock, date line, At a Glance lines.
        bar(88f, 190f, 520f, 200f)
        bar(86f, 500f, 560f, 56f)
        bar(86f, 650f, 540f, 56f)
        bar(86f, 740f, 420f, 48f)
        // Fingerprint and bottom shortcuts.
        drawCircle(Ghost, 100f * sx, Offset(540f * sx, 1715f * sy))
        drawCircle(Ghost, 62f * sx, Offset(105f * sx, 2252f * sy))
        drawCircle(Ghost, 62f * sx, Offset(975f * sx, 2252f * sy))
        bar(400f, 2358f, 280f, 8f)
    }
}
