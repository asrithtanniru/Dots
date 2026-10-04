package dev.asrithtanniru.dotwall.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Size
import dev.asrithtanniru.dotwall.model.DotShape
import dev.asrithtanniru.dotwall.model.Goal
import dev.asrithtanniru.dotwall.model.Look
import dev.asrithtanniru.dotwall.model.RenderSpec
import dev.asrithtanniru.dotwall.model.WeekStart
import dev.asrithtanniru.dotwall.model.YearStyle
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Port of render.py. Pure: same spec, size and date always give the same pixels. */
object WallpaperRenderer {
    private const val PAST = 0xFFECEAE4.toInt()
    private const val FUTURE = 0xFF2A2927.toInt()
    private const val MUTED = 0xFF7C7A74.toInt()
    private const val TITLE = 0xFFBDBAB3.toInt()

    private const val LABEL_GAP = 56f
    private const val SEPARATOR = "  ·  "

    fun render(
        spec: RenderSpec,
        size: Size,
        today: LocalDate,
        fonts: WallpaperFonts = WallpaperFonts.System,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(spec.look.background.argb)

        val p = spec.placement
        val band = RectF(
            p.side * size.width,
            p.top * size.height,
            (1 - p.side) * size.width,
            p.bottom * size.height,
        )
        val look = spec.look
        val painter = Painter(canvas, look, fonts)

        when (val r = GridMath.resolve(spec, today)) {
            Resolved.Year -> if (spec.yearStyle == YearStyle.Months) {
                painter.months(band, today, spec.weekStart)
            } else {
                painter.grid(
                    band, GridMath.yearCounts(today), today, cols = null, maxPitch = 70f,
                    header = null, footer = GridMath.yearProgress(today),
                )
            }
            Resolved.Life -> painter.grid(
                band, GridMath.lifeCounts(spec.birthDate, today, spec.lifespanYears), today,
                cols = GridMath.LIFE_COLS, maxPitch = 70f, header = null,
                footer = GridMath.lifeProgress(spec.birthDate, today, spec.lifespanYears),
            )
            is Resolved.ActiveGoal -> painter.goal(band, r.goal, today, finished = false)
            is Resolved.FinishedGoal -> painter.goal(band, r.goal, today, finished = true)
        }
        return bmp
    }

    private class Painter(private val canvas: Canvas, private val look: Look, private val fonts: WallpaperFonts) {
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        private fun drawDot(cx: Float, cy: Float, radius: Float, color: Int) {
            dot.color = color
            when (look.shape) {
                DotShape.Circle -> canvas.drawCircle(cx, cy, radius, dot)
                DotShape.RoundedSquare -> {
                    rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                    canvas.drawRoundRect(rect, radius * 0.35f, radius * 0.35f, dot)
                }
            }
        }

        fun goal(band: RectF, goal: Goal, today: LocalDate, finished: Boolean) {
            val footer = if (finished) GridMath.finishedProgress(goal) else GridMath.goalProgress(goal, today)
            grid(
                band, GridMath.goalCounts(goal, today), today, cols = GridMath.GOAL_COLS, maxPitch = 58f,
                header = goal.title.takeIf { look.showGoalTitle }, footer = footer, anchorTop = true,
            )
        }

        fun grid(
            band: RectF, counts: DotCounts, today: LocalDate, cols: Int?, maxPitch: Float,
            header: String?, footer: ProgressText, anchorTop: Boolean = false,
        ) {
            val showFooter = look.showFooter
            val headH = if (header != null) LABEL_GAP else 0f
            val footH = if (showFooter) LABEL_GAP else 0f
            val gw = band.width()
            val gh = band.height() - headH - footH
            val fit = if (cols != null) GridMath.fixedCols(counts.total, cols, gw, gh)
            else GridMath.bestCols(counts.total, gw, gh)
            val pitch = min(fit.pitch, maxPitch)
            val r = pitch * look.density.radiusFactor
            val tw = fit.cols * pitch
            val th = fit.rows * pitch
            val ox = band.left + (gw - tw) / 2
            // Short grids hug the top of the band so they stay clear of the fingerprint area.
            val oy = if (anchorTop) band.top + headH else band.top + (band.height() - (th + headH + footH)) / 2 + headH

            for (i in 0 until counts.total) {
                val cx = ox + (i % fit.cols + 0.5f) * pitch
                val cy = oy + (i / fit.cols + 0.5f) * pitch
                when {
                    i == counts.today -> drawDot(cx, cy, max(r * 1.35f, r + 5f), look.accent)
                    i < counts.filled -> drawDot(cx, cy, r, PAST)
                    else -> drawDot(cx, cy, r, FUTURE)
                }
            }

            val mid = band.centerX()
            if (header != null) {
                text.typeface = fonts.medium
                text.textSize = 36f
                text.color = TITLE
                text.textAlign = Paint.Align.CENTER
                canvas.drawText(header, mid, oy - 22f, text)
            }
            if (showFooter) footerText(footer, mid, oy + th + 50f)
        }

        fun footerText(footer: ProgressText, centerX: Float, baseline: Float) {
            text.typeface = fonts.medium
            text.textSize = 32f
            text.textAlign = Paint.Align.LEFT
            val lw = text.measureText(footer.left)
            val sw = text.measureText(SEPARATOR)
            val rw = text.measureText(footer.right)
            val sx = centerX - (lw + sw + rw) / 2
            text.color = look.accent
            canvas.drawText(footer.left, sx, baseline, text)
            text.color = MUTED
            canvas.drawText(SEPARATOR, sx + lw, baseline, text)
            canvas.drawText(footer.right, sx + lw + sw, baseline, text)
        }

        fun months(band: RectF, today: LocalDate, weekStart: WeekStart) {
            val colsM = 3
            val rowsM = 4
            val labelH = 46f
            val foot = if (look.showFooter) 70f else 0f
            val cellW = band.width() / colsM
            val pitch = min((cellW - 40f) / 7, (band.height() - foot) / rowsM / 7.6f)
            val r = pitch * look.density.radiusFactor
            val blockH = 6 * pitch + labelH
            val gapY = min(((band.height() - foot) - rowsM * blockH) / (rowsM - 1 + 0.0001f), 60f)
            val total = rowsM * blockH + (rowsM - 1) * gapY
            val oy = band.top + ((band.height() - foot) - total) / 2

            text.typeface = fonts.semibold
            text.textSize = 26f
            text.textAlign = Paint.Align.LEFT
            for (m in 0 until 12) {
                val bx = band.left + (m % 3) * cellW + (cellW - 7 * pitch) / 2
                val by = oy + (m / 3) * (blockH + gapY)
                val first = LocalDate.of(today.year, m + 1, 1)
                val offset = when (weekStart) {
                    WeekStart.Monday -> first.dayOfWeek.value - 1
                    WeekStart.Sunday -> first.dayOfWeek.value % 7
                }
                text.color = if (m + 1 == today.monthValue) look.accent else MUTED
                canvas.drawText(
                    first.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                    bx + r * 0.2f, by + 26f, text,
                )
                for (i in 0 until first.lengthOfMonth()) {
                    val k = i + offset
                    val cx = bx + (k % 7 + 0.5f) * pitch
                    val cy = by + labelH + (k / 7 + 0.5f) * pitch
                    val day = first.plusDays(i.toLong())
                    val color = when {
                        day == today -> look.accent
                        day.isBefore(today) -> PAST
                        else -> FUTURE
                    }
                    drawDot(cx, cy, r, color)
                }
            }
            if (look.showFooter) footerText(GridMath.yearProgress(today), band.centerX(), oy + total + 64f)
        }
    }
}
