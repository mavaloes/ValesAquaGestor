package com.valoes.valesaquagestor.presentation.util

import androidx.annotation.StringRes
import com.valoes.valesaquagestor.R

/**
 * Como avisar que o registro foi salvo (`specs/04-requisitos-ux.md`, seção 11). O registro sempre
 * é salvo no aparelho; sem internet o aviso diz que ele fica guardado até a sincronização.
 */
enum class SaveFeedback {
    /** "Registro salvo". */
    SAVED,

    /** "Sem conexão: dados guardados no aparelho". */
    SAVED_OFFLINE,
    ;

    companion object {
        /** O aviso certo para a situação da conexão agora. */
        fun forConnection(isOnline: Boolean): SaveFeedback = if (isOnline) SAVED else SAVED_OFFLINE
    }
}

@StringRes
fun SaveFeedback.stringRes(): Int = when (this) {
    SaveFeedback.SAVED -> R.string.message_saved
    SaveFeedback.SAVED_OFFLINE -> R.string.message_saved_offline
}
