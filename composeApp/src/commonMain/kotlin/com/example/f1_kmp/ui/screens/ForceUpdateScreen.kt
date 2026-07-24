package com.example.f1_kmp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Black
import com.example.f1_kmp.util.onLocaleChanged
import com.example.f1_kmp.util.openUrl
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.error_car
import f1_kmp.composeapp.generated.resources.force_update_button
import f1_kmp.composeapp.generated.resources.force_update_subtitle
import f1_kmp.composeapp.generated.resources.force_update_title
import f1_kmp.composeapp.generated.resources.locale_code_en
import f1_kmp.composeapp.generated.resources.locale_code_ru
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_RELEASES_URL = "https://github.com/DaniilPavlov/f1_kmp/releases"

/** Блокирующий экран: версия ниже минимума из Remote Config. */
@Composable
fun ForceUpdateScreen(modifier: Modifier = Modifier) {
    val language by LocaleController.language.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = AppDimens.horizontalPadding.dp,
                vertical = AppDimens.verticalPadding.dp,
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    if (language == "en") Res.string.locale_code_ru else Res.string.locale_code_en,
                ),
                style = AppStyles.body.copy(color = F1Black, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable {
                        LocaleController.toggle()
                        onLocaleChanged()
                    }
                    .padding(8.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.error_car),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(180.dp),
                contentScale = ContentScale.FillWidth,
            )
            Text(
                text = stringResource(Res.string.force_update_title),
                style = AppStyles.h2,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            Text(
                text = stringResource(Res.string.force_update_subtitle),
                style = AppStyles.h3,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            BlackButton(
                text = stringResource(Res.string.force_update_button),
                onClick = { openUrl(GITHUB_RELEASES_URL) },
                modifier = Modifier.padding(horizontal = 50.dp),
            )
        }
    }
}
