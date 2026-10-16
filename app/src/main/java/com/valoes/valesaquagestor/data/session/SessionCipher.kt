package com.valoes.valesaquagestor.data.session

/** Cifra e decifra o texto da sessão antes de ir para o disco. */
interface SessionCipher {
    /** Devolve o texto cifrado (em Base64, com o vetor de inicialização na frente). */
    fun encrypt(plainText: String): String

    /** Decifra o resultado de [encrypt]. Lança exceção se o texto foi alterado ou a chave sumiu. */
    fun decrypt(cipherText: String): String
}
