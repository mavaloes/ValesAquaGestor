# Fase 03: DAOs, AppDatabase e Hilt

Leia antes: `02-dominio.md`, `05-offline-sync.md`. Pré-requisito: Fases 01 e 02.

- [ ] Etapa 1: um DAO por entity (`Tanque`, `Lote`, `QualidadeAgua`, `Arracoamento`, `Biometria`, `MovimentacaoLote`). Consultas com `Flow`, **sempre filtrando `excluido = 0`**. Consultas mínimas:
  - tanques ativos; lotes ativos de um tanque;
  - últimas medições de água de um tanque (por parâmetro) e do dia;
  - arraçoamentos do lote por período; últimas biometrias do lote;
  - movimentações do lote (para `quantidadeAtual` e mortalidade acumulada);
  - registros com `sincronizado = 0` (para a Fase 09).
- [ ] Etapa 2: `AppDatabase` (versão 1, `exportSchema = true`, TypeConverters, sem `fallbackToDestructiveMigration`).
- [ ] Etapa 3: módulo Hilt (`DatabaseModule`) que fornece o banco e os DAOs.
- [ ] Etapa 4: repositórios no `data` (`TankRepository`, `LotRepository`, `WaterQualityRepository`, `FeedingRepository`, `BiometryRepository`, `LotMovementRepository`) com interfaces no `domain` e mapeamento entity ↔ modelo de domínio. Ao salvar: preencher `uuid`, `atualizadoEm` e `sincronizado = false`; excluir = `excluido = true`.
- [ ] Etapa 5: ao salvar `Biometria`, atualizar `Lote.pesoMedioAtualG` na **mesma transação**. Ao salvar `DESPESCA_FINAL`, marcar o lote `ENCERRADO` na mesma transação.
- [ ] Etapa 6: testes de DAO e repositório com Room em memória (inclui: registro excluído não aparece; `quantidadeAtual` após mortes e despesca; salvar biometria atualiza o lote; casos G1 a G3 de `09-casos-de-teste.md`).

## Fora do escopo agora

ViewModels, telas, navegação, servidor.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` (e os testes de DAO) passam; schema do Room exportado.
