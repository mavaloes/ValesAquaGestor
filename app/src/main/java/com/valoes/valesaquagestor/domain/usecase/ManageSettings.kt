package com.valoes.valesaquagestor.domain.usecase

import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.permission.Permission
import com.valoes.valesaquagestor.domain.permission.PermissionChecker
import com.valoes.valesaquagestor.domain.repository.AuthRepository
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Altera as faixas da água e a agenda de medição. Só o ADMIN pode (`specs/06-perfis-permissoes.md`);
 * a checagem é feita aqui, no caso de uso. Valores fora de ordem são recusados sem gravar nada.
 */
class ManageSettings @Inject constructor(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
) {

    suspend fun saveWaterRanges(ranges: WaterRanges): UseCaseResult<Unit> = whenAllowed {
        if (ranges.invalidFields().isNotEmpty()) return@whenAllowed UseCaseResult.Failure(UseCaseError.INVALID_VALUES)
        settings.saveWaterRanges(ranges)
        UseCaseResult.Success(Unit)
    }

    /** Volta as faixas da água aos padrões das fontes. */
    suspend fun resetWaterRanges(): UseCaseResult<Unit> = whenAllowed {
        settings.resetWaterRanges()
        UseCaseResult.Success(Unit)
    }

    suspend fun saveMonitoringSchedule(schedule: MonitoringSchedule): UseCaseResult<Unit> = whenAllowed {
        if (schedule.invalidFields().isNotEmpty()) return@whenAllowed UseCaseResult.Failure(UseCaseError.INVALID_VALUES)
        settings.saveMonitoringSchedule(schedule)
        UseCaseResult.Success(Unit)
    }

    /** Volta a agenda de medição aos padrões das fontes. */
    suspend fun resetMonitoringSchedule(): UseCaseResult<Unit> = whenAllowed {
        settings.resetMonitoringSchedule()
        UseCaseResult.Success(Unit)
    }

    private suspend fun whenAllowed(action: suspend () -> UseCaseResult<Unit>): UseCaseResult<Unit> {
        val papel = auth.currentSession.first()?.papel
        return if (PermissionChecker.isAllowed(papel, Permission.MANAGE_SETTINGS)) {
            action()
        } else {
            UseCaseResult.Failure(UseCaseError.PERMISSION_DENIED)
        }
    }
}
