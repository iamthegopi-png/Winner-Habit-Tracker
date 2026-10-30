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
// 1. ALPHA STUDIO COLOR SYSTEM + EYE-SOOTHING SANCTUARY READING THEMES
// ============================================================================
object AppColors {
    val BgDeep = Color(0xFF070910)
    val BgSurface = Color(0xFF0E131F)
    val BgCard = Color(0xFF151C2C)
    val BgCardElevated = Color(0xFF1E283D)
    val BorderSubtle = Color(0xFF2A3752)

    val MintGlow = Color(0xFF34D399)
    val EmeraldNeon = Color(0xFF10B981)
    val ElectricViolet = Color(0xFFA855F7)
    val CyberCyan = Color(0xFF06B6D4)
    val SunsetCoral = Color(0xFFF97316)
    val CrimsonRose = Color(0xFFF43F5E)
    val SolarGold = Color(0xFFFBBF24)
    val IndigoPulse = Color(0xFF6366F1)

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
        bgHex = 0xFF070910,
        cardHex = 0xFF151C2C,
        borderHex = 0xFF2A3752,
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
// 2. SUPERHERO EVOLUTION & MOTIVATION ENGINE (Upgrades with XP, Drains on Wasted Hours)
// ============================================================================
enum class CharacterStage(
    val stageNumber: Int,
    val title: String,
    val formName: String,
    val suitName: String,
    val superpower: String,
    val tagline: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val capeColorHex: Long,
    val weaponOrGear: String
) {
    STAGE_1_SPARK(
        stageNumber = 1,
        title = "Shadow Vigilante",
        formName = "Alpha Recruit",
        suitName = "Stealth Kevlar Mk-I",
        superpower = "Kinetic Reflexes",
        tagline = "Every superhero starts with one disciplined hour. Protect your time!",
        primaryColorHex = 0xFF38BDF8,
        secondaryColorHex = 0xFF34D399,
        capeColorHex = 0xFF1E3A8A,
        weaponOrGear = "Pulse Grappler & Cowl"
    ),
    STAGE_2_VANGUARD(
        stageNumber = 2,
        title = "Cyber Centurion",
        formName = "Cobalt Defender",
        suitName = "Titanium Exo-Suit Mk-II",
        superpower = "Plasma Barrier",
        tagline = "Your daily streaks are charging the arc core. Don't let distractions drain it!",
        primaryColorHex = 0xFF34D399,
        secondaryColorHex = 0xFF06B6D4,
        capeColorHex = 0xFF065F46,
        weaponOrGear = "Vibranium Aegis & Thrusters"
    ),
    STAGE_3_SPARTAN(
        stageNumber = 3,
        title = "Crimson Avenger",
        formName = "Apex Striker",
        suitName = "Nanotech Battle Suit Mk-III",
        superpower = "Supersonic Flight",
        tagline = "High-output momentum unlocked! Your cape and photon gauntlets are online.",
        primaryColorHex = 0xFFF43F5E,
        secondaryColorHex = 0xFFFBBF24,
        capeColorHex = 0xFF9F1239,
        weaponOrGear = "Photon Gauntlets & Crimson Cape"
    ),
    STAGE_4_WARLORD(
        stageNumber = 4,
        title = "Thunder Paragon",
        formName = "Storm Sovereign",
        suitName = "Asgardian Electro-Armor Mk-IV",
        superpower = "Bio-Electric Lightning",
        tagline = "You command the hours of your day. Lightning surges through your armor!",
        primaryColorHex = 0xFFA855F7,
        secondaryColorHex = 0xFF38BDF8,
        capeColorHex = 0xFF581C87,
        weaponOrGear = "Stormbreaker Energy Core"
    ),
    STAGE_5_EMPEROR(
        stageNumber = 5,
        title = "Solar Hyperion",
        formName = "Sunforged Sentinel",
        suitName = "Celestial Gold Armor Mk-V",
        superpower = "Solar Nova Beam",
        tagline = "Peak human discipline! Golden wings of light and unstoppable focus.",
        primaryColorHex = 0xFFF97316,
        secondaryColorHex = 0xFFFBBF24,
        capeColorHex = 0xFFB45309,
        weaponOrGear = "Solar Halo & Golden Cape"
    ),
    STAGE_6_SOVEREIGN(
        stageNumber = 6,
        title = "Omega Prime",
        formName = "Cosmic Alpha Hero",
        suitName = "Infinity Quantum Armor Mk-VI",
        superpower = "Reality & Time Mastery",
        tagline = "Ultimate Superhero form achieved! Master of time, habits, and destiny.",
        primaryColorHex = 0xFFFBBF24,
        secondaryColorHex = 0xFFF43F5E,
        capeColorHex = 0xFF7F1D1D,
        weaponOrGear = "Omega Infinity Crest & Cosmic Aura"
    )
}

@Immutable
data class AlphaCharacterStats(
    val stage: CharacterStage,
    val level: Int,
    val combatPower: Int,
    val strength: Int,
    val intellect: Int,
    val willpower: Int,
    val vitality: Int,
    val activeStreakBonus: Int,
    val heroSpeechBubble: String
)

// ============================================================================
// 3. HOURLY PRODUCTIVITY LEDGER MODELS (New 5th Section: Day Audit)
// ============================================================================
enum class HourProductivityStatus(
    val label: String,
    val shortBadge: String,
    val xpDelta: Int,
    val scoreDelta: Int,
    val colorHex: Long
) {
    PEAK_FLOW("🔥 Peak Flow / Deep Work", "+25 XP", 25, 15, 0xFF34D399),
    PRODUCTIVE("✅ Productive / Useful", "+15 XP", 15, 10, 0xFF06B6D4),
    NEUTRAL("☕ Rest / Recharge", "0 XP", 0, 2, 0xFF94A3B8),
    LOW_FOCUS("⚠️ Distracted / Slow", "-15 XP", -15, -10, 0xFFF97316),
    TIME_WASTED("💀 Wasted / Doomscroll", "-30 XP", -30, -20, 0xFFF43F5E);

    val color get() = Color(colorHex)
}

@Immutable
data class HourlyLogItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val hour24: Int, // 0..23
    val activityNote: String,
    val status: HourProductivityStatus
) {
    fun formattedHourRange(): String {
        val start = LocalTime.of(hour24.coerceIn(0, 23), 0)
        val end = start.plusHours(1)
        val fmt = DateTimeFormatter.ofPattern("hh:mm a")
        return "${start.format(fmt)} - ${end.format(fmt)}"
    }
}

// ============================================================================
// 4. HABIT, AUTO-RENEW TASK, JOURNAL & VAULT MODELS
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
    val isRecurring: Boolean = false,       // Auto-Renew Daily flag
    val lastCompletedDate: String = "",     // Used to auto-reset on the next day
    val renewalStreak: Int = 0,             // Counts consecutive days completed
    val estimatedMinutes: Int = 25,
    val subTasks: List<SubTask> = emptyList(),
    val notes: String = ""
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
// 5. SYNTHESIZED SOUND ENGINE (Celebration, Motivational Chime & Penalty Buzz)
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

    /**
     * Plays a descending power-drain tone when the user logs a wasted/unproductive hour
     * that deducts XP from their superhero.
     */
    fun playPenaltyDrainSound(context: Context) {
        triggerHaptics(context, isLevelUp = false)
        val drainNotes = listOf(
            Triple(392.00, 0.00, 0.35), // G4
            Triple(349.23, 0.14, 0.35), // F4
            Triple(293.66, 0.28, 0.55)  // D4
        )
        playSynthesizedSequence(drainNotes, durationSeconds = 0.90)
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
// 6. SCHEDULED MOTIVATIONAL NOTIFICATIONS
// ============================================================================
object ReminderScheduler {
    private val motivationalQuotes = listOf(
        "Superhero Protocol: Every completed habit upgrades your armor! 🔥",
        "Don't let distractions drain your Hero XP today—execute now! ⚡",
        "Your daily auto-renewing missions are waiting. Claim your XP! 🚀",
        "Champions guard every hour of the day. Log a Peak Flow hour! 🏆"
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
        val quote = intent.getStringExtra("EXTRA_QUOTE") ?: "Upgrade your Alpha Superhero today! 🔥"
        val timeStr = intent.getStringExtra("EXTRA_TIME") ?: ""
        val isHabit = intent.getBooleanExtra("EXTRA_IS_HABIT", true)

        CelebrationSoundEngine.playMotivationalReminderSound(context)

        val channelId = "alpha_motivation_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alpha Superhero Reminders",
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
            .setStyle(NotificationCompat.BigTextStyle().bigText("$quote\nTap to complete & power up your Superhero!"))
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
// 7. HOME SCREEN WIDGETS (Habits Widget + Auto-Renewing Tasks Widget)
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
                Toast.makeText(context, "⚡ $toggledTitle Complete! +35 XP (Hero Powered Up!)", Toast.LENGTH_SHORT).show()
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
            val completedTitle = storage.toggleTaskDirectlyFromWidget(taskId)
            if (completedTitle != null) {
                CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = false)
                Toast.makeText(context, "🎯 Mission Complete: $completedTitle!", Toast.LENGTH_SHORT).show()
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

                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openPending = PendingIntent.getActivity(
                        context, 1, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.task_widget_header_title, openPending)

                    for (i in 0 until 4) {
                        val (rowId, titleId, btnId) = rowIds[i]
                        if (i < tasks.size) {
                            val t = tasks[i]
                            val renewIcon = if (t.isRecurring) " 🔄" else ""
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(
                                titleId,
                                "[${t.priority.shortTag}] ${t.title}$renewIcon"
                            )
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
// 8. LOCAL PERSISTENCE & GOOGLE DRIVE / CLOUD BACKUP ENGINE
// ============================================================================
class StudioStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("aura_studio_vault_v2", Context.MODE_PRIVATE)

    fun saveFullState(
        xp: Int,
        streakShields: Int,
        bonusHeroStr: Int = 0,
        bonusHeroInt: Int = 0,
        bonusHeroWil: Int = 0,
        bonusHeroVit: Int = 0,
        googleEmail: String?,
        autoBackup: Boolean,
        readingTheme: SanctuaryReadingTheme = SanctuaryReadingTheme.WARM_SEPIA,
        readingFontScale: Float = 1.0f,
        habits: List<Habit>,
        todos: List<TodoTask>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): String {
        val root = serializeToJson(
            xp = xp,
            streakShields = streakShields,
            bonusHeroStr = bonusHeroStr,
            bonusHeroInt = bonusHeroInt,
            bonusHeroWil = bonusHeroWil,
            bonusHeroVit = bonusHeroVit,
            habits = habits,
            todos = todos,
            hourlyLogs = hourlyLogs,
            journals = journals,
            notes = notes
        )
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

    fun loadTasksOnly(): List<TodoTask> {
        val jsonStr = getSavedJson() ?: return emptyList()
        val result = mutableListOf<TodoTask>()
        val todayStr = LocalDate.now().toString()
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: JSONArray()
            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                val isRec = o.optBoolean("isRecurring", false)
                val lastDate = o.optString("lastCompletedDate", "")
                val rawCompleted = o.optBoolean("isCompleted", false)
                // Auto-renew recurring tasks if completed on a previous day
                val effectiveCompleted = if (isRec && rawCompleted && lastDate.isNotBlank() && lastDate != todayStr) {
                    false
                } else {
                    rawCompleted
                }
                result.add(
                    TodoTask(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category", "Personal"),
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }.getOrDefault(TodoPriority.P2_HIGH),
                        isCompleted = effectiveCompleted,
                        isRecurring = isRec,
                        lastCompletedDate = lastDate,
                        renewalStreak = o.optInt("renewalStreak", 0)
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
                root.put("xp", root.optInt("xp", 460) + 35)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return completedHabitTitle
        } catch (_: Exception) {
            return null
        }
    }

    fun toggleTaskDirectlyFromWidget(taskId: String): String? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: return null
            val todayStr = LocalDate.now().toString()
            var newlyCompletedTitle: String? = null

            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                if (o.optString("id") == taskId) {
                    val wasDone = o.optBoolean("isCompleted", false)
                    val nowDone = !wasDone
                    o.put("isCompleted", nowDone)
                    if (nowDone) {
                        o.put("lastCompletedDate", todayStr)
                        o.put("renewalStreak", o.optInt("renewalStreak", 0) + 1)
                        newlyCompletedTitle = o.optString("title")
                    }
                    break
                }
            }

            if (newlyCompletedTitle != null) {
                root.put("xp", root.optInt("xp", 460) + 25)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return newlyCompletedTitle
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
        bonusHeroStr: Int,
        bonusHeroInt: Int,
        bonusHeroWil: Int,
        bonusHeroVit: Int,
        habits: List<Habit>,
        todos: List<TodoTask>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 5)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("xp", xp)
        root.put("streakShields", streakShields)
        root.put("bonusHeroStr", bonusHeroStr)
        root.put("bonusHeroInt", bonusHeroInt)
        root.put("bonusHeroWil", bonusHeroWil)
        root.put("bonusHeroVit", bonusHeroVit)

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
                put("lastCompletedDate", t.lastCompletedDate)
                put("renewalStreak", t.renewalStreak)
                put("estimatedMinutes", t.estimatedMinutes)
                put("subTasks", subArr)
                put("notes", t.notes)
            })
        }
        root.put("todos", todosArr)

        val hourlyArr = JSONArray()
        hourlyLogs.forEach { hl ->
            hourlyArr.put(JSONObject().apply {
                put("id", hl.id)
                put("date", hl.date)
                put("hour24", hl.hour24)
                put("activityNote", hl.activityNote)
                put("status", hl.status.name)
            })
        }
        root.put("hourlyLogs", hourlyArr)

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
// 9. ALPHA SUPERHERO VIEWMODEL (5 Sections, Auto-Renewal, Hourly Score, Hero Coach)
// ============================================================================
class HabitTrackerViewModel : ViewModel() {

    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val hourlyLogs = mutableStateListOf<HourlyLogItem>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()

    val selectedTimeOfDay = mutableStateOf(TimeOfDay.ALL)
    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.STUDIO_CARDS)
    val selectedDate = mutableStateOf(LocalDate.now())

    val readingTheme = mutableStateOf(SanctuaryReadingTheme.WARM_SEPIA)
    val readingFontScale = mutableFloatStateOf(1.0f)

    // Superhero XP & Manual Stat Upgrades
    val userXp = mutableIntStateOf(520)
    val streakShields = mutableIntStateOf(3)
    val bonusHeroStr = mutableIntStateOf(0)
    val bonusHeroInt = mutableIntStateOf(0)
    val bonusHeroWil = mutableIntStateOf(0)
    val bonusHeroVit = mutableIntStateOf(0)
    val customHeroQuote = mutableStateOf<String?>(null)

    val userLevel: Int get() = (userXp.intValue.coerceAtLeast(0) / 200) + 1
    val xpProgressInLevel: Float get() = ((userXp.intValue.coerceAtLeast(0)) % 200) / 200f
    val userRankTitle: String get() = getCharacterStats().stage.title

    private val heroMotivationalLines = listOf(
        "“Every hour you guard makes my armor stronger. Let's crush today's mission list!”",
        "“Don't let doomscrolling drain our XP! Lock in one Peak Flow hour right now.”",
        "“Auto-renewing daily tasks are ready. Champions win the morning first!”",
        "“Your consistency is forging the Omega Prime suit—keep the streak alive!”",
        "“Log your hours honestly in the Day Audit. Awareness is our greatest superpower!”"
    )

    fun triggerNewHeroMotivation(context: Context) {
        customHeroQuote.value = heroMotivationalLines.random()
        CelebrationSoundEngine.playMotivationalReminderSound(context)
    }

    /**
     * Allows the user to invest 50 XP into permanently boosting one of their
     * Superhero's core powers (STR, INT, WIL, VIT).
     */
    fun upgradeSuperheroPower(context: Context, statType: String) {
        if (userXp.intValue < 50) {
            Toast.makeText(context, "Earn at least 50 XP from Habits/Tasks to upgrade!", Toast.LENGTH_SHORT).show()
            return
        }
        when (statType) {
            "STR" -> bonusHeroStr.intValue += 15
            "INT" -> bonusHeroInt.intValue += 15
            "WIL" -> bonusHeroWil.intValue += 15
            "VIT" -> bonusHeroVit.intValue += 15
        }
        userXp.intValue += 10 // Bonus mastery XP
        CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = true)
        persistState(context)
    }

    fun getCharacterStats(): AlphaCharacterStats {
        val lvl = userLevel
        val peakStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0
        val totalStreakSum = habits.sumOf { it.currentStreak() }
        val todayStr = LocalDate.now().toString()
        val wastedToday = hourlyLogs.count { it.date == todayStr && (it.status == HourProductivityStatus.TIME_WASTED || it.status == HourProductivityStatus.LOW_FOCUS) }
        val flowToday = hourlyLogs.count { it.date == todayStr && it.status == HourProductivityStatus.PEAK_FLOW }

        val stage = when {
            lvl >= 12 || peakStreak >= 30 -> CharacterStage.STAGE_6_SOVEREIGN
            lvl >= 9 || peakStreak >= 21 -> CharacterStage.STAGE_5_EMPEROR
            lvl >= 6 || peakStreak >= 14 -> CharacterStage.STAGE_4_WARLORD
            lvl >= 4 || peakStreak >= 7 -> CharacterStage.STAGE_3_SPARTAN
            lvl >= 2 || peakStreak >= 3 -> CharacterStage.STAGE_2_VANGUARD
            else -> CharacterStage.STAGE_1_SPARK
        }

        val strLogs = habits.filter {
            it.category.contains("Fitness", true) || it.category.contains("Health", true)
        }.sumOf { it.logs.size }
        val intLogs = habits.filter {
            it.category.contains("Career", true) || it.category.contains("Growth", true)
        }.sumOf { it.logs.size } + (notes.size * 5) + (flowToday * 4)
        val wilLogs = totalStreakSum * 3 + todos.count { it.isCompleted } * 3 - (wastedToday * 5)
        val vitLogs = journals.size * 5 + habits.size * 3

        val str = (35 + strLogs * 2 + lvl * 5 + bonusHeroStr.intValue).coerceAtLeast(10)
        val intStat = (40 + intLogs * 2 + lvl * 5 + bonusHeroInt.intValue).coerceAtLeast(10)
        val wil = (38 + wilLogs + lvl * 6 + bonusHeroWil.intValue).coerceAtLeast(10)
        val vit = (35 + vitLogs * 2 + lvl * 4 + bonusHeroVit.intValue).coerceAtLeast(10)
        val power = ((str + intStat + wil + vit) * 3 + userXp.intValue).coerceAtLeast(100)

        val dynamicBubble = customHeroQuote.value ?: when {
            wastedToday > 0 && flowToday == 0 ->
                "⚠️ Alert! $wastedToday wasted hour(s) drained our energy today. Log a Peak Flow hour to recover!"
            flowToday >= 2 ->
                "🔥 Unstoppable! $flowToday Peak Flow hours logged today. My ${stage.suitName} is at maximum output!"
            todos.any { !it.isCompleted && it.isRecurring } ->
                "🔄 Your daily auto-renewing missions are active. Complete them to boost Willpower!"
            else ->
                "“I am ${stage.formName}. Every habit, task, and productive hour upgrades my ${stage.suitName}!”"
        }

        return AlphaCharacterStats(
            stage = stage,
            level = lvl,
            combatPower = power,
            strength = str,
            intellect = intStat,
            willpower = wil,
            vitality = vit,
            activeStreakBonus = totalStreakSum,
            heroSpeechBubble = dynamicBubble
        )
    }

    // --- Daily Productivity Score Calculation (0..100%) ---
    fun getDailyProductivityScore(date: LocalDate = selectedDate.value): Int {
        val dateStr = date.toString()
        val habitsDone = habits.count { it.isCompletedOn(date) }
        val habitBase = if (habits.isEmpty()) 50 else ((habitsDone.toFloat() / habits.size) * 55f).toInt()
        val tasksBonus = (todos.count { it.isCompleted } * 8).coerceAtMost(30)
        val hourlyDelta = hourlyLogs.filter { it.date == dateStr }.sumOf { it.status.scoreDelta }
        return (20 + habitBase + tasksBonus + hourlyDelta).coerceIn(0, 100)
    }

    fun getDailyProductivityGrade(score: Int): String = when {
        score >= 90 -> "S+ SUPREME"
        score >= 80 -> "A • EXCELLENT"
        score >= 65 -> "B • SOLID"
        score >= 45 -> "C • AVERAGE"
        else -> "D • NEEDS FOCUS"
    }

    // --- Hourly Productivity Ledger Operations ---
    fun logOrUpdateHour(
        context: Context,
        hour24: Int,
        activityNote: String,
        status: HourProductivityStatus,
        date: LocalDate = selectedDate.value
    ) {
        val dateStr = date.toString()
        val existingIdx = hourlyLogs.indexOfFirst { it.date == dateStr && it.hour24 == hour24 }
        val previousXpDelta = if (existingIdx != -1) hourlyLogs[existingIdx].status.xpDelta else 0

        val newItem = HourlyLogItem(
            id = if (existingIdx != -1) hourlyLogs[existingIdx].id else UUID.randomUUID().toString(),
            date = dateStr,
            hour24 = hour24,
            activityNote = activityNote.ifBlank { status.label },
            status = status
        )

        if (existingIdx != -1) {
            hourlyLogs[existingIdx] = newItem
        } else {
            hourlyLogs.add(newItem)
        }
        hourlyLogs.sortBy { it.hour24 }

        // Adjust XP by the net change
        val netXpChange = status.xpDelta - previousXpDelta
        userXp.intValue = (userXp.intValue + netXpChange).coerceAtLeast(0)

        if (status.xpDelta > 0) {
            awardXpAndCelebrate(
                context = context,
                title = "⚡ Productive Hour Logged!",
                subtitle = "${newItem.formattedHourRange()} • ${status.shortBadge} added to Superhero!",
                xpEarned = 0 // Already applied netXpChange above
            )
        } else if (status.xpDelta < 0) {
            CelebrationSoundEngine.playPenaltyDrainSound(context)
            Toast.makeText(
                context,
                "⚠️ ${status.xpDelta} XP Penalty! Unproductive hour deducted from your score.",
                Toast.LENGTH_LONG
            ).show()
            persistState(context)
        } else {
            persistState(context)
        }
    }

    fun deleteHourlyLog(context: Context, id: String) {
        val idx = hourlyLogs.indexOfFirst { it.id == id }
        if (idx != -1) {
            val removed = hourlyLogs.removeAt(idx)
            userXp.intValue = (userXp.intValue - removed.status.xpDelta).coerceAtLeast(0)
            persistState(context)
        }
    }

    // --- Auto-Renewing Daily Tasks Engine ---
    fun checkAndAutoRenewDailyTasks(context: Context, forceRenewForTesting: Boolean = false) {
        val todayStr = LocalDate.now().toString()
        var renewedCount = 0

        for (i in todos.indices) {
            val t = todos[i]
            val shouldRenew = t.isRecurring && t.isCompleted && (forceRenewForTesting || (t.lastCompletedDate.isNotBlank() && t.lastCompletedDate != todayStr))
            if (shouldRenew) {
                todos[i] = t.copy(
                    isCompleted = false,
                    date = todayStr,
                    subTasks = t.subTasks.map { it.copy(isDone = false) }
                )
                renewedCount++
            }
        }

        if (renewedCount > 0) {
            persistState(context)
            if (forceRenewForTesting) {
                Toast.makeText(
                    context,
                    "🔄 $renewedCount Auto-Renewing task(s) refreshed for the new day!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (forceRenewForTesting) {
            Toast.makeText(
                context,
                "Complete a 🔄 Daily Auto-Renew task first to test next-day renewal!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Pomodoro Timer
    val activeFocusTitle = mutableStateOf("Deep Focus Session")
    val activeFocusHabitId = mutableStateOf<String?>(null)
    val focusRemainingSeconds = mutableLongStateOf(25 * 60L)
    val focusTotalSeconds = mutableLongStateOf(25 * 60L)
    val isFocusTimerRunning = mutableStateOf(false)
    private var timerJob: Job? = null

    // Full-Screen Celebration
    val showCelebration = mutableStateOf(false)
    val celebrationTitle = mutableStateOf("")
    val celebrationSubtitle = mutableStateOf("")
    val celebrationXpEarned = mutableIntStateOf(25)

    // Cloud Backup
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
            // Automatically renew any daily recurring tasks completed on a previous day!
            checkAndAutoRenewDailyTasks(context, forceRenewForTesting = false)
        } else if (!isInitialized) {
            isInitialized = true
            seedStarterTemplates()
            persistState(context)
        }
    }

    fun persistState(context: Context): String {
        val storage = StudioStorageManager(context)
        val json = storage.saveFullState(
            xp = userXp.intValue,
            streakShields = streakShields.intValue,
            bonusHeroStr = bonusHeroStr.intValue,
            bonusHeroInt = bonusHeroInt.intValue,
            bonusHeroWil = bonusHeroWil.intValue,
            bonusHeroVit = bonusHeroVit.intValue,
            googleEmail = connectedGoogleEmail.value,
            autoBackup = autoCloudSync.value,
            readingTheme = readingTheme.value,
            readingFontScale = readingFontScale.floatValue,
            habits = habits,
            todos = todos,
            hourlyLogs = hourlyLogs,
            journals = journals,
            notes = notes
        )
        lastCloudBackup.value = storage.getLastBackupTime()
        return json
    }

    fun restoreFromJsonString(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            userXp.intValue = root.optInt("xp", 520)
            streakShields.intValue = root.optInt("streakShields", 3)
            bonusHeroStr.intValue = root.optInt("bonusHeroStr", 0)
            bonusHeroInt.intValue = root.optInt("bonusHeroInt", 0)
            bonusHeroWil.intValue = root.optInt("bonusHeroWil", 0)
            bonusHeroVit.intValue = root.optInt("bonusHeroVit", 0)

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
                        lastCompletedDate = o.optString("lastCompletedDate", ""),
                        renewalStreak = o.optInt("renewalStreak", 0),
                        estimatedMinutes = o.optInt("estimatedMinutes", 25),
                        subTasks = subs,
                        notes = o.optString("notes", "")
                    )
                )
            }

            hourlyLogs.clear()
            val hourlyArr = root.optJSONArray("hourlyLogs") ?: JSONArray()
            for (i in 0 until hourlyArr.length()) {
                val o = hourlyArr.getJSONObject(i)
                hourlyLogs.add(
                    HourlyLogItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        date = o.optString("date", LocalDate.now().toString()),
                        hour24 = o.optInt("hour24", 9),
                        activityNote = o.optString("activityNote", ""),
                        status = runCatching { HourProductivityStatus.valueOf(o.optString("status")) }
                            .getOrDefault(HourProductivityStatus.PRODUCTIVE)
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
        val oldStage = getCharacterStats().stage
        val oldLevel = userLevel
        userXp.intValue += xpEarned
        val newLevel = userLevel
        val newStage = getCharacterStats().stage
        val leveledUp = newLevel > oldLevel || newStage != oldStage
        if (leveledUp) streakShields.intValue += 1

        celebrationTitle.value = when {
            newStage != oldStage -> "🦸 SUPERHERO UPGRADED: ${newStage.formName.uppercase()}!"
            newLevel > oldLevel -> "🎉 HERO LEVEL $newLevel UNLOCKED!"
            else -> title
        }
        celebrationSubtitle.value = when {
            newStage != oldStage -> "Unlocked ${newStage.suitName} & ${newStage.superpower}!"
            newLevel > oldLevel -> "Superhero Rank: ${newStage.title} • Power Surging!"
            else -> subtitle
        }
        celebrationXpEarned.intValue = if (xpEarned == 0) 25 else xpEarned
        showCelebration.value = true

        CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = leveledUp)
        persistState(context)

        viewModelScope.launch {
            delay(3400)
            showCelebration.value = false
        }
    }

    // --- Focus Timer ---
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
                    title = "⏱️ Superhero Focus Complete!",
                    subtitle = "${activeFocusTitle.value} finished • Hero Intellect +5",
                    xpEarned = 45
                )
            }
        }
    }

    // --- Habit Operations ---
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
                subtitle = "🔥 ${updated.currentStreak()}-day streak • Superhero Armor Upgraded!",
                xpEarned = 35
            )
        } else {
            persistState(context)
        }
    }

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

    fun deleteHabit(context: Context, habitId: String) {
        ReminderScheduler.cancelReminder(context, habitId)
        habits.removeAll { it.id == habitId }
        persistState(context)
    }

    // --- To-Do Operations (With Daily Auto-Renewal Tracking) ---
    fun toggleTodo(context: Context, task: TodoTask) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val nowDone = !task.isCompleted
        val todayStr = LocalDate.now().toString()
        val newStreak = if (nowDone) task.renewalStreak + 1 else (task.renewalStreak - 1).coerceAtLeast(0)

        todos[idx] = task.copy(
            isCompleted = nowDone,
            lastCompletedDate = if (nowDone) todayStr else task.lastCompletedDate,
            renewalStreak = newStreak
        )

        if (nowDone) {
            val renewNote = if (task.isRecurring) " • 🔄 Will auto-renew tomorrow!" else ""
            awardXpAndCelebrate(
                context = context,
                title = "🎯 Mission Accomplished!",
                subtitle = "\"${task.title}\" completed$renewNote",
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
        val todayStr = LocalDate.now().toString()

        todos[idx] = task.copy(
            subTasks = updatedSubs,
            isCompleted = allDone,
            lastCompletedDate = if (allDone) todayStr else task.lastCompletedDate,
            renewalStreak = if (!wasDone && allDone) task.renewalStreak + 1 else task.renewalStreak
        )
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

    fun deleteTodoTask(context: Context, id: String) {
        ReminderScheduler.cancelReminder(context, id)
        todos.removeAll { it.id == id }
        persistState(context)
    }

    // --- Journal Operations ---
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
                        priority = TodoPriority.P1_CRITICAL,
                        isRecurring = false
                    )
                )
            }
        }

        awardXpAndCelebrate(
            context = context,
            title = "📓 Sanctuary Reflection Saved!",
            subtitle = "Hero Vitality +5 • Self-mastery unlocked",
            xpEarned = 35
        )
    }

    fun deleteJournalEntry(context: Context, id: String) {
        journals.removeAll { it.id == id }
        persistState(context)
    }

    // --- Notebook Vault Operations ---
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
            subtitle = "Hero Intellect +5 • \"${entry.title}\" indexed",
            xpEarned = 35
        )
    }

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
        val todayStr = today.toString()

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
        waterLogs[todayStr] = 1.5f
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

        // Starter Tasks with Auto-Renew Every Day enabled
        todos.addAll(
            listOf(
                TodoTask(
                    title = "Plan Top 3 High-Impact Outcomes",
                    category = "Daily Ritual",
                    priority = TodoPriority.P1_CRITICAL,
                    reminderTime = "08:30",
                    isRecurring = true,
                    renewalStreak = 7,
                    estimatedMinutes = 15,
                    subTasks = listOf(
                        SubTask(title = "Review calendar & deadlines", isDone = true),
                        SubTask(title = "Lock in 90m maker block")
                    )
                ),
                TodoTask(
                    title = "Read 20 Pages & Log Vault Summary",
                    category = "Growth",
                    priority = TodoPriority.P2_HIGH,
                    reminderTime = "21:00",
                    isRecurring = true,
                    renewalStreak = 12,
                    estimatedMinutes = 30
                ),
                TodoTask(
                    title = "Complete morning high-leverage project block",
                    category = "Deep Work",
                    priority = TodoPriority.P1_CRITICAL,
                    reminderTime = "09:30",
                    isRecurring = false,
                    estimatedMinutes = 45
                )
            )
        )

        // Starter Hourly Productivity Logs for Today
        hourlyLogs.addAll(
            listOf(
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 7,
                    activityNote = "Morning hydration, sunlight & mobility routine",
                    status = HourProductivityStatus.PRODUCTIVE
                ),
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 8,
                    activityNote = "90m Deep Work coding sprint with zero phone checks",
                    status = HourProductivityStatus.PEAK_FLOW
                ),
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 9,
                    activityNote = "Shipped core architecture & reviewed metrics",
                    status = HourProductivityStatus.PEAK_FLOW
                ),
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 13,
                    activityNote = "Scrolled social media after lunch for 35 mins",
                    status = HourProductivityStatus.LOW_FOCUS
                )
            )
        )

        journals.add(
            JournalEntry(
                title = "Superhero Momentum & Hourly Audit",
                moodEmoji = "🔥",
                energyScore = 9,
                gratitudeOrWins = "Locked in two back-to-back Peak Flow hours this morning.",
                dailyStory = "Tracking each hour in the Day Audit makes me super conscious of how I spend my afternoon. Watching my Superhero suit upgrade with every productive hour is awesome motivation.",
                frictionOrMistakes = "Lost 15 XP at 1:00 PM from post-lunch scrolling.",
                actionItems = listOf("Take a 10-minute walk after lunch instead of opening social media")
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
