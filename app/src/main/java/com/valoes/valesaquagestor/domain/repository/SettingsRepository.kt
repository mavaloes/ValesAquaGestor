package com.valoes.valesaquagestor.domain.repository

import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import kotlinx.coroutines.flow.Flow

/**
 * Faixas da água e agenda de medição ajustadas pelo ADMIN. Os padrões continuam sendo as constantes
 * com fonte; aqui ficam só os ajustes. Ler não exige permissão (todas as telas usam as faixas);
 * **gravar** passa pelo caso de uso `ManageSettings`, que confere o perfil.
 */
interface SettingsRepository {

    /** Faixas em uso: os ajustes salvos ou, sem ajuste, os padrões. */
    val waterRanges: Flow<WaterRanges>

    /** Agenda em uso: os ajustes salvos ou, sem ajuste, os padrões. */
    val monitoringSchedule: Flow<MonitoringSchedule>

    suspend fun saveWaterRanges(ranges: WaterRanges)

    /** Apaga os ajustes das faixas: volta aos padrões das fontes. */
    suspend fun resetWaterRanges()

    suspend fun saveMonitoringSchedule(schedule: MonitoringSchedule)

    /** Apaga os ajustes da agenda: volta aos padrões das fontes. */
    suspend fun resetMonitoringSchedule()
}
