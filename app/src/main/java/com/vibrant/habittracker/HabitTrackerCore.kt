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
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
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
import androidx.core.content.ContextCompat
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
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// ============================================================================
// 1. TITANIUM OBSIDIAN COLOR SYSTEM (Apple / One UI Studio Aesthetics)
// ============================================================================
object AppColors {
    val BgDeep = Color(0xFF05070B)
    val BgSurface = Color(0xFF0B0F17)
    val BgCard = Color(0xFF121824)
    val BgCardElevated = Color(0xFF1A2233)
    val BorderSubtle = Color(0xFF253046)
    val BorderHighlight = Color(0xFF384766)

    val Platinum = Color(0xFFE2E8F0)
    val MintGlow = Color(0xFF34D399)
    val EmeraldNeon = Color(0xFF10B981)
    val ElectricViolet = Color(0xFFA855F7)
    val CyberCyan = Color(0xFF38BDF8)
    val SunsetCoral = Color(0xFFF97316)
    val CrimsonRose = Color(0xFFF43F5E)
    val SolarGold = Color(0xFFFBBF24)
    val IndigoPulse = Color(0xFF6366F1)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    val StudioPalette = listOf(
        MintGlow,
        CyberCyan,
        ElectricViolet,
        SolarGold,
        SunsetCoral,
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
        label = "☕ Candlelight",
        bgHex = 0xFF17130F,
        cardHex = 0xFF221C16,
        borderHex = 0xFF3A3026,
        primaryTextHex = 0xFFF5E6D3,
        secondaryTextHex = 0xFFC8B6A2,
        accentHex = 0xFFEAB308
    ),
    FOREST_SAGE(
        label = "🌿 Forest Zen",
        bgHex = 0xFF0B1411,
        cardHex = 0xFF12211C,
        borderHex = 0xFF223A31,
        primaryTextHex = 0xFFE6F4EE,
        secondaryTextHex = 0xFFA3C4B6,
        accentHex = 0xFF34D399
    ),
    VELVET_DUSK(
        label = "🌙 Velvet Dusk",
        bgHex = 0xFF100E1B,
        cardHex = 0xFF191528,
        borderHex = 0xFF2E2747,
        primaryTextHex = 0xFFEDE9FE,
        secondaryTextHex = 0xFFB8B0D8,
        accentHex = 0xFFC084FC
    ),
    STUDIO_OBSIDIAN(
        label = "✨ Obsidian",
        bgHex = 0xFF05070B,
        cardHex = 0xFF121824,
        borderHex = 0xFF253046,
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
// 2. STEP-BY-STEP DARK KNIGHT SENTINEL EVOLUTION (NO SHORTCUTS — EARNED ONLY)
// ============================================================================
enum class CharacterStage(
    val stageNumber: Int,
    val title: String,
    val formName: String,
    val suitName: String,
    val minXpRequired: Int,
    val minStreakRequired: Int,
    val armorUpgradeDetail: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val capeColorHex: Long
) {
    STAGE_1_SCOUT(
        stageNumber = 1,
        title = "Obsidian Scout",
        formName = "Mk-I Initiate",
        suitName = "Matte Carbon Kevlar",
        minXpRequired = 0,
        minStreakRequired = 0,
        armorUpgradeDetail = "Tactical Cowl & Matte Chest Plate",
        primaryColorHex = 0xFF38BDF8,
        secondaryColorHex = 0xFF94A3B8,
        capeColorHex = 0xFF0F172A
    ),
    STAGE_2_SENTINEL(
        stageNumber = 2,
        title = "Night Sentinel",
        formName = "Mk-II Tactical",
        suitName = "Reinforced Titanium Weave",
        minXpRequired = 400,
        minStreakRequired = 3,
        armorUpgradeDetail = "Layered Pauldrons & Illuminated Visor",
        primaryColorHex = 0xFF34D399,
        secondaryColorHex = 0xFF38BDF8,
        capeColorHex = 0xFF0B1F1D
    ),
    STAGE_3_WARDEN(
        stageNumber = 3,
        title = "Shadow Warden",
        formName = "Mk-III Armored",
        suitName = "Sculpted Stealth Alloy",
        minXpRequired = 900,
        minStreakRequired = 7,
        armorUpgradeDetail = "High-Collar Mantle & Kinetic Chest Core",
        primaryColorHex = 0xFF6366F1,
        secondaryColorHex = 0xFF38BDF8,
        capeColorHex = 0xFF17142E
    ),
    STAGE_4_KNIGHT(
        stageNumber = 4,
        title = "Eclipse Knight",
        formName = "Mk-IV Vanguard",
        suitName = "Ballistic Graphite Exo-Suit",
        minXpRequired = 1500,
        minStreakRequired = 14,
        armorUpgradeDetail = "Winged Glide Cloak & Gauntlet Blades",
        primaryColorHex = 0xFFA855F7,
        secondaryColorHex = 0xFFF43F5E,
        capeColorHex = 0xFF210E2E
    ),
    STAGE_5_NOCTURNE(
        stageNumber = 5,
        title = "Nocturne Apex",
        formName = "Mk-V Sovereign",
        suitName = "Gilded Obsidian Battle Armor",
        minXpRequired = 2400,
        minStreakRequired = 21,
        armorUpgradeDetail = "Gold-Trimmed Armor Plates & Energy Crest",
        primaryColorHex = 0xFFF43F5E,
        secondaryColorHex = 0xFFFBBF24,
        capeColorHex = 0xFF2A0A14
    ),
    STAGE_6_AETHER(
        stageNumber = 6,
        title = "Aether Sovereign",
        formName = "Mk-VI Ascended",
        suitName = "Celestial Carbon Prime",
        minXpRequired = 3600,
        minStreakRequired = 30,
        armorUpgradeDetail = "Full 3D Radiant Mantle & Sovereign Halo",
        primaryColorHex = 0xFFFBBF24,
        secondaryColorHex = 0xFF34D399,
        capeColorHex = 0xFF1F1606
    )
}

@Immutable
data class AlphaCharacterStats(
    val stage: CharacterStage,
    val nextStage: CharacterStage?,
    val stageProgressFraction: Float,
    val level: Int,
    val combatPower: Int,
    val strength: Int,
    val intellect: Int,
    val willpower: Int,
    val vitality: Int,
    val activeStreakBonus: Int,
    val comboMultiplier: Float,
    val heroSpeechBubble: String,
    val nextUpgradeRequirementText: String
)

@Immutable
data class DailyQuestStatus(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val current: Int,
    val target: Int,
    val rewardXp: Int,
    val grantsShield: Boolean,
    val isClaimed: Boolean
) {
    val isCompleted: Boolean get() = current >= target
}

@Immutable
data class NotificationHealthStatus(
    val notificationsGranted: Boolean,
    val exactAlarmsGranted: Boolean,
    val batteryUnrestricted: Boolean,
    val scheduledRemindersCount: Int
) {
    val allHealthy: Boolean get() = notificationsGranted && exactAlarmsGranted && batteryUnrestricted
}

// ============================================================================
// 3. NEW GOAL ACHIEVEMENT & DAILY WORK PROGRESS MODELS
// ============================================================================
@Immutable
data class GoalWorkLog(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val workSummary: String,
    val progressDelta: Float,
    val minutesSpent: Int = 30
)

@Immutable
data class GoalItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "🎯",
    val category: String = "Career",
    val description: String = "",
    val deadlineIso: String = LocalDate.now().plusDays(30).toString(),
    val targetValue: Float = 100f,
    val currentValue: Float = 0f,
    val unit: String = "%",
    val colorHex: Long = 0xFF38BDF8,
    val dailyLogs: List<GoalWorkLog> = emptyList(),
    val createdAt: String = LocalDate.now().toString()
) {
    val accentColor: Color get() = Color(colorHex)

    val progressFraction: Float
        get() = if (targetValue <= 0f) 0f else (currentValue / targetValue).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt()

    val isCompleted: Boolean
        get() = currentValue >= targetValue && targetValue > 0f

    fun daysRemaining(today: LocalDate = LocalDate.now()): Long {
        return try {
            ChronoUnit.DAYS.between(today, LocalDate.parse(deadlineIso))
        } catch (_: Exception) {
            0L
        }
    }

    fun requiredDailyPace(today: LocalDate = LocalDate.now()): Float {
        val remainingWork = (targetValue - currentValue).coerceAtLeast(0f)
        val daysLeft = daysRemaining(today).coerceAtLeast(1L)
        return remainingWork / daysLeft.toFloat()
    }

    fun workedToday(today: LocalDate = LocalDate.now()): Boolean {
        val key = today.toString()
        return dailyLogs.any { it.date == key }
    }

    fun totalMinutesInvested(): Int = dailyLogs.sumOf { it.minutesSpent }
}

// ============================================================================
// 4. HOURLY PRODUCTIVITY LEDGER MODELS
// ============================================================================
enum class HourProductivityStatus(
    val label: String,
    val shortBadge: String,
    val xpDelta: Int,
    val scoreDelta: Int,
    val colorHex: Long
) {
    PEAK_FLOW("🔥 Peak Flow / Deep Work", "+25 XP", 25, 15, 0xFF34D399),
    PRODUCTIVE("✅ Productive / Useful", "+15 XP", 15, 10, 0xFF38BDF8),
    NEUTRAL("☕ Rest / Recharge", "0 XP", 0, 2, 0xFF94A3B8),
    LOW_FOCUS("⚠️ Distracted / Slow", "-15 XP", -15, -10, 0xFFF97316),
    TIME_WASTED("💀 Wasted / Doomscroll", "-30 XP", -30, -20, 0xFFF43F5E);

    val color get() = Color(colorHex)
}

@Immutable
data class HourlyLogItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val hour24: Int,
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
// 5. HABIT, AUTO-RENEW TASK, JOURNAL & VAULT MODELS
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
    COMPACT_WEEKLY
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
    val customSoundUri: String = "",
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
    P3_MEDIUM("P3 • Medium", "P3", 0xFF38BDF8, 15),
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
    val customSoundUri: String = "",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val lastCompletedDate: String = "",
    val renewalStreak: Int = 0,
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
    BOOK_SUMMARY("Reading Notes"),
    SECOND_BRAIN("Mental Model"),
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
// 6. LOW-BATTERY SOUND ENGINE (Custom Phone Audio + Crisp Synth Fallback)
// ============================================================================
object CelebrationSoundEngine {

    private var activeMediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getAudioFileName(context: Context, uriString: String): String {
        if (uriString.isBlank()) return "Default Studio Sound"
        return try {
            val uri = Uri.parse(uriString)
            var name = "Custom Audio"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: "Custom Audio"
                }
            }
            name
        } catch (_: Exception) {
            "Custom Audio File"
        }
    }

    fun playCustomOrDefaultSound(
        context: Context,
        customSoundUri: String,
        isReminder: Boolean = false,
        isLevelUp: Boolean = false
    ) {
        if (customSoundUri.isNotBlank()) {
            try {
                stopActiveCustomSound()
                triggerHaptics(context, isLevelUp = true)
                val uri = Uri.parse(customSoundUri)
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(if (isReminder) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, uri)
                    prepare()
                    start()
                    setOnCompletionListener { player ->
                        try {
                            player.release()
                        } catch (_: Exception) {
                        }
                    }
                }
                activeMediaPlayer = mp
                mainHandler.postDelayed({
                    try {
                        if (mp.isPlaying) mp.stop()
                        mp.release()
                    } catch (_: Exception) {
                    }
                }, 10_000L)
                return
            } catch (_: Exception) {
            }
        }

        if (isReminder) {
            playMotivationalReminderSound(context)
        } else {
            playCelebrationFanfare(context, isLevelUp = isLevelUp)
        }
    }

    fun stopActiveCustomSound() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            activeMediaPlayer = null
        }
    }

    fun playCelebrationFanfare(context: Context, isLevelUp: Boolean = false) {
        triggerHaptics(context, isLevelUp)
        val notes = if (isLevelUp) {
            listOf(
                Triple(523.25, 0.00, 0.36),
                Triple(659.25, 0.07, 0.36),
                Triple(783.99, 0.14, 0.42),
                Triple(1046.50, 0.21, 0.60),
                Triple(1318.51, 0.30, 0.70)
            )
        } else {
            listOf(
                Triple(587.33, 0.00, 0.30),
                Triple(739.99, 0.07, 0.35),
                Triple(880.00, 0.14, 0.40),
                Triple(1174.66, 0.21, 0.65)
            )
        }
        playSynthesizedSequence(notes, durationSeconds = if (isLevelUp) 1.15 else 0.90)
    }

    fun playMotivationalReminderSound(context: Context) {
        triggerHaptics(context, isLevelUp = true)
        val motivationalChord = listOf(
            Triple(440.00, 0.00, 0.48),
            Triple(554.37, 0.11, 0.48),
            Triple(659.25, 0.22, 0.52),
            Triple(880.00, 0.33, 0.80),
            Triple(1108.73, 0.46, 0.85)
        )
        playSynthesizedSequence(motivationalChord, durationSeconds = 1.35)
    }

    fun playPenaltyDrainSound(context: Context) {
        triggerHaptics(context, isLevelUp = false)
        val drainNotes = listOf(
            Triple(392.00, 0.00, 0.30),
            Triple(349.23, 0.12, 0.30),
            Triple(293.66, 0.24, 0.45)
        )
        playSynthesizedSequence(drainNotes, durationSeconds = 0.75)
    }

    private fun playSynthesizedSequence(
        notes: List<Triple<Double, Double, Double>>,
        durationSeconds: Double
    ) {
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0
                    for ((freq, start, dur) in notes) {
                        if (t >= start && t <= start + dur) {
                            val localT = t - start
                            val attack = (localT / 0.015).coerceAtMost(1.0)
                            val decay = exp(-3.6 * localT)
                            val wave = sin(2.0 * PI * freq * localT) +
                                    0.30 * sin(2.0 * PI * freq * 2.0 * localT)
                            sample += wave * attack * decay * 0.26
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
                Thread.sleep((durationSeconds * 1000).toLong() + 80)
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
            val timings = if (isLevelUp) longArrayOf(0, 35, 35, 55, 35, 95) else longArrayOf(0, 30, 35, 60)
            val amps = if (isLevelUp) intArrayOf(0, 160, 0, 200, 0, 255) else intArrayOf(0, 150, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } catch (_: Exception) {
        }
    }
}

// ============================================================================
// 7. DOZE-PROOF ALARM SCHEDULER, RECEIVER & BOOT AUTO-RECOVERY
// ============================================================================
object ReminderScheduler {
    private val motivationalQuotes = listOf(
        "Discipline compounds quietly—show up and execute right now.",
        "Every completed rep strengthens your armor. No shortcuts, pure focus.",
        "Your goals depend on today's work block. Start with 2 focused minutes.",
        "Guard the hour. Consistency turns ambition into reality."
    )

    fun scheduleReminder(
        context: Context,
        itemId: String,
        title: String,
        timeStr: String,
        isHabit: Boolean,
        isRecurring: Boolean = isHabit,
        customSoundUri: String = ""
    ) {
        if (timeStr.isBlank() || !timeStr.contains(":")) {
            cancelReminder(context, itemId)
            return
        }
        try {
            val parts = timeStr.split(":")
            val hour = parts[0].trim().toInt().coerceIn(0, 23)
            val minute = parts[1].trim().toInt().coerceIn(0, 59)

            var targetDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
            if (!targetDateTime.isAfter(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            scheduleExactMillis(context, itemId, title, timeStr, isHabit, isRecurring, customSoundUri, triggerMillis)
        } catch (_: Exception) {
        }
    }

    fun scheduleTestNotificationInSeconds(context: Context, seconds: Int = 5) {
        val triggerMillis = System.currentTimeMillis() + (seconds * 1000L)
        scheduleExactMillis(
            context = context,
            itemId = "alpha_test_alarm",
            title = "Notification Shield Verified!",
            timeStr = "",
            isHabit = true,
            isRecurring = false,
            customSoundUri = "",
            triggerMillis = triggerMillis
        )
    }

    private fun scheduleExactMillis(
        context: Context,
        itemId: String,
        title: String,
        timeStr: String,
        isHabit: Boolean,
        isRecurring: Boolean,
        customSoundUri: String,
        triggerMillis: Long
    ) {
        val intent = Intent(context, MotivationReminderReceiver::class.java).apply {
            putExtra("EXTRA_ID", itemId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_TIME", timeStr)
            putExtra("EXTRA_IS_HABIT", isHabit)
            putExtra("EXTRA_IS_RECURRING", isRecurring)
            putExtra("EXTRA_SOUND_URI", customSoundUri)
            putExtra("EXTRA_QUOTE", motivationalQuotes.random())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showAppIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode() + 7,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, showAppIntent), pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun rescheduleAllSavedAlarms(context: Context): Int {
        val storage = StudioStorageManager(context)
        var count = 0
        storage.loadHabitsOnly().forEach { h ->
            if (h.reminderTime.isNotBlank()) {
                scheduleReminder(context, h.id, h.title, h.reminderTime, isHabit = true, isRecurring = true, customSoundUri = h.customSoundUri)
                count++
            }
        }
        storage.loadTasksOnly().forEach { t ->
            if (t.reminderTime.isNotBlank() && (!t.isCompleted || t.isRecurring)) {
                scheduleReminder(context, t.id, t.title, t.reminderTime, isHabit = false, isRecurring = t.isRecurring, customSoundUri = t.customSoundUri)
                count++
            }
        }
        return count
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
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun checkHealth(context: Context): NotificationHealthStatus {
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val storage = StudioStorageManager(context)
        val count = storage.loadHabitsOnly().count { it.reminderTime.isNotBlank() } +
                storage.loadTasksOnly().count { it.reminderTime.isNotBlank() && (!it.isCompleted || it.isRecurring) }

        return NotificationHealthStatus(notifOk, exactOk, batteryOk, count)
    }
}

class MotivationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlphaHabit:ReminderWakeLock")
                wakeLock.acquire(12_000L)

                val id = intent.getStringExtra("EXTRA_ID") ?: UUID.randomUUID().toString()
                val title = intent.getStringExtra("EXTRA_TITLE") ?: "Time to Execute!"
                val quote = intent.getStringExtra("EXTRA_QUOTE") ?: "Honor your discipline today."
                val timeStr = intent.getStringExtra("EXTRA_TIME") ?: ""
                val customSoundUri = intent.getStringExtra("EXTRA_SOUND_URI") ?: ""
                val isHabit = intent.getBooleanExtra("EXTRA_IS_HABIT", true)
                val isRecurring = intent.getBooleanExtra("EXTRA_IS_RECURRING", isHabit)

                CelebrationSoundEngine.playCustomOrDefaultSound(context, customSoundUri, isReminder = true)

                val channelId = "alpha_studio_alerts_v6"
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Alpha Studio Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Exact habit & mission notifications that survive Doze mode"
                        enableVibration(true)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(channel)
                }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context, id.hashCode(), launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(if (isHabit) "⚡ Alpha Habit: $title" else "🎯 Alpha Mission: $title")
                    .setContentText(quote)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$quote\nTap to log progress and evolve your Sentinel."))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                nm.notify(id.hashCode(), notification)

                if (isRecurring && timeStr.isNotBlank()) {
                    ReminderScheduler.scheduleReminder(context, id, title, timeStr, isHabit, true, customSoundUri)
                }
                Thread.sleep(2400L)
            } catch (_: Exception) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }.start()
    }
}

class BootAndAlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            try {
                ReminderScheduler.rescheduleAllSavedAlarms(context)
                HabitWidgetProvider.updateAllWidgets(context)
                TaskWidgetProvider.updateAllTaskWidgets(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

// ============================================================================
// 8. HOME SCREEN WIDGETS (Habits + Auto-Renewing Tasks)
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
                        context, 0, Intent(context, MainActivity::class.java),
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
                            views.setInt(btnId, "setBackgroundResource", if (isDone) R.drawable.widget_btn_done else R.drawable.widget_btn_pending)
                            views.setTextColor(btnId, if (isDone) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#34D399"))

                            val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_TOGGLE_HABIT
                                putExtra(EXTRA_HABIT_ID, h.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context, h.id.hashCode(), toggleIntent,
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
            val resultPair = storage.toggleTaskDirectlyFromWidget(taskId)
            if (resultPair != null) {
                val (completedTitle, soundUri) = resultPair
                CelebrationSoundEngine.playCustomOrDefaultSound(context, soundUri, isReminder = false)
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

                    val openPending = PendingIntent.getActivity(
                        context, 1, Intent(context, MainActivity::class.java),
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
                            views.setInt(btnId, "setBackgroundResource", if (t.isCompleted) R.drawable.widget_btn_done else R.drawable.widget_btn_pending)
                            views.setTextColor(btnId, if (t.isCompleted) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#34D399"))

                            val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_TOGGLE_TASK
                                putExtra(EXTRA_TASK_ID, t.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context, t.id.hashCode(), toggleIntent,
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
// 9. ASYNC PERSISTENCE & GOOGLE DRIVE BACKUP ENGINE (Schema v8 with Goals)
// ============================================================================
class StudioStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("aura_studio_vault_v2", Context.MODE_PRIVATE)

    fun saveFullState(
        xp: Int,
        streakShields: Int,
        claimedQuests: Set<String> = emptySet(),
        googleEmail: String?,
        autoBackup: Boolean,
        readingTheme: SanctuaryReadingTheme = SanctuaryReadingTheme.WARM_SEPIA,
        readingFontScale: Float = 1.0f,
        habits: List<Habit>,
        todos: List<TodoTask>,
        goals: List<GoalItem>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): String {
        val root = serializeToJson(
            xp = xp,
            streakShields = streakShields,
            claimedQuests = claimedQuests,
            habits = habits,
            todos = todos,
            goals = goals,
            hourlyLogs = hourlyLogs,
            journals = journals,
            notes = notes
        )
        val compactJson = root.toString()
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        prefs.edit()
            .putString("master_state_json", compactJson)
            .putString("google_email", googleEmail)
            .putBoolean("auto_backup", autoBackup)
            .putString("reading_theme", readingTheme.name)
            .putFloat("reading_font_scale", readingFontScale)
            .putString("last_saved_time", nowFormatted)
            .apply()
        HabitWidgetProvider.updateAllWidgets(context)
        TaskWidgetProvider.updateAllTaskWidgets(context)
        return compactJson
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
                        customSoundUri = o.optString("customSoundUri", ""),
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
                val effectiveCompleted = if (isRec && rawCompleted && lastDate.isNotBlank() && lastDate != todayStr) false else rawCompleted
                result.add(
                    TodoTask(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category", "Personal"),
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }.getOrDefault(TodoPriority.P2_HIGH),
                        reminderTime = o.optString("reminderTime", ""),
                        customSoundUri = o.optString("customSoundUri", ""),
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

    fun toggleHabitDirectlyFromWidget(habitId: String): Pair<String, String>? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todayKey = LocalDate.now().toString()
            val habitsArr = root.optJSONArray("habits") ?: return null
            var completedHabitTitle: String? = null
            var soundUri = ""

            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                if (o.optString("id") == habitId) {
                    val target = o.optDouble("targetValue", 1.0).toFloat()
                    val step = o.optDouble("stepSize", 1.0).toFloat()
                    val type = o.optString("type", "CHECKLIST")
                    soundUri = o.optString("customSoundUri", "")
                    val logsObj = o.optJSONObject("logs") ?: JSONObject()
                    val currentVal = logsObj.optDouble(todayKey, 0.0).toFloat()

                    if (type == "CHECKLIST" || type == "QUIT_SHIELD") {
                        if (currentVal >= target) logsObj.remove(todayKey)
                        else {
                            logsObj.put(todayKey, target.toDouble())
                            completedHabitTitle = o.optString("title")
                        }
                    } else {
                        val nextVal = currentVal + step
                        logsObj.put(todayKey, nextVal.toDouble())
                        if (currentVal < target && nextVal >= target) completedHabitTitle = o.optString("title")
                    }
                    o.put("logs", logsObj)
                    break
                }
            }

            if (completedHabitTitle != null) root.put("xp", root.optInt("xp", 520) + 35)
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return completedHabitTitle?.let { it to soundUri }
        } catch (_: Exception) {
            return null
        }
    }

    fun toggleTaskDirectlyFromWidget(taskId: String): Pair<String, String>? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: return null
            val todayStr = LocalDate.now().toString()
            var newlyCompletedTitle: String? = null
            var soundUri = ""

            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                if (o.optString("id") == taskId) {
                    val nowDone = !o.optBoolean("isCompleted", false)
                    soundUri = o.optString("customSoundUri", "")
                    o.put("isCompleted", nowDone)
                    if (nowDone) {
                        o.put("lastCompletedDate", todayStr)
                        o.put("renewalStreak", o.optInt("renewalStreak", 0) + 1)
                        newlyCompletedTitle = o.optString("title")
                    }
                    break
                }
            }

            if (newlyCompletedTitle != null) root.put("xp", root.optInt("xp", 520) + 25)
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return newlyCompletedTitle?.let { it to soundUri }
        } catch (_: Exception) {
            return null
        }
    }

    fun exportBackupToUri(uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out -> out.write(jsonContent.toByteArray(Charsets.UTF_8)) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input -> BufferedReader(InputStreamReader(input)).readText() }
        } catch (_: Exception) {
            null
        }
    }

    fun serializeToJson(
        xp: Int,
        streakShields: Int,
        claimedQuests: Set<String>,
        habits: List<Habit>,
        todos: List<TodoTask>,
        goals: List<GoalItem>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 8)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("xp", xp)
        root.put("streakShields", streakShields)
        root.put("claimedQuests", JSONArray(claimedQuests.toList()))

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
                put("customSoundUri", h.customSoundUri)
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
                put("customSoundUri", t.customSoundUri)
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

        val goalsArr = JSONArray()
        goals.forEach { g ->
            val workArr = JSONArray()
            g.dailyLogs.forEach { wl ->
                workArr.put(JSONObject().apply {
                    put("id", wl.id)
                    put("date", wl.date)
                    put("workSummary", wl.workSummary)
                    put("progressDelta", wl.progressDelta.toDouble())
                    put("minutesSpent", wl.minutesSpent)
                })
            }
            goalsArr.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("emoji", g.emoji)
                put("category", g.category)
                put("description", g.description)
                put("deadlineIso", g.deadlineIso)
                put("targetValue", g.targetValue.toDouble())
                put("currentValue", g.currentValue.toDouble())
                put("unit", g.unit)
                put("colorHex", g.colorHex)
                put("dailyLogs", workArr)
                put("createdAt", g.createdAt)
            })
        }
        root.put("goals", goalsArr)

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
// 10. HIGH-PERFORMANCE STUDIO VIEWMODEL (Goals, Strict Character, Quests)
// ============================================================================
class HabitTrackerViewModel : ViewModel() {

    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val goals = mutableStateListOf<GoalItem>()
    val hourlyLogs = mutableStateListOf<HourlyLogItem>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()
    val claimedQuestKeys = mutableStateListOf<String>()

    val selectedTimeOfDay = mutableStateOf(TimeOfDay.ALL)
    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.STUDIO_CARDS)
    val selectedDate = mutableStateOf(LocalDate.now())

    val readingTheme = mutableStateOf(SanctuaryReadingTheme.WARM_SEPIA)
    val readingFontScale = mutableFloatStateOf(1.0f)

    val userXp = mutableIntStateOf(420)
    val streakShields = mutableIntStateOf(2)
    val customHeroQuote = mutableStateOf<String?>(null)

    val userLevel: Int get() = (userXp.intValue.coerceAtLeast(0) / 200) + 1
    val xpProgressInLevel: Float get() = ((userXp.intValue.coerceAtLeast(0)) % 200) / 200f
    val userRankTitle: String get() = cachedCharacterStats.value.stage.title

    // Cached state keeps UI smooth at 120fps with minimal CPU/battery load
    val cachedCharacterStats = mutableStateOf(
        AlphaCharacterStats(
            stage = CharacterStage.STAGE_2_SENTINEL,
            nextStage = CharacterStage.STAGE_3_WARDEN,
            stageProgressFraction = 0.2f,
            level = 3,
            combatPower = 780,
            strength = 50,
            intellect = 55,
            willpower = 55,
            vitality = 50,
            activeStreakBonus = 5,
            comboMultiplier = 1.0f,
            heroSpeechBubble = "“No shortcuts. Every earned rep refines the armor.”",
            nextUpgradeRequirementText = "Next armor tier at 900 XP or 7d streak"
        )
    )
    val cachedTodayScore = mutableIntStateOf(70)
    val cachedDailyQuests = mutableStateOf<List<DailyQuestStatus>>(emptyList())
    val notificationHealth = mutableStateOf(NotificationHealthStatus(true, true, true, 0))

    private var persistJob: Job? = null

    private val studioMotivationalPrinciples = listOf(
        "“No shortcuts exist in mastery—only the work you log today.”",
        "“Small, relentless daily wins forge an unbreakable identity.”",
        "“Protect your focus hours as fiercely as your long-term goals.”",
        "“Deadlines don't move for hesitation. Log one focused session now.”",
        "“Your character reflects your real discipline—keep earning every upgrade.”",
        "“Quiet consistency outperforms loud bursts of motivation every single week.”"
    )

    fun refreshCachedMetrics() {
        cachedCharacterStats.value = computeCharacterStats()
        cachedTodayScore.intValue = getDailyProductivityScore(LocalDate.now())
        cachedDailyQuests.value = computeDailyQuests()
    }

    fun refreshNotificationHealth(context: Context) {
        notificationHealth.value = ReminderScheduler.checkHealth(context)
    }

    fun triggerNewHeroMotivation(context: Context) {
        customHeroQuote.value = studioMotivationalPrinciples.random()
        refreshCachedMetrics()
        CelebrationSoundEngine.playMotivationalReminderSound(context)
    }

    fun claimDailyQuest(context: Context, quest: DailyQuestStatus) {
        if (!quest.isCompleted || quest.isClaimed) return
        val key = "${LocalDate.now()}_${quest.id}"
        if (!claimedQuestKeys.contains(key)) claimedQuestKeys.add(key)
        if (quest.grantsShield) streakShields.intValue += 1
        awardXpAndCelebrate(
            context = context,
            title = "${quest.emoji} Daily Quest Complete",
            subtitle = "${quest.title} • +${quest.rewardXp} XP${if (quest.grantsShield) " & +1 Shield" else ""}",
            xpEarned = quest.rewardXp
        )
    }

    // --- NEW GOAL ACHIEVEMENT OPERATIONS ---
    fun saveOrUpdateGoal(
        context: Context,
        existingId: String?,
        title: String,
        emoji: String,
        category: String,
        description: String,
        deadlineIso: String,
        targetValue: Float,
        unit: String,
        colorHex: Long
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val safeTarget = targetValue.coerceAtLeast(1f)

        if (existingId != null) {
            val idx = goals.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                goals[idx] = goals[idx].copy(
                    title = cleanTitle,
                    emoji = emoji.ifBlank { "🎯" },
                    category = category.ifBlank { "Career" },
                    description = description.trim(),
                    deadlineIso = deadlineIso,
                    targetValue = safeTarget,
                    unit = unit.ifBlank { "%" },
                    colorHex = colorHex
                )
                refreshCachedMetrics()
                persistStateAsync(context)
                return
            }
        }

        goals.add(
            0,
            GoalItem(
                title = cleanTitle,
                emoji = emoji.ifBlank { "🎯" },
                category = category.ifBlank { "Career" },
                description = description.trim(),
                deadlineIso = deadlineIso,
                targetValue = safeTarget,
                currentValue = 0f,
                unit = unit.ifBlank { "%" },
                colorHex = colorHex
            )
        )
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    fun logDailyGoalWork(
        context: Context,
        goalId: String,
        workSummary: String,
        progressDelta: Float,
        minutesSpent: Int,
        date: LocalDate = LocalDate.now()
    ) {
        val idx = goals.indexOfFirst { it.id == goalId }
        if (idx == -1) return
        val currentGoal = goals[idx]
        val wasDone = currentGoal.isCompleted

        val cleanSummary = workSummary.trim().ifBlank { "Daily progress logged" }
        val newLog = GoalWorkLog(
            date = date.toString(),
            workSummary = cleanSummary,
            progressDelta = progressDelta.coerceAtLeast(0f),
            minutesSpent = minutesSpent.coerceAtLeast(5)
        )
        val updatedLogs = listOf(newLog) + currentGoal.dailyLogs
        val updatedValue = (currentGoal.currentValue + progressDelta).coerceAtLeast(0f)
        val updatedGoal = currentGoal.copy(
            currentValue = updatedValue,
            dailyLogs = updatedLogs
        )
        goals[idx] = updatedGoal
        refreshCachedMetrics()

        val justFinished = !wasDone && updatedGoal.isCompleted
        awardXpAndCelebrate(
            context = context,
            title = if (justFinished) "🏆 GOAL ACHIEVED!" else "${updatedGoal.emoji} Goal Work Logged",
            subtitle = if (justFinished) "\"${updatedGoal.title}\" 100% completed!" else "+${progressDelta.toInt()} ${updatedGoal.unit} • ${updatedGoal.progressPercent}% total",
            xpEarned = if (justFinished) 120 else 40
        )
    }

    fun deleteGoalWorkLog(context: Context, goalId: String, logId: String) {
        val idx = goals.indexOfFirst { it.id == goalId }
        if (idx == -1) return
        val g = goals[idx]
        val removed = g.dailyLogs.find { it.id == logId } ?: return
        goals[idx] = g.copy(
            currentValue = (g.currentValue - removed.progressDelta).coerceAtLeast(0f),
            dailyLogs = g.dailyLogs.filterNot { it.id == logId }
        )
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    fun deleteGoal(context: Context, goalId: String) {
        goals.removeAll { it.id == goalId }
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    private fun computeDailyQuests(): List<DailyQuestStatus> {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val habitsDoneToday = habits.count { it.isCompletedOn(today) }
        val tasksDoneToday = todos.count { it.isCompleted && (it.lastCompletedDate == todayStr || it.date == todayStr) }
        val goalLogsToday = goals.sumOf { g -> g.dailyLogs.count { it.date == todayStr } }
        val productiveHoursToday = hourlyLogs.count {
            it.date == todayStr && (it.status == HourProductivityStatus.PEAK_FLOW || it.status == HourProductivityStatus.PRODUCTIVE)
        }

        return listOf(
            DailyQuestStatus(
                id = "q_goal",
                title = "Goal Builder",
                description = "Log daily work on 1 Goal",
                emoji = "🎯",
                current = goalLogsToday,
                target = 1,
                rewardXp = 55,
                grantsShield = false,
                isClaimed = claimedQuestKeys.contains("${todayStr}_q_goal")
            ),
            DailyQuestStatus(
                id = "q_habits",
                title = "Habit Striker",
                description = "Complete 2 habits today",
                emoji = "⚡",
                current = habitsDoneToday,
                target = 2,
                rewardXp = 50,
                grantsShield = false,
                isClaimed = claimedQuestKeys.contains("${todayStr}_q_habits")
            ),
            DailyQuestStatus(
                id = "q_tasks",
                title = "Mission Executor",
                description = "Finish 1 priority task",
                emoji = "✅",
                current = tasksDoneToday,
                target = 1,
                rewardXp = 40,
                grantsShield = false,
                isClaimed = claimedQuestKeys.contains("${todayStr}_q_tasks")
            ),
            DailyQuestStatus(
                id = "q_hours",
                title = "Time Guardian",
                description = "Log 2 productive hours",
                emoji = "⏱️",
                current = productiveHoursToday,
                target = 2,
                rewardXp = 55,
                grantsShield = true,
                isClaimed = claimedQuestKeys.contains("${todayStr}_q_hours")
            )
        )
    }

    /**
     * Strictly earned step-by-step armor evolution — zero manual upgrade shortcuts.
     */
    private fun computeCharacterStats(): AlphaCharacterStats {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val xp = userXp.intValue.coerceAtLeast(0)
        val lvl = userLevel
        var peakStreak = 0
        var totalStreakSum = 0
        var habitsDoneToday = 0

        for (h in habits) {
            val s = h.currentStreak(today)
            if (s > peakStreak) peakStreak = s
            totalStreakSum += s
            if (h.isCompletedOn(today)) habitsDoneToday++
        }

        val goalsWorkedToday = goals.count { it.workedToday(today) }
        val totalGoalWorkSessions = goals.sumOf { it.dailyLogs.size }
        val goalsCompleted = goals.count { it.isCompleted }

        val combo = when {
            habitsDoneToday + goalsWorkedToday >= 4 -> 2.0f
            habitsDoneToday + goalsWorkedToday >= 2 -> 1.5f
            habitsDoneToday + goalsWorkedToday >= 1 -> 1.25f
            else -> 1.0f
        }

        val wastedToday = hourlyLogs.count { it.date == todayStr && (it.status == HourProductivityStatus.TIME_WASTED || it.status == HourProductivityStatus.LOW_FOCUS) }
        val flowToday = hourlyLogs.count { it.date == todayStr && it.status == HourProductivityStatus.PEAK_FLOW }

        val stages = CharacterStage.entries
        var currentStage = stages.first()
        for (st in stages) {
            if (xp >= st.minXpRequired && peakStreak >= st.minStreakRequired) {
                currentStage = st
            } else if (xp >= st.minXpRequired + 350) {
                // High XP consistency can also unlock the next stage
                currentStage = st
            }
        }

        val nextIdx = stages.indexOf(currentStage) + 1
        val nextStage = if (nextIdx < stages.size) stages[nextIdx] else null
        val stageProgress = if (nextStage == null) {
            1f
        } else {
            val span = (nextStage.minXpRequired - currentStage.minXpRequired).coerceAtLeast(1)
            ((xp - currentStage.minXpRequired).toFloat() / span.toFloat()).coerceIn(0.05f, 1f)
        }

        val nextReqText = if (nextStage == null) {
            "Maximum Ascended Armor Achieved"
        } else {
            val xpNeeded = (nextStage.minXpRequired - xp).coerceAtLeast(0)
            "Next Armor (${nextStage.title}): ${if (xpNeeded > 0) "$xpNeeded XP needed" else "XP Ready"} • ${nextStage.minStreakRequired}d streak"
        }

        val strLogs = habits.filter { it.category.contains("Fitness", true) || it.category.contains("Health", true) }.sumOf { it.logs.size }
        val intLogs = habits.filter { it.category.contains("Career", true) || it.category.contains("Growth", true) }.sumOf { it.logs.size } + (notes.size * 4) + (flowToday * 4) + (totalGoalWorkSessions * 3)
        val wilLogs = totalStreakSum * 3 + todos.count { it.isCompleted } * 3 + goalsCompleted * 15 - (wastedToday * 5)
        val vitLogs = journals.size * 4 + habits.size * 3

        val str = (25 + strLogs * 2 + lvl * 4).coerceAtLeast(10)
        val intStat = (30 + intLogs * 2 + lvl * 4).coerceAtLeast(10)
        val wil = (28 + wilLogs + lvl * 5).coerceAtLeast(10)
        val vit = (25 + vitLogs * 2 + lvl * 3).coerceAtLeast(10)
        val power = ((str + intStat + wil + vit) * 3 + xp).coerceAtLeast(100)

        val dynamicBubble = customHeroQuote.value ?: when {
            wastedToday > 0 && flowToday == 0 ->
                "“$wastedToday unproductive hour(s) logged today. Reclaim the next hour with deep focus.”"
            goalsWorkedToday > 0 ->
                "“Goal work verified today. Your ${currentStage.suitName} is forging stronger step by step.”"
            combo >= 1.5f ->
                "“${combo}x execution combo active. Pure discipline—no shortcuts.”"
            else ->
                "“${currentStage.title} standing by. Complete habits, tasks & goal sessions to unlock ${nextStage?.title ?: "mastery"}.”"
        }

        return AlphaCharacterStats(
            stage = currentStage,
            nextStage = nextStage,
            stageProgressFraction = stageProgress,
            level = lvl,
            combatPower = power,
            strength = str,
            intellect = intStat,
            willpower = wil,
            vitality = vit,
            activeStreakBonus = totalStreakSum,
            comboMultiplier = combo,
            heroSpeechBubble = dynamicBubble,
            nextUpgradeRequirementText = nextReqText
        )
    }

    fun getDailyProductivityScore(date: LocalDate = selectedDate.value): Int {
        val dateStr = date.toString()
        val habitsDone = habits.count { it.isCompletedOn(date) }
        val habitBase = if (habits.isEmpty()) 45 else ((habitsDone.toFloat() / habits.size) * 45f).toInt()
        val tasksBonus = (todos.count { it.isCompleted } * 6).coerceAtMost(24)
        val goalBonus = if (goals.any { it.workedToday(date) }) 15 else 0
        val hourlyDelta = hourlyLogs.filter { it.date == dateStr }.sumOf { it.status.scoreDelta }
        return (16 + habitBase + tasksBonus + goalBonus + hourlyDelta).coerceIn(0, 100)
    }

    fun getDailyProductivityGrade(score: Int): String = when {
        score >= 90 -> "S+ SUPREME"
        score >= 80 -> "A • EXCELLENT"
        score >= 65 -> "B • SOLID"
        score >= 45 -> "C • AVERAGE"
        else -> "D • NEEDS FOCUS"
    }

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

        if (existingIdx != -1) hourlyLogs[existingIdx] = newItem else hourlyLogs.add(newItem)
        hourlyLogs.sortBy { it.hour24 }

        val netXpChange = status.xpDelta - previousXpDelta
        userXp.intValue = (userXp.intValue + netXpChange).coerceAtLeast(0)
        refreshCachedMetrics()

        if (status.xpDelta > 0) {
            awardXpAndCelebrate(
                context = context,
                title = "⚡ Productive Hour Logged",
                subtitle = "${newItem.formattedHourRange()} • ${status.shortBadge} earned",
                xpEarned = 0
            )
        } else if (status.xpDelta < 0) {
            CelebrationSoundEngine.playPenaltyDrainSound(context)
            Toast.makeText(context, "⚠️ ${status.xpDelta} XP Penalty! Unproductive hour deducted.", Toast.LENGTH_LONG).show()
            persistStateAsync(context)
        } else {
            persistStateAsync(context)
        }
    }

    fun deleteHourlyLog(context: Context, id: String) {
        val idx = hourlyLogs.indexOfFirst { it.id == id }
        if (idx != -1) {
            val removed = hourlyLogs.removeAt(idx)
            userXp.intValue = (userXp.intValue - removed.status.xpDelta).coerceAtLeast(0)
            refreshCachedMetrics()
            persistStateAsync(context)
        }
    }

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
            refreshCachedMetrics()
            persistStateAsync(context)
            if (forceRenewForTesting) {
                Toast.makeText(context, "🔄 $renewedCount Auto-Renewing task(s) refreshed!", Toast.LENGTH_SHORT).show()
            }
        } else if (forceRenewForTesting) {
            Toast.makeText(context, "Complete a 🔄 Daily Auto-Renew task first to test renewal!", Toast.LENGTH_SHORT).show()
        }
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
        val appContext = context.applicationContext
        val storage = StudioStorageManager(appContext)
        connectedGoogleEmail.value = storage.getGoogleEmail()
        lastCloudBackup.value = storage.getLastBackupTime()
        autoCloudSync.value = storage.isAutoBackup()
        readingTheme.value = storage.getReadingTheme()
        readingFontScale.floatValue = storage.getReadingFontScale()

        val savedJson = storage.getSavedJson()
        if (!savedJson.isNullOrBlank()) {
            restoreFromJsonString(savedJson)
            isInitialized = true
            checkAndAutoRenewDailyTasks(appContext, forceRenewForTesting = false)
            if (goals.isEmpty()) seedStarterGoals()
        } else if (!isInitialized) {
            isInitialized = true
            seedStarterTemplates()
            persistStateAsync(appContext)
        }
        refreshCachedMetrics()
        refreshNotificationHealth(appContext)

        viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.rescheduleAllSavedAlarms(appContext)
        }
    }

    fun persistStateAsync(context: Context) {
        val appContext = context.applicationContext
        val snapshotXp = userXp.intValue
        val snapshotShields = streakShields.intValue
        val snapshotQuests = claimedQuestKeys.toSet()
        val snapshotEmail = connectedGoogleEmail.value
        val snapshotAuto = autoCloudSync.value
        val snapshotTheme = readingTheme.value
        val snapshotScale = readingFontScale.floatValue
        val snapshotHabits = habits.toList()
        val snapshotTodos = todos.toList()
        val snapshotGoals = goals.toList()
        val snapshotHourly = hourlyLogs.toList()
        val snapshotJournals = journals.toList()
        val snapshotNotes = notes.toList()

        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250L)
            val storage = StudioStorageManager(appContext)
            storage.saveFullState(
                xp = snapshotXp,
                streakShields = snapshotShields,
                claimedQuests = snapshotQuests,
                googleEmail = snapshotEmail,
                autoBackup = snapshotAuto,
                readingTheme = snapshotTheme,
                readingFontScale = snapshotScale,
                habits = snapshotHabits,
                todos = snapshotTodos,
                goals = snapshotGoals,
                hourlyLogs = snapshotHourly,
                journals = snapshotJournals,
                notes = snapshotNotes
            )
            lastCloudBackup.value = storage.getLastBackupTime()
        }
    }

    fun buildBackupJsonNow(context: Context): String {
        val storage = StudioStorageManager(context.applicationContext)
        return storage.saveFullState(
            xp = userXp.intValue,
            streakShields = streakShields.intValue,
            claimedQuests = claimedQuestKeys.toSet(),
            googleEmail = connectedGoogleEmail.value,
            autoBackup = autoCloudSync.value,
            readingTheme = readingTheme.value,
            readingFontScale = readingFontScale.floatValue,
            habits = habits.toList(),
            todos = todos.toList(),
            goals = goals.toList(),
            hourlyLogs = hourlyLogs.toList(),
            journals = journals.toList(),
            notes = notes.toList()
        )
    }

    fun restoreFromJsonString(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            userXp.intValue = root.optInt("xp", 420)
            streakShields.intValue = root.optInt("streakShields", 2)

            claimedQuestKeys.clear()
            root.optJSONArray("claimedQuests")?.let { arr ->
                for (i in 0 until arr.length()) claimedQuestKeys.add(arr.optString(i))
            }

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
                        customSoundUri = o.optString("customSoundUri", ""),
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
                        customSoundUri = o.optString("customSoundUri", ""),
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

            goals.clear()
            val goalsArr = root.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until goalsArr.length()) {
                val o = goalsArr.getJSONObject(i)
                val workLogs = mutableListOf<GoalWorkLog>()
                val wArr = o.optJSONArray("dailyLogs") ?: JSONArray()
                for (j in 0 until wArr.length()) {
                    val w = wArr.getJSONObject(j)
                    workLogs.add(
                        GoalWorkLog(
                            id = w.optString("id", UUID.randomUUID().toString()),
                            date = w.optString("date", LocalDate.now().toString()),
                            workSummary = w.optString("workSummary", ""),
                            progressDelta = w.optDouble("progressDelta", 0.0).toFloat(),
                            minutesSpent = w.optInt("minutesSpent", 30)
                        )
                    )
                }
                goals.add(
                    GoalItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title"),
                        emoji = o.optString("emoji", "🎯"),
                        category = o.optString("category", "Career"),
                        description = o.optString("description", ""),
                        deadlineIso = o.optString("deadlineIso", LocalDate.now().plusDays(30).toString()),
                        targetValue = o.optDouble("targetValue", 100.0).toFloat(),
                        currentValue = o.optDouble("currentValue", 0.0).toFloat(),
                        unit = o.optString("unit", "%"),
                        colorHex = o.optLong("colorHex", 0xFF38BDF8),
                        dailyLogs = workLogs,
                        createdAt = o.optString("createdAt", LocalDate.now().toString())
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
                o.optJSONArray("actionItems")?.let { arr -> for (k in 0 until arr.length()) actions.add(arr.optString(k)) }
                val photos = mutableListOf<String>()
                o.optJSONArray("photoUris")?.let { arr -> for (k in 0 until arr.length()) photos.add(arr.optString(k)) }
                val tags = mutableListOf<String>()
                o.optJSONArray("tags")?.let { arr -> for (k in 0 until arr.length()) tags.add(arr.optString(k)) }
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
                o.optJSONArray("keyTakeaways")?.let { arr -> for (k in 0 until arr.length()) takeaways.add(arr.optString(k)) }
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
            refreshCachedMetrics()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun awardXpAndCelebrate(
        context: Context,
        title: String,
        subtitle: String,
        xpEarned: Int = 25,
        customSoundUri: String = ""
    ) {
        val oldStage = cachedCharacterStats.value.stage
        val oldLevel = userLevel
        val combo = cachedCharacterStats.value.comboMultiplier
        val boostedXp = if (xpEarned > 0) (xpEarned * combo).toInt() else 0

        userXp.intValue += boostedXp
        refreshCachedMetrics()

        val newLevel = userLevel
        val newStage = cachedCharacterStats.value.stage
        val leveledUp = newLevel > oldLevel || newStage != oldStage
        if (leveledUp) streakShields.intValue += 1

        celebrationTitle.value = when {
            newStage != oldStage -> "ARMOR EVOLVED: ${newStage.title.uppercase()}"
            newLevel > oldLevel -> "LEVEL $newLevel ACHIEVED"
            else -> title
        }
        celebrationSubtitle.value = when {
            newStage != oldStage -> "Unlocked ${newStage.suitName} • ${newStage.armorUpgradeDetail}"
            newLevel > oldLevel -> "Rank: ${newStage.title} • Earned through discipline"
            else -> subtitle
        }
        celebrationXpEarned.intValue = if (boostedXp == 0) 25 else boostedXp
        showCelebration.value = true

        CelebrationSoundEngine.playCustomOrDefaultSound(context, customSoundUri, isReminder = false, isLevelUp = leveledUp)
        persistStateAsync(context)

        viewModelScope.launch {
            delay(2600)
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
                var habitCustomSound = ""
                activeFocusHabitId.value?.let { id ->
                    habits.find { it.id == id }?.let { h ->
                        habitCustomSound = h.customSoundUri
                        setHabitMeasurement(context, h, h.valueOn(LocalDate.now()) + mins)
                    }
                }
                awardXpAndCelebrate(
                    context = context,
                    title = "⏱️ Focus Block Complete",
                    subtitle = "${activeFocusTitle.value} finished cleanly",
                    xpEarned = 45,
                    customSoundUri = habitCustomSound
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
        refreshCachedMetrics()

        if (!wasDoneBefore && updated.isCompletedOn(date)) {
            awardXpAndCelebrate(
                context = context,
                title = "${updated.emoji} ${updated.title} Complete",
                subtitle = "🔥 ${updated.currentStreak()}-day streak • Armor progression increased",
                xpEarned = 35,
                customSoundUri = updated.customSoundUri
            )
        } else {
            persistStateAsync(context)
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
        refreshCachedMetrics()

        if (!wasDone && updated.isCompletedOn(date)) {
            awardXpAndCelebrate(
                context = context,
                title = "${updated.emoji} Target Reached",
                subtitle = "$newAmount ${updated.unit} logged for ${updated.title}",
                xpEarned = 35,
                customSoundUri = updated.customSoundUri
            )
        } else {
            persistStateAsync(context)
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
        reminderTime: String,
        customSoundUri: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        if (customSoundUri.isNotBlank()) {
            try {
                context.contentResolver.takePersistableUriPermission(Uri.parse(customSoundUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }

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
                    reminderTime = reminderTime.trim(),
                    customSoundUri = customSoundUri.trim()
                )
                habits[idx] = updated
                ReminderScheduler.scheduleReminder(context, updated.id, updated.title, updated.reminderTime, isHabit = true, isRecurring = true, customSoundUri = updated.customSoundUri)
                refreshCachedMetrics()
                refreshNotificationHealth(context)
                persistStateAsync(context)
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
            reminderTime = reminderTime.trim(),
            customSoundUri = customSoundUri.trim()
        )
        habits.add(0, newHabit)
        ReminderScheduler.scheduleReminder(context, newHabit.id, newHabit.title, newHabit.reminderTime, isHabit = true, isRecurring = true, customSoundUri = newHabit.customSoundUri)
        refreshCachedMetrics()
        refreshNotificationHealth(context)
        persistStateAsync(context)
    }

    fun deleteHabit(context: Context, habitId: String) {
        ReminderScheduler.cancelReminder(context, habitId)
        habits.removeAll { it.id == habitId }
        refreshCachedMetrics()
        refreshNotificationHealth(context)
        persistStateAsync(context)
    }

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
        refreshCachedMetrics()

        if (nowDone) {
            val renewNote = if (task.isRecurring) " • 🔄 Auto-renews tomorrow" else ""
            awardXpAndCelebrate(
                context = context,
                title = "🎯 Mission Complete",
                subtitle = "\"${task.title}\"$renewNote",
                xpEarned = task.priority.xpReward,
                customSoundUri = task.customSoundUri
            )
        } else {
            persistStateAsync(context)
        }
    }

    fun toggleSubTask(context: Context, task: TodoTask, subTaskId: String) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val updatedSubs = task.subTasks.map { if (it.id == subTaskId) it.copy(isDone = !it.isDone) else it }
        val allDone = updatedSubs.isNotEmpty() && updatedSubs.all { it.isDone }
        val wasDone = task.isCompleted
        val todayStr = LocalDate.now().toString()

        todos[idx] = task.copy(
            subTasks = updatedSubs,
            isCompleted = allDone,
            lastCompletedDate = if (allDone) todayStr else task.lastCompletedDate,
            renewalStreak = if (!wasDone && allDone) task.renewalStreak + 1 else task.renewalStreak
        )
        refreshCachedMetrics()

        if (!wasDone && allDone) {
            awardXpAndCelebrate(
                context = context,
                title = "✅ All Subtasks Complete",
                subtitle = task.title,
                xpEarned = task.priority.xpReward,
                customSoundUri = task.customSoundUri
            )
        } else {
            persistStateAsync(context)
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
        isRecurring: Boolean,
        customSoundUri: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        if (customSoundUri.isNotBlank()) {
            try {
                context.contentResolver.takePersistableUriPermission(Uri.parse(customSoundUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }

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
                    customSoundUri = customSoundUri.trim(),
                    estimatedMinutes = estimatedMinutes,
                    subTasks = mergedSubs,
                    isRecurring = isRecurring
                )
                todos[idx] = updated
                ReminderScheduler.scheduleReminder(context, updated.id, updated.title, updated.reminderTime, isHabit = false, isRecurring = updated.isRecurring, customSoundUri = updated.customSoundUri)
                refreshCachedMetrics()
                refreshNotificationHealth(context)
                persistStateAsync(context)
                return
            }
        }

        val newTask = TodoTask(
            title = cleanTitle,
            category = category.ifBlank { "Personal" },
            priority = priority,
            reminderTime = reminderTime.trim(),
            customSoundUri = customSoundUri.trim(),
            estimatedMinutes = estimatedMinutes,
            subTasks = subTaskTitles.filter { it.isNotBlank() }.map { SubTask(title = it.trim()) },
            isRecurring = isRecurring
        )
        todos.add(0, newTask)
        ReminderScheduler.scheduleReminder(context, newTask.id, newTask.title, newTask.reminderTime, isHabit = false, isRecurring = newTask.isRecurring, customSoundUri = newTask.customSoundUri)
        refreshCachedMetrics()
        refreshNotificationHealth(context)
        persistStateAsync(context)
    }

    fun deleteTodoTask(context: Context, id: String) {
        ReminderScheduler.cancelReminder(context, id)
        todos.removeAll { it.id == id }
        refreshCachedMetrics()
        refreshNotificationHealth(context)
        persistStateAsync(context)
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
                context.contentResolver.takePersistableUriPermission(Uri.parse(uriStr), Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
                refreshCachedMetrics()
                persistStateAsync(context)
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
                todos.add(0, TodoTask(title = act.trim(), category = "Reflection Action", priority = TodoPriority.P1_CRITICAL))
            }
        }

        refreshCachedMetrics()
        awardXpAndCelebrate(context, "📓 Reflection Logged", "Clarity strengthens daily execution", xpEarned = 35)
    }

    fun deleteJournalEntry(context: Context, id: String) {
        journals.removeAll { it.id == id }
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    fun saveOrUpdateNotebookEntry(context: Context, existingId: String?, entry: NotebookEntry) {
        if (existingId != null) {
            val idx = notes.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                notes[idx] = entry.copy(id = existingId)
                refreshCachedMetrics()
                persistStateAsync(context)
                return
            }
        }
        notes.add(0, entry)
        refreshCachedMetrics()
        awardXpAndCelebrate(context, "🧠 Saved to Vault", "\"${entry.title}\" indexed", xpEarned = 35)
    }

    fun togglePinNote(context: Context, id: String) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx != -1) {
            notes[idx] = notes[idx].copy(isPinned = !notes[idx].isPinned)
            persistStateAsync(context)
        }
    }

    fun deleteNotebookEntry(context: Context, id: String) {
        notes.removeAll { it.id == id }
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    fun syncToGoogleCloud(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingCloud.value = true
            delay(500)
            buildBackupJsonNow(context)
            isSyncingCloud.value = false
        }
    }

    private fun seedStarterGoals() {
        val today = LocalDate.now()
        goals.addAll(
            listOf(
                GoalItem(
                    title = "Master Cloud Architecture & Portfolio",
                    emoji = "☁️",
                    category = "Career",
                    description = "Build production labs, sharpen hands-on projects, and hit target interview readiness.",
                    deadlineIso = today.plusDays(45).toString(),
                    targetValue = 60f,
                    currentValue = 18f,
                    unit = "hrs",
                    colorHex = 0xFF38BDF8,
                    dailyLogs = listOf(
                        GoalWorkLog(
                            date = today.minusDays(1).toString(),
                            workSummary = "Configured multi-tier network architecture & documented diagrams",
                            progressDelta = 2f,
                            minutesSpent = 120
                        )
                    )
                ),
                GoalItem(
                    title = "Lean Athletic Conditioning Target",
                    emoji = "🏋️",
                    category = "Fitness",
                    description = "Consistent heavy strength sessions + sprint conditioning.",
                    deadlineIso = today.plusDays(60).toString(),
                    targetValue = 40f,
                    currentValue = 12f,
                    unit = "sessions",
                    colorHex = 0xFF34D399,
                    dailyLogs = listOf(
                        GoalWorkLog(
                            date = today.minusDays(1).toString(),
                            workSummary = "Upper body pressing progression + 15m sprint intervals",
                            progressDelta = 1f,
                            minutesSpent = 65
                        )
                    )
                )
            )
        )
    }

    private fun seedStarterTemplates() {
        val today = LocalDate.now()
        val todayStr = today.toString()

        val deepWorkLogs = mutableMapOf<String, Float>()
        for (i in 1..14) {
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
        for (i in 1..14) {
            if (i % 2 == 1) strengthLogs[today.minusDays(i.toLong()).toString()] = 1f
        }
        habits.add(
            Habit(
                title = "Strength & Conditioning",
                emoji = "🏋️",
                category = "Fitness",
                timeOfDay = TimeOfDay.EVENING,
                habitStackingCue = "6:00 PM training ritual",
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
        for (i in 1..10) waterLogs[today.minusDays(i.toLong()).toString()] = 3.0f
        waterLogs[todayStr] = 1.5f
        habits.add(
            Habit(
                title = "Hydration Protocol",
                emoji = "💧",
                category = "Health",
                timeOfDay = TimeOfDay.ALL,
                habitStackingCue = "Keep 1L bottle on desk",
                type = HabitType.MEASUREMENT,
                colorHex = 0xFF38BDF8,
                targetValue = 3.0f,
                unit = "Liters",
                stepSize = 0.5f,
                reminderTime = "11:00",
                logs = waterLogs
            )
        )

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
                        SubTask(title = "Review goal deadlines", isDone = true),
                        SubTask(title = "Protect 90m maker block")
                    )
                ),
                TodoTask(
                    title = "Evening Language & Reading Drill",
                    category = "Growth",
                    priority = TodoPriority.P2_HIGH,
                    reminderTime = "21:00",
                    isRecurring = true,
                    renewalStreak = 10,
                    estimatedMinutes = 30
                )
            )
        )

        seedStarterGoals()

        hourlyLogs.addAll(
            listOf(
                HourlyLogItem(date = todayStr, hour24 = 7, activityNote = "Morning hydration, sunlight & mobility", status = HourProductivityStatus.PRODUCTIVE),
                HourlyLogItem(date = todayStr, hour24 = 8, activityNote = "90m Deep Work sprint with zero phone checks", status = HourProductivityStatus.PEAK_FLOW)
            )
        )

        journals.add(
            JournalEntry(
                title = "Momentum & Daily Clarity",
                moodEmoji = "🔥",
                energyScore = 9,
                gratitudeOrWins = "Locked in a distraction-free morning block and logged goal progress.",
                dailyStory = "Tracking every goal deadline alongside hourly execution makes progress tangible and keeps discipline sharp.",
                frictionOrMistakes = "Checked notifications briefly before lunch.",
                actionItems = listOf("Keep phone on Do Not Disturb during focus sprints")
            )
        )

        notes.add(
            NotebookEntry(
                category = NoteCategory.SECOND_BRAIN,
                title = "Compounding Execution Systems",
                authorOrSubtitle = "Personal Playbook",
                rating = 5,
                readingProgress = 100,
                isPinned = true,
                coverColorHex = 0xFF34D399,
                favoriteQuote = "Consistent systems outlast temporary bursts of willpower.",
                keyTakeaways = listOf(
                    "Break ambitious deadlines into measurable daily work logs.",
                    "Track both productive hours and wasted hours honestly to sharpen awareness.",
                    "Protect high-energy morning hours for your highest-leverage goal."
                ),
                detailedNotes = "Reviewing distilled principles in Sanctuary Mode keeps evening study calm and focused.",
                flashcards = listOf(
                    FlashcardItem(
                        question = "What turns a long-term deadline into predictable results?",
                        answer = "Daily measurable work logs combined with honest hourly audits."
                    )
                )
            )
        )
    }
}
