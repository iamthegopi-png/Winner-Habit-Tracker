package com.vibrant.habittracker

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// ============================================================================
// 1. NOTIFICATION & BATTERY SHIELD DIALOG
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
            border = BorderStroke(
                width = 1.2.dp,
                color = if (health.allHealthy) AppColors.MintGlow else AppColors.SolarGold
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Notification Shield",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.TextSecondary)
                    }
                }

                Text(
                    text = "Enable all 3 shields so Android Doze and battery savers never silence your habit or mission reminders while idle.",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                ShieldCheckItemRow(
                    title = "1. Notification Permission",
                    subtitle = if (health.notificationsGranted) "Allowed" else "Tap Fix to allow alerts",
                    isOk = health.notificationsGranted,
                    onFix = {
                        onRequestNotifPermission()
                        try {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    }
                )

                ShieldCheckItemRow(
                    title = "2. Exact Doze-Proof Alarms",
                    subtitle = if (health.exactAlarmsGranted) "AlarmClock priority active" else "Tap Fix to allow exact alarms",
                    isOk = health.exactAlarmsGranted,
                    onFix = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    }
                )

                ShieldCheckItemRow(
                    title = "3. Unrestricted Battery",
                    subtitle = if (health.batteryUnrestricted) "Protected from OS sleep" else "Tap Fix & allow Unrestricted",
                    isOk = health.batteryUnrestricted,
                    onFix = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: Exception) {
                            }
                        }
                    }
                )

                Surface(
                    color = AppColors.BgCardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Armed Habit & Mission Alarms:",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${health.scheduledRemindersCount} Active",
                            color = AppColors.MintGlow,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            Toast.makeText(
                                context,
                                "🔔 Test alarm set for 5s! Lock your screen to test.",
                                Toast.LENGTH_LONG
                            ).show()
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
fun ShieldCheckItemRow(
    title: String,
    subtitle: String,
    isOk: Boolean,
    onFix: () -> Unit
) {
    Surface(
        color = AppColors.BgCardElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isOk) AppColors.MintGlow.copy(alpha = 0.45f) else AppColors.SolarGold.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOk) AppColors.MintGlow else AppColors.SolarGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(subtitle, color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }

            if (!isOk) {
                Button(
                    onClick = onFix,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SolarGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Fix", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            } else {
                Text("OK", color = AppColors.MintGlow, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

// ============================================================================
// 2. CUSTOM PHONE AUDIO PICKER ROW (Used in Habit & Task Dialogs)
// ============================================================================
@Composable
fun CustomAudioPickerRow(
    customSoundUri: String,
    onSoundSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val fileName = remember(customSoundUri) {
        CelebrationSoundEngine.getAudioFileName(context, customSoundUri)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onSoundSelected(it.toString())
        }
    }

    Surface(
        color = AppColors.ElectricViolet.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.9.dp, AppColors.ElectricViolet.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎵 Custom Sound Effect / Ringtone",
                        color = AppColors.ElectricViolet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = fileName,
                        color = AppColors.TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (customSoundUri.isNotBlank()) {
                    IconButton(
                        onClick = {
                            CelebrationSoundEngine.stopActiveCustomSound()
                            onSoundSelected("")
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset sound",
                            tint = AppColors.CrimsonRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AppColors.ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AppColors.ElectricViolet,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Import from Phone",
                        color = AppColors.ElectricViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        CelebrationSoundEngine.playCustomOrDefaultSound(
                            context = context,
                            customSoundUri = customSoundUri,
                            isReminder = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("▶️ Test", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 3. CREATE / EDIT HABIT & MEASUREMENT DIALOGS
// ============================================================================
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
        title = {
            Text(
                text = if (habitToEdit == null) "Design New Habit" else "Edit Habit",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Icon") },
                        modifier = Modifier.width(70.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Habit Name") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Fitness, Career, Health, Growth)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cue,
                    onValueChange = { cue = it },
                    label = { Text("Habit Stacking Cue (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

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
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.CyberCyan)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (reminder.isBlank()) "Set Daily Reminder Time" else "🔔 Reminder Set: $reminder",
                        color = AppColors.CyberCyan,
                        fontSize = 12.sp
                    )
                }

                CustomAudioPickerRow(
                    customSoundUri = customSoundUri,
                    onSoundSelected = { customSoundUri = it }
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HabitType.entries) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label, fontSize = 11.sp) }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TimeOfDay.entries) { tod ->
                        FilterChip(
                            selected = timeOfDay == tod,
                            onClick = { timeOfDay = tod },
                            label = { Text("${tod.icon} ${tod.label}", fontSize = 11.sp) }
                        )
                    }
                }

                if (type == HabitType.MEASUREMENT || type == HabitType.FOCUS_TIMER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = { Text("Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = step,
                            onValueChange = { step = it },
                            label = { Text("Step") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
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
                            habitToEdit?.id,
                            title,
                            emoji,
                            category,
                            timeOfDay,
                            cue,
                            type,
                            selectedColor,
                            target.toFloatOrNull() ?: 1f,
                            unit,
                            step.toFloatOrNull() ?: 1f,
                            reminder,
                            customSoundUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Habit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CustomMeasurementDialog(
    habit: Habit,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
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
            Button(
                onClick = { onSave(valText.toFloatOrNull() ?: 0f) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================================
// 4. GOAL CREATION & DAILY WORK LOG DIALOGS
// ============================================================================
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
    var deadlineIso by remember {
        mutableStateOf(goalToEdit?.deadlineIso ?: LocalDate.now().plusDays(30).toString())
    }
    var target by remember { mutableStateOf(goalToEdit?.targetValue?.cleanStr() ?: "100") }
    var unit by remember { mutableStateOf(goalToEdit?.unit ?: "hrs") }
    var selectedColor by remember { mutableLongStateOf(goalToEdit?.colorHex ?: 0xFF38BDF8L) }

    val colorChoices = listOf(0xFF38BDF8L, 0xFF34D399L, 0xFFA855F7L, 0xFFF97316L, 0xFFFBBF24L, 0xFFF43F5EL)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = {
            Text(
                text = if (goalToEdit == null) "Set New Goal" else "Edit Goal",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Icon") },
                        modifier = Modifier.width(70.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Goal Title") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why this goal matters / Plan") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val parsed = runCatching { LocalDate.parse(deadlineIso) }
                            .getOrDefault(LocalDate.now().plusDays(30))
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
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.CyberCyan, modifier = Modifier.size(16.dp))
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
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (hrs, %, km)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Career, Fitness, Study)") },
                    modifier = Modifier.fillMaxWidth()
                )

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
                            goalToEdit?.id,
                            title,
                            emoji,
                            category,
                            description,
                            deadlineIso,
                            target.toFloatOrNull() ?: 100f,
                            unit,
                            selectedColor
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Goal", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
        title = {
            Text("${goal.emoji} Log Daily Work", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
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
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.Platinum, modifier = Modifier.size(16.dp))
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
                    onSave(
                        summary,
                        deltaText.toFloatOrNull() ?: 1f,
                        minsText.toIntOrNull() ?: 30,
                        logDate
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = goal.accentColor)
            ) {
                Text("Log Progress", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================================
// 5. MISSION (TASK), HOURLY AUDIT, JOURNAL & VAULT DIALOGS
// ============================================================================
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
    var subtasksRaw by remember {
        mutableStateOf(taskToEdit?.subTasks?.joinToString("\n") { it.title } ?: "")
    }
    var recurring by remember { mutableStateOf(taskToEdit?.isRecurring ?: true) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = {
            Text(
                text = if (taskToEdit == null) "Add Mission" else "Edit Mission",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mission Title") }
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TodoPriority.entries) { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
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
                        Text(
                            text = if (reminder.isBlank()) "🔔 Set Time" else "🔔 $reminder",
                            color = AppColors.CyberCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                CustomAudioPickerRow(
                    customSoundUri = customSoundUri,
                    onSoundSelected = { customSoundUri = it }
                )

                Surface(
                    color = AppColors.MintGlow.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.9.dp, AppColors.MintGlow.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔄 Auto-Renew Every Day",
                                color = AppColors.MintGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Automatically resets tomorrow after completion",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(checked = recurring, onCheckedChange = { recurring = it })
                    }
                }

                OutlinedTextField(
                    value = subtasksRaw,
                    onValueChange = { subtasksRaw = it },
                    label = { Text("Subtasks (one per line)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            taskToEdit?.id,
                            title,
                            category,
                            priority,
                            reminder,
                            mins.toIntOrNull() ?: 25,
                            subtasksRaw.lines(),
                            recurring,
                            customSoundUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MintGlow)
            ) {
                Text("Save Mission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    val fmt = DateTimeFormatter.ofPattern("hh:00 a")
    val rangeLabel = "${start.format(fmt)} - ${start.plusHours(1).format(fmt)}"
    var note by remember { mutableStateOf(existingLog?.activityNote ?: "") }
    var status by remember { mutableStateOf(existingLog?.status ?: HourProductivityStatus.PEAK_FLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        title = {
            Text(
                text = "⏰ Audit Hour: $rangeLabel",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
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
                Text(
                    text = "Productivity Rating (Impacts Score & XP):",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
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
                                Text(
                                    text = st.label,
                                    color = if (selected) st.color else AppColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = st.shortBadge,
                                    color = st.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
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
    val photos = remember {
        mutableStateListOf<String>().apply { entryToEdit?.photoUris?.let { addAll(it) } }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        uris.forEach { photos.add(it.toString()) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                Text(
                    text = if (entryToEdit == null) "Log Sanctuary Reflection" else "Edit Journal Entry",
                    color = AppColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🔥", "🌟", "😊", "🧘", "💪", "🌧️").forEach { e ->
                        FilterChip(
                            selected = mood == e,
                            onClick = { mood = e },
                            label = { Text(e, fontSize = 16.sp) }
                        )
                    }
                }

                Text(
                    text = "Energy Level: ${energy.roundToInt()}/10",
                    color = AppColors.MintGlow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(value = energy, onValueChange = { energy = it }, valueRange = 1f..10f, steps = 8)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline of the Day") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wins,
                    onValueChange = { wins = it },
                    label = { Text("Daily Wins & Gratitude") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    label = { Text("Daily Activities & Reflection Story") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.ElectricViolet)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Memory Photos (${photos.size} attached)", color = AppColors.ElectricViolet)
                }

                OutlinedTextField(
                    value = mistakes,
                    onValueChange = { mistakes = it },
                    label = { Text("What went wrong / Friction?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = actionsRaw,
                    onValueChange = { actionsRaw = it },
                    label = { Text("Actions I must take (1 per line)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pushToTodo, onCheckedChange = { pushToTodo = it })
                    Text("Add these actions to my To-Do List", color = AppColors.TextSecondary, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                entryToEdit?.id,
                                title,
                                mood,
                                energy.roundToInt(),
                                wins,
                                story,
                                mistakes,
                                actionsRaw.lines().filter { it.isNotBlank() },
                                photos.toList(),
                                listOf("Journal"),
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
        title = {
            Text(
                text = if (noteToEdit == null) "Add to Vault" else "Edit Vault Note",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NoteCategory.entries) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author / Topic") },
                        modifier = Modifier.weight(1f)
                    )
                    if (category == NoteCategory.BOOK_SUMMARY) {
                        OutlinedTextField(
                            value = progress,
                            onValueChange = { progress = it },
                            label = { Text("% Read") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Key Principle / Quote") }
                )
                OutlinedTextField(
                    value = takeaways,
                    onValueChange = { takeaways = it },
                    label = { Text("Key Takeaways (1 per line)") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Detailed Summary / Notes") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = flashQ,
                    onValueChange = { flashQ = it },
                    label = { Text("Flashcard Question") }
                )
                OutlinedTextField(
                    value = flashA,
                    onValueChange = { flashA = it },
                    label = { Text("Flashcard Answer") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val fc = if (flashQ.isNotBlank() && flashA.isNotBlank()) {
                            listOf(FlashcardItem(flashQ, flashA))
                        } else {
                            emptyList()
                        }
                        onSave(
                            NotebookEntry(
                                id = noteToEdit?.id ?: UUID.randomUUID().toString(),
                                category = category,
                                title = title,
                                authorOrSubtitle = author,
                                rating = noteToEdit?.rating ?: 5,
                                readingProgress = (progress.toIntOrNull() ?: 100).coerceIn(0, 100),
                                isPinned = noteToEdit?.isPinned ?: false,
                                coverColorHex = noteToEdit?.coverColorHex ?: 0xFFA855F7,
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================================
// 6. SOOTHING FOCUS STUDIO, ANALYTICS & GOOGLE DRIVE BACKUP MODALS
// ============================================================================
@Composable
fun FocusTimerStudioDialog(
    vm: HabitTrackerViewModel,
    onDismiss: () -> Unit
) {
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
                delay(1000L)
                breathTick = (breathTick + 1) % 16
            }
        }
    }

    val phaseIndex = breathTick / 4
    val breathPhase = when (phaseIndex) {
        0 -> "INHALE SLOWLY"
        1 -> "HOLD FULL"
        2 -> "EXHALE GENTLY"
        else -> "HOLD EMPTY"
    }
    val targetOrbScale = if (phaseIndex == 0 || phaseIndex == 1) 1.16f else 0.80f
    val orbScale by animateFloatAsState(
        targetValue = targetOrbScale,
        animationSpec = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
        label = "breathOrb"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AppColors.BgCard,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.2.dp, AppColors.CyberCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text("⏱️ Focus Timer") }
                    )
                    FilterChip(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        label = { Text("🌬️ Box Breathing") }
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (mode == 0) {
                    Text(
                        text = vm.activeFocusTitle.value,
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(16.dp))

                    Box(modifier = Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 10.dp.toPx()
                            drawCircle(color = AppColors.BgCardElevated, style = Stroke(stroke))
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(AppColors.MintGlow, AppColors.CyberCyan, AppColors.ElectricViolet)
                                ),
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
                                fontSize = 35.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (vm.isFocusTimerRunning.value) "FLOW ACTIVE" else "READY",
                                color = AppColors.MintGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = (tot / 60L) == mins.toLong(),
                                onClick = {
                                    vm.startFocusTimer(context, vm.activeFocusTitle.value, mins, vm.activeFocusHabitId.value)
                                },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "SOOTHING AMBIENT AUDIO",
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AmbientSoundMode.entries.forEach { soundMode ->
                            FilterChip(
                                selected = vm.ambientSoundMode.value == soundMode,
                                onClick = { vm.setAmbientSound(soundMode) },
                                label = { Text("${soundMode.icon} ${soundMode.label}", fontSize = 11.sp) }
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
                            Text(
                                text = if (vm.isFocusTimerRunning.value) "Pause" else "Start",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Nervous System Reset (4-4-4-4)",
                        color = AppColors.MintGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(22.dp))

                    Box(modifier = Modifier.size(185.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(145.dp)
                                .graphicsLayer {
                                    scaleX = orbScale
                                    scaleY = orbScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            AppColors.CyberCyan.copy(alpha = 0.35f),
                                            AppColors.ElectricViolet.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .border(2.dp, AppColors.CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${4 - (breathTick % 4)}s",
                                    color = AppColors.TextPrimary,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = breathPhase,
                                    color = AppColors.CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AmbientSoundMode.entries.forEach { soundMode ->
                            FilterChip(
                                selected = vm.ambientSoundMode.value == soundMode,
                                onClick = { vm.setAmbientSound(soundMode) },
                                label = { Text("${soundMode.icon} ${soundMode.label}", fontSize = 11.sp) }
                            )
                        }
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = AppColors.BgDeep,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(0.9.dp, AppColors.BorderSubtle),
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
                        text = "📊 Performance Analytics",
                        color = AppColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.TextSecondary)
                    }
                }

                val best = remember(vm.habits.toList()) {
                    vm.habits.maxOfOrNull { it.bestStreak() } ?: 0
                }
                val avg = remember(vm.habits.toList()) {
                    if (vm.habits.isEmpty()) 0 else vm.habits.map { it.completionRate30Days() }.average().roundToInt()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(Modifier.weight(1f), "🔥", "$best d", "Peak Streak", AppColors.SunsetCoral)
                    StatBox(Modifier.weight(1f), "📈", "$avg%", "30d Rate", AppColors.MintGlow)
                    StatBox(
                        Modifier.weight(1f),
                        "🎯",
                        "${vm.goals.count { it.isCompleted }}/${vm.goals.size}",
                        "Goals Hit",
                        AppColors.CyberCyan
                    )
                }

                vm.habits.forEach { habit ->
                    Surface(
                        color = AppColors.BgCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.8.dp, AppColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${habit.emoji} ${habit.title}",
                                    color = AppColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${habit.completionRate30Days()}% (30d)",
                                    color = habit.accentColor,
                                    fontWeight = FontWeight.Bold
                                )
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
fun StatBox(
    modifier: Modifier,
    icon: String,
    value: String,
    label: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        color = AppColors.BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.4f))
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

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val json = vm.buildBackupJsonNow(context)
            val ok = storage.exportBackupToUri(it, json)
            Toast.makeText(
                context,
                if (ok) "Backup saved to Google Drive!" else "Export failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
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
        title = {
            Text("☁️ Google Drive Backup", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "All habits, goals, daily work logs, Brain Mood preferences, custom sounds, audits, tasks, and notes are securely backed up.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Last Sync: ${vm.lastCloudBackup.value}", color = AppColors.MintGlow, fontSize = 12.sp)

                OutlinedButton(
                    onClick = { exportLauncher.launch("AlphaStudio_v7_Backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.MintGlow)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AppColors.MintGlow)
                    Spacer(Modifier.width(8.dp))
                    Text("Export to Google Drive", color = AppColors.MintGlow)
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AppColors.CyberCyan)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AppColors.CyberCyan)
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
