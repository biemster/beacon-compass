package com.beaconcompass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beaconcompass.data.TargetLocation
import com.beaconcompass.ui.theme.*
import com.beaconcompass.viewmodel.CompassViewModel
import com.beaconcompass.viewmodel.NavMetrics
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()
    val navMetrics by viewModel.navMetrics.collectAsStateWithLifecycle()

    var showSetupDialog by remember { mutableStateOf(false) }

    // Start tracking when screen is active, and stop when leaving
    DisposableEffect(Unit) {
        viewModel.startSensorTracking()
        onDispose {
            viewModel.stopSensorTracking()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = PitchBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PitchBlack,
                            Color(0xFF030406),
                            Color(0xFF07080F)
                        )
                    )
                )
        ) {
            // Background tactical grid behind everything
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSp = 120.dp.toPx()
                val width = size.width
                val height = size.height
                
                // Draw vertical gridlines
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = Color(0x06FF9F0A),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSp
                }
                
                // Draw horizontal gridlines
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color(0x06FF9F0A),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSp
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BEACON COMPASS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberSecondary,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Coordinate Status Indicators (OLED Night vision amber)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (sensorState.isGpsActive) Color(0x1532D74B) else Color(0x15FF453A),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val hasFix = sensorState.userLatitude != null
                                val statusColor = when {
                                    hasFix -> AlertGreen
                                    sensorState.isGpsActive -> AmberPrimary
                                    else -> AlertRed
                                }
                                val statusText = when {
                                    hasFix -> "GPS LOCKED"
                                    sensorState.isGpsActive -> "GPS SEARCHING..."
                                    else -> "GPS OFFLINE"
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            statusColor,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        sensorState.gpsAccuracyMeters?.let { accuracy ->
                            Text(
                                text = "ACCU: ±${accuracy.toInt()}m",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DimMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Small elegant heading status pill (Degrees indicator taken out the middle)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderGrey, RoundedCornerShape(20.dp))
                        .background(Color(0xFF0C0C0E))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEADING: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DimMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "${sensorState.deviceHeading.toInt()}° ${getCardinalDirection(sensorState.deviceHeading)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AmberPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Core Canvas Compass Presentation (Middle area - completely clean inside now)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Core compass mechanism drawing (Compass subtle, Needle prominent)
                    CompassHomingDial(
                        deviceHeading = sensorState.deviceHeading,
                        targetBearing = if (navMetrics.hasTarget) navMetrics.bearing else null,
                        modifier = Modifier
                            .testTag("compass_dial")
                            .fillMaxSize(0.9f)
                            .aspectRatio(1f)
                    )
                }

                // Navigation Metrics / Homing Beacon Stats (Bottom Area)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardGrey)
                        .border(1.dp, BorderGrey, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side Content (takes up weight 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        if (navMetrics.hasTarget) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Row 1: Beacon Spot Label compacted
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Beacon Spot Icon",
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = navMetrics.targetLabel.uppercase(Locale.ROOT),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DimWhite,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = "BEACON SPOT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = DimMuted,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp
                                        )
                                    )
                                }

                                // Row 2: Distance to Beacon Spot compacted
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Distance Icon",
                                            tint = AmberSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (navMetrics.isGpsAcquiring) {
                                            Text(
                                                text = "RESOLVING SIGNAL...",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = AlertRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        } else {
                                            val (formattedDistance, unit) = formatNavDistance(navMetrics.distanceMeters)
                                            Text(
                                                text = "$formattedDistance $unit",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = AmberPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "DISTANCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = DimMuted,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp
                                        )
                                    )
                                }

                                // Row 3: Bearing to Beacon Spot compacted
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Bearing Icon",
                                            tint = AmberSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${navMetrics.bearing.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DimWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = "BEARING TO SPOT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = DimMuted,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            // Helpful offline setup notice for True North fallback
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Beacon Pointer",
                                        tint = DimMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TRUE NORTH FALLBACK",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DimWhite,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap the manage spots button on the right to set search beacon.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DimMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    // Compact "Manage pinned spots" button aligned on the right
                    OutlinedButton(
                        onClick = { showSetupDialog = true },
                        modifier = Modifier
                            .testTag("set_target_button")
                            .size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = AmberPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C3005))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Manage Pinned Spots Icon",
                            tint = AmberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog containing custom preset targets creator and list of previously saved Room landmarks
    if (showSetupDialog) {
        DestinationSetupDialog(
            currentLat = sensorState.userLatitude,
            currentLng = sensorState.userLongitude,
            locations = allLocations,
            onSetCurrent = { name ->
                viewModel.saveCurrentLocationAsTarget(name)
                showSetupDialog = false
            },
            onSetManual = { name, lat, lng ->
                viewModel.saveManualTarget(name, lat, lng)
                showSetupDialog = false
            },
            onSelectLocation = { id ->
                viewModel.selectActiveLocation(id)
                showSetupDialog = false
            },
            onDeleteLocation = { loc ->
                viewModel.deleteLocation(loc)
            },
            onDismiss = { showSetupDialog = false }
        )
    }
}

/**
 * Custom-drawn high-end compass dial.
 * Fully simulated aircraft Instrument styling with absolute dark tactical color pairing.
 */
@Composable
fun CompassHomingDial(
    deviceHeading: Float,
    targetBearing: Float?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width.coerceAtMost(size.height) / 2f
        
        // 1. Draw static background circular bounds & tick markers (representing compass casing frame)
        drawCircle(
            color = Color(0xFF0C0C0E),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color(0xFF1E1E22),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
        )

        // 2. Outer Rotating Ring (rotated by -deviceHeading to position user's bearing at the top)
        rotate(degrees = -deviceHeading, pivot = center) {
            // Circle track
            drawCircle(
                color = Color(0xFF161619),
                radius = radius - 24.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
            )

            // Draw Lensatic Compass Degree ticks with increased visibility
            for (angle in 0 until 360 step 5) {
                val isMajor = angle % 30 == 0
                val tickLength = if (isMajor) 10.dp.toPx() else 4.dp.toPx()
                val tickThickness = if (isMajor) 1.5f else 0.8f
                val tickColor = if (isMajor) DimMuted.copy(alpha = 0.5f) else DimMuted.copy(alpha = 0.18f)

                val rad = Math.toRadians(angle.toDouble())
                val startDist = radius - 24.dp.toPx()
                val start = Offset(
                    (center.x + startDist * sin(rad)).toFloat(),
                    (center.y - startDist * cos(rad)).toFloat()
                )
                val endOffset = startDist + tickLength
                val end = Offset(
                    (center.x + endOffset * sin(rad)).toFloat(),
                    (center.y - endOffset * cos(rad)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = tickThickness
                )
            }

            // Draw major cardinal letter nodes N, E, S, W inside the rotatable dial with highly prominent, glowing styling
            drawCardinalText(center, radius - 44.dp.toPx(), "N", 0f, AmberPrimary)
            drawCardinalText(center, radius - 44.dp.toPx(), "E", 90f, DimWhite)
            drawCardinalText(center, radius - 44.dp.toPx(), "S", 180f, DimWhite)
            drawCardinalText(center, radius - 44.dp.toPx(), "W", 270f, DimWhite)
        }

        // 3. Draw static Top Center cursor marker indicating where the user's phone is index pointing
        val tipPath = Path().apply {
            moveTo(center.x, center.y - radius + 4.dp.toPx())
            lineTo(center.x - 5.dp.toPx(), center.y - radius + 12.dp.toPx())
            lineTo(center.x + 5.dp.toPx(), center.y - radius + 12.dp.toPx())
            close()
        }
        drawPath(
            path = tipPath,
            color = AmberSecondary.copy(alpha = 0.6f)
        )

        // 4. Draw Center Homing Needle Arrow (Extends significantly beyond the compass ring)
        val needleRotation = if (targetBearing != null) {
            targetBearing - deviceHeading
        } else {
            0f - deviceHeading
        }

        rotate(degrees = needleRotation, pivot = center) {
            // Draw dual-ended pointer needle
            val needleWidth = 14.dp.toPx()
            val needleHeight = radius * 0.95f // Extends significantly beyond the inner ring (at radius - 24.dp)
            
            val baseColor = if (targetBearing != null) AmberPrimary else AlertGreen
            val rightColor = baseColor
            val leftColor = baseColor.copy(alpha = 0.65f) // shaded left side for 3D faceted look
            
            // Long elegant pointer: Shaded Left
            val northLeft = Path().apply {
                moveTo(center.x, center.y - needleHeight)
                lineTo(center.x - needleWidth / 2f, center.y - 12.dp.toPx())
                lineTo(center.x, center.y - 12.dp.toPx())
                close()
            }
            // Long elegant pointer: Bright Right
            val northRight = Path().apply {
                moveTo(center.x, center.y - needleHeight)
                lineTo(center.x + needleWidth / 2f, center.y - 12.dp.toPx())
                lineTo(center.x, center.y - 12.dp.toPx())
                close()
            }
            
            // Speedometer-style short counter-weight extension on the South side
            val tailWidth = needleWidth * 0.4f
            val tailLength = radius * 0.3f // short tail weight
            
            val southLeft = Path().apply {
                moveTo(center.x, center.y + tailLength)
                lineTo(center.x - tailWidth, center.y + 12.dp.toPx())
                lineTo(center.x, center.y + 12.dp.toPx())
                close()
            }
            val southRight = Path().apply {
                moveTo(center.x, center.y + tailLength)
                lineTo(center.x + tailWidth, center.y + 12.dp.toPx())
                lineTo(center.x, center.y + 12.dp.toPx())
                close()
            }

            // Paint the 3D North arrow
            drawPath(path = northLeft, color = leftColor)
            drawPath(path = northRight, color = rightColor)
            
            // Paint the tactical silver/grey Speedometer south counterweight
            drawPath(path = southLeft, color = Color(0xFF323236))
            drawPath(path = southRight, color = Color(0xFF48484E))

            // Central golden/green concentric compass hub bezel
            drawCircle(
                color = PitchBlack,
                radius = 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = baseColor,
                radius = 10.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
            )
            drawCircle(
                color = PitchBlack,
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * Custom drawing logic helper to draw text into canvas elements
 */
private fun DrawScope.drawCardinalText(
    center: Offset,
    offsetRadius: Float,
    text: String,
    angleDegrees: Float,
    color: Color
) {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val textX = (center.x + offsetRadius * sin(rad)).toFloat()
    val textY = (center.y - offsetRadius * cos(rad)).toFloat()

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            this.textSize = 15.dp.toPx()
            this.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            this.textAlign = android.graphics.Paint.Align.CENTER
            // Apply glowing neon shadow layer around the letters
            this.setShadowLayer(
                10f, // blur radius
                0f,  // dx
                0f,  // dy
                android.graphics.Color.argb(
                    (color.alpha * 200).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
            )
        }
        drawText(
            text,
            textX,
            textY + paint.textSize / 3f, // vertical align center correction
            paint
        )
    }
}

/**
 * Returns cardinal representation for degree readings
 */
fun getCardinalDirection(heading: Float): String {
    val index = ((heading + 22.5) / 45).toInt() % 8
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[index]
}

/**
 * Format meters distance to professional hiking notation. Values under 1000m return in meters,
 * values at/above 1000m return in standard kilometers format.
 */
fun formatNavDistance(meters: Float): Pair<String, String> {
    return if (meters < 1000f) {
        Pair("${meters.toInt()}", "m")
    } else {
        val kms = meters / 1000f
        if (kms >= 100f) {
            Pair(String.format(Locale.ROOT, "%.0f", kms), "km")
        } else {
            Pair(String.format(Locale.ROOT, "%.2f", kms), "km")
        }
    }
}

/**
 * Simplified target spots setup dialog.
 * Excludes manual coordinate entries completely, offering a neat drop current spot tool and preset history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSetupDialog(
    currentLat: Double?,
    currentLng: Double?,
    locations: List<TargetLocation>,
    onSetCurrent: (name: String) -> Unit,
    onSetManual: (name: String, lat: Double, lng: Double) -> Unit,
    onSelectLocation: (id: Int) -> Unit,
    onDeleteLocation: (loc: TargetLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var landmarkName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkGrey)
            .border(1.dp, BorderGrey, RoundedCornerShape(20.dp)),
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkGrey
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PIN SPOT BEACON",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AmberPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Destination Manual",
                                tint = DimMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Saved Waypoints Database List at the TOP
                    Text(
                        text = "PRESET SPOTS HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DimMuted,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (locations.isEmpty()) {
                        Text(
                            text = "No saved spots yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DimMuted,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(locations, key = { it.id }) { loc ->
                                Row(
                                    modifier = Modifier
                                        .testTag("location_item_${loc.id}")
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (loc.isCurrent) Color(0x15FF9F0A) else CardGrey)
                                        .border(
                                            width = 1.dp,
                                            color = if (loc.isCurrent) AmberPrimary else BorderGrey,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectLocation(loc.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = loc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (loc.isCurrent) AmberPrimary else DimWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            if (loc.isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0x3032D74B)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = AlertGreen,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.ROOT, "LAT: %.5f / LNG: %.5f", loc.latitude, loc.longitude),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DimMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteLocation(loc) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Location ${loc.name}",
                                            tint = AlertRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Drop Pin at Current Spot at the BOTTOM (simplified and less prominent)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardGrey)
                            .border(1.dp, BorderGrey, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "DROP BEACON SPOT PIN HERE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = landmarkName,
                                onValueChange = { landmarkName = it },
                                placeholder = { Text("e.g. Starting point, My car...", color = Color.Gray) },
                                label = { Text("Spot Name", color = AmberSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberPrimary,
                                    unfocusedBorderColor = BorderGrey,
                                    focusedTextColor = DimWhite,
                                    unfocusedTextColor = DimWhite
                                ),
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .testTag("current_landmark_input")
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = { 
                                    val labelText = landmarkName.trim().ifEmpty { 
                                        "Spot #${locations.size + 1}"
                                    }
                                    onSetCurrent(labelText)
                                    landmarkName = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                modifier = Modifier
                                    .testTag("mark_current_button")
                                    .size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Trigger Drop Beacon Pin Location",
                                    tint = PitchBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (currentLat == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Acquiring live high-precision GPS satellite coordinates...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DimMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    )
}
