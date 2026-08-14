package it.bbnss.moneta.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.RatePoint
import java.time.format.DateTimeFormatter

/**
 * Grafico a linea dell'andamento di una coppia.
 *
 * Disegnato a mano invece che con una libreria: è una spezzata con griglia e
 * assi, e farla qui evita una dipendenza in più da mantenere e da giustificare
 * a F-Droid. Griglia ed etichette ci sono fin da subito — la loro assenza è una
 * delle lamentele aperte sul principale progetto concorrente.
 */
@Composable
fun RateChart(
    allPoints: List<RatePoint>,
    modifier: Modifier = Modifier,
) {
    if (allPoints.size < 2) return

    // Vent'anni di quotazioni giornaliere sono circa cinquemila punti, cioè
    // parecchi per ogni pixel disponibile: si disegna un campione, mentre
    // minimi e massimi restano calcolati sulla serie intera.
    val points = remember(allPoints) { allPoints.downsampleTo(MAX_DRAWN_POINTS) }

    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)

    val values = points.map { it.rate.toDouble() }
    val minValue = values.min()
    val maxValue = values.max()
    // Un margine verticale evita che minimo e massimo tocchino i bordi, dove
    // sarebbero indistinguibili dalla cornice.
    val padding = ((maxValue - minValue) * 0.12).takeIf { it > 0.0 } ?: (maxValue * 0.01 + 1e-9)
    val low = minValue - padding
    val high = maxValue + padding

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val leftInset = 64.dp.toPx()
            val bottomInset = 22.dp.toPx()
            val plotWidth = size.width - leftInset
            val plotHeight = size.height - bottomInset

            fun xAt(index: Int): Float =
                leftInset + plotWidth * index / (points.size - 1).toFloat()

            fun yAt(value: Double): Float =
                (plotHeight * (1 - (value - low) / (high - low))).toFloat()

            drawGrid(
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                gridColor = colors.outlineVariant,
                leftInset = leftInset,
                plotWidth = plotWidth,
                plotHeight = plotHeight,
                low = low,
                high = high,
            )

            val line = Path().apply {
                points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.rate.toDouble())
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            // L'area sotto la linea aiuta a leggere la direzione a colpo
            // d'occhio, che è il motivo per cui si guarda un grafico di cambio.
            val area = Path().apply {
                addPath(line)
                lineTo(xAt(points.size - 1), plotHeight)
                lineTo(leftInset, plotHeight)
                close()
            }

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(colors.primary.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = plotHeight,
                ),
            )

            drawPath(
                path = line,
                color = colors.primary,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Il punto più recente è quello che interessa di più: va marcato.
            drawCircle(
                color = colors.primary,
                radius = 4.dp.toPx(),
                center = Offset(xAt(points.size - 1), yAt(values.last())),
            )

            drawDateLabels(
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                points = points,
                leftInset = leftInset,
                plotWidth = plotWidth,
                plotHeight = plotHeight,
            )
        }
    }
}

private const val MAX_DRAWN_POINTS = 400

/**
 * Riduce la serie a un campione, tenendo sempre il primo e l'ultimo punto:
 * l'inizio e la fine del periodo sono gli estremi che l'utente confronta.
 */
private fun List<RatePoint>.downsampleTo(limit: Int): List<RatePoint> {
    if (size <= limit) return this
    val step = size.toDouble() / (limit - 1)
    val sampled = (0 until limit - 1).map { this[(it * step).toInt()] }
    return sampled + last()
}

private fun DrawScope.drawGrid(
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    gridColor: Color,
    leftInset: Float,
    plotWidth: Float,
    plotHeight: Float,
    low: Double,
    high: Double,
) {
    val lines = 4
    val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

    for (index in 0..lines) {
        val fraction = index / lines.toFloat()
        val y = plotHeight * fraction
        val value = high - (high - low) * fraction

        drawLine(
            color = gridColor,
            start = Offset(leftInset, y),
            end = Offset(leftInset + plotWidth, y),
            strokeWidth = 1f,
            pathEffect = dashed,
        )

        val label = AmountFormat.formatRate(value.toBigDecimal())
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = leftInset - measured.size.width - 8.dp.toPx(),
                y = y - measured.size.height / 2f,
            ),
        )
    }
}

private fun DrawScope.drawDateLabels(
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    points: List<RatePoint>,
    leftInset: Float,
    plotWidth: Float,
    plotHeight: Float,
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yy")
    val first = textMeasurer.measure(points.first().date.format(formatter), labelStyle)
    val last = textMeasurer.measure(points.last().date.format(formatter), labelStyle)

    drawText(
        textLayoutResult = first,
        topLeft = Offset(leftInset, plotHeight + 4.dp.toPx()),
    )
    drawText(
        textLayoutResult = last,
        topLeft = Offset(
            x = leftInset + plotWidth - last.size.width,
            y = plotHeight + 4.dp.toPx(),
        ),
    )
}
