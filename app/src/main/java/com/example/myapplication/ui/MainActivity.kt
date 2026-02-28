package com.example.myapplication.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.app.DownloadManager
import android.os.Environment
import android.widget.Toast
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import android.media.MediaMetadataRetriever
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
import com.example.myapplication.ui.screens.SongDetailScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

//qui relie tout

// Charge les musiques depuis le téléphone
// Communique avec MusicService pour jouer, mettre en pause, reprendre et suivre la musique
// Gère les téléchargements avec DownloadManager et affiche des notifications
//Permet de supprimer et ajouter des chansons,
//Passe entre l’écran liste (MusicPlayerScreen) et l’écran détail



class MainActivity : ComponentActivity() {
    private var musicService: MusicService? = null
    private var isBound = false
    private val pendingDownloadIds = mutableSetOf<Long>()
    private val prefs: SharedPreferences by lazy { getSharedPreferences("songs_prefs", Context.MODE_PRIVATE) }


    // Recevoir les événements de téléchargement
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (id != -1L) {
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = dm.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val title = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Toast.makeText(this@MainActivity, "Téléchargement terminé: $title", Toast.LENGTH_SHORT).show()
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            Toast.makeText(this@MainActivity, "Échec du téléchargement: $title", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                pendingDownloadIds.remove(id)
            }
        }
    }
    private val currentSong = mutableStateOf<Song?>(null)
    private val isPlaying = mutableStateOf(false)
    private val songsList = mutableStateListOf<Song>()
    private val selectedSong = mutableStateOf<Song?>(null)
    private val currentPosition = mutableStateOf(0)
    private val totalDuration = mutableStateOf(0)


    // Connexion au service
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

    // Gestion des permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                loadSongs()
            } else {
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

        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // Interface Compose
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (selectedSong.value == null) {
                        // Écran principal de lecture
                        MusicPlayerScreen(
                            songs = songsList,
                            currentSong = currentSong.value,
                            isPlaying = isPlaying.value,
                            currentPosition = currentPosition.value,
                            totalDuration = totalDuration.value,

                            onPlayClick = { song -> 
                                musicService?.playSong(song)
                                updateState()
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
                            },
                            onAddExternalSong = { s ->
                                addExternalSong(s)
                            },
                            onDownloadExternalSong = { url, name ->
                                downloadExternalSong(url, name)
                            },
                            onPreviewExternalSong = { s ->
                                musicService?.playSong(s)
                                updateState()
                            },
                            onDeleteSong = { s -> deleteSong(s) },
                            onSongClick = { s -> selectedSong.value = s }
                        )
                    } else {

                        // Écran détails d'une chanson
                        val s = selectedSong.value!!
                        SongDetailScreen(
                            song = s,
                            isCurrent = currentSong.value?.id == s.id,
                            isPlaying = isPlaying.value,
                            currentPosition = currentPosition.value,
                            totalDuration = totalDuration.value,
                            onPlayClick = { song -> 
                                musicService?.playSong(song)
                                updateState()
                            },
                            onPauseClick = { 
                                musicService?.pause() 
                                updateState()
                            },
                            onResumeClick = { 
                                musicService?.resume()
                                updateState()
                            },
                            onSeek = { pos -> 
                                musicService?.seekTo(pos)
                                currentPosition.value = pos
                            },
                            onBack = { selectedSong.value = null },
                            onDownload = { url, name -> downloadExternalSong(url, name) }
                        )
                    }
                    LaunchedEffect(Unit) {
                        while(true) {
                            updateState()
                            kotlinx.coroutines.delay(500)
                        }
                    }
                }
            }
        }
    }

    // Met à jour l'état de l'UI depuis le service
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


    // Vérifie et demande les permissions
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


    // Ajouter une musique externe à la liste
    private fun addExternalSong(song: Song) {
        if (songsList.none { it.id == song.id }) {
            songsList.add(song)
            persistAddedSong(song)    // sauvegarde dans SharedPreferences
        } else {
        }
    }


    // Télécharger une musique
    private fun downloadExternalSong(url: String, filename: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setDescription("Téléchargement de musique")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, filename)

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val id = dm.enqueue(request)
            pendingDownloadIds.add(id)
            Toast.makeText(this, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    // Supprimer une musique
    private fun deleteSong(song: Song) {
        songsList.removeAll { it.id == song.id }
        markDeleted(song.id)
        removePersistedSong(song.id)
        if (selectedSong.value?.id == song.id) {
            selectedSong.value = null
        }
        Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show()
    }

    // Charger les musiques depuis raw et téléphone
    private fun loadSongs() {
        songsList.clear()
        

        try {
            //
            // Musiques de démonstration (raw)
            val resSongs = listOf(
                Song(1001, "Demo Song 1", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song1}"), durationMs = null),
                Song(1002, "Demo Song 2", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song2}"), durationMs = null),
                Song(1003, "Demo Song 3", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song3}"), durationMs = null),
                Song(1004, "Demo Song 4", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song4}"), durationMs = null),
                Song(1005, "Demo Song 5", "SoundHelix", Uri.parse("android.resource://${packageName}/${R.raw.song5}"), durationMs = null),
            )
            songsList.addAll(resSongs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Chercher dans le téléphone (MediaStore)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
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
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    var artist = cursor.getString(artistColumn)
                    val duration = cursor.getInt(durationColumn)
                    val contentUri: Uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(this@MainActivity, contentUri)
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        retriever.release()
                        if (!metaArtist.isNullOrBlank() && !metaArtist.equals("<unknown>", ignoreCase = true)) {
                            artist = metaArtist
                        }
                    } catch (_: Exception) {}
                    songsList.add(Song(id, title, artist, contentUri, durationMs = duration))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val deleted = getDeletedIds()
        if (deleted.isNotEmpty()) {
            songsList.removeAll { deleted.contains(it.id) }
        }

        val persisted = getPersistedAddedSongs()
        if (persisted.isNotEmpty()) {
            val filtered = persisted.filter { s -> !deleted.contains(s.id) && songsList.none { it.id == s.id } }
            songsList.addAll(filtered)
        }
    }

    

    private fun getDeletedIds(): Set<Long> {
        val set = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun markDeleted(id: Long) {
        val set = prefs.getStringSet("deleted_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        prefs.edit().putStringSet("deleted_ids", set).apply()
    }
 
    private fun getPersistedAddedSongs(): List<Song> {
        val json = prefs.getString("added_songs", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optLong("id", 0L)
                val title = obj.optString("title", "Inconnu")
                val artist = obj.optString("artist", "Inconnu")
                val uriStr = obj.optString("uri", "")
                val duration = if (obj.has("durationMs") && !obj.isNull("durationMs")) obj.optInt("durationMs") else null
                if (id != 0L && uriStr.isNotBlank()) {
                    val uri = Uri.parse(uriStr)
                    list.add(Song(id, title, artist, uri, durationMs = duration))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistAddedSong(song: Song) {
        try {
            val existingJson = prefs.getString("added_songs", null)
            val arr = if (existingJson.isNullOrBlank()) JSONArray() else JSONArray(existingJson)
            // Replace if exists
            var replaced = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optLong("id", 0L) == song.id) {
                    obj.put("title", song.title)
                    obj.put("artist", song.artist)
                    obj.put("uri", song.uri.toString())
                    if (song.durationMs != null) obj.put("durationMs", song.durationMs) else obj.remove("durationMs")
                    replaced = true
                    break
                }
            }
            if (!replaced) {
                val obj = JSONObject()
                obj.put("id", song.id)
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("uri", song.uri.toString())
                if (song.durationMs != null) obj.put("durationMs", song.durationMs)
                arr.put(obj)
            }
            prefs.edit().putString("added_songs", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun removePersistedSong(id: Long) {
        try {
            val existingJson = prefs.getString("added_songs", null) ?: return
            val arr = JSONArray(existingJson)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optLong("id", 0L) != id) {
                    newArr.put(obj)
                }
            }
            prefs.edit().putString("added_songs", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

 

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (_: Exception) {}
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
