package com.valoes.valesaquagestor.domain.usecase

import com.valoes.valesaquagestor.domain.qr.TankQrCode
import com.valoes.valesaquagestor.domain.repository.TankRepository
import javax.inject.Inject

/** O que um QR Code lido significa para este aparelho. */
sealed interface QrResolution {

    /** O código é de um tanque que existe aqui: abrir o `TankDetail` de [tankId]. */
    data class Found(val tankId: Long) : QrResolution

    /** O texto lido não é um QR Code de tanque do AquaGestor. */
    data object InvalidCode : QrResolution

    /** É um QR Code de tanque, mas este aparelho não tem esse tanque (excluído ou ainda não sincronizado). */
    data object UnknownTank : QrResolution
}

/** Transforma o texto lido pela câmera no tanque a abrir. Funciona offline: só consulta o banco local. */
class ResolveTankQr @Inject constructor(
    private val tanks: TankRepository,
) {
    suspend operator fun invoke(scannedText: String?): QrResolution {
        val uuid = TankQrCode.parse(scannedText) ?: return QrResolution.InvalidCode
        val tank = tanks.getByUuid(uuid) ?: return QrResolution.UnknownTank
        return QrResolution.Found(tank.id)
    }
}
