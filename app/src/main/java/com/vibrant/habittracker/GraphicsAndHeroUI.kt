package com.vibrant.habittracker

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// 1. STUDIO MOTION, AMBIENT SKY & ROLLING COUNTERS
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

@Composable
fun RollingCounterText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 2 } +
                fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(160)))
        },
        label = "rollingCounter",
        modifier = modifier
    ) { target ->
        Text(
            text = target,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}

@Composable
fun rememberTimeOfDayAmbientColors(): List<Color> {
    val hour = remember { LocalTime.now().hour }
    return remember(hour) {
        when (hour) {
            in 5..11 -> listOf(Color(0xFF0A1624), AppColors.BgDeep, Color(0xFF030508))
            in 12..16 -> listOf(Color(0xFF0B1325), AppColors.BgDeep, Color(0xFF030508))
            in 17..20 -> listOf(Color(0xFF150E22), AppColors.BgDeep, Color(0xFF030508))
            else -> listOf(Color(0xFF090B14), AppColors.BgDeep, Color(0xFF020306))
        }
    }
}

// ============================================================================
// 2. 3D METALLIC ALPHA LOGO, TITANIUM TOP BAR & BOTTOM DOCK
// ============================================================================
@Composable
fun AlphaTitaniumLogo(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF131B2E), Color(0xFF080C15), Color(0xFF0E1626))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AppColors.Platinum.copy(alpha = 0.55f),
                        AppColors.CyberCyan.copy(alpha = 0.55f),
                        AppColors.BorderSubtle
                    )
                ),
                shape = RoundedCornerShape(13.dp)
            )
            .studioBounceClick(onClick)
    ) {
        val w = size.width
        val h = size.height

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
            path = plate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1F2A40), Color(0xFF0B101B)),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        val leftFacet = Path().apply {
            moveTo(w * 0.50f, h * 0.13f)
            lineTo(w * 0.20f, h * 0.27f)
            lineTo(w * 0.16f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.87f)
            close()
        }
        drawPath(leftFacet, color = Color.White.copy(alpha = 0.08f))
        drawPath(
            path = plate,
            brush = Brush.linearGradient(listOf(AppColors.CyberCyan, AppColors.MintGlow)),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )

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

        val strike = Path().apply {
            moveTo(w * 0.28f, h * 0.59f)
            lineTo(w * 0.50f, h * 0.49f)
            lineTo(w * 0.50f, h * 0.57f)
            lineTo(w * 0.72f, h * 0.47f)
        }
        drawPath(
            path = strike,
            color = AppColors.MintGlow,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(
            color = AppColors.SolarGold,
            radius = 2.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.22f)
        )
    }
}

@Composable
fun AlphaTitaniumTopBar(
    vm: HabitTrackerViewModel,
    onOpenSentinelSheet: () -> Unit,
    onOpenBrainStudio: () -> Unit,
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
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

            Spacer(Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stats.stage.title,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
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
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${vm.userXp.intValue} XP • $dayScore%",
                        color = AppColors.TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            HeaderActionButton(
                icon = Icons.Default.Psychology,
                tint = AppColors.CrimsonRose,
                onClick = onOpenBrainStudio
            )

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

            HeaderActionButton(
                icon = Icons.Default.Timer,
                tint = AppColors.CyberCyan,
                onClick = onOpenTimer
            )
            HeaderActionButton(
                icon = Icons.Default.ShowChart,
                tint = AppColors.Platinum,
                onClick = onOpenAnalytics
            )
            HeaderActionButton(
                icon = if (vm.connectedGoogleEmail.value != null) {
                    Icons.Default.CloudDone
                } else {
                    Icons.Default.CloudUpload
                },
                tint = if (vm.connectedGoogleEmail.value != null) {
                    AppColors.MintGlow
                } else {
                    AppColors.ElectricViolet
                },
                onClick = onOpenCloud
            )
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(AppColors.BgCard)
            .border(0.8.dp, AppColors.BorderSubtle, RoundedCornerShape(11.dp))
            .studioBounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
fun StudioTitaniumBottomDock(
    currentTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        color = AppColors.BgSurface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 9.dp),
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
                val pillBg by animateColorAsState(
                    targetValue = if (selected) AppColors.MintGlow.copy(alpha = 0.14f) else Color.Transparent,
                    label = "dockBg"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) AppColors.MintGlow else AppColors.TextSecondary,
                    label = "dockTint"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(pillBg)
                        .studioBounceClick { onSelectTab(index) }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = title,
                        color = tint,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveFocusTimerBanner(
    vm: HabitTrackerViewModel,
    onExpand: () -> Unit
) {
    val context = LocalContext.current
    val rem = vm.focusRemainingSeconds.longValue
    val timeFormatted = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)

    Surface(
        color = AppColors.CyberCyan.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.CyberCyan),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .studioBounceClick(onExpand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = AppColors.CyberCyan)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = vm.activeFocusTitle.value,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = if (vm.ambientSoundMode.value != AmbientSoundMode.OFF) {
                        "${vm.ambientSoundMode.value.icon} ${vm.ambientSoundMode.value.label} playing"
                    } else {
                        "Focus session active • Tap to expand"
                    }
                    Text(text = sub, color = AppColors.TextSecondary, fontSize = 11.sp)
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
                        imageVector = if (vm.isFocusTimerRunning.value) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Pause or Resume",
                        tint = AppColors.TextPrimary
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. REACTIVE BRAIN MOOD CARD & BRAIN WIDGET STUDIO MODAL
// ============================================================================
@Composable
fun BrainMoodDashboardCard(
    snapshot: BrainMoodSnapshot,
    onOpenWidgetStudio: () -> Unit
) {
    val bgStart by animateColorAsState(Color(snapshot.bgStartHex), tween(550), label = "brainBg1")
    val bgEnd by animateColorAsState(Color(snapshot.bgEndHex), tween(550), label = "brainBg2")
    val borderCol by animateColorAsState(
        Color(snapshot.borderHex).copy(alpha = 0.55f),
        tween(550),
        label = "brainBorder"
    )

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .studioBounceClick(onOpenWidgetStudio)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🧠 MINDSTATE • ${snapshot.mood.badgeText}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "Widget Studio →",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RollingCounterText(
                        text = snapshot.ratioText,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = snapshot.subtitleText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${snapshot.headlineText.replace("\n", " ")}  •  ${snapshot.breakdownText}",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            AnimatedContent(
                targetState = snapshot.mood,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(tween(240))) togetherWith fadeOut(tween(160))
                },
                label = "brainMiniMorph"
            ) { mood ->
                val bmp = remember(mood) {
                    BrainCharacterRenderer.renderBrainBitmap(mood, sizePx = 220).asImageBitmap()
                }
                Image(
                    bitmap = bmp,
                    contentDescription = "Brain Mood Character",
                    modifier = Modifier.size(78.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun BrainMoodWidgetStudioModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val liveSnapshot = vm.cachedBrainSnapshot.value
    val selectedColorPref = vm.brainWidgetColorPref.value
    var previewMoodOverride by remember { mutableStateOf<BrainMoodState?>(null) }

    val displayedSnapshot = remember(liveSnapshot, previewMoodOverride, selectedColorPref) {
        previewMoodOverride?.let {
            BrainMoodCalculator.buildPreviewSnapshot(it, selectedColorPref)
        } ?: liveSnapshot
    }

    val bgStart by animateColorAsState(Color(displayedSnapshot.bgStartHex), tween(450), label = "studioBg1")
    val bgEnd by animateColorAsState(Color(displayedSnapshot.bgEndHex), tween(450), label = "studioBg2")
    val borderCol by animateColorAsState(
        Color(displayedSnapshot.borderHex).copy(alpha = 0.6f),
        tween(450),
        label = "studioBorder"
    )

    val infinite = rememberInfiniteTransition(label = "brainFloat")
    val floatY by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brainFloatY"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.2.dp, borderCol),
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
                            text = "🧠 BRAIN WIDGET STUDIO",
                            color = AppColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Reacts live to Habits, Tasks & Time of Day",
                            color = AppColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 1:1 Home Screen Widget Preview
                Box(
                    modifier = Modifier
                        .size(width = 250.dp, height = 250.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
                        .border(1.4.dp, borderCol, RoundedCornerShape(32.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        RollingCounterText(
                            text = displayedSnapshot.ratioText,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = displayedSnapshot.subtitleText,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .width(112.dp)
                    ) {
                        Text(
                            text = displayedSnapshot.headlineText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = displayedSnapshot.breakdownText,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedContent(
                        targetState = displayedSnapshot.mood,
                        transitionSpec = {
                            (scaleIn(
                                initialScale = 0.8f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) + fadeIn(tween(240))) togetherWith fadeOut(tween(150))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer { translationY = floatY },
                        label = "brainStudioChar"
                    ) { mood ->
                        val bmp = remember(mood) {
                            BrainCharacterRenderer.renderBrainBitmap(mood, sizePx = 280).asImageBitmap()
                        }
                        Image(
                            bitmap = bmp,
                            contentDescription = "Brain Preview",
                            modifier = Modifier.size(118.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PREVIEW BRAIN REACTIONS",
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = previewMoodOverride == null,
                                    onClick = { previewMoodOverride = null },
                                    label = { Text("Live Now", fontSize = 11.sp) }
                                )
                            }
                            items(BrainMoodState.entries) { mood ->
                                FilterChip(
                                    selected = previewMoodOverride == mood,
                                    onClick = { previewMoodOverride = mood },
                                    label = { Text(mood.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GOOD-PERFORMANCE WIDGET THEME",
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Turns deep crimson red automatically if tasks are still incomplete as the day ends.",
                            color = AppColors.TextMuted,
                            fontSize = 11.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BrainWidgetColorPref.entries.forEach { pref ->
                                val active = selectedColorPref == pref
                                val accentDot = when (pref) {
                                    BrainWidgetColorPref.AUTO_DYNAMIC -> AppColors.CyberCyan
                                    BrainWidgetColorPref.EMERALD_GREEN -> AppColors.MintGlow
                                    BrainWidgetColorPref.SAPPHIRE_BLUE -> Color(0xFF3B82F6)
                                    BrainWidgetColorPref.COSMIC_PURPLE -> AppColors.ElectricViolet
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) accentDot.copy(alpha = 0.16f) else AppColors.BgSurface)
                                        .border(
                                            width = 0.9.dp,
                                            color = if (active) accentDot else AppColors.BorderSubtle,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { vm.setBrainWidgetColorPref(context, pref) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(accentDot)
                                        )
                                        Text(
                                            text = pref.label,
                                            color = if (active) Color.White else AppColors.TextSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (active) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = accentDot,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        BrainMoodWidgetProvider.updateAllBrainWidgets(context)
                        Toast.makeText(
                            context,
                            "🧠 Brain Mood Widget synced! Long-press home screen → Widgets → Alpha Brain Mood.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Home Screen Widget Now", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ============================================================================
// 4. 3D-SHADED DARK KNIGHT SENTINEL CANVAS, HERO CARD & ARMORY
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

    Canvas(modifier = modifier.graphicsLayer { translationY = hoverY }) {
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

        if (tier >= 6) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(AppColors.SolarGold, primary, Color.Transparent, AppColors.SolarGold)),
                radius = w * 0.44f,
                center = Offset(cx, cy - h * 0.06f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 2. Sculpted Multi-Fold Cloak
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
            path = backCape,
            brush = Brush.verticalGradient(listOf(capeBase, Color(0xFF0A0D14), Color(0xFF04060A)))
        )
        drawPath(
            path = backCape,
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.65f), Color.Transparent, secondary.copy(alpha = 0.55f)),
                start = Offset(0f, cy - h * 0.15f),
                end = Offset(w, cy + h * 0.35f)
            ),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )

        // Stage 3+ High Tactical Mantle Collar
        if (tier >= 3) {
            val collar = Path().apply {
                moveTo(cx - w * 0.24f, cy - h * 0.12f)
                lineTo(cx - w * 0.19f, cy - h * 0.26f)
                lineTo(cx + w * 0.19f, cy - h * 0.26f)
                lineTo(cx + w * 0.24f, cy - h * 0.12f)
                close()
            }
            drawPath(collar, brush = Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF090D16))))
            drawPath(collar, color = primary.copy(alpha = 0.55f), style = Stroke(width = 1.2.dp.toPx()))
        }

        // 3. 3D Sculpted Armored Torso
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
            path = torsoBase,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF161E2E), Color(0xFF080B12)),
                start = Offset(cx - shoulderHalf, cy - h * 0.20f),
                end = Offset(cx + shoulderHalf, cy + h * 0.28f)
            )
        )
        drawPath(
            path = torsoBase,
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), primary.copy(alpha = 0.85f))),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // Left & Right Pectoral Plates
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

        // Abdominal Segments
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

        // 4. Layered 3D Shoulder Pauldrons (Stage 2+)
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
                if (tier >= 5) listOf(AppColors.SolarGold, primary) else listOf(Color(0xFF475569), Color(0xFF111827))
            )
            drawPath(leftPauldron, brush = pauldronBrush)
            drawPath(rightPauldron, brush = pauldronBrush)
            drawPath(leftPauldron, color = primary.copy(alpha = 0.85f), style = Stroke(width = 1.3.dp.toPx()))
            drawPath(rightPauldron, color = primary.copy(alpha = 0.85f), style = Stroke(width = 1.3.dp.toPx()))
        }

        // 5. Winged Chest Crest & Utility Belt
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
        drawPath(wingedEmblem, color = if (tier >= 3) primary else Color(0xFF0A0E17))
        drawPath(
            path = wingedEmblem,
            color = if (tier >= 5) AppColors.SolarGold else Color.White.copy(alpha = 0.7f),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
        )

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

        // 6. Sculpted 3D Dark Knight Cowl & Luminous Lenses
        val headTop = cy - h * 0.40f
        val headBot = cy - h * 0.17f
        val cowlPath = Path().apply {
            moveTo(cx - w * 0.125f, headTop + h * 0.055f)
            lineTo(cx - w * 0.145f, headTop - h * 0.085f)
            lineTo(cx - w * 0.075f, headTop + h * 0.015f)
            lineTo(cx, headTop - h * 0.01f)
            lineTo(cx + w * 0.075f, headTop + h * 0.015f)
            lineTo(cx + w * 0.145f, headTop - h * 0.085f)
            lineTo(cx + w * 0.125f, headTop + h * 0.055f)
            lineTo(cx + w * 0.12f, headBot - h * 0.045f)
            lineTo(cx + w * 0.06f, headBot + h * 0.015f)
            lineTo(cx, headBot + h * 0.03f)
            lineTo(cx - w * 0.06f, headBot + h * 0.015f)
            lineTo(cx - w * 0.12f, headBot - h * 0.045f)
            close()
        }
        drawPath(
            path = cowlPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF111827), Color(0xFF05070B)),
                start = Offset(cx - w * 0.14f, headTop),
                end = Offset(cx + w * 0.14f, headBot)
            )
        )

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
            path = cowlPath,
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), primary)),
            style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round)
        )

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

        // 7. Armored Forearm Gauntlets & Blades (Stage 4+)
        if (tier >= 4) {
            val leftG = Offset(cx - shoulderHalf * 1.02f, cy + h * 0.14f)
            val rightG = Offset(cx + shoulderHalf * 1.02f, cy + h * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, primary, Color.Transparent), leftG, w * 0.095f),
                radius = w * 0.095f,
                center = leftG
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, secondary, Color.Transparent), rightG, w * 0.095f),
                radius = w * 0.095f,
                center = rightG
            )
            for (f in 0..1) {
                val dy = f * h * 0.045f
                drawLine(
                    color = primary,
                    start = Offset(leftG.x - w * 0.02f, leftG.y - dy),
                    end = Offset(leftG.x - w * 0.075f, leftG.y - dy - h * 0.03f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondary,
                    start = Offset(rightG.x + w * 0.02f, rightG.y - dy),
                    end = Offset(rightG.x + w * 0.075f, rightG.y - dy - h * 0.03f),
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
                        .size(90.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.radialGradient(listOf(stageColor.copy(alpha = 0.16f), AppColors.BgDeep))
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
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "New Quote",
                            tint = stageColor,
                            modifier = Modifier.size(15.dp)
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

@Composable
fun DarkKnightArmoryModal(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
    val stats = vm.cachedCharacterStats.value
    var previewStage by remember { mutableStateOf(stats.stage) }
    val previewColor = Color(previewStage.primaryColorHex)
    val isPreviewUnlocked = stats.stage.stageNumber >= previewStage.stageNumber

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                            Text(
                                text = "EARNED DISCIPLINE PROFILE",
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "CP ${stats.combatPower}",
                                color = AppColors.SolarGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        EarnedStatProgressBar("💪 Strength (Fitness & Health Habits)", stats.strength, AppColors.CrimsonRose)
                        EarnedStatProgressBar("🧠 Intellect (Goals, Focus & Vault)", stats.intellect, AppColors.CyberCyan)
                        EarnedStatProgressBar("🔥 Willpower (Streaks & Missions)", stats.willpower, AppColors.SolarGold)
                        EarnedStatProgressBar("🌿 Vitality (Journal & Consistency)", stats.vitality, AppColors.MintGlow)
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                        Text(
                            text = "STEP-BY-STEP ARMOR EVOLUTION (TAP TO PREVIEW)",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = AppColors.TextSecondary, fontSize = 12.sp)
            Text("$statValue", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = AppColors.BgCardElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

// ============================================================================
// 5. DAILY QUESTS, GPU 16-WEEK HEATMAP, SHARED BUTTONS & CELEBRATION OVERLAY
// ============================================================================
@Composable
fun DailyBonusQuestsRow(
    quests: List<DailyQuestStatus>,
    onClaimQuest: (DailyQuestStatus) -> Unit
) {
    if (quests.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "DAILY BONUS QUESTS",
            color = AppColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(quests, key = { it.id }) { q ->
                Surface(
                    color = AppColors.BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = if (!q.isClaimed && q.isCompleted) AppColors.MintGlow else AppColors.BorderSubtle
                    ),
                    modifier = Modifier.width(174.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${q.emoji} ${q.title}",
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${q.current.coerceAtMost(q.target)}/${q.target}",
                                color = AppColors.MintGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = q.description,
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (q.current.toFloat() / q.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            color = if (q.isCompleted) AppColors.MintGlow else AppColors.CyberCyan,
                            trackColor = AppColors.BgCardElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            q.isClaimed -> {
                                Text(
                                    text = "✓ Claimed",
                                    color = AppColors.TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            q.isCompleted -> {
                                Surface(
                                    color = AppColors.MintGlow,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .studioBounceClick { onClaimQuest(q) }
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
                            else -> {
                                Text(
                                    text = "Reward: +${q.rewardXp} XP${if (q.grantsShield) " +🛡️" else ""}",
                                    color = AppColors.SolarGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
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

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
    ) {
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
                drawRoundRect(
                    color = color,
                    topLeft = Offset(c * (cellW + gap), r * (cellH + gap)),
                    size = Size(cellW, cellH),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
fun SmallToolPillButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
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

private data class StudioParticle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float
)

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
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(2000, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .clickable { onDismiss() },
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
                val x = cx + p.vx * t
                val y = cy + p.vy * t + 0.5f * 1150f * t * t
                drawCircle(
                    color = p.color.copy(alpha = (1f - t * 0.85f).coerceIn(0f, 1f)),
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        }

        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.5.dp, Color(stage.primaryColorHex)),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DarkKnight3DCanvas(
                    stage = stage,
                    animateStudioLight = false,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "+$xpEarned XP  •  Lv.$level $rank",
                        color = AppColors.MintGlow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
