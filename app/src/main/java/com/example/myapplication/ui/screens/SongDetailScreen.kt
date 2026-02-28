package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.myapplication.model.Song

//Affiche l’écran détails d’une chanson sélectionnée
//Affiche le titre, l’artiste et la durée
//Permet de jouer, mettre en pause, télécharger la musique , reprendre la musique

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    currentPosition: Int,
    totalDuration: Int,
    onPlayClick: (Song) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSeek: (Int) -> Unit,
    onBack: () -> Unit,
    onDownload: (String, String) -> Unit
) {
    val context = LocalContext.current
    var cover by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(song.uri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.uri)
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) {
                cover = BitmapFactory.decodeByteArray(art, 0, art.size)
            } else {
                cover = null
            }
        } catch (_: Exception) {
            cover = null
        }
    }

    // Structure avec top bar et contenu
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Détails du morceau", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A237E), Color(0xFF000000))
                    )
                )
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // Affichage de la pochette ou icône par défaut
            Box(
                modifier = Modifier
                    .size(220.dp)
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFF7F7), Color(0xFFFFCA28))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♫", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


            // Carte avec infos de la chanson
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Titre", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.LightGray)
                    Text(song.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Artiste", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.LightGray)
                    Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Durée", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.LightGray)
                    val dur = song.durationMs ?: totalDuration
                    if (dur > 0) {
                        Text("Durée: " + formatTime(dur), color = Color.LightGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))



            // Boutons play, pause, reprendre et télécharger
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onPlayClick(song) }) { Text("Lire") }
                Button(onClick = onPauseClick, enabled = isCurrent && isPlaying) { Text("Pause") }
                Button(onClick = onResumeClick, enabled = isCurrent && !isPlaying) { Text("Reprendre") }
                IconButton(
                    onClick = {
                        val safeTitle = "${song.title} - ${song.artist}".replace("/", "-")
                        onDownload(song.uri.toString(), "$safeTitle.mp3")
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Text("⬇", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))



            // Slider pour la musique en cours
            if (isCurrent && totalDuration > 0) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFCA28),
                        activeTrackColor = Color(0xFFDFD4D4),
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition), color = Color.White)
                    Text(formatTime(totalDuration), color = Color.White)
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
