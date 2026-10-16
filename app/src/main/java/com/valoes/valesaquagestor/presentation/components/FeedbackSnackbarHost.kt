package com.valoes.valesaquagestor.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
