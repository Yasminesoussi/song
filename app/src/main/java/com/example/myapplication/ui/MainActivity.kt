package com.example.myapplication.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.model.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.ui.screens.MusicPlayerScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

//qui relie tout

// - charger les musiques
// - communiquer avec MusicService
// - mettre à jour l’interface Compose

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var isBound = false
    
    // Pour observer l'état du service depuis Compose
    private val currentSong = mutableStateOf<Song?>(null)
    private val isPlaying = mutableStateOf(false)
    private val songsList = mutableStateListOf<Song>()
    
    //  État pour la barre de progression
    private val currentPosition = mutableStateOf(0)
    private val totalDuration = mutableStateOf(0)


    //    // Connexion entre l’Activity et le Service
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    // Gestion de la demande de permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                loadSongs()
            } else {
                // Gérer le cas où l'utilisateur refuse
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Démarrer et lier le service de musique
        Intent(this, MusicService::class.java).also { intent ->
            startService(intent) // Important pour que le service survive à la fermeture
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        checkPermissions()



        // Interface Compose
        setContent {
            MyApplicationTheme {
                // Surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerScreen(
                        songs = songsList,
                        currentSong = currentSong.value,
                        isPlaying = isPlaying.value,
                        currentPosition = currentPosition.value,
                        totalDuration = totalDuration.value,

                        //   // Actions utilisateur
                        onPlayClick = { song -> 
                            musicService?.playSong(song)
                            updateState() // Force update UI state
                        },
                        onPauseClick = { 
                            musicService?.pause() 
                            updateState()
                        },
                        onResumeClick = {
                            musicService?.resume()
                            updateState()
                        },
                        onSeek = { position: Int ->
                            musicService?.seekTo(position)
                            currentPosition.value = position
                        }

                    )
                    
                    //   // Mise à jour automatique de l’UI
                    LaunchedEffect(Unit) {
                        while(true) {
                            updateState()
                            kotlinx.coroutines.delay(500) // Rafraichissement simple
                        }
                    }
                }
            }
        }
    }

    //  // MET À JOUR L'ÉTAT DE L’UI
    private fun updateState() {
        if (isBound && musicService != null) {
            // On lit les valeurs directes des StateFlow
            currentSong.value = musicService!!.currentSong.value
            isPlaying.value = musicService!!.isPlaying.value
            
            // Mise à jour de la progression
            currentPosition.value = musicService!!.getCurrentPosition()
            totalDuration.value = musicService!!.getDuration()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            loadSongs()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    // // CHARGEMENT DES MUSIQUES
    private fun loadSongs() {
        songsList.clear()
        

        try {
            //
            // Musiques de démonstration (raw)
            val resSongs = listOf(
                Song(1001, "Demo Song 1", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song1}")),
                Song(1002, "Demo Song 2", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song2}")),
                Song(1003, "Demo Song 3", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song3}")),
                Song(1004, "Demo Song 4", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song4}")),
                Song(1005, "Demo Song 5", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song5}")),
            )
            songsList.addAll(resSongs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Chercher dans le téléphone (MediaStore)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val contentUri: Uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    songsList.add(Song(id, title, artist, contentUri))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}