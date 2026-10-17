package com.valoes.valesaquagestor.presentation.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valoes.valesaquagestor.domain.model.Atividade
import com.valoes.valesaquagestor.domain.repository.ActivityRepository
import com.valoes.valesaquagestor.presentation.util.FormError
import com.valoes.valesaquagestor.presentation.util.FormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Formulário de atividade. Os campos ficam como texto enquanto o usuário digita.
 *
 * @property id `0` para uma atividade nova; senão, a atividade que está sendo editada.
 * @property hora hora marcada, ou `null` para "qualquer hora do dia".
 */
data class ActivityForm(
    val id: Long = 0,
    val titulo: String = "",
    val hora: LocalTime? = null,
    val observacao: String = "",
    val concluida: Boolean = false,
    val tituloError: FormError? = null,
) {
    /** O botão Salvar só habilita com o título preenchido. */
    val canSave: Boolean get() = titulo.isNotBlank()
}

/** Aviso que a tela mostra depois de uma ação. */
enum class ActivitiesMessage {
    SAVED,
    DELETED,
}

/**
 * Estado da tela de atividades.
 *
 * @property month mês mostrado no calendário.
 * @property today dia de hoje.
 * @property selected dia escolhido no calendário; a lista mostra as atividades dele.
 * @property monthActivities todas as atividades do [month].
 * @property form formulário aberto (nova atividade ou edição), ou `null`.
 * @property deleting atividade que está sendo confirmada para apagar, ou `null`.
 */
data class ActivitiesUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    val monthActivities: List<Atividade> = emptyList(),
    val form: ActivityForm? = null,
    val deleting: Atividade? = null,
    val message: ActivitiesMessage? = null,
) {
    /** Dias do mês com pelo menos uma atividade, para marcar no calendário. */
    val daysWithActivities: Set<LocalDate> get() = monthActivities.mapTo(mutableSetOf()) { it.data }

    /** Dias do mês com atividade ainda não concluída. */
    val daysWithPending: Set<LocalDate> get() = monthActivities.filterNot { it.concluida }.mapTo(mutableSetOf()) { it.data }

    /** Atividades do dia escolhido, em ordem de hora. */
    val selectedActivities: List<Atividade> get() = monthActivities.filter { it.data == selected }
}

/** Calendário de atividades diárias: escolher o dia, cadastrar, editar, concluir e apagar. */
@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val repository: ActivityRepository,
    clock: Clock,
) : ViewModel() {

    // O relógio do app é UTC; o "hoje" do calendário é o do fuso do aparelho.
    private val today: LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    /** O que só existe na tela. As atividades vêm do banco. */
    private data class Local(
        val month: YearMonth,
        val selected: LocalDate,
        val form: ActivityForm? = null,
        val deleting: Atividade? = null,
        val message: ActivitiesMessage? = null,
    )

    private val local = MutableStateFlow(Local(month = YearMonth.from(today), selected = today))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthActivities = local
        .map { it.month }
        .distinctUntilChanged()
        .flatMapLatest { month -> repository.observeBetween(month.atDay(1), month.atEndOfMonth()) }

    val state: StateFlow<ActivitiesUiState> = combine(local, monthActivities) { local, activities ->
        ActivitiesUiState(
            month = local.month,
            today = today,
            selected = local.selected,
            monthActivities = activities,
            form = local.form,
            deleting = local.deleting,
            message = local.message,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        ActivitiesUiState(month = YearMonth.from(today), today = today, selected = today),
    )

    fun onPreviousMonth() = local.update { it.copy(month = it.month.minusMonths(1)) }

    fun onNextMonth() = local.update { it.copy(month = it.month.plusMonths(1)) }

    /** Escolhe o dia. Tocar num dia de outro mês leva o calendário para esse mês. */
    fun onSelectDay(day: LocalDate) = local.update { it.copy(selected = day, month = YearMonth.from(day)) }

    fun onGoToToday() = local.update { it.copy(selected = today, month = YearMonth.from(today)) }

    fun onNewActivity() = local.update { it.copy(form = ActivityForm()) }

    fun onEditActivity(activity: Atividade) = local.update {
        it.copy(
            form = ActivityForm(
                id = activity.id,
                titulo = activity.titulo,
                hora = activity.hora,
                observacao = activity.observacao.orEmpty(),
                concluida = activity.concluida,
            ),
        )
    }

    fun onFormDismiss() = local.update { it.copy(form = null) }

    fun onTitleChange(value: String) =
        local.update { s -> s.copy(form = s.form?.copy(titulo = value, tituloError = null)) }

    fun onTimeChange(value: LocalTime?) = local.update { s -> s.copy(form = s.form?.copy(hora = value)) }

    fun onNotesChange(value: String) = local.update { s -> s.copy(form = s.form?.copy(observacao = value)) }

    /** Valida e salva no dia escolhido. Título em branco não salva e mostra o erro no campo. */
    fun onSave() {
        val current = local.value
        val form = current.form ?: return
        val error = FormValidation.requiredText(form.titulo)
        if (error != null) {
            local.update { it.copy(form = form.copy(tituloError = error)) }
            return
        }
        // O formulário fecha já, para um segundo toque em Salvar não gravar duas vezes.
        local.update { it.copy(form = null) }
        viewModelScope.launch {
            repository.save(
                Atividade(
                    id = form.id,
                    titulo = form.titulo,
                    data = current.selected,
                    hora = form.hora,
                    observacao = form.observacao,
                    concluida = form.concluida,
                ),
            )
            local.update { it.copy(message = ActivitiesMessage.SAVED) }
        }
    }

    fun onToggleDone(activity: Atividade) {
        viewModelScope.launch { repository.setDone(activity.id, !activity.concluida) }
    }

    fun onDeleteClick(activity: Atividade) = local.update { it.copy(deleting = activity) }

    fun onDeleteDismiss() = local.update { it.copy(deleting = null) }

    fun onDeleteConfirm() {
        val activity = local.value.deleting ?: return
        local.update { it.copy(deleting = null) }
        viewModelScope.launch {
            repository.delete(activity.id)
            local.update { it.copy(message = ActivitiesMessage.DELETED) }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
