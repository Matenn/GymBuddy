package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import android.util.Log
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
import androidx.xr.scenecore.Dimensions
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.shape.rounded

import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.*

import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.Bars
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.kaczmarzykmarcin.GymBuddy.R
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties

@Composable
fun ActivityBarChart(
    data: List<ActivityData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wyświetlenia")
        }
        return
    }

    // Get the surface color outside the Canvas
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedBarIndex by remember { mutableStateOf(-1) }
    var markerPosition by remember { mutableStateOf(Offset.Zero) }

    // Define data class for display info
    data class ValueDisplayInfo(val value: Int, val unit: String, val displayValue: String)

    // Convert all values to a common scale (seconds) for chart height calculation
    val chartDataInSeconds = remember(data) {
        data.map { activityData ->
            activityData.copy(minutes = activityData.minutes) // minutes field contains seconds
        }
    }

    // Determine the best unit for each individual value
    val valueDisplayInfo = remember(data) {
        data.map { activityData ->
            val seconds = activityData.minutes // minutes field contains seconds
            if (seconds < 60) {
                ValueDisplayInfo(value = seconds, unit = "s", displayValue = "${seconds}s")
            } else {
                val minutes = (seconds / 60.0).toInt()
                ValueDisplayInfo(value = minutes, unit = "min", displayValue = "${minutes}min")
            }
        }
    }

    // Find what would be the best common unit based on the majority of values AND max value
    val commonUnitIsSeconds = remember(valueDisplayInfo, data) {
        val maxSeconds = data.maxOfOrNull { it.minutes } ?: 0
        val secondsCount = valueDisplayInfo.count { it.unit == "s" }
        val minutesCount = valueDisplayInfo.count { it.unit == "min" }

        // Use seconds only if max value < 300 seconds (5 minutes) AND majority are in seconds
        maxSeconds < 300 && secondsCount >= minutesCount
    }

    // Convert all values to the common unit for consistent chart scaling
    val chartValues = remember(data, commonUnitIsSeconds) {
        data.map { activityData ->
            if (commonUnitIsSeconds) {
                activityData.minutes // Use seconds directly
            } else {
                (activityData.minutes / 60.0).toInt() // Convert to minutes
            }
        }
    }

    val maxValue = chartValues.maxOrNull() ?: 0
    val chartHeight = 140.dp
    val barSpacing = 8.dp
    val density = LocalDensity.current

    // Define margins
    val leftMargin = 50.dp
    val bottomMargin = 30.dp
    val topMargin = 10.dp

    // Determine unit label for Y-axis
    val unitLabel = if (commonUnitIsSeconds) "s" else "min"

    Column(modifier = modifier) {
        // Title
        Text(
            text = "Aktywność treningowa",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(bottom = 8.dp, start = leftMargin)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + topMargin + bottomMargin)
                    .padding(start = leftMargin, bottom = bottomMargin, top = topMargin)
                    .pointerInput(chartValues) { // Add chartValues as key to recompose when data changes
                        detectTapGestures { offset ->
                            // Calculate chart area dimensions
                            val chartAreaWidth = size.width
                            val chartAreaHeight = chartHeight.toPx()

                            // Check if click is within chart area
                            if (offset.x >= 0 && offset.x <= chartAreaWidth &&
                                offset.y >= 0 && offset.y <= chartAreaHeight) {

                                // Calculate bar dimensions
                                val barWidth = if (chartValues.isNotEmpty()) {
                                    (chartAreaWidth - (chartValues.size - 1) * barSpacing.toPx()) / chartValues.size
                                } else {
                                    0f
                                }

                                if (barWidth > 0) {
                                    val totalBarAndSpacing = barWidth + barSpacing.toPx()

                                    // Find which bar was clicked
                                    val clickedIndex = (offset.x / totalBarAndSpacing).toInt()

                                    // Verify the click is actually on a bar (not in spacing)
                                    val barStartX = clickedIndex * totalBarAndSpacing
                                    val barEndX = barStartX + barWidth

                                    if (clickedIndex in chartValues.indices &&
                                        offset.x >= barStartX && offset.x <= barEndX) {

                                        selectedBarIndex = if (selectedBarIndex == clickedIndex) -1 else clickedIndex

                                        if (selectedBarIndex != -1) {
                                            // Calculate marker position relative to the full canvas
                                            val barCenterX = barStartX + barWidth / 2
                                            val barTopY = chartAreaHeight - (chartValues[clickedIndex].toFloat() / maxValue) * chartAreaHeight

                                            markerPosition = Offset(
                                                x = barCenterX + leftMargin.toPx(),
                                                y = barTopY + topMargin.toPx()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val barWidth = if (chartValues.isNotEmpty()) {
                    (size.width - (chartValues.size - 1) * barSpacing.toPx()) / chartValues.size
                } else {
                    0f
                }
                val chartHeightPx = chartHeight.toPx()

                chartValues.forEachIndexed { index, value ->
                    val barHeight = if (maxValue > 0) {
                        (value.toFloat() / maxValue) * chartHeightPx
                    } else 0f

                    val barLeft = index * (barWidth + barSpacing.toPx())
                    val barTop = chartHeightPx - barHeight

                    // Draw bar
                    val barColor = if (selectedBarIndex == index) {
                        Color(0xFF1565C0) // Darker blue when selected
                    } else {
                        Color(0xFF2196F3) // Default blue
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            // Y-axis labels and guidelines - drawn separately to control positioning
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + topMargin + bottomMargin)
            ) {
                val chartHeightPx = chartHeight.toPx()
                val leftMarginPx = leftMargin.toPx()
                val topMarginPx = topMargin.toPx()
                val chartStartX = leftMarginPx
                val chartEndX = size.width

                // Draw Y-axis guidelines and labels
                val guidelines = 5
                for (i in 0..guidelines) {
                    val value = (maxValue * i / guidelines)
                    val y = topMarginPx + chartHeightPx - (i.toFloat() / guidelines) * chartHeightPx

                    // Draw guideline
                    if (i > 0) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(chartStartX, y),
                            end = Offset(chartEndX, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Y-axis label with appropriate unit
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.RIGHT
                            textSize = 10.sp.toPx()
                            color = Color.Gray.toArgb()
                            isAntiAlias = true
                        }

                        drawText(
                            "$value $unitLabel",
                            leftMarginPx - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }
                }
            }

            // X-axis labels - drawn separately
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + topMargin + bottomMargin)
            ) {
                val barWidth = if (data.isNotEmpty()) {
                    ((size.width - leftMargin.toPx()) - (data.size - 1) * barSpacing.toPx()) / data.size
                } else {
                    0f
                }
                val chartHeightPx = chartHeight.toPx()
                val topMarginPx = topMargin.toPx()
                val leftMarginPx = leftMargin.toPx()

                data.forEachIndexed { index, activityData ->
                    val barLeft = leftMarginPx + index * (barWidth + barSpacing.toPx())

                    // Draw label below chart (using original data labels)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 12.sp.toPx()
                            color = Color.Gray.toArgb()
                            isAntiAlias = true
                        }

                        drawText(
                            activityData.label,
                            barLeft + barWidth / 2,
                            topMarginPx + chartHeightPx + 20.dp.toPx(),
                            paint
                        )
                    }
                }
            }

            // Marker popup
            if (selectedBarIndex != -1 && selectedBarIndex < data.size) {
                val selectedData = data[selectedBarIndex]
                val seconds = selectedData.minutes // minutes field contains seconds
                val displayValue = if (commonUnitIsSeconds) {
                    "${seconds}s"
                } else {
                    "${(seconds / 60.0).toInt()}min"
                }

                Card(
                    modifier = Modifier
                        .offset(
                            x = with(density) { markerPosition.x.toDp() - 40.dp },
                            y = with(density) { markerPosition.y.toDp() - 50.dp }
                        )
                        .wrapContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedData.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Triangle pointer
                Canvas(
                    modifier = Modifier
                        .offset(
                            x = with(density) { markerPosition.x.toDp() - 6.dp },
                            y = with(density) { markerPosition.y.toDp() - 12.dp }
                        )
                        .size(12.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = surfaceColor
                    )
                }
            }
        }
    }
}
@Composable
fun ProgressLineChart(
    data: List<ProgressData>,
    selectedExercisesForChart: Set<String>,
    showAllExercisesInChart: Boolean,
    availableExercises: Map<String, String>,
    onToggleExerciseForChart: (String) -> Unit,
    onToggleShowAllExercisesInChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableExercises.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Brak danych do wyświetlenia")
            }
        }
        return
    }

    // Filter data based on selection
    val filteredData = remember(data, showAllExercisesInChart, selectedExercisesForChart) {
        if (showAllExercisesInChart) {
            data
        } else {
            data.filter { selectedExercisesForChart.contains(it.exerciseId) }
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    var selectedPointInfo by remember { mutableStateOf<Pair<String, ProgressPoint>?>(null) }
    var markerPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Progres obciążeń (kg)",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Exercise filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // "All" chip
                item {
                    ExerciseChipFilter(
                        exerciseId = "all",
                        exerciseName = "Wszystkie",
                        isSelected = showAllExercisesInChart,
                        onToggle = onToggleShowAllExercisesInChart
                    )
                }

                // Individual exercise chips
                items(availableExercises.toList()) { (exerciseId, exerciseName) ->
                    ExerciseChipFilter(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        isSelected = selectedExercisesForChart.contains(exerciseId),
                        onToggle = { onToggleExerciseForChart(exerciseId) }
                    )
                }
            }

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (filteredData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak danych dla wybranych ćwiczeń",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    ProgressLineChartCanvas(
                        data = filteredData,
                        onPointClick = { exerciseName, point, position ->
                            if (exerciseName.isEmpty()) {
                                // Empty exerciseName means click in empty area - hide marker
                                selectedPointInfo = null
                            } else {
                                selectedPointInfo = if (selectedPointInfo?.first == exerciseName &&
                                    selectedPointInfo?.second == point) {
                                    null // Deselect if same point clicked
                                } else {
                                    exerciseName to point
                                }
                                markerPosition = position
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Marker popup
                selectedPointInfo?.let { (exerciseName, point) ->
                    Card(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) {
                                    (markerPosition.x).toDp() - 60.dp
                                },
                                y = with(LocalDensity.current) {
                                    (markerPosition.y).toDp() - 70.dp
                                }
                            )
                            .wrapContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                            Text(
                                text = "${point.weight.toInt()} kg",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Triangle pointer
                    Canvas(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { markerPosition.x.toDp() - 6.dp },
                                y = with(LocalDensity.current) { markerPosition.y.toDp() - 12.dp }
                            )
                            .size(12.dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = surfaceColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressLineChartCanvas(
    data: List<ProgressData>,
    onPointClick: (String, ProgressPoint, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF57C00),
        Color(0xFFD32F2F),
        Color(0xFF7B1FA2),
        Color(0xFF607D8B),
        Color(0xFFE91E63),
        Color(0xFF00BCD4),
        Color(0xFF795548),
        Color(0xFF9C27B0)
    )

    // Prepare chart data
    val allPoints = data.flatMap { progressData ->
        progressData.progressPoints.map { point ->
            Triple(progressData.exerciseName, point, progressData.exerciseId)
        }
    }

    if (allPoints.isEmpty()) return

    val maxWeight = allPoints.maxOfOrNull { it.second.weight } ?: 0.0
    val minWeight = allPoints.minOfOrNull { it.second.weight } ?: 0.0
    val weightRange = maxWeight - minWeight

    // Get all unique labels in chronological order
    val allLabels = allPoints.map { it.second.label }.distinct().sorted()

    val leftMargin = 40.dp
    val bottomMargin = 40.dp
    val topMargin = 20.dp
    val rightMargin = 20.dp

    Canvas(
        modifier = modifier
            .padding(
                start = leftMargin,
                bottom = bottomMargin,
                top = topMargin,
                end = rightMargin
            )
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val chartWidth = size.width
                    val chartHeight = size.height

                    if (chartWidth <= 0 || chartHeight <= 0) return@detectTapGestures

                    val labelWidth = chartWidth / (allLabels.size - 1).coerceAtLeast(1)
                    val clickThreshold = 40f // pixels - increased for easier clicking
                    var pointClicked = false

                    // Check each exercise line for clicked points
                    data.forEachIndexed { exerciseIndex, progressData ->
                        progressData.progressPoints.forEach { point ->
                            val labelIndex = allLabels.indexOf(point.label)
                            if (labelIndex != -1) {
                                val x = labelIndex * labelWidth
                                val y = if (weightRange > 0) {
                                    chartHeight - ((point.weight - minWeight) / weightRange) * chartHeight
                                } else {
                                    chartHeight / 2
                                }

                                val distance = sqrt(
                                    (offset.x - x).pow(2) + (offset.y - y.toFloat()).pow(2)
                                )

                                if (distance <= clickThreshold) {
                                    onPointClick(
                                        progressData.exerciseName,
                                        point,
                                        Offset(
                                            x + leftMargin.toPx(),
                                            y.toFloat() + topMargin.toPx()
                                        )
                                    )
                                    pointClicked = true
                                    return@detectTapGestures
                                }
                            }
                        }
                    }

                    // If no point was clicked and there's an active marker, dismiss it
                    if (!pointClicked) {
                        onPointClick("", ProgressPoint(0L, 0.0, 0, ""), Offset.Zero)
                    }
                }
            }
    ) {
        val chartWidth = size.width
        val chartHeight = size.height

        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val labelWidth = if (allLabels.size > 1) chartWidth / (allLabels.size - 1) else 0f

        // Draw grid lines and Y-axis labels
        val guidelines = 5
        for (i in 0..guidelines) {
            val weight = minWeight + (weightRange * i / guidelines)
            val y = chartHeight - (i.toFloat() / guidelines) * chartHeight

            // Draw horizontal guideline
            if (i > 0) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Draw exercise lines
        data.forEachIndexed { exerciseIndex, progressData ->
            val exerciseColor = colors[exerciseIndex % colors.size]
            val points = progressData.progressPoints

            if (points.size > 1) {
                // Draw line segments
                for (i in 0 until points.size - 1) {
                    val currentPoint = points[i]
                    val nextPoint = points[i + 1]

                    val currentLabelIndex = allLabels.indexOf(currentPoint.label)
                    val nextLabelIndex = allLabels.indexOf(nextPoint.label)

                    if (currentLabelIndex != -1 && nextLabelIndex != -1) {
                        val startX = currentLabelIndex * labelWidth
                        val startY = if (weightRange > 0) {
                            chartHeight - ((currentPoint.weight - minWeight) / weightRange) * chartHeight
                        } else {
                            chartHeight / 2
                        }

                        val endX = nextLabelIndex * labelWidth
                        val endY = if (weightRange > 0) {
                            chartHeight - ((nextPoint.weight - minWeight) / weightRange) * chartHeight
                        } else {
                            chartHeight / 2
                        }

                        drawLine(
                            color = exerciseColor,
                            start = Offset(startX, startY.toFloat()),
                            end = Offset(endX, endY.toFloat()),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }

            // Draw points
            points.forEach { point ->
                val labelIndex = allLabels.indexOf(point.label)
                if (labelIndex != -1) {
                    val x = labelIndex * labelWidth
                    val y = if (weightRange > 0) {
                        chartHeight - ((point.weight - minWeight) / weightRange) * chartHeight
                    } else {
                        chartHeight / 2
                    }

                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(), // Increased from 6dp
                        center = Offset(x, y.toFloat())
                    )
                    drawCircle(
                        color = exerciseColor,
                        radius = 6.dp.toPx(), // Increased from 4dp
                        center = Offset(x, y.toFloat())
                    )
                }
            }
        }
    }

    // Draw axes and labels separately
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val chartWidth = size.width - leftMargin.toPx() - rightMargin.toPx()
        val chartHeight = size.height - topMargin.toPx() - bottomMargin.toPx()
        val leftMarginPx = leftMargin.toPx()
        val topMarginPx = topMargin.toPx()
        val bottomMarginPx = bottomMargin.toPx()

        // Draw Y-axis labels
        val guidelines = 5
        for (i in 0..guidelines) {
            val weight = minWeight + (weightRange * i / guidelines)
            val y = topMarginPx + chartHeight - (i.toFloat() / guidelines) * chartHeight

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.RIGHT
                    textSize = 10.sp.toPx()
                    color = Color.Gray.toArgb()
                    isAntiAlias = true
                }

                drawText(
                    "${weight.toInt()} kg",
                    leftMarginPx - 8.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }
        }

        // Draw X-axis labels
        val labelWidth = if (allLabels.size > 1) chartWidth / (allLabels.size - 1) else 0f

        allLabels.forEachIndexed { index, label ->
            val x = leftMarginPx + index * labelWidth

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                    color = Color.Gray.toArgb()
                    isAntiAlias = true
                }

                drawText(
                    label,
                    x,
                    topMarginPx + chartHeight + 25.dp.toPx(),
                    paint
                )
            }
        }
    }
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
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak danych do wyświetlenia")
        }
        return
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    var selectedSegmentIndex by remember { mutableStateOf(-1) }
    var markerPosition by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = modifier) {
        // Title
        Text(
            text = "Rozkład kategorii treningów",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pie Chart
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = minOf(size.width, size.height) / 2f * 0.8f

                                // Calculate distance from center
                                val distance = sqrt(
                                    (offset.x - center.x).pow(2) + (offset.y - center.y).pow(2)
                                )

                                // Check if click is within the pie chart
                                if (distance <= radius) {
                                    // Calculate angle of click relative to center
                                    val angle = atan2(
                                        offset.y - center.y,
                                        offset.x - center.x
                                    ).let {
                                        // Convert to degrees and normalize to 0-360
                                        var degrees = Math.toDegrees(it.toDouble()).toFloat()
                                        if (degrees < 0) degrees += 360f
                                        // Adjust for starting angle (-90 degrees)
                                        degrees = (degrees + 90f) % 360f
                                        degrees
                                    }

                                    // Find which segment was clicked
                                    var currentAngle = 0f
                                    val total = data.sumOf { it.percentage }

                                    for (i in data.indices) {
                                        val segmentAngle = (data[i].percentage.toFloat() / total) * 360f

                                        if (angle >= currentAngle && angle < currentAngle + segmentAngle) {
                                            selectedSegmentIndex = if (selectedSegmentIndex == i) -1 else i

                                            if (selectedSegmentIndex != -1) {
                                                // Calculate marker position at the middle of the segment
                                                val middleAngle = currentAngle + segmentAngle / 2f
                                                val markerRadius = radius * 0.7f
                                                val markerAngleRad = Math.toRadians((middleAngle - 90f).toDouble())

                                                markerPosition = Offset(
                                                    x = center.x + cos(markerAngleRad).toFloat() * markerRadius,
                                                    y = center.y + sin(markerAngleRad).toFloat() * markerRadius
                                                )
                                            }
                                            break
                                        }
                                        currentAngle += segmentAngle
                                    }
                                }
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = minOf(size.width, size.height) / 2f * 0.8f
                    val total = data.sumOf { it.percentage }

                    var startAngle = -90f // Start from top

                    data.forEachIndexed { index, category ->
                        val sweepAngle = (category.percentage.toFloat() / total) * 360f

                        // Parse color with fallback
                        val baseColor = try {
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
                            fallbackColors[index % fallbackColors.size]
                        }

                        // Apply selection effect
                        val segmentColor = if (selectedSegmentIndex == index) {
                            baseColor.copy(alpha = 0.7f) // Lighter when selected
                        } else {
                            baseColor
                        }

                        // Calculate segment radius (expand selected segment)
                        val segmentRadius = if (selectedSegmentIndex == index) {
                            radius * 1.1f
                        } else {
                            radius
                        }

                        // Calculate offset for selected segment (push it outward)
                        val segmentCenter = if (selectedSegmentIndex == index) {
                            val midAngle = startAngle + sweepAngle / 2f
                            val offsetDistance = radius * 0.1f
                            val angleRad = Math.toRadians(midAngle.toDouble())
                            Offset(
                                x = center.x + cos(angleRad).toFloat() * offsetDistance,
                                y = center.y + sin(angleRad).toFloat() * offsetDistance
                            )
                        } else {
                            center
                        }

                        drawArc(
                            color = segmentColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(
                                segmentCenter.x - segmentRadius,
                                segmentCenter.y - segmentRadius
                            ),
                            size = Size(segmentRadius * 2, segmentRadius * 2)
                        )

                        startAngle += sweepAngle
                    }
                }

                // Marker popup
                if (selectedSegmentIndex != -1 && selectedSegmentIndex < data.size) {
                    val selectedData = data[selectedSegmentIndex]

                    Card(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { markerPosition.x.toDp() - 100.dp },
                                y = with(LocalDensity.current) { markerPosition.y.toDp() - 60.dp }
                            )
                            .wrapContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedData.categoryName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${selectedData.count} treningów",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${selectedData.percentage}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Triangle pointer
                    Canvas(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { markerPosition.x.toDp() - 6.dp },
                                y = with(LocalDensity.current) { markerPosition.y.toDp() - 12.dp }
                            )
                            .size(12.dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = surfaceColor
                        )
                    }
                }
            }

            // Legend
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Legenda",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(data.size) { index ->
                        val category = data[index]
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
                            fallbackColors[index % fallbackColors.size]
                        }

                        val isSelected = selectedSegmentIndex == index

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index

                                    if (selectedSegmentIndex != -1) {
                                        // Calculate marker position for legend click
                                        val total = data.sumOf { it.percentage }
                                        var currentAngle = -90f

                                        for (i in 0 until index) {
                                            currentAngle += (data[i].percentage.toFloat() / total) * 360f
                                        }

                                        val segmentAngle = (category.percentage.toFloat() / total) * 360f
                                        val middleAngle = currentAngle + segmentAngle / 2f
                                        val markerRadius = 80f // Approximate radius for 200.dp size
                                        val angleRad = Math.toRadians(middleAngle.toDouble())

                                        markerPosition = Offset(
                                            x = 100f + cos(angleRad).toFloat() * markerRadius * 0.7f, // 100f = center of 200dp
                                            y = 100f + sin(angleRad).toFloat() * markerRadius * 0.7f
                                        )
                                    }
                                }
                                .background(
                                    if (isSelected) Color.Gray.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color, CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                Text(
                                    text = "${category.count} treningów • ${category.percentage}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }
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