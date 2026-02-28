package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.model.Song
import com.example.myapplication.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

//Le lecteur de musique (le moteur)


// Service Il permet :
//de jouer la musique même si l’app est fermée
//de gérer Play / Pause / Resume
//d’afficher une notification
//de garder l’état de la musique
//Permet d’avancer ou reculer dans la musique
//Indique si la musique joue ou non


class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null  //lit le son

    // État observable pour l'UI : quelle chanson joue et si c'est en pause
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()


    // état lecture (play/pause)
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    companion object {
        const val CHANNEL_ID = "MusicChannel"
        const val NOTIFICATION_ID = 1

        // actions des boutons notification
        const val ACTION_PAUSE = "com.example.myapplication.action.PAUSE"
        const val ACTION_RESUME = "com.example.myapplication.action.RESUME"
        const val ACTION_REWIND = "com.example.myapplication.action.REWIND"
        const val ACTION_FORWARD = "com.example.myapplication.action.FORWARD"
        const val ACTION_TOGGLE_REPEAT = "com.example.myapplication.action.TOGGLE_REPEAT"
        const val ACTION_DISMISS = "com.example.myapplication.action.DISMISS"
    }

    private var repeatMode = false   // mode répétition
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // Classe Binder pour communiquer avec l'Activity
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }


    // Démarrer la lecture d'une chanson
    fun playSong(song: Song) {
        // Si c'est la même chanson, on ignore ou on reprend
        if (_currentSong.value?.id == song.id) {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
                _isPlaying.value = true
                showNotification(song)
            }
            return
        }

        // Arrêter l'ancienne chanson
        mediaPlayer?.release()

        // Préparer la nouvelle chanson
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            // // Chemin du fichier audio
            setDataSource(applicationContext, song.uri)
            prepare()  // Préparer le fichier
            start()      // Démarrer la lecture

            // Quand la musique se termine
            setOnCompletionListener {
                _isPlaying.value = false
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }

        _currentSong.value = song
        _isPlaying.value = true

        // // Afficher la notification
        showNotification(song)
    }



    // Mettre en pause
    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            _currentSong.value?.let { showNotification(it) }
        }
    }



    // Reprendre la lecture
    fun resume() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            _isPlaying.value = true
            _currentSong.value?.let { showNotification(it) }
        }
    }


    // Avancer/reculer dans la musique
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_REWIND -> {
                val pos = getCurrentPosition()
                val newPos = (pos - 10000).coerceAtLeast(0)
                seekTo(newPos)
            }
            ACTION_FORWARD -> {
                val pos = getCurrentPosition()
                val dur = getDuration()
                val newPos = (pos + 10000).coerceAtMost(dur)
                seekTo(newPos)
            }
            ACTION_TOGGLE_REPEAT -> {
                repeatMode = !repeatMode
                mediaPlayer?.isLooping = repeatMode
            }
            ACTION_DISMISS -> {
                pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        _currentSong.value?.let { showNotification(it) }
        return START_STICKY
    }
    // Position actuelle
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    // Obtenir la durée totale d song
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    // Afficher la notification persistante
    private fun showNotification(song: Song) {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply { action = ACTION_PAUSE }, PendingIntent.FLAG_IMMUTABLE)
        val resumeIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply { action = ACTION_RESUME }, PendingIntent.FLAG_IMMUTABLE)
        val rewindIntent = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).apply { action = ACTION_REWIND }, PendingIntent.FLAG_IMMUTABLE)
        val forwardIntent = PendingIntent.getService(this, 4, Intent(this, MusicService::class.java).apply { action = ACTION_FORWARD }, PendingIntent.FLAG_IMMUTABLE)
        val repeatIntent = PendingIntent.getService(this, 5, Intent(this, MusicService::class.java).apply { action = ACTION_TOGGLE_REPEAT }, PendingIntent.FLAG_IMMUTABLE)
        val dismissIntent = PendingIntent.getService(this, 6, Intent(this, MusicService::class.java).apply { action = ACTION_DISMISS }, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lecture en cours")
            .setContentText("${song.title} - ${song.artist}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(false)
            .setSilent(true)
            .setDefaults(0)
            .addAction(android.R.drawable.ic_media_rew, "Reculer", rewindIntent)
            .addAction(
                if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (_isPlaying.value) "Pause" else "Lire",
                if (_isPlaying.value) pauseIntent else resumeIntent
            )
            .addAction(android.R.drawable.ic_media_ff, "Avancer", forwardIntent)
            .addAction(android.R.drawable.ic_menu_rotate, if (repeatMode) "Répéter: ON" else "Répéter: OFF", repeatIntent)
            .setDeleteIntent(dismissIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }


     // Création du canal de notification
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Player Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            serviceChannel.enableVibration(false)
            serviceChannel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }



}
