package com.example.f1_kmp.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.f1_kmp.R
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.platform.OsmdroidInitializer
import com.example.f1_kmp.ui.theme.F1Red
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.max
import kotlin.math.min

private const val PIN_HEIGHT_DP = 25f
private const val CLUSTER_RADIUS_MULTIPLIER = 9
private const val CLUSTER_MIN_RADIUS_PX = 45
private const val CLUSTER_MAX_RADIUS_PX = 72
/** Минимальный zoom: 2 — дальше белые полосы при отключённом повторении тайлов. */
private const val MAP_MIN_ZOOM = 2.0
private const val MAP_INITIAL_ZOOM = 2.5
private const val MAP_MAX_ZOOM = 18.0

/**
 * Android-карта трасс (OSMDroid): одна карта без повторения тайлов,
 * пины фиксированной высоты, красные кластеры.
 */
@Composable
actual fun CircuitsMapContent(
    circuits: List<CircuitModel>,
    onCircuitClick: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val view = mapView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            view?.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            view?.onPause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmdroidInitializer.ensureInitialized(ctx)
                MapView(ctx).apply {
                    mapView = this
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    val center = circuits.firstOrNull()?.let {
                        GeoPoint(it.location.lat.toDouble(), it.location.longitude.toDouble())
                    }
                    configureCircuitsMapView(this, center)
                    onResume()
                }
            },
            update = { view -> populateCircuitsMap(view, circuits, onCircuitClick) },
        )
    }
}

/**
 * Базовые настройки [MapView]: без повторения тайлов, лимиты scroll/zoom.
 * [MAP_MIN_ZOOM] = 2 — чтобы не появлялись белые поля по краям.
 */
private fun configureCircuitsMapView(mapView: MapView, initialCenter: GeoPoint?) {
    mapView.setHorizontalMapRepetitionEnabled(false)
    mapView.setVerticalMapRepetitionEnabled(false)
    val tileSystem = MapView.getTileSystem()
    mapView.setScrollableAreaLimitLatitude(tileSystem.maxLatitude, tileSystem.minLatitude, 0)
    mapView.setScrollableAreaLimitLongitude(tileSystem.minLongitude, tileSystem.maxLongitude, 0)
    mapView.minZoomLevel = MAP_MIN_ZOOM
    mapView.maxZoomLevel = MAP_MAX_ZOOM
    mapView.controller.setZoom(MAP_INITIAL_ZOOM)
    initialCenter?.let { mapView.controller.setCenter(it) }
}

/**
 * Маркеры трасс: уменьшенный пин + [F1CircuitsClusterer] (красные круги с числом).
 */
private fun populateCircuitsMap(
    mapView: MapView,
    circuits: List<CircuitModel>,
    onCircuitClick: (String) -> Unit,
) {
    mapView.overlays.removeAll { it is F1CircuitsClusterer }
    val pinIcon = MapMarkerIcons.scaledPinIcon(mapView.context) ?: return
    val clusterer = F1CircuitsClusterer(mapView.context)
    circuits.forEach { circuit ->
        val marker = Marker(mapView)
        marker.position = GeoPoint(circuit.location.lat.toDouble(), circuit.location.longitude.toDouble())
        marker.icon = pinIcon
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = circuit.circuitName
        marker.relatedObject = circuit.circuitId
        marker.setOnMarkerClickListener { clicked, _ ->
            onCircuitClick(clicked.relatedObject as String)
            true
        }
        clusterer.add(marker)
    }
    mapView.overlays.add(clusterer)
    mapView.invalidate()
}

/**
 * Кластеры трасс — красный круг с прозрачностью 75% и белой цифрой.
 * Размер: `min(max(size × 9, 45), 72)` px.
 */
private class F1CircuitsClusterer(context: Context) : RadiusMarkerClusterer(context) {
    init {
        setMaxClusteringZoomLevel(15)
        setRadius(50)
        mTextPaint.color = Color.WHITE
        mTextPaint.textSize = 14f * context.resources.displayMetrics.density
        mAnchorU = Marker.ANCHOR_CENTER
        mAnchorV = Marker.ANCHOR_CENTER
    }

    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = Marker(mapView)
        marker.position = cluster.position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.infoWindow = null
        marker.icon = BitmapDrawable(mapView.context.resources, buildClusterBitmap(cluster.size))
        marker.setOnMarkerClickListener { _, _ ->
            mapView.controller.animateTo(cluster.position, mapView.zoomLevelDouble + 2, 500L)
            true
        }
        return marker
    }

    private fun buildClusterBitmap(clusterSize: Int): Bitmap {
        val radius = min(max(clusterSize * CLUSTER_RADIUS_MULTIPLIER, CLUSTER_MIN_RADIUS_PX), CLUSTER_MAX_RADIUS_PX)
        val diameter = radius * 2
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E1271E")
            alpha = (255 * 0.75).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), fillPaint)
        val text = clusterSize.toString()
        val textHeight = mTextPaint.descent() + mTextPaint.ascent()
        canvas.drawText(text, radius.toFloat(), radius - textHeight / 2f, mTextPaint)
        return bitmap
    }
}

/** Иконка маркера: pin_unselected, высота [PIN_HEIGHT_DP] dp (якорь — низ пина). */
private object MapMarkerIcons {
    fun scaledPinIcon(context: Context): Drawable? {
        val source = ContextCompat.getDrawable(context, R.drawable.pin_unselected) ?: return null
        val bitmap = source.toBitmap()
        val density = context.resources.displayMetrics.density
        val targetHeightPx = (PIN_HEIGHT_DP * density).toInt().coerceAtLeast(1)
        val targetWidthPx = (bitmap.width.toFloat() / bitmap.height * targetHeightPx).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidthPx, targetHeightPx, true)
        return BitmapDrawable(context.resources, scaled)
    }
}
