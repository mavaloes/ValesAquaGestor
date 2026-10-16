package com.valoes.valesaquagestor.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.valoes.valesaquagestor.R
import androidx.compose.ui.Modifier

/** Local onde as mensagens de feedback aparecem ("Registro salvo", "Sem conexão…"). */
@Composable
fun FeedbackSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier)
}

/**
 * Mostra [message] no Snackbar quando ela muda e, depois que some, avisa por [onShown] para o
 * ViewModel limpar a mensagem. Mensagens importantes ficam mais tempo na tela.
 *
 * @param message texto já resolvido de `strings.xml`; `null` = nada a mostrar.
 */
@Composable
fun FeedbackEffect(
    message: String?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) {
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
            onShown()
        }
    }
}

/**
 * Mostra "Registro salvo" uma única vez quando [show] é `true` (a tela chegou logo depois de salvar
 * um registro). Lembra que já mostrou, mesmo se a tela girar.
 */
@Composable
fun SavedMessageEffect(show: Boolean, hostState: SnackbarHostState) {
    var shown by rememberSaveable { mutableStateOf(false) }
    val message = stringResource(R.string.message_saved)
    LaunchedEffect(show) {
        if (show && !shown) {
            shown = true
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }
}
