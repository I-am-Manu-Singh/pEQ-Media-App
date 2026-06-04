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
fun ChordVisualizer(
    chord: String, 
    guitarShapes: Map<String, List<Int>> = emptyMap(),
    pianoShapes: Map<String, List<Int>> = emptyMap(),
    modifier: Modifier = Modifier
) {
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
                    GuitarChordWidget(chord, guitarShapes)
                }
                
                // Piano Keyboard
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PIANO (88-KEY)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    PianoChordWidget(chord, pianoShapes)
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
fun GuitarChordWidget(chord: String, shapes: Map<String, List<Int>>) {
    val currentShape = shapes[chord] ?: shapes[chord.replace("#", "s")] ?: listOf(0, 0, 0, 0, 0, 0)

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
fun PianoChordWidget(chord: String, shapes: Map<String, List<Int>>) {
    val activeNotes = shapes[chord] ?: shapes[chord.replace("#", "s")] ?: emptyList()

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
            val x = (note % 12) * kw + kw / 2
            drawCircle(Color.Cyan, 5f, Offset(x, height - 10f))
        }
    }
}