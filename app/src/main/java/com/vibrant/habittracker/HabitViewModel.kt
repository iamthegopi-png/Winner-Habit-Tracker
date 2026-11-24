package com.vibrant.habittracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class HabitTrackerViewModel : ViewModel() {

    // Core Data Collections
    val habits = mutableStateListOf<Habit>()
    val todos = mutableStateListOf<TodoTask>()
    val goals = mutableStateListOf<GoalItem>()
    val hourlyLogs = mutableStateListOf<HourlyLogItem>()
    val journals = mutableStateListOf<JournalEntry>()
    val notes = mutableStateListOf<NotebookEntry>()
    val claimedQuestKeys = mutableStateListOf<String>()

    // Filters & Display Settings
    val selectedTimeOfDay = mutableStateOf(TimeOfDay.ALL)
    val selectedCategory = mutableStateOf("All")
    val habitViewMode = mutableStateOf(HabitViewMode.STUDIO_CARDS)
    val selectedDate = mutableStateOf(LocalDate.now())

    val readingTheme = mutableStateOf(SanctuaryReadingTheme.WARM_SEPIA)
    val readingFontScale = mutableFloatStateOf(1.0f)
    val brainWidgetColorPref = mutableStateOf(BrainWidgetColorPref.AUTO_DYNAMIC)
    val ambientSoundMode = mutableStateOf(AmbientSoundMode.OFF)

    // Earned XP, Level & Progression (Zero Manual Shortcuts)
    val userXp = mutableIntStateOf(420)
    val streakShields = mutableIntStateOf(2)
    val customHeroQuote = mutableStateOf<String?>(null)

    val userLevel: Int
        get() = (userXp.intValue.coerceAtLeast(0) / 200) + 1

    val xpProgressInLevel: Float
        get() = ((userXp.intValue.coerceAtLeast(0)) % 200) / 200f

    val userRankTitle: String
        get() = cachedCharacterStats.value.stage.title

    // Cached Computed Snapshots (Prevents recomputation during 120fps scrolling)
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

    val cachedBrainSnapshot = mutableStateOf(
        BrainMoodCalculator.buildPreviewSnapshot(
            mood = BrainMoodState.FOCUSED_GOOD,
            colorPref = BrainWidgetColorPref.AUTO_DYNAMIC
        )
    )

    val cachedTodayScore = mutableIntStateOf(70)
    val cachedDailyQuests = mutableStateOf<List<DailyQuestStatus>>(emptyList())
    val notificationHealth = mutableStateOf(
        NotificationHealthStatus(
            notificationsGranted = true,
            exactAlarmsGranted = true,
            batteryUnrestricted = true,
            scheduledRemindersCount = 0
        )
    )

    // Focus Timer State
    val activeFocusTitle = mutableStateOf("Deep Focus Session")
    val activeFocusHabitId = mutableStateOf<String?>(null)
    val focusRemainingSeconds = mutableLongStateOf(25 * 60L)
    val focusTotalSeconds = mutableLongStateOf(25 * 60L)
    val isFocusTimerRunning = mutableStateOf(false)
    private var timerJob: Job? = null

    // Full-Screen Celebration Overlay State
    val showCelebration = mutableStateOf(false)
    val celebrationTitle = mutableStateOf("")
    val celebrationSubtitle = mutableStateOf("")
    val celebrationXpEarned = mutableIntStateOf(25)

    // Cloud Backup State
    val connectedGoogleEmail = mutableStateOf<String?>(null)
    val lastCloudBackup = mutableStateOf("Auto-saved locally")
    val autoCloudSync = mutableStateOf(true)
    val isSyncingCloud = mutableStateOf(false)

    private var persistJob: Job? = null
    private var isInitialized = false

    private val studioMotivationalPrinciples = listOf(
        "“No shortcuts exist in mastery—only the work you log today.”",
        "“Keep your mind sharp. Finish your daily wins before the sun sets.”",
        "“Protect your focus hours as fiercely as your long-term goals.”",
        "“Deadlines don't move for hesitation. Log one focused session now.”",
        "“Calm focus beats frantic energy. Breathe, lock in, and execute.”",
        "“Quiet consistency outperforms loud bursts of motivation every week.”"
    )

    // ========================================================================
    // CACHED METRICS, AMBIENT SOUND & HEALTH CHECKS
    // ========================================================================
    fun refreshCachedMetrics() {
        cachedCharacterStats.value = computeCharacterStats()
        cachedTodayScore.intValue = getDailyProductivityScore(LocalDate.now())
        cachedDailyQuests.value = computeDailyQuests()
        cachedBrainSnapshot.value = BrainMoodCalculator.evaluate(
            habits = habits,
            tasks = todos,
            colorPref = brainWidgetColorPref.value
        )
    }

    fun setBrainWidgetColorPref(context: Context, pref: BrainWidgetColorPref) {
        brainWidgetColorPref.value = pref
        refreshCachedMetrics()
        persistStateAsync(context)
    }

    fun setAmbientSound(mode: AmbientSoundMode) {
        ambientSoundMode.value = mode
        CelebrationSoundEngine.startAmbientSound(mode)
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
        if (!claimedQuestKeys.contains(key)) {
            claimedQuestKeys.add(key)
        }
        if (quest.grantsShield) {
            streakShields.intValue += 1
        }
        awardXpAndCelebrate(
            context = context,
            title = "${quest.emoji} Daily Quest Complete",
            subtitle = "${quest.title} • +${quest.rewardXp} XP${if (quest.grantsShield) " & +1 Shield" else ""}",
            xpEarned = quest.rewardXp
        )
    }

    // ========================================================================
    // GOAL ACHIEVEMENT & DAILY WORK LOGGING
    // ========================================================================
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

        val newLog = GoalWorkLog(
            date = date.toString(),
            workSummary = workSummary.trim().ifBlank { "Daily progress logged" },
            progressDelta = progressDelta.coerceAtLeast(0f),
            minutesSpent = minutesSpent.coerceAtLeast(5)
        )
        val updatedGoal = currentGoal.copy(
            currentValue = (currentGoal.currentValue + progressDelta).coerceAtLeast(0f),
            dailyLogs = listOf(newLog) + currentGoal.dailyLogs
        )
        goals[idx] = updatedGoal
        refreshCachedMetrics()

        val justFinished = !wasDone && updatedGoal.isCompleted
        awardXpAndCelebrate(
            context = context,
            title = if (justFinished) "🏆 GOAL ACHIEVED!" else "${updatedGoal.emoji} Goal Work Logged",
            subtitle = if (justFinished) {
                "\"${updatedGoal.title}\" 100% completed!"
            } else {
                "+${progressDelta.cleanStr()} ${updatedGoal.unit} • ${updatedGoal.progressPercent}% total"
            },
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

    // ========================================================================
    // DAILY QUESTS & STRICT STEP-BY-STEP ARMOR EVOLUTION
    // ========================================================================
    private fun computeDailyQuests(): List<DailyQuestStatus> {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val habitsDoneToday = habits.count { it.isCompletedOn(today) }
        val tasksDoneToday = todos.count {
            it.isCompleted && (it.lastCompletedDate == todayStr || it.date == todayStr)
        }
        val goalLogsToday = goals.sumOf { g -> g.dailyLogs.count { it.date == todayStr } }
        val productiveHoursToday = hourlyLogs.count {
            it.date == todayStr &&
                (it.status == HourProductivityStatus.PEAK_FLOW ||
                    it.status == HourProductivityStatus.PRODUCTIVE)
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

        val wastedToday = hourlyLogs.count {
            it.date == todayStr &&
                (it.status == HourProductivityStatus.TIME_WASTED ||
                    it.status == HourProductivityStatus.LOW_FOCUS)
        }
        val flowToday = hourlyLogs.count {
            it.date == todayStr && it.status == HourProductivityStatus.PEAK_FLOW
        }

        val stages = CharacterStage.entries
        var currentStage = stages.first()
        for (st in stages) {
            val meetsXpAndStreak = xp >= st.minXpRequired && peakStreak >= st.minStreakRequired
            val meetsHighXpFallback = xp >= st.minXpRequired + 350
            if (meetsXpAndStreak || meetsHighXpFallback) {
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
            val xpLabel = if (xpNeeded > 0) "$xpNeeded XP needed" else "XP Ready"
            "Next Armor (${nextStage.title}): $xpLabel • ${nextStage.minStreakRequired}d streak"
        }

        val strLogs = habits
            .filter { it.category.contains("Fitness", true) || it.category.contains("Health", true) }
            .sumOf { it.logs.size }

        val intLogs = habits
            .filter { it.category.contains("Career", true) || it.category.contains("Growth", true) }
            .sumOf { it.logs.size } + (notes.size * 4) + (flowToday * 4) + (totalGoalWorkSessions * 3)

        val wilLogs = totalStreakSum * 3 +
            todos.count { it.isCompleted } * 3 +
            goalsCompleted * 15 -
            (wastedToday * 5)

        val vitLogs = journals.size * 4 + habits.size * 3

        val str = (25 + strLogs * 2 + lvl * 4).coerceAtLeast(10)
        val intStat = (30 + intLogs * 2 + lvl * 4).coerceAtLeast(10)
        val wil = (28 + wilLogs + lvl * 5).coerceAtLeast(10)
        val vit = (25 + vitLogs * 2 + lvl * 3).coerceAtLeast(10)
        val power = ((str + intStat + wil + vit) * 3 + xp).coerceAtLeast(100)

        val dynamicBubble = customHeroQuote.value ?: when {
            wastedToday > 0 && flowToday == 0 ->
                "“$wastedToday unproductive hour(s) logged today. Reclaim the next hour with calm focus.”"
            goalsWorkedToday > 0 ->
                "“Goal work verified today. Your ${currentStage.suitName} is forging stronger step by step.”"
            combo >= 1.5f ->
                "“${combo}x execution combo active. Pure discipline—no shortcuts.”"
            else ->
                "“${currentStage.title} standing by. Complete habits, missions & goal sessions to unlock ${nextStage?.title ?: "mastery"}.”"
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

    // ========================================================================
    // HOURLY PRODUCTIVITY AUDIT & PENALTY ENGINE
    // ========================================================================
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

        if (existingIdx != -1) {
            hourlyLogs[existingIdx] = newItem
        } else {
            hourlyLogs.add(newItem)
        }
        hourlyLogs.sortBy { it.hour24 }

        userXp.intValue = (userXp.intValue + (status.xpDelta - previousXpDelta)).coerceAtLeast(0)
        refreshCachedMetrics()

        when {
            status.xpDelta > 0 -> {
                awardXpAndCelebrate(
                    context = context,
                    title = "⚡ Productive Hour Logged",
                    subtitle = "${newItem.formattedHourRange()} • ${status.shortBadge} earned",
                    xpEarned = 0
                )
            }
            status.xpDelta < 0 -> {
                CelebrationSoundEngine.playPenaltyDrainSound(context)
                Toast.makeText(
                    context,
                    "⚠️ ${status.xpDelta} XP Penalty! Unproductive hour deducted.",
                    Toast.LENGTH_LONG
                ).show()
                persistStateAsync(context)
            }
            else -> persistStateAsync(context)
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

    // ========================================================================
    // AUTO-RENEWING DAILY TASKS
    // ========================================================================
    fun checkAndAutoRenewDailyTasks(context: Context, forceRenewForTesting: Boolean = false) {
        val todayStr = LocalDate.now().toString()
        var renewedCount = 0

        for (i in todos.indices) {
            val t = todos[i]
            val isFromPreviousDay = t.lastCompletedDate.isNotBlank() && t.lastCompletedDate != todayStr
            val shouldRenew = t.isRecurring && t.isCompleted && (forceRenewForTesting || isFromPreviousDay)
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
                Toast.makeText(
                    context,
                    "🔄 $renewedCount Auto-Renewing task(s) refreshed!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (forceRenewForTesting) {
            Toast.makeText(
                context,
                "Complete a 🔄 Daily Auto-Renew task first to test renewal!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ========================================================================
    // INITIALIZATION, ASYNC PERSISTENCE & JSON RESTORE
    // ========================================================================
    fun initializeOrRefreshFromDisk(context: Context) {
        val appContext = context.applicationContext
        val storage = StudioStorageManager(appContext)
        connectedGoogleEmail.value = storage.getGoogleEmail()
        lastCloudBackup.value = storage.getLastBackupTime()
        autoCloudSync.value = storage.isAutoBackup()
        readingTheme.value = storage.getReadingTheme()
        readingFontScale.floatValue = storage.getReadingFontScale()
        brainWidgetColorPref.value = storage.getBrainWidgetColorPref()

        val savedJson = storage.getSavedJson()
        if (!savedJson.isNullOrBlank()) {
            restoreFromJsonString(savedJson)
            isInitialized = true
            checkAndAutoRenewDailyTasks(appContext, forceRenewForTesting = false)
            if (goals.isEmpty()) {
                seedStarterGoals()
            }
        } else if (!isInitialized) {
            isInitialized = true
            seedStarterTemplates()
            persistStateAsync(appContext)
        }

        refreshCachedMetrics()
        refreshNotificationHealth(appContext)

        viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.rescheduleAllSavedAlarms(appContext)
            BrainMoodWidgetProvider.updateAllBrainWidgets(appContext)
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
        val snapshotBrainPref = brainWidgetColorPref.value
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
                brainWidgetColorPref = snapshotBrainPref,
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
            brainWidgetColorPref = brainWidgetColorPref.value,
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
                for (i in 0 until arr.length()) {
                    claimedQuestKeys.add(arr.optString(i))
                }
            }

            habits.clear()
            val habitsArr = root.optJSONArray("habits") ?: JSONArray()
            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                val logsMap = mutableMapOf<String, Float>()
                val logsObj = o.optJSONObject("logs")
                logsObj?.keys()?.forEach { k ->
                    logsMap[k] = logsObj.optDouble(k, 0.0).toFloat()
                }
                habits.add(
                    Habit(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title"),
                        emoji = o.optString("emoji", "⚡"),
                        category = o.optString("category", "Growth"),
                        timeOfDay = runCatching { TimeOfDay.valueOf(o.optString("timeOfDay")) }
                            .getOrDefault(TimeOfDay.ANYTIME),
                        habitStackingCue = o.optString("habitStackingCue", ""),
                        type = runCatching { HabitType.valueOf(o.optString("type")) }
                            .getOrDefault(HabitType.CHECKLIST),
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
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }
                            .getOrDefault(TodoPriority.P2_HIGH),
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
                o.optJSONArray("actionItems")?.let { a ->
                    for (k in 0 until a.length()) actions.add(a.optString(k))
                }
                val photos = mutableListOf<String>()
                o.optJSONArray("photoUris")?.let { a ->
                    for (k in 0 until a.length()) photos.add(a.optString(k))
                }
                val tags = mutableListOf<String>()
                o.optJSONArray("tags")?.let { a ->
                    for (k in 0 until a.length()) tags.add(a.optString(k))
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
                o.optJSONArray("keyTakeaways")?.let { a ->
                    for (k in 0 until a.length()) takeaways.add(a.optString(k))
                }
                val flashcards = mutableListOf<FlashcardItem>()
                o.optJSONArray("flashcards")?.let { a ->
                    for (k in 0 until a.length()) {
                        val f = a.getJSONObject(k)
                        flashcards.add(FlashcardItem(f.optString("question"), f.optString("answer")))
                    }
                }
                notes.add(
                    NotebookEntry(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        category = runCatching { NoteCategory.valueOf(o.optString("category")) }
                            .getOrDefault(NoteCategory.BOOK_SUMMARY),
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

    // ========================================================================
    // XP AWARDS, FOCUS TIMER & CRUD OPERATIONS
    // ========================================================================
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
        if (leveledUp) {
            streakShields.intValue += 1
        }

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

        CelebrationSoundEngine.playCustomOrDefaultSound(
            context = context,
            customSoundUri = customSoundUri,
            isReminder = false,
            isLevelUp = leveledUp
        )
        persistStateAsync(context)

        viewModelScope.launch {
            delay(2500)
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
        setAmbientSound(AmbientSoundMode.OFF)
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
                setAmbientSound(AmbientSoundMode.OFF)
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
                if (currentVal >= habit.targetValue) {
                    updatedLogs.remove(key)
                } else {
                    updatedLogs[key] = habit.targetValue
                }
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
                subtitle = "🔥 ${updated.currentStreak()}-day streak • Brain Mood boosted!",
                xpEarned = 35,
                customSoundUri = updated.customSoundUri
            )
        } else {
            persistStateAsync(context)
        }
    }

    fun setHabitMeasurement(
        context: Context,
        habit: Habit,
        newAmount: Float,
        date: LocalDate = selectedDate.value
    ) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index == -1) return
        val wasDone = habit.isCompletedOn(date)
        val updatedLogs = habit.logs.toMutableMap()

        if (newAmount <= 0f) {
            updatedLogs.remove(date.toString())
        } else {
            updatedLogs[date.toString()] = newAmount
        }

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
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(customSoundUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
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
                ReminderScheduler.scheduleReminder(
                    context = context,
                    itemId = updated.id,
                    title = updated.title,
                    timeStr = updated.reminderTime,
                    isHabit = true,
                    isRecurring = true,
                    customSoundUri = updated.customSoundUri
                )
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
        ReminderScheduler.scheduleReminder(
            context = context,
            itemId = newHabit.id,
            title = newHabit.title,
            timeStr = newHabit.reminderTime,
            isHabit = true,
            isRecurring = true,
            customSoundUri = newHabit.customSoundUri
        )
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
        val newStreak = if (nowDone) {
            task.renewalStreak + 1
        } else {
            (task.renewalStreak - 1).coerceAtLeast(0)
        }

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

        val updatedSubs = task.subTasks.map { sub ->
            if (sub.id == subTaskId) sub.copy(isDone = !sub.isDone) else sub
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
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(customSoundUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }

        if (existingId != null) {
            val idx = todos.indexOfFirst { it.id == existingId }
            if (idx != -1) {
                val oldSubs = todos[idx].subTasks.associateBy { it.title }
                val mergedSubs = subTaskTitles
                    .filter { it.isNotBlank() }
                    .map { line ->
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
                ReminderScheduler.scheduleReminder(
                    context = context,
                    itemId = updated.id,
                    title = updated.title,
                    timeStr = updated.reminderTime,
                    isHabit = false,
                    isRecurring = updated.isRecurring,
                    customSoundUri = updated.customSoundUri
                )
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
        ReminderScheduler.scheduleReminder(
            context = context,
            itemId = newTask.id,
            title = newTask.title,
            timeStr = newTask.reminderTime,
            isHabit = false,
            isRecurring = newTask.isRecurring,
            customSoundUri = newTask.customSoundUri
        )
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

        refreshCachedMetrics()
        awardXpAndCelebrate(
            context = context,
            title = "📓 Reflection Logged",
            subtitle = "Clarity strengthens daily execution",
            xpEarned = 35
        )
    }

    fun deleteJournalEntry(context: Context, id: String) {
        journals.removeAll { it.id == id }
        refreshCachedMetrics()
        persistStateAsync(context)
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
                refreshCachedMetrics()
                persistStateAsync(context)
                return
            }
        }
        notes.add(0, entry)
        refreshCachedMetrics()
        awardXpAndCelebrate(
            context = context,
            title = "🧠 Saved to Vault",
            subtitle = "\"${entry.title}\" indexed",
            xpEarned = 35
        )
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

    // ========================================================================
    // STARTER TEMPLATES FOR FIRST-TIME INSTALLS
    // ========================================================================
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
        for (i in 1..10) {
            waterLogs[today.minusDays(i.toLong()).toString()] = 3.0f
        }
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
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 7,
                    activityNote = "Morning hydration, sunlight & mobility",
                    status = HourProductivityStatus.PRODUCTIVE
                ),
                HourlyLogItem(
                    date = todayStr,
                    hour24 = 8,
                    activityNote = "90m Deep Work sprint with zero phone checks",
                    status = HourProductivityStatus.PEAK_FLOW
                )
            )
        )

        journals.add(
            JournalEntry(
                title = "Momentum & Daily Clarity",
                moodEmoji = "🔥",
                energyScore = 9,
                gratitudeOrWins = "Locked in a distraction-free morning block and logged goal progress.",
                dailyStory = "Watching my Brain Mood widget stay glowing green at the end of the day makes finishing every habit feel deeply satisfying.",
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
                    "Track both productive hours and wasted hours honestly.",
                    "Protect high-energy morning hours for your top goal."
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
