package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.CompassData
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.util.QiblaCalculator
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    compassData: CompassData,
    cityName: String,
    qiblaInfo: QiblaCalculator.QiblaInfo,
    isLandscapeOrExpanded: Boolean = false
) {
    var manualOffset by remember { mutableStateOf(0f) }
    var isManualMode by remember { mutableStateOf(false) }

    val effectiveAzimuth = if (isManualMode) manualOffset else compassData.azimuthDegrees
    val targetQibla = qiblaInfo.bearingDegrees
    val relativeAngle = (targetQibla - effectiveAzimuth + 360f) % 360f
    val angleDiff = if (relativeAngle > 180f) 360f - relativeAngle else relativeAngle
    val isAligned = angleDiff <= 4f

    val animatedAzimuth by animateFloatAsState(
        targetValue = effectiveAzimuth,
        animationSpec = spring(stiffness = 300f),
        label = "azimuth"
    )

    val animatedRelative by animateFloatAsState(
        targetValue = relativeAngle,
        animationSpec = spring(stiffness = 300f),
        label = "relative_qibla"
    )

    val glowColor by animateColorAsState(
        targetValue = if (isAligned) EmeraldLight else GoldAccent,
        label = "glow_color"
    )

    if (isLandscapeOrExpanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    azimuth = animatedAzimuth,
                    relativeQiblaAngle = animatedRelative,
                    isAligned = isAligned,
                    glowColor = glowColor
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                QiblaInfoCard(
                    cityName = cityName,
                    qiblaInfo = qiblaInfo,
                    isAligned = isAligned,
                    currentHeading = animatedAzimuth
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalibrationTipsCard(
                    isManualMode = isManualMode,
                    manualOffset = manualOffset,
                    onManualOffsetChange = {
                        manualOffset = it
                        isManualMode = true
                    },
                    onResetManual = { isManualMode = false }
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QiblaInfoCard(
                cityName = cityName,
                qiblaInfo = qiblaInfo,
                isAligned = isAligned,
                currentHeading = animatedAzimuth
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .testTag("qibla_compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    azimuth = animatedAzimuth,
                    relativeQiblaAngle = animatedRelative,
                    isAligned = isAligned,
                    glowColor = glowColor
                )
            }

            CalibrationTipsCard(
                isManualMode = isManualMode,
                manualOffset = manualOffset,
                onManualOffsetChange = {
                    manualOffset = it
                    isManualMode = true
                },
                onResetManual = { isManualMode = false }
            )
        }
    }
}

@Composable
fun CompassDial(
    azimuth: Float,
    relativeQiblaAngle: Float,
    isAligned: Boolean,
    glowColor: Color
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .size(310.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        surfaceVariantColor.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(290.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 14.dp.toPx()

            // Outer rings
            drawCircle(
                color = glowColor.copy(alpha = if (isAligned) 0.5f else 0.2f),
                radius = radius + 6.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Inner circle
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.5f),
                radius = radius * 0.72f,
                center = center
            )

            // Ticks around dial
            val step = 15
            for (angle in 0 until 360 step step) {
                val rad = Math.toRadians((angle - azimuth - 90.0)).toFloat()
                val isMajor = angle % 90 == 0
                val tickLength = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()

                val startX = center.x + (radius - tickLength) * cos(rad)
                val startY = center.y + (radius - tickLength) * sin(rad)
                val endX = center.x + radius * cos(rad)
                val endY = center.y + radius * sin(rad)

                drawLine(
                    color = if (isMajor) glowColor else onSurfaceColor.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth
                )
            }

            // Qibla needle (points to Kaaba relative to phone heading)
            rotate(degrees = relativeQiblaAngle, pivot = center) {
                // Gold / Emerald pointer to Kaaba
                val needlePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.88f)
                    lineTo(center.x + 14.dp.toPx(), center.y - radius * 0.15f)
                    lineTo(center.x, center.y)
                    lineTo(center.x - 14.dp.toPx(), center.y - radius * 0.15f)
                    close()
                }

                drawPath(
                    path = needlePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(glowColor, EmeraldPrimary)
                    )
                )

                // South counter-weight
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius * 0.65f)
                    lineTo(center.x + 9.dp.toPx(), center.y + radius * 0.15f)
                    lineTo(center.x, center.y)
                    lineTo(center.x - 9.dp.toPx(), center.y + radius * 0.15f)
                    close()
                }
                drawPath(
                    path = southPath,
                    color = onSurfaceColor.copy(alpha = 0.25f)
                )
            }

            // Center pivot pin
            drawCircle(
                color = glowColor,
                radius = 8.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Kaaba Icon at the center or aligned state text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 130.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mosque,
                contentDescription = "Кааба",
                tint = glowColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Мекка",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = glowColor
            )
        }
    }
}

@Composable
fun QiblaInfoCard(
    cityName: String,
    qiblaInfo: QiblaCalculator.QiblaInfo,
    isAligned: Boolean,
    currentHeading: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAligned) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isAligned) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EmeraldLight, GoldAccent))) else null
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAligned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Вы направлены точно на Каабу!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Направление Киблы",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${qiblaInfo.bearingDegrees.toInt()}°",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent
                    )
                    Text(
                        text = qiblaInfo.compassDirectionRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Расстояние до Мекки",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${qiblaInfo.distanceKm} км",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "из г. $cityName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CalibrationTipsCard(
    isManualMode: Boolean,
    manualOffset: Float,
    onManualOffsetChange: (Float) -> Unit,
    onResetManual: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Совет по калибровке компаса",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Для наилучшей точности держите телефон горизонтально на ладони вдали от металлических предметов и повращайте его в воздухе в виде знака бесконечности (восьмёрки).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Test / manual rotation slider for emulator or devices without sensor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isManualMode) "Ручной поворот (${manualOffset.toInt()}°)" else "Тест поворота компаса",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isManualMode) {
                    TextButton(onClick = onResetManual) {
                        Text("Автосенсор", fontSize = 11.sp)
                    }
                }
            }
            Slider(
                value = manualOffset,
                onValueChange = onManualOffsetChange,
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
