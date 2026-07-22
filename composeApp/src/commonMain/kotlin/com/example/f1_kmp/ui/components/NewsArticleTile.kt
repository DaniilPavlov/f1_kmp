package com.example.f1_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.f1_kmp.data.model.NewsArticle
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1GrayBg
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1TextGray
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.openUrl

@Composable
fun NewsArticleTile(article: NewsArticle) {
    val language by LocaleController.language.collectAsState()
    val published = article.published?.let { DateUtils.formatMediumDate(it, language) }
    val hasMeta = !article.byline.isNullOrEmpty() || published != null
    val imageUrl = article.imageUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clickable { openUrl(article.webUrl) },
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(F1GrayBg),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = article.headline,
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        if (article.description.isNotEmpty()) {
            Text(
                text = article.description,
                style = AppStyles.body,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
        if (hasMeta) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                Text(
                    text = article.byline.orEmpty(),
                    style = AppStyles.caption.copy(color = F1TextGray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (published != null) {
                    Text(published, style = AppStyles.caption.copy(color = F1TextGray))
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}
