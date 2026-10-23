package com.vibrant.habittracker

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
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
                ZenithStudioApp()
            }
        }
    }
}

// ============================================================================
// MAIN STUDIO SCAFFOLD, GAMIFICATION HEADER & NAVIGATION
// ============================================================================
@Composable
fun ZenithStudioApp(vm: HabitTrackerViewModel = viewModel()) {
    val context = LocalContext.current

    // Request Notification Permission on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        vm.initializeOrRefreshFromDisk(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showCloudVaultModal by remember { mutableStateOf(false) }
    var showFocusTimerModal by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B14), AppColors.BgDeep, Color(0xFF04060A))
                )
            )
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ZenithStudioHeader(
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
                            0 -> {
                                editingHabit = null
                                showHabitDialog = true
                            }
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
                AnimatedVisibility(visible = vm.isFocusTimerRunning.value) {
                    ActiveFocusTimerBanner(
                        vm = vm,
                        onExpand = { showFocusTimerModal = true }
                    )
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 18 } togetherWith
                                fadeOut(tween(150))
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> StudioHabitTrackerSection(
                            vm = vm,
                            onNewHabit = {
                                editingHabit = null
                                showHabitDialog = true
                            },
                            onEditHabit = { habit ->
                                editingHabit = habit
                                showHabitDialog = true
                            },
                            onLaunchFocusTimer = { habit ->
                                vm.startFocusTimer(
                                    context = context,
                                    title = "${habit.emoji} ${habit.title}",
                                    minutes = if (habit.type == HabitType.FOCUS_TIMER) habit.stepSize.toInt().coerceAtLeast(15) else 25,
                                    habitId = habit.id
                                )
                                showFocusTimerModal = true
                            }
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

        if (showFocusTimerModal) {
            FocusTimerStudioDialog(vm = vm, onDismiss = { showFocusTimerModal = false })
        }

        if (showAnalyticsSheet) {
            StudioAnalyticsModal(vm = vm, onDismiss = { showAnalyticsSheet = false })
        }

        if (showCloudVaultModal) {
            GoogleDriveBackupVaultDialog(vm = vm, onDismiss = { showCloudVaultModal = false })
        }

        if (showHabitDialog) {
            StudioCreateOrEditHabitDialog(
                habitToEdit = editingHabit,
                onDismiss = {
                    showHabitDialog = false
                    editingHabit = null
                },
                onSave = { id, title, emoji, cat, tod, cue, type, col, target, unit, step, reminder ->
                    vm.saveOrUpdateHabit(context, id, title, emoji, cat, tod, cue, type, col, target, unit, step, reminder)
                    showHabitDialog = false
                    editingHabit = null
                }
            )
        }

        AnimatedVisibility(
            visible = vm.showCelebration.value,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(400))
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

// ============================================================================
// CUSTOM VECTOR BRAND ICON & GAMIFIED HEADER
// ============================================================================
@Composable
fun ZenithBrandLogoIcon(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF062C24), Color(0xFF1E1035))
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(AppColors.MintGlow, AppColors.CyberCyan, AppColors.ElectricViolet)),
                RoundedCornerShape(15.dp)
            )
    ) {
        val w = size.width
        val h = size.height
        // Glowing outer halo
        drawCircle(
            brush = Brush.sweepGradient(listOf(AppColors.MintGlow, AppColors.CyberCyan, AppColors.ElectricViolet, AppColors.MintGlow)),
            radius = w * 0.34f,
            style = Stroke(width = 2.5.dp.toPx())
        )
        // Zenith Crystal Diamond Path
        val crystalPath = Path().apply {
            moveTo(w * 0.5f, h * 0.18f)
            lineTo(w * 0.76f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.82f)
            lineTo(w * 0.24f, h * 0.48f)
            close()
        }
        drawPath(
            path = crystalPath,
            color = AppColors.MintGlow.copy(alpha = 0.25f)
        )
        // Precision Checkmark Core
        val checkPath = Path().apply {
            moveTo(w * 0.36f, h * 0.52f)
            lineTo(w * 0.47f, h * 0.63f)
            lineTo(w * 0.66f, h * 0.40f)
        }
        drawPath(
            path = checkPath,
            color = AppColors.MintGlow,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun ZenithStudioHeader(
    vm: HabitTrackerViewModel,
    onOpenTimer: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val xpProg by animateFloatAsState(
        targetValue = vm.xpProgressInLevel,
        animationSpec = tween(550),
        label = "xpAnim"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ZenithBrandLogoIcon()

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Lv.${vm.userLevel} ${vm.userRankTitle}",
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
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
                        progress = { xpProg },
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderActionButton(Icons.Default.Timer, AppColors.CyberCyan, onOpenTimer)
            HeaderActionButton(Icons.Default.Insights, AppColors.MintGlow, onOpenAnalytics)
            HeaderActionButton(
                if (vm.connectedGoogleEmail.value != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                if (vm.connectedGoogleEmail.value != null) AppColors.MintGlow else AppColors.ElectricViolet,
                onOpenCloud
            )
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
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ActiveFocusTimerBanner(vm: HabitTrackerViewModel, onExpand: () -> Unit) {
    val context = LocalContext.current
    val rem = vm.focusRemainingSeconds.longValue
    val timeFormatted = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)

    Surface(
        color = AppColors.CyberCyan.copy(alpha = 0.16f),
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
                    Text(vm.activeFocusTitle.value, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Focus session active • Tap to expand", color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(timeFormatted, color = AppColors.CyberCyan, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { vm.togglePauseFocusTimer(context) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (vm.isFocusTimerRunning.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = AppColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StudioBottomDock(currentTab: Int, onSelectTab: (Int) -> Unit, onQuickAction: () -> Unit) {
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
                    label = "dockBg"
                )
                val tint by animateColorAsState(
                    if (selected) AppColors.MintGlow else AppColors.TextSecondary,
                    label = "dockTint"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    AnimatedVisibility(visible = selected) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(AppColors.MintGlow, AppColors.CyberCyan)))
                    .clickable { onQuickAction() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentTab == 0) Icons.Default.Add else Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
        }
    }
}

// ============================================================================
// SECTION 1: STUDIO HABIT TRACKER (Full Edit, Steppers, Reminders, & Heatmaps)
// ============================================================================
@Composable
fun StudioHabitTrackerSection(
    vm: HabitTrackerViewModel,
    onNewHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onLaunchFocusTimer: (Habit) -> Unit
) {
    val context = LocalContext.current
    var habitToLogCustom by remember { mutableStateOf<Habit?>(null) }

    val filteredHabits = remember(vm.habits.toList(), vm.selectedTimeOfDay.value, vm.selectedCategory.value) {
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
        item {
            InteractiveSevenDayCalendarStrip(
                habits = vm.habits,
                selectedDate = vm.selectedDate.value,
                onSelectDate = { vm.selectedDate.value = it }
            )
        }

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
                            border = BorderStroke(1.dp, if (active) AppColors.MintGlow else AppColors.BorderSubtle),
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
                        contentDescription = null,
                        tint = AppColors.ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (filteredHabits.isEmpty()) {
            item {
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Build Your Keystone Routine", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onNewHabit, colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)) {
                            Text("Create First Habit", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
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
                    vm.setHabitMeasurement(context, habit, (cur - habit.stepSize).coerceAtLeast(0f), vm.selectedDate.value)
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
                    animationSpec = tween(450),
                    label = "calProg"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AppColors.MintGlow.copy(alpha = 0.14f) else Color.Transparent)
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
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.5.dp.toPx()
                            drawCircle(color = AppColors.BorderSubtle, style = Stroke(width = stroke))
                            drawArc(
                                color = if (progress >= 1f) AppColors.MintGlow else AppColors.CyberCyan,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text("${date.dayOfMonth}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = habit.isCompletedOn(selectedDate)
    val valueToday = habit.valueOn(selectedDate)
    val progress = habit.progressOn(selectedDate)
    val streak = habit.currentStreak()

    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isDone) habit.accentColor.copy(alpha = 0.65f) else AppColors.BorderSubtle),
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
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(habit.accentColor.copy(alpha = 0.16f))
                            .border(1.dp, habit.accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(habit.emoji, fontSize = 23.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (habit.habitStackingCue.isNotBlank()) {
                            Text("🔗 ${habit.habitStackingCue}", color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔥 $streak d", color = AppColors.SolarGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (habit.reminderTime.isNotBlank()) {
                                Text("🔔 ${habit.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp)
                            }
                            if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                                Text("• ${valueToday.cleanStr()}/${habit.targetValue.cleanStr()} ${habit.unit}", color = habit.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (habit.type == HabitType.FOCUS_TIMER || habit.type == HabitType.MEASUREMENT) {
                        IconButton(onClick = onLaunchFocus, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Timer, "Focus", tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDecrement, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Remove, "Decrease", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onOpenCustomValue, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Dialpad, "Set Exact Value", tint = habit.accentColor, modifier = Modifier.size(15.dp))
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = AppColors.TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isDone) habit.accentColor else habit.accentColor.copy(alpha = 0.16f))
                            .border(1.5.dp, habit.accentColor, RoundedCornerShape(13.dp))
                            .clickable { onToggleOrStep() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                habit.type == HabitType.QUIT_SHIELD -> Icons.Default.Shield
                                isDone -> Icons.Default.Check
                                else -> Icons.Default.Add
                            },
                            contentDescription = null,
                            tint = if (isDone) Color.Black else habit.accentColor
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = habit.accentColor,
                    trackColor = AppColors.BgCardElevated,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(Modifier.height(12.dp))

            when (viewMode) {
                HabitViewMode.STUDIO_CARDS, HabitViewMode.COMPACT_WEEKLY -> StudioSevenDayInteractiveStrip(habit, onToggleAnyDate)
                HabitViewMode.HEATMAP_GRID -> StudioSixteenWeekHeatmap(habit)
            }
        }
    }
}

@Composable
fun StudioSevenDayInteractiveStrip(habit: Habit, onToggleDate: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { d ->
            val done = habit.isCompletedOn(d)
            val raw = habit.valueOn(d)
            val isToday = d == today

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2), color = if (isToday) AppColors.TextPrimary else AppColors.TextMuted, fontSize = 10.sp)
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
                        .border(width = if (isToday) 1.5.dp else 1.dp, color = if (isToday) Color.White else AppColors.BorderSubtle, shape = RoundedCornerShape(10.dp))
                        .clickable { onToggleDate(d) },
                    contentAlignment = Alignment.Center
                ) {
                    if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                        if (raw > 0f) Text(raw.cleanStr(), color = if (done) Color.Black else AppColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else if (done) {
                        Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (w in (weeks - 1) downTo 0) {
            Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                for (d in 6 downTo 0) {
                    val cellDate = today.minusDays((w * 7 + d).toLong())
                    val isDone = habit.isCompletedOn(cellDate)
                    val raw = habit.valueOn(cellDate)
                    val col = when {
                        isDone -> habit.accentColor
                        raw > 0f -> habit.accentColor.copy(alpha = 0.45f)
                        else -> AppColors.BgCardElevated
                    }
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(col))
                }
            }
        }
    }
}

// ============================================================================
// SECTION 2: TO-DO MATRIX (Full Edit, Subtasks, & Custom Time Notifications)
// ============================================================================
@Composable
fun StudioTodoMatrixSection(vm: HabitTrackerViewModel, onLaunchTaskTimer: (TodoTask) -> Unit) {
    val context = LocalContext.current
    var selectedPriority by remember { mutableStateOf<TodoPriority?>(null) }
    var showAddTodo by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TodoTask?>(null) }

    val filteredTodos = remember(vm.todos.toList(), selectedPriority) {
        val base = if (selectedPriority == null) vm.todos else vm.todos.filter { it.priority == selectedPriority }
        base.sortedWith(compareBy<TodoTask> { it.isCompleted }.thenBy { it.priority.ordinal })
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Execution & Priority Matrix", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${vm.todos.count { !it.isCompleted }} active missions • Tap any task to edit", color = AppColors.TextSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    editingTask = null
                    showAddTodo = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddTask, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("New Task", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            item { FilterChip(selected = selectedPriority == null, onClick = { selectedPriority = null }, label = { Text("All") }) }
            items(TodoPriority.entries) { prio ->
                FilterChip(selected = selectedPriority == prio, onClick = { selectedPriority = prio }, label = { Text(prio.label) })
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
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
                showAddTodo = false
                editingTask = null
            },
            onSave = { id, title, cat, prio, remTime, mins, subs, rec ->
                vm.saveOrUpdateTodoTask(context, id, title, cat, prio, remTime, mins, subs, rec)
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) AppColors.BorderSubtle else prioColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
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
                        if (task.isCompleted) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = prioColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(task.priority.label, color = prioColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("📂 ${task.category}", color = AppColors.TextSecondary, fontSize = 11.sp)
                            if (task.reminderTime.isNotBlank()) {
                                Text("🔔 ${task.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onStartFocus, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayCircleOutline, "Focus", tint = AppColors.CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Delete", tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (task.subTasks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.BgCardElevated).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.subTasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onToggleSubTask(sub.id) }
                        ) {
                            Icon(
                                imageVector = if (sub.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (sub.isDone) AppColors.MintGlow else AppColors.TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(sub.title, color = if (sub.isDone) AppColors.TextMuted else AppColors.TextPrimary, fontSize = 13.sp, textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION 3: EYE-SOOTHING SANCTUARY JOURNAL (Themes, Font Scaling, Edit & Photos)
// ============================================================================
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
            .background(theme.bgColor)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sanctuary Journal", color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("Eye-soothing reflections, wins & photo memories", color = theme.textSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    editingJournal = null
                    showWriteJournal = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.EditNote, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Write", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Sanctuary Comfort Theme & Font Size Bar
        SanctuaryComfortBar(
            currentTheme = theme,
            fontScale = fontScale,
            onSelectTheme = {
                vm.readingTheme.value = it
                vm.persistState(context)
            },
            onChangeFontScale = { newScale ->
                vm.readingFontScale.floatValue = newScale.coerceIn(0.85f, 1.35f)
                vm.persistState(context)
            }
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                    border = BorderStroke(1.dp, if (active) st.accent else st.borderColor),
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

        // Font Scale A- / A+ Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(currentTheme.cardColor)
                .border(1.dp, currentTheme.borderColor, RoundedCornerShape(10.dp))
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
        border = BorderStroke(1.dp, theme.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(entry.moodEmoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(entry.title, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                        Text("${entry.date}  •  ⚡ Energy: ${entry.energyScore}/10", color = theme.accent, fontSize = 12.sp)
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
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
                Surface(color = theme.accent.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("✨ DAILY WINS & GRATITUDE", color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.gratitudeOrWins, color = theme.textPrimary, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = FontFamily.Serif)
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
                Surface(color = AppColors.CrimsonRose.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔍 REFLECTION ON MISTAKES / FRICTION", color = AppColors.CrimsonRose, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.frictionOrMistakes, color = theme.textPrimary, fontSize = (13.5f * fontScale).sp, lineHeight = (20f * fontScale).sp)
                    }
                }
            }

            if (entry.actionItems.isNotEmpty()) {
                Surface(color = AppColors.MintGlow.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🎯 ACTIONS TO TAKE", color = AppColors.MintGlow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        entry.actionItems.forEach { act ->
                            Text("• $act", color = theme.textPrimary, fontSize = (13.5f * fontScale).sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SECTION 4: SECOND-BRAIN VAULT (Sanctuary Reading Mode + Edit & Flashcards)
// ============================================================================
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
        vm.notes.filter { n ->
            val matchesCat = catFilter == null || n.category == catFilter
            val matchesQ = query.isBlank() ||
                    n.title.contains(query, ignoreCase = true) ||
                    n.authorOrSubtitle.contains(query, ignoreCase = true) ||
                    n.detailedNotes.contains(query, ignoreCase = true)
            matchesCat && matchesQ
        }.sortedByDescending { it.isPinned }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgColor)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Second-Brain Vault", color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("Book summaries, quotes & active recall", color = theme.textSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    editingNote = null
                    showCreateNote = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.LibraryAdd, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("New Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        SanctuaryComfortBar(
            currentTheme = theme,
            fontScale = fontScale,
            onSelectTheme = {
                vm.readingTheme.value = it
                vm.persistState(context)
            },
            onChangeFontScale = { newScale ->
                vm.readingFontScale.floatValue = newScale.coerceIn(0.85f, 1.35f)
                vm.persistState(context)
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search books, authors, quotes, or takeaways...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.textSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            item { FilterChip(selected = catFilter == null, onClick = { catFilter = null }, label = { Text("All (${vm.notes.size})") }) }
            items(NoteCategory.entries) { c ->
                FilterChip(selected = catFilter == c, onClick = { catFilter = c }, label = { Text(c.label) })
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
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
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(6.dp).height(52.dp).clip(RoundedCornerShape(4.dp)).background(accent))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(note.category.label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        if (note.category == NoteCategory.BOOK_SUMMARY) {
                            Spacer(Modifier.width(8.dp))
                            Text("• ${note.readingProgress}% Read", color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(note.title, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                    Text(note.authorOrSubtitle, color = theme.textSecondary, fontSize = 13.sp)
                }

                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.PinInvoke,
                        contentDescription = null,
                        tint = if (note.isPinned) AppColors.SolarGold else theme.textSecondary
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            if (note.favoriteQuote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = theme.bgColor, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
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
                Text("💡 KEY TAKEAWAYS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable { flipped = !flipped }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (flipped) "🧠 ANSWER (Tap to flip)" else "🧠 ACTIVE RECALL QUESTION (Tap to reveal)", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(if (flipped) fc.answer else fc.question, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = bodyFontSize)
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIALOGS: CREATE / EDIT HABIT, TODO, JOURNAL, NOTEBOOK & NATIVE TIME PICKER
// ============================================================================
@Composable
fun StudioCreateOrEditHabitDialog(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, TimeOfDay, String, HabitType, Long, Float, String, Float, String) -> Unit
) {
    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var emoji by remember { mutableStateOf(habitToEdit?.emoji ?: "⚡") }
    var category by remember { mutableStateOf(habitToEdit?.category ?: "Growth") }
    var timeOfDay by remember { mutableStateOf(habitToEdit?.timeOfDay ?: TimeOfDay.MORNING) }
    var cue by remember { mutableStateOf(habitToEdit?.habitStackingCue ?: "") }
    var type by remember { mutableStateOf(habitToEdit?.type ?: HabitType.CHECKLIST) }
    var target by remember { mutableStateOf(habitToEdit?.targetValue?.cleanStr() ?: "1") }
    var unit by remember { mutableStateOf(habitToEdit?.unit ?: "times") }
    var step by remember { mutableStateOf(habitToEdit?.stepSize?.cleanStr() ?: "1") }
    var reminder by remember { mutableStateOf(habitToEdit?.reminderTime ?: "") }
    var selectedColor by remember { mutableLongStateOf(habitToEdit?.colorHex ?: 0xFF34D399L) }

    val context = LocalContext.current
    val colorChoices = listOf(0xFF34D399L, 0xFFA855F7L, 0xFF06B6D4L, 0xFFF97316L, 0xFFF59E0BL, 0xFFF43F5EL)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text(if (habitToEdit == null) "Design New Habit" else "Edit Habit", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Icon") }, modifier = Modifier.width(70.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Habit Name") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = cue, onValueChange = { cue = it }, label = { Text("Habit Stacking Cue (Optional)") }, modifier = Modifier.fillMaxWidth())

                // Native Clock Picker + Test Motivational Sound Button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val initH = reminder.split(":").getOrNull(0)?.toIntOrNull() ?: 8
                            val initM = reminder.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, h, m -> reminder = String.format(Locale.US, "%02d:%02d", h, m) },
                                initH,
                                initM,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppColors.CyberCyan)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (reminder.isBlank()) "Set Reminder Time" else "🔔 $reminder", color = AppColors.CyberCyan, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { CelebrationSoundEngine.playMotivationalReminderSound(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.ElectricViolet)
                    ) {
                        Text("Test Sound", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

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
                        OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Goal") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = step, onValueChange = { step = it }, label = { Text("Step") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorChoices.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(width = if (selectedColor == hex) 2.5.dp else 0.dp, color = Color.White, shape = CircleShape)
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
                            habitToEdit?.id, title, emoji, category, timeOfDay, cue, type, selectedColor,
                            target.toFloatOrNull() ?: 1f, unit, step.toFloatOrNull() ?: 1f, reminder
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Habit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudioAddOrEditTodoDialog(
    taskToEdit: TodoTask?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, TodoPriority, String, Int, List<String>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Work") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TodoPriority.P1_CRITICAL) }
    var reminder by remember { mutableStateOf(taskToEdit?.reminderTime ?: "") }
    var mins by remember { mutableStateOf(taskToEdit?.estimatedMinutes?.toString() ?: "25") }
    var subtasksRaw by remember { mutableStateOf(taskToEdit?.subTasks?.joinToString("\n") { it.title } ?: "") }
    var recurring by remember { mutableStateOf(taskToEdit?.isRecurring ?: false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text(if (taskToEdit == null) "Add Mission" else "Edit Mission", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mission Title") })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TodoPriority.entries) { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label, fontSize = 11.sp) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            val initH = reminder.split(":").getOrNull(0)?.toIntOrNull() ?: 9
                            val initM = reminder.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, h, m -> reminder = String.format(Locale.US, "%02d:%02d", h, m) },
                                initH,
                                initM,
                                true
                            ).show()
                        },
                        border = BorderStroke(1.dp, AppColors.CyberCyan)
                    ) {
                        Text(if (reminder.isBlank()) "🔔 Set Time" else "🔔 $reminder", color = AppColors.CyberCyan, fontSize = 12.sp)
                    }
                }
                OutlinedTextField(value = subtasksRaw, onValueChange = { subtasksRaw = it }, label = { Text("Subtasks (one per line)") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(taskToEdit?.id, title, category, priority, reminder, mins.toIntOrNull() ?: 25, subtasksRaw.lines(), recurring)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Mission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudioCreateOrEditJournalDialog(
    entryToEdit: JournalEntry?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, Int, String, String, String, List<String>, List<String>, List<String>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(entryToEdit?.title ?: "") }
    var mood by remember { mutableStateOf(entryToEdit?.moodEmoji ?: "🔥") }
    var energy by remember { mutableFloatStateOf(entryToEdit?.energyScore?.toFloat() ?: 8f) }
    var wins by remember { mutableStateOf(entryToEdit?.gratitudeOrWins ?: "") }
    var story by remember { mutableStateOf(entryToEdit?.dailyStory ?: "") }
    var mistakes by remember { mutableStateOf(entryToEdit?.frictionOrMistakes ?: "") }
    var actionsRaw by remember { mutableStateOf(entryToEdit?.actionItems?.joinToString("\n") ?: "") }
    var pushToTodo by remember { mutableStateOf(entryToEdit == null) }
    val photos = remember { mutableStateListOf<String>().apply { entryToEdit?.photoUris?.let { addAll(it) } } }

    val picker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)) { uris ->
        uris.forEach { photos.add(it.toString()) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (entryToEdit == null) "Log Sanctuary Reflection" else "Edit Journal Entry", color = AppColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🔥", "🌟", "😊", "🧘", "💪", "🌧️").forEach { e ->
                        FilterChip(selected = mood == e, onClick = { mood = e }, label = { Text(e, fontSize = 16.sp) })
                    }
                }

                Text("Energy Level: ${energy.roundToInt()}/10", color = AppColors.MintGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(value = energy, onValueChange = { energy = it }, valueRange = 1f..10f, steps = 8)

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Headline of the Day") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wins, onValueChange = { wins = it }, label = { Text("Daily Wins & Gratitude") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = story, onValueChange = { story = it }, label = { Text("Daily Activities & Reflection Story") }, minLines = 3, modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.ElectricViolet)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = AppColors.ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Memory Photos (${photos.size} attached)", color = AppColors.ElectricViolet)
                }

                OutlinedTextField(value = mistakes, onValueChange = { mistakes = it }, label = { Text("What went wrong / Mistakes made?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = actionsRaw, onValueChange = { actionsRaw = it }, label = { Text("Actions I must take (1 per line)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pushToTodo, onCheckedChange = { pushToTodo = it })
                    Text("Add these actions to my To-Do List", color = AppColors.TextSecondary, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                entryToEdit?.id, title, mood, energy.roundToInt(), wins, story, mistakes,
                                actionsRaw.lines().filter { it.isNotBlank() }, photos.toList(), listOf("Journal"), pushToTodo
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
fun StudioCreateOrEditNoteDialog(
    noteToEdit: NotebookEntry?,
    onDismiss: () -> Unit,
    onSave: (NotebookEntry) -> Unit
) {
    var category by remember { mutableStateOf(noteToEdit?.category ?: NoteCategory.BOOK_SUMMARY) }
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var author by remember { mutableStateOf(noteToEdit?.authorOrSubtitle ?: "") }
    var progress by remember { mutableStateOf(noteToEdit?.readingProgress?.toString() ?: "100") }
    var quote by remember { mutableStateOf(noteToEdit?.favoriteQuote ?: "") }
    var takeaways by remember { mutableStateOf(noteToEdit?.keyTakeaways?.joinToString("\n") ?: "") }
    var notes by remember { mutableStateOf(noteToEdit?.detailedNotes ?: "") }
    var flashQ by remember { mutableStateOf(noteToEdit?.flashcards?.firstOrNull()?.question ?: "") }
    var flashA by remember { mutableStateOf(noteToEdit?.flashcards?.firstOrNull()?.answer ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text(if (noteToEdit == null) "Add to Vault" else "Edit Vault Note", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NoteCategory.entries) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Book / Note Title") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author / Topic") }, modifier = Modifier.weight(1f))
                    if (category == NoteCategory.BOOK_SUMMARY) {
                        OutlinedTextField(value = progress, onValueChange = { progress = it }, label = { Text("% Read") }, modifier = Modifier.width(80.dp))
                    }
                }
                OutlinedTextField(value = quote, onValueChange = { quote = it }, label = { Text("Favorite Quote") })
                OutlinedTextField(value = takeaways, onValueChange = { takeaways = it }, label = { Text("Key Takeaways (1 per line)") }, minLines = 2)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Detailed Summary / Notes") }, minLines = 3)
                OutlinedTextField(value = flashQ, onValueChange = { flashQ = it }, label = { Text("Flashcard Question") })
                OutlinedTextField(value = flashA, onValueChange = { flashA = it }, label = { Text("Flashcard Answer") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val fc = if (flashQ.isNotBlank() && flashA.isNotBlank()) listOf(FlashcardItem(flashQ, flashA)) else emptyList()
                        onSave(
                            NotebookEntry(
                                id = noteToEdit?.id ?: UUID.randomUUID().toString(),
                                category = category,
                                title = title,
                                authorOrSubtitle = author,
                                readingProgress = (progress.toIntOrNull() ?: 100).coerceIn(0, 100),
                                isPinned = noteToEdit?.isPinned ?: false,
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
                Text("Save Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FocusTimerStudioDialog(vm: HabitTrackerViewModel, onDismiss: () -> Unit) {
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
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DEEP FOCUS STUDIO", color = AppColors.CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(vm.activeFocusTitle.value, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(22.dp))

                Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
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
                        Text(formatted, color = AppColors.TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (vm.isFocusTimerRunning.value) "FLOW ACTIVE" else "READY", color = AppColors.MintGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
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
                    OutlinedButton(onClick = { vm.stopFocusTimer() }) { Text("Reset") }
                    Button(
                        onClick = { vm.togglePauseFocusTimer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.CyberCyan)
                    ) {
                        Text(if (vm.isFocusTimerRunning.value) "Pause" else "Start", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudioAnalyticsModal(vm: HabitTrackerViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, AppColors.BorderSubtle),
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📊 Studio Performance Analytics", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary) }
                }

                val best = vm.habits.maxOfOrNull { it.bestStreak() } ?: 0
                val avg = if (vm.habits.isEmpty()) 0 else vm.habits.map { it.completionRate30Days() }.average().roundToInt()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), "🔥", "$best d", "Peak Streak", AppColors.SunsetCoral)
                    StatBox(Modifier.weight(1f), "📈", "$avg%", "30d Consistency", AppColors.MintGlow)
                    StatBox(Modifier.weight(1f), "⚡", "Lv.${vm.userLevel}", "${vm.userXp.intValue} XP", AppColors.ElectricViolet)
                }

                vm.habits.forEach { habit ->
                    Surface(color = AppColors.BgCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, AppColors.BorderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${habit.emoji} ${habit.title}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${habit.completionRate30Days()}% (30d)", color = habit.accentColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            StudioSixteenWeekHeatmap(habit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier, icon: String, value: String, label: String, accent: Color) {
    Surface(modifier = modifier, color = AppColors.BgCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(label, color = accent, fontSize = 11.sp)
        }
    }
}

@Composable
fun GoogleDriveBackupVaultDialog(vm: HabitTrackerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(vm.connectedGoogleEmail.value ?: "") }
    val storage = remember { StudioStorageManager(context) }

    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val json = vm.persistState(context)
            val ok = storage.exportBackupToUri(it, json)
            Toast.makeText(context, if (ok) "Backup saved to Google Drive!" else "Export failed", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = storage.readBackupFromUri(it)
            if (content != null && vm.restoreFromJsonString(content)) {
                vm.persistState(context)
                Toast.makeText(context, "Backup restored!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("☁️ Google Drive Backup", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("All habits, streaks, tasks, and sanctuary notes are securely backed up.", color = AppColors.TextSecondary, fontSize = 13.sp)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Last Sync: ${vm.lastCloudBackup.value}", color = AppColors.MintGlow, fontSize = 12.sp)

                OutlinedButton(onClick = { exportLauncher.launch("ZenithStudio_Backup.json") }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.MintGlow)) {
                    Icon(Icons.Default.CloudUpload, null, tint = AppColors.MintGlow)
                    Spacer(Modifier.width(8.dp))
                    Text("Export to Google Drive", color = AppColors.MintGlow)
                }

                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.CyberCyan)) {
                    Icon(Icons.Default.CloudDownload, null, tint = AppColors.CyberCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore from Google Drive", color = AppColors.CyberCyan)
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
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private data class StudioParticle(val vx: Float, val vy: Float, val color: Color, val size: Float)

@Composable
fun StudioFullScreenCelebrationOverlay(
    title: String,
    subtitle: String,
    xpEarned: Int,
    level: Int,
    rank: String,
    onDismiss: () -> Unit
) {
    // 65 particles for silky 60fps animation with minimal battery/GPU draw
    val particles = remember {
        List(65) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val speed = Random.nextFloat() * 920f + 220f
            StudioParticle(
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 320f).toFloat(),
                color = AppColors.StudioPalette.random(),
                size = Random.nextFloat() * 10f + 6f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(2500, easing = LinearOutSlowInEasing)) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = anim.value
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f

            drawCircle(
                color = AppColors.MintGlow.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.75f * t,
                center = Offset(cx, cy),
                style = Stroke(width = 5.dp.toPx())
            )

            particles.forEach { p ->
                val gravity = 1150f * t * t
                val x = cx + p.vx * t
                val y = cy + p.vy * t + 0.5f * gravity
                val a = (1f - t * 0.85f).coerceIn(0f, 1f)
                drawCircle(color = p.color.copy(alpha = a), radius = p.size, center = Offset(x, y))
            }
        }

        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(2.dp, AppColors.MintGlow),
            modifier = Modifier.padding(28.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Surface(color = AppColors.MintGlow.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Text("+$xpEarned XP  •  Lv.$level $rank", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = AppColors.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = AppColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow), shape = RoundedCornerShape(14.dp)) {
                    Text("Continue Momentum", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomMeasurementDialog(habit: Habit, selectedDate: LocalDate, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var valText by remember { mutableStateOf(habit.valueOn(selectedDate).cleanStr()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("${habit.emoji} Log ${habit.title}", color = AppColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = valText,
                onValueChange = { valText = it },
                label = { Text("Amount (${habit.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(valText.toFloatOrNull() ?: 0f) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Float.cleanStr(): String =
    if (this % 1f == 0f) this.toInt().toString() else String.format(Locale.US, "%.1f", this)
