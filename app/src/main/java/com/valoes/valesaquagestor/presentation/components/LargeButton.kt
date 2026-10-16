package com.valoes.valesaquagestor.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Altura mínima de qualquer alvo de toque (`specs/04-requisitos-ux.md`, acessibilidade). */
private val MinTouchTarget = 48.dp

/**
 * Botão grande com **ícone e texto** (`specs/04`). Altura mínima de 56 dp. Com [isLoading] mostra o
 * indicador de progresso e fica desabilitado, o que evita o toque duplo.
 */
@Composable
fun LargeButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val loadingDescription = stringResource(R.string.loading)
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = loadingDescription },
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            // O texto do botão já diz a ação; o leitor de tela não repete a descrição do ícone.
            Icon(icon = icon, description = text)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Link de texto (ex.: "Esqueci minha senha") com alvo de toque de 48 dp ou mais. */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Ícone decorativo ao lado de um texto: tem `contentDescription`, mas o leitor de tela o ignora. */
@Composable
private fun Icon(icon: ImageVector, description: String) {
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = Modifier
            .size(24.dp)
            .clearAndSetSemantics { },
    )
}

@Preview(name = "Botão grande", showBackground = true, fontScale = 2f)
@Composable
private fun LargeButtonPreview() {
    AquaGestorTheme {
        LargeButton(
            text = stringResource(R.string.login_button),
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = {},
        )
    }
}

@Preview(name = "Botão grande carregando", showBackground = true, fontScale = 2f)
@Composable
private fun LargeButtonLoadingPreview() {
    AquaGestorTheme {
        LargeButton(
            text = stringResource(R.string.login_button),
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = {},
            isLoading = true,
        )
    }
}
