package com.valoes.valesaquagestor.presentation.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.valoes.valesaquagestor.data.qr.QrEncoder
import java.io.File

/** Desenho, compartilhamento e impressão do QR Code de um tanque. */
object QrBitmap {

    /** Lado do QR Code, em pixels: grande o bastante para imprimir sem perder nitidez. */
    private const val QR_SIZE_PX = 768

    /** Faixa de baixo com o nome do tanque, para quem cola o código saber de qual tanque é. */
    private const val LABEL_HEIGHT_PX = 120
    private const val LABEL_TEXT_PX = 56f
    private const val LABEL_MARGIN_PX = 32

    /** Imagem do QR Code com o [label] (nome do tanque) embaixo, preto sobre branco. */
    fun create(content: String, label: String): Bitmap {
        val matrix = QrEncoder.encode(content, QR_SIZE_PX)
        val bitmap = Bitmap.createBitmap(QR_SIZE_PX, QR_SIZE_PX + LABEL_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix[x, y]) bitmap.setPixel(x, y, Color.BLACK)
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = LABEL_TEXT_PX
            textAlign = Paint.Align.CENTER
        }
        // Nome longo é cortado com reticências para caber na largura da imagem.
        val text = android.text.TextUtils.ellipsize(
            label, android.text.TextPaint(paint), (QR_SIZE_PX - 2 * LABEL_MARGIN_PX).toFloat(), android.text.TextUtils.TruncateAt.END,
        ).toString()
        val baseline = QR_SIZE_PX + (LABEL_HEIGHT_PX - (paint.descent() + paint.ascent())) / 2f
        Canvas(bitmap).drawText(text, QR_SIZE_PX / 2f, baseline, paint)
        return bitmap
    }

    /** Abre a folha de compartilhamento do Android com a imagem em PNG (a imagem sai pelo cache do app). */
    fun share(context: Context, bitmap: Bitmap, chooserTitle: String) {
        val directory = File(context.cacheDir, "qr").apply { mkdirs() }
        val file = File(directory, "qr-tanque.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }

    /** Abre a impressão do Android com a imagem, ajustada à folha. */
    fun print(context: Context, bitmap: Bitmap, jobName: String) {
        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap(jobName, bitmap)
    }
}
