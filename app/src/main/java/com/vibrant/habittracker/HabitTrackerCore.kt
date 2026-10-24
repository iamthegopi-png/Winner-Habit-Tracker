package com.vibrant.habittracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// ============================================================================
// 1. STUDIO COLOR SYSTEM + EYE-SOOTHING SANCTUARY READING THEMES
// ============================================================================
object AppColors {
    val BgDeep = Color(0xFF070A10)
    val BgSurface = Color(0xFF0E131D)
    val BgCard = Color(0xFF151C28)
    val BgCardElevated = Color(0xFF1E2738)
    val BorderSubtle = Color(0xFF28354A)

    val MintGlow = Color(0xFF34D399)
    val EmeraldNeon = Color(0xFF10B981)
    val ElectricViolet = Color(0xFFA855F7)
    val CyberCyan = Color(0xFF06B6D4)
    val SunsetCoral = Color(0xFFF97316)
    val CrimsonRose = Color(0xFFF43F5E)
    val SolarGold = Color(0xFFFBBF24)
    val IndigoPulse = Color(0xFF6366F1)

    val NeonGreen = MintGlow
    val NeonGreenGlow = Color(0xFF064E3B)
    val MintGreen = MintGlow
    val LavenderPurple = ElectricViolet
    val PeriwinkleBlue = IndigoPulse
    val CoralOrange = SunsetCoral
    val RoseRed = CrimsonRose
    val AmberGold = SolarGold
    val BorderDark = BorderSubtle

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    val StudioPalette = listOf(
        MintGlow,
        ElectricViolet,
        CyberCyan,
        SunsetCoral,
        SolarGold,
        CrimsonRose,
        IndigoPulse
    )
    val AccentPresets = StudioPalette
}

enum class SanctuaryReadingTheme(
    val label: String,
    val bgHex: Long,
    val cardHex: Long,
    val borderHex: Long,
    val primaryTextHex: Long,
    val secondaryTextHex: Long,
    val accentHex: Long
) {
    WARM_SEPIA(
        label = "☕ Warm Candlelight",
        bgHex = 0xFF181410,
        cardHex = 0xFF231D17,
        borderHex = 0xFF3A3026,
        primaryTextHex = 0xFFF5E6D3,
        secondaryTextHex = 0xFFC8B6A2,
        accentHex = 0xFFEAB308
    ),
    FOREST_SAGE(
        label = "🌿 Forest Zen",
        bgHex = 0xFF0C1512,
        cardHex = 0xFF13221D,
        borderHex = 0xFF233B32,
        primaryTextHex = 0xFFE6F4EE,
        secondaryTextHex = 0xFFA3C4B6,
        accentHex = 0xFF34D399
    ),
    VELVET_DUSK(
        label = "🌙 Velvet Dusk",
        bgHex = 0xFF120F1D,
        cardHex = 0xFF1B172B,
        borderHex = 0xFF30294A,
        primaryTextHex = 0xFFEDE9FE,
        secondaryTextHex = 0xFFB8B0D8,
        accentHex = 0xFFC084FC
    ),
    STUDIO_OBSIDIAN(
        label = "✨ Crisp Obsidian",
        bgHex = 0xFF070A10,
        cardHex = 0xFF151C28,
        borderHex = 0xFF28354A,
        primaryTextHex = 0xFFF8FAFC,
        secondaryTextHex = 0xFF94A3B8,
        accentHex = 0xFF38BDF8
    );

    val bgColor get() = Color(bgHex)
    val cardColor get() = Color(cardHex)
    val borderColor get() = Color(borderHex)
    val textPrimary get() = Color(primaryTextHex)
    val textSecondary get() = Color(secondaryTextHex)
    val accent get() = Color(accentHex)
}

// ============================================================================
// 2. ALPHA CHARACTER EVOLUTION & RPG SYSTEM
// ============================================================================
enum class AlphaCharacterStage(
    val stageNumber: Int,
    val title: String,
    val formName: String,
    val weaponOrAura: String,
    val perkDescription: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
) {
    STAGE_1_RECRUIT(
        1,
        "Alpha Recruit",
        "Spark Initiate",
        "Kinetic Gauntlets",
        "Awakening discipline. Every habit builds your core.",
        0xFF06B6D4,
        0xFF34D399
    ),
    STAGE_2_VANGUARD(
        2,
        "Iron Vanguard",
        "Armored Striker",
        "Cobalt Plasma Shield",
        "Unbreakable momentum. +15% Combat Power from streaks.",
        0xFF34D399,
        0xFF6366F1
    ),
    STAGE_3_RONIN(
        3,
        "Cyber Ronin",
        "Shadow Blade Master",
        "Twin Violet Katana",
        "Laser focus unlocked. Deep work slices through distraction.",
        0xFFA855F7,
        0xFF06B6D4
    ),
    STAGE_4_TITAN(
        4,
        "Solar Titan",
        "Golden Warlord",
        "Radiant Sun Wings",
        "Relentless execution. Commands the morning & evening.",
        0xFFF97316,
        0xFFFBBF24
    ),
    STAGE_5_SOVEREIGN(
        5,
        "Void Sovereign",
        "Cosmic Emperor",
        "Astral Lightning Halo",
        "Master of systems. Habits run on automatic instinct.",
        0xFFF43F5E,
        0xFFA855F7
    ),
    STAGE_6_APEX_GOD(
        6,
        "Apex Alpha God",
        "Transcendent Omnipotent",
        "Eternal Zenith Crown",
        "Peak human potential achieved. Unstoppable force.",
        0xFFFBBF24,
        0xFFF43F5E
    );

    val primaryColor get() = Color(primaryColorHex)
    val secondaryColor get() = Color(secondaryColorHex)

    companion object {
        fun forLevel(level: Int): AlphaCharacterStage = when {
            level <= 2 -> STAGE_1_RECRUIT
            level <= 4 -> STAGE_2_VANGUARD
            level <= 7 -> STAGE_3_RONIN
            level <= 11 -> STAGE_4_TITAN
            level <= 15 -> STAGE_5_SOVEREIGN
            else -> STAGE_6_APEX_GOD
        }
    }
}

@Immutable
data class AlphaRpgStats(
    val strength: Int,   // Driven by Fitness/Health habits
    val focus: Int,      // Driven by Career/Work habits & completed Tasks
    val willpower: Int,  // Driven by Streaks & Quit-Shields
    val wisdom: Int,     // Driven by Journal & Second-Brain Vault entries
    val combatPower: Int // Overall Alpha Power Rating
)

// ============================================================================
// 3. CORE DATA MODELS
// ============================================================================
enum class HabitType(val label: String) {
    CHECKLIST("Checklist"),
    MEASUREMENT("Measurement"),
    FOCUS_TIMER("Focus Timer"),
    QUIT_SHIELD("Quit / Abstain")
}

enum class TimeOfDay(val label: String, val icon: String) {
    ALL("All Day", "✨"),
    MORNING("Morning", "🌅"),
    AFTERNOON("Afternoon", "☀️"),
    EVENING("Evening", "🌙"),
    ANYTIME("Anytime", "⚡")
}

enum class HabitViewMode {
    STUDIO_CARDS,
    HEATMAP_GRID,
    COMPACT_WEEKLY,
    GRID_VIEW,
    WEEKLY_VIEW
}

@Immutable
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "⚡",
    val category: String = "Growth",
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    val habitStackingCue: String = "",
    val type: HabitType = HabitType.CHECKLIST,
    val colorHex: Long = 0xFF34D399,
    val targetValue: Float = 1f,
    val unit: String = "times",
    val stepSize: Float = 1f,
    val reminderTime: String = "",
    val logs: Map<String, Float> = emptyMap(),
    val notesPerDay: Map<String, String> = emptyMap(),
    val createdAt: String = LocalDate.now().toString()
) {
    val accentColor: Color get() = Color(colorHex)

    fun valueOn(date: LocalDate): Float = logs[date.toString()] ?: 0f

    fun isCompletedOn(date: LocalDate): Boolean {
        val v = valueOn(date)
        return if (type == HabitType.QUIT_SHIELD) v > 0f else v >= targetValue
    }

    fun progressOn(date: LocalDate): Float {
        if (type == HabitType.QUIT_SHIELD) return if (valueOn(date) > 0f) 1f else 0f
        if (targetValue <= 0f) return 0f
        return (valueOn(date) / targetValue).coerceIn(0f, 1f)
    }

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        var streak = 0
        var cursor = if (isCompletedOn(today)) today else today.minusDays(1)
        while (isCompletedOn(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(today: LocalDate = LocalDate.now()): Int {
        var best = 0
        var current = 0
        for (i in 180 downTo 0) {
            if (isCompletedOn(today.minusDays(i.toLong()))) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    fun completionRate30Days(today: LocalDate = LocalDate.now()): Int {
        var completed = 0
        for (i in 0 until 30) {
            if (isCompletedOn(today.minusDays(i.toLong()))) completed++
        }
        return ((completed / 30f) * 100f).toInt()
    }

    fun totalVolume(): Float = logs.values.filter { it > 0f }.sum()
    fun totalLoggedValue(): Float = totalVolume()
}

enum class TodoPriority(val label: String, val shortTag: String, val colorHex: Long, val xpReward: Int) {
    P1_CRITICAL("P1 • Critical", "P1", 0xFFF43F5E, 35),
    P2_HIGH("P2 • High", "P2", 0xFFF97316, 25),
    P3_MEDIUM("P3 • Medium", "P3", 0xFF06B6D4, 15),
    P4_LOW("P4 • Routine", "P4", 0xFF94A3B8, 10)
}

@Immutable
data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

@Immutable
data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "Personal",
    val priority: TodoPriority = TodoPriority.P2_HIGH,
    val date: String = LocalDate.now().toString(),
    val timeBlock: String = "Anytime",
    val reminderTime: String = "",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val estimatedMinutes: Int = 25,
    val subTasks: List<SubTask> = emptyList(),
    val notes: String = "",
    val note: String = ""
)

@Immutable
data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
    val isoDate: String = LocalDate.now().toString(),
    val title: String = "Daily Sanctuary Reflection",
    val moodEmoji: String = "🌟",
    val energyScore: Int = 8,
    val gratitudeOrWins: String = "",
    val dailyStory: String = "",
    val frictionOrMistakes: String = "",
    val actionItems: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class NoteCategory(val label: String) {
    BOOK_SUMMARY("Book Summary"),
    SECOND_BRAIN("Mental Model / Note"),
    PROJECT_PLAN("Blueprint"),
    GENERAL_NOTE("General Note")
}

@Immutable
data class FlashcardItem(
    val question: String,
    val answer: String
)

@Immutable
data class NotebookEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: NoteCategory = NoteCategory.BOOK_SUMMARY,
    val title: String,
    val authorOrSubtitle: String = "",
    val rating: Int = 5,
    val readingProgress: Int = 100,
    val isPinned: Boolean = false,
    val coverColorHex: Long = 0xFFA855F7,
    val favoriteQuote: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val detailedNotes: String = "",
    val flashcards: List<FlashcardItem> = emptyList(),
    val updatedAt: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
)

// ============================================================================
// 4. SYNTHESIZED SOUND ENGINE
// ============================================================================
object CelebrationSoundEngine {

    fun playCelebrationFanfare(context: Context, isLevelUp: Boolean = false) {
        triggerHaptics(context, isLevelUp)
        val notes = if (isLevelUp) {
            listOf(
                Triple(523.25, 0.00, 0.40),
                Triple(659.25, 0.08, 0.40),
                Triple(783.99, 0.16, 0.45),
                Triple(1046.50, 0.24, 0.65),
                Triple(1318.51, 0.34, 0.75),
                Triple(1567.98, 0.44, 0.90)
            )
        } else {
            listOf(
                Triple(587.33, 0.00, 0.35),
                Triple(739.99, 0.08, 0.40),
                Triple(880.00, 0.16, 0.45),
                Triple(1174.66, 0.24, 0.75)
            )
        }
        playSynthesizedSequence(notes, durationSeconds = if (isLevelUp) 1.4 else 1.05)
    }

    fun playMotivationalReminderSound(context: Context) {
        triggerHaptics(context, isLevelUp = true)
        val motivationalChord = listOf(
            Triple(440.00, 0.00, 0.55),
            Triple(554.37, 0.12, 0.55),
            Triple(659.25, 0.24, 0.60),
            Triple(880.00, 0.36, 0.90),
            Triple(1108.73, 0.50, 0.95)
        )
        playSynthesizedSequence(motivationalChord, durationSeconds = 1.45)
    }

    private fun playSynthesizedSequence(
        notes: List<Triple<Double, Double, Double>>,
        durationSeconds: Double
    ) {
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 24000
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0
                    for ((freq, start, dur) in notes) {
                        if (t >= start && t <= start + dur) {
                            val localT = t - start
                            val attack = (localT / 0.015).coerceAtMost(1.0)
                            val decay = exp(-3.5 * localT)
                            val wave = sin(2.0 * PI * freq * localT) +
                                    0.32 * sin(2.0 * PI * freq * 2.0 * localT)
                            sample += wave * attack * decay * 0.25
                        }
                    }
                    buffer[i] = (sample.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
                }

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((durationSeconds * 1000).toLong() + 100)
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    private fun triggerHaptics(context: Context, isLevelUp: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val timings = if (isLevelUp) longArrayOf(0, 40, 40, 60, 40, 110) else longArrayOf(0, 35, 40, 70)
            val amps = if (isLevelUp) intArrayOf(0, 160, 0, 200, 0, 255) else intArrayOf(0, 150, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } catch (_: Exception) {
        }
    }
}

// ============================================================================
// 5. SCHEDULED MOTIVATIONAL NOTIFICATIONS
// ============================================================================
object ReminderScheduler {
    private val motivationalQuotes = listOf(
        "Alpha mindset: Small daily disciplines compound into massive victories! 🔥",
        "Your character is evolving—complete your mission and claim your XP! ⚡",
        "Action cures resistance. Start for just 2 minutes! 🚀",
        "Alphas don't wait for mood—they execute their systems! 🏆"
    )

    fun scheduleReminder(context: Context, itemId: String, title: String, timeStr: String, isHabit: Boolean) {
        if (timeStr.isBlank() || !timeStr.contains(":")) {
            cancelReminder(context, itemId)
            return
        }
        try {
            val parts = timeStr.split(":")
            val hour = parts[0].trim().toInt().coerceIn(0, 23)
            val minute = parts[1].trim().toInt().coerceIn(0, 59)

            var targetDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
            if (targetDateTime.isBefore(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, MotivationReminderReceiver::class.java).apply {
                putExtra("EXTRA_ID", itemId)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_TIME", timeStr)
                putExtra("EXTRA_IS_HABIT", isHabit)
                putExtra("EXTRA_QUOTE", motivationalQuotes.random())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: Exception) {
        }
    }

    fun cancelReminder(context: Context, itemId: String) {
        try {
            val intent = Intent(context, MotivationReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }
}

class MotivationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("EXTRA_ID") ?: UUID.randomUUID().toString()
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Time to Execute!"
        val quote = intent.getStringExtra("EXTRA_QUOTE") ?: "Keep your streak alive today! 🔥"
        val timeStr = intent.getStringExtra("EXTRA_TIME") ?: ""
        val isHabit = intent.getBooleanExtra("EXTRA_IS_HABIT", true)

        CelebrationSoundEngine.playMotivationalReminderSound(context)

        val channelId = "alpha_motivation_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alpha Habit & Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom scheduled reminders with motivational sound"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (isHabit) "⚡ Alpha Habit: $title" else "🎯 Alpha Mission: $title")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$quote\nTap to open Alpha Studio and power up your character!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        nm.notify(id.hashCode(), notification)

        if (isHabit && timeStr.isNotBlank()) {
            ReminderScheduler.scheduleReminder(context, id, title, timeStr, true)
        }
    }
}

// ============================================================================
// 6. DUAL INTERACTIVE HOME SCREEN WIDGETS (1: HABITS WIDGET, 2: TASKS WIDGET)
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
            val toggledTitle = storage.toggleHabitDirectlyFromWidget(habitId)
            if (toggledTitle != null) {
                CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = false)
                Toast.makeText(context, "⚡ $toggledTitle Complete! +30 XP to Alpha!", Toast.LENGTH_SHORT).show()
            }
            updateAllWidgets(context)
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

                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openPending = PendingIntent.getActivity(
                        context, 0, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_header_title, openPending)

                    for (i in 0 until 4) {
                        val (rowId, titleId, btnId) = rowIds[i]
                        if (i < habits.size) {
                            val h = habits[i]
                            val isDone = h.isCompletedOn(today)
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(
                                titleId,
                                "${h.emoji} ${h.title}  •  🔥${h.currentStreak(today)}d"
                            )
                            views.setTextViewText(btnId, if (isDone) "✓" else "+")
                            views.setInt(
                                btnId,
                                "setBackgroundResource",
                                if (isDone) R.drawable.widget_btn_done else R.drawable.widget_btn_pending
                            )
                            views.setTextColor(
                                btnId,
                                if (isDone) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#34D399")
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

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllTaskWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
            val storage = StudioStorageManager(context)
            val completedTitle = storage.toggleTodoDirectlyFromWidget(taskId)
            if (completedTitle != null) {
                CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = false)
                Toast.makeText(context, "🎯 Mission Complete: $completedTitle (+25 XP)", Toast.LENGTH_SHORT).show()
            }
            updateAllTaskWidgets(context)
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
                val allTodos = storage.loadTodosOnly()
                    .sortedWith(compareBy<TodoTask> { it.isCompleted }.thenBy { it.priority.ordinal })
                val pendingCount = allTodos.count { !it.isCompleted }

                val rowIds = listOf(
                    Triple(R.id.task_widget_row_1, R.id.task_widget_title_1, R.id.task_widget_btn_1),
                    Triple(R.id.task_widget_row_2, R.id.task_widget_title_2, R.id.task_widget_btn_2),
                    Triple(R.id.task_widget_row_3, R.id.task_widget_title_3, R.id.task_widget_btn_3),
                    Triple(R.id.task_widget_row_4, R.id.task_widget_title_4, R.id.task_widget_btn_4)
                )

                for (widgetId in ids) {
                    val views = RemoteViews(context.packageName, R.layout.task_widget_layout)
                    views.setTextViewText(R.id.task_widget_header_score, "$pendingCount Active")

                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openPending = PendingIntent.getActivity(
                        context, 1, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.task_widget_header_title, openPending)

                    for (i in 0 until 4) {
                        val (rowId, titleId, btnId) = rowIds[i]
                        if (i < allTodos.size) {
                            val t = allTodos[i]
                            views.setViewVisibility(rowId, View.VISIBLE)
                            val prefix = if (t.isCompleted) "✅" else "[${t.priority.shortTag}]"
                            views.setTextViewText(titleId, "$prefix ${t.title}")
                            views.setTextViewText(btnId, "✓")
                            views.setInt(
                                btnId,
                                "setBackgroundResource",
                                if (t.isCompleted) R.drawable.widget_btn_done else R.drawable.widget_btn_pending
                            )
                            views.setTextColor(
                                btnId,
                                if (t.isCompleted) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#34D399")
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

// ============================================================================
// 7. LOCAL PERSISTENCE & GOOGLE DRIVE / CLOUD BACKUP ENGINE
// ============================================================================
class StudioStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("aura_studio_vault_v2", Context.MODE_PRIVATE)

    fun saveFullState(
        xp: Int,
        streakShields: Int,
        googleEmail: String?,
        autoBackup: Boolean,
        readingTheme: SanctuaryReadingTheme = SanctuaryReadingTheme.WARM_SEPIA,
        readingFontScale: Float = 1.0f,
        habits: List<Habit>,
        todos: List<TodoTask>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): String {
        val root = serializeToJson(xp, streakShields, habits, todos, journals, notes)
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        prefs.edit()
            .putString("master_state_json", root.toString())
            .putString("google_email", googleEmail)
            .putBoolean("auto_backup", autoBackup)
            .putString("reading_theme", readingTheme.name)
            .putFloat("reading_font_scale", readingFontScale)
            .putString("last_saved_time", nowFormatted)
            .apply()
        HabitWidgetProvider.updateAllWidgets(context)
        TaskWidgetProvider.updateAllTaskWidgets(context)
        return root.toString(2)
    }

    fun getSavedJson(): String? = prefs.getString("master_state_json", null)
    fun getGoogleEmail(): String? = prefs.getString("google_email", null)
    fun getLastBackupTime(): String = prefs.getString("last_saved_time", "Synced just now") ?: "Synced just now"
    fun isAutoBackup(): Boolean = prefs.getBoolean("auto_backup", true)
    fun getReadingTheme(): SanctuaryReadingTheme =
        runCatching { SanctuaryReadingTheme.valueOf(prefs.getString("reading_theme", "WARM_SEPIA")!!) }
            .getOrDefault(SanctuaryReadingTheme.WARM_SEPIA)
    fun getReadingFontScale(): Float = prefs.getFloat("reading_font_scale", 1.0f)

    fun loadHabitsOnly(): List<Habit> {
        val jsonStr = getSavedJson() ?: return emptyList()
        val result = mutableListOf<Habit>()
        try {
            val root = JSONObject(jsonStr)
            val habitsArr = root.optJSONArray("habits") ?: JSONArray()
            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                val logsMap = mutableMapOf<String, Float>()
                val logsObj = o.optJSONObject("logs")
                logsObj?.keys()?.forEach { k -> logsMap[k] = logsObj.optDouble(k, 0.0).toFloat() }
                result.add(
                    Habit(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        emoji = o.optString("emoji", "⚡"),
                        category = o.optString("category", "Growth"),
                        timeOfDay = runCatching { TimeOfDay.valueOf(o.optString("timeOfDay")) }.getOrDefault(TimeOfDay.ANYTIME),
                        habitStackingCue = o.optString("habitStackingCue", ""),
                        type = runCatching { HabitType.valueOf(o.optString("type")) }.getOrDefault(HabitType.CHECKLIST),
                        colorHex = o.optLong("colorHex", 0xFF34D399),
                        targetValue = o.optDouble("targetValue", 1.0).toFloat(),
                        unit = o.optString("unit", "times"),
                        stepSize = o.optDouble("stepSize", 1.0).toFloat(),
                        reminderTime = o.optString("reminderTime", ""),
                        logs = logsMap
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun loadTodosOnly(): List<TodoTask> {
        val jsonStr = getSavedJson() ?: return emptyList()
        val result = mutableListOf<TodoTask>()
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: JSONArray()
            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                result.add(
                    TodoTask(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category", "Personal"),
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }.getOrDefault(TodoPriority.P2_HIGH),
                        date = o.optString("date", LocalDate.now().toString()),
                        reminderTime = o.optString("reminderTime", ""),
                        isCompleted = o.optBoolean("isCompleted", false),
                        isRecurring = o.optBoolean("isRecurring", false),
                        estimatedMinutes = o.optInt("estimatedMinutes", 25)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun toggleHabitDirectlyFromWidget(habitId: String): String? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todayKey = LocalDate.now().toString()
            val habitsArr = root.optJSONArray("habits") ?: return null
            var completedHabitTitle: String? = null

            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                if (o.optString("id") == habitId) {
                    val target = o.optDouble("targetValue", 1.0).toFloat()
                    val step = o.optDouble("stepSize", 1.0).toFloat()
                    val type = o.optString("type", "CHECKLIST")
                    val logsObj = o.optJSONObject("logs") ?: JSONObject()
                    val currentVal = logsObj.optDouble(todayKey, 0.0).toFloat()

                    if (type == "CHECKLIST" || type == "QUIT_SHIELD") {
                        if (currentVal >= target) {
                            logsObj.remove(todayKey)
                        } else {
                            logsObj.put(todayKey, target.toDouble())
                            completedHabitTitle = o.optString("title")
                        }
                    } else {
                        val nextVal = currentVal + step
                        logsObj.put(todayKey, nextVal.toDouble())
                        if (currentVal < target && nextVal >= target) {
                            completedHabitTitle = o.optString("title")
                        }
                    }
                    o.put("logs", logsObj)
                    break
                }
            }

            if (completedHabitTitle != null) {
                root.put("xp", root.optInt("xp", 420) + 30)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return completedHabitTitle
        } catch (_: Exception) {
            return null
        }
    }

    fun toggleTodoDirectlyFromWidget(taskId: String): String? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: return null
            var completedTitle: String? = null

            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                if (o.optString("id") == taskId) {
                    val nowDone = !o.optBoolean("isCompleted", false)
                    o.put("isCompleted", nowDone)
                    if (nowDone) completedTitle = o.optString("title")
                    break
                }
            }

            if (completedTitle != null) {
                root.put("xp", root.optInt("xp", 420) + 25)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return completedTitle
        } catch (_: Exception) {
            return null
        }
    }

    fun exportBackupToUri(uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun serializeToJson(
        xp: Int,
        streakShields: Int,
        habits: List<Habit>,
        todos: List<TodoTask>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 4)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("xp", xp)
        root.put("streakShields", streakShields)

        val habitsArr = JSONArray()
        habits.forEach { h ->
            habitsArr.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("emoji", h.emoji)
                put("category", h.category)
                put("timeOfDay", h.timeOfDay.name)
                put("habitStackingCue", h.habitStackingCue)
                put("type", h.type.name)
                put("colorHex", h.colorHex)
                put("targetValue", h.targetValue.toDouble())
                put("unit", h.unit)
                put("stepSize", h.stepSize.toDouble())
                put("reminderTime", h.reminderTime)
                val logsObj = JSONObject()
                h.logs.forEach { (k, v) -> logsObj.put(k, v.toDouble()) }
                put("logs", logsObj)
            })
        }
        root.put("habits", habitsArr)

        val todosArr = JSONArray()
        todos.forEach { t ->
            val subArr = JSONArray()
            t.subTasks.forEach { s ->
                subArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("isDone", s.isDone)
                })
            }
            todosArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("category", t.category)
                put("priority", t.priority.name)
                put("date", t.date)
                put("timeBlock", t.timeBlock)
                put("reminderTime", t.reminderTime)
                put("isCompleted", t.isCompleted)
                put("isRecurring", t.isRecurring)
                put("estimatedMinutes", t.estimatedMinutes)
                put("subTasks", subArr)
                put("notes", t.notes)
            })
        }
        root.put("todos", todosArr)

        val journalsArr = JSONArray()
        journals.forEach { j ->
            journalsArr.put(JSONObject().apply {
                put("id", j.id)
                put("date", j.date)
                put("isoDate", j.isoDate)
                put("title", j.title)
                put("moodEmoji", j.moodEmoji)
                put("energyScore", j.energyScore)
                put("gratitudeOrWins", j.gratitudeOrWins)
                put("dailyStory", j.dailyStory)
                put("frictionOrMistakes", j.frictionOrMistakes)
                put("actionItems", JSONArray(j.actionItems))
                put("photoUris", JSONArray(j.photoUris))
                put("tags", JSONArray(j.tags))
            })
        }
        root.put("journals", journalsArr)

        val notesArr = JSONArray()
        notes.forEach { n ->
            val fcArr = JSONArray()
            n.flashcards.forEach { f ->
                fcArr.put(JSONObject().apply {
                    put("question", f.question)
                    put("answer", f.answer)
                })
            }
            notesArr.put(JSONObject().apply {
                put("id", n.id)
                put("category", n.category.name)
                put("title", n.title)
                put("authorOrSubtitle", n.authorOrSubtitle)
                put("rating", n.rating)
                put("readingProgress", n.readingProgress)
                put("isPinned", n.isPinned)
                put("coverColorHex", n.coverColorHex)
                put("favoriteQuote", n.favoriteQuote)
                put("keyTakeaways", JSONArray(n.keyTakeaways))
                put("detailedNotes", n.detailedNotes)
                put("flashcards", fcArr)
                put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notesArr)
        return root
    }
}

// ============================================================================
// 8. ALPHA STUDIO VIEWMODEL (Character Evolution, Daily Boss, RPG Stats, & Sync)
// ============================================================================
class HabitTrackerViewModel : ViewModel() {

    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()

    val selectedTimeOfDay = mutableStateOf(TimeOfDay.ALL)
    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.STUDIO_CARDS)
    val selectedDate = mutableStateOf(LocalDate.now())

    val readingTheme = mutableStateOf(SanctuaryReadingTheme.WARM_SEPIA)
    val readingFontScale = mutableFloatStateOf(1.0f)

    val userXp = mutableIntStateOf(460)
    val streakShields = mutableIntStateOf(3)

    val userLevel: Int get() = (userXp.intValue / 200) + 1
    val xpProgressInLevel: Float get() = (userXp.intValue % 200) / 200f
    val characterStage: AlphaCharacterStage get() = AlphaCharacterStage.forLevel(userLevel)
    val userRankTitle: String get() = characterStage.title

    // Dynamic RPG Stats Computed from Real User Activity
    fun computeAlphaStats(): AlphaRpgStats {
        val fitnessLogs = habits.filter { it.category.contains("Fitness", true) || it.category.contains("Health", true) }
            .sumOf { it.logs.size }
        val focusLogs = habits.filter { it.type == HabitType.FOCUS_TIMER || it.category.contains("Career", true) || it.category.contains("Work", true) }
            .sumOf { it.logs.size } + todos.count { it.isCompleted } * 2
        val maxStreak = habits.maxOfOrNull { it.bestStreak() } ?: 1
        val knowledgeCount = (journals.size * 4) + (notes.size * 6)

        val str = 25 + fitnessLogs * 2 + userLevel * 4
        val foc = 25 + focusLogs * 2 + userLevel * 4
        val wil = 25 + maxStreak * 3 + streakShields.intValue * 5 + userLevel * 4
        val wis = 25 + knowledgeCount + userLevel * 4
        val cp = (str + foc + wil + wis) * 12 + userXp.intValue
        return AlphaRpgStats(str, foc, wil, wis, cp)
    }

    // Daily Procrastination Boss HP (0f..1f remaining HP)
    fun dailyBossHpFraction(date: LocalDate = LocalDate.now()): Float {
        val totalHabits = habits.size.coerceAtLeast(1)
        val doneHabits = habits.count { it.isCompletedOn(date) }
        val doneTodos = todos.count { it.isCompleted }.coerceAtMost(3)
        val damageRatio = ((doneHabits.toFloat() + doneTodos * 0.35f) / totalHabits.toFloat()).coerceIn(0f, 1f)
        return (1f - damageRatio).coerceIn(0f, 1f)
    }

    val activeFocusTitle = mutableStateOf("Deep Focus Session")
    val activeFocusHabitId = mutableStateOf<String?>(null)
    val focusRemainingSeconds = mutableLongStateOf(25 * 60L)
    val focusTotalSeconds = mutableLongStateOf(25 * 60L)
    val isFocusTimerRunning = mutableStateOf(false)
    private var timerJob: Job? = null

    val showCelebration = mutableStateOf(false)
    val celebrationTitle = mutableStateOf("")
    val celebrationSubtitle = mutableStateOf("")
    val celebrationXpEarned = mutableIntStateOf(25)

    val connectedGoogleEmail = mutableStateOf<String?>(null)
    val lastCloudBackup = mutableStateOf("Auto-saved locally")
    val autoCloudSync = mutableStateOf(true)
    val isSyncingCloud = mutableStateOf(false)

    private var isInitialized = false

    fun initializeOrRefreshFromDisk(context: Context) {
        val storage = StudioStorageManager(context)
        connectedGoogleEmail.value = storage.getGoogleEmail()
        lastCloudBackup.value = storage.getLastBackupTime()
        autoCloudSync.value = storage.isAutoBackup()
        readingTheme.value = storage.getReadingTheme()
        readingFontScale.floatValue = storage.getReadingFontScale()

        val savedJson = storage.getSavedJson()
        if (!savedJson.isNullOrBlank()) {
            restoreFromJsonString(savedJson)
            isInitialized = true
        } else if (!isInitialized) {
            isInitialized = true
            seedStarterTemplates()
            persistState(context)
        }
    }

    fun initializeIfNeeded(context: Context) = initializeOrRefreshFromDisk(context)

    fun persistState(context: Context): String {
        val storage = StudioStorageManager(context)
        val json = storage.saveFullState(
            xp = userXp.intValue,
            streakShields = streakShields.intValue,
            googleEmail = connectedGoogleEmail.value,
            autoBackup = autoCloudSync.value,
            readingTheme = readingTheme.value,
            readingFontScale = readingFontScale.floatValue,
            habits = habits,
            todos = todos,
            journals = journals,
            notes = notes
        )
        lastCloudBackup.value = storage.getLastBackupTime()
        return json
    }

    fun restoreFromJsonString(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            userXp.intValue = root.optInt("xp", 460)
            streakShields.intValue = root.optInt("streakShields", 3)

            habits.clear()
            val habitsArr = root.optJSONArray("habits") ?: JSONArray()
            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                val logsMap = mutableMapOf<String, Float>()
                val logsObj = o.optJSONObject("logs")
                logsObj?.keys()?.forEach { k -> logsMap[k] = logsObj.optDouble(k, 0.0).toFloat() }
                habits.add(
                    Habit(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title"),
                        emoji = o.optString("emoji", "⚡"),
                        category = o.optString("category", "Growth"),
                        timeOfDay = runCatching { TimeOfDay.valueOf(o.optString("timeOfDay")) }.getOrDefault(TimeOfDay.ANYTIME),
                        habitStackingCue = o.optString("habitStackingCue", ""),
                        type = runCatching { HabitType.valueOf(o.optString("type")) }.getOrDefault(HabitType.CHECKLIST),
                        colorHex = o.optLong("colorHex", 0xFF34D399),
                        targetValue = o.optDouble("targetValue", 1.0).toFloat(),
                        unit = o.optString("unit", "times"),
                        stepSize = o.optDouble("stepSize", 1.0).toFloat(),
                        reminderTime = o.optString("reminderTime", ""),
                        logs = logsMap
                    )
                )
            }

            todos.clear()
            val todosArr = root.optJSONArray("todos") ?: JSONArray()
            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                val subs = mutableListOf<SubTask>()
                val subArr = o.optJSONArray("subTasks") ?: JSONArray()
                for (j in 0 until subArr.length()) {
                    val s = subArr.getJSONObject(j)
                    subs.add(SubTask(s.optString("id"), s.optString("title"), s.optBoolean("isDone")))
                }
                todos.add(
                    TodoTask(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title"),
                        category = o.optString("category", "Personal"),
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }.getOrDefault(TodoPriority.P2_HIGH),
                        date = o.optString("date", LocalDate.now().toString()),
                        timeBlock = o.optString("timeBlock", "Anytime"),
                        reminderTime = o.optString("reminderTime", ""),
                        isCompleted = o.optBoolean("isCompleted", false),
                        isRecurring = o.optBoolean("isRecurring", false),
                        estimatedMinutes = o.optInt("estimatedMinutes", 25),
                        subTasks = subs,
                        notes = o.optString("notes", "")
                    )
                )
            }

            journals.clear()
            val jArr = root.optJSONArray("journals") ?: JSONArray()
            for (i in 0 until jArr.length()) {
                val o = jArr.getJSONObject(i)
                val actions = mutableListOf<String>()
                o.optJSONArray("actionItems")?.let { arr ->
                    for (k in 0 until arr.length()) actions.add(arr.optString(k))
                }
                val photos = mutableListOf<String>()
                o.optJSONArray("photoUris")?.let { arr ->
                    for (k in 0 until arr.length()) photos.add(arr.optString(k))
                }
                val tags = mutableListOf<String>()
                o.optJSONArray("tags")?.let { arr ->
                    for (k in 0 until arr.length()) tags.add(arr.optString(k))
                }
                journals.add(
                    JournalEntry(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        date = o.optString("date"),
                        isoDate = o.optString("isoDate", LocalDate.now().toString()),
                        title = o.optString("title", "Daily Reflection"),
                        moodEmoji = o.optString("moodEmoji", "🌟"),
                        energyScore = o.optInt("energyScore", 8),
                        gratitudeOrWins = o.optString("gratitudeOrWins", ""),
                        dailyStory = o.optString("dailyStory", ""),
                        frictionOrMistakes = o.optString("frictionOrMistakes", ""),
                        actionItems = actions,
                        photoUris = photos,
                        tags = tags
                    )
                )
            }

            notes.clear()
            val nArr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until nArr.length()) {
                val o = nArr.getJSONObject(i)
                val takeaways = mutableListOf<String>()
                o.optJSONArray("keyTakeaways")?.let { arr ->
                    for (k in 0 until arr.length()) takeaways.add(arr.optString(k))
                }
                val flashcards = mutableListOf<FlashcardItem>()
                o.optJSONArray("flashcards")?.let { arr ->
                    for (k in 0 until arr.length()) {
                        val f = arr.getJSONObject(k)
                        flashcards.add(FlashcardItem(f.optString("question"), f.optString("answer")))
                    }
                }
                notes.add(
                    NotebookEntry(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        category = runCatching { NoteCategory.valueOf(o.optString("category")) }.getOrDefault(NoteCategory.BOOK_SUMMARY),
                        title = o.optString("title"),
                        authorOrSubtitle = o.optString("authorOrSubtitle"),
                        rating = o.optInt("rating", 5),
                        readingProgress = o.optInt("readingProgress", 100),
                        isPinned = o.optBoolean("isPinned", false),
                        coverColorHex = o.optLong("coverColorHex", 0xFFA855F7),
                        favoriteQuote = o.optString("favoriteQuote", ""),
                        keyTakeaways = takeaways,
                        detailedNotes = o.optString("detailedNotes", ""),
                        flashcards = flashcards,
                        updatedAt = o.optString("updatedAt", "")
                    )
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun awardXpAndCelebrate(
        context: Context,
        title: String,
        subtitle: String,
        xpEarned: Int = 25
    ) {
        val oldLevel = userLevel
        val oldStage = characterStage
        userXp.intValue += xpEarned
        val newLevel = userLevel
        val newStage = characterStage
        val leveledUp = newLevel > oldLevel
        val evolved = newStage != oldStage

        if (leveledUp) streakShields.intValue += 1

        celebrationTitle.value = when {
            evolved -> "⚡ ALPHA EVOLUTION: ${newStage.formName.uppercase()}!"
            leveledUp -> "🎉 LEVEL $newLevel UNLOCKED!"
            else -> title
        }
        celebrationSubtitle.value = when {
            evolved -> "Equipped ${newStage.weaponOrAura} • +1 Streak Shield!"
            leveledUp -> "Rank: $userRankTitle • Character Power Surged!"
            else -> subtitle
        }
        celebrationXpEarned.intValue = xpEarned
        showCelebration.value = true

        CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = leveledUp)
        persistState(context)

        viewModelScope.launch {
            delay(3300)
            showCelebration.value = false
        }
    }

    fun startFocusTimer(context: Context, title: String, minutes: Int, habitId: String? = null) {
        timerJob?.cancel()
        activeFocusTitle.value = title
        activeFocusHabitId.value = habitId
        focusTotalSeconds.longValue = minutes * 60L
        focusRemainingSeconds.longValue = minutes * 60L
        isFocusTimerRunning.value = true
        runTimerLoop(context)
    }

    fun togglePauseFocusTimer(context: Context) {
        if (isFocusTimerRunning.value) {
            isFocusTimerRunning.value = false
            timerJob?.cancel()
        } else {
            isFocusTimerRunning.value = true
            runTimerLoop(context)
        }
    }

    fun stopFocusTimer() {
        isFocusTimerRunning.value = false
        timerJob?.cancel()
        focusRemainingSeconds.longValue = focusTotalSeconds.longValue
    }

    private fun runTimerLoop(context: Context) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isFocusTimerRunning.value && focusRemainingSeconds.longValue > 0) {
                delay(1000L)
                focusRemainingSeconds.longValue -= 1L
            }
            if (focusRemainingSeconds.longValue <= 0L) {
                isFocusTimerRunning.value = false
                val mins = (focusTotalSeconds.longValue / 60f).coerceAtLeast(1f)
                activeFocusHabitId.value?.let { id ->
                    habits.find { it.id == id }?.let { h ->
                        setHabitMeasurement(context, h, h.valueOn(LocalDate.now()) + mins)
                    }
                }
                awardXpAndCelebrate(
                    context = context,
                    title = "⏱️ Alpha Focus Complete!",
                    subtitle = "${activeFocusTitle.value} finished • +FOC Stat!",
                    xpEarned = 40
                )
            }
        }
    }

    fun toggleOrStepHabit(context: Context, habit: Habit, date: LocalDate = selectedDate.value) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index == -1) return

        val key = date.toString()
        val currentVal = habit.logs[key] ?: 0f
        val updatedLogs = habit.logs.toMutableMap()
        val wasDoneBefore = habit.isCompletedOn(date)

        when (habit.type) {
            HabitType.CHECKLIST -> {
                if (currentVal >= habit.targetValue) updatedLogs.remove(key)
                else updatedLogs[key] = habit.targetValue
            }
            HabitType.MEASUREMENT, HabitType.FOCUS_TIMER -> {
                updatedLogs[key] = currentVal + habit.stepSize
            }
            HabitType.QUIT_SHIELD -> {
                when {
                    currentVal == 0f -> updatedLogs[key] = 1f
                    currentVal > 0f -> updatedLogs[key] = -1f
                    else -> updatedLogs.remove(key)
                }
            }
        }

        val updated = habit.copy(logs = updatedLogs)
        habits[index] = updated
        if (!wasDoneBefore && updated.isCompletedOn(date)) {
            awardXpAndCelebrate(
                context = context,
                title = "${updated.emoji} ${updated.title} Crushed!",
                subtitle = "🔥 ${updated.currentStreak()}-day streak • Alpha Character Strengthened!",
                xpEarned = 35
            )
        } else {
            persistState(context)
        }
    }

    fun toggleOrIncrementHabit(context: Context, habit: Habit, date: LocalDate = LocalDate.now()) =
        toggleOrStepHabit(context, habit, date)

    fun setHabitMeasurement(context: Context, habit: Habit, newAmount: Float, date: LocalDate = selectedDate.value) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index == -1) return
        val wasDone = habit.isCompletedOn(date)
        val updatedLogs = habit.logs.toMutableMap()
        if (newAmount <= 0f) updatedLogs.remove(date.toString())
        else updatedLogs[date.toString()] = newAmount

        val updated = habit.copy(logs = updatedLogs)
        habits[index] = updated
        if (!wasDone && updated.isCompletedOn(date)) {
            awardXpAndCelebrate(
                context = context,
                title = "${updated.emoji} Target Reached!",
                subtitle = "$newAmount ${updated.unit} logged for ${updated.title}",
                xpEarned = 35
            )
        } else {
            persistState(context)
        }
    }

    fun saveOrUpdateHabit(
        context: Context,
        existingId: String?,
        title: String,
        emoji: String,
        category: String,
        timeOfDay: TimeOfDay,
        cue: String,
        type: HabitType,
        colorHex: Long,
        targetValue: Float,
        unit: String,
        stepSize: Float,
        reminderTime: String
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        if (existingId != null) {
            val idx = habits.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                val updated = habits[idx].copy(
                    title = cleanTitle,
                    emoji = emoji.ifBlank { "⚡" },
                    category = category.ifBlank { "Growth" },
                    timeOfDay = timeOfDay,
                    habitStackingCue = cue.trim(),
                    type = type,
                    colorHex = colorHex,
                    targetValue = targetValue.coerceAtLeast(0.1f),
                    unit = unit.ifBlank { "times" },
                    stepSize = stepSize.coerceAtLeast(0.1f),
                    reminderTime = reminderTime.trim()
                )
                habits[idx] = updated
                ReminderScheduler.scheduleReminder(context, updated.id, updated.title, updated.reminderTime, true)
                persistState(context)
                return
            }
        }

        val newHabit = Habit(
            title = cleanTitle,
            emoji = emoji.ifBlank { "⚡" },
            category = category.ifBlank { "Growth" },
            timeOfDay = timeOfDay,
            habitStackingCue = cue.trim(),
            type = type,
            colorHex = colorHex,
            targetValue = targetValue.coerceAtLeast(0.1f),
            unit = unit.ifBlank { "times" },
            stepSize = stepSize.coerceAtLeast(0.1f),
            reminderTime = reminderTime.trim()
        )
        habits.add(0, newHabit)
        ReminderScheduler.scheduleReminder(context, newHabit.id, newHabit.title, newHabit.reminderTime, true)
        persistState(context)
    }

    fun createHabit(
        context: Context,
        title: String,
        emoji: String,
        category: String,
        timeOfDay: TimeOfDay,
        cue: String,
        type: HabitType,
        colorHex: Long,
        targetValue: Float,
        unit: String,
        stepSize: Float
    ) = saveOrUpdateHabit(context, null, title, emoji, category, timeOfDay, cue, type, colorHex, targetValue, unit, stepSize, "")

    fun deleteHabit(context: Context, habitId: String) {
        ReminderScheduler.cancelReminder(context, habitId)
        habits.removeAll { it.id == habitId }
        persistState(context)
    }

    fun toggleTodo(context: Context, task: TodoTask) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val nowDone = !task.isCompleted
        todos[idx] = task.copy(isCompleted = nowDone)
        if (nowDone) {
            awardXpAndCelebrate(
                context = context,
                title = "✅ Mission Accomplished!",
                subtitle = "\"${task.title}\" completed • Boss Damaged!",
                xpEarned = task.priority.xpReward
            )
        } else {
            persistState(context)
        }
    }

    fun toggleSubTask(context: Context, task: TodoTask, subTaskId: String) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val updatedSubs = task.subTasks.map {
            if (it.id == subTaskId) it.copy(isDone = !it.isDone) else it
        }
        val allDone = updatedSubs.isNotEmpty() && updatedSubs.all { it.isDone }
        val wasDone = task.isCompleted
        todos[idx] = task.copy(subTasks = updatedSubs, isCompleted = allDone)
        if (!wasDone && allDone) {
            awardXpAndCelebrate(
                context = context,
                title = "✅ All Subtasks Complete!",
                subtitle = task.title,
                xpEarned = task.priority.xpReward
            )
        } else {
            persistState(context)
        }
    }

    fun saveOrUpdateTodoTask(
        context: Context,
        existingId: String?,
        title: String,
        category: String,
        priority: TodoPriority,
        reminderTime: String,
        estimatedMinutes: Int,
        subTaskTitles: List<String>,
        isRecurring: Boolean
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        if (existingId != null) {
            val idx = todos.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                val oldSubs = todos[idx].subTasks.associateBy { it.title }
                val mergedSubs = subTaskTitles.filter { it.isNotBlank() }.map { line ->
                    val t = line.trim()
                    oldSubs[t] ?: SubTask(title = t)
                }
                val updated = todos[idx].copy(
                    title = cleanTitle,
                    category = category.ifBlank { "Personal" },
                    priority = priority,
                    reminderTime = reminderTime.trim(),
                    estimatedMinutes = estimatedMinutes,
                    subTasks = mergedSubs,
                    isRecurring = isRecurring
                )
                todos[idx] = updated
                ReminderScheduler.scheduleReminder(context, updated.id, updated.title, updated.reminderTime, false)
                persistState(context)
                return
            }
        }

        val newTask = TodoTask(
            title = cleanTitle,
            category = category.ifBlank { "Personal" },
            priority = priority,
            reminderTime = reminderTime.trim(),
            estimatedMinutes = estimatedMinutes,
            subTasks = subTaskTitles.filter { it.isNotBlank() }.map { SubTask(title = it.trim()) },
            isRecurring = isRecurring
        )
        todos.add(0, newTask)
        ReminderScheduler.scheduleReminder(context, newTask.id, newTask.title, newTask.reminderTime, false)
        persistState(context)
    }

    fun addTodoTask(
        context: Context,
        title: String,
        category: String,
        priority: TodoPriority,
        timeBlock: String,
        estimatedMinutes: Int,
        subTaskTitles: List<String>,
        isRecurring: Boolean
    ) = saveOrUpdateTodoTask(context, null, title, category, priority, "", estimatedMinutes, subTaskTitles, isRecurring)

    fun deleteTodoTask(context: Context, id: String) {
        ReminderScheduler.cancelReminder(context, id)
        todos.removeAll { it.id == id }
        persistState(context)
    }

    fun saveOrUpdateJournalEntry(
        context: Context,
        existingId: String?,
        title: String,
        moodEmoji: String,
        energyScore: Int,
        wins: String,
        story: String,
        mistakes: String,
        actions: List<String>,
        photos: List<String>,
        tags: List<String>,
        pushActionsToTodo: Boolean
    ) {
        photos.forEach { uriStr ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }

        if (existingId != null) {
            val idx = journals.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                journals[idx] = journals[idx].copy(
                    title = title.ifBlank { "Daily Reflection" },
                    moodEmoji = moodEmoji,
                    energyScore = energyScore,
                    gratitudeOrWins = wins,
                    dailyStory = story,
                    frictionOrMistakes = mistakes,
                    actionItems = actions,
                    photoUris = photos,
                    tags = tags
                )
                persistState(context)
                return
            }
        }

        journals.add(
            0,
            JournalEntry(
                title = title.ifBlank { "Daily Reflection" },
                moodEmoji = moodEmoji,
                energyScore = energyScore,
                gratitudeOrWins = wins,
                dailyStory = story,
                frictionOrMistakes = mistakes,
                actionItems = actions,
                photoUris = photos,
                tags = tags
            )
        )

        if (pushActionsToTodo) {
            actions.filter { it.isNotBlank() }.forEach { act ->
                todos.add(
                    0,
                    TodoTask(
                        title = act.trim(),
                        category = "Reflection Action",
                        priority = TodoPriority.P1_CRITICAL
                    )
                )
            }
        }

        awardXpAndCelebrate(
            context = context,
            title = "📓 Sanctuary Reflection Saved!",
            subtitle = "+WIS Stat • Self-awareness compounds into wisdom",
            xpEarned = 35
        )
    }

    fun addJournalEntry(
        context: Context,
        title: String,
        moodEmoji: String,
        energyScore: Int,
        wins: String,
        story: String,
        mistakes: String,
        actions: List<String>,
        photos: List<String>,
        tags: List<String>,
        pushActionsToTodo: Boolean
    ) = saveOrUpdateJournalEntry(context, null, title, moodEmoji, energyScore, wins, story, mistakes, actions, photos, tags, pushActionsToTodo)

    fun deleteJournalEntry(context: Context, id: String) {
        journals.removeAll { it.id == id }
        persistState(context)
    }

    fun saveOrUpdateNotebookEntry(
        context: Context,
        existingId: String?,
        entry: NotebookEntry
    ) {
        if (existingId != null) {
            val idx = notes.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                notes[idx] = entry.copy(id = existingId)
                persistState(context)
                return
            }
        }
        notes.add(0, entry)
        awardXpAndCelebrate(
            context = context,
            title = "🧠 Saved to Knowledge Vault!",
            subtitle = "+WIS Stat • \"${entry.title}\" indexed in your Second Brain",
            xpEarned = 35
        )
    }

    fun addNotebookEntry(context: Context, entry: NotebookEntry) =
        saveOrUpdateNotebookEntry(context, null, entry)

    fun togglePinNote(context: Context, id: String) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx != -1) {
            notes[idx] = notes[idx].copy(isPinned = !notes[idx].isPinned)
            persistState(context)
        }
    }

    fun deleteNotebookEntry(context: Context, id: String) {
        notes.removeAll { it.id == id }
        persistState(context)
    }

    fun syncToGoogleCloud(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingCloud.value = true
            delay(700)
            persistState(context)
            isSyncingCloud.value = false
        }
    }

    private fun seedStarterTemplates() {
        val today = LocalDate.now()
        val deepWorkLogs = mutableMapOf<String, Float>()
        for (i in 1..21) {
            if (i % 4 != 0) deepWorkLogs[today.minusDays(i.toLong()).toString()] = 90f
        }
        habits.add(
            Habit(
                title = "Deep Focus Sprint",
                emoji = "🚀",
                category = "Career",
                timeOfDay = TimeOfDay.MORNING,
                habitStackingCue = "After morning hydration",
                type = HabitType.FOCUS_TIMER,
                colorHex = 0xFF34D399,
                targetValue = 90f,
                unit = "mins",
                stepSize = 30f,
                reminderTime = "08:00",
                logs = deepWorkLogs
            )
        )

        val strengthLogs = mutableMapOf<String, Float>()
        for (i in 1..25) {
            if (i % 2 == 1) strengthLogs[today.minusDays(i.toLong()).toString()] = 1f
        }
        habits.add(
            Habit(
                title = "Strength & Conditioning",
                emoji = "🏋️",
                category = "Fitness",
                timeOfDay = TimeOfDay.EVENING,
                habitStackingCue = "At 6:00 PM shutdown ritual",
                type = HabitType.CHECKLIST,
                colorHex = 0xFFA855F7,
                targetValue = 1f,
                unit = "session",
                stepSize = 1f,
                reminderTime = "18:00",
                logs = strengthLogs
            )
        )

        val waterLogs = mutableMapOf<String, Float>()
        for (i in 1..15) waterLogs[today.minusDays(i.toLong()).toString()] = 3.0f
        waterLogs[today.toString()] = 1.5f
        habits.add(
            Habit(
                title = "Hydration Protocol",
                emoji = "💧",
                category = "Health",
                timeOfDay = TimeOfDay.ALL,
                habitStackingCue = "Keep 1L bottle on desk",
                type = HabitType.MEASUREMENT,
                colorHex = 0xFF06B6D4,
                targetValue = 3.0f,
                unit = "Liters",
                stepSize = 0.5f,
                reminderTime = "11:00",
                logs = waterLogs
            )
        )

        todos.add(
            TodoTask(
                title = "Complete morning high-leverage project block",
                category = "Deep Work",
                priority = TodoPriority.P1_CRITICAL,
                reminderTime = "09:00",
                estimatedMinutes = 45,
                subTasks = listOf(
                    SubTask(title = "Review daily goals", isDone = true),
                    SubTask(title = "Ship core feature module")
                )
            )
        )

        journals.add(
            JournalEntry(
                title = "Calm Focus & Evening Reflection",
                moodEmoji = "🔥",
                energyScore = 9,
                gratitudeOrWins = "Completed my morning deep-work sprint without checking messages.",
                dailyStory = "Today felt grounded and productive. Switching the journal to Warm Candlelight mode makes evening writing super relaxing on the eyes.",
                frictionOrMistakes = "Delayed evening stretching by 20 minutes.",
                actionItems = listOf("Start evening wind-down alarm at 9:30 PM")
            )
        )

        notes.add(
            NotebookEntry(
                category = NoteCategory.BOOK_SUMMARY,
                title = "The Almanack of Naval Ravikant",
                authorOrSubtitle = "Eric Jorgenson",
                rating = 5,
                readingProgress = 100,
                isPinned = true,
                coverColorHex = 0xFF34D399,
                favoriteQuote = "Play iterated games. All the returns in life, whether in wealth, relationships, or knowledge, come from compound interest.",
                keyTakeaways = listOf(
                    "Specific knowledge is found by pursuing your genuine talent and curiosity.",
                    "Code and media are permissionless leverage that work for you while you sleep.",
                    "A calm mind, a fit body, and a house full of love must be earned."
                ),
                detailedNotes = "Reading in Sanctuary Mode lets you review book summaries comfortably before bed without harsh blue light.",
                flashcards = listOf(
                    FlashcardItem(
                        question = "What are the two forms of permissionless leverage?",
                        answer = "Code and Media."
                    )
                )
            )
        )
    }
}
