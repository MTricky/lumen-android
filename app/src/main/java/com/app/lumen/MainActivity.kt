package com.app.lumen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                FirebaseTestScreen()
            }
        }
    }
}

@Composable
fun FirebaseTestScreen() {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val dateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    var verseText by remember { mutableStateOf<String?>(null) }
    var verseReference by remember { mutableStateOf<String?>(null) }
    var verseCategory by remember { mutableStateOf<String?>(null) }
    var reflection by remember { mutableStateOf<String?>(null) }

    var liturgyCelebration by remember { mutableStateOf<String?>(null) }
    var liturgySeason by remember { mutableStateOf<String?>(null) }
    var liturgyColor by remember { mutableStateOf<String?>(null) }
    var gospelReference by remember { mutableStateOf<String?>(null) }
    var gospelText by remember { mutableStateOf<String?>(null) }

    var verseStatus by remember { mutableStateOf("Loading daily verse...") }
    var liturgyStatus by remember { mutableStateOf("Loading daily liturgy...") }

    // Fetch daily verse
    LaunchedEffect(Unit) {
        try {
            val doc = firestore.collection("dailyVerse")
                .document(dateString)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.data ?: throw Exception("Empty document")
                verseCategory = data["category"] as? String

                val verses = data["verses"] as? Map<*, *>
                val enVerse = verses?.get("en") as? Map<*, *>
                verseText = enVerse?.get("text") as? String
                verseReference = enVerse?.get("reference") as? String

                val reflections = data["reflection"] as? Map<*, *>
                reflection = reflections?.get("en") as? String

                verseStatus = "Daily Verse — $dateString"
            } else {
                verseStatus = "No verse found for $dateString"
            }
        } catch (e: Exception) {
            verseStatus = "Verse error: ${e.message}"
        }
    }

    // Fetch daily liturgy
    LaunchedEffect(Unit) {
        try {
            val contentDoc = firestore.collection("dailyLiturgy")
                .document(dateString)
                .collection("content")
                .document("en")
                .get()
                .await()

            if (contentDoc.exists()) {
                val data = contentDoc.data ?: throw Exception("Empty document")
                liturgyCelebration = data["celebration"] as? String
                liturgySeason = data["season"] as? String
                liturgyColor = data["liturgicalColor"] as? String

                val readings = data["readings"] as? Map<*, *>
                val gospel = readings?.get("gospel") as? Map<*, *>
                gospelReference = gospel?.get("reference") as? String
                gospelText = gospel?.get("text") as? String

                liturgyStatus = "Daily Liturgy — $dateString"
            } else {
                liturgyStatus = "No liturgy found for $dateString"
            }
        } catch (e: Exception) {
            liturgyStatus = "Liturgy error: ${e.message}"
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Lumen",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Firebase Connection Test",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            // --- Daily Verse Card ---
            SectionCard(title = verseStatus) {
                if (verseText != null) {
                    if (verseCategory != null) {
                        Text(
                            text = verseCategory!!.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBB86FC),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        text = "\"$verseText\"",
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    if (verseReference != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "— $verseReference",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBB86FC)
                        )
                    }

                    if (reflection != null) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF333333))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = reflection!!,
                            fontSize = 14.sp,
                            color = Color(0xFFBBBBBB),
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFBB86FC),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Daily Liturgy Card ---
            SectionCard(title = liturgyStatus) {
                if (liturgyCelebration != null) {
                    if (liturgyColor != null) {
                        val color = when (liturgyColor) {
                            "green" -> Color(0xFF4CAF50)
                            "purple" -> Color(0xFF9C27B0)
                            "white" -> Color.White
                            "red" -> Color(0xFFF44336)
                            "rose" -> Color(0xFFE91E63)
                            else -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, shape = MaterialTheme.shapes.small)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = liturgyCelebration!!,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (liturgySeason != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = liturgySeason!!,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    if (gospelReference != null) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF333333))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Gospel: $gospelReference",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBB86FC)
                        )
                    }

                    if (gospelText != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = gospelText!!,
                            fontSize = 14.sp,
                            color = Color(0xFFBBBBBB),
                            lineHeight = 20.sp,
                            maxLines = 10
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFBB86FC),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "✓ If you see data above, Firebase is working!",
                fontSize = 13.sp,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
