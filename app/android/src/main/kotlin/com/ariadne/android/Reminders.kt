package com.ariadne.android

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ariadne.app.Reminders
import java.util.Calendar

/**
 * The evening reminder, on the one platform that can keep it while the app is closed.
 *
 * `AlarmManager` with an inexact daily repeat, plus a receiver that posts a
 * notification. Inexact on purpose: an exact alarm needs `SCHEDULE_EXACT_ALARM`, which
 * is a permission for clocks and calendars rather than for "a line at about eight", and
 * a reminder that arrives at 20:03 is not a worse reminder. Nothing about this wakes the
 * device from deep sleep to run code beyond the receiver; it is one notification.
 *
 * Two things are deliberately *not* here:
 *
 *  - **A boot receiver.** Alarms do not survive a reboot, so the reminder is re-armed
 *    the next time the app is opened (`AppModel.start`) instead of asking for
 *    `RECEIVE_BOOT_COMPLETED` and starting work the person did not ask for.
 *  - **A server.** A notification that arrives from the network is a notification that
 *    knows where you are; this one is computed on the phone from a setting.
 */
class AndroidReminders(private val activity: ComponentActivity) : Reminders {

    private var pending: ((Boolean) -> Unit)? = null

    /**
     * Registered here rather than in composition: the launcher has to exist before the
     * activity is started, and the reminder question can be raised from a page long
     * after that.
     */
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val callback = pending
        pending = null
        callback?.invoke(granted)
    }

    override val available: Boolean = true

    override val permitted: Boolean
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Before Android 13 the permission does not exist and notifications are on
            // unless the person turned them off in settings; the app has no way to ask.
            NotificationManagerCompat.from(activity).areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun request(then: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permitted) {
            then(permitted)
            return
        }
        pending = then
        runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            .onFailure {
                pending = null
                then(false)
            }
    }

    override fun schedule(hour: Int, minute: Int): Boolean {
        val alarm = activity.getSystemService(AlarmManager::class.java) ?: return false
        val trigger = nextOccurrence(hour, minute)
        return runCatching {
            alarm.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                trigger,
                AlarmManager.INTERVAL_DAY,
                deliveryIntent(activity),
            )
            true
        }.getOrDefault(false)
    }

    override fun cancel() {
        runCatching {
            activity.getSystemService(AlarmManager::class.java)
                ?.cancel(deliveryIntent(activity))
        }
    }

    companion object {
        const val CHANNEL_ID: String = "evening"

        /** One id for one reminder: setting a new time replaces the old alarm. */
        private const val REQUEST_CODE: Int = 7

        /**
         * The first 20:00 that has not happened yet.
         *
         * Set in the future so the alarm fires this evening rather than immediately: an
         * inexact repeating alarm whose start time is in the past fires at once, which
         * would greet a new setting with a notification about yesterday.
         */
        internal fun nextOccurrence(
            hour: Int,
            minute: Int,
            now: Long = System.currentTimeMillis(),
        ): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
            return calendar.timeInMillis
        }

        internal fun deliveryIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun channel(context: Context): NotificationChannel? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(
                    CHANNEL_ID,
                    "The evening reminder",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description =
                        "One line a day, at the time you chose, about the figures still waiting."
                    setShowBadge(true)
                }
            } else {
                null
            }
    }
}

/**
 * The alarm's other end: one notification, and nothing else.
 *
 * A broadcast receiver rather than a service or a worker, because the work is a
 * notification: no database opens here, no network, no coroutine. If notifications have
 * been switched off since the alarm was set, this does nothing at all rather than
 * trying to be clever about it.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidReminders.channel(context)?.let(manager::createNotificationChannel)
        }

        // Tapping it opens the app on whatever it was last showing. A deep link into
        // Close Day would be one more intent extra to maintain for a gesture nobody
        // asked for; the app opens on Home, which is where the day is closed from.
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = open?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, AndroidReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ariadne_mark)
            .setContentTitle("Close day")
            .setContentText("The day's receipts are waiting for their figures.")
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(REMINDER_ID, notification) }
    }

    private companion object {
        const val REMINDER_ID: Int = 7
    }
}
