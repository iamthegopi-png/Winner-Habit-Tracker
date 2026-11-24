package com.vibrant.habittracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private val resumeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppColors.BgDeep,
                    surface = AppColors.BgCard,
                    primary = AppColors.MintGlow,
                    secondary = AppColors.CyberCyan,
                    onBackground = AppColors.TextPrimary,
                    onSurface = AppColors.TextPrimary
                )
            ) {
                AlphaMindStateStudioApp(resumeTick = resumeSignal.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeSignal.intValue += 1
    }

    override fun onDestroy() {
        CelebrationSoundEngine.stopAmbientSound()
        CelebrationSoundEngine.stopActiveCustomSound()
        super.onDestroy()
    }
}

@Composable
fun AlphaMindStateStudioApp(
    resumeTick: Int,
    vm: HabitTrackerViewModel = viewModel()
) {
    val context = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        vm.refreshNotificationHealth(context)
    }

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

    LaunchedEffect(resumeTick) {
        if (resumeTick > 0) {
            vm.refreshNotificationHealth(context)
            vm.checkAndAutoRenewDailyTasks(context, forceRenewForTesting = false)
            vm.refreshCachedMetrics()
        }
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showSentinelModal by remember { mutableStateOf(false) }
    var showBrainWidgetStudio by remember { mutableStateOf(false) }
    var showNotificationShieldModal by remember { mutableStateOf(false) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showCloudVaultModal by remember { mutableStateOf(false) }
    var showFocusTimerModal by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    val skyColors = rememberTimeOfDayAmbientColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = skyColors))
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AlphaTitaniumTopBar(
                    vm = vm,
                    onOpenSentinelSheet = { showSentinelModal = true },
                    onOpenBrainStudio = { showBrainWidgetStudio = true },
                    onOpenNotifShield = {
                        vm.refreshNotificationHealth(context)
                        showNotificationShieldModal = true
                    },
                    onOpenTimer = { showFocusTimerModal = true },
                    onOpenAnalytics = { showAnalyticsSheet = true },
                    onOpenCloud = { showCloudVaultModal = true }
                )
            },
            bottomBar = {
                StudioTitaniumBottomDock(
                    currentTab = currentTab,
                    onSelectTab = { currentTab = it }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = vm.isFocusTimerRunning.value,
                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
                ) {
                    ActiveFocusTimerBanner(
                        vm = vm,
                        onExpand = { showFocusTimerModal = true }
                    )
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                            fadeOut(tween(130))
                    },
                    label = "StudioTabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> StudioHabitTrackerSection(
                            vm = vm,
                            onOpenSentinelSheet = { showSentinelModal = true },
                            onOpenBrainStudio = { showBrainWidgetStudio = true },
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
                                    minutes = if (habit.type == HabitType.FOCUS_TIMER) {
                                        habit.stepSize.toInt().coerceAtLeast(15)
                                    } else {
                                        25
                                    },
                                    habitId = habit.id
                                )
                                showFocusTimerModal = true
                            }
                        )
                        1 -> StudioGoalsAchievementSection(vm = vm)
                        2 -> StudioTodoMatrixSection(
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
                        3 -> StudioDayProductivityAuditSection(vm = vm)
                        else -> StudioSanctuaryHubSection(vm = vm)
                    }
                }
            }
        }

        if (showBrainWidgetStudio) {
            BrainMoodWidgetStudioModal(
                vm = vm,
                onDismiss = { showBrainWidgetStudio = false }
            )
        }

        if (showNotificationShieldModal) {
            NotificationAndBatteryShieldDialog(
                vm = vm,
                onRequestNotifPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showNotificationShieldModal = false }
            )
        }

        if (showSentinelModal) {
            DarkKnightArmoryModal(
                vm = vm,
                onDismiss = { showSentinelModal = false }
            )
        }

        if (showFocusTimerModal) {
            FocusTimerStudioDialog(
                vm = vm,
                onDismiss = { showFocusTimerModal = false }
            )
        }

        if (showAnalyticsSheet) {
            StudioAnalyticsModal(
                vm = vm,
                onDismiss = { showAnalyticsSheet = false }
            )
        }

        if (showCloudVaultModal) {
            GoogleDriveBackupVaultDialog(
                vm = vm,
                onDismiss = { showCloudVaultModal = false }
            )
        }

        if (showHabitDialog) {
            StudioCreateOrEditHabitDialog(
                habitToEdit = editingHabit,
                onDismiss = {
                    CelebrationSoundEngine.stopActiveCustomSound()
                    showHabitDialog = false
                    editingHabit = null
                },
                onSave = { id, title, emoji, cat, tod, cue, type, col, target, unit, step, reminder, customSound ->
                    CelebrationSoundEngine.stopActiveCustomSound()
                    vm.saveOrUpdateHabit(
                        context = context,
                        existingId = id,
                        title = title,
                        emoji = emoji,
                        category = cat,
                        timeOfDay = tod,
                        cue = cue,
                        type = type,
                        colorHex = col,
                        targetValue = target,
                        unit = unit,
                        stepSize = step,
                        reminderTime = reminder,
                        customSoundUri = customSound
                    )
                    showHabitDialog = false
                    editingHabit = null
                }
            )
        }

        AnimatedVisibility(
            visible = vm.showCelebration.value,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(300))
        ) {
            StudioFullScreenCelebrationOverlay(
                stage = vm.cachedCharacterStats.value.stage,
                title = vm.celebrationTitle.value,
                subtitle = vm.celebrationSubtitle.value,
                xpEarned = vm.celebrationXpEarned.intValue,
                level = vm.userLevel,
                rank = vm.userRankTitle,
                onDismiss = {
                    CelebrationSoundEngine.stopActiveCustomSound()
                    vm.showCelebration.value = false
                }
            )
        }
    }
}
