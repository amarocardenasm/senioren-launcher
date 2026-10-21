package com.seniorenlauncher.ui.components

import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

/** Cuanto hay que mantener presionado un boton para las opciones (quitar). */
const val LONG_PRESS_MS = 1500L

/**
 * Boton grande del inicio.
 * - Un toque: vibra un poco y, si abre otra app, muestra "Abriendo…" y
 *   no acepta otro toque hasta que la app aparezca (evita abrirla dos veces).
 * - Mantener presionado: un circulo blanco crece desde el dedo hasta llenar
 *   el boton; solo al completarse (1,5 s) se activa la opcion.
 */
@Composable
fun BigButton(
    emoji: String? = null,
    icon: Drawable? = null,
    vectorIcon: ImageVector? = null,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    badge: Int? = null,
    opensOtherApp: Boolean = false
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val holdProgress = remember { Animatable(0f) }
    var pressPoint by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "scale")

    // "Abriendo…" se quita cuando volvemos al launcher o tras unos segundos
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) opening = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(opening) {
        if (opening) {
            delay(4000)
            opening = false
        }
    }

    val shape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = onPress@{ offset ->
                        if (opening) return@onPress
                        pressPoint = offset
                        pressed = true
                        val canHold = currentOnLongClick != null
                        val anim = if (canHold) scope.launch {
                            delay(150) // un toque normal no muestra el circulo
                            holdProgress.animateTo(1f, tween((LONG_PRESS_MS - 150).toInt(), easing = LinearEasing))
                        } else null
                        val released = withTimeoutOrNull(if (canHold) LONG_PRESS_MS else Long.MAX_VALUE) {
                            tryAwaitRelease()
                        }
                        anim?.cancel()
                        pressed = false
                        when {
                            released == null -> {
                                // Mantuvo presionado el tiempo completo
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                currentOnLongClick?.invoke()
                                tryAwaitRelease()
                            }
                            released -> {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (opensOtherApp) opening = true
                                currentOnClick()
                            }
                        }
                        scope.launch { holdProgress.animateTo(0f, tween(200)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            // El icono ocupa lo que sobra y el nombre siempre cabe completo
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val iconMod = Modifier.fillMaxSize(0.8f).aspectRatio(1f)
                when {
                    vectorIcon != null -> Icon(vectorIcon, contentDescription = null, modifier = iconMod, tint = Color.White)
                    icon != null -> Image(
                        painter = rememberAsyncImagePainter(icon),
                        contentDescription = null,
                        modifier = iconMod,
                        contentScale = ContentScale.Fit
                    )
                    emoji != null -> Text(emoji, fontSize = 52.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            AutoSizeText(
                text = label,
                maxFontSize = 20.sp,
                minFontSize = 13.sp,
                withShadow = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Circulo que crece mientras se mantiene presionado
        if (holdProgress.value > 0f) {
            Canvas(Modifier.matchParentSize()) {
                val maxRadius = hypot(size.width, size.height)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = maxRadius * holdProgress.value,
                    center = pressPoint
                )
            }
        }

        // Abriendo la app: oscurece y muestra una ruedita
        if (opening) {
            Box(
                Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Abriendo…", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (badge != null && badge > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, color.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(badge.toString(), fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF1E293B), // Slate 800
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "←", 
                        fontSize = 44.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(24.dp))
            Text(
                title, 
                fontSize = 34.sp,
                fontWeight = FontWeight.Black, 
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                lineHeight = 42.sp
            )
        }
    }
}
