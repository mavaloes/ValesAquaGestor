# Fase 04: Login, cadastro, recuperação de senha e navegação base

Leia antes: `10-telas-e-navegacao.md`, `11-autenticacao.md`, `04-requisitos-ux.md`, `06-perfis-permissoes.md`.
Pré-requisito: Fases 01 a 03 prontas (entities, regras, DAOs e Hilt).

- [x] Etapa 1: tema Material 3 e componentes base (botão grande com ícone e texto, campo de texto com erro, campo de senha com mostrar/ocultar, Snackbar de feedback).
- [x] Etapa 2: `AuthRepository` (interface), `Session`, `FakeAuthRepository` e módulo Hilt que escolhe a implementação por flag de build.
- [x] Etapa 3: telas `Login`, `SignUp` e `ForgotPassword`, cada uma com ViewModel, `UiState` e validação (mensagens de `11-autenticacao.md`).
- [x] Etapa 4: navegação type-safe com tela inicial decidida por `currentSession`; `Home` provisória vazia com botão "Sair".
- [x] Etapa 5: sessão salva em armazenamento criptografado; abrir o app com sessão vai direto à `Home`, **sem internet**.
- [x] Etapa 6: testes: validação de e-mail e senha, fluxo do `FakeAuthRepository`, decisão da tela inicial.

## Fora do escopo agora

Backend real, login social, Onboarding, telas de tanque/lote, gerenciamento de usuários, convites.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; todos os textos em `strings.xml`; `@Preview` de cada tela com `fontScale = 2f`; nenhuma senha em log, banco ou `UiState` após o envio.
