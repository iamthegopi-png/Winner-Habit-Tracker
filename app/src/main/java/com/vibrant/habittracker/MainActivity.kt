package com.vibrant.habittracker

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppColors.BgDeep,
                    surface = AppColors.BgCard,
                    primary = AppColors.MintGlow,
                    secondary = AppColors.ElectricViolet,
                    onBackground = AppColors.TextPrimary,
                    onSurface = AppColors.TextPrimary
                )
            ) {
                StudioHabitApp()
            }
        }
    }
}

// ============================================================================
// MAIN STUDIO SCAFFOLD, GAMIFICATION HEADER & NAVIGATION
// ============================================================================
@Composable
fun StudioHabitApp(vm: HabitTrackerViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.initializeIfNeeded(context)
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showCloudVaultModal by remember { mutableStateOf(false) }
    var showFocusTimerModal by remember { mutableStateOf(false) }
    var showCreateHabitModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B101B), AppColors.BgDeep, Color(0xFF05070B))
                )
            )
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                StudioGamifiedHeader(
                    vm = vm,
                    onOpenTimer = { showFocusTimerModal = true },
                    onOpenAnalytics = { showAnalyticsSheet = true },
                    onOpenCloud = { showCloudVaultModal = true }
                )
            },
            bottomBar = {
                StudioBottomDock(
                    currentTab = currentTab,
                    onSelectTab = { currentTab = it },
                    onQuickAction = {
                        when (currentTab) {
                            0 -> showCreateHabitModal = true
                            else -> showFocusTimerModal = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Live Active Pomodoro Banner (Visible anywhere when timer is running)
                AnimatedVisibility(visible = vm.isFocusTimerRunning.value) {
                    ActiveFocusTimerBanner(
                        vm = vm,
                        onExpand = { showFocusTimerModal = true }
                    )
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 16 } togetherWith
                                fadeOut(tween(180))
                    },
                    label = "StudioTabSwitcher"
                ) { tab ->
                    when (tab) {
                        0 -> StudioHabitTrackerSection(
                            vm = vm,
                            onNewHabit = { showCreateHabitModal = true },
                            onLaunchFocusTimer = { habit ->
                                vm.startFocusTimer(
                                    context = context,
                                    title = "${habit.emoji} ${habit.title}",
                                    minutes = if (habit.type == HabitType.FOCUS_TIMER) habit.stepSize.toInt().coerceAtLeast(15) else 25,
                                    habitId = habit.id
                                )
                                showFocusTimerModal = true
                            },
                            onOpenAnalytics = { showAnalyticsSheet = true }
                        )
                        1 -> StudioTodoMatrixSection(
                            vm = vm,
                            onLaunchTaskTimer = { task ->
                                vm.startFocusTimer(
                                    context = context,
                                    title = "🎯 ${task.title}",
                                    minutes = task.estimatedMinutes.coerceAtLeast(10),
                                    habitId = null
                                )
                                showFocusTimerModal = true
                            }
                        )
                        2 -> StudioReflectionJournalSection(vm = vm)
                        3 -> StudioSecondBrainNotebookSection(vm = vm)
                    }
                }
            }
        }

        // Deep Focus / Pomodoro Modal
        if (showFocusTimerModal) {
            FocusTimerStudioDialog(
                vm = vm,
                onDismiss = { showFocusTimerModal = false }
            )
        }

        // Studio Analytics & Insights Modal
        if (showAnalyticsSheet) {
            StudioAnalyticsModal(
                vm = vm,
                onDismiss = { showAnalyticsSheet = false }
            )
        }

        // Google Drive & Cloud Backup Vault Modal
        if (showCloudVaultModal) {
            GoogleDriveBackupVaultDialog(
                vm = vm,
                onDismiss = { showCloudVaultModal = false }
            )
        }

        // Create New Habit Dialog
        if (showCreateHabitModal) {
            StudioCreateHabitDialog(
                onDismiss = { showCreateHabitModal = false },
                onSave = { title, emoji, cat, timeOfDay, cue, type, colorHex, target, unit, step ->
                    vm.createHabit(context, title, emoji, cat, timeOfDay, cue, type, colorHex, target, unit, step)
                    showCreateHabitModal = false
                }
            )
        }

        // FULL-SCREEN STUDIO CELEBRATION & XP OVERLAY
        AnimatedVisibility(
            visible = vm.showCelebration.value,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(450))
        ) {
            StudioFullScreenCelebrationOverlay(
                title = vm.celebrationTitle.value,
                subtitle = vm.celebrationSubtitle.value,
                xpEarned = vm.celebrationXpEarned.intValue,
                level = vm.userLevel,
                rank = vm.userRankTitle,
                onDismiss = { vm.showCelebration.value = false }
            )
        }
    }
}

@Composable
fun StudioGamifiedHeader(
    vm: HabitTrackerViewModel,
    onOpenTimer: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val animatedXpProgress by animateFloatAsState(
        targetValue = vm.xpProgressInLevel,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "xpBar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Level Badge + Rank & XP Progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.MintGlow, AppColors.CyberCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lv.${vm.userLevel}",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = vm.userRankTitle,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Surface(
                            color = AppColors.SolarGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AppColors.SolarGold.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🛡️ ${vm.streakShields.intValue}",
                                color = AppColors.SolarGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { animatedXpProgress },
                            color = AppColors.MintGlow,
                            trackColor = AppColors.BgCardElevated,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${vm.userXp.intValue} XP",
                            color = AppColors.MintGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right Action Buttons: Focus Timer, Analytics, Google Cloud Backup
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderActionButton(
                    icon = Icons.Default.Timer,
                    tint = AppColors.CyberCyan,
                    onClick = onOpenTimer
                )
                HeaderActionButton(
                    icon = Icons.Default.Insights,
                    tint = AppColors.MintGlow,
                    onClick = onOpenAnalytics
                )
                HeaderActionButton(
                    icon = if (vm.connectedGoogleEmail.value != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    tint = if (vm.connectedGoogleEmail.value != null) AppColors.MintGlow else AppColors.ElectricViolet,
                    onClick = onOpenCloud
                )
            }
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(AppColors.BgCard)
            .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(13.dp))
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ActiveFocusTimerBanner(
    vm: HabitTrackerViewModel,
    onExpand: () -> Unit
) {
    val context = LocalContext.current
    val mins = vm.focusRemainingSeconds.longValue / 60
    val secs = vm.focusRemainingSeconds.longValue % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    Surface(
        color = AppColors.CyberCyan.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.CyberCyan),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onExpand() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = AppColors.CyberCyan)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = vm.activeFocusTitle.value,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Deep Focus Session Active • Tap to expand",
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormatted,
                    color = AppColors.CyberCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { vm.togglePauseFocusTimer(context) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (vm.isFocusTimerRunning.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AppColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StudioBottomDock(
    currentTab: Int,
    onSelectTab: (Int) -> Unit,
    onQuickAction: () -> Unit
) {
    Surface(
        color = AppColors.BgSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        border = BorderStroke(1.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(0, "Habits", Icons.Default.AutoAwesomeMosaic),
                Triple(1, "Tasks", Icons.Default.Checklist),
                Triple(2, "Journal", Icons.Default.AutoStories),
                Triple(3, "Vault", Icons.Default.Psychology)
            )

            items.forEach { (index, title, icon) ->
                val selected = currentTab == index
                val pillBg by animateColorAsState(
                    if (selected) AppColors.MintGlow.copy(alpha = 0.16f) else Color.Transparent,
                    label = "navBg"
                )
                val tint by animateColorAsState(
                    if (selected) AppColors.MintGlow else AppColors.TextSecondary,
                    label = "navTint"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
                    AnimatedVisibility(visible = selected) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = title,
                                color = tint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Studio Gradient Quick-Create Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.MintGlow, AppColors.CyberCyan)
                        )
                    )
                    .clickable { onQuickAction() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentTab == 0) Icons.Default.Add else Icons.Default.Timer,
                    contentDescription = "Quick Action",
                    tint = Color.Black
                )
            }
        }
    }
}

// ============================================================================
// SECTION 1: STUDIO HABIT TRACKER (Interactive Calendar, Time-of-Day, 3 Views)
// ============================================================================
@Composable
fun StudioHabitTrackerSection(
    vm: HabitTrackerViewModel,
    onNewHabit: () -> Unit,
    onLaunchFocusTimer: (Habit) -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val context = LocalContext.current
    var habitToLogCustomValue by remember { mutableStateOf<Habit?>(null) }

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
        // 1. Interactive 7-Day Score Rings Strip (Tap any day to log/inspect)
        item {
            InteractiveSevenDayCalendarStrip(
                habits = vm.habits,
                selectedDate = vm.selectedDate.value,
                onSelectDate = { vm.selectedDate.value = it }
            )
        }

        // 2. Time-of-Day Routine Filter + View Mode Switcher
        item {
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
                            color = if (active) AppColors.MintGlow.copy(alpha = 0.18f) else AppColors.BgCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (active) AppColors.MintGlow else AppColors.BorderSubtle
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

                Spacer(Modifier.width(8.dp))

                // Cycle between Studio Cards -> 16-Week Heatmap -> Compact Weekly
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
                        tint = AppColors.ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Empty State if user cleared all habits
        if (filteredHabits.isEmpty()) {
            item {
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✨", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Build Your Keystone Routine",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Create checklists, quantifiable measurements, focus timers, or abstinence shields.",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onNewHabit,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
                        ) {
                            Text("Create First Habit", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Habit Cards List
        items(filteredHabits, key = { it.id }) { habit ->
            StudioHabitCard(
                habit = habit,
                selectedDate = vm.selectedDate.value,
                viewMode = vm.habitViewMode.value,
                onToggleOrStep = { vm.toggleOrStepHabit(context, habit, vm.selectedDate.value) },
                onDecrement = {
                    val cur = habit.valueOn(vm.selectedDate.value)
                    vm.setHabitMeasurement(context, habit, (cur - habit.stepSize).coerceAtLeast(0f), vm.selectedDate.value)
                },
                onToggleAnyDate = { d -> vm.toggleOrStepHabit(context, habit, d) },
                onOpenCustomValue = { habitToLogCustomValue = habit },
                onLaunchFocus = { onLaunchFocusTimer(habit) },
                onDelete = { vm.deleteHabit(context, habit.id) }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    habitToLogCustomValue?.let { habit ->
        CustomMeasurementDialog(
            habit = habit,
            selectedDate = vm.selectedDate.value,
            onDismiss = { habitToLogCustomValue = null },
            onSave = { amount ->
                vm.setHabitMeasurement(context, habit, amount, vm.selectedDate.value)
                habitToLogCustomValue = null
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
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val total = habits.size.coerceAtLeast(1)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val doneCount = habits.count { it.isCompletedOn(date) }
                val progress by animateFloatAsState(
                    targetValue = (doneCount.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(500),
                    label = "dayRing"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) AppColors.MintGlow.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .clickable { onSelectDate(date) }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isToday) "TODAY" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                        color = if (isSelected) AppColors.MintGlow else AppColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.5.dp.toPx()
                            drawCircle(
                                color = AppColors.BorderSubtle,
                                style = Stroke(width = stroke)
                            )
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
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$doneCount/${habits.size}",
                        color = if (doneCount == habits.size && habits.isNotEmpty()) AppColors.MintGlow else AppColors.TextMuted,
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
    onDelete: () -> Unit
) {
    val isDone = habit.isCompletedOn(selectedDate)
    val valueToday = habit.valueOn(selectedDate)
    val progress = habit.progressOn(selectedDate)
    val streak = habit.currentStreak()
    val rate30d = habit.completionRate30Days()

    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDone) habit.accentColor.copy(alpha = 0.65f) else AppColors.BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Icon, Title, Stacking Cue, and Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(habit.accentColor.copy(alpha = 0.16f))
                            .border(1.dp, habit.accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = habit.emoji, fontSize = 23.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.title,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Habit Stacking Cue Badge
                        if (habit.habitStackingCue.isNotBlank()) {
                            Text(
                                text = "🔗 ${habit.habitStackingCue}",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🔥 $streak d",
                                color = AppColors.SolarGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• $rate30d% (30d)",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                            if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                                Text(
                                    text = "• ${valueToday.cleanStr()}/${habit.targetValue.cleanStr()} ${habit.unit}",
                                    color = habit.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Focus Timer Quick Launch
                    if (habit.type == HabitType.FOCUS_TIMER || habit.type == HabitType.MEASUREMENT) {
                        IconButton(
                            onClick = onLaunchFocus,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.BgCardElevated)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Start Focus Timer",
                                tint = AppColors.CyberCyan,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.BgCardElevated)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenCustomValue,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.BgCardElevated)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Custom Value",
                                tint = habit.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Primary Complete / Increment Button
                    Box(
                        modifier = Modifier
                            .scale(checkScale)
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDone) habit.accentColor
                                else habit.accentColor.copy(alpha = 0.16f)
                            )
                            .border(1.5.dp, habit.accentColor, RoundedCornerShape(14.dp))
                            .clickable { onToggleOrStep() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                habit.type == HabitType.QUIT_SHIELD -> Icons.Default.Shield
                                isDone -> Icons.Default.Check
                                habit.type == HabitType.CHECKLIST -> Icons.Default.Check
                                else -> Icons.Default.Add
                            },
                            contentDescription = "Complete",
                            tint = if (isDone) Color.Black else habit.accentColor
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Progress Bar for quantifiable / timer habits
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

            Spacer(Modifier.height(14.dp))

            // Dynamic Visualization Based on Selected Mode
            when (viewMode) {
                HabitViewMode.STUDIO_CARDS, HabitViewMode.COMPACT_WEEKLY -> {
                    StudioSevenDayInteractiveStrip(
                        habit = habit,
                        onToggleDate = onToggleAnyDate
                    )
                }
                HabitViewMode.HEATMAP_GRID -> {
                    StudioSixteenWeekHeatmap(habit = habit)
                }
            }
        }
    }
}

@Composable
fun StudioSevenDayInteractiveStrip(
    habit: Habit,
    onToggleDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }

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
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            when {
                                done -> habit.accentColor
                                raw > 0f -> habit.accentColor.copy(alpha = 0.35f)
                                else -> AppColors.BgCardElevated
                            }
                        )
                        .border(
                            width = if (isToday) 1.5.dp else 1.dp,
                            color = if (isToday) Color.White else AppColors.BorderSubtle,
                            shape = RoundedCornerShape(11.dp)
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
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioSixteenWeekHeatmap(habit: Habit) {
    val today = LocalDate.now()
    val weeks = 16

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (w in (weeks - 1) downTo 0) {
            Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                for (d in 6 downTo 0) {
                    val cellDate = today.minusDays((w * 7 + d).toLong())
                    val isDone = habit.isCompletedOn(cellDate)
                    val raw = habit.valueOn(cellDate)
                    val cellColor = when {
                        isDone -> habit.accentColor
                        raw > 0f -> habit.accentColor.copy(alpha = 0.45f)
                        else -> AppColors.BgCardElevated
                    }
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(cellColor)
                    )
                }
            }
        }
    }
}

// ============================================================================
// SECTION 2: PRIORITY-MATRIX TO-DO LIST WITH SUBTASKS & FOCUS LAUNCHER
// ============================================================================
@Composable
fun StudioTodoMatrixSection(
    vm: HabitTrackerViewModel,
    onLaunchTaskTimer: (TodoTask) -> Unit
) {
    val context = LocalContext.current
    var selectedPriorityFilter by remember { mutableStateOf<TodoPriority?>(null) }
    var showAddTaskModal by remember { mutableStateOf(false) }

    val filteredTodos = remember(vm.todos.toList(), selectedPriorityFilter) {
        val base = if (selectedPriorityFilter == null) vm.todos
        else vm.todos.filter { it.priority == selectedPriorityFilter }
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
            Column {
                Text(
                    text = "Execution & Priority Matrix",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vm.todos.count { !it.isCompleted }} active missions • Tap timer to enter focus mode",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showAddTaskModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddTask, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("New Task", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Priority Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPriorityFilter == null,
                    onClick = { selectedPriorityFilter = null },
                    label = { Text("All Priorities") }
                )
            }
            items(TodoPriority.entries) { prio ->
                FilterChip(
                    selected = selectedPriorityFilter == prio,
                    onClick = { selectedPriorityFilter = prio },
                    label = { Text(prio.label) }
                )
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
                    onDelete = { vm.deleteTodoTask(context, task.id) }
                )
            }
        }
    }

    if (showAddTaskModal) {
        StudioAddTodoDialog(
            onDismiss = { showAddTaskModal = false },
            onSave = { title, cat, prio, timeBlock, mins, subs, recurring ->
                vm.addTodoTask(context, title, cat, prio, timeBlock, mins, subs, recurring)
                showAddTaskModal = false
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
    onDelete: () -> Unit
) {
    val prioColor = Color(task.priority.colorHex)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) AppColors.BorderSubtle else prioColor.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (task.isCompleted) AppColors.MintGlow else Color.Transparent)
                            .border(2.dp, if (task.isCompleted) AppColors.MintGlow else prioColor, CircleShape)
                            .clickable { onToggleMain() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            color = if (task.isCompleted) AppColors.TextMuted else AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = prioColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = task.priority.label,
                                    color = prioColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "📂 ${task.category} • ⏱️ ${task.estimatedMinutes}m",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onStartFocus, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.PlayCircleOutline, "Focus", tint = AppColors.CyberCyan)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "Delete", tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Nested Subtasks Checklist
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
// SECTION 3: MULTIMEDIA REFLECTION JOURNAL (Energy Slider, Photos, Mistakes & Actions)
// ============================================================================
@Composable
fun StudioReflectionJournalSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var showWriteJournalModal by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    text = "Daily Life & Memory Journal",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Photo Memories • Daily Wins • Mistakes Audit • Next Actions",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showWriteJournalModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.EditNote, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Log Day", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(vm.journals, key = { it.id }) { entry ->
                StudioJournalEntryCard(
                    entry = entry,
                    onDelete = { vm.deleteJournalEntry(context, entry.id) }
                )
            }
        }
    }

    if (showWriteJournalModal) {
        StudioCreateJournalDialog(
            onDismiss = { showWriteJournalModal = false },
            onSave = { title, mood, energy, wins, story, mistakes, actions, photos, tags, pushToTodo ->
                vm.addJournalEntry(
                    context = context,
                    title = title,
                    moodEmoji = mood,
                    energyScore = energy,
                    wins = wins,
                    story = story,
                    mistakes = mistakes,
                    actions = actions,
                    photos = photos,
                    tags = tags,
                    pushActionsToTodo = pushToTodo
                )
                showWriteJournalModal = false
            }
        )
    }
}

@Composable
fun StudioJournalEntryCard(
    entry: JournalEntry,
    onDelete: () -> Unit
) {
    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.moodEmoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = entry.title,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${entry.date}  •  ⚡ Energy: ${entry.energyScore}/10",
                            color = AppColors.MintGlow,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            // Memory Photos Carousel
            if (entry.photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entry.photoUris) { uriStr ->
                        AsyncImage(
                            model = Uri.parse(uriStr),
                            contentDescription = "Memory Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(135.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            if (entry.gratitudeOrWins.isNotBlank()) {
                Surface(
                    color = AppColors.SolarGold.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppColors.SolarGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🏆 KEY WINS & GRATITUDE", color = AppColors.SolarGold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.gratitudeOrWins, color = AppColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (entry.dailyStory.isNotBlank()) {
                Column {
                    Text("📖 DAILY ACTIVITIES & REFLECTION", color = AppColors.CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.dailyStory, color = AppColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            if (entry.frictionOrMistakes.isNotBlank()) {
                Surface(
                    color = AppColors.CrimsonRose.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppColors.CrimsonRose.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ WHAT WENT WRONG / MISTAKES AUDIT", color = AppColors.CrimsonRose, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.frictionOrMistakes, color = AppColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (entry.actionItems.isNotEmpty()) {
                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppColors.MintGlow.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🎯 CORRECTIVE ACTIONS TO TAKE", color = AppColors.MintGlow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        entry.actionItems.forEach { act ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("→ ", color = AppColors.MintGlow, fontWeight = FontWeight.Bold)
                                Text(act, color = AppColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION 4: SECOND-BRAIN NOTEBOOK & BOOK SUMMARY VAULT (With Flip Flashcards)
// ============================================================================
@Composable
fun StudioSecondBrainNotebookSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<NoteCategory?>(null) }
    var showCreateNoteModal by remember { mutableStateOf(false) }

    val displayedNotes = remember(vm.notes.toList(), searchQuery, selectedCat) {
        vm.notes.filter { n ->
            val matchesCat = selectedCat == null || n.category == selectedCat
            val matchesSearch = searchQuery.isBlank() ||
                    n.title.contains(searchQuery, ignoreCase = true) ||
                    n.authorOrSubtitle.contains(searchQuery, ignoreCase = true) ||
                    n.detailedNotes.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesSearch
        }.sortedByDescending { it.isPinned }
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
            Column {
                Text(
                    text = "Second-Brain & Book Vault",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Book Summaries • Active Recall Flashcards • Mental Models",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showCreateNoteModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ElectricViolet),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.LibraryAdd, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Add Vault", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search book titles, authors, quotes, or takeaways...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = AppColors.TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCat == null,
                    onClick = { selectedCat = null },
                    label = { Text("All (${vm.notes.size})") }
                )
            }
            items(NoteCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { selectedCat = cat },
                    label = { Text(cat.label) }
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
                    onTogglePin = { vm.togglePinNote(context, note.id) },
                    onDelete = { vm.deleteNotebookEntry(context, note.id) }
                )
            }
        }
    }

    if (showCreateNoteModal) {
        StudioCreateNoteDialog(
            onDismiss = { showCreateNoteModal = false },
            onSave = { entry ->
                vm.addNotebookEntry(context, entry)
                showCreateNoteModal = false
            }
        )
    }
}

@Composable
fun StudioNotebookCard(
    note: NotebookEntry,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(note.coverColorHex)
    var showFlashcardAnswer by remember { mutableStateOf(false) }

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                                color = AppColors.MintGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = note.title,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = note.authorOrSubtitle,
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.PinInvoke,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) AppColors.SolarGold else AppColors.TextMuted
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null, tint = AppColors.TextMuted)
                }
            }

            if (note.favoriteQuote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = AppColors.BgCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${note.favoriteQuote}”",
                        color = AppColors.MintGlow,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (note.keyTakeaways.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("💡 KEY TAKEAWAYS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                note.keyTakeaways.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = accent, fontWeight = FontWeight.Bold)
                        Text(item, color = AppColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (note.detailedNotes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note.detailedNotes,
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Interactive Active-Recall Flashcard
            if (note.flashcards.isNotEmpty()) {
                val card = note.flashcards.first()
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFlashcardAnswer = !showFlashcardAnswer }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (showFlashcardAnswer) "🧠 FLASHCARD ANSWER (Tap to flip)" else "🧠 ACTIVE RECALL QUESTION (Tap to reveal)",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (showFlashcardAnswer) card.answer else card.question,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// BUILT-IN POMODORO FOCUS TIMER MODAL, ANALYTICS & GOOGLE DRIVE BACKUP VAULT
// ============================================================================
@Composable
fun FocusTimerStudioDialog(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rem = vm.focusRemainingSeconds.longValue
    val tot = vm.focusTotalSeconds.longValue.coerceAtLeast(1L)
    val progress = (rem.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
    val formatted = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, AppColors.CyberCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DEEP FOCUS STUDIO",
                    color = AppColors.CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = vm.activeFocusTitle.value,
                    color = AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx()
                        drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                        drawArc(
                            brush = Brush.sweepGradient(listOf(AppColors.MintGlow, AppColors.CyberCyan, AppColors.ElectricViolet)),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatted,
                            color = AppColors.TextPrimary,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (vm.isFocusTimerRunning.value) "FLOW STATE ACTIVE" else "READY",
                            color = AppColors.MintGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Duration Presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = (tot / 60L) == mins.toLong(),
                            onClick = { vm.startFocusTimer(context, vm.activeFocusTitle.value, mins, vm.activeFocusHabitId.value) },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.stopFocusTimer() }) {
                        Text("Reset")
                    }
                    Button(
                        onClick = { vm.togglePauseFocusTimer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.CyberCyan)
                    ) {
                        Text(
                            text = if (vm.isFocusTimerRunning.value) "Pause" else "Start Focus",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioAnalyticsModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, AppColors.BorderSubtle),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Studio Performance Analytics",
                        color = AppColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary)
                    }
                }

                val bestStreakOverall = vm.habits.maxOfOrNull { it.bestStreak() } ?: 0
                val avgRate = if (vm.habits.isEmpty()) 0 else vm.habits.map { it.completionRate30Days() }.average().roundToInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCardBox(Modifier.weight(1f), "🔥", "$bestStreakOverall d", "Peak Streak", AppColors.SunsetCoral)
                    StatCardBox(Modifier.weight(1f), "📈", "$avgRate%", "30d Consistency", AppColors.MintGlow)
                    StatCardBox(Modifier.weight(1f), "⚡", "Lv.${vm.userLevel}", "${vm.userXp.intValue} Total XP", AppColors.ElectricViolet)
                }

                vm.habits.forEach { habit ->
                    Surface(
                        color = AppColors.BgCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, AppColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${habit.emoji} ${habit.title}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${habit.completionRate30Days()}% (30d)", color = habit.accentColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            StudioSixteenWeekHeatmap(habit = habit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCardBox(modifier: Modifier, icon: String, value: String, label: String, accent: Color) {
    Surface(
        modifier = modifier,
        color = AppColors.BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(label, color = accent, fontSize = 11.sp)
        }
    }
}

@Composable
fun GoogleDriveBackupVaultDialog(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(vm.connectedGoogleEmail.value ?: "") }
    val storage = remember { StudioStorageManager(context) }

    // Native Android Storage Access Framework -> Saves directly to Google Drive!
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val json = vm.persistState(context)
            val ok = storage.exportBackupToUri(it, json)
            Toast.makeText(
                context,
                if (ok) "Backup saved to Google Drive / Storage!" else "Export failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val content = storage.readBackupFromUri(it)
            if (content != null && vm.restoreFromJsonString(content)) {
                vm.persistState(context)
                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = {
            Text("☁️ Google Account & Drive Backup", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "All habits, streaks, tasks, journal photos, and book summaries auto-save locally and can be backed up directly to your Google Drive.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Last Sync: ${vm.lastCloudBackup.value}", color = AppColors.MintGlow, fontSize = 12.sp)

                OutlinedButton(
                    onClick = { exportLauncher.launch("PulseHabit_GoogleDrive_Backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.MintGlow)
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = AppColors.MintGlow)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Backup to Google Drive", color = AppColors.MintGlow)
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.CyberCyan)
                ) {
                    Icon(Icons.Default.CloudDownload, null, tint = AppColors.CyberCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Backup from Google Drive", color = AppColors.CyberCyan)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.connectedGoogleEmail.value = email.ifBlank { null }
                    vm.syncToGoogleCloud(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save & Sync", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// ============================================================================
// FULL-SCREEN CELEBRATION PARTICLE & SHOCKWAVE OVERLAY
// ============================================================================
private data class StudioParticle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float
)

@Composable
fun StudioFullScreenCelebrationOverlay(
    title: String,
    subtitle: String,
    xpEarned: Int,
    level: Int,
    rank: String,
    onDismiss: () -> Unit
) {
    val particles = remember {
        List(110) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val speed = Random.nextFloat() * 980f + 240f
            StudioParticle(
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 340f).toFloat(),
                color = AppColors.StudioPalette.random(),
                size = Random.nextFloat() * 11f + 6f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(2900, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = anim.value
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f

            drawCircle(
                color = AppColors.MintGlow.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.8f * t,
                center = Offset(cx, cy),
                style = Stroke(width = 6.dp.toPx())
            )

            particles.forEach { p ->
                val gravity = 1200f * t * t
                val x = cx + p.vx * t
                val y = cy + p.vy * t + 0.5f * gravity
                val a = (1f - t * 0.85f).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = a),
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        }

        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(2.dp, AppColors.MintGlow),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+$xpEarned XP  •  Lv.$level $rank",
                        color = AppColors.MintGlow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue Momentum", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// CREATION DIALOGS (Habit, Todo, Journal, Notebook, Custom Measurement)
// ============================================================================
@Composable
fun StudioCreateHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, TimeOfDay, String, HabitType, Long, Float, String, Float) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⚡") }
    var category by remember { mutableStateOf("Growth") }
    var timeOfDay by remember { mutableStateOf(TimeOfDay.MORNING) }
    var cue by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HabitType.CHECKLIST) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("times") }
    var step by remember { mutableStateOf("1") }
    var selectedColor by remember { mutableLongStateOf(0xFF34D399L) }

    val colorChoices = listOf(0xFF34D399L, 0xFFA855F7L, 0xFF06B6D4L, 0xFFF97316L, 0xFFF59E0BL, 0xFFF43F5EL)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("Design New Habit", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Icon") }, modifier = Modifier.width(70.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Habit Name") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = cue,
                    onValueChange = { cue = it },
                    label = { Text("Habit Stacking Cue (e.g. After morning coffee)") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HabitType.entries) { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label, fontSize = 11.sp) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TimeOfDay.entries) { tod ->
                        FilterChip(selected = timeOfDay == tod, onClick = { timeOfDay = tod }, label = { Text("${tod.icon} ${tod.label}", fontSize = 11.sp) })
                    }
                }
                if (type == HabitType.MEASUREMENT || type == HabitType.FOCUS_TIMER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = { Text("Daily Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (mins/L)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorChoices.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedColor == hex) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            emoji,
                            category,
                            timeOfDay,
                            cue,
                            type,
                            selectedColor,
                            target.toFloatOrNull() ?: 1f,
                            unit,
                            step.toFloatOrNull() ?: 1f
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Create Habit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudioAddTodoDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, TodoPriority, String, Int, List<String>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") }
    var priority by remember { mutableStateOf(TodoPriority.P1_CRITICAL) }
    var mins by remember { mutableStateOf("25") }
    var subtasksRaw by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("Add Priority Mission", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mission Title") })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TodoPriority.entries) { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label, fontSize = 11.sp) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = mins,
                        onValueChange = { mins = it },
                        label = { Text("Focus Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(105.dp)
                    )
                }
                OutlinedTextField(
                    value = subtasksRaw,
                    onValueChange = { subtasksRaw = it },
                    label = { Text("Subtasks / Checklist (one per line)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            category,
                            priority,
                            "Today",
                            mins.toIntOrNull() ?: 25,
                            subtasksRaw.lines(),
                            recurring
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Add Mission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudioCreateJournalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, String, List<String>, List<String>, List<String>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("🔥") }
    var energy by remember { mutableFloatStateOf(8f) }
    var wins by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var mistakes by remember { mutableStateOf("") }
    var actionsRaw by remember { mutableStateOf("") }
    var pushToTodo by remember { mutableStateOf(true) }
    val photos = remember { mutableStateListOf<String>() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        uris.forEach { photos.add(it.toString()) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Log Daily Reflection & Memories", color = AppColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🔥", "🌟", "😊", "🧘", "💪", "🌧️").forEach { e ->
                        FilterChip(selected = mood == e, onClick = { mood = e }, label = { Text(e, fontSize = 16.sp) })
                    }
                }

                Text("Energy Level: ${energy.roundToInt()}/10", color = AppColors.MintGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(value = energy, onValueChange = { energy = it }, valueRange = 1f..10f, steps = 8)

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Headline of the Day") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wins, onValueChange = { wins = it }, label = { Text("Key Wins & Gratitude") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = story, onValueChange = { story = it }, label = { Text("Daily Activities & Story") }, minLines = 3, modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.ElectricViolet)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = AppColors.ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Memory Photos (${photos.size} attached)", color = AppColors.ElectricViolet)
                }

                OutlinedTextField(value = mistakes, onValueChange = { mistakes = it }, label = { Text("What went wrong / Mistakes made?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = actionsRaw, onValueChange = { actionsRaw = it }, label = { Text("Actions I must take tomorrow (1 per line)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pushToTodo, onCheckedChange = { pushToTodo = it })
                    Text("Auto-convert actions into P1 Critical To-Do missions", color = AppColors.TextSecondary, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                title,
                                mood,
                                energy.roundToInt(),
                                wins,
                                story,
                                mistakes,
                                actionsRaw.lines().filter { it.isNotBlank() },
                                photos.toList(),
                                listOf("DailyAudit"),
                                pushToTodo
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
                    ) {
                        Text("Save Entry", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudioCreateNoteDialog(
    onDismiss: () -> Unit,
    onSave: (NotebookEntry) -> Unit
) {
    var category by remember { mutableStateOf(NoteCategory.BOOK_SUMMARY) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf("100") }
    var quote by remember { mutableStateOf("") }
    var takeaways by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var flashQ by remember { mutableStateOf("") }
    var flashA by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("Add to Second-Brain Vault", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NoteCategory.entries) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Book / Note Title") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author / Topic") }, modifier = Modifier.weight(1f))
                    if (category == NoteCategory.BOOK_SUMMARY) {
                        OutlinedTextField(
                            value = progress,
                            onValueChange = { progress = it },
                            label = { Text("% Read") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(85.dp)
                        )
                    }
                }
                OutlinedTextField(value = quote, onValueChange = { quote = it }, label = { Text("Favorite Quote") })
                OutlinedTextField(value = takeaways, onValueChange = { takeaways = it }, label = { Text("Key Takeaways (1 per line)") }, minLines = 2)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Detailed Summary / Notes") }, minLines = 3)
                OutlinedTextField(value = flashQ, onValueChange = { flashQ = it }, label = { Text("Active Recall Question (Optional)") })
                OutlinedTextField(value = flashA, onValueChange = { flashA = it }, label = { Text("Active Recall Answer") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val fc = if (flashQ.isNotBlank() && flashA.isNotBlank()) listOf(FlashcardItem(flashQ, flashA)) else emptyList()
                        onSave(
                            NotebookEntry(
                                category = category,
                                title = title,
                                authorOrSubtitle = author,
                                readingProgress = (progress.toIntOrNull() ?: 100).coerceIn(0, 100),
                                favoriteQuote = quote,
                                keyTakeaways = takeaways.lines().filter { it.isNotBlank() },
                                detailedNotes = notes,
                                flashcards = fc
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ElectricViolet)
            ) {
                Text("Save to Vault", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CustomMeasurementDialog(
    habit: Habit,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var valueText by remember { mutableStateOf(habit.valueOn(selectedDate).cleanStr()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("${habit.emoji} Log ${habit.title}", color = AppColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Amount (${habit.unit}) • Goal: ${habit.targetValue.cleanStr()}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(valueText.toFloatOrNull() ?: 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Float.cleanStr(): String =
    if (this % 1f == 0f) this.toInt().toString() else String.format(Locale.US, "%.1f", this)
