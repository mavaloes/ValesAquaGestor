package com.valoes.valesaquagestor.data.local.converter

import androidx.room.TypeConverter
import com.valoes.valesaquagestor.domain.model.ComportamentoPeixe
import com.valoes.valesaquagestor.domain.model.Especie
import com.valoes.valesaquagestor.domain.model.FaseLote
import com.valoes.valesaquagestor.domain.model.MomentoMedicao
import com.valoes.valesaquagestor.domain.model.MotivoSuspensao
import com.valoes.valesaquagestor.domain.model.StatusLote
import com.valoes.valesaquagestor.domain.model.StatusTanque
import com.valoes.valesaquagestor.domain.model.TipoMovimentacao
import com.valoes.valesaquagestor.domain.model.TipoTanque

/**
 * Grava os enums do domínio como `String` no Room (decisão de `specs/02-dominio.md`).
 *
 * O valor gravado é o `name` da constante. Por isso, **nunca renomeie** uma constante de enum
 * depois de publicada: os registros antigos (no aparelho e no servidor) deixariam de ser lidos.
 */
class EnumConverters {

    @TypeConverter
    fun fromTipoTanque(value: TipoTanque): String = value.name

    @TypeConverter
    fun toTipoTanque(value: String): TipoTanque = TipoTanque.valueOf(value)

    @TypeConverter
    fun fromStatusTanque(value: StatusTanque): String = value.name

    @TypeConverter
    fun toStatusTanque(value: String): StatusTanque = StatusTanque.valueOf(value)

    @TypeConverter
    fun fromEspecie(value: Especie): String = value.name

    @TypeConverter
    fun toEspecie(value: String): Especie = Especie.valueOf(value)

    @TypeConverter
    fun fromFaseLote(value: FaseLote): String = value.name

    @TypeConverter
    fun toFaseLote(value: String): FaseLote = FaseLote.valueOf(value)

    @TypeConverter
    fun fromStatusLote(value: StatusLote): String = value.name

    @TypeConverter
    fun toStatusLote(value: String): StatusLote = StatusLote.valueOf(value)

    @TypeConverter
    fun fromMomentoMedicao(value: MomentoMedicao): String = value.name

    @TypeConverter
    fun toMomentoMedicao(value: String): MomentoMedicao = MomentoMedicao.valueOf(value)

    @TypeConverter
    fun fromMotivoSuspensao(value: MotivoSuspensao?): String? = value?.name

    @TypeConverter
    fun toMotivoSuspensao(value: String?): MotivoSuspensao? = value?.let(MotivoSuspensao::valueOf)

    @TypeConverter
    fun fromComportamentoPeixe(value: ComportamentoPeixe): String = value.name

    @TypeConverter
    fun toComportamentoPeixe(value: String): ComportamentoPeixe = ComportamentoPeixe.valueOf(value)

    @TypeConverter
    fun fromTipoMovimentacao(value: TipoMovimentacao): String = value.name

    @TypeConverter
    fun toTipoMovimentacao(value: String): TipoMovimentacao = TipoMovimentacao.valueOf(value)
}
