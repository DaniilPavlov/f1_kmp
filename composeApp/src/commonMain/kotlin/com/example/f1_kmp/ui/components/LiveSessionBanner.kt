package com.example.f1_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.live.LiveWeekendController
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1OnChrome
import com.example.f1_kmp.ui.theme.F1Red
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.live_session_banner
import f1_kmp.composeapp.generated.resources.live_session_banner_with_session

@Composable
fun LiveSessionBanner(
    controller: LiveWeekendController,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreboard by controller.scoreboard.collectAsState()
    val live = scoreboard?.isLive == true
    if (!live) return
    val abbr = scoreboard?.highlightedSession?.abbreviation?.takeIf { it.isNotBlank() }
    val label = if (abbr == null) {
        stringResource(Res.string.live_session_banner)
    } else {
        stringResource(Res.string.live_session_banner_with_session, abbr)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(F1Red)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppStyles.body.copy(color = F1OnChrome),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = F1OnChrome,
        )
    }
}
