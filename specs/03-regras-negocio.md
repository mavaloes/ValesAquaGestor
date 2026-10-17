# Regras de negócio (v2, baseada nos manuais)

> **Como ler as fontes:** `F13 p.27` = documento F13 da lista em `08-fontes-e-conflitos.md`, página do PDF (não a numeração impressa).
> **Regra de ouro:** nenhum número deste arquivo pode ir para o código sem a fonte no comentário (`@see F13 p.27`).
> Itens marcados com 🔶 são **decisões de projeto** (as fontes divergem ou não cobrem). Veja `08`.

---

## 1. Níveis de alerta (valem para todo parâmetro de água)

| Nível | Cor | Significado | Ação padrão |
|---|---|---|---|
| ADEQUADO | verde | Dentro da faixa ideal | Nada a fazer |
| ATENCAO | amarelo | Saiu do ideal | Reduzir ração, medir de novo |
| CRITICO | laranja | Passou do limite de alimentação | **Não alimentar**, renovar água |
| EMERGENCIA | vermelho | Risco de mortalidade | Aeração de emergência, chamar técnico |

Todo alerta mostra: **valor + o que significa + o que fazer** (requisito 4 do UX).
Limites são **inclusivos no mínimo da faixa** (ex.: 4,0 mg/L já é ADEQUADO). Nos limites superiores de pH e temperatura, o valor exato ainda pertence à faixa melhor (8,5 é ADEQUADO; 32 °C é ADEQUADO).

---

## 2. Faixas por parâmetro (tilápia, viveiro escavado)

| Parâmetro (unidade) | ADEQUADO | ATENCAO | CRITICO | EMERGENCIA | Fontes |
|---|---|---|---|---|---|
| Oxigênio dissolvido (mg/L) | ≥ 4,0 | 3,0 a < 4,0 | 2,0 a < 3,0 | < 2,0 | F01 p.59; F11 p.14; F13 p.27; F14 p.25 |
| pH | 6,5 a 8,5 🔶 | 6,0 a < 6,5 · > 8,5 a 9,0 🔶 | 4,0 a < 6,0 · > 9,0 a 11,0 | < 4,0 · > 11,0 | F11 p.20; F15 p.10; F07 p.7 |
| Temperatura (°C) | 26 a 32 | 16 a < 26 | < 16 · > 32 | — | F01 p.70; F04 p.6; F14 p.26 e p.38 |
| Amônia **tóxica** NH₃ (mg/L) | < 0,05 | 0,05 a < 0,2 | 0,2 a < 0,5 🔶 | ≥ 0,5 🔶 | F11 p.26; F13 p.38 |
| Nitrito NO₂ (mg/L) | < 0,03 | 0,03 a < 0,3 | 0,3 a < 0,7 | ≥ 0,7 | F01 p.59; F13 p.41; F11 p.27 |
| Transparência/Secchi (cm) | 30 a 45 🔶 | 20 a < 30 · > 45 a 60 | < 20 · > 60 | — | F01 p.59; F07 p.6; F10 p.2 |
| Alcalinidade (mg/L CaCO₃) | ≥ 20 | 10 a < 20 | < 10 | — | F01 p.59; F13 p.35; F11 p.13 |
| Dureza (mg/L CaCO₃) | ≥ 20 | < 20 | — | — | F10 p.2; F11 p.28; F07 p.8 |

Notas que o código deve respeitar:

- **O limite de CRITICO é o "limite para alimentar"** das fontes: O₂ 3 mg/L (F13 p.27, F14 p.25), NH₃ 0,2 (F13 p.38), NO₂ 0,3 (F13 p.41), temperatura < 16 ou > 32 °C (F14 p.38).
- **EMERGENCIA** é o limite de mortalidade: O₂ < 2 (F13 p.27), NO₂ 0,7 (F13 p.41). Para NH₃ a F13 diz 1,0 e a F11 diz 0,5; usamos o mais cuidadoso (0,5) 🔶.
- **Sem faixa por fase.** Nenhum manual traz faixas de água diferentes para alevino, recria e engorda. A fase muda a **ração** e a **frequência de medição** (seção 6), não a faixa.
- Oxigênio muito alto (> 15 mg/L) é informativo: supersaturação, risco de "trauma da bolha de gás" (F11 p.14).
- Outras espécies (tambaqui, pirarucu, "OUTRA"): usar as faixas acima como padrão de "peixes tropicais" e avisar "faixa de referência geral" no alerta.
- Alertas **compostos** na seção 3.

### Ações sugeridas por situação (texto base para `strings.xml`)

| Situação | Ação sugerida | Fonte |
|---|---|---|
| O₂ ATENCAO | "Reduza a ração. Observe se os peixes sobem à superfície." | F13 p.27 |
| O₂ CRITICO | "Não alimente agora. Renove a água e deixe o aerador pronto." | F13 p.27; F01 p.72 |
| O₂ EMERGENCIA | "Risco de mortalidade. Ligue o aerador de emergência e renove a água." | F13 p.27 |
| NH₃ / NO₂ CRITICO | "Suspenda a ração e renove a água até o valor baixar." | F13 p.38; F04 p.7 |
| pH baixo + alcalinidade < 20 | "Aplique calcário agrícola (ver dose na seção 5)." | F01 p.72; F13 p.35 |
| pH alto | "Reduza ou suspenda a ração e renove a água." | F04 p.7 |
| Transparência < 30 (água muito verde) | "Reduza a ração; risco de falta de oxigênio de madrugada." | F04 p.4; F01 p.72 |
| Transparência > 45 | "Água muito clara: verifique alcalinidade e adube." | F01 p.72; F04 p.3 |

---

## 3. Regras compostas

1. **Variação do pH no dia > 2 unidades** (manhã × fim da tarde) → ATENCAO: sinal de alcalinidade baixa ou excesso de plâncton. `F01 p.66; F04 p.4; F11 p.20`
2. **Previsão de oxigênio da madrugada** (seção 4.3): previsão < 4 mg/L → ATENCAO; < 3 → CRITICO; < 2 → EMERGENCIA. `F11 p.14-16; F13 p.30`
3. **Carga de ração por hectare** = ração diária (kg) ÷ (área m² ÷ 10.000):
   - > 40 kg/ha/dia → ATENCAO: aumenta ocorrência de oxigênio crítico (F11 p.15, Boyd 1998).
   - Sem aeração e sem troca de água: máximo recomendado 10 a 30 kg/ha/dia (F11 p.15).
   - > 60 kg/ha/dia → exigir medição de amônia total + pH + temperatura às 17h, a cada 5 dias (F13 p.38).
4. **Mortalidade acumulada do ciclo > 10%** → ATENCAO: "Chame um técnico para investigar." `F02 p.69`
5. **Peixe novo comprado** → quarentena de 45 a 90 dias em viveiro separado antes de misturar. `F02 p.35`
6. **Sobra de ração** 10 minutos depois do trato → reduzir a próxima refeição. `F14 p.46`
7. **Dias nublados seguidos** → reduzir ração e medir O₂ com mais cuidado. `F01 p.72; F04 p.2`

---

## 4. Fórmulas

### 4.1 Amônia tóxica (substitui o "amoniaPpm" do prompt original)

O kit de campo mede a **amônia total** (NH₃ + NH₄⁺). A tóxica depende de pH e temperatura. `F13 p.36-38`

```
amoniaToxicaMgL = amoniaTotalMgL × fracaoNH3(pH, temperaturaC) / 100
```

- Implementar `fracaoNH3` pela fórmula de equilíbrio químico, com pKa = 0,09018 + 2729,92 / (T + 273,15) e fração = 100 / (1 + 10^(pKa − pH)). Foi conferida contra a tabela do SENAR (F13 p.37): erro máximo de 0,25 ponto percentual. 🔶
- Alternativa fiel à fonte: usar a tabela abaixo, pegando o pH e a temperatura mais próximos.
- Se faltar pH ou temperatura no registro, usar a última leitura do mesmo tanque nas últimas 24 h; se não houver, mostrar "Informe pH e temperatura para calcular a amônia tóxica".

Tabela F13 p.37 (% de NH₃ na amônia total):

| pH \ °C | 24 | 26 | 28 | 30 | 32 |
|---|---|---|---|---|---|
| 6,5 | 0,17 | 0,19 | 0,22 | 0,25 | 0,29 |
| 7,0 | 0,52 | 0,60 | 0,69 | 0,79 | 0,90 |
| 7,5 | 1,63 | 1,87 | 2,14 | 2,45 | 2,80 |
| 8,0 | 4,97 | 5,68 | 6,48 | 7,36 | 8,35 |
| 8,5 | 14,18 | 16,00 | 17,96 | 20,08 | 22,36 |
| 9,0 | 34,32 | 37,58 | 40,91 | 44,28 | 47,66 |
| 9,5 | 62,30 | 65,56 | 68,65 | 71,53 | 74,22 |
| 10,0 | 83,94 | 85,76 | 87,38 | 88,82 | 90,10 |

### 4.2 Ração (tilápia)

Tabela F14 p.34 (temperatura 25 a 28 °C). Faixa de peso é **[mínimo, máximo)**: 50 g pertence à faixa "50 a 150".

| Peso do peixe (g) | Ração | Refeições/dia (mín.) | % do peso vivo/dia | TCA esperada |
|---|---|---|---|---|
| 1 a 5 | pó, 42% PB | 5 | 14,0 | 1,0 |
| 5 a 10 | 2-3 mm, 42% PB | 4 | 8,0 | 1,0 |
| 10 a 20 | 2-3 mm, 42% PB | 3 | 5,0 | 1,1 |
| 20 a 50 | 2-3 mm, 42% PB | 3 | 4,5 | 1,1 |
| 50 a 150 | 3-4 mm, 36% PB | 3 | 3,4 | 1,2 |
| 150 a 250 | 4-6 mm, 32% PB | 3 | 3,0 | 1,3 |
| 250 a 400 | 4-6 mm, 28-32% PB | 2 | 2,2 | 1,4 |
| 400 a 600 | 4-6 mm, 28-32% PB | 2 | 1,4 | 1,6 |
| 600 a 800 | 4-6 mm, 28-32% PB | 2 | 1,0 | 1,7 |
| 800 a 1.300 | 6-8 mm, 28-32% PB | 2 | 0,8 | 1,8 |
| 1.300 a 1.800 | 6-8 mm, 28-32% PB | 2 | 0,6 | 2,0 |

Fator por temperatura da água (F14 p.38; confirmado olhando a imagem da página). A fonte escreve números inteiros ("16 a 19", "20 a 24"); no código as faixas são contínuas: [16, 20) · [20, 25) · [25, 30) · [30, 32] · acima de 32 = 0.

| < 16 °C | 16 a 19 | 20 a 24 | 25 a 29 | 30 a 32 | > 32 °C |
|---|---|---|---|---|---|
| não fornecer | 60% | 80% | 100% | 80% | não fornecer |

```
biomassaKg      = nPeixes × pesoMedioG / 1000
racaoDiariaKg   = biomassaKg × (pctPV / 100) × fatorTemperatura
racaoPorRefeicao = racaoDiariaKg / refeicoesPorDia
```

**Travas antes de liberar o trato** (o app mostra o aviso e sugere 0 kg): O₂ < 3 mg/L (F13 p.27; a F14 p.25 diz "só iniciar acima de 3": no valor exato 3,0 as duas divergem e adotamos o permissivo, com aviso ATENCAO) · temperatura < 16 ou > 32 °C (F14 p.38) · NH₃ tóxica ≥ 0,2 ou NO₂ ≥ 0,3 (F13 p.38, p.41).
O manual avisa que a tabela é um **guia**: se houver sobra, reduzir (F14 p.34).

### 4.3 Reajuste semanal da ração e conversão alimentar

Passos (F14 p.38-41), repetidos a cada semana:

```
ganhoPesoVivoKg  = racaoOfertadaNaSemanaKg / TCAesperada(faixa atual)
pesoVivoAjustado = pesoVivoAnteriorKg + ganhoPesoVivoKg
pesoMedioAjustado = pesoVivoAjustado / nPeixes        -> nova faixa -> novo % PV
```

- Alevinos: reajustar a cada 3 ou 4 dias (F14 p.32).
- Depois de cada **biometria**, o peso médio medido substitui o estimado.
- `TCA real = racaoFornecidaKg / ganhoPesoKg` (F14 p.48). `GPD (g/dia) = (pesoMedioAtual − pesoMedioAnterior) / dias`.

### 4.4 Previsão de oxigênio da madrugada

- **Método 2 (duas medidas à noite)**: `queda/h = (O₂ primeira − O₂ segunda) / horas entre elas`; `O₂ previsto às 7h = O₂ segunda − queda/h × horas até as 7h`. `F11 p.16`
- **Horas até o limite**: `(O₂ última − 3) / queda/h`. Ex.: 19h = 7,0 e 20h = 6,5 → 0,5 mg/L/h; chega a 3 mg/L 7 h depois das 20h, ou seja, 3h da manhã → ligar o aerador. `F13 p.30`
- **Método 1 (leituras diárias manhã e tarde)**: `diferença(d) = tarde(d−1) − manhã(d)`; `previsão manhã(d+1) = tarde(d) − diferença(d)`. `F11 p.15-16` (Kubitza 2003)
- Valor previsto negativo vira 0 e nível EMERGENCIA.

### 4.5 Povoamento e capacidade

Tabela F03 p.15 (kg de peixe por 1.000 m², **colunas conferidas olhando a imagem**; o texto extraído do PDF troca as colunas):

| Condição | Alevinagem | Recria | Engorda |
|---|---|---|---|
| 1. Sem aerador, sem troca de água | 100 a 120 | 200 a 250 | 500 a 600 |
| 2. Com aerador, sem troca de água | 140 a 180 | 280 a 330 | 800 a 1.000 |
| 3. Sem aerador, com troca de 5 a 10% ao dia | 140 a 180 | 280 a 330 | 800 a 1.000 |
| 4. Com aerador e troca de 5 a 10% ao dia | 200 a 350 | 350 a 600 | 1.000 a 2.000 |

```
capacidadeKg  = produtividade(kg/1000 m²) × areaM2 / 1000      // os exemplos do manual usam o limite superior 🔶
nPeixesMax    = capacidadeKg / pesoFinalKg
```

Peso final típico: alevinagem 20 a 30 g (30 a 50 dias); recria 300 a 400 g; engorda até o peso de mercado. `F03 p.16`
Potência de aeração recomendada: 8.000 kg/ha → 5 HP; 10.000 → 6 HP; 12.000 → 8 HP. `F13 p.29`

---

## 5. Calagem (fase futura, mas as constantes já ficam definidas)

Dose de calcário agrícola por 1.000 m² de viveiro cheio, pela alcalinidade (F13 p.35; a F07 p.9 traz doses da mesma ordem de grandeza, em kg/ha: de 4.000 a 1.000 kg/ha conforme a alcalinidade):

| Alcalinidade (mg/L CaCO₃) | Dose por 1.000 m² |
|---|---|
| < 10 | 300 kg |
| 10 a 20 | 200 kg |
| 20 a 30 | 100 kg |

Medir de novo após 2 a 3 semanas. `F07 p.9`

---

## 6. Agenda de medições (alimenta a "Rotina diária", requisito 12)

| O quê | Quando | Fonte |
|---|---|---|
| Oxigênio | 2× ao dia: 6 h (antes da 1ª ração, janela 6 a 8 h) e 17 h; a ~20 cm de profundidade | F13 p.27; F01 p.59; F11 p.14 |
| Temperatura | Diária, meio do dia (e manhã/tarde antes de alimentar) | F01 p.70; F14 p.26 |
| Transparência (Secchi) | Diária, entre 11 h e 12 h | F01 p.65 |
| pH | Manhã e fim da tarde | F01 p.66; F11 p.20 |
| Amônia total, nitrito, alcalinidade, dureza | Semanal (ex.: toda segunda); amônia a cada 5 dias às 17 h se ração > 60 kg/ha/dia | F01 p.152; F15 p.15; F13 p.38 |
| Biometria | Recria: quinzenal. Engorda: mensal. (Fazenda real: ~30 e ~80 dias e véspera do abate) | F01 p.77; F15 p.15 |
| Retirar e contar peixes mortos | Diária | F02 p.69 |

Em **final de ciclo** amônia e oxigênio pioram (peixe maior, mais ração): aumentar a atenção. `F04 p.3 e p.6; F01 p.70`

Todos os horários e intervalos ficam como constantes editáveis em `MonitoringSchedule`.
