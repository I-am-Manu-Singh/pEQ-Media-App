package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChordVisualizer(chord: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEURAL HARMONIC ANALYSIS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // Guitar Fretboard
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GUITAR (EADGBE)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    GuitarChordWidget(chord)
                }
                
                // Piano Keyboard
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PIANO (88-KEY)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    PianoChordWidget(chord)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                chord.uppercase(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GuitarChordWidget(chord: String) {
    // Chord DB: [StringIndex (0-5)] to [Fret (0-5, -1 for mute)]
    val shapes = mapOf(
        "C" to listOf(-1, 3, 2, 0, 1, 0),
        "Cm" to listOf(-1, 3, 5, 5, 4, 3),
        "D" to listOf(-1, -1, 0, 2, 3, 2),
        "Dm" to listOf(-1, -1, 0, 2, 3, 1),
        "E" to listOf(0, 2, 2, 1, 0, 0),
        "Em" to listOf(0, 2, 2, 0, 0, 0),
        "F" to listOf(1, 3, 3, 2, 1, 1),
        "Fm" to listOf(1, 3, 3, 1, 1, 1),
        "G" to listOf(3, 2, 0, 0, 0, 3),
        "Gm" to listOf(3, 5, 5, 3, 3, 3),
        "A" to listOf(-1, 0, 2, 2, 2, 0),
        "Am" to listOf(-1, 0, 2, 2, 1, 0),
        "B" to listOf(-1, 2, 4, 4, 4, 2),
        "Bm" to listOf(-1, 2, 4, 4, 3, 2)
    )

    val currentShape = shapes[chord.replace("#", "s")] ?: listOf(0, 0, 0, 0, 0, 0)

    Canvas(modifier = Modifier.size(80.dp, 100.dp)) {
        val width = size.width
        val height = size.height
        val strings = 6
        val frets = 5
        
        // Strings (Vertical)
        for (i in 0 until strings) {
            val x = (width / (strings - 1)) * i
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
        }
        
        // Frets (Horizontal)
        for (i in 0..frets) {
            val y = (height / frets) * i
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        }
        
        // Nut
        drawLine(Color.White, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 4f)
        
        // Finger Positions
        currentShape.forEachIndexed { sIdx, fret ->
            if (fret > 0) {
                val x = (width / 5) * sIdx
                val y = (height / 5) * (fret - 0.5f)
                drawCircle(Color.Cyan, 8f, Offset(x, y))
            } else if (fret == -1) {
                // Draw X for mute
                val x = (width / 5) * sIdx
                drawLine(Color.Red, Offset(x - 5f, -15f), Offset(x + 5f, -5f), strokeWidth = 2f)
                drawLine(Color.Red, Offset(x + 5f, -15f), Offset(x - 5f, -5f), strokeWidth = 2f)
            } else if (fret == 0) {
                // Open string
                val x = (width / 5) * sIdx
                drawCircle(Color.Green, 4f, Offset(x, -10f))
            }
        }
    }
}

@Composable
fun PianoChordWidget(chord: String) {
    // Notes in chord (semitone offsets from C)
    val chordNotes = mapOf(
        "C" to listOf(0, 4, 7),
        "Cm" to listOf(0, 3, 7),
        "D" to listOf(2, 6, 9),
        "Dm" to listOf(2, 5, 9),
        "E" to listOf(4, 8, 11),
        "Em" to listOf(4, 7, 11),
        "F" to listOf(5, 9, 0),
        "Fm" to listOf(5, 8, 0),
        "G" to listOf(7, 11, 2),
        "Gm" to listOf(7, 10, 2),
        "A" to listOf(9, 1, 4),
        "Am" to listOf(9, 0, 4),
        "B" to listOf(11, 3, 6),
        "Bm" to listOf(11, 2, 6)
    )

    val activeNotes = chordNotes[chord.replace("#", "s")] ?: listOf(0, 4, 7)

    Canvas(modifier = Modifier.size(140.dp, 50.dp)) {
        val width = size.width
        val height = size.height
        val totalKeys = 14
        val kw = width / totalKeys
        
        // White Keys
        for (i in 0 until totalKeys) {
            drawRect(
                color = if (activeNotes.contains(i % 12)) Color.Cyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                topLeft = Offset(i * kw, 0f),
                size = Size(kw - 2, height)
            )
            drawLine(Color.White.copy(alpha = 0.2f), Offset(i * kw, 0f), Offset(i * kw, height))
        }
        
        // Black Keys
        val bw = kw * 0.7f
        val bh = height * 0.6f
        val blackKeyIndices = listOf(0, 1, 3, 4, 5, 7, 8, 10, 11, 12)
        blackKeyIndices.forEach { i ->
            val x = (i + 1) * kw - (bw / 2)
            drawRect(
                color = Color.Black,
                topLeft = Offset(x, 0f),
                size = Size(bw, bh)
            )
        }
        
        // Chord Indicators
        activeNotes.forEach { note ->
            val octaveOffset = if (note < 0) -1 else 0
            val x = (note % 12) * kw + kw / 2
            drawCircle(Color.Cyan, 5f, Offset(x, height - 10f))
        }
    }
}