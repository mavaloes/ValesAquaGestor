# Fase 02: Regras de domínio (Kotlin puro + testes)

Sem Android, sem Room, sem telas. Só lógica e testes. Leia antes: `03-regras-negocio.md` e `09-casos-de-teste.md`.

- [ ] Etapa 1: `WaterThresholds` (constantes com a fonte no comentário) e `WaterEvaluator` que devolve `WaterStatus(level, messageKey, actionKey)`.
- [ ] Etapa 2: `AmmoniaCalculator` (amônia total + pH + temperatura → tóxica). Usar a skill `water-quality-rules`.
- [ ] Etapa 3: `FeedingCalculator` (tabela da tilápia, fator de temperatura, travas, reajuste semanal, TCA). Usar a skill `feeding-calculation`.
- [ ] Etapa 4: `CapacityCalculator`, `OxygenForecaster` (métodos 1 e 2), `PondLoadChecker` (kg de ração/ha/dia), `MonitoringSchedule`.
- [ ] Etapa 5: testes JUnit com **todos** os casos de `09-casos-de-teste.md` (seções A a F).

## Critério de pronto

`./gradlew :app:testDebugUnitTest` passa com os casos A a F. Nenhuma constante numérica sem `@see F## p.##`.

## Depois desta fase

Fase 03: DAOs, `AppDatabase` e módulo Hilt (era a "Fase 2" do plano anterior). As fases de telas vêm depois e usam estas regras.
