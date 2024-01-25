package com.example.wastesamaritanassignment.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarHalf
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.wastesamaritanassignment.R

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Float = 0f,
    stars: Int = 5,
    onRatingChanged: (Double) -> Unit,
) {

    var isHalfStar = (rating % 1) != 0f

    Row {
        for (index in 1..stars) {
            Icon(
                imageVector =
                if (index <= rating) {
                    Icons.Rounded.Star
                } else {
                    if (isHalfStar) {
                        isHalfStar = false
                        Icons.Rounded.StarHalf
                    } else {
                        Icons.Rounded.StarOutline
                    }
                },
                contentDescription = null,
                tint = colorResource(id = R.color.darkYellow),
                modifier = modifier
                    .clickable { onRatingChanged(index.toDouble()) }
                    .size(40.dp)
            )
        }
    }
}