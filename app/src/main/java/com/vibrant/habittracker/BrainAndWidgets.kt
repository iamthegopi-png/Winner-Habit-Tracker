package com.vibrant.habittracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalTime
import android.graphics.Color as AColor

// ============================================================================
// BRAIN MOOD CALCULATOR (Tracks Habits + Tasks vs. Time of Day)
// ============================================================================
object BrainMoodCalculator {

    fun evaluate(
        habits: List<Habit>,
        tasks: List<TodoTask>,
        colorPref: BrainWidgetColorPref = BrainWidgetColorPref.AUTO_DYNAMIC,
        nowTime: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now()
    ): BrainMoodSnapshot {
        val habitsTotal = habits.size
        val habitsDone = habits.count { it.isCompletedOn(today) }

        val todayStr = today.toString()
        val relevantTasks = tasks.filter { task ->
            !task.isCompleted ||
                task.isRecurring ||
                task.lastCompletedDate == todayStr ||
                task.date == todayStr
        }
        val tasksTotal = relevantTasks.size
        val tasksDone = relevantTasks.count { it.isCompleted }

        val total = habitsTotal + tasksTotal
        val completed = habitsDone + tasksDone
        val remaining = (total - completed).coerceAtLeast(0)
        val ratio = if (total == 0) 1f else completed.toFloat() / total.toFloat()
        val hour = nowTime.hour

        val mood = when {
            total == 0 || ratio >= 0.85f -> BrainMoodState.RADIANT_HAPPY
            hour >= 20 && ratio < 0.75f -> BrainMoodState.ROTTEN_SAD
            hour >= 18 && ratio < 0.40f -> BrainMoodState.ROTTEN_SAD
            ratio >= 0.50f -> BrainMoodState.FOCUSED_GOOD
            hour < 14 && ratio >= 0.20f -> BrainMoodState.FOCUSED_GOOD
            hour >= 15 && ratio < 0.50f -> BrainMoodState.TIRED_SLIPPING
            else -> BrainMoodState.TIRED_SLIPPING
        }

        return buildSnapshot(
            mood = mood,
            completed = completed,
            total = total,
            remaining = remaining,
            habitsDone = habitsDone,
            habitsTotal = habitsTotal,
            tasksDone = tasksDone,
            tasksTotal = tasksTotal,
            hour = hour,
            colorPref = colorPref
        )
    }

    fun buildPreviewSnapshot(
        mood: BrainMoodState,
        colorPref: BrainWidgetColorPref
    ): BrainMoodSnapshot {
        return when (mood) {
            BrainMoodState.RADIANT_HAPPY ->
                buildSnapshot(mood, 8, 8, 0, 4, 4, 4, 4, 19, colorPref)
            BrainMoodState.FOCUSED_GOOD ->
                buildSnapshot(mood, 6, 8, 2, 3, 4, 3, 4, 14, colorPref)
            BrainMoodState.TIRED_SLIPPING ->
                buildSnapshot(mood, 3, 8, 5, 2, 4, 1, 4, 17, colorPref)
            BrainMoodState.ROTTEN_SAD ->
                buildSnapshot(mood, 1, 8, 7, 1, 4, 0, 4, 22, colorPref)
        }
    }

    private fun buildSnapshot(
        mood: BrainMoodState,
        completed: Int,
        total: Int,
        remaining: Int,
        habitsDone: Int,
        habitsTotal: Int,
        tasksDone: Int,
        tasksTotal: Int,
        hour: Int,
        colorPref: BrainWidgetColorPref
    ): BrainMoodSnapshot {
        val ratio = if (total == 0) 1f else completed.toFloat() / total.toFloat()

        val subtitle = when {
            total == 0 -> "Add habits or missions"
            remaining == 0 -> "All done today!"
            mood == BrainMoodState.ROTTEN_SAD -> "$remaining missed today"
            else -> "$remaining left today"
        }

        val headline = when (mood) {
            BrainMoodState.RADIANT_HAPPY ->
                if (remaining == 0) "Peak mind\nunlocked" else "Crushing it\ntoday!"
            BrainMoodState.FOCUSED_GOOD ->
                if (ratio >= 0.70f) "Almost\nthere!" else "Locked in\ntoday"
            BrainMoodState.TIRED_SLIPPING ->
                if (hour < 12) "Start your\nfirst win" else "Finish\nstrong now"
            BrainMoodState.ROTTEN_SAD ->
                if (hour >= 21 || ratio < 0.25f) "Try again\ntomorrow" else "Save your\nday now"
        }

        val themeColors = resolveThemeColors(mood, ratio, hour, colorPref)

        return BrainMoodSnapshot(
            mood = mood,
            completedCount = completed,
            totalCount = total,
            habitsDone = habitsDone,
            habitsTotal = habitsTotal,
            tasksDone = tasksDone,
            tasksTotal = tasksTotal,
            ratioText = "$completed / $total",
            subtitleText = subtitle,
            headlineText = headline,
            breakdownText = "$habitsDone/$habitsTotal Habits • $tasksDone/$tasksTotal Tasks",
            bgStartHex = themeColors.startHex,
            bgEndHex = themeColors.endHex,
            borderHex = themeColors.borderHex,
            drawableResId = themeColors.drawableRes
        )
    }

    private fun resolveThemeColors(
        mood: BrainMoodState,
        ratio: Float,
        hour: Int,
        colorPref: BrainWidgetColorPref
    ): WidgetThemeStyle {
        val emerald = WidgetThemeStyle(0xFF065F46L, 0xFF047857L, 0xFF6EE7B7L, R.drawable.widget_brain_bg_emerald)
        val blue = WidgetThemeStyle(0xFF1E3A8AL, 0xFF1D4ED8L, 0xFF7DD3FCL, R.drawable.widget_brain_bg_blue)
        val purple = WidgetThemeStyle(0xFF4C1D95L, 0xFF6D28D9L, 0xFFD8B4FEL, R.drawable.widget_brain_bg_purple)
        val amber = WidgetThemeStyle(0xFF78350FL, 0xFF92400EL, 0xFFFCD34DL, R.drawable.widget_brain_bg_amber)
        val crimson = WidgetThemeStyle(0xFF6B1120L, 0xFF881828L, 0xFFFDA4AFL, R.drawable.widget_brain_bg_crimson)

        return when (mood) {
            BrainMoodState.ROTTEN_SAD -> crimson
            BrainMoodState.TIRED_SLIPPING -> if (hour >= 16) amber else purple
            BrainMoodState.FOCUSED_GOOD -> when (colorPref) {
                BrainWidgetColorPref.EMERALD_GREEN -> emerald
                BrainWidgetColorPref.COSMIC_PURPLE -> purple
                BrainWidgetColorPref.SAPPHIRE_BLUE -> blue
                BrainWidgetColorPref.AUTO_DYNAMIC -> if (ratio >= 0.68f) purple else blue
            }
            BrainMoodState.RADIANT_HAPPY -> when (colorPref) {
                BrainWidgetColorPref.SAPPHIRE_BLUE -> blue
                BrainWidgetColorPref.COSMIC_PURPLE -> purple
                BrainWidgetColorPref.EMERALD_GREEN,
                BrainWidgetColorPref.AUTO_DYNAMIC -> emerald
            }
        }
    }

    private data class WidgetThemeStyle(
        val startHex: Long,
        val endHex: Long,
        val borderHex: Long,
        val drawableRes: Int
    )
}

// ============================================================================
// PROCEDURAL 3D-SHADED BRAIN CHARACTER RENDERER
// ============================================================================
object BrainCharacterRenderer {

    fun renderBrainBitmap(mood: BrainMoodState, sizePx: Int = 240): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = sizePx.toFloat()
        val h = sizePx.toFloat()
        val cx = w * 0.50f
        val cy = h * 0.50f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val palette = when (mood) {
            BrainMoodState.RADIANT_HAPPY -> BrainPalette(
                top = 0xFFFFC1D6.toInt(),
                mid = 0xFFFF82A9.toInt(),
                shadow = 0xFFB83E67.toInt(),
                outline = 0xFF4A1328.toInt(),
                neckLight = 0xFFFFD3E1.toInt(),
                neckDark = 0xFFC96889.toInt()
            )
            BrainMoodState.FOCUSED_GOOD -> BrainPalette(
                top = 0xFFFBCFE8.toInt(),
                mid = 0xFFF472B6.toInt(),
                shadow = 0xFF9D3466.toInt(),
                outline = 0xFF3E1428.toInt(),
                neckLight = 0xFFF5C7DB.toInt(),
                neckDark = 0xFFAD5D81.toInt()
            )
            BrainMoodState.TIRED_SLIPPING -> BrainPalette(
                top = 0xFFDFCFD4.toInt(),
                mid = 0xFFB59DA6.toInt(),
                shadow = 0xFF6B565E.toInt(),
                outline = 0xFF2D2126.toInt(),
                neckLight = 0xFFD4C5C9.toInt(),
                neckDark = 0xFF836E76.toInt()
            )
            BrainMoodState.ROTTEN_SAD -> BrainPalette(
                top = 0xFFDAD3C6.toInt(),
                mid = 0xFFA59B8C.toInt(),
                shadow = 0xFF564D44.toInt(),
                outline = 0xFF251F1B.toInt(),
                neckLight = 0xFFC9C1B4.toInt(),
                neckDark = 0xFF70675C.toInt()
            )
        }

        // 1. Golden ambient glow when thriving
        if (mood == BrainMoodState.RADIANT_HAPPY) {
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx,
                cy * 0.92f,
                w * 0.48f,
                intArrayOf(0x66FDE047, 0x2234D399, 0x00000000),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy * 0.92f, w * 0.48f, paint)
            paint.shader = null
        }

        // 2. Stylized neck & shoulders
        val neckPath = Path().apply {
            moveTo(cx - w * 0.14f, cy + h * 0.22f)
            quadTo(cx - w * 0.15f, cy + h * 0.36f, cx - w * 0.24f, h * 0.99f)
            lineTo(cx + w * 0.24f, h * 0.99f)
            quadTo(cx + w * 0.15f, cy + h * 0.36f, cx + w * 0.14f, cy + h * 0.22f)
            close()
        }
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            cx, cy + h * 0.20f, cx, h,
            palette.neckDark, palette.neckLight,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(neckPath, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.022f
        paint.color = palette.outline
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(neckPath, paint)

        // 3. Multi-lobed brain silhouette
        val lobes = listOf(
            Triple(cx - w * 0.17f, cy - h * 0.15f, w * 0.19f),
            Triple(cx + w * 0.17f, cy - h * 0.15f, w * 0.19f),
            Triple(cx - w * 0.29f, cy + h * 0.01f, w * 0.17f),
            Triple(cx + w * 0.29f, cy + h * 0.01f, w * 0.17f),
            Triple(cx - w * 0.21f, cy + h * 0.16f, w * 0.15f),
            Triple(cx + w * 0.21f, cy + h * 0.16f, w * 0.15f),
            Triple(cx, cy + h * 0.18f, w * 0.18f),
            Triple(cx, cy - h * 0.02f, w * 0.25f)
        )

        paint.style = Paint.Style.FILL
        paint.color = palette.outline
        for ((lx, ly, lr) in lobes) {
            canvas.drawCircle(lx, ly, lr + w * 0.024f, paint)
        }

        paint.shader = RadialGradient(
            cx - w * 0.08f,
            cy - h * 0.20f,
            w * 0.56f,
            intArrayOf(palette.top, palette.mid, palette.shadow),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        for ((lx, ly, lr) in lobes) {
            canvas.drawCircle(lx, ly, lr, paint)
        }
        paint.shader = null

        // Upper lobe specular highlights
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.016f
        paint.color = if (mood == BrainMoodState.ROTTEN_SAD) 0x33FFFFFF else 0x55FFFFFF
        canvas.drawArc(
            RectF(cx - w * 0.34f, cy - h * 0.32f, cx - w * 0.02f, cy - h * 0.04f),
            195f, 100f, false, paint
        )
        canvas.drawArc(
            RectF(cx + w * 0.02f, cy - h * 0.32f, cx + w * 0.34f, cy - h * 0.04f),
            245f, 100f, false, paint
        )

        // 4. Cortical folds & hemisphere split
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = palette.outline
        paint.alpha = if (mood == BrainMoodState.ROTTEN_SAD) 165 else 115
        paint.strokeWidth = w * 0.016f

        canvas.drawLine(cx, cy - h * 0.32f, cx, cy - h * 0.11f, paint)

        val folds = listOf(
            Path().apply {
                moveTo(cx - w * 0.26f, cy - h * 0.21f)
                quadTo(cx - w * 0.16f, cy - h * 0.14f, cx - w * 0.08f, cy - h * 0.22f)
            },
            Path().apply {
                moveTo(cx + w * 0.26f, cy - h * 0.21f)
                quadTo(cx + w * 0.16f, cy - h * 0.14f, cx + w * 0.08f, cy - h * 0.22f)
            },
            Path().apply {
                moveTo(cx - w * 0.39f, cy - h * 0.04f)
                quadTo(cx - w * 0.31f, cy - h * 0.01f, cx - w * 0.33f, cy + h * 0.09f)
            },
            Path().apply {
                moveTo(cx + w * 0.39f, cy - h * 0.04f)
                quadTo(cx + w * 0.31f, cy - h * 0.01f, cx + w * 0.33f, cy + h * 0.09f)
            },
            Path().apply {
                moveTo(cx - w * 0.26f, cy + h * 0.22f)
                quadTo(cx - w * 0.18f, cy + h * 0.19f, cx - w * 0.15f, cy + h * 0.26f)
            },
            Path().apply {
                moveTo(cx + w * 0.26f, cy + h * 0.22f)
                quadTo(cx + w * 0.18f, cy + h * 0.19f, cx + w * 0.15f, cy + h * 0.26f)
            }
        )
        for (fold in folds) {
            canvas.drawPath(fold, paint)
        }

        if (mood == BrainMoodState.ROTTEN_SAD) {
            paint.style = Paint.Style.FILL
            paint.color = 0x384A5236
            canvas.drawCircle(cx - w * 0.24f, cy - h * 0.19f, w * 0.045f, paint)
            canvas.drawCircle(cx + w * 0.27f, cy + h * 0.12f, w * 0.050f, paint)
            canvas.drawCircle(cx + w * 0.19f, cy - h * 0.23f, w * 0.032f, paint)
        }

        // 5. Facial expressions per mood
        drawExpression(canvas, paint, mood, w, h, cx, cy, palette)

        return bitmap
    }

    private fun drawExpression(
        c: Canvas,
        p: Paint,
        mood: BrainMoodState,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        palette: BrainPalette
    ) {
        val leftEyeX = cx - w * 0.145f
        val rightEyeX = cx + w * 0.145f
        val eyeY = cy + h * 0.02f

        when (mood) {
            BrainMoodState.RADIANT_HAPPY -> {
                p.style = Paint.Style.FILL
                p.color = 0x55FF3B7F
                c.drawOval(RectF(leftEyeX - w * 0.11f, eyeY + h * 0.06f, leftEyeX + w * 0.02f, eyeY + h * 0.13f), p)
                c.drawOval(RectF(rightEyeX - w * 0.02f, eyeY + h * 0.06f, rightEyeX + w * 0.11f, eyeY + h * 0.13f), p)

                drawGlossyEye(c, p, leftEyeX, eyeY, w * 0.072f, palette.outline, isSparkling = true)
                drawGlossyEye(c, p, rightEyeX, eyeY, w * 0.072f, palette.outline, isSparkling = true)

                val smilePath = Path().apply {
                    moveTo(cx - w * 0.11f, cy + h * 0.14f)
                    quadTo(cx, cy + h * 0.27f, cx + w * 0.11f, cy + h * 0.14f)
                    close()
                }
                p.style = Paint.Style.FILL
                p.color = 0xFF6B1435.toInt()
                c.drawPath(smilePath, p)
                p.color = 0xFFFF7AA2.toInt()
                c.drawOval(RectF(cx - w * 0.05f, cy + h * 0.18f, cx + w * 0.05f, cy + h * 0.225f), p)
                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.strokeWidth = w * 0.018f
                c.drawPath(smilePath, p)

                drawFourPointStar(c, p, cx - w * 0.36f, cy - h * 0.28f, w * 0.058f, 0xFFFDE047.toInt())
                drawFourPointStar(c, p, cx + w * 0.37f, cy - h * 0.25f, w * 0.048f, 0xFFFFFFFF.toInt())
                drawFourPointStar(c, p, cx + w * 0.39f, cy + h * 0.22f, w * 0.038f, 0xFF6EE7B7.toInt())
            }

            BrainMoodState.FOCUSED_GOOD -> {
                p.style = Paint.Style.FILL
                p.color = 0x3DFF4D8D
                c.drawOval(RectF(leftEyeX - w * 0.10f, eyeY + h * 0.06f, leftEyeX + w * 0.01f, eyeY + h * 0.115f), p)
                c.drawOval(RectF(rightEyeX - w * 0.01f, eyeY + h * 0.06f, rightEyeX + w * 0.10f, eyeY + h * 0.115f), p)

                drawGlossyEye(c, p, leftEyeX, eyeY, w * 0.066f, palette.outline, isSparkling = false)
                drawGlossyEye(c, p, rightEyeX, eyeY, w * 0.066f, palette.outline, isSparkling = false)

                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.strokeWidth = w * 0.020f
                p.strokeCap = Paint.Cap.ROUND
                c.drawArc(RectF(leftEyeX - w * 0.07f, eyeY - h * 0.12f, leftEyeX + w * 0.07f, eyeY - h * 0.05f), 200f, 140f, false, p)
                c.drawArc(RectF(rightEyeX - w * 0.07f, eyeY - h * 0.12f, rightEyeX + w * 0.07f, eyeY - h * 0.05f), 200f, 140f, false, p)

                val smile = Path().apply {
                    moveTo(cx - w * 0.095f, cy + h * 0.155f)
                    quadTo(cx, cy + h * 0.235f, cx + w * 0.095f, cy + h * 0.155f)
                }
                p.strokeWidth = w * 0.022f
                c.drawPath(smile, p)
            }

            BrainMoodState.TIRED_SLIPPING -> {
                p.style = Paint.Style.FILL
                p.color = 0x444A2C3B
                c.drawOval(RectF(leftEyeX - w * 0.09f, eyeY + h * 0.02f, leftEyeX + w * 0.09f, eyeY + h * 0.10f), p)
                c.drawOval(RectF(rightEyeX - w * 0.09f, eyeY + h * 0.02f, rightEyeX + w * 0.09f, eyeY + h * 0.10f), p)

                drawTiredEye(c, p, leftEyeX, eyeY, w * 0.078f, h * 0.048f, palette.mid, palette.outline, heavyDroop = false)
                drawTiredEye(c, p, rightEyeX, eyeY, w * 0.078f, h * 0.048f, palette.mid, palette.outline, heavyDroop = false)

                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.strokeWidth = w * 0.020f
                c.drawLine(leftEyeX - w * 0.08f, eyeY - h * 0.06f, leftEyeX + w * 0.06f, eyeY - h * 0.095f, p)
                c.drawLine(rightEyeX - w * 0.06f, eyeY - h * 0.095f, rightEyeX + w * 0.08f, eyeY - h * 0.06f, p)

                val mouth = Path().apply {
                    moveTo(cx - w * 0.085f, cy + h * 0.18f)
                    quadTo(cx - w * 0.03f, cy + h * 0.155f, cx, cy + h * 0.18f)
                    quadTo(cx + w * 0.03f, cy + h * 0.20f, cx + w * 0.085f, cy + h * 0.175f)
                }
                p.strokeWidth = w * 0.020f
                c.drawPath(mouth, p)

                val dropPath = Path().apply {
                    moveTo(cx + w * 0.30f, cy - h * 0.19f)
                    quadTo(cx + w * 0.355f, cy - h * 0.11f, cx + w * 0.30f, cy - h * 0.08f)
                    quadTo(cx + w * 0.245f, cy - h * 0.11f, cx + w * 0.30f, cy - h * 0.19f)
                    close()
                }
                p.style = Paint.Style.FILL
                p.color = 0xFF38BDF8.toInt()
                c.drawPath(dropPath, p)
                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.strokeWidth = w * 0.012f
                c.drawPath(dropPath, p)
            }

            BrainMoodState.ROTTEN_SAD -> {
                p.style = Paint.Style.FILL
                p.color = 0x8C52313B.toInt()
                c.drawOval(RectF(leftEyeX - w * 0.115f, eyeY + h * 0.01f, leftEyeX + w * 0.105f, eyeY + h * 0.135f), p)
                c.drawOval(RectF(rightEyeX - w * 0.105f, eyeY + h * 0.01f, rightEyeX + w * 0.115f, eyeY + h * 0.135f), p)

                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.alpha = 175
                p.strokeWidth = w * 0.013f
                c.drawArc(RectF(leftEyeX - w * 0.10f, eyeY + h * 0.02f, leftEyeX + w * 0.09f, eyeY + h * 0.115f), 15f, 150f, false, p)
                c.drawArc(RectF(rightEyeX - w * 0.09f, eyeY + h * 0.02f, rightEyeX + w * 0.10f, eyeY + h * 0.115f), 15f, 150f, false, p)

                drawTiredEye(c, p, leftEyeX, eyeY + h * 0.015f, w * 0.095f, h * 0.055f, palette.mid, palette.outline, heavyDroop = true)
                drawTiredEye(c, p, rightEyeX, eyeY + h * 0.015f, w * 0.095f, h * 0.055f, palette.mid, palette.outline, heavyDroop = true)

                p.style = Paint.Style.STROKE
                p.color = palette.outline
                p.alpha = 255
                p.strokeWidth = w * 0.024f
                p.strokeCap = Paint.Cap.ROUND
                val frown = Path().apply {
                    moveTo(cx - w * 0.11f, cy + h * 0.215f)
                    quadTo(cx, cy + h * 0.135f, cx + w * 0.11f, cy + h * 0.215f)
                }
                c.drawPath(frown, p)

                p.strokeWidth = w * 0.013f
                p.alpha = 140
                c.drawArc(RectF(cx - w * 0.055f, cy + h * 0.205f, cx + w * 0.055f, cy + h * 0.245f), 205f, 130f, false, p)
            }
        }
    }

    private fun drawGlossyEye(
        c: Canvas,
        p: Paint,
        x: Float,
        y: Float,
        r: Float,
        outlineColor: Int,
        isSparkling: Boolean
    ) {
        p.style = Paint.Style.FILL
        p.color = AColor.WHITE
        c.drawCircle(x, y, r, p)

        p.color = 0xFF1A1025.toInt()
        c.drawCircle(x, y, r * 0.72f, p)

        p.color = AColor.WHITE
        c.drawCircle(x - r * 0.25f, y - r * 0.28f, r * 0.28f, p)
        if (isSparkling) {
            c.drawCircle(x + r * 0.22f, y + r * 0.20f, r * 0.14f, p)
        }

        p.style = Paint.Style.STROKE
        p.color = outlineColor
        p.strokeWidth = r * 0.22f
        c.drawCircle(x, y, r, p)
    }

    private fun drawTiredEye(
        c: Canvas,
        p: Paint,
        x: Float,
        y: Float,
        rx: Float,
        ry: Float,
        lidColor: Int,
        outlineColor: Int,
        heavyDroop: Boolean
    ) {
        val eyeRect = RectF(x - rx, y - ry, x + rx, y + ry)

        p.style = Paint.Style.FILL
        p.color = if (heavyDroop) 0xFFE8DED6.toInt() else 0xFFF8F4F6.toInt()
        c.drawOval(eyeRect, p)

        p.color = 0xFF1F1619.toInt()
        c.drawCircle(x, y + ry * 0.12f, ry * 0.52f, p)

        p.color = 0xBBFFFFFF.toInt()
        c.drawCircle(x - ry * 0.18f, y, ry * 0.16f, p)

        val lidCutY = if (heavyDroop) y + ry * 0.10f else y - ry * 0.12f
        val lidPath = Path().apply {
            addArc(eyeRect, 180f, 180f)
            lineTo(x + rx, lidCutY)
            quadTo(x, lidCutY + ry * (if (heavyDroop) 0.28f else 0.10f), x - rx, lidCutY)
            close()
        }
        p.color = lidColor
        c.drawPath(lidPath, p)

        p.style = Paint.Style.STROKE
        p.color = outlineColor
        p.strokeWidth = rx * 0.18f
        c.drawOval(eyeRect, p)

        val lidLine = Path().apply {
            moveTo(x - rx * 1.05f, lidCutY)
            quadTo(x, lidCutY + ry * (if (heavyDroop) 0.28f else 0.10f), x + rx * 1.05f, lidCutY)
        }
        c.drawPath(lidLine, p)
    }

    private fun drawFourPointStar(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        val star = Path().apply {
            moveTo(cx, cy - r)
            quadTo(cx, cy, cx + r, cy)
            quadTo(cx, cy, cx, cy + r)
            quadTo(cx, cy, cx - r, cy)
            quadTo(cx, cy, cx, cy - r)
            close()
        }
        p.style = Paint.Style.FILL
        p.color = color
        c.drawPath(star, p)
    }

    private data class BrainPalette(
        val top: Int,
        val mid: Int,
        val shadow: Int,
        val outline: Int,
        val neckLight: Int,
        val neckDark: Int
    )
}

// ============================================================================
// HOME SCREEN WIDGET 1: REACTIVE BRAIN MOOD WIDGET
// ============================================================================
class BrainMoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllBrainWidgets(context)
    }

    companion object {
        fun updateAllBrainWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, BrainMoodWidgetProvider::class.java))
                if (ids.isEmpty()) return

                val storage = StudioStorageManager(context)
                val habits = storage.loadHabitsOnly()
                val tasks = storage.loadTasksOnly()
                val colorPref = storage.getBrainWidgetColorPref()

                val snapshot = BrainMoodCalculator.evaluate(
                    habits = habits,
                    tasks = tasks,
                    colorPref = colorPref
                )
                val brainBitmap = BrainCharacterRenderer.renderBrainBitmap(snapshot.mood, sizePx = 240)

                val openPending = PendingIntent.getActivity(
                    context,
                    99,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in ids) {
                    val views = RemoteViews(context.packageName, R.layout.brain_widget_layout)
                    views.setInt(R.id.brain_widget_root, "setBackgroundResource", snapshot.drawableResId)
                    views.setTextViewText(R.id.brain_widget_ratio, snapshot.ratioText)
                    views.setTextViewText(R.id.brain_widget_subtitle, snapshot.subtitleText)
                    views.setTextViewText(R.id.brain_widget_message, snapshot.headlineText)
                    views.setTextViewText(R.id.brain_widget_breakdown, snapshot.breakdownText)
                    views.setImageViewBitmap(R.id.brain_widget_character, brainBitmap)
                    views.setOnClickPendingIntent(R.id.brain_widget_root, openPending)
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            }
        }
    }
}

// ============================================================================
// HOME SCREEN WIDGET 2: ALPHA HABITS CHECKLIST WIDGET
// ============================================================================
class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_HABIT) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
            val storage = StudioStorageManager(context)
            val resultPair = storage.toggleHabitDirectlyFromWidget(habitId)
            if (resultPair != null) {
                val (toggledTitle, soundUri) = resultPair
                CelebrationSoundEngine.playCustomOrDefaultSound(context, soundUri, isReminder = false)
                Toast.makeText(context, "⚡ $toggledTitle Complete! +35 XP", Toast.LENGTH_SHORT).show()
            }
            updateAllWidgets(context)
            BrainMoodWidgetProvider.updateAllBrainWidgets(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE_HABIT = "com.vibrant.habittracker.ACTION_WIDGET_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"

        fun updateAllWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
                if (ids.isEmpty()) return

                val storage = StudioStorageManager(context)
                val habits = storage.loadHabitsOnly()
                val today = LocalDate.now()
                val doneCount = habits.count { it.isCompletedOn(today) }

                val rowIds = listOf(
                    Triple(R.id.widget_row_1, R.id.widget_title_1, R.id.widget_btn_1),
                    Triple(R.id.widget_row_2, R.id.widget_title_2, R.id.widget_btn_2),
                    Triple(R.id.widget_row_3, R.id.widget_title_3, R.id.widget_btn_3),
                    Triple(R.id.widget_row_4, R.id.widget_title_4, R.id.widget_btn_4)
                )

                for (widgetId in ids) {
                    val views = RemoteViews(context.packageName, R.layout.habit_widget_layout)
                    views.setTextViewText(R.id.widget_header_score, "$doneCount/${habits.size} Done")

                    val openPending = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_header_title, openPending)

                    for (i in 0 until 4) {
                        val (rowId, titleId, btnId) = rowIds[i]
                        if (i < habits.size) {
                            val h = habits[i]
                            val isDone = h.isCompletedOn(today)
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(titleId, "${h.emoji} ${h.title}  •  🔥${h.currentStreak(today)}d")
                            views.setTextViewText(btnId, if (isDone) "✓" else "+")
                            views.setInt(
                                btnId,
                                "setBackgroundResource",
                                if (isDone) R.drawable.widget_btn_done else R.drawable.widget_btn_pending
                            )
                            views.setTextColor(
                                btnId,
                                if (isDone) AColor.BLACK else AColor.parseColor("#34D399")
                            )

                            val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_TOGGLE_HABIT
                                putExtra(EXTRA_HABIT_ID, h.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context,
                                h.id.hashCode(),
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(btnId, togglePending)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            }
        }
    }
}

// ============================================================================
// HOME SCREEN WIDGET 3: ALPHA MISSIONS (TASKS) WIDGET
// ============================================================================
class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllTaskWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
            val storage = StudioStorageManager(context)
            val resultPair = storage.toggleTaskDirectlyFromWidget(taskId)
            if (resultPair != null) {
                val (completedTitle, soundUri) = resultPair
                CelebrationSoundEngine.playCustomOrDefaultSound(context, soundUri, isReminder = false)
                Toast.makeText(context, "🎯 Mission Complete: $completedTitle!", Toast.LENGTH_SHORT).show()
            }
            updateAllTaskWidgets(context)
            BrainMoodWidgetProvider.updateAllBrainWidgets(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE_TASK = "com.vibrant.habittracker.ACTION_WIDGET_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

        fun updateAllTaskWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
                if (ids.isEmpty()) return

                val storage = StudioStorageManager(context)
                val tasks = storage.loadTasksOnly()
                    .sortedWith(compareBy<TodoTask> { it.isCompleted }.thenBy { it.priority.ordinal })
                val pendingCount = tasks.count { !it.isCompleted }

                val rowIds = listOf(
                    Triple(R.id.task_widget_row_1, R.id.task_widget_title_1, R.id.task_widget_btn_1),
                    Triple(R.id.task_widget_row_2, R.id.task_widget_title_2, R.id.task_widget_btn_2),
                    Triple(R.id.task_widget_row_3, R.id.task_widget_title_3, R.id.task_widget_btn_3),
                    Triple(R.id.task_widget_row_4, R.id.task_widget_title_4, R.id.task_widget_btn_4)
                )

                for (widgetId in ids) {
                    val views = RemoteViews(context.packageName, R.layout.task_widget_layout)
                    views.setTextViewText(R.id.task_widget_header_score, "$pendingCount Active")

                    val openPending = PendingIntent.getActivity(
                        context,
                        1,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.task_widget_header_title, openPending)

                    for (i in 0 until 4) {
                        val (rowId, titleId, btnId) = rowIds[i]
                        if (i < tasks.size) {
                            val t = tasks[i]
                            val renewIcon = if (t.isRecurring) " 🔄" else ""
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(titleId, "[${t.priority.shortTag}] ${t.title}$renewIcon")
                            views.setTextViewText(btnId, "✓")
                            views.setInt(
                                btnId,
                                "setBackgroundResource",
                                if (t.isCompleted) R.drawable.widget_btn_done else R.drawable.widget_btn_pending
                            )
                            views.setTextColor(
                                btnId,
                                if (t.isCompleted) AColor.BLACK else AColor.parseColor("#34D399")
                            )

                            val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_TOGGLE_TASK
                                putExtra(EXTRA_TASK_ID, t.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context,
                                t.id.hashCode(),
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(btnId, togglePending)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            }
        }
    }
}
