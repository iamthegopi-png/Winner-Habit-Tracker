package com.vibrant.habittracker

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// TAB 0: STUDIO HABIT TRACKER
// ============================================================================
@Composable
fun StudioHabitTrackerSection(
    vm: HabitTrackerViewModel,
    onOpenSentinelSheet: () -> Unit,
    onOpenBrainStudio: () -> Unit,
    onNewHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onLaunchFocusTimer: (Habit) -> Unit
) {
    val context = LocalContext.current
    var habitToLogCustom by remember { mutableStateOf<Habit?>(null) }

    val filteredHabits = remember(
        vm.habits.toList(),
        vm.selectedTimeOfDay.value,
        vm.selectedCategory.value
    ) {
        vm.habits.filter { h ->
            val matchesTime = vm.selectedTimeOfDay.value == TimeOfDay.ALL ||
                h.timeOfDay == vm.selectedTimeOfDay.value ||
                h.timeOfDay == TimeOfDay.ALL
            val matchesCat = vm.selectedCategory.value == "All" ||
                h.category.equals(vm.selectedCategory.value, ignoreCase = true)
            matchesTime && matchesCat
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "brain_mood_card") {
            BrainMoodDashboardCard(
                snapshot = vm.cachedBrainSnapshot.value,
                onOpenWidgetStudio = onOpenBrainStudio
            )
        }

        item(key = "sentinel_hero") {
            SentinelHeroCard(
                vm = vm,
                onOpenSentinelSheet = onOpenSentinelSheet
            )
        }

        item(key = "daily_quests") {
            DailyBonusQuestsRow(
                quests = vm.cachedDailyQuests.value,
                onClaimQuest = { vm.claimDailyQuest(context, it) }
            )
        }

        item(key = "seven_day_strip") {
            InteractiveSevenDayCalendarStrip(
                habits = vm.habits,
                selectedDate = vm.selectedDate.value,
                onSelectDate = { vm.selectedDate.value = it }
            )
        }

        item(key = "filter_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TimeOfDay.entries) { tod ->
                        val active = vm.selectedTimeOfDay.value == tod
                        Surface(
                            color = if (active) AppColors.MintGlow.copy(alpha = 0.16f) else AppColors.BgCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 0.8.dp,
                                color = if (active) AppColors.MintGlow else AppColors.BorderSubtle
                            ),
                            modifier = Modifier.clickable { vm.selectedTimeOfDay.value = tod }
                        ) {
                            Text(
                                text = "${tod.icon} ${tod.label}",
                                color = if (active) AppColors.MintGlow else AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        vm.habitViewMode.value = when (vm.habitViewMode.value) {
                            HabitViewMode.STUDIO_CARDS -> HabitViewMode.HEATMAP_GRID
                            HabitViewMode.HEATMAP_GRID -> HabitViewMode.COMPACT_WEEKLY
                            HabitViewMode.COMPACT_WEEKLY -> HabitViewMode.STUDIO_CARDS
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.BgCardElevated)
                ) {
                    Icon(
                        imageVector = when (vm.habitViewMode.value) {
                            HabitViewMode.STUDIO_CARDS -> Icons.Default.ViewAgenda
                            HabitViewMode.HEATMAP_GRID -> Icons.Default.GridOn
                            HabitViewMode.COMPACT_WEEKLY -> Icons.Default.ViewWeek
                        },
                        contentDescription = "Switch View",
                        tint = AppColors.CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onNewHabit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.MintGlow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        items(filteredHabits, key = { it.id }) { habit ->
            StudioHabitCard(
                habit = habit,
                selectedDate = vm.selectedDate.value,
                viewMode = vm.habitViewMode.value,
                onToggleOrStep = { vm.toggleOrStepHabit(context, habit, vm.selectedDate.value) },
                onDecrement = {
                    val cur = habit.valueOn(vm.selectedDate.value)
                    vm.setHabitMeasurement(
                        context = context,
                        habit = habit,
                        newAmount = (cur - habit.stepSize).coerceAtLeast(0f),
                        date = vm.selectedDate.value
                    )
                },
                onToggleAnyDate = { d -> vm.toggleOrStepHabit(context, habit, d) },
                onOpenCustomValue = { habitToLogCustom = habit },
                onLaunchFocus = { onLaunchFocusTimer(habit) },
                onEdit = { onEditHabit(habit) },
                onDelete = { vm.deleteHabit(context, habit.id) }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    habitToLogCustom?.let { habit ->
        CustomMeasurementDialog(
            habit = habit,
            selectedDate = vm.selectedDate.value,
            onDismiss = { habitToLogCustom = null },
            onSave = { amount ->
                vm.setHabitMeasurement(context, habit, amount, vm.selectedDate.value)
                habitToLogCustom = null
            }
        )
    }
}

@Composable
fun InteractiveSevenDayCalendarStrip(
    habits: List<Habit>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val total = habits.size.coerceAtLeast(1)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val doneCount = habits.count { it.isCompletedOn(date) }
                val progress = (doneCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AppColors.MintGlow.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelectDate(date) }
                        .padding(horizontal = 5.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isToday) {
                            "TODAY"
                        } else {
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase().take(3)
                        },
                        color = if (isSelected) AppColors.MintGlow else AppColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.dp.toPx()
                            drawCircle(color = AppColors.BorderSubtle, style = Stroke(width = stroke))
                            drawArc(
                                color = if (progress >= 1f) AppColors.MintGlow else AppColors.CyberCyan,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${date.dayOfMonth}",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$doneCount/${habits.size}",
                        color = if (doneCount == habits.size && habits.isNotEmpty()) {
                            AppColors.MintGlow
                        } else {
                            AppColors.TextMuted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StudioHabitCard(
    habit: Habit,
    selectedDate: LocalDate,
    viewMode: HabitViewMode,
    onToggleOrStep: () -> Unit,
    onDecrement: () -> Unit,
    onToggleAnyDate: (LocalDate) -> Unit,
    onOpenCustomValue: () -> Unit,
    onLaunchFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = habit.isCompletedOn(selectedDate)
    val valueToday = habit.valueOn(selectedDate)
    val progress = habit.progressOn(selectedDate)
    val streak = remember(habit.logs, selectedDate) { habit.currentStreak() }
    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 0.9.dp,
            color = if (isDone) habit.accentColor.copy(alpha = 0.6f) else AppColors.BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(habit.accentColor.copy(alpha = 0.14f))
                            .border(0.9.dp, habit.accentColor.copy(alpha = 0.4f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(habit.emoji, fontSize = 22.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (habit.habitStackingCue.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "🔗 ${habit.habitStackingCue}",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isDone) habit.accentColor else habit.accentColor.copy(alpha = 0.15f))
                        .border(1.4.dp, habit.accentColor, RoundedCornerShape(15.dp))
                        .studioBounceClick(onToggleOrStep),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            habit.type == HabitType.QUIT_SHIELD -> Icons.Default.Shield
                            isDone -> Icons.Default.Check
                            else -> Icons.Default.Add
                        },
                        contentDescription = "Complete",
                        tint = if (isDone) Color.Black else habit.accentColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(color = AppColors.SolarGold.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "🔥 $streak d",
                            color = AppColors.SolarGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (habit.reminderTime.isNotBlank()) {
                        Surface(color = AppColors.CyberCyan.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "🔔 ${habit.reminderTime}",
                                color = AppColors.CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (habit.customSoundUri.isNotBlank()) {
                        Surface(color = AppColors.ElectricViolet.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "🎵",
                                color = AppColors.ElectricViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                        Surface(color = habit.accentColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "${valueToday.cleanStr()}/${habit.targetValue.cleanStr()} ${habit.unit}",
                                color = habit.accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (habit.type == HabitType.FOCUS_TIMER || habit.type == HabitType.MEASUREMENT) {
                        SmallToolPillButton(Icons.Default.Timer, AppColors.CyberCyan, onLaunchFocus)
                        SmallToolPillButton(Icons.Default.Remove, AppColors.TextSecondary, onDecrement)
                        SmallToolPillButton(Icons.Default.MoreVert, habit.accentColor, onOpenCustomValue)
                    }
                    SmallToolPillButton(Icons.Default.Edit, AppColors.TextSecondary, onEdit)
                    SmallToolPillButton(Icons.Default.Delete, AppColors.TextMuted, onDelete)
                }
            }

            if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = habit.accentColor,
                    trackColor = AppColors.BgCardElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(Modifier.height(12.dp))

            when (viewMode) {
                HabitViewMode.HEATMAP_GRID -> GpuSixteenWeekHeatmapCanvas(habit)
                else -> StudioSevenDayInteractiveStrip(habit, onToggleAnyDate)
            }
        }
    }
}

@Composable
fun StudioSevenDayInteractiveStrip(
    habit: Habit,
    onToggleDate: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { d ->
            val done = habit.isCompletedOn(d)
            val raw = habit.valueOn(d)
            val isToday = d == today

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                    color = if (isToday) AppColors.TextPrimary else AppColors.TextMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                done -> habit.accentColor
                                raw > 0f -> habit.accentColor.copy(alpha = 0.35f)
                                else -> AppColors.BgCardElevated
                            }
                        )
                        .border(
                            width = if (isToday) 1.4.dp else 0.8.dp,
                            color = if (isToday) Color.White else AppColors.BorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleDate(d) },
                    contentAlignment = Alignment.Center
                ) {
                    if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                        if (raw > 0f) {
                            Text(
                                text = raw.cleanStr(),
                                color = if (done) Color.Black else AppColors.TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (done) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 1: GOAL ACHIEVEMENT & DAILY WORK MONITOR
// ============================================================================
@Composable
fun StudioGoalsAchievementSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var showCreateGoal by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalItem?>(null) }
    var loggingWorkForGoal by remember { mutableStateOf<GoalItem?>(null) }

    val today = remember { LocalDate.now() }
    val activeGoalsCount = vm.goals.count { !it.isCompleted }
    val completedGoalsCount = vm.goals.count { it.isCompleted }
    val workedTodayCount = vm.goals.count { it.workedToday(today) }
    val totalMinutesAllGoals = vm.goals.sumOf { it.totalMinutesInvested() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "goals_header_card") {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, AppColors.CyberCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GOAL ACHIEVEMENT STUDIO",
                                color = AppColors.CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Deadlines & Daily Execution",
                                color = AppColors.TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                editingGoal = null
                                showCreateGoal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Goal", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuditStatPill(Modifier.weight(1f), "Active", "$activeGoalsCount", AppColors.CyberCyan)
                        AuditStatPill(Modifier.weight(1f), "Worked Today", "$workedTodayCount/${vm.goals.size}", AppColors.MintGlow)
                        AuditStatPill(
                            Modifier.weight(1f),
                            "Invested",
                            "${totalMinutesAllGoals / 60}h ${totalMinutesAllGoals % 60}m",
                            AppColors.SolarGold
                        )
                    }

                    if (completedGoalsCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "🏆 $completedGoalsCount major goal(s) achieved",
                            color = AppColors.MintGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (vm.goals.isEmpty()) {
            item(key = "empty_goals") {
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎯", fontSize = 38.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Set Your First Measurable Goal",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Define a deadline and log your daily work progress step by step.",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateGoal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.CyberCyan)
                        ) {
                            Text("Create Goal", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(vm.goals, key = { it.id }) { goal ->
            StudioGoalCard(
                goal = goal,
                today = today,
                onLogWork = { loggingWorkForGoal = goal },
                onEdit = {
                    editingGoal = goal
                    showCreateGoal = true
                },
                onDelete = { vm.deleteGoal(context, goal.id) },
                onDeleteWorkLog = { logId -> vm.deleteGoalWorkLog(context, goal.id, logId) }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    if (showCreateGoal) {
        StudioCreateOrEditGoalDialog(
            goalToEdit = editingGoal,
            onDismiss = {
                showCreateGoal = false
                editingGoal = null
            },
            onSave = { id, title, emoji, cat, desc, deadlineIso, target, unit, colorHex ->
                vm.saveOrUpdateGoal(context, id, title, emoji, cat, desc, deadlineIso, target, unit, colorHex)
                showCreateGoal = false
                editingGoal = null
            }
        )
    }

    loggingWorkForGoal?.let { goal ->
        LogDailyGoalWorkDialog(
            goal = goal,
            onDismiss = { loggingWorkForGoal = null },
            onSave = { summary, delta, mins, date ->
                vm.logDailyGoalWork(context, goal.id, summary, delta, mins, date)
                loggingWorkForGoal = null
            }
        )
    }
}

@Composable
fun StudioGoalCard(
    goal: GoalItem,
    today: LocalDate,
    onLogWork: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleteWorkLog: (String) -> Unit
) {
    var expandedLogs by remember { mutableStateOf(false) }
    val daysLeft = goal.daysRemaining(today)
    val pace = goal.requiredDailyPace(today)
    val workedToday = goal.workedToday(today)
    val animatedProg by animateFloatAsState(
        targetValue = goal.progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "goalProg"
    )

    val deadlineBadgeText = when {
        goal.isCompleted -> "ACHIEVED ✓"
        daysLeft < 0 -> "OVERDUE (${-daysLeft}d)"
        daysLeft == 0L -> "DUE TODAY"
        else -> "${daysLeft}d LEFT"
    }
    val deadlineBadgeColor = when {
        goal.isCompleted -> AppColors.MintGlow
        daysLeft <= 3 -> AppColors.CrimsonRose
        daysLeft <= 10 -> AppColors.SolarGold
        else -> goal.accentColor
    }

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (goal.isCompleted) AppColors.MintGlow.copy(alpha = 0.65f) else goal.accentColor.copy(alpha = 0.42f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(goal.accentColor.copy(alpha = 0.16f))
                            .border(1.dp, goal.accentColor.copy(alpha = 0.4f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(goal.emoji, fontSize = 22.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = deadlineBadgeColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = deadlineBadgeText,
                                    color = deadlineBadgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text("Due ${goal.deadlineIso}", color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 4.5.dp.toPx()
                        drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                        drawArc(
                            color = if (goal.isCompleted) AppColors.MintGlow else goal.accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProg,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${goal.progressPercent}%",
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(goal.description, color = AppColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProg },
                color = if (goal.isCompleted) AppColors.MintGlow else goal.accentColor,
                trackColor = AppColors.BgCardElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.currentValue.cleanStr()} / ${goal.targetValue.cleanStr()} ${goal.unit}",
                    color = goal.accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = if (goal.isCompleted) "Target Reached!" else "Daily pace: ${pace.cleanStr()} ${goal.unit}/day",
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLogWork,
                    colors = ButtonDefaults.buttonColors(containerColor = goal.accentColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (workedToday) "Log More Work ✓" else "Log Today's Work",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = AppColors.BgCardElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { expandedLogs = !expandedLogs }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Logs (${goal.dailyLogs.size})",
                                color = AppColors.Platinum,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (expandedLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    SmallToolPillButton(Icons.Default.Edit, AppColors.TextSecondary, onEdit)
                    SmallToolPillButton(Icons.Default.Delete, AppColors.TextMuted, onDelete)
                }
            }

            AnimatedVisibility(
                visible = expandedLogs,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.BgSurface)
                        .border(0.8.dp, AppColors.BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DAILY WORK LOGS (${goal.totalMinutesInvested()} mins total)",
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (goal.dailyLogs.isEmpty()) {
                        Text(
                            text = "No daily work logged yet. Tap 'Log Today's Work' to start.",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        goal.dailyLogs.take(10).forEach { wl ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wl.workSummary,
                                        color = AppColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${wl.date} • +${wl.progressDelta.cleanStr()} ${goal.unit} • ${wl.minutesSpent}m",
                                        color = goal.accentColor,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteWorkLog(wl.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Log",
                                        tint = AppColors.TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: TO-DO MATRIX WITH AUTOMATIC DAILY RENEWAL
// ============================================================================
@Composable
fun StudioTodoMatrixSection(
    vm: HabitTrackerViewModel,
    onLaunchTaskTimer: (TodoTask) -> Unit
) {
    val context = LocalContext.current
    var selectedPriority by remember { mutableStateOf<TodoPriority?>(null) }
    var showAddTodo by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TodoTask?>(null) }

    val filteredTodos = remember(vm.todos.toList(), selectedPriority) {
        val base = if (selectedPriority == null) {
            vm.todos
        } else {
            vm.todos.filter { it.priority == selectedPriority }
        }
        base.sortedWith(compareBy<TodoTask> { it.isCompleted }.thenBy { it.priority.ordinal })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mission Matrix",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tasks marked 🔄 Auto-Renew reset automatically each day",
                    color = AppColors.MintGlow,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = {
                    editingTask = null
                    showAddTodo = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(4.dp))
                Text("New Task", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedPriority == null,
                        onClick = { selectedPriority = null },
                        label = { Text("All") }
                    )
                }
                items(TodoPriority.entries) { prio ->
                    FilterChip(
                        selected = selectedPriority == prio,
                        onClick = { selectedPriority = prio },
                        label = { Text(prio.shortTag) }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(
                onClick = { vm.checkAndAutoRenewDailyTasks(context, forceRenewForTesting = true) },
                border = BorderStroke(1.dp, AppColors.CyberCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AppColors.CyberCyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Renew Daily", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(filteredTodos, key = { it.id }) { task ->
                StudioTodoCard(
                    task = task,
                    onToggleMain = { vm.toggleTodo(context, task) },
                    onToggleSubTask = { subId -> vm.toggleSubTask(context, task, subId) },
                    onStartFocus = { onLaunchTaskTimer(task) },
                    onEdit = {
                        editingTask = task
                        showAddTodo = true
                    },
                    onDelete = { vm.deleteTodoTask(context, task.id) }
                )
            }
        }
    }

    if (showAddTodo) {
        StudioAddOrEditTodoDialog(
            taskToEdit = editingTask,
            onDismiss = {
                CelebrationSoundEngine.stopActiveCustomSound()
                showAddTodo = false
                editingTask = null
            },
            onSave = { id, title, cat, prio, remTime, mins, subs, rec, customSound ->
                CelebrationSoundEngine.stopActiveCustomSound()
                vm.saveOrUpdateTodoTask(context, id, title, cat, prio, remTime, mins, subs, rec, customSound)
                showAddTodo = false
                editingTask = null
            }
        )
    }
}

@Composable
fun StudioTodoCard(
    task: TodoTask,
    onToggleMain: () -> Unit,
    onToggleSubTask: (String) -> Unit,
    onStartFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val prioColor = Color(task.priority.colorHex)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 0.9.dp,
            color = if (task.isCompleted) AppColors.BorderSubtle else prioColor.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) AppColors.MintGlow else Color.Transparent)
                        .border(2.dp, if (task.isCompleted) AppColors.MintGlow else prioColor, CircleShape)
                        .studioBounceClick(onToggleMain),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = task.title,
                    color = if (task.isCompleted) AppColors.TextMuted else AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(color = prioColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = task.priority.shortTag,
                            color = prioColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (task.isRecurring) {
                        Surface(color = AppColors.MintGlow.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "🔄 Daily • ${task.renewalStreak}d",
                                color = AppColors.MintGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.reminderTime.isNotBlank()) {
                        Text(
                            text = "🔔 ${task.reminderTime}",
                            color = AppColors.CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (task.customSoundUri.isNotBlank()) {
                        Surface(color = AppColors.ElectricViolet.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "🎵",
                                color = AppColors.ElectricViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallToolPillButton(Icons.Default.PlayArrow, AppColors.CyberCyan, onStartFocus)
                    SmallToolPillButton(Icons.Default.Edit, AppColors.TextSecondary, onEdit)
                    SmallToolPillButton(Icons.Default.Close, AppColors.TextMuted, onDelete)
                }
            }

            if (task.subTasks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.BgCardElevated)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.subTasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubTask(sub.id) }
                        ) {
                            Icon(
                                imageVector = if (sub.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (sub.isDone) AppColors.MintGlow else AppColors.TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                color = if (sub.isDone) AppColors.TextMuted else AppColors.TextPrimary,
                                fontSize = 13.sp,
                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: HOURLY DAY PRODUCTIVITY AUDIT (WITH PAST-DAY NAVIGATOR)
// ============================================================================
@Composable
fun StudioDayProductivityAuditSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    val selectedDate = vm.selectedDate.value
    val today = remember { LocalDate.now() }
    val dateStr = selectedDate.toString()
    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    }

    val dayScore = vm.getDailyProductivityScore(selectedDate)
    val grade = vm.getDailyProductivityGrade(dayScore)
    val animatedDayScore by animateFloatAsState(
        targetValue = dayScore / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dayScoreAnim"
    )
    val logsForDay = remember(vm.hourlyLogs.toList(), dateStr) {
        vm.hourlyLogs.filter { it.date == dateStr }.associateBy { it.hour24 }
    }

    val peakHours = logsForDay.values.count {
        it.status == HourProductivityStatus.PEAK_FLOW || it.status == HourProductivityStatus.PRODUCTIVE
    }
    val wastedHours = logsForDay.values.count {
        it.status == HourProductivityStatus.TIME_WASTED || it.status == HourProductivityStatus.LOW_FOCUS
    }
    val netXpDay = logsForDay.values.sumOf { it.status.xpDelta }
    var editingHour24 by remember { mutableStateOf<Int?>(null) }

    val scoreColor = when {
        dayScore >= 80 -> AppColors.MintGlow
        dayScore >= 55 -> AppColors.CyberCyan
        dayScore >= 40 -> AppColors.SolarGold
        else -> AppColors.CrimsonRose
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "date_nav") {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { vm.selectedDate.value = selectedDate.minusDays(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = AppColors.TextPrimary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        vm.selectedDate.value = LocalDate.of(year, month + 1, dayOfMonth)
                                    },
                                    selectedDate.year,
                                    selectedDate.monthValue - 1,
                                    selectedDate.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.CyberCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (selectedDate == today) "Today ($formattedDate)" else formattedDate,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedDate != today) {
                            TextButton(onClick = { vm.selectedDate.value = today }) {
                                Text("Today", color = AppColors.MintGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(
                            onClick = {
                                if (selectedDate.isBefore(today)) {
                                    vm.selectedDate.value = selectedDate.plusDays(1)
                                }
                            },
                            enabled = selectedDate.isBefore(today)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Day",
                                tint = if (selectedDate.isBefore(today)) AppColors.TextPrimary else AppColors.TextMuted
                            )
                        }
                    }
                }
            }
        }

        item(key = "score_card") {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, scoreColor.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRODUCTIVITY REPORT • ${formattedDate.uppercase()}",
                                color = scoreColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Grade: $grade",
                                color = AppColors.TextPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Log every hour honestly. Wasted hours deduct score & XP.",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 6.5.dp.toPx()
                                drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                                drawArc(
                                    color = scoreColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedDayScore,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$dayScore%",
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuditStatPill(Modifier.weight(1f), "🔥 Productive", "$peakHours hrs", AppColors.MintGlow)
                        AuditStatPill(Modifier.weight(1f), "💀 Wasted", "$wastedHours hrs", AppColors.CrimsonRose)
                        AuditStatPill(
                            Modifier.weight(1f),
                            "⚡ Net XP",
                            if (netXpDay >= 0) "+$netXpDay XP" else "$netXpDay XP",
                            if (netXpDay >= 0) AppColors.SolarGold else AppColors.CrimsonRose
                        )
                    }
                }
            }
        }

        item(key = "ledger_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ Hourly Activity Ledger (6 AM – 11 PM)",
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Tap to log",
                    color = AppColors.CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val hoursList = (6..23).toList()
        items(hoursList, key = { "$dateStr-$it" }) { hour24 ->
            HourlySlotCard(
                hour24 = hour24,
                loggedItem = logsForDay[hour24],
                onClick = { editingHour24 = hour24 },
                onQuickPenalty = {
                    vm.logOrUpdateHour(
                        context = context,
                        hour24 = hour24,
                        activityNote = "Wasted time / Unproductive hour",
                        status = HourProductivityStatus.TIME_WASTED,
                        date = selectedDate
                    )
                },
                onQuickFlow = {
                    vm.logOrUpdateHour(
                        context = context,
                        hour24 = hour24,
                        activityNote = "Focused productive work block",
                        status = HourProductivityStatus.PEAK_FLOW,
                        date = selectedDate
                    )
                }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    editingHour24?.let { hour ->
        LogHourActivityDialog(
            hour24 = hour,
            existingLog = logsForDay[hour],
            onDismiss = { editingHour24 = null },
            onSave = { note, status ->
                vm.logOrUpdateHour(context, hour, note, status, selectedDate)
                editingHour24 = null
            },
            onDelete = {
                logsForDay[hour]?.let { vm.deleteHourlyLog(context, it.id) }
                editingHour24 = null
            }
        )
    }
}

@Composable
fun AuditStatPill(
    modifier: Modifier,
    title: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = AppColors.BgCardElevated,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = AppColors.TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HourlySlotCard(
    hour24: Int,
    loggedItem: HourlyLogItem?,
    onClick: () -> Unit,
    onQuickPenalty: () -> Unit,
    onQuickFlow: () -> Unit
) {
    val timeLabel = remember(hour24) {
        LocalTime.of(hour24, 0).format(DateTimeFormatter.ofPattern("hh:00 a"))
    }
    val statusColor = loggedItem?.status?.color ?: AppColors.BorderSubtle

    Surface(
        color = if (loggedItem != null) AppColors.BgCard else AppColors.BgSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 0.9.dp,
            color = if (loggedItem != null) statusColor.copy(alpha = 0.55f) else AppColors.BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = if (loggedItem != null) statusColor.copy(alpha = 0.16f) else AppColors.BgCardElevated,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = timeLabel,
                    color = if (loggedItem != null) statusColor else AppColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (loggedItem != null) {
                    Text(
                        text = loggedItem.activityNote,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${loggedItem.status.label} (${loggedItem.status.shortBadge})",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("Unlogged hour • Tap to record", color = AppColors.TextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.MintGlow.copy(alpha = 0.16f))
                        .border(0.8.dp, AppColors.MintGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .studioBounceClick(onQuickFlow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.MintGlow)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.CrimsonRose.copy(alpha = 0.16f))
                        .border(0.8.dp, AppColors.CrimsonRose.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .studioBounceClick(onQuickPenalty),
                    contentAlignment = Alignment.Center
                ) {
                    Text("-💀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.CrimsonRose)
                }
            }
        }
    }
}

// ============================================================================
// TAB 4: SANCTUARY HUB (JOURNAL + SECOND-BRAIN KNOWLEDGE VAULT)
// ============================================================================
@Composable
fun StudioSanctuaryHubSection(vm: HabitTrackerViewModel) {
    var subTab by remember { mutableIntStateOf(0) }
    val theme = vm.readingTheme.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgColor)
    ) {
        Surface(
            color = theme.cardColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.8.dp, theme.borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                listOf(0 to "📖 Sanctuary Journal", 1 to "🧠 Knowledge Vault").forEach { (idx, label) ->
                    val selected = subTab == idx
                    Surface(
                        color = if (selected) theme.accent else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { subTab = idx }
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.Black else theme.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = subTab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "SanctuarySwitch"
        ) { current ->
            if (current == 0) {
                StudioReflectionJournalSection(vm = vm)
            } else {
                StudioSecondBrainNotebookSection(vm = vm)
            }
        }
    }
}

@Composable
fun StudioReflectionJournalSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var showWriteJournal by remember { mutableStateOf(false) }
    var editingJournal by remember { mutableStateOf<JournalEntry?>(null) }
    val theme = vm.readingTheme.value
    val fontScale = vm.readingFontScale.floatValue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sanctuary Journal",
                    color = theme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Eye-soothing reflections & photo memories",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = {
                    editingJournal = null
                    showWriteJournal = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Write", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        SanctuaryComfortBar(
            currentTheme = theme,
            fontScale = fontScale,
            onSelectTheme = {
                vm.readingTheme.value = it
                vm.persistStateAsync(context)
            },
            onChangeFontScale = { newScale ->
                vm.readingFontScale.floatValue = newScale.coerceIn(0.85f, 1.35f)
                vm.persistStateAsync(context)
            }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(vm.journals, key = { it.id }) { entry ->
                StudioJournalEntryCard(
                    entry = entry,
                    theme = theme,
                    fontScale = fontScale,
                    onEdit = {
                        editingJournal = entry
                        showWriteJournal = true
                    },
                    onDelete = { vm.deleteJournalEntry(context, entry.id) }
                )
            }
        }
    }

    if (showWriteJournal) {
        StudioCreateOrEditJournalDialog(
            entryToEdit = editingJournal,
            onDismiss = {
                showWriteJournal = false
                editingJournal = null
            },
            onSave = { id, title, mood, energy, wins, story, mistakes, actions, photos, tags, push ->
                vm.saveOrUpdateJournalEntry(context, id, title, mood, energy, wins, story, mistakes, actions, photos, tags, push)
                showWriteJournal = false
                editingJournal = null
            }
        )
    }
}

@Composable
fun SanctuaryComfortBar(
    currentTheme: SanctuaryReadingTheme,
    fontScale: Float,
    onSelectTheme: (SanctuaryReadingTheme) -> Unit,
    onChangeFontScale: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(SanctuaryReadingTheme.entries) { st ->
                val active = currentTheme == st
                Surface(
                    color = if (active) st.accent.copy(alpha = 0.22f) else st.cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, if (active) st.accent else st.borderColor),
                    modifier = Modifier.clickable { onSelectTheme(st) }
                ) {
                    Text(
                        text = st.label,
                        color = if (active) st.accent else st.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(currentTheme.cardColor)
                .border(0.8.dp, currentTheme.borderColor, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = "A-",
                color = currentTheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onChangeFontScale(fontScale - 0.1f) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
            Text(
                text = "A+",
                color = currentTheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onChangeFontScale(fontScale + 0.1f) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun StudioJournalEntryCard(
    entry: JournalEntry,
    theme: SanctuaryReadingTheme,
    fontScale: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bodyFontSize = (15f * fontScale).sp
    val bodyLineHeight = (23f * fontScale).sp

    Surface(
        color = theme.cardColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.8.dp, theme.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(entry.moodEmoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = entry.title,
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "${entry.date}  •  ⚡ Energy: ${entry.energyScore}/10",
                            color = theme.accent,
                            fontSize = 12.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (entry.photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entry.photoUris) { uriStr ->
                        AsyncImage(
                            model = Uri.parse(uriStr),
                            contentDescription = "Memory Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, theme.borderColor, RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            if (entry.gratitudeOrWins.isNotBlank()) {
                Surface(
                    color = theme.accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✨ DAILY WINS & GRATITUDE",
                            color = theme.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.gratitudeOrWins,
                            color = theme.textPrimary,
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }

            if (entry.dailyStory.isNotBlank()) {
                Text(
                    text = entry.dailyStory,
                    color = theme.textPrimary,
                    fontSize = bodyFontSize,
                    lineHeight = bodyLineHeight,
                    fontFamily = FontFamily.Serif
                )
            }

            if (entry.frictionOrMistakes.isNotBlank()) {
                Surface(
                    color = AppColors.CrimsonRose.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🔍 REFLECTION ON FRICTION",
                            color = AppColors.CrimsonRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.frictionOrMistakes,
                            color = theme.textPrimary,
                            fontSize = (13.5f * fontScale).sp,
                            lineHeight = (20f * fontScale).sp
                        )
                    }
                }
            }

            if (entry.actionItems.isNotEmpty()) {
                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 ACTIONS TO TAKE",
                            color = AppColors.MintGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        entry.actionItems.forEach { act ->
                            Text(
                                text = "• $act",
                                color = theme.textPrimary,
                                fontSize = (13.5f * fontScale).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioSecondBrainNotebookSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var catFilter by remember { mutableStateOf<NoteCategory?>(null) }
    var showCreateNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NotebookEntry?>(null) }
    val theme = vm.readingTheme.value
    val fontScale = vm.readingFontScale.floatValue

    val displayedNotes = remember(vm.notes.toList(), query, catFilter) {
        vm.notes
            .filter { n ->
                val matchesCat = catFilter == null || n.category == catFilter
                val matchesQuery = query.isBlank() ||
                    n.title.contains(query, ignoreCase = true) ||
                    n.authorOrSubtitle.contains(query, ignoreCase = true) ||
                    n.detailedNotes.contains(query, ignoreCase = true)
                matchesCat && matchesQuery
            }
            .sortedByDescending { it.isPinned }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Second-Brain Vault",
                    color = theme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Notes, mental models & active recall",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = {
                    editingNote = null
                    showCreateNote = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        SanctuaryComfortBar(
            currentTheme = theme,
            fontScale = fontScale,
            onSelectTheme = {
                vm.readingTheme.value = it
                vm.persistStateAsync(context)
            },
            onChangeFontScale = { newScale ->
                vm.readingFontScale.floatValue = newScale.coerceIn(0.85f, 1.35f)
                vm.persistStateAsync(context)
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search notes, quotes, or takeaways...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.textSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = catFilter == null,
                    onClick = { catFilter = null },
                    label = { Text("All (${vm.notes.size})") }
                )
            }
            items(NoteCategory.entries) { c ->
                FilterChip(
                    selected = catFilter == c,
                    onClick = { catFilter = c },
                    label = { Text(c.label) }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(displayedNotes, key = { it.id }) { note ->
                StudioNotebookCard(
                    note = note,
                    theme = theme,
                    fontScale = fontScale,
                    onTogglePin = { vm.togglePinNote(context, note.id) },
                    onEdit = {
                        editingNote = note
                        showCreateNote = true
                    },
                    onDelete = { vm.deleteNotebookEntry(context, note.id) }
                )
            }
        }
    }

    if (showCreateNote) {
        StudioCreateOrEditNoteDialog(
            noteToEdit = editingNote,
            onDismiss = {
                showCreateNote = false
                editingNote = null
            },
            onSave = { entry ->
                vm.saveOrUpdateNotebookEntry(context, editingNote?.id, entry)
                showCreateNote = false
                editingNote = null
            }
        )
    }
}

@Composable
fun StudioNotebookCard(
    note: NotebookEntry,
    theme: SanctuaryReadingTheme,
    fontScale: Float,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(note.coverColorHex)
    var flipped by remember { mutableStateOf(false) }
    val bodyFontSize = (14.5f * fontScale).sp
    val bodyLineHeight = (22f * fontScale).sp

    Surface(
        color = theme.cardColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.9.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = note.category.label.uppercase(),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (note.category == NoteCategory.BOOK_SUMMARY) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "• ${note.readingProgress}% Read",
                                color = theme.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = note.title,
                        color = theme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = note.authorOrSubtitle,
                        color = theme.textSecondary,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) AppColors.SolarGold else theme.textSecondary.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            if (note.favoriteQuote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = theme.bgColor,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${note.favoriteQuote}”",
                        color = theme.accent,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (note.keyTakeaways.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "💡 KEY TAKEAWAYS",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                note.keyTakeaways.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        color = theme.textPrimary,
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (note.detailedNotes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note.detailedNotes,
                    color = theme.textSecondary,
                    fontSize = bodyFontSize,
                    lineHeight = bodyLineHeight,
                    fontFamily = FontFamily.Serif
                )
            }

            if (note.flashcards.isNotEmpty()) {
                val fc = note.flashcards.first()
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.9.dp, accent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { flipped = !flipped }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (flipped) {
                                "🧠 ANSWER (Tap to flip)"
                            } else {
                                "🧠 ACTIVE RECALL QUESTION (Tap to reveal)"
                            },
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (flipped) fc.answer else fc.question,
                            color = theme.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = bodyFontSize
                        )
                    }
                }
            }
        }
    }
}
