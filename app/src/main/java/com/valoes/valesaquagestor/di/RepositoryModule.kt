package com.valoes.valesaquagestor.di

import com.valoes.valesaquagestor.data.local.database.RoomTransactionRunner
import com.valoes.valesaquagestor.data.local.database.TransactionRunner
import com.valoes.valesaquagestor.data.repository.BiometryRepositoryImpl
import com.valoes.valesaquagestor.data.repository.FeedingRepositoryImpl
import com.valoes.valesaquagestor.data.repository.LotMovementRepositoryImpl
import com.valoes.valesaquagestor.data.repository.LotRepositoryImpl
import com.valoes.valesaquagestor.data.repository.PendingSyncRepositoryImpl
import com.valoes.valesaquagestor.data.repository.TankRepositoryImpl
import com.valoes.valesaquagestor.data.repository.UserRepositoryImpl
import com.valoes.valesaquagestor.data.repository.WaterQualityRepositoryImpl
import com.valoes.valesaquagestor.domain.repository.BiometryRepository
import com.valoes.valesaquagestor.domain.repository.FeedingRepository
import com.valoes.valesaquagestor.domain.repository.LotMovementRepository
import com.valoes.valesaquagestor.domain.repository.LotRepository
import com.valoes.valesaquagestor.domain.repository.PendingSyncRepository
import com.valoes.valesaquagestor.domain.repository.TankRepository
import com.valoes.valesaquagestor.domain.repository.UserRepository
import com.valoes.valesaquagestor.domain.repository.WaterQualityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Liga cada interface do domínio à sua implementação em `data`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindTankRepository(impl: TankRepositoryImpl): TankRepository

    @Binds
    @Singleton
    abstract fun bindLotRepository(impl: LotRepositoryImpl): LotRepository

    @Binds
    @Singleton
    abstract fun bindWaterQualityRepository(impl: WaterQualityRepositoryImpl): WaterQualityRepository

    @Binds
    @Singleton
    abstract fun bindFeedingRepository(impl: FeedingRepositoryImpl): FeedingRepository

    @Binds
    @Singleton
    abstract fun bindBiometryRepository(impl: BiometryRepositoryImpl): BiometryRepository

    @Binds
    @Singleton
    abstract fun bindLotMovementRepository(impl: LotMovementRepositoryImpl): LotMovementRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindPendingSyncRepository(impl: PendingSyncRepositoryImpl): PendingSyncRepository
}
