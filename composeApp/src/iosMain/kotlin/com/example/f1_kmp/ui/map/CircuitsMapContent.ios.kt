package com.example.f1_kmp.ui.map

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.ui.theme.F1Red
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPointAnnotation
import platform.darwin.NSObject

/**
 * iOS-карта трасс на MapKit: пины по координатам, tap → [onCircuitClick].
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CircuitsMapContent(
    circuits: List<CircuitModel>,
    onCircuitClick: (String) -> Unit,
) {
    val delegate = remember(onCircuitClick) { CircuitsMapDelegate(onCircuitClick) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
    ) {
        UIKitView(
            factory = {
                MKMapView().apply {
                    this.delegate = delegate
                    showsUserLocation = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                val existing = mapView.annotations
                    .mapNotNull { it as? MKPointAnnotation }
                existing.forEach { mapView.removeAnnotation(it) }

                val annotations = circuits.map { circuit ->
                    MKPointAnnotation().apply {
                        setCoordinate(
                            CLLocationCoordinate2DMake(
                                circuit.location.lat.toDouble(),
                                circuit.location.longitude.toDouble(),
                            ),
                        )
                        setTitle(circuit.circuitName)
                        setSubtitle(circuit.circuitId)
                    }
                }
                mapView.addAnnotations(annotations)
                if (annotations.isNotEmpty()) {
                    mapView.showAnnotations(annotations, animated = false)
                }
            },
        )
    }
}

/** Обработка выбора пина: circuitId хранится в subtitle аннотации. */
private class CircuitsMapDelegate(
    private val onCircuitClick: (String) -> Unit,
) : NSObject(), MKMapViewDelegateProtocol {

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation as? MKPointAnnotation ?: return
        val circuitId = annotation.subtitle ?: return
        onCircuitClick(circuitId)
        mapView.deselectAnnotation(annotation, animated = true)
    }
}
