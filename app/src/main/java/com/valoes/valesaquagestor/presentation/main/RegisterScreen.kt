package com.valoes.valesaquagestor.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.usecase.ObserveFarmOverview
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.EmptyMessage
import com.valoes.valesaquagestor.presentation.components.HelpTips
import com.valoes.valesaquagestor.presentation.components.LargeButton
import com.valoes.valesaquagestor.presentation.components.labelRes
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Um lote ativo na aba "Registrar". */
data class RegisterLot(val lotId: Long, val especie: Especie, val fase: FaseLote)

/** Um tanque ativo na aba "Registrar", com os lotes ativos. */
data class RegisterTank(val tankId: Long, val nome: String, val lots: List<RegisterLot>)

data class RegisterUiState(
    val isLoading: Boolean = true,
    val tanks: List<RegisterTank> = emptyList(),
)

/** Lista os tanques e lotes ativos para escolher onde registrar (no máximo 3 toques até salvar um trato). */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    observeFarmOverview: ObserveFarmOverview,
) : ViewModel() {

    val state: StateFlow<RegisterUiState> = observeFarmOverview()
        .map { farm ->
            RegisterUiState(
                isLoading = false,
                tanks = farm.tanks.map { tank ->
                    RegisterTank(
                        tankId = tank.tanque.id,
                        nome = tank.tanque.nome,
                        lots = tank.lots.map { RegisterLot(it.lote.id, it.lote.especie, it.lote.fase) },
                    )
                }.sortedBy { it.nome },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RegisterUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Aba "Registrar": para cada tanque, "Registrar água"; para cada lote, ração, biometria e
 * mortalidade/despesca. Home → Registrar → botão → Salvar são 3 toques.
 */
@Composable
fun RegisterScreen(
    onRegisterWater: (tankId: Long) -> Unit,
    onRegisterFeeding: (lotId: Long) -> Unit,
    onRegisterBiometry: (lotId: Long) -> Unit,
    onRegisterMovement: (lotId: Long) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterContent(state, onRegisterWater, onRegisterFeeding, onRegisterBiometry, onRegisterMovement)
}

@Composable
internal fun RegisterContent(
    state: RegisterUiState,
    onRegisterWater: (Long) -> Unit,
    onRegisterFeeding: (Long) -> Unit,
    onRegisterBiometry: (Long) -> Unit,
    onRegisterMovement: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.register_title), help = HelpTips.Register) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(innerPadding),
        ) {
            if (!state.isLoading && state.tanks.isEmpty()) {
                item { EmptyMessage(text = stringResource(R.string.register_empty)) }
            }
            items(state.tanks, key = { it.tankId }) { tank ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = tank.nome, style = MaterialTheme.typography.titleLarge)
                        LargeButton(
                            text = stringResource(R.string.tank_register_water),
                            icon = Icons.Default.Add,
                            onClick = { onRegisterWater(tank.tankId) },
                        )
                        tank.lots.forEach { lot ->
                            Text(
                                text = stringResource(
                                    R.string.lot_detail_title,
                                    stringResource(lot.especie.labelRes()),
                                    stringResource(lot.fase.labelRes()),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            LargeButton(
                                text = stringResource(R.string.lot_register_feeding),
                                icon = Icons.Default.Add,
                                onClick = { onRegisterFeeding(lot.lotId) },
                            )
                            LargeButton(
                                text = stringResource(R.string.register_biometry),
                                icon = Icons.Default.Add,
                                onClick = { onRegisterBiometry(lot.lotId) },
                            )
                            LargeButton(
                                text = stringResource(R.string.register_movement),
                                icon = Icons.Default.Add,
                                onClick = { onRegisterMovement(lot.lotId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Registrar", showBackground = true, fontScale = 2f)
@Composable
private fun RegisterPreview() {
    AquaGestorTheme {
        RegisterContent(
            state = RegisterUiState(
                isLoading = false,
                tanks = listOf(RegisterTank(1, "Viveiro 1", listOf(RegisterLot(1, Especie.TILAPIA, FaseLote.ENGORDA)))),
            ),
            onRegisterWater = {}, onRegisterFeeding = {}, onRegisterBiometry = {}, onRegisterMovement = {},
        )
    }
}

@Preview(name = "Registrar vazio", showBackground = true, fontScale = 2f)
@Composable
private fun RegisterEmptyPreview() {
    AquaGestorTheme {
        RegisterContent(RegisterUiState(isLoading = false), {}, {}, {}, {})
    }
}
