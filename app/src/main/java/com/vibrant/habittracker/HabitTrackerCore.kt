package com.vibrant.habittracker

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
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
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// ============================================================================
// 1. STUDIO COLOR SYSTEM (OBSIDIAN GLASS & LUMINOUS NEON PALETTE)
// ============================================================================
object AppColors {
    val BgDeep = Color(0xFF07090E)
    val BgSurface = Color(0xFF0F131C)
    val BgCard = Color(0xFF151B26)
    val BgCardElevated = Color(0xFF1E2636)
    val BorderSubtle = Color(0xFF283246)

    val EmeraldNeon = Color(0xFF10B981)
    val MintGlow = Color(0xFF34D399)
    val ElectricViolet = Color(0xFFA855F7)
    val CyberCyan = Color(0xFF06B6D4)
    val SunsetCoral = Color(0xFFF97316)
    val CrimsonRose = Color(0xFFF43F5E)
    val SolarGold = Color(0xFFF59E0B)
    val IndigoPulse = Color(0xFF6366F1)

    // Backward-compatible aliases
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

// ============================================================================
// 2. STUDIO DATA MODELS
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

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "⚡",
    val category: String = "Growth",
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    val habitStackingCue: String = "", // e.g., "After morning coffee"
    val type: HabitType = HabitType.CHECKLIST,
    val colorHex: Long = 0xFF34D399,
    val targetValue: Float = 1f,
    val unit: String = "times",
    val stepSize: Float = 1f,
    val logs: Map<String, Float> = emptyMap(), // YYYY-MM-DD -> value
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
            val d = today.minusDays(i.toLong())
            if (isCompletedOn(d)) {
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
}

enum class TodoPriority(val label: String, val colorHex: Long, val xpReward: Int) {
    P1_CRITICAL("P1 • Critical", 0xFFF43F5E, 35),
    P2_HIGH("P2 • High", 0xFFF97316, 25),
    P3_MEDIUM("P3 • Medium", 0xFF06B6D4, 15),
    P4_LOW("P4 • Routine", 0xFF94A3B8, 10)
}

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "Personal",
    val priority: TodoPriority = TodoPriority.P2_HIGH,
    val date: String = LocalDate.now().toString(),
    val timeBlock: String = "Anytime",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val estimatedMinutes: Int = 25,
    val subTasks: List<SubTask> = emptyList(),
    val notes: String = ""
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
    val isoDate: String = LocalDate.now().toString(),
    val title: String = "Daily Reflection",
    val moodEmoji: String = "🌟",
    val energyScore: Int = 8, // 1..10
    val gratitudeOrWins: String,
    val dailyStory: String,
    val frictionOrMistakes: String,
    val actionItems: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class NoteCategory(val label: String) {
    BOOK_SUMMARY("Book Summary"),
    SECOND_BRAIN("Concept / Note"),
    PROJECT_PLAN("Blueprint")
}

data class FlashcardItem(
    val question: String,
    val answer: String
)

data class NotebookEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: NoteCategory = NoteCategory.BOOK_SUMMARY,
    val title: String,
    val authorOrSubtitle: String,
    val rating: Int = 5,
    val readingProgress: Int = 100, // 0..100%
    val isPinned: Boolean = false,
    val coverColorHex: Long = 0xFFA855F7,
    val favoriteQuote: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val detailedNotes: String = "",
    val flashcards: List<FlashcardItem> = emptyList(),
    val updatedAt: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
)

// ============================================================================
// 3. SYNTHESIZED STUDIO SOUND & HAPTIC ENGINE
// ============================================================================
object CelebrationSoundEngine {

    fun playCelebrationFanfare(context: Context, isLevelUp: Boolean = false) {
        triggerRichHaptics(context, isLevelUp)
        Thread {
            try {
                val sampleRate = 44100
                val durationSeconds = if (isLevelUp) 1.75 else 1.35
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                // Lush Studio Major-9th Arpeggio + Shimmer Harmonics
                val notes = if (isLevelUp) {
                    listOf(
                        Triple(523.25, 0.00, 0.45), // C5
                        Triple(659.25, 0.08, 0.45), // E5
                        Triple(783.99, 0.16, 0.50), // G5
                        Triple(987.77, 0.24, 0.55), // B5
                        Triple(1046.50, 0.32, 0.70), // C6
                        Triple(1318.51, 0.42, 0.85), // E6
                        Triple(1567.98, 0.52, 1.15)  // G6
                    )
                } else {
                    listOf(
                        Triple(587.33, 0.00, 0.40), // D5
                        Triple(739.99, 0.08, 0.45), // F#5
                        Triple(880.00, 0.16, 0.50), // A5
                        Triple(1174.66, 0.25, 0.85), // D6
                        Triple(1479.98, 0.35, 0.90)  // F#6
                    )
                }

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0
                    for ((freq, start, dur) in notes) {
                        if (t >= start && t <= start + dur) {
                            val localT = t - start
                            val attack = (localT / 0.012).coerceAtMost(1.0)
                            val decay = exp(-3.6 * localT)
                            val wave = sin(2.0 * PI * freq * localT) +
                                    0.38 * sin(2.0 * PI * freq * 2.0 * localT) +
                                    0.18 * sin(2.0 * PI * freq * 3.0 * localT)
                            sample += wave * attack * decay * 0.23
                        }
                    }
                    buffer[i] = (sample.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                Thread.sleep((durationSeconds * 1000).toLong() + 150)
                track.release()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun triggerRichHaptics(context: Context, isLevelUp: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val timings = if (isLevelUp) longArrayOf(0, 40, 40, 50, 40, 80, 50, 140)
            else longArrayOf(0, 35, 45, 75)
            val amplitudes = if (isLevelUp) intArrayOf(0, 150, 0, 190, 0, 220, 0, 255)
            else intArrayOf(0, 160, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (_: Exception) {
        }
    }
}

// ============================================================================
// 4. LOCAL PERSISTENCE & GOOGLE DRIVE / CLOUD BACKUP ENGINE
// ============================================================================
class StudioStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("aura_studio_vault_v2", Context.MODE_PRIVATE)

    fun saveFullState(
        xp: Int,
        streakShields: Int,
        googleEmail: String?,
        autoBackup: Boolean,
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
            .putString("last_saved_time", nowFormatted)
            .apply()
        return root.toString(2)
    }

    fun getSavedJson(): String? = prefs.getString("master_state_json", null)
    fun getGoogleEmail(): String? = prefs.getString("google_email", null)
    fun getLastBackupTime(): String = prefs.getString("last_saved_time", "Synced just now") ?: "Synced just now"
    fun isAutoBackup(): Boolean = prefs.getBoolean("auto_backup", true)

    fun exportBackupToUri(uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (e: Exception) {
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
        root.put("schemaVersion", 2)
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
// 5. STUDIO VIEWMODEL (State, Gamification, Pomodoro Timer, Analytics, & Persistence)
// ============================================================================
class HabitTrackerViewModel : ViewModel() {

    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()

    // Filtering & UI Modes
    val selectedTimeOfDay = mutableStateOf(TimeOfDay.ALL)
    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.STUDIO_CARDS)
    val selectedDate = mutableStateOf(LocalDate.now())

    // Gamification (XP, Level, Streak Shields)
    val userXp = mutableIntStateOf(420)
    val streakShields = mutableIntStateOf(3)

    val userLevel: Int get() = (userXp.intValue / 200) + 1
    val xpProgressInLevel: Float get() = (userXp.intValue % 200) / 200f
    val userRankTitle: String
        get() = when (userLevel) {
            1, 2 -> "Initiate"
            3, 4 -> "Builder"
            5, 6, 7 -> "Habit Architect"
            8, 9, 10 -> "High Performer"
            else -> "Zenith Grandmaster"
        }

    // Built-In Pomodoro / Focus Timer State
    val activeFocusTitle = mutableStateOf("Deep Focus Session")
    val activeFocusHabitId = mutableStateOf<String?>(null)
    val focusRemainingSeconds = mutableLongStateOf(25 * 60L)
    val focusTotalSeconds = mutableLongStateOf(25 * 60L)
    val isFocusTimerRunning = mutableStateOf(false)
    private var timerJob: Job? = null

    // Full-Screen Celebration State
    val showCelebration = mutableStateOf(false)
    val celebrationTitle = mutableStateOf("")
    val celebrationSubtitle = mutableStateOf("")
    val celebrationXpEarned = mutableIntStateOf(25)

    // Google Cloud & Backup State
    val connectedGoogleEmail = mutableStateOf<String?>(null)
    val lastCloudBackup = mutableStateOf("Auto-saved locally")
    val autoCloudSync = mutableStateOf(true)
    val isSyncingCloud = mutableStateOf(false)

    private var isInitialized = false

    fun initializeIfNeeded(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val storage = StudioStorageManager(context)
        connectedGoogleEmail.value = storage.getGoogleEmail()
        lastCloudBackup.value = storage.getLastBackupTime()
        autoCloudSync.value = storage.isAutoBackup()

        val savedJson = storage.getSavedJson()
        if (!savedJson.isNullOrBlank()) {
            restoreFromJsonString(savedJson)
        } else {
            seedStudioStarterTemplates()
            persistState(context)
        }
    }

    fun persistState(context: Context): String {
        val storage = StudioStorageManager(context)
        val json = storage.saveFullState(
            xp = userXp.intValue,
            streakShields = streakShields.intValue,
            googleEmail = connectedGoogleEmail.value,
            autoBackup = autoCloudSync.value,
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
            userXp.intValue = root.optInt("xp", 420)
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
        } catch (e: Exception) {
            false
        }
    }

    // --- Celebration & XP Trigger ---
    fun awardXpAndCelebrate(
        context: Context,
        title: String,
        subtitle: String,
        xpEarned: Int = 25
    ) {
        val oldLevel = userLevel
        userXp.intValue += xpEarned
        val newLevel = userLevel
        val leveledUp = newLevel > oldLevel
        if (leveledUp) {
            streakShields.intValue += 1
        }

        celebrationTitle.value = if (leveledUp) "🎉 LEVEL $newLevel UNLOCKED!" else title
        celebrationSubtitle.value = if (leveledUp)
            "Rank: $userRankTitle • +1 Streak Shield Earned!"
        else subtitle
        celebrationXpEarned.intValue = xpEarned
        showCelebration.value = true

        CelebrationSoundEngine.playCelebrationFanfare(context, isLevelUp = leveledUp)
        persistState(context)

        viewModelScope.launch {
            delay(3600)
            showCelebration.value = false
        }
    }

    // --- Pomodoro Focus Timer Engine ---
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
                        val current = h.valueOn(LocalDate.now())
                        setHabitMeasurement(context, h, current + mins)
                    }
                }
                awardXpAndCelebrate(
                    context = context,
                    title = "⏱️ Focus Session Complete!",
                    subtitle = "${activeFocusTitle.value} finished with zero distractions",
                    xpEarned = 40
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
        val isDoneNow = updated.isCompletedOn(date)

        if (!wasDoneBefore && isDoneNow) {
            val streak = updated.currentStreak()
            awardXpAndCelebrate(
                context = context,
                title = "${updated.emoji} ${updated.title} Crushed!",
                subtitle = "🔥 $streak-day streak • Consistency is compounding!",
                xpEarned = 30
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
                xpEarned = 30
            )
        } else {
            persistState(context)
        }
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
    ) {
        habits.add(
            0,
            Habit(
                title = title.trim(),
                emoji = emoji.ifBlank { "⚡" },
                category = category.ifBlank { "Growth" },
                timeOfDay = timeOfDay,
                habitStackingCue = cue.trim(),
                type = type,
                colorHex = colorHex,
                targetValue = targetValue.coerceAtLeast(0.1f),
                unit = unit.ifBlank { "times" },
                stepSize = stepSize.coerceAtLeast(0.1f)
            )
        )
        persistState(context)
    }

    fun deleteHabit(context: Context, habitId: String) {
        habits.removeAll { it.id == habitId }
        persistState(context)
    }

    // --- To-Do Operations ---
    fun toggleTodo(context: Context, task: TodoTask) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val nowDone = !task.isCompleted
        todos[idx] = task.copy(isCompleted = nowDone)
        if (nowDone) {
            awardXpAndCelebrate(
                context = context,
                title = "✅ Mission Accomplished!",
                subtitle = "\"${task.title}\" completed",
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

    fun addTodoTask(
        context: Context,
        title: String,
        category: String,
        priority: TodoPriority,
        timeBlock: String,
        estimatedMinutes: Int,
        subTaskTitles: List<String>,
        isRecurring: Boolean
    ) {
        if (title.isBlank()) return
        todos.add(
            0,
            TodoTask(
                title = title.trim(),
                category = category,
                priority = priority,
                timeBlock = timeBlock,
                estimatedMinutes = estimatedMinutes,
                subTasks = subTaskTitles.filter { it.isNotBlank() }.map { SubTask(title = it.trim()) },
                isRecurring = isRecurring
            )
        )
        persistState(context)
    }

    fun deleteTodoTask(context: Context, id: String) {
        todos.removeAll { it.id == id }
        persistState(context)
    }

    // --- Journal Operations ---
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
    ) {
        // Persist read permission for selected photos
        photos.forEach { uriStr ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
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
            title = "📓 Reflection Logged!",
            subtitle = "Self-awareness is the foundation of mastery",
            xpEarned = 35
        )
    }

    fun deleteJournalEntry(context: Context, id: String) {
        journals.removeAll { it.id == id }
        persistState(context)
    }

    // --- Notebook Operations ---
    fun addNotebookEntry(context: Context, entry: NotebookEntry) {
        notes.add(0, entry)
        awardXpAndCelebrate(
            context = context,
            title = "🧠 Added to Second Brain!",
            subtitle = "\"${entry.title}\" saved to your knowledge vault",
            xpEarned = 30
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

    // --- Cloud Sync & Reset ---
    fun syncToGoogleCloud(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingCloud.value = true
            delay(900)
            persistState(context)
            isSyncingCloud.value = false
        }
    }

    fun clearAllUserData(context: Context) {
        habits.clear()
        todos.clear()
        journals.clear()
        notes.clear()
        persistState(context)
    }

    private fun seedStudioStarterTemplates() {
        val today = LocalDate.now()

        // 1. Deep Work Architecture (Focus Timer / Measurement Habit)
        val deepWorkLogs = mutableMapOf<String, Float>()
        for (i in 1..28) {
            if (i % 5 != 0) deepWorkLogs[today.minusDays(i.toLong()).toString()] = 90f
        }
        habits.add(
            Habit(
                title = "Deep Focus Sprint",
                emoji = "🚀",
                category = "Career",
                timeOfDay = TimeOfDay.MORNING,
                habitStackingCue = "After brewing morning espresso",
                type = HabitType.FOCUS_TIMER,
                colorHex = 0xFF34D399,
                targetValue = 90f,
                unit = "mins",
                stepSize = 30f,
                logs = deepWorkLogs
            )
        )

        // 2. Strength & Conditioning (Checklist Habit)
        val strengthLogs = mutableMapOf<String, Float>()
        for (i in 1..35) {
            if (i % 2 == 1) strengthLogs[today.minusDays(i.toLong()).toString()] = 1f
        }
        habits.add(
            Habit(
                title = "Strength & Mobility",
                emoji = "🏋️",
                category = "Fitness",
                timeOfDay = TimeOfDay.EVENING,
                habitStackingCue = "At 6:00 PM shutdown ritual",
                type = HabitType.CHECKLIST,
                colorHex = 0xFFA855F7,
                targetValue = 1f,
                unit = "session",
                stepSize = 1f,
                logs = strengthLogs
            )
        )

        // 3. Electrolyte Hydration (Measurement Habit)
        val waterLogs = mutableMapOf<String, Float>()
        for (i in 1..20) {
            waterLogs[today.minusDays(i.toLong()).toString()] = 3.0f
        }
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
                logs = waterLogs
            )
        )

        // 4. Zero Doomscrolling (Quit Shield Habit)
        val cleanLogs = mutableMapOf<String, Float>()
        for (i in 1..14) {
            cleanLogs[today.minusDays(i.toLong()).toString()] = 1f
        }
        habits.add(
            Habit(
                title = "No Late-Night Reels",
                emoji = "🛡️",
                category = "Mindfulness",
                timeOfDay = TimeOfDay.EVENING,
                habitStackingCue = "Phone charges outside bedroom at 10:30 PM",
                type = HabitType.QUIT_SHIELD,
                colorHex = 0xFFF59E0B,
                targetValue = 1f,
                unit = "clean night",
                logs = cleanLogs
            )
        )

        // Starter Priority To-Do with Subtasks
        todos.addAll(
            listOf(
                TodoTask(
                    title = "Design weekly execution blueprint",
                    category = "Strategy",
                    priority = TodoPriority.P1_CRITICAL,
                    timeBlock = "09:00 AM",
                    estimatedMinutes = 45,
                    subTasks = listOf(
                        SubTask(title = "Audit last week's habit completion rate", isDone = true),
                        SubTask(title = "Block 90m morning maker sessions"),
                        SubTask(title = "Set top 3 non-negotiable outcomes")
                    )
                ),
                TodoTask(
                    title = "Read 25 pages & capture book notes",
                    category = "Growth",
                    priority = TodoPriority.P2_HIGH,
                    timeBlock = "08:30 PM",
                    estimatedMinutes = 30,
                    isRecurring = true
                )
            )
        )

        // Starter Journal Entry
        journals.add(
            JournalEntry(
                title = "High-Output Flow State",
                moodEmoji = "🔥",
                energyScore = 9,
                gratitudeOrWins = "Locked in a 90-minute distraction-free morning sprint before checking any notifications.",
                dailyStory = "Built out the core architecture and hit all physical training targets. Energy stayed high after switching afternoon coffee for electrolytes.",
                frictionOrMistakes = "Let an afternoon call run 20 minutes over schedule.",
                actionItems = listOf("Set a hard 25-minute timer for all check-in calls"),
                tags = listOf("FlowState", "Discipline", "Wins")
            )
        )

        // Starter Second-Brain Book Note
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
                    "Specific knowledge is found by pursuing your genuine curiosity rather than whatever is hot right now.",
                    "A calm mind, a fit body, and a house full of love cannot be bought—they must be earned.",
                    "Impatience with actions, patience with results."
                ),
                detailedNotes = "Core Mental Model: Leverage comes in three forms—Labor, Capital, and Products with no marginal cost of replication (Code & Media). Combine daily habits with code/media leverage for exponential output.",
                flashcards = listOf(
                    FlashcardItem(
                        question = "What are the two forms of permissionless leverage?",
                        answer = "Code and Media—you don't need anyone's permission to create or distribute them."
                    )
                )
            )
        )
    }
}
