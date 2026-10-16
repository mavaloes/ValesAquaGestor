package com.valoes.valesaquagestor.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.valoes.valesaquagestor.domain.calc.MonitoringSchedule
import com.valoes.valesaquagestor.domain.repository.SettingsRepository
import com.valoes.valesaquagestor.domain.rules.WaterRanges
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Ajustes do ADMIN no DataStore do aparelho. Cada campo só é gravado depois de ajustado; campo sem
 * ajuste vale o padrão das constantes com fonte, então "restaurar padrão" é apagar as chaves.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val waterRanges: Flow<WaterRanges> = preferences.map { p ->
        val d = WaterRanges.DEFAULT
        WaterRanges(
            oxygenAdequateMinMgL = p[OXYGEN_ADEQUATE_MIN] ?: d.oxygenAdequateMinMgL,
            oxygenCriticalBelowMgL = p[OXYGEN_CRITICAL_BELOW] ?: d.oxygenCriticalBelowMgL,
            oxygenEmergencyBelowMgL = p[OXYGEN_EMERGENCY_BELOW] ?: d.oxygenEmergencyBelowMgL,
            phAdequateMin = p[PH_ADEQUATE_MIN] ?: d.phAdequateMin,
            phAdequateMax = p[PH_ADEQUATE_MAX] ?: d.phAdequateMax,
            phAttentionMin = p[PH_ATTENTION_MIN] ?: d.phAttentionMin,
            phAttentionMax = p[PH_ATTENTION_MAX] ?: d.phAttentionMax,
            phCriticalMin = p[PH_CRITICAL_MIN] ?: d.phCriticalMin,
            phCriticalMax = p[PH_CRITICAL_MAX] ?: d.phCriticalMax,
            temperatureAdequateMinC = p[TEMPERATURE_ADEQUATE_MIN] ?: d.temperatureAdequateMinC,
            temperatureAdequateMaxC = p[TEMPERATURE_ADEQUATE_MAX] ?: d.temperatureAdequateMaxC,
            temperatureCriticalBelowC = p[TEMPERATURE_CRITICAL_BELOW] ?: d.temperatureCriticalBelowC,
            ammoniaAdequateBelowMgL = p[AMMONIA_ADEQUATE_BELOW] ?: d.ammoniaAdequateBelowMgL,
            ammoniaCriticalFromMgL = p[AMMONIA_CRITICAL_FROM] ?: d.ammoniaCriticalFromMgL,
            ammoniaEmergencyFromMgL = p[AMMONIA_EMERGENCY_FROM] ?: d.ammoniaEmergencyFromMgL,
            nitriteAdequateBelowMgL = p[NITRITE_ADEQUATE_BELOW] ?: d.nitriteAdequateBelowMgL,
            nitriteCriticalFromMgL = p[NITRITE_CRITICAL_FROM] ?: d.nitriteCriticalFromMgL,
            nitriteEmergencyFromMgL = p[NITRITE_EMERGENCY_FROM] ?: d.nitriteEmergencyFromMgL,
            transparencyAdequateMinCm = p[TRANSPARENCY_ADEQUATE_MIN] ?: d.transparencyAdequateMinCm,
            transparencyAdequateMaxCm = p[TRANSPARENCY_ADEQUATE_MAX] ?: d.transparencyAdequateMaxCm,
            transparencyCriticalBelowCm = p[TRANSPARENCY_CRITICAL_BELOW] ?: d.transparencyCriticalBelowCm,
            transparencyCriticalAboveCm = p[TRANSPARENCY_CRITICAL_ABOVE] ?: d.transparencyCriticalAboveCm,
            alkalinityAdequateMinMgL = p[ALKALINITY_ADEQUATE_MIN] ?: d.alkalinityAdequateMinMgL,
            alkalinityCriticalBelowMgL = p[ALKALINITY_CRITICAL_BELOW] ?: d.alkalinityCriticalBelowMgL,
            hardnessAdequateMinMgL = p[HARDNESS_ADEQUATE_MIN] ?: d.hardnessAdequateMinMgL,
        )
    }.distinctUntilChanged()

    override val monitoringSchedule: Flow<MonitoringSchedule> = preferences.map { p ->
        val d = MonitoringSchedule()
        MonitoringSchedule(
            oxygenMorningHour = p[OXYGEN_MORNING_HOUR] ?: d.oxygenMorningHour,
            oxygenAfternoonHour = p[OXYGEN_AFTERNOON_HOUR] ?: d.oxygenAfternoonHour,
            transparencyFromHour = p[TRANSPARENCY_FROM_HOUR] ?: d.transparencyFromHour,
            transparencyToHour = p[TRANSPARENCY_TO_HOUR] ?: d.transparencyToHour,
            weeklyDay = p[WEEKLY_DAY]?.let { DayOfWeek.of(it) } ?: d.weeklyDay,
            highLoadAmmoniaIntervalDays = p[HIGH_LOAD_AMMONIA_INTERVAL] ?: d.highLoadAmmoniaIntervalDays,
            highLoadAmmoniaHour = p[HIGH_LOAD_AMMONIA_HOUR] ?: d.highLoadAmmoniaHour,
            recriaBiometryIntervalDays = p[RECRIA_BIOMETRY_INTERVAL] ?: d.recriaBiometryIntervalDays,
            engordaBiometryIntervalDays = p[ENGORDA_BIOMETRY_INTERVAL] ?: d.engordaBiometryIntervalDays,
            alevinagemFeedReadjustIntervalDays = p[ALEVINAGEM_FEED_READJUST_INTERVAL] ?: d.alevinagemFeedReadjustIntervalDays,
            feedReadjustIntervalDays = p[FEED_READJUST_INTERVAL] ?: d.feedReadjustIntervalDays,
        )
    }.distinctUntilChanged()

    override suspend fun saveWaterRanges(ranges: WaterRanges) {
        dataStore.edit { p ->
            p[OXYGEN_ADEQUATE_MIN] = ranges.oxygenAdequateMinMgL
            p[OXYGEN_CRITICAL_BELOW] = ranges.oxygenCriticalBelowMgL
            p[OXYGEN_EMERGENCY_BELOW] = ranges.oxygenEmergencyBelowMgL
            p[PH_ADEQUATE_MIN] = ranges.phAdequateMin
            p[PH_ADEQUATE_MAX] = ranges.phAdequateMax
            p[PH_ATTENTION_MIN] = ranges.phAttentionMin
            p[PH_ATTENTION_MAX] = ranges.phAttentionMax
            p[PH_CRITICAL_MIN] = ranges.phCriticalMin
            p[PH_CRITICAL_MAX] = ranges.phCriticalMax
            p[TEMPERATURE_ADEQUATE_MIN] = ranges.temperatureAdequateMinC
            p[TEMPERATURE_ADEQUATE_MAX] = ranges.temperatureAdequateMaxC
            p[TEMPERATURE_CRITICAL_BELOW] = ranges.temperatureCriticalBelowC
            p[AMMONIA_ADEQUATE_BELOW] = ranges.ammoniaAdequateBelowMgL
            p[AMMONIA_CRITICAL_FROM] = ranges.ammoniaCriticalFromMgL
            p[AMMONIA_EMERGENCY_FROM] = ranges.ammoniaEmergencyFromMgL
            p[NITRITE_ADEQUATE_BELOW] = ranges.nitriteAdequateBelowMgL
            p[NITRITE_CRITICAL_FROM] = ranges.nitriteCriticalFromMgL
            p[NITRITE_EMERGENCY_FROM] = ranges.nitriteEmergencyFromMgL
            p[TRANSPARENCY_ADEQUATE_MIN] = ranges.transparencyAdequateMinCm
            p[TRANSPARENCY_ADEQUATE_MAX] = ranges.transparencyAdequateMaxCm
            p[TRANSPARENCY_CRITICAL_BELOW] = ranges.transparencyCriticalBelowCm
            p[TRANSPARENCY_CRITICAL_ABOVE] = ranges.transparencyCriticalAboveCm
            p[ALKALINITY_ADEQUATE_MIN] = ranges.alkalinityAdequateMinMgL
            p[ALKALINITY_CRITICAL_BELOW] = ranges.alkalinityCriticalBelowMgL
            p[HARDNESS_ADEQUATE_MIN] = ranges.hardnessAdequateMinMgL
        }
    }

    override suspend fun resetWaterRanges() {
        dataStore.edit { p -> waterKeys.forEach { p.remove(it) } }
    }

    override suspend fun saveMonitoringSchedule(schedule: MonitoringSchedule) {
        dataStore.edit { p ->
            p[OXYGEN_MORNING_HOUR] = schedule.oxygenMorningHour
            p[OXYGEN_AFTERNOON_HOUR] = schedule.oxygenAfternoonHour
            p[TRANSPARENCY_FROM_HOUR] = schedule.transparencyFromHour
            p[TRANSPARENCY_TO_HOUR] = schedule.transparencyToHour
            p[WEEKLY_DAY] = schedule.weeklyDay.value
            p[HIGH_LOAD_AMMONIA_INTERVAL] = schedule.highLoadAmmoniaIntervalDays
            p[HIGH_LOAD_AMMONIA_HOUR] = schedule.highLoadAmmoniaHour
            p[RECRIA_BIOMETRY_INTERVAL] = schedule.recriaBiometryIntervalDays
            p[ENGORDA_BIOMETRY_INTERVAL] = schedule.engordaBiometryIntervalDays
            p[ALEVINAGEM_FEED_READJUST_INTERVAL] = schedule.alevinagemFeedReadjustIntervalDays
            p[FEED_READJUST_INTERVAL] = schedule.feedReadjustIntervalDays
        }
    }

    override suspend fun resetMonitoringSchedule() {
        dataStore.edit { p -> scheduleKeys.forEach { p.remove(it) } }
    }

    private companion object {
        fun water(name: String) = doublePreferencesKey("water_$name")
        fun schedule(name: String) = intPreferencesKey("schedule_$name")

        val OXYGEN_ADEQUATE_MIN = water("oxygen_adequate_min")
        val OXYGEN_CRITICAL_BELOW = water("oxygen_critical_below")
        val OXYGEN_EMERGENCY_BELOW = water("oxygen_emergency_below")
        val PH_ADEQUATE_MIN = water("ph_adequate_min")
        val PH_ADEQUATE_MAX = water("ph_adequate_max")
        val PH_ATTENTION_MIN = water("ph_attention_min")
        val PH_ATTENTION_MAX = water("ph_attention_max")
        val PH_CRITICAL_MIN = water("ph_critical_min")
        val PH_CRITICAL_MAX = water("ph_critical_max")
        val TEMPERATURE_ADEQUATE_MIN = water("temperature_adequate_min")
        val TEMPERATURE_ADEQUATE_MAX = water("temperature_adequate_max")
        val TEMPERATURE_CRITICAL_BELOW = water("temperature_critical_below")
        val AMMONIA_ADEQUATE_BELOW = water("ammonia_adequate_below")
        val AMMONIA_CRITICAL_FROM = water("ammonia_critical_from")
        val AMMONIA_EMERGENCY_FROM = water("ammonia_emergency_from")
        val NITRITE_ADEQUATE_BELOW = water("nitrite_adequate_below")
        val NITRITE_CRITICAL_FROM = water("nitrite_critical_from")
        val NITRITE_EMERGENCY_FROM = water("nitrite_emergency_from")
        val TRANSPARENCY_ADEQUATE_MIN = water("transparency_adequate_min")
        val TRANSPARENCY_ADEQUATE_MAX = water("transparency_adequate_max")
        val TRANSPARENCY_CRITICAL_BELOW = water("transparency_critical_below")
        val TRANSPARENCY_CRITICAL_ABOVE = water("transparency_critical_above")
        val ALKALINITY_ADEQUATE_MIN = water("alkalinity_adequate_min")
        val ALKALINITY_CRITICAL_BELOW = water("alkalinity_critical_below")
        val HARDNESS_ADEQUATE_MIN = water("hardness_adequate_min")

        val OXYGEN_MORNING_HOUR = schedule("oxygen_morning_hour")
        val OXYGEN_AFTERNOON_HOUR = schedule("oxygen_afternoon_hour")
        val TRANSPARENCY_FROM_HOUR = schedule("transparency_from_hour")
        val TRANSPARENCY_TO_HOUR = schedule("transparency_to_hour")
        val WEEKLY_DAY = schedule("weekly_day")
        val HIGH_LOAD_AMMONIA_INTERVAL = schedule("high_load_ammonia_interval")
        val HIGH_LOAD_AMMONIA_HOUR = schedule("high_load_ammonia_hour")
        val RECRIA_BIOMETRY_INTERVAL = schedule("recria_biometry_interval")
        val ENGORDA_BIOMETRY_INTERVAL = schedule("engorda_biometry_interval")
        val ALEVINAGEM_FEED_READJUST_INTERVAL = schedule("alevinagem_feed_readjust_interval")
        val FEED_READJUST_INTERVAL = schedule("feed_readjust_interval")

        val waterKeys = listOf(
            OXYGEN_ADEQUATE_MIN, OXYGEN_CRITICAL_BELOW, OXYGEN_EMERGENCY_BELOW,
            PH_ADEQUATE_MIN, PH_ADEQUATE_MAX, PH_ATTENTION_MIN, PH_ATTENTION_MAX, PH_CRITICAL_MIN, PH_CRITICAL_MAX,
            TEMPERATURE_ADEQUATE_MIN, TEMPERATURE_ADEQUATE_MAX, TEMPERATURE_CRITICAL_BELOW,
            AMMONIA_ADEQUATE_BELOW, AMMONIA_CRITICAL_FROM, AMMONIA_EMERGENCY_FROM,
            NITRITE_ADEQUATE_BELOW, NITRITE_CRITICAL_FROM, NITRITE_EMERGENCY_FROM,
            TRANSPARENCY_ADEQUATE_MIN, TRANSPARENCY_ADEQUATE_MAX, TRANSPARENCY_CRITICAL_BELOW, TRANSPARENCY_CRITICAL_ABOVE,
            ALKALINITY_ADEQUATE_MIN, ALKALINITY_CRITICAL_BELOW, HARDNESS_ADEQUATE_MIN,
        )

        val scheduleKeys = listOf(
            OXYGEN_MORNING_HOUR, OXYGEN_AFTERNOON_HOUR, TRANSPARENCY_FROM_HOUR, TRANSPARENCY_TO_HOUR, WEEKLY_DAY,
            HIGH_LOAD_AMMONIA_INTERVAL, HIGH_LOAD_AMMONIA_HOUR, RECRIA_BIOMETRY_INTERVAL, ENGORDA_BIOMETRY_INTERVAL,
            ALEVINAGEM_FEED_READJUST_INTERVAL, FEED_READJUST_INTERVAL,
        )
    }
}
