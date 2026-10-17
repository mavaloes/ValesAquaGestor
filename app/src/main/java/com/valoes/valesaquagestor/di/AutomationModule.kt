package com.valoes.valesaquagestor.di

import com.valoes.valesaquagestor.data.sensor.ManualWaterSensorSource
import com.valoes.valesaquagestor.domain.sensor.WaterSensorSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Liga a interface de sensor à implementação em uso (digitada, até o protocolo do sensor ser escolhido). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    @Singleton
    abstract fun bindWaterSensorSource(impl: ManualWaterSensorSource): WaterSensorSource
}
