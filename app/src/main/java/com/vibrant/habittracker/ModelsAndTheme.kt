package com.vibrant.habittracker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

// ============================================================================
// TITANIUM OBSIDIAN COLOR SYSTEM & SANCTUARY READING THEMES
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

    val bgColor: Color get() = Color(bgHex)
    val cardColor: Color get() = Color(cardHex)
    val borderColor: Color get() = Color(borderHex)
    val textPrimary: Color get() = Color(primaryTextHex)
    val textSecondary: Color get() = Color(secondaryTextHex)
    val accent: Color get() = Color(accentHex)
}

// ============================================================================
// REACTIVE BRAIN MOOD & WIDGET MODELS
// ============================================================================
enum class BrainMoodState(
    val label: String,
    val badgeText: String
) {
    RADIANT_HAPPY("Peak Mind", "THRIVING"),
    FOCUSED_GOOD("Locked In", "ON TRACK"),
    TIRED_SLIPPING("Slipping", "HURRY UP"),
    ROTTEN_SAD("Exhausted", "AT RISK")
}

enum class BrainWidgetColorPref(val label: String) {
    AUTO_DYNAMIC("Auto (Blue → Purple → Green)"),
    EMERALD_GREEN("Emerald Green"),
    SAPPHIRE_BLUE("Sapphire Blue"),
    COSMIC_PURPLE("Cosmic Purple")
}

@Immutable
data class BrainMoodSnapshot(
    val mood: BrainMoodState,
    val completedCount: Int,
    val totalCount: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val tasksDone: Int,
    val tasksTotal: Int,
    val ratioText: String,
    val subtitleText: String,
    val headlineText: String,
    val breakdownText: String,
    val bgStartHex: Long,
    val bgEndHex: Long,
    val borderHex: Long,
    val drawableResId: Int
)

// ============================================================================
// DARK KNIGHT SENTINEL EVOLUTION, DAILY QUESTS & SYSTEM HEALTH
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
    val allHealthy: Boolean
        get() = notificationsGranted && exactAlarmsGranted && batteryUnrestricted
}

// ============================================================================
// GOALS, HOURLY AUDIT, HABITS, TASKS, JOURNAL & VAULT MODELS
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

    val progressPercent: Int get() = (progressFraction * 100f).toInt()

    val isCompleted: Boolean get() = currentValue >= targetValue && targetValue > 0f

    fun daysRemaining(today: LocalDate = LocalDate.now()): Long {
        return runCatching {
            ChronoUnit.DAYS.between(today, LocalDate.parse(deadlineIso))
        }.getOrDefault(0L)
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

    val color: Color get() = Color(colorHex)
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

enum class TodoPriority(
    val label: String,
    val shortTag: String,
    val colorHex: Long,
    val xpReward: Int
) {
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

enum class AmbientSoundMode(val label: String, val icon: String) {
    OFF("Silent", "🔇"),
    SOFT_RAIN("Soft Rain", "🌧️"),
    CALM_DRONE("432Hz Calm", "🌊")
}

internal fun Float.cleanStr(): String {
    return if (this % 1f == 0f) {
        this.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
}
