package com.example.f1_kmp.ui.components

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.showing_cached_data
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.appColors

/** Компактная полоска: данные показаны из кэша / офлайна. */
@Composable
fun CachedDataBanner(modifier: Modifier = Modifier) {
    val colors = appColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.strokeGray.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.textGray,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.showing_cached_data),
            style = AppStyles.caption.copy(color = colors.textGray),
        )
    }
}
