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


class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null  //lit le son

    // État observable pour l'UI : quelle chanson joue et si c'est en pause
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    companion object {
        const val CHANNEL_ID = "MusicChannel"
        const val NOTIFICATION_ID = 1
    }

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
            // Mettre à jour la notif pour dire "Paused" si on voulait,
            // mais ici on garde la notif active.
            stopForeground(STOP_FOREGROUND_DETACH) // On peut enlever le foreground si pause
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

        // Quand on clique sur la notif → ouvrir l'app
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lecture en cours")
            .setContentText("${song.title} - ${song.artist}")
            // Petite icône de musique (utiliser une par défaut android si pas de ressource)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Empêche de swiper la notif tant que ça joue
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
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }



}
