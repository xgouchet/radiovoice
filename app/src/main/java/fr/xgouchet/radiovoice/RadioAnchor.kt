package fr.xgouchet.radiovoice

import android.content.Context
import android.util.Log
import fr.xgouchet.elmyr.Forge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RadioAnchor(context: Context) {

    private val voice = AnchorVoice(context)
    private var lastTrackAnnounced: Track? = null

    private var lastAnnouncementTimestamp : Long = 0L

    private val announcementDelay = TimeUnit.MINUTES.toMillis(20)

    private val forge = Forge()

    suspend fun handleMusicUpdate(
        track: Track,
        state: TrackState
    ) {

        val previousTrack = lastTrackAnnounced
        Log.i("RadioAnchor", "Received update $track changed to $state")
        if (track == previousTrack) {
            Log.d("RadioAnchor", "$track already announced")
            return
        } else {
            Log.d("RadioAnchor", "$track ≠ $previousTrack")
        }

        val now = System.currentTimeMillis()
        if (lastAnnouncementTimestamp < now - announcementDelay){
            sendGreetings()
        }

        val message = buildMsg(track, previousTrack, state)
        if (message != null) {
            withContext(Dispatchers.Default) {
                voice.queueMessage(message)
            }
            lastTrackAnnounced = track
        }
    }

    private fun sendGreetings() {
        
    }

    private fun buildMsg(
        track: Track,
        previousTrack: Track?,
        state: TrackState
    ): String? {
        return when (state) {
            TrackState.PLAYING -> buildMsgPlaying(track, previousTrack)
            else -> null
        }
    }

    private fun buildMsgPlaying(
        track: Track,
        previousTrack: Track?
    ): String {
        val trackArtist = track.artist.nullIfBlank()
        val trackAlbum = track.album.nullIfBlank()
        val trackTitle = track.getCleanTitle()
        val sameArtist = trackArtist == previousTrack?.artist.nullIfBlank()
        val sameAlbum = trackAlbum == previousTrack?.album.nullIfBlank()

        return when {
            trackTitle == null -> buildMsgPlayingNoTitle(trackArtist, trackAlbum)
            trackArtist == null && trackAlbum == null -> buildMsgPlayingNoSource(trackTitle)
            trackArtist != null && sameArtist -> buildMsgPlayingSameArtist(trackArtist, trackTitle)
            trackAlbum != null && sameAlbum -> buildMsgPlayingSameAlbum(trackAlbum, trackTitle)
            trackArtist == null -> buildMsgPlayingNewAlbum(trackAlbum!!, trackTitle)
            else -> buildMsgPlayingNewArtist(trackArtist, trackTitle)
        }
    }

    private fun buildMsgPlayingNoSource(title: String): String {
        return forge.anElementFrom(
            "And now, we're listening to: $title",
            "Next up: $title",
            "Let's change the mood now, with: $title"
        )
    }

    private fun buildMsgPlayingNoTitle(artist: String?, album: String?): String {
        return if (artist != null && album != null) {
            forge.anElementFrom(
                "Here's a track from: $album; the famous album by $artist.",
                "I just pulled up $album; by $artist.",
                "Let's spend the next few minutes with: $artist, and a track from: $album."
            )
        } else if (artist != null) {
            forge.anElementFrom(
                "Let's change the mood with a few notes by: $artist.",
                "It's now time for some $artist."
            )
        } else if (album != null) {
            forge.anElementFrom(
                "Let's change the mood with a few notes from: $album.",
                "The next track comes straight out of: $album."
            )
        } else {
            forge.anElementFrom(
                "And now, close your eyes and let the music speak to you…",
                "The next one is a surprise… Can you guess what it is?"
            )
        }
    }

    private fun buildMsgPlayingSameArtist(artist: String, title: String): String {
        return forge.anElementFrom(
            "I hope you like $artist, because here's another one of their best songs: $title!",
            "I can't have enough of $artist. Here comes: $title.",
            "That was $artist. And again now, with: $title."
        )
    }

    private fun buildMsgPlayingSameAlbum(album: String, title: String): String {
        return forge.anElementFrom(
            "I could play $album all day long. Hope you don't mind, cause now it's: $title.",
            "And now, let's hear: $title; also from $album."
        )
    }

    private fun buildMsgPlayingNewAlbum(album: String, title: String): String {
        return forge.anElementFrom(
            "Here’s another trip down memory lane with: $title; from $album",
            "We have $album on tap now, with: $title",
            "Next up is a song sure to soothe you. $title, from $album",
            "Let’s change the pace with: $title; from: $album"
        )
    }
    private fun buildMsgPlayingNewArtist(artist: String, title: String): String {
        return forge.anElementFrom(
            "We'll spend the next minutes with $artist, and their hit: $title",
            "Here’s another trip down memory lane with: $title; by $artist",
            "We have $artist on tap now, with: $title",
            "Next up is a song sure to soothe you. $title, by $artist",
            "Let’s change the pace with: $title; by: $artist"
        )
    }

    private fun Track.getCleanTitle(): String? {
        return if (title.isNullOrBlank()) null
        else title.replace(Regex("\\([^)]+\\)"), "")
    }

    private fun String?.nullIfBlank(): String? {
        return if (isNullOrBlank()) null else this
    }
}
