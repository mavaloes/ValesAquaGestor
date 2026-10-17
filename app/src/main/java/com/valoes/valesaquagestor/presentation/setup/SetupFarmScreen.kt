package com.valoes.valesaquagestor.presentation.setup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.valoes.valesaquagestor.presentation.lot.LotFormScreen
import com.valoes.valesaquagestor.presentation.tank.TankFormScreen

/**
 * Passo a passo da configuração da fazenda (`SetupFarm`): primeiro o tanque, depois o lote.
 *
 * @property onSkip pular a configuração e ir para a Home. Pode ser retomada depois a partir da Home.
 */
class SetupMode(val onSkip: () -> Unit)

/**
 * Configuração guiada: **passo 1**, criar o primeiro tanque; **passo 2**, criar o primeiro lote nele.
 * Reaproveita os formulários de tanque e de lote, com um exemplo em cada campo, o título do passo
 * e o botão "Pular". Se já existe um tanque, a configuração retomada começa direto no lote.
 *
 * @param tankId `null` no passo 1; o id do tanque no passo 2.
 * @param onTankSaved o tanque foi criado: seguir para o passo 2 com o id dele.
 * @param onLotSaved o lote foi criado: a configuração terminou.
 * @param onSkip o usuário pulou: ir para a Home.
 */
@Composable
fun SetupFarmScreen(
    tankId: Long?,
    onTankSaved: (tankId: Long) -> Unit,
    onLotSaved: (lotId: Long) -> Unit,
    onSkip: () -> Unit,
) {
    // Voltar na configuração é o mesmo que pular: não há tela anterior para onde voltar.
    BackHandler(onBack = onSkip)
    val setup = SetupMode(onSkip = onSkip)
    if (tankId == null) {
        TankFormScreen(onBack = onSkip, onSaved = onTankSaved, setup = setup)
    } else {
        LotFormScreen(onBack = onSkip, onSaved = onLotSaved, setup = setup)
    }
}
