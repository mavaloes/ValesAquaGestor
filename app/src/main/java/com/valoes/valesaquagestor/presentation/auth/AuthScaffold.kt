package com.valoes.valesaquagestor.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.validation.PasswordRules
import com.valoes.valesaquagestor.presentation.components.FeedbackSnackbarHost

/**
 * Estrutura comum das telas de autenticação: título, subtítulo, conteúdo rolável (para fonte
 * ampliada e teclado aberto) e o local das mensagens de feedback.
 */
@Composable
fun AuthScaffold(
    title: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FeedbackSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/**
 * As três regras da senha, sempre visíveis (antes de digitar) e marcadas enquanto o usuário digita.
 * Cada regra tem ícone e texto, então o estado não depende só da cor.
 */
@Composable
fun PasswordRulesChecklist(rules: PasswordRules, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.password_rules_title),
            style = MaterialTheme.typography.titleSmall,
        )
        RuleRow(stringResource(R.string.password_rule_length), rules.hasMinLength)
        RuleRow(stringResource(R.string.password_rule_letter), rules.hasLetter)
        RuleRow(stringResource(R.string.password_rule_digit), rules.hasDigit)
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Default.Check else Icons.Default.Close,
            contentDescription = stringResource(if (met) R.string.password_rule_met else R.string.password_rule_missing),
            tint = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
