package com.valoes.valesaquagestor.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Home provisória e vazia (Fase 04): só confirma a sessão e oferece "Sair". */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    HomeContent(userName = userName, onSignOut = viewModel::onSignOut)
}

@Composable
internal fun HomeContent(
    userName: String?,
    onSignOut: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (userName != null) {
                Text(text = stringResource(R.string.home_greeting, userName), style = MaterialTheme.typography.titleMedium)
            }
            Text(text = stringResource(R.string.home_placeholder), style = MaterialTheme.typography.bodyLarge)
            LargeButton(
                text = stringResource(R.string.home_sign_out),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onSignOut,
            )
        }
    }
}

@Preview(name = "Home provisória", showBackground = true, fontScale = 2f)
@Composable
private fun HomeScreenPreview() {
    AquaGestorTheme { HomeContent(userName = "Maria", onSignOut = {}) }
}
