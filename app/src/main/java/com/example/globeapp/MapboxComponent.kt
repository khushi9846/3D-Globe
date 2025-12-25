package com.example.globeapp

// -------------------- Compose animation imports --------------------
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

// -------------------- Compose UI imports --------------------
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -------------------- Mapbox imports --------------------
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroup
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.gestures.gestures

import kotlinx.coroutines.delay

// -------------------- Math imports --------------------
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Main composable that renders:
 * - Half globe (Mapbox)
 * - Animated flight arc
 * - Destination selector cards
 */
@Composable
fun MapBoxComponent() {

    // Starting point (Dubai)
    val start = Point.fromLngLat(55.2708, 25.2048)

    // Destination list: (label, geo point, image)
    val places = listOf(
        Triple("India", Point.fromLngLat(78.9629, 20.5937), R.drawable.india),
        Triple("Africa", Point.fromLngLat(17.6791, 1.6508), R.drawable.africa),
        Triple("Europe", Point.fromLngLat(15.2551, 54.5260), R.drawable.europe),
        Triple("Australia", Point.fromLngLat(133.7751, -25.2744), R.drawable.australia)
    )

    // Currently selected destination
    var selectedDestination by remember { mutableStateOf<Point?>(null) }

    // Controls how much of the arc is visible (0 → hidden, 1 → full)
    var arcProgress by remember { mutableStateOf(0f) }

    // Smooth animation for arc drawing
    val animatedProgress by animateFloatAsState(
        targetValue = arcProgress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "arcProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFF1C1C1E))
    ) {

        // Title
        Text(
            text = "Globe Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        /**
         * Map container with clipping to achieve "half globe" look
         */
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {

            // Aspect ratio used to tune pitch dynamically
            val aspectRatio = maxWidth / maxHeight

            /**
             * Camera center:
             * - When destination is selected → midpoint of arc, shifted north
             * - Otherwise → start location shifted north
             */
            val cameraCenter = selectedDestination?.let { dest ->
                calculateArcMidpoint(start, dest, offsetNorth = 25.0)
            } ?: offsetPointNorth(start, 25.0)

            // Viewport state controls camera and animations
            val mapViewportState = rememberMapViewportState {
                setCameraOptions {
                    zoom(1.0)
                    center(cameraCenter)
                    pitch(0.0)
                    bearing(0.0)
                }
            }

            /**
             * Animate camera + trigger arc animation
             * whenever destination changes
             */
            LaunchedEffect(selectedDestination) {
                selectedDestination?.let { dest ->
                    val frontFacingCenter =
                        calculateArcMidpoint(start, dest, offsetNorth = 25.0)

                    mapViewportState.flyTo(
                        buildCameraOptions(frontFacingCenter, 0.0)
                    )

                    delay(100)
                    arcProgress = 1f
                }
            }

            // -------------------- Mapbox Map --------------------
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar = {},
                logo = {},
                compass = {},
                attribution = {},
                style = {
                    GenericStyle("mapbox://styles/vinisha/cmjcp3jw9001h01qv2tfi9rdf")
                }
            ) {

                /**
                 * Disable gestures to keep cinematic globe view stable
                 */
                MapEffect(Unit) { mapView ->
                    mapView.gestures.apply {
                        rotateEnabled = false
                        pinchToZoomEnabled = false
                        doubleTapToZoomInEnabled = false
                        doubleTouchToZoomOutEnabled = false
                        quickZoomEnabled = false
                        scrollEnabled = false
                    }
                }

                /**
                 * Render markers:
                 * - Start point (larger)
                 * - All destinations
                 */
                CircleAnnotationGroup(
                    annotations =
                        listOf(
                            CircleAnnotationOptions()
                                .withPoint(start)
                                .withCircleRadius(4.0)
                                .withCircleColor("#66BB6A")
                                .withCircleStrokeWidth(2.5)
                                .withCircleStrokeColor("rgba(0, 0, 0, 0.5)")
                        ) +
                                places.map { (_, point) ->
                                    CircleAnnotationOptions()
                                        .withPoint(point)
                                        .withCircleRadius(2.5)
                                        .withCircleColor("#FACE00")
                                        .withCircleStrokeWidth(2.5)
                                        .withCircleStrokeColor("rgba(255, 255, 0, 0.4)")
                                },
                    annotationConfig = AnnotationConfig()
                )

                /**
                 * Draw animated flight arc
                 */
                selectedDestination?.let { dest ->

                    // Generate full arc path
                    val fullArcPoints =
                        generateSemiCircularArc(start, dest, 200, arcHeight = 10.0)

                    // Reveal only a portion of the arc based on animation
                    val visiblePointsCount =
                        (fullArcPoints.size * animatedProgress).toInt()

                    val arcPoints =
                        fullArcPoints.take(visiblePointsCount.coerceAtLeast(2))

                    if (arcPoints.size >= 2) {
                        PolylineAnnotationGroup(
                            annotations = listOf(
                                PolylineAnnotationOptions()
                                    .withPoints(arcPoints)
                                    .withLineColor("#FACE00")
                                    .withLineWidth(2.5)
                                    .withLineBorderColor("#FFFFFF")
                                    .withLineBorderWidth(0.5)
                                    .withLineJoin(LineJoin.ROUND)
                            ),
                            annotationConfig = AnnotationConfig()
                        )
                    }
                }
            }
        }

        /**
         * Destination selector cards
         */
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                ,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(places) { (label, point, imageRes) ->
                DestinationCard(
                    name = label,
                    imageRes = imageRes,
                    onClick = {
                        selectedDestination = point
                        arcProgress = 0f // reset arc animation
                    },
                    isSelected = selectedDestination == point
                )
            }
        }
    }
}

/**
 * Calculates midpoint of great-circle arc and optionally shifts north
 * Used to keep the arc front-facing on the globe
 */
fun calculateArcMidpoint(start: Point, end: Point, offsetNorth: Double? = null): Point {
    val lat1 = Math.toRadians(start.latitude())
    val lon1 = Math.toRadians(start.longitude())
    val lat2 = Math.toRadians(end.latitude())
    val lon2 = Math.toRadians(end.longitude())

    val d =
        acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon2 - lon1))
    if (d == 0.0) return start

    val A = sin(0.5 * d) / sin(d)
    val B = A

    val x = A * cos(lat1) * cos(lon1) + B * cos(lat2) * cos(lon2)
    val y = A * cos(lat1) * sin(lon1) + B * cos(lat2) * sin(lon2)
    val z = A * sin(lat1) + B * sin(lat2)

    val lat = atan2(z, sqrt(x * x + y * y))
    val lon = atan2(y, x)

    val midpoint = Point.fromLngLat(Math.toDegrees(lon), Math.toDegrees(lat))
    return if (offsetNorth != null) offsetPointNorth(midpoint, offsetNorth) else midpoint
}

/**
 * Shifts a point northward (used to push globe horizon down)
 */
fun offsetPointNorth(point: Point, degrees: Double): Point =
    Point.fromLngLat(point.longitude(), point.latitude() + degrees)

/**
 * Helper to build camera options consistently
 */
fun buildCameraOptions(center: Point, pitch: Double, zoom: Double = 1.0): CameraOptions =
    CameraOptions.Builder()
        .center(center)
        .zoom(zoom)
        .pitch(pitch)
        .bearing(0.0)
        .build()

/**
 * Generates a visually pleasing semi-circular arc
 * NOTE: This is NOT true great-circle math,
 * but tuned for globe UI aesthetics
 */
fun generateSemiCircularArc(
    start: Point,
    end: Point,
    steps: Int = 100,
    arcHeight: Double = 20.0
): List<Point> =
    (0..steps).map { i ->
        val t = i.toDouble() / steps
        val lon = start.longitude() + t * (end.longitude() - start.longitude())
        val latBase = start.latitude() + t * (end.latitude() - start.latitude())
        val offset = arcHeight * sin(Math.PI * t)
        Point.fromLngLat(lon, latBase + offset)
    }

/**
 * Destination card with animated elevation and offset
 */
@Composable
fun DestinationCard(
    name: String,
    imageRes: Int,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    val animatedElevation by animateDpAsState(if (isSelected) 12.dp else 2.dp)
    val animatedOffsetY by animateDpAsState(if (isSelected) (-10).dp else 0.dp)
    val animatedHeight by animateDpAsState(if (isSelected) 140.dp else 100.dp)

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(animatedHeight)
            .offset(y = animatedOffsetY)
            .clickable { onClick() }
            .then(
                if (isSelected)
                    Modifier.border(3.dp, Color.Yellow, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(animatedElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF006400)
        )
    ) {

        Box(Modifier.fillMaxSize()) {

            // Background image
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Destination label
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}
