package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.Song
import com.example.myapplication.ui.components.SongItem

//L’écran principal

//Affiche la liste des musiques
//// - afficher l’état de lecture (play/pause/resume)
// Envoie les actions (play / pause / resume ) vers main
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
    onSeek: (Int) -> Unit //
) {

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

            // LazyColumn = liste scrollable optimisée
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Si aucune musique trouvée
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

                    // Composant SongItem = une ligne musique
                    SongItem(
                        song = song,
                        isCurrentSong = isThisSongPlaying,
                        isPlaying = isPlaying,

                        //  // On affiche le slider seulemnt
                        // pour la chanson en cours
                        currentPosition = if (isThisSongPlaying) currentPosition else 0,
                        totalDuration = if (isThisSongPlaying) totalDuration else 0,

                         // Actions renvoyées à MainActivity
                        onPlayClick = { onPlayClick(song) },
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onSeek = onSeek
                    )
                }
            }
        }
    }
}
