package com.momro.weltentdecker.ui

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.momro.weltentdecker.R
import com.momro.weltentdecker.data.LocationHelper
import com.momro.weltentdecker.data.NominatimRepository
import com.momro.weltentdecker.data.SearchResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private data class ChipItem(val label: String, val query: String)

private val chips = listOf(
    ChipItem("🦁 Zoo", "Zoo"),
    ChipItem("🌳 Spielplatz", "Spielplatz"),
    ChipItem("🏰 Schloss", "Schloss"),
    ChipItem("🎢 Freizeitpark", "Freizeitpark"),
    ChipItem("🐠 Aquarium", "Aquarium"),
)

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val repository = remember { NominatimRepository() }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showInfo by remember { mutableStateOf(false) }
    var searchMarker by remember { mutableStateOf<Marker?>(null) }
    var hereMarker by remember { mutableStateOf<Marker?>(null) }

    fun doSearch(term: String = query) {
        val q = term.trim()
        if (q.isEmpty()) return
        keyboard?.hide()
        scope.launch {
            loading = true
            val found = repository.search(q)
            loading = false
            if (found.isEmpty()) {
                Toast.makeText(context, R.string.nothing_found, Toast.LENGTH_SHORT).show()
            } else {
                results = found
            }
        }
    }

    fun showOnMap(result: SearchResult) {
        val mv = mapView ?: return
        searchMarker?.let { mv.overlays.remove(it) }
        val marker = Marker(mv).apply {
            position = GeoPoint(result.lat, result.lon)
            title = result.name
            icon = ContextCompat.getDrawable(context, R.drawable.ic_pin)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mv.overlays.add(marker)
        searchMarker = marker
        mv.controller.animateTo(GeoPoint(result.lat, result.lon))
        mv.controller.setZoom(14.0)
        marker.showInfoWindow()
        mv.invalidate()
    }

    fun showHere(location: Location) {
        val mv = mapView ?: return
        if (hereMarker == null) {
            hereMarker = Marker(mv).apply {
                icon = ContextCompat.getDrawable(context, R.drawable.ic_you_are_here)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = context.getString(R.string.you_are_here)
            }
            mv.overlays.add(hereMarker)
        }
        val point = GeoPoint(location.latitude, location.longitude)
        hereMarker?.position = point
        mv.controller.animateTo(point)
        if (mv.zoomLevelDouble < 15.0) mv.controller.setZoom(16.0)
        hereMarker?.showInfoWindow()
        mv.invalidate()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            locate()
        } else {
            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    fun locate() {
        if (!LocationHelper.hasPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        scope.launch {
            loading = true
            val location = withTimeoutOrNull(20000) { LocationHelper.awaitLocation(context) }
            loading = false
            if (location == null) {
                Toast.makeText(context, R.string.no_gps, Toast.LENGTH_SHORT).show()
            } else {
                showHere(location)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    minZoomLevel = 4.0
                    maxZoomLevel = 19.0
                    controller.setZoom(6.0)
                    controller.setCenter(GeoPoint(51.1657, 10.4515))
                }.also { mapView = it }
            },
            update = { mapView = it }
        )

        DisposableEffect(mapView) {
            mapView?.onResume()
            onDispose { mapView?.onPause() }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.search_hint), fontSize = 18.sp) },
                            textStyle = TextStyle(fontSize = 18.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { doSearch() }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_cd)
                            )
                        }
                    }
                }
                IconButton(onClick = { showInfo = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.info_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chips) { chip ->
                    AssistChip(
                        onClick = { doSearch(chip.query) },
                        label = { Text(chip.label, fontSize = 16.sp) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(onClick = { mapView?.controller?.zoomIn() }) {
                Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.zoom_in_cd))
            }
            Spacer(modifier = Modifier.height(12.dp))
            FloatingActionButton(onClick = { mapView?.controller?.zoomOut() }) {
                Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.zoom_out_cd))
            }
        }

        FloatingActionButton(
            onClick = { locate() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = Color(0xFF66BB6A)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.locate_cd))
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    if (results.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { results = emptyList() },
            title = { Text(stringResource(R.string.results_title)) },
            text = {
                LazyColumn {
                    items(results) { result ->
                        TextButton(
                            onClick = {
                                showOnMap(result)
                                results = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(result.name, maxLines = 2)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { results = emptyList() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.info_title)) },
            text = { Text(stringResource(R.string.info_message)) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
