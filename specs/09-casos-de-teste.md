# Casos de teste (tirados dos exemplos dos manuais)

Cada caso vira um teste JUnit em `domain/` (Kotlin puro, sem Android). Os valores esperados foram **recalculados em código**, não copiados dos PDFs, porque os PDFs têm erros de conta (ver `08`, seção 4).
Tolerância padrão: `1e-6`, exceto onde indicado.

## A. Amônia tóxica (F13 p.37-38)

| # | amônia total (mg/L) | pH | °C | Esperado (mg/L) | Observação |
|---|---|---|---|---|---|
| A1 | 1,5 | 7,0 | 30 | 0,01185 (± 0,0002 se usar a fórmula; a fonte arredonda para 0,0118) | NH₃ inofensiva |
| A2 | 1,5 | 9,0 | 30 | 0,6642 (± 0,005 se usar a fórmula) | Mesma amônia total, pH alto: já é EMERGENCIA |
| A3 | qualquer | cada nó da tabela | cada nó | `total × tabela / 100` | Se usar a tabela, deve bater exato; se usar a fórmula, erro ≤ 0,3 ponto percentual |

## B. Ração da tilápia (F14 p.34-38)

| # | Entrada | Esperado |
|---|---|---|
| B1 | 500 peixes × 43 g | biomassa 21,5 kg; faixa 20 a 50 g; 4,5% → **0,9675 kg/dia**; 3 refeições → **0,3225 kg** cada |
| B2 | Fator de temperatura em 23 °C | 0,8. Com 8 kg calculados → **6,4 kg** (a fonte diz 64 kg, erro) |
| B3 | Temperaturas-limite | 15,9 °C → 0 (não fornecer) · 16 → 0,6 · 20 → 0,8 · 25 → 1,0 · 30 → 0,8 · 32 → 0,8 · 32,1 → 0 |
| B4 | Peso exatamente 50 g | cai na faixa "50 a 150" (3,4%), pois a faixa é [mín, máx) |
| B5 | Trava de O₂ | O₂ = 2,9 mg/L → ração 0 · O₂ = 3,0 → permitido, com aviso ATENCAO (F13 e F14 divergem só no valor exato 3,0) |

## C. Reajuste semanal e conversão (F14 p.40-41 e p.48)

Tolerância desta seção: os valores esperados estão arredondados. Usar **± 0,005 kg** nas quantidades e **± 0,05 g** no peso médio (ex.: C1 dá exatamente 287,49 g; C2 dá 31,6239 kg/dia e 15,812 kg por refeição).

| # | Entrada | Esperado |
|---|---|---|
| C1 | 5.000 peixes × 259 g; 199,43 kg de ração na semana; TCA da faixa = 1,4 | PV = **1.295 kg**; ganho = **142,45 kg**; PV ajustado = **1.437,45 kg**; peso médio = **287,5 g** |
| C2 | Continuação de C1 (faixa 250 a 400 g, 2,2%, 2 refeições) | ração = **31,62 kg/dia**; **15,81 kg** por refeição (a fonte mostra 31,64 e 15,82 por usar 1.296) |
| C3 | 500 peixes: de 200 g para 450 g; 190 kg de ração | ganho = **125 kg**; TCA = **1,52** |
| C4 | Tambaqui, 375 peixes × 100 g (F01 p.78) | 37,5 kg; faixa 70 a 200 g, 3,0%; **1,125 kg/dia**; 2 refeições → **0,5625 kg** (fase seguinte: **fora da Fase 02**, a tabela do tambaqui ainda não está em `03`) |

## D. Capacidade do viveiro (F03 p.17-21)

| # | Entrada | Esperado |
|---|---|---|
| D1 | Alevinagem, 225 m², condição 1, limite superior 120 kg/1.000 m², 20 g | 27 kg → **1.350 alevinos** |
| D2 | Recria, 1.500 m², condição 1, superior 250, 300 g | 375 kg → **1.250 peixes** |
| D3 | Engorda, 4.025 m², condição 1, superior 600, 2 kg | 2.415 kg → **1.207 peixes** (arredondar para baixo; resultado exato 1.207,5) |

## E. Oxigênio (F11 p.14-16; F13 p.30)

| # | Entrada | Esperado |
|---|---|---|
| E1 | Método 2: 20 h = 12 mg/L; 22 h = 10 mg/L | queda 1 mg/L por hora; previsão às 7 h = **1,0 mg/L** (EMERGENCIA) |
| E2 | Método 2: 19 h = 7,0; 20 h = 6,5 | queda 0,5 mg/L/h; chega a 3 mg/L **7 h depois das 20 h** (03:00) |
| E3 | Método 1, série tarde/manhã da F11 p.16 (tarde 4/6 = 14,2; manhã 5/6 = 5,5; tarde 5/6 = 15,0) | previsão da manhã de 6/6 = **6,3** |
| E4 | Continuação (previsões para 7/6 a 12/6) | **6,8 · 4,8 · 4,0 · 2,2 · 1,8 · −0,2** (o último vira 0 e EMERGENCIA). **Aguardando dados:** a série de leituras de manhã e de tarde de 6/6 a 11/6 (F11 p.16) precisa ser conferida na página e copiada aqui antes da Fase 02, Etapa 5 |
| E5 | Níveis de O₂ | 4,0 → ADEQUADO · 3,99 → ATENCAO · 3,0 → ATENCAO · 2,99 → CRITICO · 2,0 → CRITICO · 1,99 → EMERGENCIA |

## F. Alertas compostos

| # | Entrada | Esperado |
|---|---|---|
| F1 | 4 kg de ração/dia em viveiro de 1.000 m² | **40 kg/ha/dia** → exatamente no limite: sem alerta (o alerta é acima de 40) |
| F2 | 6,1 kg de ração/dia em 1.000 m² | **61 kg/ha/dia** → exigir amônia a cada 5 dias às 17 h |
| F3 | Lote de 1.000 peixes, 101 mortos no ciclo | **10,1%** → ATENCAO "chame um técnico" |
| F4 | pH 6,8 de manhã e 9,0 no fim da tarde | variação 2,2 > 2 → ATENCAO |
| F5 | pH 6,5 · 8,5 · 8,51 · 9,0 · 9,01 · 4,0 · 11,0 · 11,01 | ADEQUADO · ADEQUADO · ATENCAO · ATENCAO · CRITICO · CRITICO · CRITICO · EMERGENCIA |

## G. Sincronização (do requisito 7)

| # | Cenário | Esperado |
|---|---|---|
| G1 | Mesmo registro enviado duas vezes (mesmo `uuid`) | 1 registro no servidor |
| G2 | Edição offline em dois aparelhos | vence o maior `atualizadoEm` |
| G3 | Registro `excluido = true` ainda não sincronizado | não aparece nas listas, mas não some do banco |
