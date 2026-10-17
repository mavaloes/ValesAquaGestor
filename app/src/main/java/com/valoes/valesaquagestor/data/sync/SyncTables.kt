package com.valoes.valesaquagestor.data.sync

import com.valoes.valesaquagestor.data.local.database.AppDatabase
import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity
import com.valoes.valesaquagestor.data.local.entity.QualidadeAguaEntity
import com.valoes.valesaquagestor.data.local.entity.SyncEntity
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity
import com.valoes.valesaquagestor.data.local.entity.UsuarioEntity
import com.valoes.valesaquagestor.data.repository.MergeResult
import com.valoes.valesaquagestor.data.repository.mergeByUuid
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.domain.model.Papel
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.model.TipoTanque
import kotlinx.serialization.json.JsonObject

/** Um registro local pronto para ir ao servidor. */
class PendingRow(val uuid: String, val atualizadoEm: Long, val json: JsonObject)

/**
 * Uma tabela sincronizada, vista pelo [SyncEngine]. A ordem da lista de tabelas respeita as
 * referências (tanque antes do lote, lote antes da ração): o servidor recusa filho sem pai.
 */
interface SyncTable {
    /** Nome da tabela no servidor. */
    val remoteName: String

    /** Registros com `sincronizado = false`, **inclusive excluídos** (a exclusão também é enviada). */
    suspend fun pendingRows(): List<PendingRow>

    /** O servidor confirmou o envio: marca como enviado, se o registro não mudou desde então. */
    suspend fun markSynced(row: PendingRow)

    /** Aplica um registro vindo do servidor: deduplica por `uuid`, vence o maior `atualizadoEm`. */
    suspend fun applyRemote(json: JsonObject): MergeResult?

    /** Tira do banco os registros excluídos que o servidor já confirmou. */
    suspend fun purgeConfirmedDeleted()
}

/**
 * Tabela sincronizada de uma entity. Cada função recebida liga a regra genérica ao DAO da entity.
 * Referência para um pai que não existe aqui (`null` na conversão) deixa o registro de fora, sem travar o resto.
 */
private class EntitySyncTable<E : SyncEntity>(
    override val remoteName: String,
    private val getPending: suspend () -> List<E>,
    private val toRemote: suspend (E) -> JsonObject?,
    private val fromRemote: suspend (JsonObject) -> E?,
    private val find: suspend (String) -> E?,
    private val insert: suspend (E) -> Unit,
    private val update: suspend (E, Long) -> Unit,
    private val markSyncedAt: suspend (String, Long) -> Unit,
    private val confirmedDeleted: suspend () -> List<String>,
    private val purge: suspend (String) -> Unit,
) : SyncTable {

    override suspend fun pendingRows(): List<PendingRow> =
        getPending().mapNotNull { entity -> toRemote(entity)?.let { PendingRow(entity.uuid, entity.atualizadoEm, it) } }

    override suspend fun markSynced(row: PendingRow) = markSyncedAt(row.uuid, row.atualizadoEm)

    override suspend fun applyRemote(json: JsonObject): MergeResult? {
        val incoming = fromRemote(json) ?: return null
        // Exclusão que já foi apagada daqui: não traz o registro de volta só para guardar um túmulo.
        if (incoming.excluido && find(incoming.uuid) == null) return null
        return mergeByUuid(incoming, find = find, insert = insert, update = update)
    }

    override suspend fun purgeConfirmedDeleted() {
        for (uuid in confirmedDeleted()) {
            // Se outra tabela ainda aponta para o registro, a exclusão lógica continua valendo.
            runCatching { purge(uuid) }
        }
    }
}

/** Nome das tabelas no servidor, na ordem de sincronização (pais antes dos filhos). */
object RemoteTables {
    const val TANQUE = "tanque"
    const val USUARIO = "usuario"
    const val LOTE = "lote"
    const val QUALIDADE_AGUA = "qualidade_agua"
    const val ARRACOAMENTO = "arracoamento"
    const val BIOMETRIA = "biometria"
    const val MOVIMENTACAO_LOTE = "movimentacao_lote"
}

/**
 * Liga as sete tabelas do banco ao servidor. As colunas do servidor são `snake_case` e as referências
 * entre tabelas vão por `uuid` (o `id` local não existe fora do aparelho).
 */
fun buildSyncTables(db: AppDatabase): List<SyncTable> {
    val tanques = db.tanqueDao()
    val lotes = db.loteDao()
    val usuarios = db.usuarioDao()
    val agua = db.qualidadeAguaDao()
    val racao = db.arracoamentoDao()
    val biometrias = db.biometriaDao()
    val movimentacoes = db.movimentacaoLoteDao()

    suspend fun tankUuid(id: Long): String? = tanques.getByIdIncludingDeleted(id)?.uuid
    suspend fun tankId(uuid: String?): Long? = uuid?.let { tanques.findByUuid(it)?.id }
    suspend fun lotUuid(id: Long): String? = lotes.getByIdIncludingDeleted(id)?.uuid
    suspend fun lotId(uuid: String?): Long? = uuid?.let { lotes.findByUuid(it)?.id }

    return listOf(
        EntitySyncTable<TanqueEntity>(
            RemoteTables.TANQUE,
            getPending = tanques::getPending,
            toRemote = { e ->
                row(
                    "uuid" to e.uuid, "nome" to e.nome, "tipo" to e.tipo, "area_m2" to e.areaM2,
                    "profundidade_media_m" to e.profundidadeMediaM, "volume_m3" to e.volumeM3,
                    "usa_aerador" to e.usaAerador, "aerador_potencia_hp" to e.aeradorPotenciaHp,
                    "renovacao_agua_pct_dia" to e.renovacaoAguaPctDia, "status" to e.status,
                    "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                )
            },
            fromRemote = { j ->
                runCatching {
                    TanqueEntity(
                        nome = j.string("nome")!!, tipo = TipoTanque.valueOf(j.string("tipo")!!),
                        areaM2 = j.double("area_m2"), profundidadeMediaM = j.double("profundidade_media_m"),
                        volumeM3 = j.double("volume_m3"), usaAerador = j.boolean("usa_aerador")!!,
                        aeradorPotenciaHp = j.double("aerador_potencia_hp"),
                        renovacaoAguaPctDia = j.double("renovacao_agua_pct_dia")!!,
                        status = StatusTanque.valueOf(j.string("status")!!),
                        uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                        excluido = j.boolean("excluido") ?: false, sincronizado = true,
                    )
                }.getOrNull()
            },
            find = tanques::findByUuid,
            insert = { tanques.insert(it.copy(id = 0)) },
            update = { e, id -> tanques.update(e.copy(id = id)) },
            markSyncedAt = tanques::markSynced,
            confirmedDeleted = tanques::confirmedDeletedUuids,
            purge = tanques::purge,
        ),
        EntitySyncTable<UsuarioEntity>(
            RemoteTables.USUARIO,
            getPending = usuarios::getPending,
            toRemote = { e ->
                row(
                    "uuid" to e.uuid, "nome" to e.nome, "email" to e.email, "papel" to e.papel, "status" to e.status,
                    "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                )
            },
            fromRemote = { j ->
                runCatching {
                    UsuarioEntity(
                        nome = j.string("nome")!!, email = j.string("email")!!.trim().lowercase(),
                        papel = Papel.valueOf(j.string("papel")!!), status = StatusUsuario.valueOf(j.string("status")!!),
                        uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                        excluido = j.boolean("excluido") ?: false, sincronizado = true,
                    )
                }.getOrNull()
            },
            find = usuarios::findByUuid,
            insert = { usuarios.insert(it.copy(id = 0)) },
            update = { e, id -> usuarios.update(e.copy(id = id)) },
            markSyncedAt = usuarios::markSynced,
            confirmedDeleted = usuarios::confirmedDeletedUuids,
            purge = usuarios::purge,
        ),
        EntitySyncTable<LoteEntity>(
            RemoteTables.LOTE,
            getPending = lotes::getPending,
            toRemote = { e ->
                tankUuid(e.tanqueId)?.let { tanque ->
                    row(
                        "uuid" to e.uuid, "tanque_uuid" to tanque, "especie" to e.especie, "fase" to e.fase,
                        "quantidade_inicial" to e.quantidadeInicial, "peso_medio_inicial_g" to e.pesoMedioInicialG,
                        "data_povoamento" to e.dataPovoamento, "peso_medio_atual_g" to e.pesoMedioAtualG,
                        "origem_alevinos" to e.origemAlevinos, "status" to e.status,
                        "data_encerramento" to e.dataEncerramento,
                        "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                    )
                }
            },
            fromRemote = { j ->
                val tanque = tankId(j.string("tanque_uuid"))
                if (tanque == null) {
                    null
                } else {
                    runCatching {
                        LoteEntity(
                            tanqueId = tanque, especie = Especie.valueOf(j.string("especie")!!),
                            fase = FaseLote.valueOf(j.string("fase")!!), quantidadeInicial = j.int("quantidade_inicial")!!,
                            pesoMedioInicialG = j.double("peso_medio_inicial_g")!!, dataPovoamento = j.long("data_povoamento")!!,
                            pesoMedioAtualG = j.double("peso_medio_atual_g")!!, origemAlevinos = j.string("origem_alevinos"),
                            status = StatusLote.valueOf(j.string("status")!!), dataEncerramento = j.long("data_encerramento"),
                            uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                            excluido = j.boolean("excluido") ?: false, sincronizado = true,
                        )
                    }.getOrNull()
                }
            },
            find = lotes::findByUuid,
            insert = { lotes.insert(it.copy(id = 0)) },
            update = { e, id -> lotes.update(e.copy(id = id)) },
            markSyncedAt = lotes::markSynced,
            confirmedDeleted = lotes::confirmedDeletedUuids,
            purge = lotes::purge,
        ),
        EntitySyncTable<QualidadeAguaEntity>(
            RemoteTables.QUALIDADE_AGUA,
            getPending = agua::getPending,
            toRemote = { e ->
                tankUuid(e.tanqueId)?.let { tanque ->
                    row(
                        "uuid" to e.uuid, "tanque_uuid" to tanque, "data_hora" to e.dataHora, "momento" to e.momento,
                        "transparencia_cm" to e.transparenciaCm, "temperatura_c" to e.temperaturaC,
                        "oxigenio_mg_l" to e.oxigenioMgL, "ph" to e.ph, "amonia_total_mg_l" to e.amoniaTotalMgL,
                        "nitrito_mg_l" to e.nitritoMgL, "alcalinidade_mg_l" to e.alcalinidadeMgL,
                        "dureza_mg_l" to e.durezaMgL, "dia_nublado" to e.diaNublado,
                        "criado_por_user_id" to e.criadoPorUserId,
                        "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                    )
                }
            },
            fromRemote = { j ->
                val tanque = tankId(j.string("tanque_uuid"))
                if (tanque == null) {
                    null
                } else {
                    runCatching {
                        QualidadeAguaEntity(
                            tanqueId = tanque, dataHora = j.long("data_hora")!!,
                            momento = MomentoMedicao.valueOf(j.string("momento")!!),
                            transparenciaCm = j.double("transparencia_cm"), temperaturaC = j.double("temperatura_c"),
                            oxigenioMgL = j.double("oxigenio_mg_l"), ph = j.double("ph"),
                            amoniaTotalMgL = j.double("amonia_total_mg_l"), nitritoMgL = j.double("nitrito_mg_l"),
                            alcalinidadeMgL = j.double("alcalinidade_mg_l"), durezaMgL = j.double("dureza_mg_l"),
                            diaNublado = j.boolean("dia_nublado") ?: false, criadoPorUserId = j.string("criado_por_user_id"),
                            uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                            excluido = j.boolean("excluido") ?: false, sincronizado = true,
                        )
                    }.getOrNull()
                }
            },
            find = agua::findByUuid,
            insert = { agua.insert(it.copy(id = 0)) },
            update = { e, id -> agua.update(e.copy(id = id)) },
            markSyncedAt = agua::markSynced,
            confirmedDeleted = agua::confirmedDeletedUuids,
            purge = agua::purge,
        ),
        EntitySyncTable<ArracoamentoEntity>(
            RemoteTables.ARRACOAMENTO,
            getPending = racao::getPending,
            toRemote = { e ->
                lotUuid(e.loteId)?.let { lote ->
                    row(
                        "uuid" to e.uuid, "lote_uuid" to lote, "data_hora" to e.dataHora,
                        "quantidade_kg" to e.quantidadeKg, "suspenso" to e.suspenso,
                        "motivo_suspensao" to e.motivoSuspensao, "marca_racao" to e.marcaRacao,
                        "proteina_bruta_pct" to e.proteinaBrutaPct, "tamanho_pellet_mm" to e.tamanhoPelletMm,
                        "comportamento" to e.comportamento, "sobra_racao" to e.sobraRacao,
                        "criado_por_user_id" to e.criadoPorUserId,
                        "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                    )
                }
            },
            fromRemote = { j ->
                val lote = lotId(j.string("lote_uuid"))
                if (lote == null) {
                    null
                } else {
                    runCatching {
                        ArracoamentoEntity(
                            loteId = lote, dataHora = j.long("data_hora")!!, quantidadeKg = j.double("quantidade_kg")!!,
                            suspenso = j.boolean("suspenso") ?: false,
                            motivoSuspensao = j.string("motivo_suspensao")?.let(MotivoSuspensao::valueOf),
                            marcaRacao = j.string("marca_racao"), proteinaBrutaPct = j.double("proteina_bruta_pct"),
                            tamanhoPelletMm = j.double("tamanho_pellet_mm"),
                            comportamento = ComportamentoPeixe.valueOf(j.string("comportamento")!!),
                            sobraRacao = j.boolean("sobra_racao") ?: false, criadoPorUserId = j.string("criado_por_user_id"),
                            uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                            excluido = j.boolean("excluido") ?: false, sincronizado = true,
                        )
                    }.getOrNull()
                }
            },
            find = racao::findByUuid,
            insert = { racao.insert(it.copy(id = 0)) },
            update = { e, id -> racao.update(e.copy(id = id)) },
            markSyncedAt = racao::markSynced,
            confirmedDeleted = racao::confirmedDeletedUuids,
            purge = racao::purge,
        ),
        EntitySyncTable<BiometriaEntity>(
            RemoteTables.BIOMETRIA,
            getPending = biometrias::getPending,
            toRemote = { e ->
                lotUuid(e.loteId)?.let { lote ->
                    row(
                        "uuid" to e.uuid, "lote_uuid" to lote, "data_hora" to e.dataHora,
                        "peso_amostra_kg" to e.pesoAmostraKg, "numero_peixes_amostra" to e.numeroPeixesAmostra,
                        "observacao_saude" to e.observacaoSaude, "criado_por_user_id" to e.criadoPorUserId,
                        "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                    )
                }
            },
            fromRemote = { j ->
                val lote = lotId(j.string("lote_uuid"))
                if (lote == null) {
                    null
                } else {
                    runCatching {
                        BiometriaEntity(
                            loteId = lote, dataHora = j.long("data_hora")!!, pesoAmostraKg = j.double("peso_amostra_kg")!!,
                            numeroPeixesAmostra = j.int("numero_peixes_amostra")!!,
                            observacaoSaude = j.string("observacao_saude"), criadoPorUserId = j.string("criado_por_user_id"),
                            uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                            excluido = j.boolean("excluido") ?: false, sincronizado = true,
                        )
                    }.getOrNull()
                }
            },
            find = biometrias::findByUuid,
            insert = { biometrias.insert(it.copy(id = 0)) },
            update = { e, id -> biometrias.update(e.copy(id = id)) },
            markSyncedAt = biometrias::markSynced,
            confirmedDeleted = biometrias::confirmedDeletedUuids,
            purge = biometrias::purge,
        ),
        EntitySyncTable<MovimentacaoLoteEntity>(
            RemoteTables.MOVIMENTACAO_LOTE,
            getPending = movimentacoes::getPending,
            toRemote = { e ->
                val lote = lotUuid(e.loteId)
                val destino = e.tanqueDestinoId?.let { tankUuid(it) }
                // Transferência com destino que sumiu do banco não vai pela metade.
                if (lote == null || (e.tanqueDestinoId != null && destino == null)) {
                    null
                } else {
                    row(
                        "uuid" to e.uuid, "lote_uuid" to lote, "data_hora" to e.dataHora, "tipo" to e.tipo,
                        "quantidade" to e.quantidade, "peso_total_kg" to e.pesoTotalKg,
                        "tanque_destino_uuid" to destino, "observacao" to e.observacao,
                        "criado_por_user_id" to e.criadoPorUserId,
                        "atualizado_em" to e.atualizadoEm, "excluido" to e.excluido,
                    )
                }
            },
            fromRemote = { j ->
                val lote = lotId(j.string("lote_uuid"))
                val destinoUuid = j.string("tanque_destino_uuid")
                val destino = tankId(destinoUuid)
                if (lote == null || (destinoUuid != null && destino == null)) {
                    null
                } else {
                    runCatching {
                        MovimentacaoLoteEntity(
                            loteId = lote, dataHora = j.long("data_hora")!!,
                            tipo = TipoMovimentacao.valueOf(j.string("tipo")!!), quantidade = j.int("quantidade")!!,
                            pesoTotalKg = j.double("peso_total_kg"), tanqueDestinoId = destino,
                            observacao = j.string("observacao"), criadoPorUserId = j.string("criado_por_user_id"),
                            uuid = j.string("uuid")!!, atualizadoEm = j.long("atualizado_em")!!,
                            excluido = j.boolean("excluido") ?: false, sincronizado = true,
                        )
                    }.getOrNull()
                }
            },
            find = movimentacoes::findByUuid,
            insert = { movimentacoes.insert(it.copy(id = 0)) },
            update = { e, id -> movimentacoes.update(e.copy(id = id)) },
            markSyncedAt = movimentacoes::markSynced,
            confirmedDeleted = movimentacoes::confirmedDeletedUuids,
            purge = movimentacoes::purge,
        ),
    )
}
