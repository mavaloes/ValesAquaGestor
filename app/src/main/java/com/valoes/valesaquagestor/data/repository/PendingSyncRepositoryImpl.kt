package com.valoes.valesaquagestor.data.repository

import com.valoes.valesaquagestor.data.local.dao.ArracoamentoDao
import com.valoes.valesaquagestor.data.local.dao.BiometriaDao
import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.dao.MovimentacaoLoteDao
import com.valoes.valesaquagestor.data.local.dao.QualidadeAguaDao
import com.valoes.valesaquagestor.data.local.dao.TanqueDao
import com.valoes.valesaquagestor.data.local.dao.UsuarioDao
import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Soma os registros com `sincronizado = false` de todas as tabelas. */
class PendingSyncRepositoryImpl @Inject constructor(
    private val tanques: TanqueDao,
    private val lotes: LoteDao,
    private val agua: QualidadeAguaDao,
    private val arracoamentos: ArracoamentoDao,
    private val biometrias: BiometriaDao,
    private val movimentacoes: MovimentacaoLoteDao,
    private val usuarios: UsuarioDao,
) : PendingSyncRepository {

    override fun observePendingCount(): Flow<Int> = combine(
        tanques.observePendingCount(),
        lotes.observePendingCount(),
        agua.observePendingCount(),
        arracoamentos.observePendingCount(),
        biometrias.observePendingCount(),
        movimentacoes.observePendingCount(),
        usuarios.observePendingCount(),
    ) { counts -> counts.sum() }
}
