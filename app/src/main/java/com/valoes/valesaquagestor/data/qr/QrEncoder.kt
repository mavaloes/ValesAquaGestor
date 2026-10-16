package com.valoes.valesaquagestor.data.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Gera a matriz do QR Code (ZXing). Kotlin puro: não depende do Android, então é testada na JVM.
 * A correção de erro é a mais alta (H) porque o código é impresso e colado em tanque, onde suja e desbota.
 */
object QrEncoder {

    /** Matriz quadrada de [sizePx] pixels com a margem de silêncio do QR Code; `true` = módulo escuro. */
    fun encode(content: String, sizePx: Int): BitMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    }
}
