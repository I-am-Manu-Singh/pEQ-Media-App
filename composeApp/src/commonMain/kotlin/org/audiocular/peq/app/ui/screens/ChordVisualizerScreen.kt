package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.AppViewModel

@Composable
fun ChordVisualizerScreen(viewModel: AppViewModel) {
    val currentChord by viewModel.detectedChord
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(16.dp)) {
        Text(
            "HARMONIC VISUALIZER",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "REAL-TIME CHORD & FINGER POSITIONING",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 🎸 GUITAR FRETBOARD
            Column(modifier = Modifier.weight(1f)) {
                Text("GUITAR POSITION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                GuitarFretboard(chord = currentChord ?: "READY")
            }

            // 🎹 PIANO KEYBOARD
            Column(modifier = Modifier.weight(1f)) {
                Text("PIANO POSITION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                PianoKeyboard(chord = currentChord ?: "READY")
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // 📊 HARMONIC DATA
        Surface(
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceAround) {
                HarmonicStat("KEY", viewModel.currentKey.value)
                HarmonicStat("CHORD", currentChord ?: "READY")
                HarmonicStat("BPM", "${viewModel.detectedBpm.value.toInt()}")
                HarmonicStat("SCALE", "Major")
            }
        }
    }
}

@Composable
fun GuitarFretboard(chord: String) {
    Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0xFF2D1B0D))) {
        val fretCount = 5
        val stringCount = 6
        val fretWidth = size.width / fretCount
        val stringHeight = size.height / (stringCount + 1)

        // Draw Frets
        for (i in 0..fretCount) {
            drawLine(
                color = Color(0xFFC0C0C0),
                start = Offset(i * fretWidth, 0f),
                end = Offset(i * fretWidth, size.height),
                strokeWidth = if (i == 0) 8f else 2f
            )
        }

        // Draw Strings
        for (i in 1..stringCount) {
            drawLine(
                color = Color(0xFFD4AF37).copy(alpha = 0.6f),
                start = Offset(0f, i * stringHeight),
                end = Offset(size.width, i * stringHeight),
                strokeWidth = (i * 0.5f) + 1f
            )
        }

        // Draw Fingers (Simplified for demo)
        if (chord != "READY") {
            drawCircle(
                color = Color(0xFFFACC15),
                radius = 12f,
                center = Offset(fretWidth * 1.5f, stringHeight * 3)
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = 12f,
                center = Offset(fretWidth * 2.5f, stringHeight * 4)
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = 12f,
                center = Offset(fretWidth * 2.5f, stringHeight * 5)
            )
        }
    }
}

@Composable
fun PianoKeyboard(chord: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until 14) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.Black)
                        .background(if (chord.startsWith("C") && i == 0) Color(0xFFFACC15) else Color.White)
                )
            }
        }
        // Black Keys
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
            for (i in 0 until 14) {
                val hasBlackKey = i % 7 != 2 && i % 7 != 6
                if (hasBlackKey) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HarmonicStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}
