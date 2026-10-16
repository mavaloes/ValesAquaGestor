package com.valoes.valesaquagestor.domain.usecase

/** Motivo de um caso de uso não ter feito o que foi pedido. A tela converte em texto de `strings.xml`. */
enum class UseCaseError {
    /** O perfil da sessão não pode fazer isso (`specs/06-perfis-permissoes.md`). */
    PERMISSION_DENIED,

    INVALID_EMAIL,
    INVALID_NAME,

    /** Já existe usuário ativo ou convidado com esse e-mail. */
    EMAIL_ALREADY_IN_USE,

    USER_NOT_FOUND,

    /** O ADMIN não pode trocar o próprio perfil nem se desativar (a fazenda ficaria sem ADMIN). */
    CANNOT_CHANGE_SELF,

    /** Faixas ou agenda com valores fora de ordem. */
    INVALID_VALUES,
}

/** Resultado de um caso de uso: deu certo com [Success] ou não fez nada com [Failure]. */
sealed interface UseCaseResult<out T> {
    data class Success<T>(val value: T) : UseCaseResult<T>
    data class Failure(val error: UseCaseError) : UseCaseResult<Nothing>
}
