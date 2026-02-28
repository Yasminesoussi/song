package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Song

// Affiche une seule ligne pour une musique dans la liste
//Montre titre, artiste, durée
//Permet de jouer, mettre en pause ou reprendre , supprimer ou télécharger  la musique

@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    currentPosition: Int = 0,
    totalDuration: Int = 0,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSeek: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onClick: () -> Unit
) {

    // Carte principale de la musique
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) Color(0xFF3949AB) else Color(0xFF282828)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }  // clique sur la carte
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {


            // Icône musique
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEF5350), Color(0xFFFFCA28))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♫",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))


               // Infos musique  : titre, artiste, durée
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (song.artist.isBlank() || song.artist.equals("<unknown>", ignoreCase = true)) "Artiste inconnu" else song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        val dur = song.durationMs ?: 0
                        if (dur > 0) {
                            Text(
                                text = formatTime(dur),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Bouton Play / Pause
                IconButton(
                    onClick = {
                        if (isCurrentSong && isPlaying) {
                            onPauseClick()  // pause si déjà en cours
                        } else if (isCurrentSong && !isPlaying) {
                            onResumeClick()   // reprendre si en pause
                        } else {
                            onPlayClick()   // jouer sinon
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Text(
                        text = if (isCurrentSong && isPlaying) "⏸" else "▶",
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))


                // Bouton supprimer
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEF5350), CircleShape)
                ) {
                    Text("🗑", color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))


                // Bouton télécharger
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Text("⬇", color = Color.Black)
                }
            }



            // Slider pour musique en cours
            if (isCurrentSong && totalDuration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { newValue ->
                        onSeek(newValue.toInt())   // changer position
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFCA28),
                        activeTrackColor = Color(0xFFEF5350),
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = formatTime(totalDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Fonction utilitaire pour formater le temps (mm:ss)
fun formatTime(milliseconds: Int): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
