package com.vibrant.habittracker

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// 1. LOW-BATTERY SOUND & SOOTHING AMBIENT SOUNDSCAPE ENGINE
// ============================================================================
object CelebrationSoundEngine {

    private var activeMediaPlayer: MediaPlayer? = null
    private var ambientTrack: AudioTrack? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startAmbientSound(mode: AmbientSoundMode) {
        stopAmbientSound()
        if (mode == AmbientSoundMode.OFF) return

        Thread {
            try {
                val sampleRate = 16000
                val seconds = 3
                val numSamples = sampleRate * seconds
                val buffer = ShortArray(numSamples)

                if (mode == AmbientSoundMode.SOFT_RAIN) {
                    var b0 = 0.0
                    var b1 = 0.0
                    var b2 = 0.0
                    val rng = Random(42)
                    for (i in 0 until numSamples) {
                        val white = rng.nextDouble(-1.0, 1.0)
                        b0 = 0.997 * b0 + white * 0.029
                        b1 = 0.985 * b1 + white * 0.032
                        b2 = 0.950 * b2 + white * 0.048
                        val raw = (b0 + b1 + b2 + white * 0.015) * 0.30
                        buffer[i] = (raw.coerceIn(-0.6, 0.6) * Short.MAX_VALUE).toInt().toShort()
                    }
                } else {
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val wave = 0.14 * sin(2.0 * PI * 216.0 * t) +
                            0.08 * sin(2.0 * PI * 324.0 * t) +
                            0.05 * sin(2.0 * PI * 432.0 * t)
                        buffer[i] = (wave.coerceIn(-0.6, 0.6) * Short.MAX_VALUE).toInt().toShort()
                    }
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.setLoopPoints(0, numSamples, -1)
                track.play()
                ambientTrack = track
            } catch (_: Exception) {
            }
        }.start()
    }

    fun stopAmbientSound() {
        try {
            ambientTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (_: Exception) {
        } finally {
            ambientTrack = null
        }
    }

    fun getAudioFileName(context: Context, uriString: String): String {
        if (uriString.isBlank()) return "Default Studio Sound"
        return try {
            val uri = Uri.parse(uriString)
            var name = "Custom Audio"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: "Custom Audio"
                }
            }
            name
        } catch (_: Exception) {
            "Custom Audio File"
        }
    }

    fun playCustomOrDefaultSound(
        context: Context,
        customSoundUri: String,
        isReminder: Boolean = false,
        isLevelUp: Boolean = false
    ) {
        if (customSoundUri.isNotBlank()) {
            try {
                stopActiveCustomSound()
                triggerHaptics(context, isLevelUp = true)

                val uri = Uri.parse(customSoundUri)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(
                                if (isReminder) AudioAttributes.USAGE_ALARM
                                else AudioAttributes.USAGE_MEDIA
                            )
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, uri)
                    prepare()
                    start()
                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                        } catch (_: Exception) {
                        }
                    }
                }
                activeMediaPlayer = player

                // Auto-stop after 10 seconds to prevent full songs from draining battery
                mainHandler.postDelayed({
                    try {
                        if (player.isPlaying) {
                            player.stop()
                        }
                        player.release()
                    } catch (_: Exception) {
                    }
                }, 10_000L)
                return
            } catch (_: Exception) {
                // Fall back to synthesized studio chime if file is unavailable
            }
        }

        if (isReminder) {
            playMotivationalReminderSound(context)
        } else {
            playCelebrationFanfare(context, isLevelUp = isLevelUp)
        }
    }

    fun stopActiveCustomSound() {
        try {
            activeMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
        } finally {
            activeMediaPlayer = null
        }
    }

    fun playCelebrationFanfare(context: Context, isLevelUp: Boolean = false) {
        triggerHaptics(context, isLevelUp)
        val notes = if (isLevelUp) {
            listOf(
                Triple(523.25, 0.00, 0.36),
                Triple(659.25, 0.07, 0.36),
                Triple(783.99, 0.14, 0.42),
                Triple(1046.50, 0.21, 0.60),
                Triple(1318.51, 0.30, 0.70)
            )
        } else {
            listOf(
                Triple(587.33, 0.00, 0.30),
                Triple(739.99, 0.07, 0.35),
                Triple(880.00, 0.14, 0.40),
                Triple(1174.66, 0.21, 0.65)
            )
        }
        playSynthesizedSequence(
            notes = notes,
            durationSeconds = if (isLevelUp) 1.15 else 0.90
        )
    }

    fun playMotivationalReminderSound(context: Context) {
        triggerHaptics(context, isLevelUp = true)
        val chord = listOf(
            Triple(440.00, 0.00, 0.48),
            Triple(554.37, 0.11, 0.48),
            Triple(659.25, 0.22, 0.52),
            Triple(880.00, 0.33, 0.80),
            Triple(1108.73, 0.46, 0.85)
        )
        playSynthesizedSequence(notes = chord, durationSeconds = 1.35)
    }

    fun playPenaltyDrainSound(context: Context) {
        triggerHaptics(context, isLevelUp = false)
        val drain = listOf(
            Triple(392.00, 0.00, 0.30),
            Triple(349.23, 0.12, 0.30),
            Triple(293.66, 0.24, 0.45)
        )
        playSynthesizedSequence(notes = drain, durationSeconds = 0.75)
    }

    private fun playSynthesizedSequence(
        notes: List<Triple<Double, Double, Double>>,
        durationSeconds: Double
    ) {
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0
                    for ((freq, start, dur) in notes) {
                        if (t >= start && t <= start + dur) {
                            val localT = t - start
                            val attack = (localT / 0.015).coerceAtMost(1.0)
                            val decay = exp(-3.6 * localT)
                            val wave = sin(2.0 * PI * freq * localT) +
                                0.30 * sin(2.0 * PI * freq * 2.0 * localT)
                            sample += wave * attack * decay * 0.26
                        }
                    }
                    buffer[i] = (sample.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
                }

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((durationSeconds * 1000).toLong() + 80)
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    private fun triggerHaptics(context: Context, isLevelUp: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val timings = if (isLevelUp) {
                longArrayOf(0, 35, 35, 55, 35, 95)
            } else {
                longArrayOf(0, 30, 35, 60)
            }
            val amps = if (isLevelUp) {
                intArrayOf(0, 160, 0, 200, 0, 255)
            } else {
                intArrayOf(0, 150, 0, 255)
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } catch (_: Exception) {
        }
    }
}

// ============================================================================
// 2. DOZE-PROOF ALARM SCHEDULER, RECEIVER & BOOT AUTO-RECOVERY
// ============================================================================
object ReminderScheduler {

    private val motivationalQuotes = listOf(
        "Keep your mind sharp—finish today's habits before nightfall.",
        "Every completed rep strengthens your armor. No shortcuts, pure focus.",
        "Your goals depend on today's work block. Start with 2 focused minutes.",
        "Guard the hour. Consistency turns ambition into reality."
    )

    fun scheduleReminder(
        context: Context,
        itemId: String,
        title: String,
        timeStr: String,
        isHabit: Boolean,
        isRecurring: Boolean = isHabit,
        customSoundUri: String = ""
    ) {
        if (timeStr.isBlank() || !timeStr.contains(":")) {
            cancelReminder(context, itemId)
            return
        }
        try {
            val parts = timeStr.split(":")
            val hour = parts[0].trim().toInt().coerceIn(0, 23)
            val minute = parts[1].trim().toInt().coerceIn(0, 59)

            var targetDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
            if (!targetDateTime.isAfter(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val triggerMillis = targetDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            scheduleExactMillis(
                context = context,
                itemId = itemId,
                title = title,
                timeStr = timeStr,
                isHabit = isHabit,
                isRecurring = isRecurring,
                customSoundUri = customSoundUri,
                triggerMillis = triggerMillis
            )
        } catch (_: Exception) {
        }
    }

    fun scheduleTestNotificationInSeconds(context: Context, seconds: Int = 5) {
        val triggerMillis = System.currentTimeMillis() + (seconds * 1000L)
        scheduleExactMillis(
            context = context,
            itemId = "alpha_test_alarm",
            title = "Notification Shield Verified!",
            timeStr = "",
            isHabit = true,
            isRecurring = false,
            customSoundUri = "",
            triggerMillis = triggerMillis
        )
    }

    private fun scheduleExactMillis(
        context: Context,
        itemId: String,
        title: String,
        timeStr: String,
        isHabit: Boolean,
        isRecurring: Boolean,
        customSoundUri: String,
        triggerMillis: Long
    ) {
        val intent = Intent(context, MotivationReminderReceiver::class.java).apply {
            putExtra("EXTRA_ID", itemId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_TIME", timeStr)
            putExtra("EXTRA_IS_HABIT", isHabit)
            putExtra("EXTRA_IS_RECURRING", isRecurring)
            putExtra("EXTRA_SOUND_URI", customSoundUri)
            putExtra("EXTRA_QUOTE", motivationalQuotes.random())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = PendingIntent.getActivity(
            context,
            itemId.hashCode() + 7,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                val info = AlarmManager.AlarmClockInfo(triggerMillis, showAppIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun rescheduleAllSavedAlarms(context: Context): Int {
        val storage = StudioStorageManager(context)
        var count = 0

        storage.loadHabitsOnly().forEach { habit ->
            if (habit.reminderTime.isNotBlank()) {
                scheduleReminder(
                    context = context,
                    itemId = habit.id,
                    title = habit.title,
                    timeStr = habit.reminderTime,
                    isHabit = true,
                    isRecurring = true,
                    customSoundUri = habit.customSoundUri
                )
                count++
            }
        }

        storage.loadTasksOnly().forEach { task ->
            if (task.reminderTime.isNotBlank() && (!task.isCompleted || task.isRecurring)) {
                scheduleReminder(
                    context = context,
                    itemId = task.id,
                    title = task.title,
                    timeStr = task.reminderTime,
                    isHabit = false,
                    isRecurring = task.isRecurring,
                    customSoundUri = task.customSoundUri
                )
                count++
            }
        }
        return count
    }

    fun cancelReminder(context: Context, itemId: String) {
        try {
            val intent = Intent(context, MotivationReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun checkHealth(context: Context): NotificationHealthStatus {
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val storage = StudioStorageManager(context)
        val habitReminders = storage.loadHabitsOnly().count { it.reminderTime.isNotBlank() }
        val taskReminders = storage.loadTasksOnly().count {
            it.reminderTime.isNotBlank() && (!it.isCompleted || it.isRecurring)
        }

        return NotificationHealthStatus(
            notificationsGranted = notifOk,
            exactAlarmsGranted = exactOk,
            batteryUnrestricted = batteryOk,
            scheduledRemindersCount = habitReminders + taskReminders
        )
    }
}

class MotivationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        Thread {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlphaHabit:ReminderWakeLock")
                wakeLock.acquire(12_000L)

                val id = intent.getStringExtra("EXTRA_ID") ?: UUID.randomUUID().toString()
                val title = intent.getStringExtra("EXTRA_TITLE") ?: "Time to Execute!"
                val quote = intent.getStringExtra("EXTRA_QUOTE") ?: "Honor your discipline today."
                val timeStr = intent.getStringExtra("EXTRA_TIME") ?: ""
                val customSoundUri = intent.getStringExtra("EXTRA_SOUND_URI") ?: ""
                val isHabit = intent.getBooleanExtra("EXTRA_IS_HABIT", true)
                val isRecurring = intent.getBooleanExtra("EXTRA_IS_RECURRING", isHabit)

                CelebrationSoundEngine.playCustomOrDefaultSound(
                    context = context,
                    customSoundUri = customSoundUri,
                    isReminder = true
                )

                val channelId = "alpha_studio_alerts_v7"
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Alpha Studio Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Exact habit & mission notifications that survive Doze mode"
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(channel)
                }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    id.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(if (isHabit) "⚡ Alpha Habit: $title" else "🎯 Alpha Mission: $title")
                    .setContentText(quote)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("$quote\nTap to keep your Brain Mood thriving!")
                    )
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                nm.notify(id.hashCode(), notification)
                BrainMoodWidgetProvider.updateAllBrainWidgets(context)

                if (isRecurring && timeStr.isNotBlank()) {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        itemId = id,
                        title = title,
                        timeStr = timeStr,
                        isHabit = isHabit,
                        isRecurring = true,
                        customSoundUri = customSoundUri
                    )
                }

                Thread.sleep(2400L)
            } catch (_: Exception) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }.start()
    }
}

class BootAndAlarmRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            try {
                ReminderScheduler.rescheduleAllSavedAlarms(context)
                HabitWidgetProvider.updateAllWidgets(context)
                TaskWidgetProvider.updateAllTaskWidgets(context)
                BrainMoodWidgetProvider.updateAllBrainWidgets(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

// ============================================================================
// 3. LOCAL JSON STORAGE & GOOGLE DRIVE BACKUP MANAGER
// ============================================================================
class StudioStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("aura_studio_vault_v2", Context.MODE_PRIVATE)

    fun saveFullState(
        xp: Int,
        streakShields: Int,
        claimedQuests: Set<String> = emptySet(),
        googleEmail: String?,
        autoBackup: Boolean,
        readingTheme: SanctuaryReadingTheme = SanctuaryReadingTheme.WARM_SEPIA,
        readingFontScale: Float = 1.0f,
        brainWidgetColorPref: BrainWidgetColorPref = BrainWidgetColorPref.AUTO_DYNAMIC,
        habits: List<Habit>,
        todos: List<TodoTask>,
        goals: List<GoalItem>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): String {
        val root = serializeToJson(
            xp = xp,
            streakShields = streakShields,
            claimedQuests = claimedQuests,
            habits = habits,
            todos = todos,
            goals = goals,
            hourlyLogs = hourlyLogs,
            journals = journals,
            notes = notes
        )

        val compactJson = root.toString()
        val nowFormatted = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

        prefs.edit()
            .putString("master_state_json", compactJson)
            .putString("google_email", googleEmail)
            .putBoolean("auto_backup", autoBackup)
            .putString("reading_theme", readingTheme.name)
            .putFloat("reading_font_scale", readingFontScale)
            .putString("brain_widget_color_pref", brainWidgetColorPref.name)
            .putString("last_saved_time", nowFormatted)
            .apply()

        HabitWidgetProvider.updateAllWidgets(context)
        TaskWidgetProvider.updateAllTaskWidgets(context)
        BrainMoodWidgetProvider.updateAllBrainWidgets(context)

        return compactJson
    }

    fun getSavedJson(): String? = prefs.getString("master_state_json", null)

    fun getGoogleEmail(): String? = prefs.getString("google_email", null)

    fun getLastBackupTime(): String =
        prefs.getString("last_saved_time", "Synced just now") ?: "Synced just now"

    fun isAutoBackup(): Boolean = prefs.getBoolean("auto_backup", true)

    fun getReadingTheme(): SanctuaryReadingTheme {
        val raw = prefs.getString("reading_theme", "WARM_SEPIA") ?: "WARM_SEPIA"
        return runCatching { SanctuaryReadingTheme.valueOf(raw) }
            .getOrDefault(SanctuaryReadingTheme.WARM_SEPIA)
    }

    fun getReadingFontScale(): Float = prefs.getFloat("reading_font_scale", 1.0f)

    fun getBrainWidgetColorPref(): BrainWidgetColorPref {
        val raw = prefs.getString("brain_widget_color_pref", "AUTO_DYNAMIC") ?: "AUTO_DYNAMIC"
        return runCatching { BrainWidgetColorPref.valueOf(raw) }
            .getOrDefault(BrainWidgetColorPref.AUTO_DYNAMIC)
    }

    fun loadHabitsOnly(): List<Habit> {
        val jsonStr = getSavedJson() ?: return emptyList()
        val result = mutableListOf<Habit>()
        try {
            val root = JSONObject(jsonStr)
            val habitsArr = root.optJSONArray("habits") ?: JSONArray()
            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                val logsMap = mutableMapOf<String, Float>()
                val logsObj = o.optJSONObject("logs")
                logsObj?.keys()?.forEach { k ->
                    logsMap[k] = logsObj.optDouble(k, 0.0).toFloat()
                }
                result.add(
                    Habit(
                        id = o.optString("id"),
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
        } catch (_: Exception) {
        }
        return result
    }

    fun loadTasksOnly(): List<TodoTask> {
        val jsonStr = getSavedJson() ?: return emptyList()
        val result = mutableListOf<TodoTask>()
        val todayStr = LocalDate.now().toString()
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: JSONArray()
            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                val isRec = o.optBoolean("isRecurring", false)
                val lastDate = o.optString("lastCompletedDate", "")
                val rawCompleted = o.optBoolean("isCompleted", false)
                val effectiveCompleted = if (
                    isRec && rawCompleted && lastDate.isNotBlank() && lastDate != todayStr
                ) {
                    false
                } else {
                    rawCompleted
                }
                result.add(
                    TodoTask(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category", "Personal"),
                        priority = runCatching { TodoPriority.valueOf(o.optString("priority")) }
                            .getOrDefault(TodoPriority.P2_HIGH),
                        date = o.optString("date", todayStr),
                        reminderTime = o.optString("reminderTime", ""),
                        customSoundUri = o.optString("customSoundUri", ""),
                        isCompleted = effectiveCompleted,
                        isRecurring = isRec,
                        lastCompletedDate = lastDate,
                        renewalStreak = o.optInt("renewalStreak", 0)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun toggleHabitDirectlyFromWidget(habitId: String): Pair<String, String>? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todayKey = LocalDate.now().toString()
            val habitsArr = root.optJSONArray("habits") ?: return null
            var completedHabitTitle: String? = null
            var soundUri = ""

            for (i in 0 until habitsArr.length()) {
                val o = habitsArr.getJSONObject(i)
                if (o.optString("id") == habitId) {
                    val target = o.optDouble("targetValue", 1.0).toFloat()
                    val step = o.optDouble("stepSize", 1.0).toFloat()
                    val type = o.optString("type", "CHECKLIST")
                    soundUri = o.optString("customSoundUri", "")
                    val logsObj = o.optJSONObject("logs") ?: JSONObject()
                    val currentVal = logsObj.optDouble(todayKey, 0.0).toFloat()

                    if (type == "CHECKLIST" || type == "QUIT_SHIELD") {
                        if (currentVal >= target) {
                            logsObj.remove(todayKey)
                        } else {
                            logsObj.put(todayKey, target.toDouble())
                            completedHabitTitle = o.optString("title")
                        }
                    } else {
                        val nextVal = currentVal + step
                        logsObj.put(todayKey, nextVal.toDouble())
                        if (currentVal < target && nextVal >= target) {
                            completedHabitTitle = o.optString("title")
                        }
                    }
                    o.put("logs", logsObj)
                    break
                }
            }

            if (completedHabitTitle != null) {
                root.put("xp", root.optInt("xp", 520) + 35)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return completedHabitTitle?.let { it to soundUri }
        } catch (_: Exception) {
            return null
        }
    }

    fun toggleTaskDirectlyFromWidget(taskId: String): Pair<String, String>? {
        val jsonStr = getSavedJson() ?: return null
        try {
            val root = JSONObject(jsonStr)
            val todosArr = root.optJSONArray("todos") ?: return null
            val todayStr = LocalDate.now().toString()
            var newlyCompletedTitle: String? = null
            var soundUri = ""

            for (i in 0 until todosArr.length()) {
                val o = todosArr.getJSONObject(i)
                if (o.optString("id") == taskId) {
                    val nowDone = !o.optBoolean("isCompleted", false)
                    soundUri = o.optString("customSoundUri", "")
                    o.put("isCompleted", nowDone)
                    if (nowDone) {
                        o.put("lastCompletedDate", todayStr)
                        o.put("renewalStreak", o.optInt("renewalStreak", 0) + 1)
                        newlyCompletedTitle = o.optString("title")
                    }
                    break
                }
            }

            if (newlyCompletedTitle != null) {
                root.put("xp", root.optInt("xp", 520) + 25)
            }
            prefs.edit().putString("master_state_json", root.toString()).apply()
            return newlyCompletedTitle?.let { it to soundUri }
        } catch (_: Exception) {
            return null
        }
    }

    fun exportBackupToUri(uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun serializeToJson(
        xp: Int,
        streakShields: Int,
        claimedQuests: Set<String>,
        habits: List<Habit>,
        todos: List<TodoTask>,
        goals: List<GoalItem>,
        hourlyLogs: List<HourlyLogItem>,
        journals: List<JournalEntry>,
        notes: List<NotebookEntry>
    ): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 9)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("xp", xp)
        root.put("streakShields", streakShields)
        root.put("claimedQuests", JSONArray(claimedQuests.toList()))

        val habitsArr = JSONArray()
        habits.forEach { h ->
            habitsArr.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("emoji", h.emoji)
                    put("category", h.category)
                    put("timeOfDay", h.timeOfDay.name)
                    put("habitStackingCue", h.habitStackingCue)
                    put("type", h.type.name)
                    put("colorHex", h.colorHex)
                    put("targetValue", h.targetValue.toDouble())
                    put("unit", h.unit)
                    put("stepSize", h.stepSize.toDouble())
                    put("reminderTime", h.reminderTime)
                    put("customSoundUri", h.customSoundUri)
                    val logsObj = JSONObject()
                    h.logs.forEach { (k, v) -> logsObj.put(k, v.toDouble()) }
                    put("logs", logsObj)
                }
            )
        }
        root.put("habits", habitsArr)

        val todosArr = JSONArray()
        todos.forEach { t ->
            val subArr = JSONArray()
            t.subTasks.forEach { s ->
                subArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("isDone", s.isDone)
                    }
                )
            }
            todosArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("category", t.category)
                    put("priority", t.priority.name)
                    put("date", t.date)
                    put("timeBlock", t.timeBlock)
                    put("reminderTime", t.reminderTime)
                    put("customSoundUri", t.customSoundUri)
                    put("isCompleted", t.isCompleted)
                    put("isRecurring", t.isRecurring)
                    put("lastCompletedDate", t.lastCompletedDate)
                    put("renewalStreak", t.renewalStreak)
                    put("estimatedMinutes", t.estimatedMinutes)
                    put("subTasks", subArr)
                    put("notes", t.notes)
                }
            )
        }
        root.put("todos", todosArr)

        val goalsArr = JSONArray()
        goals.forEach { g ->
            val workArr = JSONArray()
            g.dailyLogs.forEach { wl ->
                workArr.put(
                    JSONObject().apply {
                        put("id", wl.id)
                        put("date", wl.date)
                        put("workSummary", wl.workSummary)
                        put("progressDelta", wl.progressDelta.toDouble())
                        put("minutesSpent", wl.minutesSpent)
                    }
                )
            }
            goalsArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("emoji", g.emoji)
                    put("category", g.category)
                    put("description", g.description)
                    put("deadlineIso", g.deadlineIso)
                    put("targetValue", g.targetValue.toDouble())
                    put("currentValue", g.currentValue.toDouble())
                    put("unit", g.unit)
                    put("colorHex", g.colorHex)
                    put("dailyLogs", workArr)
                    put("createdAt", g.createdAt)
                }
            )
        }
        root.put("goals", goalsArr)

        val hourlyArr = JSONArray()
        hourlyLogs.forEach { hl ->
            hourlyArr.put(
                JSONObject().apply {
                    put("id", hl.id)
                    put("date", hl.date)
                    put("hour24", hl.hour24)
                    put("activityNote", hl.activityNote)
                    put("status", hl.status.name)
                }
            )
        }
        root.put("hourlyLogs", hourlyArr)

        val journalsArr = JSONArray()
        journals.forEach { j ->
            journalsArr.put(
                JSONObject().apply {
                    put("id", j.id)
                    put("date", j.date)
                    put("isoDate", j.isoDate)
                    put("title", j.title)
                    put("moodEmoji", j.moodEmoji)
                    put("energyScore", j.energyScore)
                    put("gratitudeOrWins", j.gratitudeOrWins)
                    put("dailyStory", j.dailyStory)
                    put("frictionOrMistakes", j.frictionOrMistakes)
                    put("actionItems", JSONArray(j.actionItems))
                    put("photoUris", JSONArray(j.photoUris))
                    put("tags", JSONArray(j.tags))
                }
            )
        }
        root.put("journals", journalsArr)

        val notesArr = JSONArray()
        notes.forEach { n ->
            val fcArr = JSONArray()
            n.flashcards.forEach { f ->
                fcArr.put(
                    JSONObject().apply {
                        put("question", f.question)
                        put("answer", f.answer)
                    }
                )
            }
            notesArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("category", n.category.name)
                    put("title", n.title)
                    put("authorOrSubtitle", n.authorOrSubtitle)
                    put("rating", n.rating)
                    put("readingProgress", n.readingProgress)
                    put("isPinned", n.isPinned)
                    put("coverColorHex", n.coverColorHex)
                    put("favoriteQuote", n.favoriteQuote)
                    put("keyTakeaways", JSONArray(n.keyTakeaways))
                    put("detailedNotes", n.detailedNotes)
                    put("flashcards", fcArr)
                    put("updatedAt", n.updatedAt)
                }
            )
        }
        root.put("notes", notesArr)

        return root
    }
}
