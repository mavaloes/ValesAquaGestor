package com.valoes.valesaquagestor.presentation.sync

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.sync.SyncPhase
import com.valoes.valesaquagestor.domain.sync.SyncStatus
import com.valoes.valesaquagestor.domain.sync.SyncStatusRepository
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** O que o indicador discreto mostra. */
sealed interface SyncIndicator {
    /** Sem servidor configurado: não há o que mostrar. */
    data object Hidden : SyncIndicator
    data object Synced : SyncIndicator
    data object Syncing : SyncIndicator
    data class Pending(val count: Int) : SyncIndicator
}

/** Estado da sincronização para o texto do indicador. */
fun SyncStatus.toIndicator(): SyncIndicator = when {
    !backendConfigured -> SyncIndicator.Hidden
    phase == SyncPhase.RUNNING -> SyncIndicator.Syncing
    pendingCount > 0 -> SyncIndicator.Pending(pendingCount)
    else -> SyncIndicator.Synced
}

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    repository: SyncStatusRepository,
) : ViewModel() {

    val indicator: StateFlow<SyncIndicator> = repository.status
        .map { it.toIndicator() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SyncIndicator.Hidden)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Linha discreta com o estado da sincronização: "Tudo sincronizado" ou "3 registros aguardando conexão". */
@Composable
fun SyncStatusIndicator(modifier: Modifier = Modifier, viewModel: SyncStatusViewModel = hiltViewModel()) {
    val indicator by viewModel.indicator.collectAsStateWithLifecycle()
    SyncStatusText(indicator, modifier)
}

@Composable
internal fun SyncStatusText(indicator: SyncIndicator, modifier: Modifier = Modifier) {
    val text = when (indicator) {
        SyncIndicator.Hidden -> return
        SyncIndicator.Synced -> stringResource(R.string.sync_status_synced)
        SyncIndicator.Syncing -> stringResource(R.string.sync_status_syncing)
        is SyncIndicator.Pending -> pluralStringResource(R.plurals.sync_status_pending, indicator.count, indicator.count)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Preview(name = "Sincronização pendente", showBackground = true, fontScale = 2f)
@Composable
private fun SyncPendingPreview() {
    AquaGestorTheme { SyncStatusText(SyncIndicator.Pending(3)) }
}

@Preview(name = "Tudo sincronizado", showBackground = true, fontScale = 2f)
@Composable
private fun SyncSyncedPreview() {
    AquaGestorTheme { SyncStatusText(SyncIndicator.Synced) }
}
