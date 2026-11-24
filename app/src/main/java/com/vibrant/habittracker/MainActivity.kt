package com.vibrant.habittracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
                AlphaTitaniumStudioApp(resumeTick = resumeSignal.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeSignal.intValue += 1
    }
}

// ============================================================================
// APPLE / ONE UI SPRING-PHYSICS TOUCH MODIFIER (Zero Idle CPU Cost)
// ============================================================================
fun Modifier.studioBounceClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressBounce"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// ============================================================================
// MAIN TITANIUM STUDIO SCAFFOLD (5 Clean Tabs: Habits, Goals, Tasks, Audit, Sanctuary)
// ============================================================================
@Composable
fun AlphaTitaniumStudioApp(
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
        }
    }

    // 0 = Habits, 1 = Goals, 2 = Tasks, 3 = Day Audit, 4 = Sanctuary (Journal & Vault)
    var currentTab by remember { mutableIntStateOf(0) }
    var showSentinelModal by remember { mutableStateOf(false) }
    var showNotificationShieldModal by remember { mutableStateOf(false) }
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
                    colors = listOf(Color(0xFF090D16), AppColors.BgDeep, Color(0xFF030508))
                )
            )
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AlphaTitaniumTopBar(
                    vm = vm,
                    onOpenSentinelSheet = { showSentinelModal = true },
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

// ============================================================================
// 3D METALLIC FACETED ALPHA LOGO & TITANIUM TOP BAR
// ============================================================================
@Composable
fun AlphaTitaniumLogo(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF131B2E), Color(0xFF080C15), Color(0xFF0E1626))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(AppColors.Platinum.copy(alpha = 0.55f), AppColors.CyberCyan.copy(alpha = 0.55f), AppColors.BorderSubtle)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .studioBounceClick(onClick)
    ) {
        val w = size.width
        val h = size.height

        // 3D Dark Titanium Octagonal Shield Base
        val plate = Path().apply {
            moveTo(w * 0.50f, h * 0.13f)
            lineTo(w * 0.80f, h * 0.27f)
            lineTo(w * 0.84f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.87f)
            lineTo(w * 0.16f, h * 0.60f)
            lineTo(w * 0.20f, h * 0.27f)
            close()
        }
        drawPath(
            plate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1F2A40), Color(0xFF0B101B)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
        // Left bevel specular sheen
        val leftFacet = Path().apply {
            moveTo(w * 0.50f, h * 0.13f)
            lineTo(w * 0.20f, h * 0.27f)
            lineTo(w * 0.16f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.87f)
            close()
        }
        drawPath(leftFacet, color = Color.White.copy(alpha = 0.08f))
        drawPath(
            plate,
            brush = Brush.linearGradient(listOf(AppColors.CyberCyan, AppColors.MintGlow)),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )

        // Sculpted 3D Platinum Alpha Apex Left & Right Wings
        val leftAlpha = Path().apply {
            moveTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.29f, h * 0.72f)
            lineTo(w * 0.39f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.44f)
            close()
        }
        val rightAlpha = Path().apply {
            moveTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.71f, h * 0.72f)
            lineTo(w * 0.61f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.44f)
            close()
        }
        drawPath(leftAlpha, color = Color(0xFFF8FAFC))
        drawPath(rightAlpha, color = Color(0xFF94A3B8))

        // Emerald-Cyan Energy Crossbar
        val strike = Path().apply {
            moveTo(w * 0.28f, h * 0.59f)
            lineTo(w * 0.50f, h * 0.49f)
            lineTo(w * 0.50f, h * 0.57f)
            lineTo(w * 0.72f, h * 0.47f)
        }
        drawPath(
            strike,
            color = AppColors.MintGlow,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Gold Apex Spark
        drawCircle(color = AppColors.SolarGold, radius = 2.dp.toPx(), center = Offset(w * 0.50f, h * 0.22f))
    }
}

@Composable
fun AlphaTitaniumTopBar(
    vm: HabitTrackerViewModel,
    onOpenSentinelSheet: () -> Unit,
    onOpenNotifShield: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val xpProg by animateFloatAsState(
        targetValue = vm.xpProgressInLevel,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "xpBar"
    )
    val stats = vm.cachedCharacterStats.value
    val dayScore = vm.cachedTodayScore.intValue
    val notifHealth = vm.notificationHealth.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .studioBounceClick(onOpenSentinelSheet)
                .padding(end = 6.dp)
        ) {
            AlphaTitaniumLogo(onClick = onOpenSentinelSheet)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stats.stage.title,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = AppColors.MintGlow.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, AppColors.MintGlow.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Lv.${vm.userLevel}",
                            color = AppColors.MintGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { xpProg },
                        color = Color(stats.stage.primaryColorHex),
                        trackColor = AppColors.BgCardElevated,
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "${vm.userXp.intValue} XP • $dayScore%",
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                HeaderActionButton(
                    icon = Icons.Default.NotificationsActive,
                    tint = if (notifHealth.allHealthy) AppColors.MintGlow else AppColors.SolarGold,
                    onClick = onOpenNotifShield
                )
                if (!notifHealth.allHealthy) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.CrimsonRose)
                    )
                }
            }
            HeaderActionButton(Icons.Default.Timer, AppColors.CyberCyan, onOpenTimer)
            HeaderActionButton(Icons.Default.ShowChart, AppColors.Platinum, onOpenAnalytics)
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
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.BgCard)
            .border(0.8.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp))
            .studioBounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ============================================================================
// 3D-SHADED STUDIO-GRADE DARK KNIGHT SENTINEL CANVAS (Evolves Step-by-Step)
// ============================================================================
@Composable
fun DarkKnight3DCanvas(
    stage: CharacterStage,
    modifier: Modifier = Modifier,
    animateStudioLight: Boolean = false
) {
    val primary = Color(stage.primaryColorHex)
    val secondary = Color(stage.secondaryColorHex)
    val capeBase = Color(stage.capeColorHex)

    // Smooth GPU hover + subtle breathing light — ONLY active inside Armory modal (0% idle battery drain on main screen)
    val hoverY: Float
    val auraPulse: Float
    if (animateStudioLight) {
        val infinite = rememberInfiniteTransition(label = "knight3d")
        val y by infinite.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "floatY"
        )
        val p by infinite.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "auraScale"
        )
        hoverY = y
        auraPulse = p
    } else {
        hoverY = 0f
        auraPulse = 1f
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = hoverY
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.55f
        val tier = stage.stageNumber

        // 1. Studio Rim-Light Backdrop Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = (0.28f + tier * 0.04f).coerceAtMost(0.55f)),
                    secondary.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(cx, cy - h * 0.08f),
                radius = w * 0.48f * auraPulse
            ),
            radius = w * 0.48f * auraPulse,
            center = Offset(cx, cy - h * 0.08f)
        )

        // Stage 6 Sovereign Outer Ring
        if (tier >= 6) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(AppColors.SolarGold, primary, Color.Transparent, AppColors.SolarGold)),
                radius = w * 0.44f,
                center = Offset(cx, cy - h * 0.06f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 2. Sculpted Multi-Fold Dark Knight Cloak / Cape (Expands wider at Stage 4+)
        val capeSpread = if (tier >= 4) 0.46f else 0.37f
        val backCape = Path().apply {
            moveTo(cx - w * 0.22f, cy - h * 0.16f)
            lineTo(cx + w * 0.22f, cy - h * 0.16f)
            lineTo(cx + w * capeSpread, cy + h * 0.36f)
            lineTo(cx + w * 0.24f, cy + h * 0.30f)
            lineTo(cx + w * 0.11f, cy + h * 0.39f)
            lineTo(cx, cy + h * 0.32f)
            lineTo(cx - w * 0.11f, cy + h * 0.39f)
            lineTo(cx - w * 0.24f, cy + h * 0.30f)
            lineTo(cx - w * capeSpread, cy + h * 0.36f)
            close()
        }
        drawPath(
            backCape,
            brush = Brush.verticalGradient(
                colors = listOf(capeBase, Color(0xFF0A0D14), Color(0xFF04060A))
            )
        )
        // Left & Right 3D Cape Rim Lighting
        drawPath(
            backCape,
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.65f), Color.Transparent, secondary.copy(alpha = 0.55f)),
                start = Offset(0f, cy - h * 0.15f),
                end = Offset(w, cy + h * 0.35f)
            ),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )

        // Stage 3+ High Tactical Mantle Collar behind Cowl
        if (tier >= 3) {
            val collar = Path().apply {
                moveTo(cx - w * 0.24f, cy - h * 0.12f)
                lineTo(cx - w * 0.19f, cy - h * 0.26f)
                lineTo(cx + w * 0.19f, cy - h * 0.26f)
                lineTo(cx + w * 0.24f, cy - h * 0.12f)
                close()
            }
            drawPath(
                collar,
                brush = Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF090D16)))
            )
            drawPath(collar, color = primary.copy(alpha = 0.55f), style = Stroke(width = 1.2.dp.toPx()))
        }

        // 3. 3D Sculpted Obsidian Armored Torso (Directional Key Light + Rim Shadow)
        val shoulderHalf = w * (0.25f + tier * 0.011f)
        val waistHalf = w * 0.14f
        val torsoBase = Path().apply {
            moveTo(cx - shoulderHalf, cy - h * 0.13f)
            lineTo(cx - w * 0.13f, cy - h * 0.19f)
            lineTo(cx + w * 0.13f, cy - h * 0.19f)
            lineTo(cx + shoulderHalf, cy - h * 0.13f)
            lineTo(cx + waistHalf, cy + h * 0.25f)
            lineTo(cx, cy + h * 0.31f)
            lineTo(cx - waistHalf, cy + h * 0.25f)
            close()
        }
        drawPath(
            torsoBase,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF161E2E), Color(0xFF080B12)),
                start = Offset(cx - shoulderHalf, cy - h * 0.20f),
                end = Offset(cx + shoulderHalf, cy + h * 0.28f)
            )
        )
        drawPath(
            torsoBase,
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), primary.copy(alpha = 0.85f))),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // 3D Sculpted Left & Right Pectoral Armor Plates (Key-lit on left, shadowed on right)
        val leftPec = Path().apply {
            moveTo(cx - w * 0.015f, cy - h * 0.13f)
            lineTo(cx - shoulderHalf * 0.72f, cy - h * 0.11f)
            lineTo(cx - shoulderHalf * 0.62f, cy + h * 0.02f)
            lineTo(cx - w * 0.015f, cy + h * 0.04f)
            close()
        }
        val rightPec = Path().apply {
            moveTo(cx + w * 0.015f, cy - h * 0.13f)
            lineTo(cx + shoulderHalf * 0.72f, cy - h * 0.11f)
            lineTo(cx + shoulderHalf * 0.62f, cy + h * 0.02f)
            lineTo(cx + w * 0.015f, cy + h * 0.04f)
            close()
        }
        drawPath(leftPec, brush = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))))
        drawPath(rightPec, brush = Brush.linearGradient(listOf(Color(0xFF273449), Color(0xFF0F172A))))

        // Sculpted Abdominal Armor Segments
        for (seg in 0..2) {
            val topY = cy + h * (0.06f + seg * 0.055f)
            val segW = waistHalf * (0.78f - seg * 0.08f)
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF111827))),
                topLeft = Offset(cx - segW, topY),
                size = Size(segW * 2f, h * 0.038f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // 4. Layered 3D Shoulder Pauldrons (Upgraded from Stage 2+)
        if (tier >= 2) {
            val leftPauldron = Path().apply {
                moveTo(cx - shoulderHalf * 1.12f, cy - h * 0.09f)
                lineTo(cx - shoulderHalf * 0.56f, cy - h * 0.21f)
                lineTo(cx - shoulderHalf * 0.44f, cy - h * 0.02f)
                lineTo(cx - shoulderHalf * 0.92f, cy + h * 0.02f)
                close()
            }
            val rightPauldron = Path().apply {
                moveTo(cx + shoulderHalf * 1.12f, cy - h * 0.09f)
                lineTo(cx + shoulderHalf * 0.56f, cy - h * 0.21f)
                lineTo(cx + shoulderHalf * 0.44f, cy - h * 0.02f)
                lineTo(cx + shoulderHalf * 0.92f, cy + h * 0.02f)
                close()
            }
            val pauldronBrush = Brush.linearGradient(
                colors = if (tier >= 5) listOf(AppColors.SolarGold, primary) else listOf(Color(0xFF475569), Color(0xFF111827))
            )
            drawPath(leftPauldron, brush = pauldronBrush)
            drawPath(rightPauldron, brush = pauldronBrush)
            drawPath(leftPauldron, color = primary.copy(alpha = 0.85f), style = Stroke(width = 1.3.dp.toPx()))
            drawPath(rightPauldron, color = primary.copy(alpha = 0.85f), style = Stroke(width = 1.3.dp.toPx()))
        }

        // 5. Iconic Winged Dark Sentinel Chest Crest (Matte at Stage 1-2, Illuminated Kinetic Core at Stage 3+)
        val crestCy = cy - h * 0.03f
        val wingedEmblem = Path().apply {
            moveTo(cx, crestCy - h * 0.045f)
            lineTo(cx + w * 0.04f, crestCy - h * 0.015f)
            lineTo(cx + w * 0.145f, crestCy - h * 0.04f)
            lineTo(cx + w * 0.105f, crestCy + h * 0.02f)
            lineTo(cx + w * 0.045f, crestCy + h * 0.015f)
            lineTo(cx, crestCy + h * 0.055f)
            lineTo(cx - w * 0.045f, crestCy + h * 0.015f)
            lineTo(cx - w * 0.105f, crestCy + h * 0.02f)
            lineTo(cx - w * 0.145f, crestCy - h * 0.04f)
            lineTo(cx - w * 0.04f, crestCy - h * 0.015f)
            close()
        }
        val emblemColor = if (tier >= 3) primary else Color(0xFF0A0E17)
        drawPath(wingedEmblem, color = emblemColor)
        drawPath(
            wingedEmblem,
            color = if (tier >= 5) AppColors.SolarGold else Color.White.copy(alpha = 0.7f),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
        )

        // Tactical Metallic Utility Belt (Upgrades to Gold Titanium at Stage 2+)
        val beltY = cy + h * 0.23f
        drawLine(
            color = if (tier >= 2) AppColors.SolarGold else Color(0xFF475569),
            start = Offset(cx - waistHalf * 1.05f, beltY),
            end = Offset(cx + waistHalf * 1.05f, beltY),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = if (tier >= 5) Color.White else AppColors.SolarGold,
            topLeft = Offset(cx - w * 0.035f, beltY - h * 0.02f),
            size = Size(w * 0.07f, h * 0.04f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 6. Sculpted 3D Dark Knight Cowl (Sharp Pointed Ears, Jawline Armor, Luminous Narrow Eyes)
        val headTop = cy - h * 0.40f
        val headBot = cy - h * 0.17f
        val cowlPath = Path().apply {
            // Left pointed cowl ear
            moveTo(cx - w * 0.125f, headTop + h * 0.055f)
            lineTo(cx - w * 0.145f, headTop - h * 0.085f)
            lineTo(cx - w * 0.075f, headTop + h * 0.015f)
            // Domed crown
            lineTo(cx, headTop - h * 0.01f)
            lineTo(cx + w * 0.075f, headTop + h * 0.015f)
            // Right pointed cowl ear
            lineTo(cx + w * 0.145f, headTop - h * 0.085f)
            lineTo(cx + w * 0.125f, headTop + h * 0.055f)
            // Sculpted cheekbones & angular chin guard
            lineTo(cx + w * 0.12f, headBot - h * 0.045f)
            lineTo(cx + w * 0.06f, headBot + h * 0.015f)
            lineTo(cx, headBot + h * 0.03f)
            lineTo(cx - w * 0.06f, headBot + h * 0.015f)
            lineTo(cx - w * 0.12f, headBot - h * 0.045f)
            close()
        }
        drawPath(
            cowlPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF111827), Color(0xFF05070B)),
                start = Offset(cx - w * 0.14f, headTop),
                end = Offset(cx + w * 0.14f, headBot)
            )
        )
        // Chin shadow & brow highlight
        val jawPlate = Path().apply {
            moveTo(cx - w * 0.065f, headBot - h * 0.045f)
            lineTo(cx + w * 0.065f, headBot - h * 0.045f)
            lineTo(cx + w * 0.045f, headBot + h * 0.01f)
            lineTo(cx, headBot + h * 0.022f)
            lineTo(cx - w * 0.045f, headBot + h * 0.01f)
            close()
        }
        drawPath(jawPlate, color = Color(0xFF1E293B))
        drawPath(
            cowlPath,
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), primary)),
            style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round)
        )

        // Sharp Illuminated Tactical Eyes
        val eyeY = (headTop + headBot) * 0.50f
        val eyeColor = if (tier >= 2) Color.White else AppColors.CyberCyan
        drawLine(
            color = eyeColor,
            start = Offset(cx - w * 0.085f, eyeY - h * 0.012f),
            end = Offset(cx - w * 0.025f, eyeY + h * 0.008f),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = eyeColor,
            start = Offset(cx + w * 0.085f, eyeY - h * 0.012f),
            end = Offset(cx + w * 0.025f, eyeY + h * 0.008f),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 7. Armored Forearm Gauntlets & Scalloped Blades (Stage 4+)
        if (tier >= 4) {
            val leftGauntlet = Offset(cx - shoulderHalf * 1.02f, cy + h * 0.14f)
            val rightGauntlet = Offset(cx + shoulderHalf * 1.02f, cy + h * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, primary, Color.Transparent), center = leftGauntlet, radius = w * 0.095f),
                radius = w * 0.095f,
                center = leftGauntlet
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, secondary, Color.Transparent), center = rightGauntlet, radius = w * 0.095f),
                radius = w * 0.095f,
                center = rightGauntlet
            )
            // Tactical Gauntlet Fins
            for (f in 0..1) {
                val dy = f * h * 0.045f
                drawLine(
                    color = primary,
                    start = Offset(leftGauntlet.x - w * 0.02f, leftGauntlet.y - dy),
                    end = Offset(leftGauntlet.x - w * 0.075f, leftGauntlet.y - dy - h * 0.03f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondary,
                    start = Offset(rightGauntlet.x + w * 0.02f, rightGauntlet.y - dy),
                    end = Offset(rightGauntlet.x + w * 0.075f, rightGauntlet.y - dy - h * 0.03f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun SentinelHeroCard(
    vm: HabitTrackerViewModel,
    onOpenSentinelSheet: () -> Unit
) {
    val context = LocalContext.current
    val stats = vm.cachedCharacterStats.value
    val stageColor = Color(stats.stage.primaryColorHex)
    val stageProg by animateFloatAsState(
        targetValue = stats.stageProgressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "armorProg"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(stageColor.copy(alpha = 0.65f), AppColors.BorderSubtle, AppColors.BorderHighlight)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .studioBounceClick(onOpenSentinelSheet)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DarkKnight3DCanvas(
                    stage = stats.stage,
                    animateStudioLight = false,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(stageColor.copy(alpha = 0.16f), AppColors.BgDeep)
                            )
                        )
                        .border(1.dp, stageColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = stageColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = "${stats.stage.formName.uppercase()} • ${stats.stage.title.uppercase()}",
                                color = stageColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = if (stats.comboMultiplier > 1f) "${stats.comboMultiplier}x" else "${stats.combatPower} CP",
                            color = AppColors.SolarGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = stats.stage.suitName,
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // Step-by-Step Armor Evolution Progress Bar
                    LinearProgressIndicator(
                        progress = { stageProg },
                        color = stageColor,
                        trackColor = AppColors.BgCardElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stats.nextUpgradeRequirementText,
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CharacterMiniStatPill("STR", stats.strength, AppColors.CrimsonRose)
                        CharacterMiniStatPill("INT", stats.intellect, AppColors.CyberCyan)
                        CharacterMiniStatPill("WIL", stats.willpower, AppColors.SolarGold)
                        CharacterMiniStatPill("VIT", stats.vitality, AppColors.MintGlow)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // High-Agency Motivation Bar
            Surface(
                color = AppColors.BgSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stats.heroSpeechBubble,
                        color = AppColors.Platinum,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { vm.triggerNewHeroMotivation(context) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(stageColor.copy(alpha = 0.18f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, "New Quote", tint = stageColor, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterMiniStatPill(label: String, value: Int, color: Color) {
    Surface(
        color = AppColors.BgCardElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = "$label $value",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * DARK KNIGHT ARMORY MODAL (NO SHORTCUTS — PURE EARNED PROGRESSION)
 * Tap any stage in the roadmap to inspect its 3D armor preview.
 */
@Composable
fun DarkKnightArmoryModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val stats = vm.cachedCharacterStats.value
    var previewStage by remember { mutableStateOf(stats.stage) }
    val previewColor = Color(previewStage.primaryColorHex)
    val isPreviewUnlocked = stats.stage.stageNumber >= previewStage.stageNumber

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.2.dp, previewColor.copy(alpha = 0.75f)),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SENTINEL ARMORY",
                            color = AppColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Earned strictly through daily discipline • Zero shortcuts",
                            color = AppColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Interactive 3D Stage Showcase with Studio Floating Animation
                Box(contentAlignment = Alignment.TopEnd) {
                    DarkKnight3DCanvas(
                        stage = previewStage,
                        animateStudioLight = true,
                        modifier = Modifier
                            .size(205.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(previewColor.copy(alpha = 0.22f), AppColors.BgCard, AppColors.BgDeep)
                                )
                            )
                            .border(1.5.dp, previewColor.copy(alpha = 0.7f), RoundedCornerShape(30.dp))
                    )
                    Surface(
                        color = if (isPreviewUnlocked) AppColors.MintGlow else AppColors.BgCardElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = if (isPreviewUnlocked) "UNLOCKED" else "PREVIEW 🔒",
                            color = if (isPreviewUnlocked) Color.Black else AppColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "${previewStage.title} (${previewStage.formName})",
                    color = previewColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${previewStage.suitName} • ${previewStage.armorUpgradeDetail}",
                    color = AppColors.Platinum,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stats.nextUpgradeRequirementText,
                    color = AppColors.SolarGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // Earned Discipline Attributes (Read-Only — No Boost Shortcut Buttons)
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("EARNED DISCIPLINE PROFILE", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("CP ${stats.combatPower}", color = AppColors.SolarGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        EarnedStatProgressBar("💪 Strength (Fitness & Health Habits)", stats.strength, AppColors.CrimsonRose)
                        EarnedStatProgressBar("🧠 Intellect (Goals, Focus & Vault)", stats.intellect, AppColors.CyberCyan)
                        EarnedStatProgressBar("🔥 Willpower (Streaks & Missions)", stats.willpower, AppColors.SolarGold)
                        EarnedStatProgressBar("🌿 Vitality (Journal & Consistency)", stats.vitality, AppColors.MintGlow)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 6-Stage Interactive Step-by-Step Armor Roadmap
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("STEP-BY-STEP ARMOR EVOLUTION (TAP TO PREVIEW)", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        CharacterStage.entries.forEach { st ->
                            val unlocked = stats.stage.stageNumber >= st.stageNumber
                            val isSelected = previewStage == st
                            val stColor = Color(st.primaryColorHex)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> stColor.copy(alpha = 0.22f)
                                            unlocked -> stColor.copy(alpha = 0.08f)
                                            else -> AppColors.BgSurface
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) stColor else AppColors.BorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { previewStage = st }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${st.formName}: ${st.title}",
                                        color = if (unlocked || isSelected) stColor else AppColors.TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${st.armorUpgradeDetail} • Req: ${st.minXpRequired} XP & ${st.minStreakRequired}d streak",
                                        color = AppColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (unlocked) Icons.Default.Verified else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (unlocked) AppColors.MintGlow else AppColors.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarnedStatProgressBar(label: String, statValue: Int, color: Color) {
    val ratio by animateFloatAsState(
        targetValue = (statValue / 240f).coerceIn(0.08f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statBar"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AppColors.TextSecondary, fontSize = 12.sp)
            Text("$statValue", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = AppColors.BgCardElevated,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

// ============================================================================
// NEW SECTION (TAB 1): GOAL ACHIEVEMENT & DAILY WORK MONITOR
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
        // 1. Studio Goals Command Summary Card
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
                            Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Goal", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuditStatPill(Modifier.weight(1f), "Active", "$activeGoalsCount", AppColors.CyberCyan)
                        AuditStatPill(Modifier.weight(1f), "Worked Today", "$workedTodayCount/${vm.goals.size}", AppColors.MintGlow)
                        AuditStatPill(Modifier.weight(1f), "Invested", "${totalMinutesAllGoals / 60}h ${totalMinutesAllGoals % 60}m", AppColors.SolarGold)
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
                        Text("Set Your First Measurable Goal", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Define a deadline and log your daily work progress step by step.", color = AppColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
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

        // 2. Interactive Goal Cards with Expandable Daily Work Log History
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
            // Top Row: Emoji, Title, Deadline Countdown + Circular Gauge
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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

                // Animated Circular Completion Ring
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

            // Progress Bar + Required Daily Pace Info
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

            // Action Row: Log Today's Work + History Toggle + Edit/Delete
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
                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (workedToday) "Log More Work ✓" else "Log Today's Work",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = AppColors.BgCardElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { expandedLogs = !expandedLogs }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Logs (${goal.dailyLogs.size})", color = AppColors.Platinum, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

            // Expandable Daily Work Timeline
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
                        Text("No daily work logged yet. Tap 'Log Today's Work' to start.", color = AppColors.TextMuted, fontSize = 12.sp)
                    } else {
                        goal.dailyLogs.take(10).forEach { wl ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wl.workSummary, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${wl.date} • +${wl.progressDelta.cleanStr()} ${goal.unit} • ${wl.minutesSpent}m",
                                        color = goal.accentColor,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { onDeleteWorkLog(wl.id) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Close, "Remove Log", tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioCreateOrEditGoalDialog(
    goalToEdit: GoalItem?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, String, Float, String, Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var emoji by remember { mutableStateOf(goalToEdit?.emoji ?: "🎯") }
    var category by remember { mutableStateOf(goalToEdit?.category ?: "Career") }
    var description by remember { mutableStateOf(goalToEdit?.description ?: "") }
    var deadlineIso by remember { mutableStateOf(goalToEdit?.deadlineIso ?: LocalDate.now().plusDays(30).toString()) }
    var target by remember { mutableStateOf(goalToEdit?.targetValue?.cleanStr() ?: "100") }
    var unit by remember { mutableStateOf(goalToEdit?.unit ?: "hrs") }
    var selectedColor by remember { mutableLongStateOf(goalToEdit?.colorHex ?: 0xFF38BDF8L) }

    val colorChoices = listOf(0xFF38BDF8L, 0xFF34D399L, 0xFFA855F7L, 0xFFF97316L, 0xFFFBBF24L, 0xFFF43F5EL)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text(if (goalToEdit == null) "Set New Goal" else "Edit Goal", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Icon") }, modifier = Modifier.width(70.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Why this goal matters / Plan") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = {
                        val parsed = runCatching { LocalDate.parse(deadlineIso) }.getOrDefault(LocalDate.now().plusDays(30))
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> deadlineIso = LocalDate.of(y, m + 1, d).toString() },
                            parsed.year,
                            parsed.monthValue - 1,
                            parsed.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.CyberCyan)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deadline: $deadlineIso", color = AppColors.CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (hrs, %, km)") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Career, Fitness, Study)") }, modifier = Modifier.fillMaxWidth())

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
                        onSave(goalToEdit?.id, title, emoji, category, description, deadlineIso, target.toFloatOrNull() ?: 100f, unit, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Goal", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LogDailyGoalWorkDialog(
    goal: GoalItem,
    onDismiss: () -> Unit,
    onSave: (String, Float, Int, LocalDate) -> Unit
) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf("") }
    var deltaText by remember { mutableStateOf("1") }
    var minsText by remember { mutableStateOf("45") }
    var logDate by remember { mutableStateOf(LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("${goal.emoji} Log Daily Work", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(goal.title, color = goal.accentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> logDate = LocalDate.of(y, m + 1, d) },
                            logDate.year,
                            logDate.monthValue - 1,
                            logDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.BorderHighlight)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = AppColors.Platinum, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Work Date: $logDate", color = AppColors.Platinum, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("What concrete work did you finish?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("Progress (+${goal.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(summary, deltaText.toFloatOrNull() ?: 1f, minsText.toIntOrNull() ?: 30, logDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = goal.accentColor)
            ) {
                Text("Log Progress", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================================================
// UNIFIED SANCTUARY HUB (TAB 4): APPLE-STYLE SEGMENTED SWITCHER (JOURNAL | VAULT)
// ============================================================================
@Composable
fun StudioSanctuaryHubSection(vm: HabitTrackerViewModel) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Journal, 1 = Vault
    val theme = vm.readingTheme.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgColor)
    ) {
        // Frosted Segmented Control Pill
        Surface(
            color = theme.cardColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.8.dp, theme.borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
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

// ============================================================================
// 5-TAB TITANIUM BOTTOM DOCK (Habits, Goals, Tasks, Audit, Sanctuary)
// ============================================================================
@Composable
fun StudioTitaniumBottomDock(currentTab: Int, onSelectTab: (Int) -> Unit) {
    Surface(
        color = AppColors.BgSurface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(0, "Habits", Icons.Default.GridOn),
                Triple(1, "Goals", Icons.Default.Flag),
                Triple(2, "Tasks", Icons.Default.CheckCircle),
                Triple(3, "Audit", Icons.Default.Schedule),
                Triple(4, "Sanctuary", Icons.Default.AutoAwesome)
            )

            items.forEach { (index, title, icon) ->
                val selected = currentTab == index
                val pillBg by animateColorAsState(if (selected) AppColors.MintGlow.copy(alpha = 0.14f) else Color.Transparent, label = "dockBg")
                val tint by animateColorAsState(if (selected) AppColors.MintGlow else AppColors.TextSecondary, label = "dockTint")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(pillBg)
                        .studioBounceClick { onSelectTab(index) }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(title, color = tint, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp)
                }
            }
        }
    }
}

// ============================================================================
// NOTIFICATION & BATTERY SHIELD DIALOG
// ============================================================================
@SuppressLint("BatteryLife")
@Composable
fun NotificationAndBatteryShieldDialog(
    vm: HabitTrackerViewModel,
    onRequestNotifPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val health = vm.notificationHealth.value

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.2.dp, if (health.allHealthy) AppColors.MintGlow else AppColors.SolarGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️ Notification Shield", color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary) }
                }

                Text(
                    text = "Enable all 3 shields so Android Doze and battery savers never silence your habit or mission reminders while idle.",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                ShieldCheckItemRow("1. Notification Permission", if (health.notificationsGranted) "Allowed" else "Tap Fix to allow alerts", health.notificationsGranted) {
                    onRequestNotifPermission()
                    try {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    } catch (_: Exception) {
                    }
                }

                ShieldCheckItemRow("2. Exact Doze-Proof Alarms", if (health.exactAlarmsGranted) "AlarmClock priority active" else "Tap Fix to allow exact alarms", health.exactAlarmsGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:${context.packageName}")))
                        } catch (_: Exception) {
                        }
                    }
                }

                ShieldCheckItemRow("3. Unrestricted Battery", if (health.batteryUnrestricted) "Protected from OS sleep" else "Tap Fix & allow Unrestricted", health.batteryUnrestricted) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${context.packageName}")))
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }
                }

                Surface(color = AppColors.BgCardElevated, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Armed Habit & Mission Alarms:", color = AppColors.TextSecondary, fontSize = 12.sp)
                        Text("${health.scheduledRemindersCount} Active", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val armed = ReminderScheduler.rescheduleAllSavedAlarms(context)
                            vm.refreshNotificationHealth(context)
                            Toast.makeText(context, "🔄 Re-armed $armed alarm(s)!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppColors.CyberCyan)
                    ) {
                        Text("Re-Arm All", color = AppColors.CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            ReminderScheduler.scheduleTestNotificationInSeconds(context, 5)
                            Toast.makeText(context, "🔔 Test alarm set for 5s! Lock your screen to test.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
                    ) {
                        Text("5s Test Alarm", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShieldCheckItemRow(title: String, subtitle: String, isOk: Boolean, onFix: () -> Unit) {
    Surface(
        color = AppColors.BgCardElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, if (isOk) AppColors.MintGlow.copy(alpha = 0.45f) else AppColors.SolarGold.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (isOk) AppColors.MintGlow else AppColors.SolarGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(subtitle, color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
            if (!isOk) {
                Button(onClick = onFix, colors = ButtonDefaults.buttonColors(containerColor = AppColors.SolarGold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Fix", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            } else {
                Text("OK", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

// ============================================================================
// DAILY QUESTS, ACTIVE FOCUS BANNER & SECTION 1 (HABITS)
// ============================================================================
@Composable
fun DailyBonusQuestsRow(quests: List<DailyQuestStatus>, onClaimQuest: (DailyQuestStatus) -> Unit) {
    if (quests.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("DAILY BONUS QUESTS", color = AppColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 2.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(quests, key = { it.id }) { q ->
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, if (!q.isClaimed && q.isCompleted) AppColors.MintGlow else AppColors.BorderSubtle),
                    modifier = Modifier.width(174.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${q.emoji} ${q.title}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${q.current.coerceAtMost(q.target)}/${q.target}", color = AppColors.MintGlow, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(q.description, color = AppColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (q.current.toFloat() / q.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            color = if (q.isCompleted) AppColors.MintGlow else AppColors.CyberCyan,
                            trackColor = AppColors.BgCardElevated,
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            q.isClaimed -> Text("✓ Claimed", color = AppColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            q.isCompleted -> {
                                Surface(
                                    color = AppColors.MintGlow,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().studioBounceClick { onClaimQuest(q) }
                                ) {
                                    Text(
                                        text = "Claim +${q.rewardXp} XP${if (q.grantsShield) " +🛡️" else ""}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                            else -> Text("Reward: +${q.rewardXp} XP${if (q.grantsShield) " +🛡️" else ""}", color = AppColors.SolarGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveFocusTimerBanner(vm: HabitTrackerViewModel, onExpand: () -> Unit) {
    val context = LocalContext.current
    val rem = vm.focusRemainingSeconds.longValue
    val timeFormatted = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)

    Surface(
        color = AppColors.CyberCyan.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.CyberCyan),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).studioBounceClick(onExpand)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Timer, null, tint = AppColors.CyberCyan)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(vm.activeFocusTitle.value, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Focus session active • Tap to expand", color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(timeFormatted, color = AppColors.CyberCyan, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { vm.togglePauseFocusTimer(context) }, modifier = Modifier.size(28.dp)) {
                    Icon(if (vm.isFocusTimerRunning.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = AppColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
fun StudioHabitTrackerSection(
    vm: HabitTrackerViewModel,
    onOpenSentinelSheet: () -> Unit,
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
        item(key = "sentinel_hero") {
            SentinelHeroCard(vm = vm, onOpenSentinelSheet = onOpenSentinelSheet)
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TimeOfDay.entries) { tod ->
                        val active = vm.selectedTimeOfDay.value == tod
                        Surface(
                            color = if (active) AppColors.MintGlow.copy(alpha = 0.16f) else AppColors.BgCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, if (active) AppColors.MintGlow else AppColors.BorderSubtle),
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
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.BgCardElevated)
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
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.MintGlow)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.Black, modifier = Modifier.size(20.dp))
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
    val today = remember { LocalDate.now() }
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val total = habits.size.coerceAtLeast(1)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
                        text = if (isToday) "TODAY" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase().take(3),
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
                        Text("${date.dayOfMonth}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(3.dp))
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
    val streak = remember(habit.logs, selectedDate) { habit.currentStreak() }

    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.9.dp, if (isDone) habit.accentColor.copy(alpha = 0.6f) else AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onEdit() }.padding(end = 10.dp)
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
                        Text(habit.title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (habit.habitStackingCue.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("🔗 ${habit.habitStackingCue}", color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Surface(color = AppColors.SolarGold.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                        Text("🔥 $streak d", color = AppColors.SolarGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                    if (habit.reminderTime.isNotBlank()) {
                        Surface(color = AppColors.CyberCyan.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text("🔔 ${habit.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                    if (habit.customSoundUri.isNotBlank()) {
                        Surface(color = AppColors.ElectricViolet.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                            Text("🎵", color = AppColors.ElectricViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (habit.type == HabitType.MEASUREMENT || habit.type == HabitType.FOCUS_TIMER) {
                        Surface(color = habit.accentColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text("${valueToday.cleanStr()}/${habit.targetValue.cleanStr()} ${habit.unit}", color = habit.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
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
fun GpuSixteenWeekHeatmapCanvas(habit: Habit) {
    val today = remember { LocalDate.now() }
    val accent = habit.accentColor
    val partialAccent = accent.copy(alpha = 0.45f)
    val emptyCol = AppColors.BgCardElevated

    val cellStates = remember(habit.logs, today) {
        IntArray(16 * 7) { index ->
            val w = 15 - (index / 7)
            val d = 6 - (index % 7)
            val cellDate = today.minusDays((w * 7 + d).toLong())
            when {
                habit.isCompletedOn(cellDate) -> 2
                habit.valueOn(cellDate) > 0f -> 1
                else -> 0
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(98.dp)) {
        val cols = 16
        val rows = 7
        val gap = 3.5.dp.toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * (rows - 1)) / rows
        val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val color = when (cellStates[c * rows + r]) {
                    2 -> accent
                    1 -> partialAccent
                    else -> emptyCol
                }
                drawRoundRect(color = color, topLeft = Offset(c * (cellW + gap), r * (cellH + gap)), size = Size(cellW, cellH), cornerRadius = radius)
            }
        }
    }
}

@Composable
fun SmallToolPillButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(AppColors.BgCardElevated)
            .studioBounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
fun StudioSevenDayInteractiveStrip(habit: Habit, onToggleDate: (LocalDate) -> Unit) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }

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
                        .border(width = if (isToday) 1.4.dp else 0.8.dp, color = if (isToday) Color.White else AppColors.BorderSubtle, shape = RoundedCornerShape(10.dp))
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

// ============================================================================
// SECTION 2 (TAB 2): TO-DO MATRIX WITH AUTOMATIC DAILY RENEWAL & CUSTOM SOUND
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
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mission Matrix", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tasks marked 🔄 Auto-Renew reset automatically each day", color = AppColors.MintGlow, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    editingTask = null
                    showAddTodo = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
                Spacer(Modifier.width(4.dp))
                Text("New Task", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedPriority == null, onClick = { selectedPriority = null }, label = { Text("All") }) }
                items(TodoPriority.entries) { prio -> FilterChip(selected = selectedPriority == prio, onClick = { selectedPriority = prio }, label = { Text(prio.shortTag) }) }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { vm.checkAndAutoRenewDailyTasks(context, forceRenewForTesting = true) },
                border = BorderStroke(1.dp, AppColors.CyberCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = AppColors.CyberCyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Renew Daily", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        border = BorderStroke(0.9.dp, if (task.isCompleted) AppColors.BorderSubtle else prioColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) AppColors.MintGlow else Color.Transparent)
                        .border(2.dp, if (task.isCompleted) AppColors.MintGlow else prioColor, CircleShape)
                        .studioBounceClick(onToggleMain),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
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

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(color = prioColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                        Text(task.priority.shortTag, color = prioColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (task.isRecurring) {
                        Surface(color = AppColors.MintGlow.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text("🔄 Daily • ${task.renewalStreak}d", color = AppColors.MintGlow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (task.reminderTime.isNotBlank()) {
                        Text("🔔 ${task.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (task.customSoundUri.isNotBlank()) {
                        Surface(color = AppColors.ElectricViolet.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text("🎵", color = AppColors.ElectricViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
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
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.BgCardElevated).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    task.subTasks.forEach { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggleSubTask(sub.id) }) {
                            Icon(if (sub.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null, tint = if (sub.isDone) AppColors.MintGlow else AppColors.TextSecondary, modifier = Modifier.size(17.dp))
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
// SECTION 3 (TAB 3): HOURLY DAY PRODUCTIVITY AUDIT (WITH PAST-DAY NAVIGATOR)
// ============================================================================
@Composable
fun StudioDayProductivityAuditSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    val selectedDate = vm.selectedDate.value
    val today = remember { LocalDate.now() }
    val dateStr = selectedDate.toString()
    val formattedDate = remember(selectedDate) { selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) }

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

    val peakHours = logsForDay.values.count { it.status == HourProductivityStatus.PEAK_FLOW || it.status == HourProductivityStatus.PRODUCTIVE }
    val wastedHours = logsForDay.values.count { it.status == HourProductivityStatus.TIME_WASTED || it.status == HourProductivityStatus.LOW_FOCUS }
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
            Surface(color = AppColors.BgCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(0.8.dp, AppColors.BorderSubtle), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { vm.selectedDate.value = selectedDate.minusDays(1) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Day", tint = AppColors.TextPrimary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth -> vm.selectedDate.value = LocalDate.of(year, month + 1, dayOfMonth) },
                                    selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = AppColors.CyberCyan, modifier = Modifier.size(18.dp))
                        Text(if (selectedDate == today) "Today ($formattedDate)" else formattedDate, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedDate != today) {
                            TextButton(onClick = { vm.selectedDate.value = today }) {
                                Text("Today", color = AppColors.MintGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { if (selectedDate.isBefore(today)) vm.selectedDate.value = selectedDate.plusDays(1) }, enabled = selectedDate.isBefore(today)) {
                            Icon(Icons.Default.ChevronRight, "Next Day", tint = if (selectedDate.isBefore(today)) AppColors.TextPrimary else AppColors.TextMuted)
                        }
                    }
                }
            }
        }

        item(key = "score_card") {
            Surface(color = AppColors.BgCard, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.2.dp, scoreColor.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PRODUCTIVITY REPORT • ${formattedDate.uppercase()}", color = scoreColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Grade: $grade", color = AppColors.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Log every hour honestly. Wasted hours deduct score & XP.", color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 6.5.dp.toPx()
                                drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                                drawArc(color = scoreColor, startAngle = -90f, sweepAngle = 360f * animatedDayScore, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round))
                            }
                            Text("$dayScore%", color = AppColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuditStatPill(Modifier.weight(1f), "🔥 Productive", "$peakHours hrs", AppColors.MintGlow)
                        AuditStatPill(Modifier.weight(1f), "💀 Wasted", "$wastedHours hrs", AppColors.CrimsonRose)
                        AuditStatPill(Modifier.weight(1f), "⚡ Net XP", if (netXpDay >= 0) "+$netXpDay XP" else "$netXpDay XP", if (netXpDay >= 0) AppColors.SolarGold else AppColors.CrimsonRose)
                    }
                }
            }
        }

        item(key = "ledger_header") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⏰ Hourly Activity Ledger (6 AM – 11 PM)", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Tap to log", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        val hoursList = (6..23).toList()
        items(hoursList, key = { "$dateStr-$it" }) { hour24 ->
            HourlySlotCard(
                hour24 = hour24,
                loggedItem = logsForDay[hour24],
                onClick = { editingHour24 = hour24 },
                onQuickPenalty = { vm.logOrUpdateHour(context, hour24, "Wasted time / Unproductive hour", HourProductivityStatus.TIME_WASTED, selectedDate) },
                onQuickFlow = { vm.logOrUpdateHour(context, hour24, "Focused productive work block", HourProductivityStatus.PEAK_FLOW, selectedDate) }
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
fun AuditStatPill(modifier: Modifier, title: String, value: String, color: Color) {
    Surface(modifier = modifier, color = AppColors.BgCardElevated, shape = RoundedCornerShape(13.dp), border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
    val timeLabel = remember(hour24) { LocalTime.of(hour24, 0).format(DateTimeFormatter.ofPattern("hh:00 a")) }
    val statusColor = loggedItem?.status?.color ?: AppColors.BorderSubtle

    Surface(
        color = if (loggedItem != null) AppColors.BgCard else AppColors.BgSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.9.dp, if (loggedItem != null) statusColor.copy(alpha = 0.55f) else AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(color = if (loggedItem != null) statusColor.copy(alpha = 0.16f) else AppColors.BgCardElevated, shape = RoundedCornerShape(10.dp)) {
                Text(timeLabel, color = if (loggedItem != null) statusColor else AppColors.TextSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (loggedItem != null) {
                    Text(loggedItem.activityNote, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("${loggedItem.status.label} (${loggedItem.status.shortBadge})", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Unlogged hour • Tap to record", color = AppColors.TextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.MintGlow.copy(alpha = 0.16f)).border(0.8.dp, AppColors.MintGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).studioBounceClick(onQuickFlow),
                    contentAlignment = Alignment.Center
                ) { Text("+⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.MintGlow) }
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.CrimsonRose.copy(alpha = 0.16f)).border(0.8.dp, AppColors.CrimsonRose.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).studioBounceClick(onQuickPenalty),
                    contentAlignment = Alignment.Center
                ) { Text("-💀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.CrimsonRose) }
            }
        }
    }
}

@Composable
fun LogHourActivityDialog(
    hour24: Int,
    existingLog: HourlyLogItem?,
    onDismiss: () -> Unit,
    onSave: (String, HourProductivityStatus) -> Unit,
    onDelete: () -> Unit
) {
    val start = LocalTime.of(hour24, 0)
    val rangeLabel = "${start.format(DateTimeFormatter.ofPattern("hh:00 a"))} - ${start.plusHours(1).format(DateTimeFormatter.ofPattern("hh:00 a"))}"
    var note by remember { mutableStateOf(existingLog?.activityNote ?: "") }
    var status by remember { mutableStateOf(existingLog?.status ?: HourProductivityStatus.PEAK_FLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text("⏰ Audit Hour: $rangeLabel", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("What did you do during this hour?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("Productivity Rating (Impacts Score & XP):", color = AppColors.TextSecondary, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HourProductivityStatus.entries.forEach { st ->
                        val selected = status == st
                        Surface(
                            color = if (selected) st.color.copy(alpha = 0.2f) else AppColors.BgCardElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) st.color else AppColors.BorderSubtle),
                            modifier = Modifier.fillMaxWidth().clickable { status = st }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(st.label, color = if (selected) st.color else AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(st.shortBadge, color = st.color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(note, status) }, colors = ButtonDefaults.buttonColors(containerColor = status.color)) {
                Text("Save Hour", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (existingLog != null) TextButton(onClick = onDelete) { Text("Clear", color = AppColors.CrimsonRose) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ============================================================================
// SANCTUARY JOURNAL & KNOWLEDGE VAULT SECTIONS
// ============================================================================
@Composable
fun StudioReflectionJournalSection(vm: HabitTrackerViewModel) {
    val context = LocalContext.current
    var showWriteJournal by remember { mutableStateOf(false) }
    var editingJournal by remember { mutableStateOf<JournalEntry?>(null) }
    val theme = vm.readingTheme.value
    val fontScale = vm.readingFontScale.floatValue

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sanctuary Journal", color = theme.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("Eye-soothing reflections & photo memories", color = theme.textSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    editingJournal = null
                    showWriteJournal = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Edit, null, tint = Color.Black, modifier = Modifier.size(16.dp))
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(SanctuaryReadingTheme.entries) { st ->
                val active = currentTheme == st
                Surface(
                    color = if (active) st.accent.copy(alpha = 0.22f) else st.cardColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, if (active) st.accent else st.borderColor),
                    modifier = Modifier.clickable { onSelectTheme(st) }
                ) {
                    Text(st.label, color = if (active) st.accent else st.textSecondary, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(currentTheme.cardColor).border(0.8.dp, currentTheme.borderColor, RoundedCornerShape(10.dp))
        ) {
            Text("A-", color = currentTheme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onChangeFontScale(fontScale - 0.1f) }.padding(horizontal = 8.dp, vertical = 5.dp))
            Text("A+", color = currentTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onChangeFontScale(fontScale + 0.1f) }.padding(horizontal = 8.dp, vertical = 5.dp))
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

    Surface(color = theme.cardColor, shape = RoundedCornerShape(24.dp), border = BorderStroke(0.8.dp, theme.borderColor), modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
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
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Edit, "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp)) }
                }
            }

            if (entry.photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entry.photoUris) { uriStr ->
                        AsyncImage(
                            model = Uri.parse(uriStr),
                            contentDescription = "Memory Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, theme.borderColor, RoundedCornerShape(16.dp))
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
                Text(entry.dailyStory, color = theme.textPrimary, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = FontFamily.Serif)
            }

            if (entry.frictionOrMistakes.isNotBlank()) {
                Surface(color = AppColors.CrimsonRose.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔍 REFLECTION ON FRICTION", color = AppColors.CrimsonRose, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
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
                        entry.actionItems.forEach { act -> Text("• $act", color = theme.textPrimary, fontSize = (13.5f * fontScale).sp) }
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
        vm.notes.filter { n ->
            val matchesCat = catFilter == null || n.category == catFilter
            val matchesQ = query.isBlank() || n.title.contains(query, true) || n.authorOrSubtitle.contains(query, true) || n.detailedNotes.contains(query, true)
            matchesCat && matchesQ
        }.sortedByDescending { it.isPinned }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Second-Brain Vault", color = theme.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("Notes, mental models & active recall", color = theme.textSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    editingNote = null
                    showCreateNote = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
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
            leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.textSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            item { FilterChip(selected = catFilter == null, onClick = { catFilter = null }, label = { Text("All (${vm.notes.size})") }) }
            items(NoteCategory.entries) { c -> FilterChip(selected = catFilter == c, onClick = { catFilter = c }, label = { Text(c.label) }) }
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

    Surface(color = theme.cardColor, shape = RoundedCornerShape(24.dp), border = BorderStroke(0.9.dp, accent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
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

                IconButton(onClick = onTogglePin) { Icon(Icons.Default.PushPin, "Pin", tint = if (note.isPinned) AppColors.SolarGold else theme.textSecondary.copy(alpha = 0.4f)) }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Edit, "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp)) }
            }

            if (note.favoriteQuote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = theme.bgColor, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("“${note.favoriteQuote}”", color = theme.accent, fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif, fontSize = bodyFontSize, lineHeight = bodyLineHeight, modifier = Modifier.padding(14.dp))
                }
            }

            if (note.keyTakeaways.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("💡 KEY TAKEAWAYS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                note.keyTakeaways.forEach { bullet ->
                    Text("• $bullet", color = theme.textPrimary, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = FontFamily.Serif, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            if (note.detailedNotes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(note.detailedNotes, color = theme.textSecondary, fontSize = bodyFontSize, lineHeight = bodyLineHeight, fontFamily = FontFamily.Serif)
            }

            if (note.flashcards.isNotEmpty()) {
                val fc = note.flashcards.first()
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.9.dp, accent.copy(alpha = 0.4f)),
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
// CUSTOM PHONE AUDIO PICKER & DIALOGS
// ============================================================================
@Composable
fun CustomAudioPickerRow(customSoundUri: String, onSoundSelected: (String) -> Unit) {
    val context = LocalContext.current
    val fileName = remember(customSoundUri) { CelebrationSoundEngine.getAudioFileName(context, customSoundUri) }

    val audioPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            onSoundSelected(it.toString())
        }
    }

    Surface(color = AppColors.ElectricViolet.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), border = BorderStroke(0.9.dp, AppColors.ElectricViolet.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎵 Custom Sound Effect / Ringtone", color = AppColors.ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(fileName, color = AppColors.TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (customSoundUri.isNotBlank()) {
                    IconButton(onClick = { CelebrationSoundEngine.stopActiveCustomSound(); onSoundSelected("") }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, "Reset", tint = AppColors.CrimsonRose, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AppColors.ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = AppColors.ElectricViolet, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import from Phone", color = AppColors.ElectricViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { CelebrationSoundEngine.playCustomOrDefaultSound(context, customSoundUri, isReminder = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("▶️ Test", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudioCreateOrEditHabitDialog(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, TimeOfDay, String, HabitType, Long, Float, String, Float, String, String) -> Unit
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
    var customSoundUri by remember { mutableStateOf(habitToEdit?.customSoundUri ?: "") }
    var selectedColor by remember { mutableLongStateOf(habitToEdit?.colorHex ?: 0xFF34D399L) }

    val context = LocalContext.current
    val colorChoices = listOf(0xFF34D399L, 0xFFA855F7L, 0xFF38BDF8L, 0xFFF97316L, 0xFFF59E0BL, 0xFFF43F5EL)

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

                OutlinedButton(
                    onClick = {
                        val initH = reminder.split(":").getOrNull(0)?.toIntOrNull() ?: 8
                        val initM = reminder.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, h, m -> reminder = String.format(Locale.US, "%02d:%02d", h, m) }, initH, initM, true).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.CyberCyan)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (reminder.isBlank()) "Set Daily Reminder Time" else "🔔 Reminder Set: $reminder", color = AppColors.CyberCyan, fontSize = 12.sp)
                }

                CustomAudioPickerRow(customSoundUri = customSoundUri, onSoundSelected = { customSoundUri = it })

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HabitType.entries) { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label, fontSize = 11.sp) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TimeOfDay.entries) { tod -> FilterChip(selected = timeOfDay == tod, onClick = { timeOfDay = tod }, label = { Text("${tod.icon} ${tod.label}", fontSize = 11.sp) }) }
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
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(hex))
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
                        onSave(habitToEdit?.id, title, emoji, category, timeOfDay, cue, type, selectedColor, target.toFloatOrNull() ?: 1f, unit, step.toFloatOrNull() ?: 1f, reminder, customSoundUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) { Text("Save Habit", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudioAddOrEditTodoDialog(
    taskToEdit: TodoTask?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, TodoPriority, String, Int, List<String>, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Work") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TodoPriority.P1_CRITICAL) }
    var reminder by remember { mutableStateOf(taskToEdit?.reminderTime ?: "") }
    var customSoundUri by remember { mutableStateOf(taskToEdit?.customSoundUri ?: "") }
    var mins by remember { mutableStateOf(taskToEdit?.estimatedMinutes?.toString() ?: "25") }
    var subtasksRaw by remember { mutableStateOf(taskToEdit?.subTasks?.joinToString("\n") { it.title } ?: "") }
    var recurring by remember { mutableStateOf(taskToEdit?.isRecurring ?: true) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = { Text(if (taskToEdit == null) "Add Mission" else "Edit Mission", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mission Title") })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TodoPriority.entries) { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label, fontSize = 11.sp) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            val initH = reminder.split(":").getOrNull(0)?.toIntOrNull() ?: 9
                            val initM = reminder.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h, m -> reminder = String.format(Locale.US, "%02d:%02d", h, m) }, initH, initM, true).show()
                        },
                        border = BorderStroke(1.dp, AppColors.CyberCyan)
                    ) { Text(if (reminder.isBlank()) "🔔 Set Time" else "🔔 $reminder", color = AppColors.CyberCyan, fontSize = 12.sp) }
                }

                CustomAudioPickerRow(customSoundUri = customSoundUri, onSoundSelected = { customSoundUri = it })

                Surface(color = AppColors.MintGlow.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), border = BorderStroke(0.9.dp, AppColors.MintGlow.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔄 Auto-Renew Every Day", color = AppColors.MintGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Automatically resets tomorrow after completion", color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = recurring, onCheckedChange = { recurring = it })
                    }
                }

                OutlinedTextField(value = subtasksRaw, onValueChange = { subtasksRaw = it }, label = { Text("Subtasks (one per line)") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(taskToEdit?.id, title, category, priority, reminder, mins.toIntOrNull() ?: 25, subtasksRaw.lines(), recurring, customSoundUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) { Text("Save Mission", color = Color.Black, fontWeight = FontWeight.Bold) }
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
        Surface(color = AppColors.BgCard, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (entryToEdit == null) "Log Sanctuary Reflection" else "Edit Journal Entry", color = AppColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🔥", "🌟", "😊", "🧘", "💪", "🌧️").forEach { e -> FilterChip(selected = mood == e, onClick = { mood = e }, label = { Text(e, fontSize = 16.sp) }) }
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
                    Icon(Icons.Default.Add, null, tint = AppColors.ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Memory Photos (${photos.size} attached)", color = AppColors.ElectricViolet)
                }

                OutlinedTextField(value = mistakes, onValueChange = { mistakes = it }, label = { Text("What went wrong / Friction?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
                    ) { Text("Save Entry", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun StudioCreateOrEditNoteDialog(noteToEdit: NotebookEntry?, onDismiss: () -> Unit, onSave: (NotebookEntry) -> Unit) {
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
                    items(NoteCategory.entries) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label, fontSize = 11.sp) }) }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author / Topic") }, modifier = Modifier.weight(1f))
                    if (category == NoteCategory.BOOK_SUMMARY) {
                        OutlinedTextField(value = progress, onValueChange = { progress = it }, label = { Text("% Read") }, modifier = Modifier.width(80.dp))
                    }
                }
                OutlinedTextField(value = quote, onValueChange = { quote = it }, label = { Text("Key Principle / Quote") })
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
            ) { Text("Save Note", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FocusTimerStudioDialog(vm: HabitTrackerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableIntStateOf(0) }

    val rem = vm.focusRemainingSeconds.longValue
    val tot = vm.focusTotalSeconds.longValue.coerceAtLeast(1L)
    val progress = (rem.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
    val formatted = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)

    var breathTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(mode) {
        if (mode == 1) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                breathTick = (breathTick + 1) % 16
            }
        }
    }
    val breathPhase = when (breathTick / 4) {
        0 -> "INHALE SLOWLY (4s)"
        1 -> "HOLD FULL (4s)"
        2 -> "EXHALE GENTLY (4s)"
        else -> "HOLD EMPTY (4s)"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = AppColors.BgCard, shape = RoundedCornerShape(28.dp), border = BorderStroke(1.2.dp, AppColors.CyberCyan), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("⏱️ Focus Timer") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("🌬️ Box Breathing") })
                }

                Spacer(Modifier.height(14.dp))

                if (mode == 0) {
                    Text(vm.activeFocusTitle.value, color = AppColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(18.dp))

                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
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
                            Text(formatted, color = AppColors.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (vm.isFocusTimerRunning.value) "FLOW ACTIVE" else "READY", color = AppColors.MintGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = (tot / 60L) == mins.toLong(),
                                onClick = { vm.startFocusTimer(context, vm.activeFocusTitle.value, mins, vm.activeFocusHabitId.value) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.stopFocusTimer() }) { Text("Reset") }
                        Button(onClick = { vm.togglePauseFocusTimer(context) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.CyberCyan)) {
                            Text(if (vm.isFocusTimerRunning.value) "Pause" else "Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Nervous System Reset (4-4-4-4)", color = AppColors.MintGlow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier.size(160.dp).clip(CircleShape).background(AppColors.CyberCyan.copy(alpha = 0.16f)).border(2.dp, AppColors.CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${4 - (breathTick % 4)}s", color = AppColors.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Text(breathPhase, color = AppColors.CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Synchronize your breath before starting deep work.", color = AppColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StudioAnalyticsModal(vm: HabitTrackerViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = AppColors.BgDeep, shape = RoundedCornerShape(28.dp), border = BorderStroke(0.9.dp, AppColors.BorderSubtle), modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📊 Performance Analytics", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary) }
                }

                val best = remember(vm.habits.toList()) { vm.habits.maxOfOrNull { it.bestStreak() } ?: 0 }
                val avg = remember(vm.habits.toList()) { if (vm.habits.isEmpty()) 0 else vm.habits.map { it.completionRate30Days() }.average().roundToInt() }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), "🔥", "$best d", "Peak Streak", AppColors.SunsetCoral)
                    StatBox(Modifier.weight(1f), "📈", "$avg%", "30d Rate", AppColors.MintGlow)
                    StatBox(Modifier.weight(1f), "🎯", "${vm.goals.count { it.isCompleted }}/${vm.goals.size}", "Goals Hit", AppColors.CyberCyan)
                }

                vm.habits.forEach { habit ->
                    Surface(color = AppColors.BgCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(0.8.dp, AppColors.BorderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${habit.emoji} ${habit.title}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${habit.completionRate30Days()}% (30d)", color = habit.accentColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            GpuSixteenWeekHeatmapCanvas(habit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier, icon: String, value: String, label: String, accent: Color) {
    Surface(modifier = modifier, color = AppColors.BgCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(0.8.dp, accent.copy(alpha = 0.4f))) {
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
            val json = vm.buildBackupJsonNow(context)
            val ok = storage.exportBackupToUri(it, json)
            Toast.makeText(context, if (ok) "Backup saved to Google Drive!" else "Export failed", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = storage.readBackupFromUri(it)
            if (content != null && vm.restoreFromJsonString(content)) {
                vm.persistStateAsync(context)
                ReminderScheduler.rescheduleAllSavedAlarms(context)
                Toast.makeText(context, "Backup restored & alarms re-armed!", Toast.LENGTH_LONG).show()
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
                Text("All habits, goals, daily work logs, custom sounds, Sentinel progression, audits, tasks, and notes are securely backed up.", color = AppColors.TextSecondary, fontSize = 13.sp)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Last Sync: ${vm.lastCloudBackup.value}", color = AppColors.MintGlow, fontSize = 12.sp)

                OutlinedButton(onClick = { exportLauncher.launch("AlphaStudio_v6_Backup.json") }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.MintGlow)) {
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
            ) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private data class StudioParticle(val vx: Float, val vy: Float, val color: Color, val size: Float)

@Composable
fun StudioFullScreenCelebrationOverlay(
    stage: CharacterStage,
    title: String,
    subtitle: String,
    xpEarned: Int,
    level: Int,
    rank: String,
    onDismiss: () -> Unit
) {
    val particles = remember {
        List(50) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val speed = Random.nextFloat() * 880f + 220f
            StudioParticle(
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed - 300f).toFloat(),
                color = AppColors.StudioPalette.random(),
                size = Random.nextFloat() * 8f + 4f
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(2000, easing = LinearOutSlowInEasing)) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.80f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = anim.value
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f

            drawCircle(
                color = Color(stage.primaryColorHex).copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.72f * t,
                center = Offset(cx, cy),
                style = Stroke(width = 4.dp.toPx())
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
            border = BorderStroke(1.5.dp, Color(stage.primaryColorHex)),
            modifier = Modifier.padding(28.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DarkKnight3DCanvas(
                    stage = stage,
                    animateStudioLight = false,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(10.dp))
                Surface(color = AppColors.MintGlow.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                    Text("+$xpEarned XP  •  Lv.$level $rank", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = AppColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = AppColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow), shape = RoundedCornerShape(14.dp)) {
                    Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
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
