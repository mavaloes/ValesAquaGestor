# Telas e navegação

Estas são as telas conhecidas até agora. Cada fase de telas só constrói as que estiverem na sua tarefa.

## Fluxo de entrada (tela inicial decidida pela sessão)

```
Abrir o app
  ├─ sem sessão ........................ Login
  │      ├─ "Criar conta" ............. Cadastro → Primeiro acesso → Home
  │      └─ "Esqueci minha senha" ..... Recuperar senha → volta ao Login
  └─ com sessão
         ├─ primeiro acesso não concluído → Primeiro acesso (pode pular)
         └─ concluído ................. Home (Rotina diária)
```

- Com sessão salva, **o app abre direto na Home, mesmo sem internet**.
- Voltar (botão do sistema) na Home sai do app; nunca volta para o Login.
- Ao fazer logout, limpar a pilha de navegação.

## Rotas type-safe (`@Serializable`)

Grupo de autenticação (fora do menu): `Login`, `SignUp`, `ForgotPassword`.
Primeiro acesso: `Onboarding`, `SetupFarm` (criar o 1º tanque e o 1º lote).
Menu principal (máximo 4 itens, ver `04`): `Home` (Rotina), `Tanques`, `Registrar`, `Mais`.
Dentro deles: `TankDetail(id)`, `LotDetail(id)`, `WaterRecord(tankId)`, `FeedingRecord(lotId)`, `Biometry(lotId)`, `MortalityRecord(lotId)`, `Settings`, `Users` (só ADMIN), `Help`.

## Lista de telas por fase

| Fase | Telas |
|---|---|
| 04 | `Login`, `SignUp`, `ForgotPassword`, tela de destino provisória "Home" vazia, navegação base e tema |
| 05 | `Home` (rotina diária), `Tanques`, `TankDetail`, `LotDetail` |
| 06 | `WaterRecord`, `FeedingRecord`, `Biometry`, `MortalityRecord` |
| 07 | `Onboarding`, `SetupFarm`, `Help` |
| 08 | `Settings`, `Users` (ADMIN), logout com aviso de pendências |

(A numeração das fases mais antigas muda; o plano atualizado está no `LEIA-ME.md`.)

## Regras de todas as telas

Seguem `04-requisitos-ux.md` e as skills `compose-accessible-screen` e `safe-form`: botões de 48 dp ou mais, ícone com texto, textos em `sp` e `strings.xml`, feedback depois de toda ação, `@Preview` com `fontScale = 2f`.
