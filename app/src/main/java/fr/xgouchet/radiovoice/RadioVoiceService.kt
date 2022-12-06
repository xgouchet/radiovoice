package fr.xgouchet.radiovoice

import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.

 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.

 */
class RadioVoiceService : Service() {

    private var musicPlaybackReceiver: BroadcastReceiver? = null
    private var isStarted = false

    // region Service

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        listTTSServices(this).forEach {
            Log.i("RadioVoiceService", "Found engine '$it'")
        }

        return when (intent?.action) {
            ACTION_START_ANCHOR -> handleActionStart()
            ACTION_STOP_ANCHOR -> handleActionStop()
            else -> START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    // endregion

    // region Internal

    private fun handleActionStart(): Int {

        synchronized(this) {
            if (!isStarted) {
                val receiver = createBroadcastReceiver()
                val intentFilter = createIntentFilter()
                registerReceiver(receiver, intentFilter)

                createNotificationChannel()

                startForeground(NOTIFICATION_ID_ANCHOR, createNotification())
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun handleActionStop(): Int {
        musicPlaybackReceiver?.let { unregisterReceiver(it) }
        musicPlaybackReceiver = null

        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val stopServicePendingIntent = PendingIntent.getService(this, 0, buildStopIntent(this), 0)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_mic_24)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopServicePendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Radio On Air",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction("com.android.music.playstatechanged")
        }
    }

    private fun createBroadcastReceiver(): MusicPlaybackReceiver {
        return MusicPlaybackReceiver(RadioAnchor(this))
    }

    private fun listTTSServices(currentContext: Context): List<String> {
        val pm = currentContext.packageManager
        val voiceIntent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val services = pm.queryIntentServices(voiceIntent, PackageManager.GET_META_DATA)

        return services.map { it.serviceInfo.packageName }
    }

    // endregion

    companion object {

        const val NOTIFICATION_CHANNEL_ID = "radio_voice"
        const val NOTIFICATION_ID_ANCHOR = 42

        private const val ACTION_START_ANCHOR = "fr.xgouchet.radiovoice.action.START_ANCHOR"
        private const val ACTION_STOP_ANCHOR = "fr.xgouchet.radiovoice.action.STOP_ANCHOR"

        @JvmStatic
        fun buildStartIntent(context: Context): Intent {
            return Intent(context, RadioVoiceService::class.java).apply {
                action = ACTION_START_ANCHOR
            }
        }

        @JvmStatic
        fun buildStopIntent(context: Context): Intent {
            return Intent(context, RadioVoiceService::class.java).apply {
                action = ACTION_STOP_ANCHOR
            }
        }
    }
}