package it.bbnss.moneta.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun AmountText(text: String, modifier: Modifier = Modifier) {
    val value = text.ifEmpty { "0" }
    val size = when { value.length <= 9 -> 30.sp; value.length <= 14 -> 24.sp; else -> 20.sp }
    Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = size),
        textAlign = TextAlign.End, maxLines = 1,
        modifier = modifier.horizontalScroll(rememberScrollState()))
}
