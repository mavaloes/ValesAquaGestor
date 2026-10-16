package com.valoes.valesaquagestor.di

import android.content.Context
import androidx.room.Room
import com.valoes.valesaquagestor.data.local.dao.ArracoamentoDao
import com.valoes.valesaquagestor.data.local.dao.BiometriaDao
import com.valoes.valesaquagestor.data.local.dao.LoteDao
import com.valoes.valesaquagestor.data.local.dao.MovimentacaoLoteDao
import com.valoes.valesaquagestor.data.local.dao.QualidadeAguaDao
import com.valoes.valesaquagestor.data.local.dao.TanqueDao
import com.valoes.valesaquagestor.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Fornece o banco, os DAOs e o relógio.
 *
 * O banco é criado **sem** `fallbackToDestructiveMigration`: uma mudança de versão sem migração
 * falha de propósito, em vez de apagar registros ainda não sincronizados.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideTanqueDao(database: AppDatabase): TanqueDao = database.tanqueDao()

    @Provides
    fun provideLoteDao(database: AppDatabase): LoteDao = database.loteDao()

    @Provides
    fun provideQualidadeAguaDao(database: AppDatabase): QualidadeAguaDao = database.qualidadeAguaDao()

    @Provides
    fun provideArracoamentoDao(database: AppDatabase): ArracoamentoDao = database.arracoamentoDao()

    @Provides
    fun provideBiometriaDao(database: AppDatabase): BiometriaDao = database.biometriaDao()

    @Provides
    fun provideMovimentacaoLoteDao(database: AppDatabase): MovimentacaoLoteDao = database.movimentacaoLoteDao()

    /** Relógio do sistema; os testes trocam por um relógio fixo. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
