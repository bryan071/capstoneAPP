package com.project.webapp.api

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPagerApi::class)
@Composable
fun AutoImageSlider(images: List<String>) {
    val pagerState = rememberPagerState()

    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)  // Change image every 3 seconds
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    HorizontalPager(
        count = images.size,
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) { page ->
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        ) {
            val painter: Painter = rememberImagePainter(images[page])
            Image(
                painter = painter,
                contentDescription = "Banner Image",
                contentScale = ContentScale.Crop, // Ensures image fits box
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
