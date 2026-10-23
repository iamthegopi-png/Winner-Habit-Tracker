package com.vibrant.habittracker

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
                    primary = AppColors.NeonGreen,
                    secondary = AppColors.LavenderPurple,
                    onBackground = AppColors.TextPrimary,
                    onSurface = AppColors.TextPrimary
                )
            ) {
                PulseHabitApp()
            }
        }
    }
}

// ============================================================================
// MAIN SCAFFOLD & NAVIGATION
// ============================================================================
@Composable
fun PulseHabitApp(vm: HabitTrackerViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    var showAnalyticsModal by remember { mutableStateOf(false) }
    var showCloudBackupModal by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BgDeep)
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = AppColors.BgDeep,
            topBar = {
                TopHeaderBar(
                    vm = vm,
                    onOpenAnalytics = { showAnalyticsModal = true },
                    onOpenCloud = { showCloudBackupModal = true }
                )
            },
            bottomBar = {
                VibrantBottomBar(
                    currentTab = currentTab,
                    onSelectTab = { currentTab = it },
                    onQuickAddHabit = { showAddHabitDialog = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 18 } togetherWith
                                fadeOut(tween(180))
                    },
                    label = "MainTabs"
                ) { tab ->
                    when (tab) {
                        0 -> HabitTrackerScreen(
                            vm = vm,
                            onAddHabitClick = { showAddHabitDialog = true },
                            onOpenAnalytics = { showAnalyticsModal = true }
                        )
                        1 -> TodoTimelineScreen(vm = vm)
                        2 -> ReflectionJournalScreen(vm = vm)
                        3 -> AdvancedNotebookScreen(vm = vm)
                    }
                }
            }
        }

        // Analytics Modal (Matches Image 1)
        if (showAnalyticsModal) {
            AnalyticsFullModal(
                vm = vm,
                onDismiss = { showAnalyticsModal = false }
            )
        }

        // Google Cloud Backup Modal
        if (showCloudBackupModal) {
            GoogleCloudBackupDialog(
                vm = vm,
                onDismiss = { showCloudBackupModal = false }
            )
        }

        // Create Habit Modal
        if (showAddHabitDialog) {
            CreateHabitDialog(
                onDismiss = { showAddHabitDialog = false },
                onSave = { title, emoji, cat, type, color, target, unit, step ->
                    vm.addNewHabit(title, emoji, cat, type, color, target, unit, step)
                    showAddHabitDialog = false
                }
            )
        }

        // FULL-SCREEN CELEBRATION PARTICLE OVERLAY
        AnimatedVisibility(
            visible = vm.showCelebration.value,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(450))
        ) {
            FullScreenCelebrationOverlay(
                title = vm.celebrationTitle.value,
                subtitle = vm.celebrationSubtitle.value,
                onDismiss = { vm.showCelebration.value = false }
            )
        }
    }
}

// ============================================================================
// TOP HEADER BAR & FLOATING BOTTOM NAVIGATION
// ============================================================================
@Composable
fun TopHeaderBar(
    vm: HabitTrackerViewModel,
    onOpenAnalytics: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Today, $todayFormatted",
                color = AppColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AppColors.NeonGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (vm.connectedGoogleEmail.value != null)
                        "Synced • ${vm.connectedGoogleEmail.value}"
                    else "Local Mode • Tap cloud to connect",
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Analytics Icon Button (Green Sparkline Icon from Screenshots)
            IconButton(
                onClick = onOpenAnalytics,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.BgCardElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Analytics",
                    tint = AppColors.NeonGreen
                )
            }

            // Google Cloud Sync Button
            IconButton(
                onClick = onOpenCloud,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.BgCardElevated)
            ) {
                Icon(
                    imageVector = if (vm.connectedGoogleEmail.value != null)
                        Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = "Google Cloud Backup",
                    tint = if (vm.connectedGoogleEmail.value != null)
                        AppColors.MintGreen else AppColors.TextSecondary
                )
            }

            // Grid vs Weekly View Toggle
            IconButton(
                onClick = {
                    vm.habitViewMode.value =
                        if (vm.habitViewMode.value == HabitViewMode.GRID_VIEW)
                            HabitViewMode.WEEKLY_VIEW else HabitViewMode.GRID_VIEW
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.BgCardElevated)
            ) {
                Icon(
                    imageVector = if (vm.habitViewMode.value == HabitViewMode.GRID_VIEW)
                        Icons.Default.ViewWeek else Icons.Default.GridOn,
                    contentDescription = "Toggle Grid/Weekly",
                    tint = AppColors.LavenderPurple
                )
            }
        }
    }
}

@Composable
fun VibrantBottomBar(
    currentTab: Int,
    onSelectTab: (Int) -> Unit,
    onQuickAddHabit: () -> Unit
) {
    Surface(
        color = AppColors.BgCard,
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppColors.BorderDark,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, "Habits", Icons.Default.GridView),
                Triple(1, "To-Do", Icons.Default.CheckCircleOutline),
                Triple(2, "Journal", Icons.Default.AutoStories),
                Triple(3, "Notebook", Icons.Default.MenuBook)
            )

            tabs.forEach { (index, label, icon) ->
                val selected = currentTab == index
                val bg by animateColorAsState(
                    if (selected) AppColors.NeonGreen.copy(alpha = 0.16f) else Color.Transparent,
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    if (selected) AppColors.NeonGreen else AppColors.TextSecondary,
                    label = "tabColor"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
                    AnimatedVisibility(visible = selected) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Neon Green Floating Action (+) Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.NeonGreen)
                    .clickable { onQuickAddHabit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============================================================================
// SECTION 1: HABIT TRACKER SCREEN (Grids, Weekly View, Rings, & Measurements)
// ============================================================================
@Composable
fun HabitTrackerScreen(
    vm: HabitTrackerViewModel,
    onAddHabitClick: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val context = LocalContext.current
    val categories = listOf("All", "Fitness", "Health", "Morning", "Social", "Work")
    var habitForCustomMeasurement by remember { mutableStateOf<Habit?>(null) }

    val filteredHabits = remember(vm.habits.toList(), vm.selectedCategory.value) {
        if (vm.selectedCategory.value == "All") vm.habits
        else vm.habits.filter { it.category.equals(vm.selectedCategory.value, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 7-Day Glowing Top Progress Rings (Matches Image 4 & 5)
        item {
            WeeklyDailyCompletionRingsRow(habits = vm.habits)
        }

        // 2. Category Chips + View Mode Pill
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { category ->
                    val isSelected = vm.selectedCategory.value == category
                    Surface(
                        color = if (isSelected) AppColors.NeonGreenGlow.copy(alpha = 0.55f) else AppColors.BgCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AppColors.NeonGreen else AppColors.BorderDark
                        ),
                        modifier = Modifier.clickable { vm.selectedCategory.value = category }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) AppColors.NeonGreen else AppColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 3. Habit Cards (Either 16-Week Contribution Grid or 7-Day Weekly Row)
        items(filteredHabits, key = { it.id }) { habit ->
            HabitCardItem(
                habit = habit,
                viewMode = vm.habitViewMode.value,
                onToggleToday = { vm.toggleOrIncrementHabit(context, habit, LocalDate.now()) },
                onDecrementToday = {
                    val current = habit.valueOn(LocalDate.now())
                    vm.setHabitMeasurement(context, habit, (current - habit.stepSize).coerceAtLeast(0f))
                },
                onToggleDate = { date -> vm.toggleOrIncrementHabit(context, habit, date) },
                onOpenCustomMeasurement = { habitForCustomMeasurement = habit },
                onOpenAnalytics = onOpenAnalytics
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    habitForCustomMeasurement?.let { habit ->
        MeasurementInputDialog(
            habit = habit,
            onDismiss = { habitForCustomMeasurement = null },
            onConfirm = { newAmount ->
                vm.setHabitMeasurement(context, habit, newAmount)
                habitForCustomMeasurement = null
            }
        )
    }
}

@Composable
fun WeeklyDailyCompletionRingsRow(habits: List<Habit>) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val totalHabits = habits.size.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isToday = date == today
            val completedCount = habits.count { it.isCompletedOn(date) }
            val progress by animateFloatAsState(
                targetValue = (completedCount.toFloat() / totalHabits.toFloat()).coerceIn(0f, 1f),
                animationSpec = tween(600),
                label = "ringProg"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    color = if (isToday) AppColors.NeonGreen else AppColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 3.5.dp.toPx()
                        // Dark Track Rounded Rect
                        drawRoundRect(
                            color = AppColors.BorderDark,
                            cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                            style = Stroke(width = strokeWidth)
                        )
                        // Neon Progress Arc/Border
                        drawArc(
                            color = AppColors.NeonGreen,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$completedCount/${habits.size}",
                        color = AppColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun HabitCardItem(
    habit: Habit,
    viewMode: HabitViewMode,
    onToggleToday: () -> Unit,
    onDecrementToday: () -> Unit,
    onToggleDate: (LocalDate) -> Unit,
    onOpenCustomMeasurement: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val today = LocalDate.now()
    val isDoneToday = habit.isCompletedOn(today)
    val todayValue = habit.valueOn(today)
    val streak = habit.currentStreak(today)

    val buttonScale by animateFloatAsState(
        targetValue = if (isDoneToday) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppColors.BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(habit.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = habit.emoji, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = habit.title,
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥 Streak: $streak",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            if (habit.type == HabitType.MEASUREMENT) {
                                Text(
                                    text = "  •  ${todayValue.formatClean()}/${habit.targetValue.formatClean()} ${habit.unit}",
                                    color = habit.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Right Action Controls (Checklist Checkmark, Measurement -/+, or Quit Shield)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (habit.type == HabitType.MEASUREMENT) {
                        // Minus Step Button
                        IconButton(
                            onClick = onDecrementToday,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .border(1.dp, AppColors.BorderDark, RoundedCornerShape(11.dp))
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Custom number input button
                        IconButton(
                            onClick = onOpenCustomMeasurement,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .border(1.dp, AppColors.BorderDark, RoundedCornerShape(11.dp))
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Set exact value",
                                tint = habit.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Primary Action Button
                    Box(
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDoneToday) habit.accentColor
                                else habit.accentColor.copy(alpha = 0.18f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = habit.accentColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleToday() },
                        contentAlignment = Alignment.Center
                    ) {
                        when (habit.type) {
                            HabitType.CHECKLIST -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = if (isDoneToday) Color.Black else habit.accentColor
                            )
                            HabitType.MEASUREMENT -> Icon(
                                imageVector = if (isDoneToday) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Add measurement",
                                tint = if (isDoneToday) Color.Black else habit.accentColor
                            )
                            HabitType.QUIT_SHIELD -> Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Active",
                                tint = if (isDoneToday) Color.Black else habit.accentColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenAnalytics,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Details",
                            tint = AppColors.TextSecondary
                        )
                    }
                }
            }

            // Optional Measurement Progress Bar
            if (habit.type == HabitType.MEASUREMENT) {
                Spacer(Modifier.height(10.dp))
                val ratio = (todayValue / habit.targetValue).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { ratio },
                    color = habit.accentColor,
                    trackColor = AppColors.BgCardElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(Modifier.height(14.dp))

            // Switch between 16-Week Heatmap Grid (Image 5) and 7-Day Weekly Row (Image 4)
            if (viewMode == HabitViewMode.GRID_VIEW) {
                ContributionHeatmapGrid(habit = habit)
            } else {
                WeeklyInteractiveHabitRow(habit = habit, onToggleDate = onToggleDate)
            }
        }
    }
}

@Composable
fun ContributionHeatmapGrid(habit: Habit) {
    val today = LocalDate.now()
    val weeks = 16
    val totalDays = weeks * 7

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Day labels column (M, W, F, S)
        Column(
            modifier = Modifier.height(114.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "", "W", "", "F", "", "S").forEach { label ->
                Text(
                    text = label,
                    color = AppColors.TextMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.height(13.dp)
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // 16 Columns x 7 Rows Grid
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (w in (weeks - 1) downTo 0) {
                Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    for (d in 6 downTo 0) {
                        val daysAgo = w * 7 + d
                        val cellDate = today.minusDays(daysAgo.toLong())
                        val rawVal = habit.valueOn(cellDate)
                        val isDone = habit.isCompletedOn(cellDate)
                        val isFailedShield = habit.type == HabitType.QUIT_SHIELD && rawVal < 0f

                        val cellColor = when {
                            isFailedShield -> AppColors.RoseRed.copy(alpha = 0.25f)
                            isDone -> habit.accentColor
                            rawVal > 0f -> habit.accentColor.copy(alpha = 0.45f)
                            else -> AppColors.BgCardElevated
                        }

                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(cellColor)
                                .then(
                                    if (cellDate == today) Modifier.border(
                                        1.dp,
                                         Color.White,
                                        RoundedCornerShape(3.5.dp)
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFailedShield) {
                                Text("×", color = AppColors.RoseRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyInteractiveHabitRow(
    habit: Habit,
    onToggleDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isDone = habit.isCompletedOn(date)
            val rawVal = habit.valueOn(date)
            val isToday = date == today

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                    color = if (isToday) AppColors.TextPrimary else AppColors.TextMuted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (isDone) habit.accentColor
                            else AppColors.BgCardElevated
                        )
                        .border(
                            width = if (isToday) 1.5.dp else 1.dp,
                            color = if (isToday) Color.White else AppColors.BorderDark,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable { onToggleDate(date) },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        habit.type == HabitType.QUIT_SHIELD && rawVal < 0f ->
                            Icon(Icons.Default.Close, null, tint = AppColors.RoseRed, modifier = Modifier.size(16.dp))
                        habit.type == HabitType.QUIT_SHIELD && isDone ->
                            Icon(Icons.Default.Shield, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        habit.type == HabitType.MEASUREMENT && rawVal > 0f ->
                            Text(
                                text = rawVal.formatClean(),
                                color = if (isDone) Color.Black else habit.accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        isDone ->
                            Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// ANALYTICS MODAL SHEET (Matches Reference Image 1: Streaks, Insights, & Time Trend)
// ============================================================================
@Composable
fun AnalyticsFullModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    var selectedHabitIndex by remember { mutableIntStateOf(0) }
    var selectedRangeDays by remember { mutableIntStateOf(30) } // 7, 30, 90

    val habitsList = vm.habits
    if (habitsList.isEmpty()) return
    val currentHabit = habitsList[selectedHabitIndex.coerceIn(0, habitsList.lastIndex)]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, AppColors.BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(28.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, null, tint = AppColors.NeonGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Analytics",
                            color = AppColors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Habit Carousel Selector (< 💻 Side Project >)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedHabitIndex =
                                if (selectedHabitIndex - 1 < 0) habitsList.lastIndex else selectedHabitIndex - 1
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = AppColors.TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentHabit.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentHabit.title,
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            selectedHabitIndex = (selectedHabitIndex + 1) % habitsList.size
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextPrimary)
                    }
                }

                // Pager Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedHabitIndex + 1} of ${habitsList.size}   ",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp
                    )
                    habitsList.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (idx == selectedHabitIndex) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == selectedHabitIndex) AppColors.NeonGreen else AppColors.BorderDark
                                )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 3 Stat Cards Row (Current Streak, Best Streak, Total Logged)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🔥",
                        value = "${currentHabit.currentStreak()}",
                        label = "current streak",
                        footer = "Personal best!",
                        accent = AppColors.CoralOrange
                    )
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🏆",
                        value = "${currentHabit.bestStreak()}",
                        label = "best streak",
                        footer = "All-time high",
                        accent = AppColors.AmberGold
                    )
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = "⏱️",
                        value = currentHabit.totalLoggedValue().formatClean(),
                        label = "total ${currentHabit.unit}",
                        footer = "Incredible!",
                        accent = AppColors.NeonGreen
                    )
                }

                Spacer(Modifier.height(18.dp))

                // INSIGHTS Card (Matches Image 1)
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppColors. NeonGreenGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "INSIGHTS",
                                color = AppColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        HorizontalDivider(color = AppColors.BorderDark)
                        InsightRowItem(
                            emoji = "🔥",
                            bg = AppColors.CoralOrange.copy(alpha = 0.16f),
                            title = "On fire! Personal best streak",
                            subtitle = "You're at a ${currentHabit.currentStreak()}-day streak. Keep it going!"
                        )
                        InsightRowItem(
                            emoji = "📅",
                            bg = AppColors.NeonGreen.copy(alpha = 0.16f),
                            title = "Thursdays are your power day",
                            subtitle = "88% completion rate on Thursdays."
                        )
                        InsightRowItem(
                            emoji = "👍",
                            bg = AppColors.PeriwinkleBlue.copy(alpha = 0.16f),
                            title = "Strong consistency",
                            subtitle = "76% 30-day completion rate. Keep pushing!"
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // TIME TREND Line Graph Card (Matches Image 1)
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppColors.BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ShowChart,
                                        null,
                                        tint = AppColors.NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "TIME TREND",
                                        color = AppColors.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "${currentHabit.unit.replaceFirstChar { it.uppercase() }} logged per day over time",
                                    color = AppColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppColors.BgCardElevated)
                                    .padding(2.dp)
                            ) {
                                listOf(7 to "7d", 30 to "30d", 90 to "90d").forEach { (days, label) ->
                                    val selected = selectedRangeDays == days
                                    Text(
                                        text = label,
                                        color = if (selected) AppColors.NeonGreen else AppColors.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selected) AppColors.NeonGreenGlow.copy(alpha = 0.6f)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedRangeDays = days }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        val today = LocalDate.now()
                        val points = (selectedRangeDays - 1 downTo 0).map { offset ->
                            currentHabit.valueOn(today.minusDays(offset.toLong())).coerceAtLeast(0f)
                        }

                        NeonAreaLineChart(
                            points = points,
                            lineColor = AppColors.NeonGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    footer: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        color = AppColors.BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = label, color = AppColors.TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(text = footer, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InsightRowItem(emoji: String, bg: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = AppColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun NeonAreaLineChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.2f

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        // Subtle horizontal grid lines
        for (i in 0..4) {
            val y = h * (i / 4f)
            drawLine(
                color = AppColors.BorderDark.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal) * h
            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = h - (points[i - 1] / maxVal) * h
                val ctrlX = (prevX + x) / 2f
                strokePath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                fillPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
            }
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ============================================================================
// SECTION 2: TIMELINE TO-DO LIST SCREEN (Matches Reference Image 2)
// ============================================================================
@Composable
fun TodoTimelineScreen(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Timeline, 1 = All Tasks
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val todayStr = LocalDate.now().toString()
    val yesterdayStr = LocalDate.now().minusDays(1).toString()

    Column(modifier = Modifier.fillMaxSize()) {
        // Timeline | All Tasks Top Sub-Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Timeline", "All Tasks").forEachIndexed { idx, title ->
                val active = selectedSubTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSubTab = idx }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = if (active) AppColors.NeonGreen else AppColors.TextSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(72.dp)
                            .background(if (active) AppColors.NeonGreen else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(color = AppColors.BorderDark)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val groupedByDate = if (selectedSubTab == 0) {
                listOf(
                    "TODAY" to vm.todos.filter { it.date == todayStr },
                    "YESTERDAY" to vm.todos.filter { it.date == yesterdayStr }
                )
            } else {
                listOf("ALL TASKS" to vm.todos.toList())
            }

            items(groupedByDate) { (dayLabel, dayTasks) ->
                TimelineDaySection(
                    dayLabel = dayLabel,
                    tasks = dayTasks,
                    onToggleTask = { vm.toggleTodo(context, it) },
                    onAddTaskClick = { showAddTaskDialog = true }
                )
            }
        }
    }

    if (showAddTaskDialog) {
        AddTodoTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, cat, recurring ->
                vm.addTodoTask(title = title, category = cat, isRecurring = recurring)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TimelineDaySection(
    dayLabel: String,
    tasks: List<TodoTask>,
    onToggleTask: (TodoTask) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val completedCount = tasks.count { it.isCompleted }
    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val isToday = dayLabel == "TODAY"

    Row(modifier = Modifier.fillMaxWidth()) {
        // Left Timeline Node & Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 14.dp, top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isToday) AppColors.NeonGreen else Color.Transparent)
                    .border(2.dp, AppColors.NeonGreen, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(220.dp)
                    .background(AppColors.BorderDark)
            )
        }

        // Right Content Column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isToday) LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
                        else LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMM d")),
                        color = if (isToday) AppColors.NeonGreen else AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isToday) {
                        Surface(
                            color = AppColors.NeonGreen,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "$completedCount/${tasks.size}",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            if (pendingTasks.isEmpty() && tasks.isNotEmpty()) {
                // "All done!" green banner from Image 2
                Surface(
                    color = AppColors.NeonGreenGlow.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.NeonGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = AppColors.NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "All done! ",
                            color = AppColors.NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${tasks.size} tasks completed",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Group Pending by Category (Personal, Work, Health)
                val byCategory = pendingTasks.groupBy { it.category }
                byCategory.forEach { (category, catTasks) ->
                    Text(
                        text = "⌄  $category",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    catTasks.forEach { task ->
                        TodoTaskRow(task = task, onToggle = { onToggleTask(task) })
                    }
                }
            }

            if (completedTasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "›  Completed  ${completedTasks.size}",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
                completedTasks.forEach { task ->
                    TodoTaskRow(task = task, onToggle = { onToggleTask(task) })
                }
            }

            Spacer(Modifier.height(10.dp))

            // + Add task button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAddTaskClick() }
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(1.dp, AppColors.BorderDark, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Add task", color = AppColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TodoTaskRow(task: TodoTask, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) AppColors.NeonGreen else Color.Transparent)
                    .border(
                        width = 1.8.dp,
                        color = if (task.isCompleted) AppColors.NeonGreen else AppColors.TextSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.title,
                color = if (task.isCompleted) AppColors.TextMuted else AppColors.TextPrimary,
                fontSize = 15.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        }

        if (task.isRecurring) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Recurring",
                tint = AppColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================================================
// SECTION 3: DAILY REFLECTION JOURNAL (Activities, Memory Photos, Mistakes & Actions)
// ============================================================================
@Composable
fun ReflectionJournalScreen(vm: HabitTrackerViewModel) {
    var showCreateJournalModal by remember { mutableStateOf(false) }

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
                    text = "Reflection & Memory Journal",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Daily Wins • Memory Photos • Mistakes • Corrective Actions",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showCreateJournalModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.EditNote, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Write", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(vm.journals, key = { it.id }) { entry ->
                JournalEntryCard(entry = entry)
            }
        }
    }

    if (showCreateJournalModal) {
        CreateJournalDialog(
            onDismiss = { showCreateJournalModal = false },
            onSave = { mood, activities, wrong, actions, photos, syncToTodo ->
                vm.addJournalEntry(mood, activities, wrong, actions, photos, syncToTodo)
                showCreateJournalModal = false
            }
        )
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry) {
    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppColors.BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.moodEmoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = entry.date,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Surface(
                    color = AppColors.BgCardElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Daily Audit",
                        color = AppColors.MintGreen,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Daily Activities
            Column {
                Text(
                    text = "📖 DAILY ACTIVITIES & MEMORIES",
                    color = AppColors.LavenderPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(text = entry.dailyActivities, color = AppColors.TextPrimary, fontSize = 14.sp)
            }

            // Uploaded Photos Gallery
            if (entry.photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entry.photoUris) { uriStr ->
                        AsyncImage(
                            model = Uri.parse(uriStr),
                            contentDescription = "Memory Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, AppColors.BorderDark, RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            // 2. What I Did Wrong (Self-Correction Block)
            if (entry.whatWentWrong.isNotBlank()) {
                Surface(
                    color = AppColors.RoseRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppColors.RoseRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ WHAT I DID WRONG / FRICTION POINTS",
                            color = AppColors.RoseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(text = entry.whatWentWrong, color = AppColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // 3. Actions I Must Take
            if (entry.actionsToTake.isNotEmpty()) {
                Surface(
                    color = AppColors.NeonGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppColors.NeonGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 ACTIONS I MUST TAKE TOMORROW",
                            color = AppColors.NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        entry.actionsToTake.forEach { action ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowRightAlt,
                                    null,
                                    tint = AppColors.NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(text = action, color = AppColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateJournalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>, List<String>, Boolean) -> Unit
) {
    var mood by remember { mutableStateOf("🔥") }
    var activities by remember { mutableStateOf("") }
    var wentWrong by remember { mutableStateOf("") }
    var actionsRaw by remember { mutableStateOf("") }
    var pushActionsToTodo by remember { mutableStateOf(true) }
    val selectedPhotos = remember { mutableStateListOf<String>() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        uris.forEach { selectedPhotos.add(it.toString()) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = AppColors.BgCard,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, AppColors.BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("New Daily Reflection", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                // Mood selector
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("🔥", "🌟", "😊", "🧘", "⚡", "🌧️").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (mood == emoji) AppColors.NeonGreenGlow else AppColors.BgCardElevated)
                                .border(
                                    1.dp,
                                    if (mood == emoji) AppColors.NeonGreen else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { mood = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = activities,
                    onValueChange = { activities = it },
                    label = { Text("What did you do today?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Photo Upload Button
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.LavenderPurple)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = AppColors.LavenderPurple)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Attach Memory Photos (${selectedPhotos.size} selected)",
                        color = AppColors.LavenderPurple
                    )
                }

                OutlinedTextField(
                    value = wentWrong,
                    onValueChange = { wentWrong = it },
                    label = { Text("What went wrong / Mistakes made?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = actionsRaw,
                    onValueChange = { actionsRaw = it },
                    label = { Text("Actions I must take (one per line)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pushActionsToTodo,
                        onCheckedChange = { pushActionsToTodo = it }
                    )
                    Text(
                        "Automatically add these actions to my To-Do List",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (activities.isNotBlank()) {
                                val actions = actionsRaw.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                onSave(mood, activities, wentWrong, actions, selectedPhotos.toList(), pushActionsToTodo)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen)
                    ) {
                        Text("Save Reflection", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION 4: ADVANCED BOOK SUMMARY & NOTES NOTEBOOK
// ============================================================================
@Composable
fun AdvancedNotebookScreen(vm: HabitTrackerViewModel) {
    var selectedFilter by remember { mutableStateOf<NoteCategory?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    val displayedNotes = remember(vm.notes.toList(), selectedFilter) {
        if (selectedFilter == null) vm.notes
        else vm.notes.filter { it.category == selectedFilter }
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
                    text = "Knowledge Vault & Book Notes",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Book Summaries • Key Takeaways • Second Brain Notes",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { showCreateNoteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.LavenderPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("New Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Filter Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            listOf(
                null to "All Vault (${vm.notes.size})",
                NoteCategory.BOOK_SUMMARY to "📚 Book Summaries",
                NoteCategory.GENERAL_NOTE to "📝 Ideas & Notes"
            ).forEach { (cat, label) ->
                val active = selectedFilter == cat
                FilterChip(
                    selected = active,
                    onClick = { selectedFilter = cat },
                    label = { Text(label) }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(displayedNotes, key = { it.id }) { note ->
                NotebookCard(note = note)
            }
        }
    }

    if (showCreateNoteDialog) {
        CreateNotebookDialog(
            onDismiss = { showCreateNoteDialog = false },
            onSave = { newEntry ->
                vm.addNotebookEntry(newEntry)
                showCreateNoteDialog = false
            }
        )
    }
}

@Composable
fun NotebookCard(note: NotebookEntry) {
    val accent = Color(note.coverColorHex)
    var expanded by remember { mutableStateOf(true) }

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Book Spine / Accent Pillar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (note.category == NoteCategory.BOOK_SUMMARY) "BOOK SUMMARY" else "NOTE",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "• ${note.updatedAt}", color = AppColors.TextMuted, fontSize = 10.sp)
                    }
                    Text(
                        text = note.title,
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = note.subtitleOrAuthor,
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                if (note.category == NoteCategory.BOOK_SUMMARY) {
                    Row {
                        repeat(note.rating) {
                            Text("★", color = AppColors.AmberGold, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (note.favoriteQuote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = AppColors.BgCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${note.favoriteQuote}”",
                        color = AppColors.MintGreen,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (note.keyTakeaways.isNotEmpty()) {
                        Text(
                            text = "KEY TAKEAWAYS",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        note.keyTakeaways.forEach { bullet ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = accent, fontWeight = FontWeight.Bold)
                                Text(bullet, color = AppColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Text(
                        text = note.content,
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// FULL-SCREEN CELEBRATION EFFECTS OVERLAY (Confetti, Shockwave, & Trophy Banner)
// ============================================================================
private data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val radius: Float
)

@Composable
fun FullScreenCelebrationOverlay(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    val particles = remember {
        List(95) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val speed = Random.nextFloat() * 900f + 220f
            ConfettiParticle(
                startX = 0.5f,
                startY = 0.42f,
                velocityX = (cos(angle) * speed).toFloat(),
                velocityY = (sin(angle) * speed - 300f).toFloat(),
                color = AppColors.AccentPresets.random(),
                radius = Random.nextFloat() * 10f + 6f
            )
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = animProgress.value
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f

            // Expanding Neon Shockwave Ring
            drawCircle(
                color = AppColors.NeonGreen.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.75f * t,
                center = Offset(cx, cy),
                style = Stroke(width = 6.dp.toPx())
            )

            // 95 Physics-driven Confetti Particles
            particles.forEach { p ->
                val gravity = 1150f * t * t
                val x = cx + p.velocityX * t
                val y = cy + p.velocityY * t + 0.5f * gravity
                val alpha = (1f - t * 0.85f).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(x, y)
                )
            }
        }

        // Celebration Card
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, AppColors.NeonGreen),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 54.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = AppColors.NeonGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = AppColors.TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Keep Crushing It!", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// GOOGLE ACCOUNT & CLOUD BACKUP DIALOG + CREATION DIALOGS
// ============================================================================
@Composable
fun GoogleCloudBackupDialog(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf(vm.connectedGoogleEmail.value ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, null, tint = AppColors.NeonGreen)
                Spacer(Modifier.width(10.dp))
                Text("Google Drive Backup", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Connect your Google Account to keep encrypted snapshots of your habits, streaks, journal memories, and book notes.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Backup Daily", color = AppColors.TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = vm.autoCloudSync.value,
                        onCheckedChange = { vm.autoCloudSync.value = it }
                    )
                }
                Text(
                    text = "Last Cloud Snapshot: ${vm.lastCloudBackup.value}",
                    color = AppColors.MintGreen,
                    fontSize = 12.sp
                )
                if (vm.isSyncingCloud.value) {
                    LinearProgressIndicator(
                        color = AppColors.NeonGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.connectedGoogleEmail.value = emailInput.ifBlank { null }
                    vm.syncToGoogleCloud(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen)
            ) {
                Text("Backup Now", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, HabitType, Long, Float, String, Float) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⚡") }
    var category by remember { mutableStateOf("Fitness") }
    var type by remember { mutableStateOf(HabitType.CHECKLIST) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("times") }
    var step by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("Create New Habit", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Icon") },
                        modifier = Modifier.width(72.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Habit Name") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitType.entries.forEach { hType ->
                        FilterChip(
                            selected = type == hType,
                            onClick = { type = hType },
                            label = { Text(hType.name.take(9), fontSize = 11.sp) }
                        )
                    }
                }
                if (type == HabitType.MEASUREMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = { Text("Target") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (hrs/ml)") },
                            modifier = Modifier.weight(1f)
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
                            type,
                            0xFF4ADE80,
                            target.toFloatOrNull() ?: 1f,
                            unit,
                            step.toFloatOrNull() ?: 1f
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MeasurementInputDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var valueText by remember { mutableStateOf(habit.valueOn(LocalDate.now()).formatClean()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("${habit.emoji} Log ${habit.title}", color = AppColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Amount (${habit.unit}) • Goal: ${habit.targetValue.formatClean()}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(valueText.toFloatOrNull() ?: 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTodoTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var recurring by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("New To-Do Task", color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Personal", "Work", "Health").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                    Text("Recurring daily task", color = AppColors.TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, category, recurring) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.NeonGreen)
            ) {
                Text("Add Task", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    onSave: (NotebookEntry) -> Unit
) {
    var isBook by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var takeaways by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("Add to Notebook Vault", color = AppColors.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isBook, onClick = { isBook = true }, label = { Text("Book Summary") })
                    FilterChip(selected = !isBook, onClick = { isBook = false }, label = { Text("General Note") })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text(if (isBook) "Author" else "Topic") })
                OutlinedTextField(value = quote, onValueChange = { quote = it }, label = { Text("Favorite Quote") })
                OutlinedTextField(value = takeaways, onValueChange = { takeaways = it }, label = { Text("Key Takeaways (1 per line)") }, minLines = 2)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Detailed Summary / Notes") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            NotebookEntry(
                                category = if (isBook) NoteCategory.BOOK_SUMMARY else NoteCategory.GENERAL_NOTE,
                                title = title,
                                subtitleOrAuthor = author,
                                favoriteQuote = quote,
                                keyTakeaways = takeaways.lines().filter { it.isNotBlank() },
                                content = content
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.LavenderPurple)
            ) {
                Text("Save Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Float.formatClean(): String =
    if (this % 1f == 0f) this.toInt().toString() else String.format(Locale.US, "%.1f", this)
