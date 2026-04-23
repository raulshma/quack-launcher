package com.raulshma.minkoa.gallery

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(checkGalleryPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    if (!hasPermission) {
        GalleryPermissionPlaceholder(
            onRequestPermission = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(permission)
            },
            modifier = modifier
        )
        return
    }

    val viewModel: GalleryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isActive) {
        if (isActive && !uiState.isLoaded && !uiState.isLoading) {
            viewModel.loadImages()
        }
    }

    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }

    val filteredImages by remember(uiState.images, uiState.selectedAlbum) {
        mutableStateOf(
            when (uiState.selectedAlbum) {
                null -> uiState.images
                else -> uiState.images.filter { it.bucketId == uiState.selectedAlbum }
            }
        )
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedImage,
            transitionSpec = {
                fadeIn(animationSpec = tween(350)) togetherWith
                    fadeOut(animationSpec = tween(200))
            },
            label = "gallery-transition"
        ) { target ->
            if (target == null) {
                GalleryGrid(
                    images = filteredImages,
                    albums = uiState.albums,
                    selectedAlbum = uiState.selectedAlbum,
                    onAlbumSelected = viewModel::selectAlbum,
                    onImageClick = { selectedImage = it },
                    isLoading = uiState.isLoading,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                    modifier = modifier
                )
            } else {
                ImageViewer(
                    image = target,
                    onDismiss = { selectedImage = null },
                    onDeleted = { viewModel.loadImages(); selectedImage = null },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryGrid(
    images: List<GalleryImage>,
    albums: List<GalleryAlbum>,
    selectedAlbum: String?,
    onAlbumSelected: (String?) -> Unit,
    onImageClick: (GalleryImage) -> Unit,
    isLoading: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.displayMedium
            )
        }

        if (albums.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ElevatedFilterChip(
                        selected = selectedAlbum == null,
                        onClick = { onAlbumSelected(null) },
                        label = { Text("All") }
                    )
                }
                items(
                    count = albums.size,
                    key = { albums[it].bucketId }
                ) { index ->
                    val album = albums[index]
                    ElevatedFilterChip(
                        selected = selectedAlbum == album.bucketId,
                        onClick = { onAlbumSelected(album.bucketId) },
                        label = {
                            Text(
                                text = "${album.bucketName} (${album.count})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            images.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalItemSpacing = 3.dp
                ) {
                    items(
                        items = images,
                        key = { it.id }
                    ) { image ->
                        with(sharedTransitionScope) {
                            AsyncImage(
                                model = image.uri,
                                contentDescription = image.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        image.aspectRatio.coerceIn(0.6f, 1.6f)
                                    )
                                    .clip(RoundedCornerShape(6.dp))
                                    .sharedElement(
                                        state = rememberSharedContentState(
                                            key = "image-${image.id}"
                                        ),
                                        animatedVisibilityScope = animatedContentScope
                                    )
                                    .clickable { onImageClick(image) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageViewer(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetY,
        label = "viewer-offset"
    )
    val progress = abs(offsetY) / 800f
    val backgroundAlpha by animateFloatAsState(
        targetValue = 1f - progress.coerceIn(0f, 1f),
        label = "viewer-bg"
    )

    val bgColor by animateColorAsState(
        targetValue = Color.Black.copy(alpha = backgroundAlpha.coerceIn(0.5f, 0.95f)),
        label = "viewer-bg-color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        offsetY += dragAmount
                    },
                    onDragEnd = {
                        if (abs(offsetY) > 250f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onDragCancel = {
                        offsetY = 0f
                    }
                )
            }
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = image.uri,
                contentDescription = image.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "image-${image.id}"),
                        animatedVisibilityScope = animatedContentScope
                    )
                    .offset { IntOffset(0, animatedOffset.roundToInt()) },
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = image.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { shareImage(context, image) }) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
                IconButton(onClick = { setAsWallpaper(context, image) }) {
                    Icon(
                        Icons.Rounded.Wallpaper,
                        contentDescription = "Set as wallpaper",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
                IconButton(onClick = { deleteImage(context, image); onDeleted() }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Text(
                text = "Swipe down to close",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun shareImage(context: Context, image: GalleryImage) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, image.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun setAsWallpaper(context: Context, image: GalleryImage) {
    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(image.uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("mimeType", "image/*")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Set as wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun deleteImage(context: Context, image: GalleryImage) {
    runCatching {
        context.contentResolver.delete(image.uri, null, null)
    }
}

@Composable
private fun GalleryPermissionPlaceholder(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Collections,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Grant permission to view your photos in an immersive gallery experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                FilledTonalButton(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

private fun checkGalleryPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
