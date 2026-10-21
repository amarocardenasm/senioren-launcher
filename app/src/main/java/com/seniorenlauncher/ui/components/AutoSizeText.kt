package com.seniorenlauncher.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Texto que se achica de a poco hasta caber completo, sin cortar palabras
 * a la mitad ("Calenda-rio") ni terminar en "…". Ideal para nombres de apps.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 12.sp,
    maxLines: Int = 2,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Black,
    withShadow: Boolean = false
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var ready by remember(text, maxFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.15f,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        softWrap = true,
        style = if (withShadow) TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 6f)) else TextStyle.Default,
        onTextLayout = { result ->
            if (!fits(result, text) && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1f).sp
            } else {
                ready = true
            }
        }
    )
}

/** Cabe si no se sale del espacio y ninguna palabra quedo partida entre dos lineas. */
private fun fits(result: TextLayoutResult, text: String): Boolean {
    if (result.hasVisualOverflow) return false
    for (line in 0 until result.lineCount - 1) {
        val end = result.getLineEnd(line, visibleEnd = true)
        if (end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace() && text[end - 1] != '-') {
            return false
        }
    }
    return true
}
