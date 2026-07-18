package com.example.f1_kmp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.openUrl
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.date_of_birth
import f1_kmp.composeapp.generated.resources.driver_code
import f1_kmp.composeapp.generated.resources.driver_number
import f1_kmp.composeapp.generated.resources.nationality
import f1_kmp.composeapp.generated.resources.open_in_wikipedia
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import com.example.f1_kmp.domain.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverInfoBottomSheet(driver: DriverModel, onDismiss: () -> Unit) {
    fun value(value: String?): String = value?.takeUnless { it.isBlank() || it.equals("none", true) } ?: "—"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(driver.fullName, style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            Text("${stringResource(Res.string.driver_code)}: ${value(driver.code)}", style = AppStyles.body)
            Text("${stringResource(Res.string.driver_number)}: ${value(driver.permanentNumber)}", style = AppStyles.body)
            Text("${stringResource(Res.string.nationality)}: ${value(driver.nationality)}", style = AppStyles.body)
            Text(
                "${stringResource(Res.string.date_of_birth)}: ${formatDate(driver.dateOfBirth)}",
                style = AppStyles.body,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { openUrl(driver.url) }) {
                Text(stringResource(Res.string.open_in_wikipedia))
            }
        }
    }
}

private fun formatDate(value: String?): String = runCatching {
    val date = LocalDate.parse(value!!)
    "${date.dayOfMonth} ${DateUtils.monthName(date.month.number)} ${date.year}"
}.getOrElse { "—" }
