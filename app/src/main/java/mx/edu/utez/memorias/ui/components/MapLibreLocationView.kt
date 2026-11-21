package mx.edu.utez.memorias.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@Composable
fun MapLibreLocationView(
    latitud: Double,
    longitud: Double,
    markerTitle: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                val target = LatLng(latitud, longitud)
                if (map.style == null) {

                    map.setStyle("https://demotiles.maplibre.org/style.json") {
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(

                                CameraPosition.Builder().target(target).zoom(13.0).build()
                            )
                        )

                        map.addMarker(MarkerOptions().position(target).title(markerTitle))
                    }
                } else {
                    map.clear()

                    map.addMarker(MarkerOptions().position(target).title(markerTitle))

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 13.0))
                }
                if (interactive && onLocationSelected != null) {
                    map.addOnMapClickListener { point ->
                        onLocationSelected(point.latitude, point.longitude)
                        true
                    }
                }
            }
        }
    )
}