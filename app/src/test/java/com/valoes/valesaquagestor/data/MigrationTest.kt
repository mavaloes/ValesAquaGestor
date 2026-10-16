package com.valoes.valesaquagestor.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.valoes.valesaquagestor.data.local.database.AppDatabase
import com.valoes.valesaquagestor.data.local.database.AppDatabase_Impl
import com.valoes.valesaquagestor.data.local.database.MIGRATION_1_2
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migração 1 para 2 (Fase 08): confere, com o `MigrationTestHelper` e o schema exportado em
 * `app/schemas/`, que a tabela `usuario` e a coluna `criadoPorUserId` são criadas sem perder os
 * registros que já existiam.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val databaseFile = File(System.getProperty("java.io.tmpdir"), "migration-test-${UUID.randomUUID()}.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = databaseFile,
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabase_Impl() },
        autoMigrationSpecs = emptyList(),
    )

    @After
    fun tearDown() {
        databaseFile.delete()
    }

    @Test
    fun `a migracao 1 para 2 mantem os registros e deixa o criador vazio`() {
        helper.createDatabase(1).use { db ->
            db.execSQL(
                "INSERT INTO tanque (id, nome, tipo, usaAerador, renovacaoAguaPctDia, status, uuid, atualizadoEm, excluido, sincronizado) " +
                    "VALUES (1, 'Viveiro 1', 'ESCAVADO', 0, 0.0, 'ATIVO', 'tanque-1', 1000, 0, 1)",
            )
            db.execSQL(
                "INSERT INTO qualidade_agua (id, tanqueId, dataHora, momento, oxigenioMgL, diaNublado, uuid, atualizadoEm, excluido, sincronizado) " +
                    "VALUES (1, 1, 2000, 'MANHA', 5.5, 0, 'agua-1', 2000, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { db ->
            db.prepare("SELECT oxigenioMgL, criadoPorUserId, uuid, sincronizado FROM qualidade_agua").use { statement ->
                assertTrue(statement.step())
                assertEquals(5.5, statement.getDouble(0), 0.0)
                assertTrue("registros antigos ficam sem criador", statement.isNull(1))
                assertEquals("agua-1", statement.getText(2))
                assertEquals(0L, statement.getLong(3))
            }
            db.prepare("SELECT nome FROM tanque").use { statement ->
                assertTrue(statement.step())
                assertEquals("Viveiro 1", statement.getText(0))
            }
        }
    }

    @Test
    fun `depois da migracao a tabela usuario aceita usuarios e as outras tabelas tem o criador`() {
        helper.createDatabase(1).close()

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { db ->
            db.execSQL(
                "INSERT INTO usuario (nome, email, papel, status, uuid, atualizadoEm, excluido, sincronizado) " +
                    "VALUES ('Maria', 'maria@exemplo.com', 'ADMIN', 'ATIVO', 'u1', 1000, 0, 0)",
            )
            db.prepare("SELECT papel FROM usuario WHERE email = 'maria@exemplo.com'").use { statement ->
                assertTrue(statement.step())
                assertEquals("ADMIN", statement.getText(0))
            }
            listOf("qualidade_agua", "arracoamento", "biometria", "movimentacao_lote").forEach { table ->
                db.prepare("SELECT criadoPorUserId FROM $table").close()
            }
            assertEquals(1L, count(db, "usuario"))
        }
    }

    private fun count(db: SQLiteConnection, table: String): Long =
        db.prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0)
        }
}
