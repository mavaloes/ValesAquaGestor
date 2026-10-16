package com.valoes.valesaquagestor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valoes.valesaquagestor.data.local.converter.EnumConverters
import com.valoes.valesaquagestor.data.local.dao.ArracoamentoDao
import com.valoes.valesaquagestor.data.local.dao.BiometriaDao
import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.dao.MovimentacaoLoteDao
import com.valoes.valesaquagestor.data.local.dao.QualidadeAguaDao
import com.valoes.valesaquagestor.data.local.dao.TanqueDao
import com.valoes.valesaquagestor.data.local.dao.UsuarioDao
import com.valoes.valesaquagestor.data.local.entity.ArracoamentoEntity
import com.valoes.valesaquagestor.data.local.entity.BiometriaEntity
import com.valoes.valesaquagestor.data.local.entity.LoteEntity
import com.valoes.valesaquagestor.data.local.entity.MovimentacaoLoteEntity
import com.valoes.valesaquagestor.data.local.entity.QualidadeAguaEntity
import com.valoes.valesaquagestor.data.local.entity.TanqueEntity
import com.valoes.valesaquagestor.data.local.entity.UsuarioEntity

/**
 * Banco local (Room): a fonte da verdade do app, que nunca depende de internet para salvar.
 *
 * Versão 2 (a Fase 08 acrescentou a tabela `usuario` e `criadoPorUserId` nos registros; ver
 * [MIGRATION_1_2]), com `exportSchema = true` (o schema fica em `app/schemas/`). Mudanças de estrutura
 * exigem `Migration` com teste: o app **não** usa `fallbackToDestructiveMigration`, porque
 * apagaria o trabalho de campo que ainda não foi sincronizado.
 */
@Database(
    entities = [
        TanqueEntity::class,
        LoteEntity::class,
        QualidadeAguaEntity::class,
        ArracoamentoEntity::class,
        BiometriaEntity::class,
        MovimentacaoLoteEntity::class,
        UsuarioEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tanqueDao(): TanqueDao
    abstract fun loteDao(): LoteDao
    abstract fun qualidadeAguaDao(): QualidadeAguaDao
    abstract fun arracoamentoDao(): ArracoamentoDao
    abstract fun biometriaDao(): BiometriaDao
    abstract fun movimentacaoLoteDao(): MovimentacaoLoteDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        const val NAME = "aquagestor.db"
    }
}
