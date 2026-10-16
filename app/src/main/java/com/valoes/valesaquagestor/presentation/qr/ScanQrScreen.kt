package com.valoes.valesaquagestor.presentation.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.LinkButton
import com.valoes.valesaquagestor.presentation.components.WarningCard
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Leitura do QR Code do tanque. Ao entrar, já pede a câmera e abre o leitor (menos um toque). Se a
 * câmera for negada, não existir ou o código não servir, mostra o motivo e oferece [onChooseFromList].
 *
 * @param onOpenTank chamado com o id do tanque quando o código é de um tanque deste aparelho.
 * @param onChooseFromList volta para a lista de tanques.
 */
@Composable
fun ScanQrScreen(
    onBack: () -> Unit,
    onOpenTank: (Long) -> Unit,
    onChooseFromList: () -> Unit,
    viewModel: ScanQrViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = stringResource(R.string.scan_prompt)

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> viewModel.onScanResult(result.contents) }
    val startScan = {
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(prompt)
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onCameraPermissionResult(granted)
        if (granted) startScan()
    }
    val requestScan = {
        viewModel.onRetry()
        when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> viewModel.onNoCamera()
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> startScan()
            else -> permission.launch(Manifest.permission.CAMERA)
        }
    }

    // Abre o leitor sozinho na primeira vez; depois, só quando o usuário tocar em "Ler QR Code".
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!started) {
            started = true
            requestScan()
        }
    }
    state.openTankId?.let { id ->
        LaunchedEffect(id) {
            viewModel.onTankOpened()
            onOpenTank(id)
        }
    }

    ScanQrContent(state = state, onBack = onBack, onScan = requestScan, onChooseFromList = onChooseFromList)
}

@Composable
internal fun ScanQrContent(
    state: ScanQrUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onChooseFromList: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.scan_title), onBack = onBack, help = HelpTips.Scan) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.scan_hint), style = MaterialTheme.typography.bodyLarge)
            state.problem?.let { WarningCard(messages = listOf(stringResource(it.messageRes()))) }
            // Sem câmera não adianta tentar de novo: o caminho é a lista.
            if (state.problem != ScanProblem.NO_CAMERA) {
                LargeButton(text = stringResource(R.string.scan_button), icon = Icons.Default.Search, onClick = onScan)
            }
            LinkButton(text = stringResource(R.string.scan_choose_list), onClick = onChooseFromList)
        }
    }
}

@StringRes
internal fun ScanProblem.messageRes(): Int = when (this) {
    ScanProblem.CAMERA_DENIED -> R.string.scan_error_camera_denied
    ScanProblem.NO_CAMERA -> R.string.scan_error_no_camera
    ScanProblem.INVALID_CODE -> R.string.scan_error_invalid
    ScanProblem.UNKNOWN_TANK -> R.string.scan_error_unknown_tank
}

@Preview(name = "Ler QR Code", showBackground = true, fontScale = 2f)
@Composable
private fun ScanQrPreview() {
    AquaGestorTheme { ScanQrContent(ScanQrUiState(), onBack = {}, onScan = {}, onChooseFromList = {}) }
}

@Preview(name = "Ler QR Code com código inválido", showBackground = true, fontScale = 2f)
@Composable
private fun ScanQrInvalidPreview() {
    AquaGestorTheme {
        ScanQrContent(ScanQrUiState(problem = ScanProblem.INVALID_CODE), onBack = {}, onScan = {}, onChooseFromList = {})
    }
}

@Preview(name = "Ler QR Code com câmera negada", showBackground = true, fontScale = 2f)
@Composable
private fun ScanQrDeniedPreview() {
    AquaGestorTheme {
        ScanQrContent(ScanQrUiState(problem = ScanProblem.CAMERA_DENIED), onBack = {}, onScan = {}, onChooseFromList = {})
    }
}
