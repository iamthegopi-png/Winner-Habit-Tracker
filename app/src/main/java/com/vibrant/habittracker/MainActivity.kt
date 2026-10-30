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
                AlphaSuperheroStudioApp()
            }
        }
    }
}

// ============================================================================
// MAIN 5-SECTION SUPERHERO STUDIO SCAFFOLD
// ============================================================================
@Composable
fun AlphaSuperheroStudioApp(vm: HabitTrackerViewModel = viewModel()) {
    val context = LocalContext.current

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

    var currentTab by remember { mutableIntStateOf(0) } // 0=Habits, 1=Tasks, 2=Day Audit, 3=Journal, 4=Vault
    var showSuperheroModal by remember { mutableStateOf(false) }
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
                    colors = listOf(Color(0xFF0A0D18), AppColors.BgDeep, Color(0xFF05070C))
                )
            )
            .systemBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AlphaStudioTopBar(
                    vm = vm,
                    onOpenSuperheroSheet = { showSuperheroModal = true },
                    onOpenTimer = { showFocusTimerModal = true },
                    onOpenAnalytics = { showAnalyticsSheet = true },
                    onOpenCloud = { showCloudVaultModal = true }
                )
            },
            bottomBar = {
                StudioFiveTabBottomDock(
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
                    label = "FiveTabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> StudioHabitTrackerSection(
                            vm = vm,
                            onOpenSuperheroSheet = { showSuperheroModal = true },
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
                        2 -> StudioDayProductivityAuditSection(
                            vm = vm,
                            onOpenSuperheroSheet = { showSuperheroModal = true }
                        )
                        3 -> StudioReflectionJournalSection(vm = vm)
                        else -> StudioSecondBrainNotebookSection(vm = vm)
                    }
                }
            }
        }

        if (showSuperheroModal) {
            AlphaSuperheroHeadquartersModal(
                vm = vm,
                onDismiss = { showSuperheroModal = false }
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
// HIGH-ENERGY "ALPHA (α)" APEX LOGO & TOP BAR
// ============================================================================
@Composable
fun AlphaApexBrandLogo(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A0B1E), Color(0xFF0B1324), Color(0xFF28091A))
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(AppColors.CrimsonRose, AppColors.SolarGold, AppColors.CyberCyan)),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        val w = size.width
        val h = size.height

        val hex = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.84f, h * 0.30f)
            lineTo(w * 0.84f, h * 0.70f)
            lineTo(w * 0.5f, h * 0.88f)
            lineTo(w * 0.16f, h * 0.70f)
            lineTo(w * 0.16f, h * 0.30f)
            close()
        }
        drawPath(hex, color = AppColors.CrimsonRose.copy(alpha = 0.22f))
        drawPath(
            hex,
            brush = Brush.linearGradient(listOf(AppColors.CrimsonRose, AppColors.SolarGold)),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )

        val alphaFrame = Path().apply {
            moveTo(w * 0.30f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.70f, h * 0.72f)
        }
        drawPath(
            alphaFrame,
            color = Color.White,
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val bolt = Path().apply {
            moveTo(w * 0.26f, h * 0.56f)
            lineTo(w * 0.52f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.74f, h * 0.50f)
        }
        drawPath(
            bolt,
            color = AppColors.MintGlow,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun AlphaStudioTopBar(
    vm: HabitTrackerViewModel,
    onOpenSuperheroSheet: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val xpProg by animateFloatAsState(
        targetValue = vm.xpProgressInLevel,
        animationSpec = tween(550),
        label = "xpAnim"
    )
    val charStats = vm.getCharacterStats()
    val dayScore = vm.getDailyProductivityScore()

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
                .clickable { onOpenSuperheroSheet() }
                .padding(end = 6.dp)
        ) {
            AlphaApexBrandLogo(onClick = onOpenSuperheroSheet)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Lv.${vm.userLevel} ${charStats.stage.formName}",
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = AppColors.MintGlow.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AppColors.MintGlow.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Day $dayScore%",
                            color = AppColors.MintGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { xpProg },
                        color = Color(charStats.stage.primaryColorHex),
                        trackColor = AppColors.BgCardElevated,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${vm.userXp.intValue} XP",
                        color = AppColors.SolarGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderActionButton(Icons.Default.Timer, AppColors.CyberCyan, onOpenTimer)
            HeaderActionButton(Icons.Default.ShowChart, AppColors.MintGlow, onOpenAnalytics)
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
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.BgCard)
            .border(1.dp, AppColors.BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

// ============================================================================
// PROFESSIONALLY DESIGNED SUPERHERO ILLUSTRATION CANVAS & HERO HQ
// ============================================================================
@Composable
fun AlphaSuperheroCanvas(
    stage: CharacterStage,
    modifier: Modifier = Modifier
) {
    val primary = Color(stage.primaryColorHex)
    val secondary = Color(stage.secondaryColorHex)
    val capeColor = Color(stage.capeColorHex)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.54f

        // 1. Multi-Layered Cosmic / Bio-Electric Aura Behind Superhero
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.42f),
                    secondary.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(cx, cy - h * 0.05f),
                radius = w * 0.49f
            ),
            radius = w * 0.49f,
            center = Offset(cx, cy - h * 0.05f)
        )

        if (stage.stageNumber >= 3) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(primary, secondary, Color.Transparent, primary)),
                radius = w * 0.42f,
                center = Offset(cx, cy - h * 0.04f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 2. Flowing Superhero Cape ( Billowing behind shoulders )
        val capePath = Path().apply {
            moveTo(cx - w * 0.22f, cy - h * 0.14f)
            lineTo(cx + w * 0.22f, cy - h * 0.14f)
            lineTo(cx + w * 0.42f, cy + h * 0.38f)
            lineTo(cx + w * 0.16f, cy + h * 0.33f)
            lineTo(cx, cy + h * 0.40f)
            lineTo(cx - w * 0.18f, cy + h * 0.34f)
            lineTo(cx - w * 0.42f, cy + h * 0.38f)
            close()
        }
        drawPath(
            path = capePath,
            brush = Brush.verticalGradient(
                colors = listOf(capeColor, capeColor.copy(alpha = 0.85f), Color(0xFF090D16))
            )
        )
        drawPath(
            path = capePath,
            color = primary.copy(alpha = 0.55f),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // Stage 5+ Celestial Solar Wings
        if (stage.stageNumber >= 5) {
            val leftWing = Path().apply {
                moveTo(cx - w * 0.18f, cy - h * 0.12f)
                lineTo(cx - w * 0.47f, cy - h * 0.32f)
                lineTo(cx - w * 0.38f, cy - h * 0.02f)
                lineTo(cx - w * 0.45f, cy + h * 0.14f)
                lineTo(cx - w * 0.20f, cy + h * 0.06f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(cx + w * 0.18f, cy - h * 0.12f)
                lineTo(cx + w * 0.47f, cy - h * 0.32f)
                lineTo(cx + w * 0.38f, cy - h * 0.02f)
                lineTo(cx + w * 0.45f, cy + h * 0.14f)
                lineTo(cx + w * 0.20f, cy + h * 0.06f)
                close()
            }
            drawPath(leftWing, brush = Brush.linearGradient(listOf(AppColors.SolarGold, primary)))
            drawPath(rightWing, brush = Brush.linearGradient(listOf(AppColors.SolarGold, primary)))
        }

        // 3. Sculpted V-Taper Superhero Armored Torso & Pauldrons
        val shoulderHalf = w * (0.27f + stage.stageNumber * 0.012f)
        val waistHalf = w * 0.15f
        val torsoPath = Path().apply {
            moveTo(cx - shoulderHalf, cy - h * 0.12f)
            lineTo(cx - w * 0.14f, cy - h * 0.18f)
            lineTo(cx + w * 0.14f, cy - h * 0.18f)
            lineTo(cx + shoulderHalf, cy - h * 0.12f)
            lineTo(cx + waistHalf, cy + h * 0.26f)
            lineTo(cx, cy + h * 0.32f)
            lineTo(cx - waistHalf, cy + h * 0.26f)
            close()
        }
        drawPath(
            path = torsoPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF090D16))
            )
        )
        drawPath(
            path = torsoPath,
            color = primary,
            style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
        )

        // Armored Shoulder Pauldrons
        val leftPauldron = Path().apply {
            moveTo(cx - shoulderHalf * 1.08f, cy - h * 0.10f)
            lineTo(cx - shoulderHalf * 0.55f, cy - h * 0.19f)
            lineTo(cx - shoulderHalf * 0.48f, cy - h * 0.04f)
            close()
        }
        val rightPauldron = Path().apply {
            moveTo(cx + shoulderHalf * 1.08f, cy - h * 0.10f)
            lineTo(cx + shoulderHalf * 0.55f, cy - h * 0.19f)
            lineTo(cx + shoulderHalf * 0.48f, cy - h * 0.04f)
            close()
        }
        drawPath(leftPauldron, color = primary.copy(alpha = 0.85f))
        drawPath(rightPauldron, color = primary.copy(alpha = 0.85f))

        // 4. Iconic Superhero Chest Emblem (Glowing Diamond Shield + Alpha Bolt)
        val crestTop = cy - h * 0.08f
        val crestBot = cy + h * 0.12f
        val chestShield = Path().apply {
            moveTo(cx, crestTop)
            lineTo(cx + w * 0.12f, crestTop + h * 0.06f)
            lineTo(cx, crestBot)
            lineTo(cx - w * 0.12f, crestTop + h * 0.06f)
            close()
        }
        drawPath(chestShield, color = secondary)
        drawPath(chestShield, color = Color.White, style = Stroke(width = 1.6.dp.toPx()))

        // Utility Belt (Superhero Waistline)
        drawLine(
            color = AppColors.SolarGold,
            start = Offset(cx - waistHalf, cy + h * 0.24f),
            end = Offset(cx + waistHalf, cy + h * 0.24f),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 5. Sculpted Superhero Cowl / Mask & Glowing Eyes
        val headTop = cy - h * 0.42f
        val headBot = cy - h * 0.16f
        val cowlPath = Path().apply {
            // Left ear fin
            moveTo(cx - w * 0.14f, headTop + h * 0.06f)
            lineTo(cx - w * 0.17f, headTop - h * 0.05f)
            lineTo(cx - w * 0.09f, headTop + h * 0.02f)
            // Crown
            lineTo(cx, headTop)
            lineTo(cx + w * 0.09f, headTop + h * 0.02f)
            // Right ear fin
            lineTo(cx + w * 0.17f, headTop - h * 0.05f)
            lineTo(cx + w * 0.14f, headTop + h * 0.06f)
            // Jawline
            lineTo(cx + w * 0.13f, headBot - h * 0.04f)
            lineTo(cx, headBot + h * 0.02f)
            lineTo(cx - w * 0.13f, headBot - h * 0.04f)
            close()
        }
        drawPath(cowlPath, color = Color(0xFF0F172A))
        drawPath(cowlPath, color = primary, style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round))

        // Angular Glowing Superhero Eyes
        val eyeY = (headTop + headBot) * 0.52f
        drawLine(
            color = secondary,
            start = Offset(cx - w * 0.09f, eyeY - h * 0.01f),
            end = Offset(cx - w * 0.03f, eyeY + h * 0.01f),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondary,
            start = Offset(cx + w * 0.09f, eyeY - h * 0.01f),
            end = Offset(cx + w * 0.03f, eyeY + h * 0.01f),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 6. Glowing Energy Gauntlets / Fists (Left & Right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf( Color.White, secondary, Color.Transparent),
                center = Offset(cx - shoulderHalf * 1.05f, cy + h * 0.12f),
                radius = w * 0.11f
            ),
            radius = w * 0.11f,
            center = Offset(cx - shoulderHalf * 1.05f, cy + h * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, primary, Color.Transparent),
                center = Offset(cx + shoulderHalf * 1.05f, cy + h * 0.12f),
                radius = w * 0.11f
            ),
            radius = w * 0.11f,
            center = Offset(cx + shoulderHalf * 1.05f, cy + h * 0.12f)
        )

        // Stage 6 Omega Crown Halo
        if (stage.stageNumber >= 6) {
            drawCircle(
                color = AppColors.SolarGold,
                radius = w * 0.18f,
                center = Offset(cx, headTop - h * 0.02f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Composable
fun AlphaCharacterHeroCard(
    vm: HabitTrackerViewModel,
    onOpenSuperheroSheet: () -> Unit
) {
    val context = LocalContext.current
    val stats = vm.getCharacterStats()
    val stageColor = Color(stats.stage.primaryColorHex)

    Surface(
        color = AppColors.BgCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, stageColor.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSuperheroSheet() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlphaSuperheroCanvas(
                    stage = stats.stage,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.BgDeep)
                        .border(1.5.dp, stageColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = stageColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🦸 ${stats.stage.formName.uppercase()} • MK-${stats.stage.stageNumber}",
                                color = stageColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "⚡ ${stats.combatPower} CP",
                            color = AppColors.SolarGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${stats.stage.suitName} • Power: ${stats.stage.superpower}",
                        color = AppColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CharacterMiniStatPill("💪 STR", stats.strength, AppColors.CrimsonRose)
                        CharacterMiniStatPill("🧠 INT", stats.intellect, AppColors.CyberCyan)
                        CharacterMiniStatPill("🔥 WIL", stats.willpower, AppColors.SolarGold)
                        CharacterMiniStatPill("🌿 VIT", stats.vitality, AppColors.MintGlow)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Superhero Motivational Coach Speech Bubble
            Surface(
                color = AppColors.BgCardElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, stageColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stats.heroSpeechBubble,
                        color = AppColors.TextPrimary,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = stageColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { vm.triggerNewHeroMotivation(context) }
                    ) {
                        Text(
                            text = "⚡ Boost",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = "$label $value",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AlphaSuperheroHeadquartersModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = vm.getCharacterStats()
    val stageColor = Color(stats.stage.primaryColorHex)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, stageColor),
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
                    Text(
                        text = "🦸 SUPERHERO UPGRADE HQ",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                AlphaSuperheroCanvas(
                    stage = stats.stage,
                    modifier = Modifier
                        .size(175.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(AppColors.BgCard)
                        .border(2.dp, stageColor, RoundedCornerShape(28.dp))
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "${stats.stage.formName} (${stats.stage.title})",
                    color = stageColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${stats.stage.suitName}  •  Superpower: ${stats.stage.superpower}",
                    color = AppColors.SolarGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stats.heroSpeechBubble,
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )

                Spacer(Modifier.height(14.dp))

                // Interactive Power Upgrade Buttons
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppColors.BorderSubtle),
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
                            Text("SUPERHERO POWERS & UPGRADES", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Available XP: ${vm.userXp.intValue}", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        SuperheroPowerUpgradeRow(
                            label = "💪 Super Strength (Fitness & Health)",
                            statValue = stats.strength,
                            color = AppColors.CrimsonRose,
                            onUpgrade = { vm.upgradeSuperheroPower(context, "STR") }
                        )
                        SuperheroPowerUpgradeRow(
                            label = "🧠 Telepathic Intellect (Focus & Vault)",
                            statValue = stats.intellect,
                            color = AppColors.CyberCyan,
                            onUpgrade = { vm.upgradeSuperheroPower(context, "INT") }
                        )
                        SuperheroPowerUpgradeRow(
                            label = "🔥 Indomitable Will (Streaks & Missions)",
                            statValue = stats.willpower,
                            color = AppColors.SolarGold,
                            onUpgrade = { vm.upgradeSuperheroPower(context, "WIL") }
                        )
                        SuperheroPowerUpgradeRow(
                            label = "🌿 Bio-Regen Vitality (Journal & Rest)",
                            statValue = stats.vitality,
                            color = AppColors.MintGlow,
                            onUpgrade = { vm.upgradeSuperheroPower(context, "VIT") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // All 6 Superhero Suits Roadmap
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("SUPERHERO SUIT EVOLUTION ROADMAP", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        CharacterStage.entries.forEach { st ->
                            val unlocked = stats.stage.stageNumber >= st.stageNumber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (unlocked) Color(st.primaryColorHex).copy(alpha = 0.12f) else AppColors.BgCardElevated)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mk-${st.stageNumber}: ${st.formName} (${st.suitName})",
                                        color = if (unlocked) Color(st.primaryColorHex) else AppColors.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Power: ${st.superpower} • Gear: ${st.weaponOrGear}",
                                        color = AppColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (unlocked) "ACTIVE ✓" else "LOCKED 🔒",
                                    color = if (unlocked) AppColors.MintGlow else AppColors.TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
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
fun SuperheroPowerUpgradeRow(
    label: String,
    statValue: Int,
    color: Color,
    onUpgrade: () -> Unit
) {
    val ratio = (statValue / 300f).coerceIn(0.08f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$statValue", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, color),
                    modifier = Modifier.clickable { onUpgrade() }
                ) {
                    Text(
                        text = "+ Boost",
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = AppColors.BgCardElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

// ============================================================================
// 5-TAB BOTTOM NAVIGATION DOCK (Habits, Tasks, Day Audit, Journal, Vault)
// ============================================================================
@Composable
fun StudioDayProductivityAuditSection(
    vm: HabitTrackerViewModel,
    onOpenSuperheroSheet: () -> Unit
) {
    val context = LocalContext.current
    val selectedDate = vm.selectedDate.value
    val today = LocalDate.now()
    val dateStr = selectedDate.toString()
    val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))

    val dayScore = vm.getDailyProductivityScore(selectedDate)
    val grade = vm.getDailyProductivityGrade(dayScore)

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
        // 1. Date Navigator Bar (< Previous Day | Date Picker | Next Day >)
        item {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AppColors.BorderSubtle),
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
                                android.app.DatePickerDialog(
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
                        Icon(Icons.Default.DateRange, null, tint = AppColors.CyberCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (selectedDate == today) "Today ($formattedDate)" else formattedDate,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
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
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Day",
                                tint = if (selectedDate.isBefore(today)) AppColors.TextPrimary else AppColors.TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. Daily Productivity Score & Penalty Dashboard Card
        item {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.6f)),
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
                                text = "📊 PRODUCTIVITY REPORT • ${formattedDate.uppercase()}",
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
                                text = "Log every hour. Unproductive hours deduct score & Superhero XP!",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier.size(74.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 7.dp.toPx()
                                drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                                drawArc(
                                    color = scoreColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (dayScore / 100f),
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
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "🔥 Productive",
                            value = "$peakHours hrs",
                            color = AppColors.MintGlow
                        )
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "💀 Wasted",
                            value = "$wastedHours hrs",
                            color = AppColors.CrimsonRose
                        )
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "⚡ Net Hero XP",
                            value = if (netXpDay >= 0) "+$netXpDay XP" else "$netXpDay XP",
                            color = if (netXpDay >= 0) AppColors.SolarGold else AppColors.CrimsonRose
                        )
                    }
                }
            }
        }

        // 3. Hour-by-Hour Timeline Header
        item {
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
                    text = "Tap any hour to edit",
                    color = AppColors.CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. 18 Interactive Hour Slots (06:00 to 23:00)
        val hoursList = (6..23).toList()
        items(hoursList, key = { "$dateStr-$it" }) { hour24 ->
            val loggedItem = logsForDay[hour24]
            HourlySlotCard(
                hour24 = hour24,
                loggedItem = loggedItem,
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

// ============================================================================
// NEW SECTION 3 (TAB 2): HOURLY DAY PRODUCTIVITY AUDIT & PENALTY LEDGER
// ============================================================================
@Composable
fun StudioDayProductivityAuditSection(
    vm: HabitTrackerViewModel,
    onOpenSuperheroSheet: () -> Unit
) {
    val context = LocalContext.current
    val todayStr = vm.selectedDate.value.toString()
    val dayScore = vm.getDailyProductivityScore(vm.selectedDate.value)
    val grade = vm.getDailyProductivityGrade(dayScore)

    val logsForToday = remember(vm.hourlyLogs.toList(), todayStr) {
        vm.hourlyLogs.filter { it.date == todayStr }.associateBy { it.hour24 }
    }

    val peakHours = logsForToday.values.count { it.status == HourProductivityStatus.PEAK_FLOW || it.status == HourProductivityStatus.PRODUCTIVE }
    val wastedHours = logsForToday.values.count { it.status == HourProductivityStatus.TIME_WASTED || it.status == HourProductivityStatus.LOW_FOCUS }
    val netXpToday = logsForToday.values.sumOf { it.status.xpDelta }

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
        // 1. Daily Productivity Score & Penalty Dashboard Card
        item {
            Surface(
                color = AppColors.BgCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.6f)),
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
                                text = "📊 DAILY PRODUCTIVITY AUDIT",
                                color = scoreColor,
                                fontSize = 11.sp,
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
                                text = "Log every hour. Unproductive hours deduct score & Superhero XP!",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Circular Day Score Gauge
                        Box(
                            modifier = Modifier.size(74.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 7.dp.toPx()
                                drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                                drawArc(
                                    color = scoreColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (dayScore / 100f),
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
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "🔥 Productive",
                            value = "$peakHours hrs",
                            color = AppColors.MintGlow
                        )
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "💀 Wasted",
                            value = "$wastedHours hrs",
                            color = AppColors.CrimsonRose
                        )
                        AuditStatPill(
                            modifier = Modifier.weight(1f),
                            title = "⚡ Net Hero XP",
                            value = if (netXpToday >= 0) "+$netXpToday XP" else "$netXpToday XP",
                            color = if (netXpToday >= 0) AppColors.SolarGold else AppColors.CrimsonRose
                        )
                    }
                }
            }
        }

        // 2. Hour-by-Hour Timeline Header
        item {
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
                    text = "Tap any hour to log",
                    color = AppColors.CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3. 18 Interactive Hour Slots (06:00 to 23:00)
        val hoursList = (6..23).toList()
        items(hoursList, key = { it }) { hour24 ->
            val loggedItem = logsForToday[hour24]
            HourlySlotCard(
                hour24 = hour24,
                loggedItem = loggedItem,
                onClick = { editingHour24 = hour24 },
                onQuickPenalty = {
                    vm.logOrUpdateHour(
                        context = context,
                        hour24 = hour24,
                        activityNote = "Wasted time / Unproductive hour",
                        status = HourProductivityStatus.TIME_WASTED
                    )
                },
                onQuickFlow = {
                    vm.logOrUpdateHour(
                        context = context,
                        hour24 = hour24,
                        activityNote = "Focused productive work block",
                        status = HourProductivityStatus.PEAK_FLOW
                    )
                }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    editingHour24?.let { hour ->
        LogHourActivityDialog(
            hour24 = hour,
            existingLog = logsForToday[hour],
            onDismiss = { editingHour24 = null },
            onSave = { note, status ->
                vm.logOrUpdateHour(context, hour, note, status)
                editingHour24 = null
            },
            onDelete = {
                logsForToday[hour]?.let { vm.deleteHourlyLog(context, it.id) }
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
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
    val start = LocalTime.of(hour24, 0)
    val timeLabel = start.format(DateTimeFormatter.ofPattern("hh:00 a"))
    val statusColor = loggedItem?.status?.color ?: AppColors.BorderSubtle

    Surface(
        color = if (loggedItem != null) AppColors.BgCard else AppColors.BgSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (loggedItem != null) statusColor.copy(alpha = 0.55f) else AppColors.BorderSubtle
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
            // Left Time Pill
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

            // Middle Activity Description
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
                    Text(
                        text = "Unlogged hour • Tap to record what you did",
                        color = AppColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right Quick 1-Tap Buttons (+Flow or -Penalty)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.MintGlow.copy(alpha = 0.16f))
                        .border(1.dp, AppColors.MintGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onQuickFlow() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.MintGlow)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.CrimsonRose.copy(alpha = 0.16f))
                        .border(1.dp, AppColors.CrimsonRose.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onQuickPenalty() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-💀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.CrimsonRose)
                }
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
        title = {
            Text("⏰ Audit Hour: $rangeLabel", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("What did you do during this hour?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text("Productivity Rating (Impacts Score & Hero XP):", color = AppColors.TextSecondary, fontSize = 12.sp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HourProductivityStatus.entries.forEach { st ->
                        val selected = status == st
                        Surface(
                            color = if (selected) st.color.copy(alpha = 0.2f) else AppColors.BgCardElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) st.color else AppColors.BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { status = st }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(st.label, color = if (selected) st.color else AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(st.shortBadge, color = st.color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(note, status) },
                colors = ButtonDefaults.buttonColors(containerColor = status.color)
            ) {
                Text("Save Hour", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (existingLog != null) {
                    TextButton(onClick = onDelete) {
                        Text("Clear", color = AppColors.CrimsonRose)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ============================================================================
// SECTION 1 (TAB 0): STUDIO HABIT TRACKER
// ============================================================================
@Composable
fun StudioHabitTrackerSection(
    vm: HabitTrackerViewModel,
    onOpenSuperheroSheet: () -> Unit,
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
            AlphaCharacterHeroCard(
                vm = vm,
                onOpenSuperheroSheet = onOpenSuperheroSheet
            )
        }

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
                    onClick = onNewHabit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.MintGlow)
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
                .padding(horizontal = 10.dp, vertical = 10.dp),
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
                            val stroke = 3.2.dp.toPx()
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
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(habit.accentColor.copy(alpha = 0.16f))
                            .border(1.dp, habit.accentColor.copy(alpha = 0.45f), RoundedCornerShape(15.dp)),
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
                        .background(if (isDone) habit.accentColor else habit.accentColor.copy(alpha = 0.16f))
                        .border(1.5.dp, habit.accentColor, RoundedCornerShape(15.dp))
                        .clickable { onToggleOrStep() },
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
                        Text("🔥 $streak d", color = AppColors.SolarGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                    if (habit.reminderTime.isNotBlank()) {
                        Surface(color = AppColors.CyberCyan.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text("🔔 ${habit.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
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
                        SmallToolPillButton(icon = Icons.Default.Timer, tint = AppColors.CyberCyan, onClick = onLaunchFocus)
                        SmallToolPillButton(icon = Icons.Default.Remove, tint = AppColors.TextSecondary, onClick = onDecrement)
                        SmallToolPillButton(icon = Icons.Default.MoreVert, tint = habit.accentColor, onClick = onOpenCustomValue)
                    }
                    SmallToolPillButton(icon = Icons.Default.Edit, tint = AppColors.TextSecondary, onClick = onEdit)
                    SmallToolPillButton(icon = Icons.Default.Delete, tint = AppColors.TextMuted, onClick = onDelete)
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
                HabitViewMode.HEATMAP_GRID, HabitViewMode.GRID_VIEW -> StudioSixteenWeekHeatmap(habit)
                else -> StudioSevenDayInteractiveStrip(habit, onToggleAnyDate)
            }
        }
    }
}

@Composable
fun SmallToolPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(AppColors.BgCardElevated)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
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
// SECTION 2 (TAB 1): TO-DO MATRIX WITH AUTOMATIC DAILY TASK RENEWAL
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Alpha Mission Matrix", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tasks marked 🔄 Auto-Renew reset every new day automatically!", color = AppColors.MintGlow, fontSize = 11.sp)
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

        // Priority Filter + Test Next-Day Auto-Renew Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = selectedPriority == null, onClick = { selectedPriority = null }, label = { Text("All") }) }
                items(TodoPriority.entries) { prio ->
                    FilterChip(selected = selectedPriority == prio, onClick = { selectedPriority = prio }, label = { Text(prio.shortTag) })
                }
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
                        .clickable { onToggleMain() },
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
                        Text(task.priority.shortTag, color = prioColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (task.isRecurring) {
                        Surface(color = AppColors.MintGlow.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "🔄 Daily Renew • ${task.renewalStreak}d",
                                color = AppColors.MintGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.reminderTime.isNotBlank()) {
                        Text("🔔 ${task.reminderTime}", color = AppColors.CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallToolPillButton(icon = Icons.Default.PlayArrow, tint = AppColors.CyberCyan, onClick = onStartFocus)
                    SmallToolPillButton(icon = Icons.Default.Edit, tint = AppColors.TextSecondary, onClick = onEdit)
                    SmallToolPillButton(icon = Icons.Default.Close, tint = AppColors.TextMuted, onClick = onDelete)
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
// SECTION 4 (TAB 3): EYE-SOOTHING SANCTUARY JOURNAL
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
                Icon(Icons.Default.Edit, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Write", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        Icon(Icons.Default.Delete, "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
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
// SECTION 5 (TAB 4): SECOND-BRAIN VAULT
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
                Icon(Icons.Default.Add, null, tint = Color.Black)
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
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) AppColors.SolarGold else theme.textSecondary.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
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
// DIALOGS: CREATE / EDIT HABIT, AUTO-RENEW TODO, JOURNAL, NOTEBOOK & FOCUS
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
                        Icon(Icons.Default.Notifications, null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (reminder.isBlank()) "Set Reminder" else "🔔 $reminder", color = AppColors.CyberCyan, fontSize = 12.sp)
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

                // Auto-Renew Every Day Switch
                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.MintGlow.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔄 Auto-Renew Every Day", color = AppColors.MintGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Automatically resets tomorrow after you complete it today", color = AppColors.TextSecondary, fontSize = 11.sp)
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
                    Icon(Icons.Default.Add, null, tint = AppColors.ElectricViolet)
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
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, AppColors.CyberCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
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
                        Button(
                            onClick = { vm.togglePauseFocusTimer(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.CyberCyan)
                        ) {
                            Text(if (vm.isFocusTimerRunning.value) "Pause" else "Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Nervous System Reset (4-4-4-4)", color = AppColors.MintGlow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(AppColors.CyberCyan.copy(alpha = 0.16f))
                            .border(2.dp, AppColors.CyberCyan, CircleShape),
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
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, AppColors.BorderSubtle),
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📊 Alpha Performance Analytics", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                Text("All habits, Superhero upgrades, hourly day audits, tasks, and notes are securely backed up.", color = AppColors.TextSecondary, fontSize = 13.sp)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Last Sync: ${vm.lastCloudBackup.value}", color = AppColors.MintGlow, fontSize = 12.sp)

                OutlinedButton(onClick = { exportLauncher.launch("AlphaSuperhero_Backup.json") }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.MintGlow)) {
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
                Text("🦸", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Surface(color = AppColors.MintGlow.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Text("+$xpEarned XP  •  Lv.$level $rank", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = AppColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow), shape = RoundedCornerShape(14.dp)) {
                    Text("Keep Ascending", color = Color.Black, fontWeight = FontWeight.Bold)
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
