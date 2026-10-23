package com.vibrant.habittracker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 1. VIBRANT NEON DARK THEME PALETTE (Matched to Reference Screenshots)
// ============================================================================
object AppColors {
    val BgDeep = Color(0xFF0B0E11)
    val BgCard = Color(0xFF14181D)
    val BgCardElevated = Color(0xFF1C2229)
    val BorderDark = Color(0xFF262E38)

    val NeonGreen = Color(0xFF4ADE80)
    val NeonGreenGlow = Color(0xFF166534)
    val MintGreen = Color(0xFF6EE7B7)
    val LavenderPurple = Color(0xFFC084FC)
    val PeriwinkleBlue = Color(0xFF818CF8)
    val CoralOrange = Color(0xFFFB923C)
    val RoseRed = Color(0xFFF87171)
    val AmberGold = Color(0xFFFBBF24)

    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    val TextMuted = Color(0xFF6B7280)

    val AccentPresets = listOf(
        LavenderPurple,
        MintGreen,
        PeriwinkleBlue,
        CoralOrange,
        NeonGreen,
        AmberGold,
        RoseRed
    )
}

// ============================================================================
// 2. CORE DATA MODELS
// ============================================================================
enum class HabitType {
    CHECKLIST,     // Done / Not Done (e.g., Morning Stretch)
    MEASUREMENT,   // Quantifiable target (e.g., Drink Water 8 glasses, Side Project 2.0 hrs)
    QUIT_SHIELD    // Abstinence / Resistance tracker (e.g., Quit Smoking)
}

enum class HabitViewMode {
    GRID_VIEW,     // 16-week contribution heatmap grid
    WEEKLY_VIEW    // 7-day interactive M-Su row
}

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String,
    val category: String, // Fitness, Health, Morning, Social, Work
    val type: HabitType,
    val colorHex: Long,
    val targetValue: Float = 1f,
    val unit: String = "times",
    val stepSize: Float = 1f,
    // Maps ISO Date String (YYYY-MM-DD) -> logged value (e.g. 1f for checklist, 4.5f for hours, -1f for failed shield)
    val logs: Map<String, Float> = emptyMap()
) {
    val accentColor: Color get() = Color(colorHex)

    fun isCompletedOn(date: LocalDate): Boolean {
        val v = logs[date.toString()] ?: 0f
        return if (type == HabitType.QUIT_SHIELD) v > 0f else v >= targetValue
    }

    fun valueOn(date: LocalDate): Float = logs[date.toString()] ?: 0f

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
        for (i in 120 downTo 0) {
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

    fun totalLoggedValue(): Float = logs.values.filter { it > 0f }.sum()
}

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // Personal, Work, Health
    val date: String = LocalDate.now().toString(),
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val note: String = ""
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val moodEmoji: String = "🌟",
    val dailyActivities: String,
    val whatWentWrong: String,
    val actionsToTake: List<String>,
    val photoUris: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class NoteCategory {
    BOOK_SUMMARY,
    GENERAL_NOTE
}

data class NotebookEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: NoteCategory,
    val title: String,
    val subtitleOrAuthor: String,
    val rating: Int = 5, // 1..5 stars for books
    val progressPercent: Int = 100,
    val keyTakeaways: List<String> = emptyList(),
    val content: String,
    val favoriteQuote: String = "",
    val coverColorHex: Long = 0xFFC084FC,
    val updatedAt: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
)

// ============================================================================
// 3. SYNTHESIZED CELEBRATION SOUND & HAPTICS ENGINE
// ============================================================================
object CelebrationSoundEngine {
    /**
     * Synthesizes a bright, polyphonic major-9th arpeggio + sparkle chime
     * directly in PCM memory so celebration audio works 100% offline with zero raw files.
     */
    fun playCelebrationFanfare(context: Context) {
        triggerCelebrationHaptics(context)
        Thread {
            try {
                val sampleRate = 44100
                val durationSeconds = 1.35
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                // Notes: C5 (523.25), E5 (659.25), G5 (783.99), B5 (987.77), C6 (1046.50), G6 (1567.98)
                val notes = listOf(
                    Triple(523.25, 0.00, 0.45),
                    Triple(659.25, 0.09, 0.50),
                    Triple(783.99, 0.18, 0.55),
                    Triple(987.77, 0.27, 0.65),
                    Triple(1046.50, 0.36, 0.95),
                    Triple(1318.51, 0.46, 0.85),
                    Triple(1567.98, 0.54, 0.80)
                )

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0
                    for ((freq, start, dur) in notes) {
                        if (t >= start && t <= start + dur) {
                            val localT = t - start
                            val attack = (localT / 0.015).coerceAtMost(1.0)
                            val decay = exp(-3.8 * localT)
                            // Fundamental + warm octave harmonic + bell overtone
                            val wave = sin(2.0 * PI * freq * localT) +
                                    0.35 * sin(2.0 * PI * freq * 2.0 * localT) +
                                    0.15 * sin(2.0 * PI * freq * 3.0 * localT)
                            sample += wave * attack * decay * 0.22
                        }
                    }
                    val clamped = sample.coerceIn(-0.98, 0.98)
                    buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(1500)
                audioTrack.release()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun triggerCelebrationHaptics(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val timings = longArrayOf(0, 35, 45, 50, 50, 90)
            val amplitudes = intArrayOf(0, 140, 0, 200, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (_: Exception) {
        }
    }
}

// ============================================================================
// 4. GOOGLE ACCOUNT & CLOUD BACKUP / PERSISTENCE MANAGER
// ============================================================================
class GoogleBackupManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("pulse_habit_cloud_prefs", Context.MODE_PRIVATE)

    fun getConnectedAccountEmail(): String? = prefs.getString("google_email", null)
    fun getLastBackupTime(): String = prefs.getString("last_backup_time", "Never") ?: "Never"
    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean("auto_backup", true)

    fun setConnectedAccount(email: String?) {
        prefs.edit().putString("google_email", email).apply()
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup", enabled).apply()
    }

    fun saveBackupSnapshot(
        habits: List<Habit>,
        todos: List<TodoTask>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val habitsArr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("emoji", h.emoji)
                put("category", h.category)
                put("type", h.type.name)
                put("colorHex", h.colorHex)
                put("targetValue", h.targetValue.toDouble())
                put("unit", h.unit)
                put("stepSize", h.stepSize.toDouble())
                val logsObj = JSONObject()
                h.logs.forEach { (k, v) -> logsObj.put(k, v.toDouble()) }
                put("logs", logsObj)
            }
            habitsArr.put(obj)
        }
        root.put("habits", habitsArr)

        val todosArr = JSONArray()
        todos.forEach { t ->
            todosArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("category", t.category)
                put("date", t.date)
                put("isCompleted", t.isCompleted)
                put("isRecurring", t.isRecurring)
                put("note", t.note)
            })
        }
        root.put("todos", todosArr)

        val jsonString = root.toString(2)
        val nowFormatted = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

        prefs.edit()
            .putString("cloud_snapshot_json", jsonString)
            .putString("last_backup_time", nowFormatted)
            .apply()

        return jsonString
    }
}

// ============================================================================
// 5. MAIN VIEWMODEL (State, Analytics, Celebrations, & Sample Data)
// ============================================================================
class HabitTrackerViewModel : ViewModel() {

    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()

    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.GRID_VIEW)

    // Full-screen celebration state
    val showCelebration = mutableStateOf(false)
    val celebrationTitle = mutableStateOf("")
    val celebrationSubtitle = mutableStateOf("")

    // Google Cloud Backup state
    val connectedGoogleEmail = mutableStateOf<String?>("alex.productivity@gmail.com")
    val lastCloudBackup = mutableStateOf("Today, 12:45 PM")
    val autoCloudSync = mutableStateOf(true)
    val isSyncingCloud = mutableStateOf(false)

    init {
        seedRichDemoData()
    }

    fun triggerCelebration(context: Context, title: String, subtitle: String) {
        celebrationTitle.value = title
        celebrationSubtitle.value = subtitle
        showCelebration.value = true
        CelebrationSoundEngine.playCelebrationFanfare(context)
        viewModelScope.launch {
            delay(3400)
            showCelebration.value = false
        }
    }

    // --- Habit Operations ---
    fun toggleOrIncrementHabit(context: Context, habit: Habit, date: LocalDate = LocalDate.now()) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index == -1) return

        val key = date.toString()
        val currentVal = habit.logs[key] ?: 0f
        val updatedLogs = habit.logs.toMutableMap()

        val wasCompletedBefore = habit.isCompletedOn(date)

        when (habit.type) {
            HabitType.CHECKLIST -> {
                if (currentVal >= habit.targetValue) updatedLogs.remove(key)
                else updatedLogs[key] = habit.targetValue
            }
            HabitType.MEASUREMENT -> {
                val nextVal = currentVal + habit.stepSize
                if (nextVal > habit.targetValue * 1.5f && currentVal >= habit.targetValue) {
                    updatedLogs[key] = nextVal
                } else {
                    updatedLogs[key] = nextVal
                }
            }
            HabitType.QUIT_SHIELD -> {
                // Toggle between Shield Resisted (1f) -> Slip (-1f) -> Clear
                when {
                    currentVal == 0f -> updatedLogs[key] = 1f
                    currentVal > 0f -> updatedLogs[key] = -1f
                    else -> updatedLogs.remove(key)
                }
            }
        }

        val updatedHabit = habit.copy(logs = updatedLogs)
        habits[index] = updatedHabit

        val isCompletedNow = updatedHabit.isCompletedOn(date)
        if (!wasCompletedBefore && isCompletedNow) {
            val streak = updatedHabit.currentStreak()
            triggerCelebration(
                context = context,
                title = "${updatedHabit.emoji} ${updatedHabit.title} Complete!",
                subtitle = "🔥 $streak day streak • Keep the momentum alive!"
            )
        }
    }

    fun setHabitMeasurement(context: Context, habit: Habit, amount: Float, date: LocalDate = LocalDate.now()) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index == -1) return
        val wasCompleted = habit.isCompletedOn(date)
        val updatedLogs = habit.logs.toMutableMap()
        if (amount <= 0f) updatedLogs.remove(date.toString())
        else updatedLogs[date.toString()] = amount

        val updated = habit.copy(logs = updatedLogs)
        habits[index] = updated
        if (!wasCompleted && updated.isCompletedOn(date)) {
            triggerCelebration(
                context = context,
                title = "${updated.emoji} Goal Crushed!",
                subtitle = "${amount} ${updated.unit} logged for ${updated.title}"
            )
        }
    }

    fun addNewHabit(
        title: String,
        emoji: String,
        category: String,
        type: HabitType,
        colorHex: Long,
        targetValue: Float,
        unit: String,
        stepSize: Float
    ) {
        habits.add(
            0,
            Habit(
                title = title,
                emoji = emoji,
                category = category,
                type = type,
                colorHex = colorHex,
                targetValue = targetValue,
                unit = unit,
                stepSize = stepSize
            )
        )
    }

    // --- Todo Operations ---
    fun toggleTodo(context: Context, task: TodoTask) {
        val idx = todos.indexOfFirst { it.id == task.id }
        if (idx == -1) return
        val nowCompleted = !task.isCompleted
        todos[idx] = task.copy(isCompleted = nowCompleted)
        if (nowCompleted) {
            triggerCelebration(
                context = context,
                title = "✅ Task Completed!",
                subtitle = "\"${task.title}\" checked off your timeline"
            )
        }
    }

    fun addTodoTask(title: String, category: String, date: String = LocalDate.now().toString(), isRecurring: Boolean = false) {
        if (title.isBlank()) return
        todos.add(0, TodoTask(title = title.trim(), category = category, date = date, isRecurring = isRecurring))
    }

    // --- Journal Operations ---
    fun addJournalEntry(
        moodEmoji: String,
        dailyActivities: String,
        whatWentWrong: String,
        actionsToTake: List<String>,
        photoUris: List<String>,
        convertActionsToTodos: Boolean
    ) {
        val entry = JournalEntry(
            moodEmoji = moodEmoji,
            dailyActivities = dailyActivities,
            whatWentWrong = whatWentWrong,
            actionsToTake = actionsToTake,
            photoUris = photoUris
        )
        journals.add(0, entry)
        if (convertActionsToTodos) {
            actionsToTake.forEach { action ->
                if (action.isNotBlank()) {
                    addTodoTask(title = action, category = "Personal")
                }
            }
        }
    }

    // --- Notebook Operations ---
    fun addNotebookEntry(entry: NotebookEntry) {
        notes.add(0, entry)
    }

    // --- Cloud Sync Operation ---
    fun syncToGoogleCloud(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingCloud.value = true
            delay(1100)
            val manager = GoogleBackupManager(context)
            manager.saveBackupSnapshot(habits, todos, journals, notes)
            lastCloudBackup.value = manager.getLastBackupTime()
            isSyncingCloud.value = false
        }
    }

    // --- Seed Rich Demo Data Matching Reference Screenshots ---
    private fun seedRichDemoData() {
        val today = LocalDate.now()

        // 1. Morning Stretch (Purple Grid - Streak 12)
        val stretchLogs = mutableMapOf<String, Float>()
        for (i in 0..105) {
            val d = today.minusDays(i.toLong())
            if (i < 12 || (i % 3 != 0 && i % 7 != 4)) {
                stretchLogs[d.toString()] = 1f
            }
        }
        habits.add(
            Habit(
                title = "Morning Stretch",
                emoji = "🧘",
                category = "Morning",
                type = HabitType.CHECKLIST,
                colorHex = 0xFFC084FC,
                targetValue = 1f,
                unit = "session",
                logs = stretchLogs
            )
        )

        // 2. Gym Session (Mint Green Grid)
        val gymLogs = mutableMapOf<String, Float>()
        for (i in 0..105) {
            val d = today.minusDays(i.toLong())
            if (i in listOf(0, 2, 4, 7, 9, 11, 14, 16, 19, 21, 24, 28, 30, 35, 38, 42, 45, 50, 54, 60, 63, 70)) {
                gymLogs[d.toString()] = 1f
            }
        }
        habits.add(
            Habit(
                title = "Gym Session",
                emoji = "💪",
                category = "Fitness",
                type = HabitType.CHECKLIST,
                colorHex = 0xFF6EE7B7,
                targetValue = 1f,
                unit = "workout",
                logs = gymLogs
            )
        )

        // 3. Drink Water (Coral Orange Measurement - 8 Glasses)
        val waterLogs = mutableMapOf<String, Float>()
        for (i in 0..90) {
            val d = today.minusDays(i.toLong())
            waterLogs[d.toString()] = when {
                i == 0 -> 6f
                i % 4 == 0 -> 5f
                else -> 8f
            }
        }
        habits.add(
            Habit(
                title = "Drink Water",
                emoji = "💧",
                category = "Health",
                type = HabitType.MEASUREMENT,
                colorHex = 0xFFFB923C,
                targetValue = 8f,
                unit = "glasses",
                stepSize = 1f,
                logs = waterLogs
            )
        )

        // 4. Side Project (Neon Green Measurement - Hours Logged for Analytics Chart)
        val projectLogs = mutableMapOf<String, Float>()
        val sampleHours = listOf(3.5f, 4.8f, 4.0f, 4.2f, 5.0f, 3.0f, 2.5f, 4.0f, 1.8f, 3.2f, 4.5f, 2.0f)
        for (i in 0..89) {
            val d = today.minusDays(i.toLong())
            if (i < 12) {
                projectLogs[d.toString()] = sampleHours[i]
            } else if (i % 2 == 0) {
                projectLogs[d.toString()] = 2.4f + (i % 4) * 0.8f
            }
        }
        habits.add(
            Habit(
                title = "Side Project",
                emoji = "💻",
                category = "Work",
                type = HabitType.MEASUREMENT,
                colorHex = 0xFF4ADE80,
                targetValue = 2.0f,
                unit = "hrs",
                stepSize = 0.5f,
                logs = projectLogs
            )
        )

        // 5. Quit Smoking (Periwinkle Blue Shield Tracker)
        val quitLogs = mutableMapOf<String, Float>()
        for (i in 0..95) {
            val d = today.minusDays(i.toLong())
            quitLogs[d.toString()] = if (i in listOf(5, 13, 27, 41)) -1f else 1f
        }
        habits.add(
            Habit(
                title = "Quit Smoking",
                emoji = "🚭",
                category = "Health",
                type = HabitType.QUIT_SHIELD,
                colorHex = 0xFF818CF8,
                targetValue = 1f,
                unit = "day clean",
                logs = quitLogs
            )
        )

        // Timeline To-Do Tasks (Matched to Image 2)
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()
        todos.addAll(
            listOf(
                TodoTask(title = "Call mom", category = "Personal", date = todayStr, isRecurring = true),
                TodoTask(title = "Water the plants", category = "Personal", date = todayStr),
                TodoTask(title = "Prepare presentation for client meeting", category = "Work", date = todayStr),
                TodoTask(title = "Take vitamins", category = "Health", date = todayStr, isRecurring = true),
                TodoTask(title = "Morning 20m cardio", category = "Health", date = todayStr, isCompleted = true),
                TodoTask(title = "Review quarterly budget", category = "Work", date = yesterdayStr, isCompleted = true),
                TodoTask(title = "Zero inbox cleanup", category = "Work", date = yesterdayStr, isCompleted = true),
                TodoTask(title = "Read 20 pages of Atomic Habits", category = "Personal", date = yesterdayStr, isCompleted = true)
            )
        )

        // Sample Reflection Journal Entry
        journals.add(
            JournalEntry(
                date = today.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                moodEmoji = "🔥",
                dailyActivities = "Completed morning stretch on the balcony, shipped the new analytics chart module for the side project, and hit an evening upper-body gym session.",
                whatWentWrong = "Checked social media for 40 minutes right after lunch which broke my deep-work flow state.",
                actionsToTake = listOf(
                    "Keep phone in another room during 1 PM - 4 PM deep work block",
                    "Prep gym bag the night before"
                )
            )
        )

        // Sample Book Summaries & Advanced Notes
        notes.addAll(
            listOf(
                NotebookEntry(
                    category = NoteCategory.BOOK_SUMMARY,
                    title = "Atomic Habits",
                    subtitleOrAuthor = "James Clear",
                    rating = 5,
                    progressPercent = 100,
                    coverColorHex = 0xFF4ADE80,
                    favoriteQuote = "You do not rise to the level of your goals. You fall to the level of your systems.",
                    keyTakeaways = listOf(
                        "1% better every day compounds to 37x improvement in a year.",
                        "Use Habit Stacking: After [Current Habit], I will [New Habit].",
                        "Never miss twice—missing once is an accident, missing twice is the start of a new habit."
                    ),
                    content = "Core Framework: The Four Laws of Behavior Change:\n1. Make it Obvious (design your environment)\n2. Make it Attractive (temptation bundling)\n3. Make it Easy (the 2-Minute Rule)\n4. Make it Satisfying (immediate celebration & visual streak tracking)."
                ),
                NotebookEntry(
                    category = NoteCategory.BOOK_SUMMARY,
                    title = "Deep Work",
                    subtitleOrAuthor = "Cal Newport",
                    rating = 5,
                    progressPercent = 85,
                    coverColorHex = 0xFFC084FC,
                    favoriteQuote = "Clarity about what matters provides clarity about what does not.",
                    keyTakeaways = listOf(
                        "Schedule every minute of your workday in time blocks.",
                        "Embrace boredom—train your brain to resist novelty on demand.",
                        "Execute a strict shutdown ritual at 6:00 PM."
                    ),
                    content = "Deep Work is professional activities performed in a state of distraction-free concentration that push your cognitive capabilities to their limit."
                ),
                NotebookEntry(
                    category = NoteCategory.GENERAL_NOTE,
                    title = "Q4 Personal Architecture & Keystone Systems",
                    subtitleOrAuthor = "Life Strategy Note",
                    coverColorHex = 0xFF818CF8,
                    keyTakeaways = listOf("Sleep 7.5h minimum", "90m morning maker block before checking messages"),
                    content = "Key focus areas for the next 90 days:\n• Energy: Hydration (8 glasses/day) + 4x strength sessions/week.\n• Output: 15 hours/week logged on Side Project.\n• Reflection: Nightly 5-minute journal audit of mistakes & corrective actions."
                )
            )
        )
    }
}
