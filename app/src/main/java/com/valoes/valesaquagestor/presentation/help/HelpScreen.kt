package com.valoes.valesaquagestor.presentation.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valoes.valesaquagestor.R
import com.valoes.valesaquagestor.presentation.components.AppTopBar
import com.valoes.valesaquagestor.presentation.components.SectionTitle
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme

/** Um passo a passo curto: título e passos numerados. */
class HelpTutorial(@StringRes val title: Int, val steps: List<Int>)

/** Uma palavra do campo explicada em linguagem simples (`specs/00-glossario.md`). */
class GlossaryEntry(@StringRes val term: Int, @StringRes val text: Int)

/** Os tutoriais da tela de Ajuda: registrar ração, medir a água e fazer biometria. */
val helpTutorials = listOf(
    HelpTutorial(
        R.string.help_feed_title,
        listOf(R.string.help_feed_step1, R.string.help_feed_step2, R.string.help_feed_step3, R.string.help_feed_step4),
    ),
    HelpTutorial(
        R.string.help_water_title,
        listOf(R.string.help_water_step1, R.string.help_water_step2, R.string.help_water_step3, R.string.help_water_step4),
    ),
    HelpTutorial(
        R.string.help_biometry_title,
        listOf(R.string.help_biometry_step1, R.string.help_biometry_step2, R.string.help_biometry_step3, R.string.help_biometry_step4),
    ),
)

/** As palavras do campo que o app usa, do glossário. */
val helpGlossary = listOf(
    GlossaryEntry(R.string.glossary_viveiro_term, R.string.glossary_viveiro_text),
    GlossaryEntry(R.string.glossary_acude_term, R.string.glossary_acude_text),
    GlossaryEntry(R.string.glossary_tanque_rede_term, R.string.glossary_tanque_rede_text),
    GlossaryEntry(R.string.glossary_fases_term, R.string.glossary_fases_text),
    GlossaryEntry(R.string.glossary_povoamento_term, R.string.glossary_povoamento_text),
    GlossaryEntry(R.string.glossary_biometria_term, R.string.glossary_biometria_text),
    GlossaryEntry(R.string.glossary_biomassa_term, R.string.glossary_biomassa_text),
    GlossaryEntry(R.string.glossary_arracoamento_term, R.string.glossary_arracoamento_text),
    GlossaryEntry(R.string.glossary_pb_term, R.string.glossary_pb_text),
    GlossaryEntry(R.string.glossary_tca_term, R.string.glossary_tca_text),
    GlossaryEntry(R.string.glossary_od_term, R.string.glossary_od_text),
    GlossaryEntry(R.string.glossary_secchi_term, R.string.glossary_secchi_text),
    GlossaryEntry(R.string.glossary_amonia_term, R.string.glossary_amonia_text),
    GlossaryEntry(R.string.glossary_alcalinidade_term, R.string.glossary_alcalinidade_text),
    GlossaryEntry(R.string.glossary_despesca_term, R.string.glossary_despesca_text),
)

/** Tela de Ajuda: tutoriais curtos e as palavras do campo explicadas em linguagem simples. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(R.string.help_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(text = stringResource(R.string.help_tutorials_title))
            helpTutorials.forEach { tutorial ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(tutorial.title), style = MaterialTheme.typography.titleMedium)
                        tutorial.steps.forEachIndexed { index, step ->
                            Text(
                                text = stringResource(R.string.help_step_number, index + 1, stringResource(step)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            SectionTitle(text = stringResource(R.string.help_glossary_title))
            helpGlossary.forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(entry.term), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(entry.text), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Preview(name = "Ajuda", showBackground = true, fontScale = 2f)
@Composable
private fun HelpScreenPreview() {
    AquaGestorTheme { HelpScreen(onBack = {}) }
}
