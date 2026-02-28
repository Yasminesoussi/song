package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import android.net.Uri

//Affiche une fenêtre (dialog) pour chercher des musiques
//Envoie une requête à l’API iTunes
//Récupère une liste de chansons depuis Internet
//Affiche les résultats dans une liste
//Permet d’écouter un aperçu (preview)
//Permet de mettre pause sur le preview
//Permet d’ajouter une musique dans l’application
//Permet de télécharger une musique
//Gère les états (chargement, erreur)
//Ferme la fenêtre quand on clique sur "Fermer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSongDialog(
    onClose: () -> Unit,
    onAddSong: (Song) -> Unit,   // ajouter une musique à la liste
    onDownload: (String, String) -> Unit,  // télécharger une musique
    currentSongId: Long?,
    isPlaying: Boolean,
    onTogglePreview: (Song) -> Unit,  // jouer un aperçu
    onPausePreview: () -> Unit        // pause aperçu
) {
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf(listOf<SearchResult>()) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Titre
                Text(
                    "Ajouter une musique depuis Internet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))


                // Champ pour taper la recherche
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Rechercher un titre ou artiste", color = Color.White) },
                    placeholder = { Text("Tape ta recherche ici", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))


                // Boutons Rechercher et Fermer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (query.isBlank()) return@Button

                            // Lancer recherche
                            scope.launch {
                                isLoading = true
                                errorMsg = null
                                try {
                                    results = searchItunes(query)   // appel API iTunes
                                } catch (e: Exception) {
                                    errorMsg = "Erreur de recherche: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Rechercher")
                    }

                    Button(onClick = onClose) {
                        Text("Fermer")
                    }
                }



                // Affichage état chargement
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chargement...", color = Color.Gray)
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color.Red)
                }
                Spacer(modifier = Modifier.height(12.dp))


                // Liste des résultats
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                // titre et artiste
                                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(item.artist, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                val song = Song(
                                    id = item.id,
                                    title = item.title,
                                    artist = item.artist,
                                    uri = Uri.parse(item.previewUrl),
                                    durationMs = item.durationMs
                                )
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isCurrent = currentSongId != null && song.id == currentSongId
                                        val label = if (isCurrent && isPlaying) "⏸" else "▶"

                                        // Bouton play/pause preview
                                        Button(
                                            onClick = {
                                                if (isCurrent && isPlaying) {
                                                    onPausePreview()
                                                } else {
                                                    onTogglePreview(song)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label)
                                        }

                                        // Bouton ajouter musique
                                        Button(
                                            onClick = { onAddSong(song) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Ajouter")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Bouton télécharger musique
                                    Button(
                                        onClick = {
                                            val safeTitle = "${item.title} - ${item.artist}".replace("/", "-")
                                            onDownload(item.previewUrl, "$safeTitle.mp3")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Télécharger")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// Classe pour stocker résultats API
private data class SearchResult(
    val id: Long,
    val title: String,
    val artist: String,
    val previewUrl: String,
    val durationMs: Int?
)



// Fonction pour chercher musiques sur iTunes
private suspend fun searchItunes(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = URL("https://itunes.apple.com/search?term=$encoded&media=music&limit=25")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
    }
    conn.connect()
    val code = conn.responseCode
    if (code != 200) throw RuntimeException("HTTP $code")
    val body = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()

    val json = JSONObject(body)
    val arr = json.getJSONArray("results")
    val res = mutableListOf<SearchResult>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val preview = obj.optString("previewUrl", "")
        val trackId = obj.optLong("trackId", 0L)
        val trackName = obj.optString("trackName", "Inconnu")
        val artistName = obj.optString("artistName", "Inconnu")
        val timeMs = obj.optInt("trackTimeMillis", 0)
        if (preview.isNotBlank() && trackId != 0L) {
            res.add(SearchResult(trackId, trackName, artistName, preview, durationMs = if (timeMs > 0) timeMs else null))
        }
    }
    res
}
