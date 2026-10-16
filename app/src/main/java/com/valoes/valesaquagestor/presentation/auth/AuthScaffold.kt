package com.valoes.valesaquagestor.presentation.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.validation.PasswordRules
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost
import com.valoes.valesaquagestor.presentation.components.HelpButton
import com.valoes.valesaquagestor.presentation.components.HelpTip
import com.valoes.valesaquagestor.presentation.theme.confirmedColor

/**
 * Estrutura comum das telas de autenticação: título, subtítulo, conteúdo rolável (para fonte
 * ampliada e teclado aberto) e o local das mensagens de feedback.
 *
 * Com [centerVertically], o conteúdo fica no meio da tela quando cabe; se não couber (fonte
 * ampliada ou teclado aberto), a tela rola normalmente. [logo] é o espaço da logomarca, no topo.
 * Sem [title], o cabeçalho (e o botão de ajuda que fica nele) não aparece.
 */
@Composable
fun AuthScaffold(
    title: String?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    help: HelpTip? = null,
    centerVertically: Boolean = false,
    logo: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding(),
        ) {
            val minHeight = if (centerVertically) maxHeight else Dp.Unspecified
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = if (centerVertically) {
                    Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                } else {
                    Arrangement.spacedBy(12.dp)
                },
            ) {
                if (logo != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { logo() }
                }
                AuthHeader(title = title, subtitle = subtitle, help = help)
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
    }
}

/**
 * Espaço reservado para a logomarca do app (um quadrado). Trocar o conteúdo por a imagem
 * definitiva quando a marca estiver pronta.
 */
@Composable
fun LogoPlaceholder(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.logo_placeholder_description)
    Box(
        modifier = modifier
            .size(120.dp)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .semantics { contentDescription = description },
    )
}

@Composable
private fun AuthHeader(title: String?, subtitle: String?, help: HelpTip?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (help != null) HelpButton(help)
            }
        }
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * As três regras da senha, sempre visíveis (antes de digitar) e marcadas enquanto o usuário digita.
 * Cada regra tem ícone e texto, então o estado não depende só da cor.
 */
@Composable
fun PasswordRulesChecklist(
    rules: PasswordRules,
    modifier: Modifier = Modifier,
    passwordsMatch: Boolean? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.password_rules_title),
            style = MaterialTheme.typography.titleSmall,
        )
        RuleRow(stringResource(R.string.password_rule_length), rules.hasMinLength)
        RuleRow(stringResource(R.string.password_rule_letter), rules.hasLetter)
        RuleRow(stringResource(R.string.password_rule_digit), rules.hasDigit)
        if (passwordsMatch != null) {
            RuleRow(stringResource(R.string.password_rule_match), passwordsMatch)
        }
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Default.Check else Icons.Default.Close,
            contentDescription = stringResource(if (met) R.string.password_rule_met else R.string.password_rule_missing),
            tint = if (met) confirmedColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
