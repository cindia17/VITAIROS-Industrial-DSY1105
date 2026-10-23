
package com.vitairos.industrial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PantallaCarga(alTerminar: () -> Unit) {

    var progreso by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(Unit) {
        for (numero in 0..100) {
            progreso = numero / 100f
            delay(35)
        }
        alTerminar()
    }

    val animacion = rememberInfiniteTransition(
        label = "Logo"
    )

    val salto by animacion.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Salto"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF061C17))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "VITAIROS INDUSTRIAL",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(40.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val recorrido = maxWidth - 80.dp

            Image(
                painter = painterResource(
                    id = R.drawable.logo_vitairos
                ),
                contentDescription = "Logo VITAIROS",
                modifier = Modifier
                    .offset(
                        x = recorrido * progreso,
                        y = salto.dp
                    )
                    .size(80.dp),
                contentScale = ContentScale.Fit
            )
        }

        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = Color(0xFF00A859),
            trackColor = Color(0xFF285244)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "${(progreso * 100).toInt()}%",
            color = Color.White,
            fontSize = 22.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Iniciando aplicación...",
            color = Color.LightGray
        )
    }
}
