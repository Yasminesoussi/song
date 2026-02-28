package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.myapplication.model.Song
import com.example.myapplication.ui.components.SongItem
import com.example.myapplication.ui.components.SearchSongDialog

//L’écran principal

//Affiche la liste des musiques
//// - afficher l’état de lecture (play/pause/resume)
// Envoie les actions (play / pause / resume ) vers main
//Permet d’ajouter une musique depuis Internet via un dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    songs: List<Song>,  // // Liste des chansons à afficher
    currentSong: Song?,  // // Chanson actuellement en lecture (ou null)
    isPlaying: Boolean,  // Indique si la musique est en train de jouer
    currentPosition: Int, //
    totalDuration: Int,

    // Actions envoyées à l’Activity
    onPlayClick: (Song) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSeek: (Int) -> Unit,
    onAddExternalSong: (Song) -> Unit,
    onDownloadExternalSong: (String, String) -> Unit,
    onPreviewExternalSong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onSongClick: (Song) -> Unit //
) {

    val showSearch = remember { mutableStateOf(false) }   // afficher le dialog de recherche

    //structure (TopBar + contenu)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Playlist Music",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    ) 
                },
                actions = {
                    Button(
                        onClick = { showSearch.value = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCA28))
                    ) {
                        Text("Ajouter", color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Pour laisser voir le gradient
                )
            )
        }
    ) { paddingValues ->
        // Fond dégradé
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E), // Bleu
                            Color(0xFF000000)  // Noir
                        )
                    )
                )
                .padding(paddingValues)
        ) {



             // Liste scrollable des musiques
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Message si liste vide
                if (songs.isEmpty()) {
                    item {
                        Text(
                            "Aucune musique trouvée. Veuillez en ajouter sur votre appareil.",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }


                // Pour chaque chanson de la liste
                items(songs) { song ->

                    // // Vérifie si cette chanson est celle en cours
                    val isThisSongPlaying = song.id == currentSong?.id

                    // Ligne musique avec toutes les actions
                    SongItem(
                        song = song,
                        isCurrentSong = isThisSongPlaying,
                        isPlaying = isPlaying,
                        currentPosition = if (isThisSongPlaying) currentPosition else 0,
                        totalDuration = if (isThisSongPlaying) totalDuration else 0,

                         // Actions renvoyées à MainActivity
                        onPlayClick = { onPlayClick(song) },
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onSeek = onSeek,
                        onDeleteClick = { onDeleteSong(song) },
                        onDownloadClick = { 
                            val safeTitle = "${song.title} - ${song.artist}".replace("/", "-")
                            onDownloadExternalSong(song.uri.toString(), "$safeTitle.mp3")
                        },
                        onClick = { onSongClick(song) }
                    )
                }
            }


            // Dialog pour rechercher une musique externe
            if (showSearch.value) {
                SearchSongDialog(
                    onClose = { showSearch.value = false },
                    onAddSong = { s -> onAddExternalSong(s) },
                    onDownload = { url, name -> onDownloadExternalSong(url, name) },
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onTogglePreview = { s -> onPreviewExternalSong(s) },
                    onPausePreview = { onPauseClick() }
                )
            }
        }
    }
}
