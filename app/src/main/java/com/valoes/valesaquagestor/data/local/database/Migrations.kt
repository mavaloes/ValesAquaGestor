package com.valoes.valesaquagestor.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versão 1 para 2 (Fase 08): cria a tabela `usuario` e acrescenta `criadoPorUserId` nos registros de
 * água, ração, biometria e movimentação. A coluna nova aceita `NULL`: os registros que já existem
 * ficam sem criador, sem perder nenhum dado. Testada em `MigrationTest` com o schema exportado.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `usuario` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`nome` TEXT NOT NULL, `email` TEXT NOT NULL, `papel` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`uuid` TEXT NOT NULL, `atualizadoEm` INTEGER NOT NULL, `excluido` INTEGER NOT NULL, " +
                "`sincronizado` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usuario_uuid` ON `usuario` (`uuid`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usuario_email` ON `usuario` (`email`)")
        listOf("qualidade_agua", "arracoamento", "biometria", "movimentacao_lote").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `criadoPorUserId` TEXT")
        }
    }
}

/**
 * Versão 2 para 3: cria a tabela `atividade` (atividades diárias do calendário, só do aparelho).
 * Não mexe em nenhuma tabela que já existia. Testada em `MigrationTest` com o schema exportado.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `atividade` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`titulo` TEXT NOT NULL, `dataEpochDia` INTEGER NOT NULL, `horaMinutos` INTEGER, " +
                "`observacao` TEXT, `concluida` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_atividade_dataEpochDia` ON `atividade` (`dataEpochDia`)")
    }
}
