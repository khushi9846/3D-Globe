package com.example.globeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapBoxComponent()
            /*val india = Point.fromLngLat(78.9629, 20.5937)
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        zoom(1.0)
                        center(india)
                        pitch(0.0)
                        bearing(0.0)
                    }
                }
            )*/
            /* GlobeAppTheme {
                 Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                     //GlobeScreen()
                 }
             }*/
        }
    }
}


