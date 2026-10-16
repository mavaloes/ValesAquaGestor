# LEIA-ME: plano de execução do Valões AquaGestor

Este é o roteiro geral do projeto: ordem das fases, dependências, decisões pendentes (🔶) e como verificar cada entrega.
Cada fase é executada **só** com o escopo do próprio arquivo em `specs/tasks/` (regra do `CLAUDE.md`). Se este arquivo e uma tarefa discordarem, vale a tarefa.

## Marcos

| Marco | Fases | O que entrega | Bloqueio |
|---|---|---|---|
| M1: núcleo offline | 01, 02, 03 | Entities, regras de domínio testadas, Room + Hilt + repositórios. Ainda sem tela | Série do caso E4 (ver `09`) antes da Fase 02, Etapa 5 |
| M2: MVP de campo | 04, 05, 06 | Login (fake), tanques, lotes, rotina diária e todos os registros. **Primeiro APK testável à beira do tanque** | Pontos abertos da Fase 06 |
| M3: uso real | 07, 08 | Primeiro acesso guiado, ajuda, perfis ADMIN/FUNCIONARIO, configurações | — |
| M4: conectado | 09, 10 | Sincronização com servidor, QR Code, interface de sensor | **Escolher o backend** (🔶 `11` #1) |

Ordem obrigatória: 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09. A Fase 10 só depende da 06 e pode vir antes ou depois da 09.

## Etapa 0: preparação (feita em 16/10/2026)

- [x] Este arquivo criado (o `10` o cita como "plano atualizado").
- [x] `09`: tolerância da seção C escrita; C4 marcado como fora da Fase 02; E4 marcado como "aguardando dados".
- [x] `02`: índice único em `uuid` em toda entidade.
- [x] `08`, conflito 13: só O₂, temperatura, NH₃ tóxica e NO₂ zeram a ração sugerida; texto da Fase 06, Etapa 2 ajustado.
- Git: um branch por fase (`fase-01-fundacao`, `fase-02-regras-dominio`, …). Commit no fim da fase só com autorização do responsável.

## Estrutura de pacotes

Pacote base: `com.valoes.valesaquagestor`. A dependência vai sempre de fora para dentro: `presentation` → `domain` ← `data`. O `domain` não conhece Android.

```
com.valoes.valesaquagestor
├── data                    Acesso a dados: banco local agora, servidor depois.
│   ├── local
│   │   ├── entity          Entities do Room (tabelas e colunas em português).
│   │   ├── converter       TypeConverters do Room (enums gravados como String).
│   │   ├── relation        Classes @Relation (TanqueComLotes, LoteComArracoamentos, LoteCompleto).
│   │   ├── dao             (Fase 03) DAOs com consultas em Flow, sempre filtrando excluido = 0.
│   │   └── database        (Fase 03) AppDatabase.
│   └── repository          (Fase 03) Implementações dos repositórios do domínio.
├── domain                  Kotlin puro: regras do negócio, sem Android.
│   ├── model               Enums e modelos do domínio (TipoTanque, FaseLote, ...).
│   ├── rules               (Fase 02) Faixas de água e avaliação dos níveis de alerta.
│   ├── calc                (Fase 02) Cálculos: amônia tóxica, ração, capacidade, oxigênio.
│   ├── repository          (Fase 03) Interfaces dos repositórios.
│   └── usecase             (Fase 04 em diante) Casos de uso; a permissão é checada aqui.
├── presentation            Telas Compose, ViewModels e UiState.
│   ├── theme               Tema Material 3 (cores e tipografia).
│   └── components          (Fase 04) Componentes reutilizáveis (botão grande, StatusCard, ...).
└── di                      (Fase 03) Módulos Hilt.
```

## Fases

### Fase 01: Fundação
- Pacotes acima; Gradle com Version Catalog (AGP 9 já traz o Kotlin embutido: não aplicar `org.jetbrains.kotlin.android`); `minSdk 26`.
- Entities com os 4 campos de sincronização, índice único em `uuid`, toda FK com `@Index` e `onDelete = RESTRICT`; enums via TypeConverter; classes `@Relation`.
- Risco para a Fase 03: `@Relation` traz também os filhos com `excluido = 1`. Ou se filtra no repositório, ou se usa `@DatabaseView`.

### Fase 02: Regras de domínio (Kotlin puro)
- `domain/rules`: `WaterThresholds` (cada `const` com `@see F## p.##`), `AlertLevel`, `WaterStatus`, `WaterEvaluator`; regras compostas em funções separadas.
- `domain/calc`: `AmmoniaCalculator` (fórmula do pKa), `FeedingCalculator`, `CapacityCalculator` (devolve a faixa; padrão = limite superior 🔶), `OxygenForecaster`, `PondLoadChecker`, `MonitoringSchedule`.
- Testes primeiro, com os casos A a F de `09` (menos o C4; o E4 quando a série chegar).
- Trava de ração: só os 4 parâmetros do conflito 13 de `08`, lendo os limites do `WaterThresholds`.
- Limites com vírgula flutuante: calcular a carga como `kg × 10.000 / áreaM2` (F1 dá exatamente 40,0).

### Fase 03: DAOs, AppDatabase e Hilt
- DAOs em `Flow` filtrando `excluido = 0`, mais a consulta de pendentes (`sincronizado = 0`).
- `AppDatabase` v1 com `exportSchema = true` (schema em `app/schemas/`, versionado no git).
- `@HiltAndroidApp`, `DatabaseModule`, repositórios com upsert por `uuid` (vence o maior `atualizadoEm`).
- Testes de DAO com Room em memória e **Robolectric** (rodam no `testDebugUnitTest`, sem emulador).

### Fase 04: Login e navegação base
- Tema renomeado para `AquaGestorTheme` (sem "õ" no identificador).
- `FakeAuthRepository` escolhido por `BuildConfig`.
- Sessão: o `EncryptedSharedPreferences` foi descontinuado; usar DataStore + chave AES-GCM do Android Keystore atrás de uma interface `SessionStore` (falsa nos testes).
- `ConnectivityObserver` para as mensagens de "Sem conexão"; tela inicial decidida por função pura.

### Fase 05: Home, Tanques e Lotes
- Rotina do dia num `DailyRoutineBuilder` em Kotlin puro (usa `MonitoringSchedule`).
- Item "Registrar" do menu com destino provisório até a Fase 06.

### Fase 06: Telas de registro
- Perguntar antes de codar (os specs não trazem): horários do `momento` sugerido; limites de "valor improvável" além de pH > 10 e peso 10× maior; transferência cria lote no destino? (🔶 `08` §5.4).

### Fase 07: Primeiro acesso e ajuda
- "Onboarding concluído" no DataStore; `HelpTip` (o "?") e tela `Help`.

### Fase 08: Perfis e configurações
- `Usuario` e `criadoPorUserId` levam o banco à versão 2: exige `Migration(1, 2)` com teste (`MigrationTestHelper`).
- `PermissionChecker` nos casos de uso; FUNCIONARIO barrado mesmo chamando o caso de uso direto.
- Faixas editáveis pelo ADMIN: os padrões continuam sendo as constantes com fonte; os ajustes ficam guardados à parte.

### Fase 09: Sincronização (bloqueada até escolher o backend)
- Decisões 🔶 do `11`: backend, quem pode se cadastrar, verificação de e-mail.

### Fase 10: QR Code e sensores
- Avaliar ZXing ou CameraX + ML Kit na hora da fase; protocolo do sensor continua 🔶.

## Decisões 🔶 e quando cada uma é necessária

| Decisão | Onde está | Situação | Precisa estar decidida antes da |
|---|---|---|---|
| Guardar a amônia total e calcular a tóxica | `08` §5.1 | Assumido no spec | Fase 01 |
| Capacidade por área para viveiro e açude | `08` §5.2 | Assumido no spec | Fase 01 |
| Limite superior ou inferior da capacidade | `08` §5.3 | Superior (o código devolve a faixa) | Fase 02 |
| Quais níveis travam a ração | `08` conflito 13 | **Decidido** em 16/10/2026 | — |
| Transferência cria lote no destino? | `08` §5.4 | Só registra a baixa | Fase 06 |
| Backend, cadastro aberto, verificação de e-mail | `11` #1 a #3 | Fake até decidir | Fase 09 |
| Protocolo do sensor | `07`, Fase 10 | Só a interface | Depois da Fase 10 |

## Verificação (em toda fase)

1. `./gradlew :app:assembleDebug` e `./gradlew :app:testDebugUnitTest` passam (e `./gradlew :app:kspDebugKotlin`).
2. Buscas de conferência: `const val` de domínio sem `@see`; texto fixo em Composable; `contentDescription = null`; `@Preview` sem `fontScale = 2f`.
3. A partir do M2: APK no emulador ou aparelho, testado em **modo avião**.
4. Itens marcados em `specs/tasks/fase-XX-*.md` e resumo final com o próximo passo.
