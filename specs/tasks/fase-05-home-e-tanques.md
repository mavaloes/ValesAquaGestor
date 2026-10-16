# Fase 05: Home (rotina diária), Tanques e Lotes

Leia antes: `10-telas-e-navegacao.md`, `04-requisitos-ux.md`, `03-regras-negocio.md` (seções 1, 2 e 6). Pré-requisito: Fases 01 a 04.
Skills: `compose-accessible-screen`, `water-quality-rules`, `fish-lot-records`.

- [ ] Etapa 1: componente `StatusCard` (valor + o que significa + o que fazer + cor do nível). Textos em `strings.xml`.
- [ ] Etapa 2: tela `Tanques`: lista com nome, tipo, lote ativo e pior nível de alerta do tanque. Botão grande "Novo tanque" (formulário com a skill `safe-form`: área em m², profundidade, aeração, renovação de água).
- [ ] Etapa 3: `TankDetail`: últimas medições com `StatusCard`, lotes do tanque, atalhos para registrar água e ração.
- [ ] Etapa 4: `LotDetail`: peixes vivos, biomassa, peso médio, mortalidade acumulada, TCA (quando houver biometria), alertas do lote. Formulário de novo lote mostra a capacidade do viveiro e avisa se passar dela.
- [ ] Etapa 5: `Home` = rotina diária: lista do que falta fazer hoje (medições da agenda de `03`, seção 6; tratos previstos; biometria vencida) e alertas por prioridade (EMERGENCIA primeiro).
- [ ] Etapa 6: menu inferior com no máximo 4 itens (`Home`, `Tanques`, `Registrar`, `Mais`).
- [ ] Etapa 7: testes dos ViewModels (ordem dos alertas, pendências do dia).

## Fora do escopo agora

Telas de registro (Fase 06), onboarding, perfis, sincronização.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; `@Preview` com `fontScale = 2f`; todo ícone com `contentDescription`.
