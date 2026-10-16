# Fase 01: Fundação (somente estas 4 etapas)

Leia antes: `specs/00-glossario.md`, `specs/02-dominio.md`, `specs/05-offline-sync.md`.

- [x] Etapa 1: árvore de pacotes + 1 frase por pacote. Já criar os pacotes vazios `domain/rules` e `domain/calc` (usados na Fase 02).
- [x] Etapa 2: `libs.versions.toml` + `build.gradle.kts` (app), informando as versões. Incluir JUnit e Turbine para testes de domínio.
- [x] Etapa 3: Entities `Tanque`, `Lote`, `QualidadeAgua`, `Arracoamento`, `Biometria`, `MovimentacaoLote`; os enums de `02-dominio.md`; TypeConverters; classes `@Relation`. Todas com os 4 campos de sincronização.
- [x] Etapa 4: resumo de 5 linhas com o próximo passo (Fase 02).

## Fora do escopo agora

ViewModels, telas, DAOs, AppDatabase, navegação, **e qualquer cálculo** (os cálculos ficam na Fase 02).

## Critério de pronto

`./gradlew :app:assembleDebug` compila; Room com KSP; cada FK com índice e `onDelete = RESTRICT`.
