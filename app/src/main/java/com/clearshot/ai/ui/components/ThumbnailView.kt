package com.clearshot.ai.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clearshot.ai.ui.theme.OverlayPill

/**
 * Circular thumbnail displaying the most recently captured photograph.
 * Clicking launches the system gallery viewer.
 */
@Composable
fun ThumbnailView(
    latestImageUri: Uri?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(OverlayPill)
            .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.5f), shape = CircleShape)
            .clickable(enabled = latestImageUri != null) {
                if (onClick != null) {
                    onClick()
                } else {
                    latestImageUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open photograph"))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (latestImageUri != null) {
            AsyncImage(
                model = latestImageUri,
                contentDescription = "Last captured photograph",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = "Gallery",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
