package com.example.f1_kmp.ui.screens.home

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.home_headlines_title
import f1_kmp.composeapp.generated.resources.news_empty
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.NewsArticle
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.NewsArticleTile
import com.example.f1_kmp.ui.components.shimmer.ScreenShimmer
import com.example.f1_kmp.ui.components.shimmer.ShimmerSkeleton
import com.example.f1_kmp.ui.components.shimmer.ShimmerTextLine
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.appColors

/** ESPN headlines на Home (без вложенного LazyColumn — внутри общего scroll). */
@Composable
fun HomeHeadlinesSection(
    articles: AsyncValue<List<NewsArticle>>,
    visibleArticles: List<NewsArticle>,
    titleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    Column(
        modifier = modifier.padding(
            start = AppDimens.horizontalPadding.dp,
            end = AppDimens.horizontalPadding.dp,
            top = 8.dp,
            bottom = AppDimens.verticalPadding.dp,
        ),
    ) {
        Text(
            text = stringResource(Res.string.home_headlines_title),
            style = AppStyles.h3,
            modifier = titleModifier,
        )
        Spacer(Modifier.height(12.dp))
        when {
            articles.isLoading -> HomeHeadlinesShimmer()
            articles is AsyncValue.Error ||
                (articles is AsyncValue.Value && articles.value.isEmpty()) -> {
                Text(
                    text = stringResource(Res.string.news_empty),
                    style = AppStyles.caption.copy(color = colors.textGray),
                )
            }
            else -> {
                visibleArticles.forEachIndexed { index, article ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    NewsArticleTile(article)
                }
            }
        }
    }
}

@Composable
private fun HomeHeadlinesShimmer() {
    val colors = appColors()
    ScreenShimmer {
        Column {
            repeat(3) { index ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.strokeGray, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    ShimmerSkeleton(
                        radius = 0.dp,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        ShimmerTextLine(height = 16.dp, width = 220.dp, bottomGap = 14.dp)
                        ShimmerTextLine()
                        ShimmerTextLine(width = 180.dp, bottomGap = 0.dp)
                    }
                }
            }
        }
    }
}
