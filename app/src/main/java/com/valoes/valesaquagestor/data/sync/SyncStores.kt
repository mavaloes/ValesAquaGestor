package com.valoes.valesaquagestor.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.valoes.valesaquagestor.data.local.dao.UsuarioDao
import com.valoes.valesaquagestor.data.session.SessionStore
import com.valoes.valesaquagestor.data.supabase.TokenStore
import com.valoes.valesaquagestor.domain.model.StatusUsuario
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Marca de cada tabela no DataStore do aparelho. */
@Singleton
class DataStoreSyncCursorStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SyncCursorStore {

    override suspend fun get(table: String): String? = dataStore.data.first()[key(table)]

    override suspend fun set(table: String, cursor: String) {
        dataStore.edit { it[key(table)] = cursor }
    }

    private fun key(table: String) = stringPreferencesKey("sync_cursor_$table")
}

/**
 * Depois de baixar os usuários, confere o próprio: se o ADMIN trocou o perfil, a sessão acompanha;
 * se a conta foi desativada, a sessão acaba (os registros locais ficam).
 */
@Singleton
class SessionProfileRefresher @Inject constructor(
    private val sessionStore: SessionStore,
    private val usuarios: UsuarioDao,
    private val tokens: TokenStore,
) : OwnProfileRefresher {

    override suspend fun refresh() {
        val session = sessionStore.session.first() ?: return
        val own = usuarios.findByUuid(session.userId)?.takeUnless { it.excluido } ?: return
        when {
            own.status == StatusUsuario.DESATIVADO -> {
                tokens.clear()
                sessionStore.clear()
            }

            own.papel != session.papel || own.nome != session.nome ->
                sessionStore.save(session.copy(papel = own.papel, nome = own.nome))
        }
    }
}
