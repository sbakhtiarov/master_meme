package com.devcampus.memes_list.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.devcampus.memes_list.domain.model.Meme

@Composable
internal fun MemeContentScreen(
    memes: List<Meme>,
    selection: List<Meme>?,
    onItemClick: (Meme) -> Unit,
    onItemLongClick: (Meme) -> Unit,
    onItemFavouriteClick: (Meme) -> Unit,
) {

    val configuration = LocalConfiguration.current

    val columns = when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> 4
        else -> 2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = memes,
            key = { it.path }
        ) { meme ->
            MemeGridItem(
                modifier = Modifier.animateItem(),
                meme = meme,
                isSelected = { selection?.contains(meme) },
                onClick = { onItemClick(meme) },
                onLongClick = { onItemLongClick(meme) },
                onFavouriteClick = { onItemFavouriteClick(meme) }
            )
        }
    }
}
