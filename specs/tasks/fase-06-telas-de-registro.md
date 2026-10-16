# Fase 06: Telas de registro (água, ração, biometria, mortalidade e despesca)

Leia antes: `04-requisitos-ux.md`, `03-regras-negocio.md`, `09-casos-de-teste.md`. Pré-requisito: Fases 01 a 05.
Skills: `safe-form`, `fish-lot-records`, `feeding-calculation`, `water-quality-rules`, `compose-accessible-screen`.

- [x] Etapa 1: `WaterRecord`: campos da Planilha 2 e 3 (transparência, temperatura, oxigênio, pH, amônia total, nitrito, alcalinidade, dureza). Data e hora automáticas e editáveis; `momento` sugerido pelo horário. Mostrar a amônia tóxica calculada e o nível de cada valor ao salvar.
- [x] Etapa 2: `FeedingRecord`: quantidade sugerida pela `FeedingCalculator` (editável), horário atual, marca/proteína/mm da última ração, comportamento dos peixes, sobra, opção "ração suspensa" com motivo. Trava (sugere 0 kg, com aviso) quando O₂, temperatura, amônia tóxica ou nitrito estiverem em CRITICO ou EMERGENCIA; os outros parâmetros em CRITICO só mostram aviso (`08`, conflito 13). **Máximo de 3 toques** a partir do `LotDetail`.
- [x] Etapa 3: `Biometry`: várias pesagens (peso com peixes, tara, nº de peixes), cálculo do peso médio, aviso se a amostra for menor que a recomendada, e sugestão do novo valor de ração depois de salvar.
- [x] Etapa 4: `MortalityRecord` e despesca (parcial ou final) em fluxo rápido. Alerta de mortalidade acumulada acima de 10%. Transferência fica sem tela (`08`, conflito 16).
- [x] Etapa 5: validação e prevenção de erros em todos os formulários (unidade visível, obrigatórios marcados, aviso de valor improvável com "Salvar mesmo assim", confirmação ao excluir).
- [x] Etapa 6: feedback ao salvar: "Registro salvo" ou "Sem conexão: dados guardados no aparelho".
- [x] Etapa 7: testes de ViewModel para cada formulário (validações, travas, cálculo exibido).

## Fora do escopo agora

Onboarding, perfis, sincronização real, QR Code, sensores.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; fluxo "abrir o lote, informar a quantidade e salvar" em até 3 toques; `@Preview` com `fontScale = 2f`.
