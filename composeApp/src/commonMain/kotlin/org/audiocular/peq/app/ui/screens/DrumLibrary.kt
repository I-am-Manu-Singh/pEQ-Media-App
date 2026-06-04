package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.AppViewModel

@Composable
fun DrumLibrary(viewModel: AppViewModel) {
    val categories = listOf("Kick", "Snare", "Hats", "Clap", "Perc", "Tom")
    var selectedCategory by remember { mutableStateOf("Kick") }
    
    val samples = remember(selectedCategory) {
        // Mocking sample list based on the files found earlier
        when(selectedCategory) {
            "Kick" -> listOf("808", "Acoustic", "Big", "Classic", "Deep", "Dry", "Heavy", "Tape", "Thump")
            "Snare" -> listOf("808", "Analog", "Big", "Block", "Brute", "Electro", "LoFi", "Tape", "Vinyl")
            "Hats" -> listOf("808", "Acoustic", "Analog", "Digital", "Electro", "Plain", "Ring")
            "Clap" -> listOf("808", "Analog", "Crushed", "Fat", "Slapper", "Tape")
            else -> listOf("Analog 1", "Analog 2", "Box", "Digital Noise", "Kung Fu", "Wobble")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(16.dp)) {
        Text(
            "NEURAL DRUM REPOSITORY",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 🎛️ CATEGORY SELECTOR
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 🧠 KIT INTELLIGENCE
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("KIT ANALYTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KitDetailItem("ACTIVE KITS", "4 RACKS")
                    KitDetailItem("SAMPLES", "209 TOTAL")
                    KitDetailItem("ENGINE", "0ms LATENCY")
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(12.dp))
                Text("NEURAL MAPPING: 808, GRIME, LOFI, ANALOG", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 🥁 SAMPLE LIST
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samples) { sample ->
                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sample, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${selectedCategory.uppercase()} • 24-BIT WAV", color = Color.Gray, fontSize = 10.sp)
                        }
                        
                        IconButton(
                            onClick = { viewModel.audioEngine.playDrumSample(selectedCategory.lowercase(), -1) },
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun KitDetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
    }
}

