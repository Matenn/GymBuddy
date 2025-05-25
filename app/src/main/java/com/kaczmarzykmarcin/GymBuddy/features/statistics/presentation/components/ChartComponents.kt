package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.Component
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.patrykandpatrick.vico.views.cartesian.CartesianChartView
import kotlin.math.*

@Composable
fun ActivityBarChart(
    data: List<ActivityData>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.map { it.minutes.toFloat() })
            }
        }
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wyświetlenia")
        }
        return
    }

    val columnLayer = rememberColumnCartesianLayer()

    CartesianChartHost(
        chart = CartesianChart(
            columnLayer
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(200.dp)
    )
}

@Composable
fun ProgressLineChart(
    data: List<ProgressData>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                data.forEachIndexed { index, progressData ->
                    series(progressData.progressPoints.map { it.weight.toFloat() })
                }
            }
        }
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wyświetlenia")
        }
        return
    }

    val colors = listOf(
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF57C00),
        Color(0xFFD32F2F),
        Color(0xFF7B1FA2)
    )

    val lineLayer = rememberLineCartesianLayer()

    CartesianChartHost(
        chart = CartesianChart(
            lineLayer
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(200.dp)
    )
}

@Composable
fun SingleExerciseProgressChart(
    data: List<ProgressPoint>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data.map { it.weight.toFloat() })
            }
        }
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wyświetlenia")
        }
        return
    }

    val lineLayer = rememberLineCartesianLayer()

    CartesianChartHost(
        chart = CartesianChart(
            lineLayer
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(200.dp)
    )
}

@Composable
fun CategoryPieChart(
    data: List<CategoryDistribution>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Simple pie chart implementation using Canvas
        val total = data.sumOf { it.percentage }

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2

            var startAngle = 0f

            data.forEachIndexed { index, category ->
                val sweepAngle = (category.percentage / total) * 360f
                val color = try {
                    Color(android.graphics.Color.parseColor(category.categoryColor))
                } catch (e: Exception) {
                    // Fallback colors if parsing fails
                    val fallbackColors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF388E3C),
                        Color(0xFFF57C00),
                        Color(0xFFD32F2F),
                        Color(0xFF7B1FA2),
                        Color(0xFF607D8B)
                    )
                    fallbackColors[index % fallbackColors.size]
                }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }
        }

        // Legend
        LazyColumn(
            modifier = Modifier.height(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(data) { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = try {
                        Color(android.graphics.Color.parseColor(category.categoryColor))
                    } catch (e: Exception) {
                        val fallbackColors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF388E3C),
                            Color(0xFFF57C00),
                            Color(0xFFD32F2F),
                            Color(0xFF7B1FA2),
                            Color(0xFF607D8B)
                        )
                        fallbackColors[data.indexOf(category) % fallbackColors.size]
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${category.categoryName} (${category.percentage}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisePieChart(
    data: List<ExerciseDistribution>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Simple pie chart implementation using Canvas
        val total = data.sumOf { it.percentage }

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2

            var startAngle = 0f

            val colors = listOf(
                Color(0xFF1976D2),
                Color(0xFF388E3C),
                Color(0xFFF57C00),
                Color(0xFFD32F2F),
                Color(0xFF7B1FA2),
                Color(0xFF607D8B)
            )

            data.forEachIndexed { index, exercise ->
                val sweepAngle = (exercise.percentage / total) * 360f
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }
        }

        // Legend
        LazyColumn(
            modifier = Modifier.height(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(data) { exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF388E3C),
                        Color(0xFFF57C00),
                        Color(0xFFD32F2F),
                        Color(0xFF7B1FA2),
                        Color(0xFF607D8B)
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                colors[data.indexOf(exercise) % colors.size],
                                CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${exercise.exerciseName} (${exercise.percentage}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}