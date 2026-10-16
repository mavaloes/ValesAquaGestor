package com.valoes.valesaquagestor.domain.qr

/**
 * Conteúdo do QR Code de um tanque (`specs/07-automacoes.md`): o `uuid` do tanque, e nada mais.
 * Como o `uuid` é a identidade do tanque na sincronização, o mesmo código continua valendo em
 * qualquer aparelho da fazenda.
 */
object TankQrCode {

    private val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** Texto que vai dentro do QR Code do tanque de `uuid` [tankUuid]. */
    fun encode(tankUuid: String): String = tankUuid.trim().lowercase()

    /** O `uuid` do tanque lido do QR Code, ou `null` se o texto não é um `uuid` (código de outra coisa). */
    fun parse(scannedText: String?): String? {
        val text = scannedText?.trim().orEmpty()
        return if (uuidPattern.matches(text)) text.lowercase() else null
    }
}
