package fr.xgouchet.radiovoice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MusicPlaybackReceiver(
    private val anchor: RadioAnchor
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.w(
            "MusicPlaybackReceiver",
            "received intent ${intent.action} with data:"
        )

        val extras = intent.extras ?: return
        extras.keySet().forEach {
            Log.d("MusicPlaybackReceiver", "- $it: ${extras.get(it)}")
        }

        val artist = extras.getString("artist")
        val album = extras.getString("album")
        val title = extras.getString("track")
        val isPlaying = extras.getBoolean("playing")
        val isPreparing = extras.getBoolean("preparing")

        val state = when {
            isPlaying -> TrackState.PLAYING
            isPreparing -> TrackState.PREPARING
            else -> TrackState.STOPPED
        }

        GlobalScope.launch {
            anchor.handleMusicUpdate(Track(title, artist, album), state)
        }
    }
}