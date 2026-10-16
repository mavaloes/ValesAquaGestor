# Fontes, conflitos e decisões

Este arquivo é o "diário de bordo" dos números do app. Se um valor mudar no código, ele muda aqui primeiro.

## 1. Fontes (PDFs analisados)

As páginas citadas (`p.`) são as do **PDF**, não a numeração impressa na página.

| ID | Documento | Ano | Uso no projeto | Cuidado |
|---|---|---|---|---|
| F01 | Embrapa, *Manual de piscicultura familiar em viveiros escavados*, 2ª ed. | 2024 | Fonte mais recente. Faixas de água, 7 planilhas de campo, tabelas de tambaqui e pirarucu | Não é específico para tilápia |
| F02 | SENAR 196, *Piscicultura: manejo sanitário* | — | Quarentena, mortalidade, registro de mortos | — |
| F03 | SENAR 206, *Manejo da produção de peixes em viveiros* | — | Fases, capacidade (kg/1.000 m²), biometria, exemplos de povoamento | O texto extraído troca as colunas da Tabela 1 |
| F04 | Embrapa Pesca e Aquicultura, folheto *Qualidade da água* (Piscicultura Familiar) | — | Faixas simples e quadro "problema → ação" | PDF é imagem (lido por OCR) |
| F05 | Embrapa, Comunicado Técnico 44, *BPMs para reduzir o acúmulo de amônia* | 2007 | Contexto de amônia e ração | Cita limite do CONAMA, que vale para corpos d'água |
| F06 | Embrapa Amazônia Ocidental, Instruções Técnicas 5 (IT599) | 1999 | Só para confirmar outros valores | Antigo; foco em espécies amazônicas; PDF é imagem |
| F07 | Alfakit, *Manual de qualidade da água para aquicultura* | — | Tabela de Secchi, calagem, % de NH₃ | Material comercial de fabricante de kits |
| F08 | Cyrino et al., *O uso de alimentos ambientalmente corretos em piscicultura* (R. Bras. Zootec.) | 2010 | Contexto de nutrição e efluentes. **Não usado no MVP** | Artigo científico, sem tabelas de manejo |
| F09 | Embrapa Amazônia Oriental, *Parâmetros ambientais e qualidade da água* | 2000 | Só para confirmar | Antigo |
| F10 | Embrapa Agropecuária Oeste, cartaz *Qualidade da água* | 2016 | Quadro de sinais e ações | PDF é imagem; usa limites mais exigentes (O₂ > 5, pH 6,5 a 7,5) |
| F11 | Embrapa Amazônia Oriental, cartilha *Qualidade da água na piscicultura* (TC3217) | 2018 | Tabelas de efeito (O₂, pH, NH₃, NO₂) e previsão de oxigênio | — |
| F12 | SENAR 195, *Piscicultura: fundamentos da produção de peixes* | 2017 | Tipos de estrutura (viveiro, açude, tanque-rede) | — |
| F13 | SENAR 262, *Piscicultura: manejo da água* | — | Limites para tilápia, tabela de NH₃ tóxica, aeração, calagem | — |
| F14 | SENAR 263, *Piscicultura: alimentação* | — | Tabela de arraçoamento da tilápia, ajuste por temperatura, TCA | Tem 3 erros de conta (seção 4) |
| F15 | Rocha, A. F. F., relatório de estágio (UNESP Jaboticabal) | — | Rotina real de uma fazenda de tilápia GIFT | Estudo de caso, não é norma |

## 2. Qual fonte vence quando elas discordam

1. Fonte **específica da espécie** (tilápia) e de **manejo** (F13, F14, F03, F02).
2. Fonte **mais recente** (F01, F11).
3. Fontes antigas ou comerciais (F06, F07, F09, F10) só **confirmam**; não decidem.
4. Se ainda houver dúvida, escolher o lado **mais seguro para o peixe** e marcar 🔶.

## 3. Conflitos encontrados e decisão tomada

| # | Parâmetro | O que as fontes dizem | Decisão no spec |
|---|---|---|---|
| 1 | Oxigênio | ≥ 4 (F01 p.59, F11 p.13); > 5 para crescimento ótimo (F09 p.2, F10, F06 p.3); crítico 3 (F13 p.27, F14 p.25); risco de morte < 2 (F13 p.27) | 4 níveis: ≥ 4 · 3 a 4 · 2 a 3 · < 2. O "> 5" fica como constante informativa |
| 2 | pH | 6,5 a 8 (F01); 6,5 a 9 (F11); 6,5 a 7,5 (F09, F10); 6 a 8,5 tilápia (F15 p.10) | ADEQUADO 6,5 a 8,5 🔶 (mantém o valor do prompt; é a interseção de F11 e F15). Entre 8,5 e 9 vira ATENCAO porque a fração tóxica da amônia vai de 14 a 22% em pH 8,5 para 34 a 48% em pH 9 (F13 p.37) |
| 3 | Temperatura | 26 a 32 (F01, F04); 25 a 32 (F11, F09); 27 a 32 tilápia (F15); 25 a 30 (F10) | 26 a 32 (mantém o prompt) |
| 4 | **Amônia** | Tóxica < 0,05 (F11); < 0,10 (F01, F04, F10); limite de alimentação 0,2 e mortalidade 1,0 para tilápia (F13 p.38); risco de morte > 0,5 (F11 p.26); 0,01 já afeta crescimento (F09 p.4); "N-amoniacal máx. 0,5" (F07 p.12); CONAMA 0,02 (F05, água de rio) | Guardar **amônia total**, calcular a **tóxica**. ADEQUADO < 0,05 · ATENCAO < 0,2 · CRITICO < 0,5 · EMERGENCIA ≥ 0,5 🔶 |
| 5 | Nitrito | < 0,03 (F01, F04, F10, F11); alimentação 0,3 e mortalidade 0,7 (F13 p.41) | Adotado como está |
| 6 | Transparência | 35 a 40 (F01, F04); 30 a 40 (F10); 30 a 45 (F07 p.6) | ADEQUADO 30 a 45 🔶 (união das fontes, para não gerar alarme falso) |
| 7 | Alcalinidade | ≥ 20 (F01, F04, F09, F10); ideal 60 a 150 (F11 p.13); 20 a 300 (F07) | Mínimo 20; 60 a 150 só informativo |
| 8 | % de ração no fim da fase | 5%, 3% e 1% (F03 p.17) × tilápia 250 a 400 g = 2,2% (F14 p.34) | Usar F14 (específica de tilápia) |
| 9 | Capacidade do viveiro | Faixa de kg/1.000 m² (F03 p.15); exemplos do manual usam o **limite superior** | Mostrar a faixa e usar o superior como padrão 🔶 (para ser conservador, trocar para o inferior) |
| 10 | Frequência de biometria | Quinzenal na recria, mensal na engorda (F01 p.77); ~30 e ~80 dias (F15 p.15); alevino a cada 3 a 4 dias para ajustar ração (F14 p.32) | Constantes editáveis por fase |
| 11 | Tipo de estrutura | Prompt: REDONDO, ESCAVADO, ALVENARIA. Fontes: viveiro escavado, açude, tanque-rede | Mantidos os 3 e acrescentados ACUDE e TANQUE_REDE 🔶 |
| 12 | Faixa de água por fase | **Nenhuma fonte traz faixas diferentes por fase** | A fase muda a ração e a frequência de medição, não as faixas |
| 13 | Trava de ração | `03` §4.2 lista O₂ < 3, temperatura fora de 16 a 32 °C, NH₃ tóxica ≥ 0,2 e NO₂ ≥ 0,3 (F13 p.27, p.38, p.41; F14 p.38). `03` §1 diz que CRITICO = "não alimentar" para qualquer parâmetro | Só os 4 parâmetros da §4.2 zeram a ração sugerida. pH, transparência e alcalinidade em CRITICO mostram aviso forte, sem zerar (decisão do responsável, 16/10/2026) |

## 4. Erros encontrados nas próprias fontes (não copiar para o código)

| Onde | O que está escrito | O correto |
|---|---|---|
| F14 p.38 | "8 kg × 80% = 64 kg de ração" | **6,4 kg** (confirmado na imagem da página) |
| F14 p.40 | 5.000 × 0,259 kg = "1.296 kg" | **1.295 kg**. Os resultados seguintes do exemplo ficam ~0,07% diferentes |
| F14 p.41 | "316,40 g por dia" (ração ajustada) | É **31,64 kg** por dia (com a conta do manual) |
| Extração de texto de F03 p.15 | Tabela de capacidade com colunas trocadas | Valores certos estão em `03-regras-negocio.md`, seção 4.5 |

> **Regra do projeto:** tabela copiada de PDF só entra no spec depois de conferida **olhando a página**. Texto extraído pode trocar colunas.

## 5. Decisões que dependem de você (🔶)

1. **Amônia:** concordar que o app guarda a amônia total e calcula a tóxica (o seu 0,5 passa a ser o limite de EMERGENCIA, não o de "ideal").
2. **Tanque:** aceitar capacidade por **área** (m²) para viveiro e açude.
3. **Capacidade do viveiro:** usar o limite superior da faixa (como o manual) ou o inferior (mais seguro)?
4. **Transferência entre tanques:** criar lote novo no destino automaticamente? (pode esperar a fase de telas)
5. **Outras espécies:** o MVP calcula ração só para tilápia; tambaqui e pirarucu entram depois.
