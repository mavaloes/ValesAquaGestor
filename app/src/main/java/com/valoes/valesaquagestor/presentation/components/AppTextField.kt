package com.valoes.valesaquagestor.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/**
 * Campo de texto com rótulo, exemplo e **mensagem de erro** logo abaixo (`safe-form`).
 * O erro também é anunciado pelo leitor de tela.
 *
 * @param errorText texto do erro (de `strings.xml`); `null` quando o campo está certo.
 * @param placeholder exemplo do que digitar, visível enquanto o campo está vazio.
 * @param contentType dica para o gerenciador de senhas (e-mail, usuário, senha).
 * @param onImeAction chamado quando o usuário toca na ação "Concluir" do teclado.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    contentType: ContentType? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = errorText?.let { { Text(it) } },
        isError = errorText != null,
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        trailingIcon = trailingContent,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = keyboardType == KeyboardType.Text,
        ),
        keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }, onGo = { onImeAction?.invoke() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (contentType != null) this.contentType = contentType
                if (errorText != null) error(errorText)
            },
    )
}

/**
 * Campo de senha com botão **Mostrar/Ocultar** (texto, não só ícone). O texto fica oculto por padrão.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    contentType: ContentType = ContentType.Password,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    val toggleText = stringResource(if (visible) R.string.password_hide else R.string.password_show)
    val toggleDescription = stringResource(
        if (visible) R.string.password_hide_description else R.string.password_show_description,
    )
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorText = errorText,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        contentType = contentType,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        onImeAction = onImeAction,
        enabled = enabled,
        trailingContent = {
            TextButton(
                onClick = { visible = !visible },
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            ) {
                Text(toggleText)
            }
        },
    )
}

@Preview(name = "Campos", showBackground = true, fontScale = 2f)
@Composable
private fun TextFieldsPreview() {
    AquaGestorTheme {
        androidx.compose.foundation.layout.Column {
            AppTextField(
                value = "maria@",
                onValueChange = {},
                label = stringResource(R.string.field_email),
                errorText = stringResource(R.string.error_invalid_email),
                keyboardType = KeyboardType.Email,
            )
            PasswordTextField(
                value = "abc12345",
                onValueChange = {},
                label = stringResource(R.string.field_password),
            )
        }
    }
}
