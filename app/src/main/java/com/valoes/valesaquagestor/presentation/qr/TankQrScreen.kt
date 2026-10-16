package com.valoes.valesaquagestor.presentation.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** QR Code do tanque, para imprimir ou compartilhar. O código contém o `uuid` do tanque. */
@Composable
fun TankQrScreen(onBack: () -> Unit, viewModel: TankQrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.qr_share_chooser)
    val printJob = stringResource(R.string.qr_print_job, state.tankName)
    // A imagem é desenhada uma vez por tanque; é a mesma que vai para a folha e para o compartilhamento.
    val bitmap = remember(state.content, state.tankName) {
        state.content?.let { QrBitmap.create(it, state.tankName) }
    }
    TankQrContent(
        state = state,
        bitmap = bitmap,
        onBack = onBack,
        onShare = { bitmap?.let { QrBitmap.share(context, it, chooserTitle) } },
        onPrint = { bitmap?.let { QrBitmap.print(context, it, printJob) } },
    )
}

@Composable
internal fun TankQrContent(
    state: TankQrUiState,
    bitmap: Bitmap?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.qr_title), onBack = onBack, help = HelpTips.TankQr) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bitmap == null) {
                if (!state.isLoading) EmptyMessage(stringResource(R.string.qr_unavailable))
                return@Column
            }
            Text(text = stringResource(R.string.qr_hint), style = MaterialTheme.typography.bodyLarge)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.qr_image_description, state.tankName),
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxWidth(),
            )
            LargeButton(text = stringResource(R.string.qr_share), icon = Icons.Default.Share, onClick = onShare)
            LargeButton(text = stringResource(R.string.qr_print), icon = Icons.Default.Print, onClick = onPrint)
        }
    }
}

@Preview(name = "QR Code do tanque", showBackground = true, fontScale = 2f)
@Composable
private fun TankQrPreview() {
    AquaGestorTheme {
        TankQrContent(
            state = TankQrUiState(isLoading = false, tankName = "Viveiro 1", content = "2c1f3a44-7b8e-4d27-9a31-5c0e8d6b1f90"),
            bitmap = QrBitmap.create("2c1f3a44-7b8e-4d27-9a31-5c0e8d6b1f90", "Viveiro 1"),
            onBack = {}, onShare = {}, onPrint = {},
        )
    }
}

@Preview(name = "QR Code indisponível", showBackground = true, fontScale = 2f)
@Composable
private fun TankQrUnavailablePreview() {
    AquaGestorTheme {
        TankQrContent(state = TankQrUiState(isLoading = false), bitmap = null, onBack = {}, onShare = {}, onPrint = {})
    }
}
