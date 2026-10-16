# Glossário

Termos do campo e como aparecem no projeto (domínio em português, código em inglês).

| Termo | Significa | No código |
|---|---|---|
| Viveiro | Tanque de terra escavado, o mais comum no Brasil (F12 p.10) | `Tanque` com tipo `ESCAVADO` |
| Açude | Represa feita com aterro; formato depende do terreno (F12 p.11) | tipo `ACUDE` |
| Tanque-rede | Gaiola flutuante em represa ou lago (F12 p.14) | tipo `TANQUE_REDE` |
| Alevinagem, recria, engorda | As 3 fases do cultivo. Na fazenda real: berçário, recria, terminação (F15 p.14) | `FaseLote` |
| Povoamento | Colocar os alevinos no viveiro | `Lote.dataPovoamento` |
| Biometria | Pesar uma amostra de peixes para saber o peso médio | `Biometria` |
| Biomassa / peso vivo (PV) | Peso total dos peixes do viveiro, em kg | `biomassaKg` |
| Arraçoamento | Dar ração; a quantidade é uma % do peso vivo | `Arracoamento` |
| PB | Proteína bruta da ração, em % | `proteinaBrutaPct` |
| TCA | Taxa de conversão alimentar: kg de ração por kg de peixe ganho | `tcaReal` |
| GPD | Ganho de peso diário, em g/dia | `dailyGainG` |
| OD | Oxigênio dissolvido, em mg/L | `oxigenioMgL` |
| Secchi | Disco preto e branco que mede a transparência da água, em cm | `transparenciaCm` |
| Amônia total | O que o kit mede (NH₃ + NH₄⁺) | `amoniaTotalMgL` |
| Amônia tóxica | A parte NH₃, que depende do pH e da temperatura. É a que mata | calculada |
| Alcalinidade | Capacidade da água de segurar o pH, em mg/L de CaCO₃ | `alcalinidadeMgL` |
| Calagem | Aplicar calcário para subir alcalinidade e pH | fase futura |
| Despesca | Retirar peixes do viveiro (venda, classificação ou ajuste de densidade) | `MovimentacaoLote` |
| Quarentena | 45 a 90 dias de observação de peixe novo, em viveiro separado (F02 p.35) | fase futura |

## Convenção de nomes

- Entidades e colunas em português: `Tanque`, `quantidadeKg`, `oxigenioMgL`.
- Classes, funções e variáveis de lógica em inglês: `WaterThresholds`, `FeedingCalculator`.
- Toda unidade vai **no nome** da coluna: `...Kg`, `...G`, `...MgL`, `...Cm`, `...C`, `...M2`.
