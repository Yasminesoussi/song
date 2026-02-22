package com.example.myapplication.model

import android.net.Uri

// Modèle simple représentant une chanson
data class Song(
    val id: Long,          // identifiant unique
    val title: String,     // titre
    val artist: String,    // nom de l'artiste
    val uri: Uri           // chemin vers le fichier audio
)
