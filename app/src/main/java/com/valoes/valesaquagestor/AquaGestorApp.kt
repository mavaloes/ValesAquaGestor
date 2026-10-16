package com.valoes.valesaquagestor

import android.app.Application
import com.valoes.valesaquagestor.data.sync.SyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application do app: ponto de entrada do Hilt (injeção de dependências) e início da sincronização. */
@HiltAndroidApp
class AquaGestorApp : Application() {

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    override fun onCreate() {
        super.onCreate()
        // Pede a sincronização sozinho (ao entrar e quando houver registros pendentes), sem depender de nenhuma tela.
        syncCoordinator.start()
    }
}
