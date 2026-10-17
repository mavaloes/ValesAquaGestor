---
name: auth-screens
description: Use ao criar ou alterar as telas de login, cadastro, recuperação de senha, logout, a sessão do usuário ou a decisão da tela inicial.
---

# Telas de autenticação

## Antes de escrever código

1. Leia `specs/11-autenticacao.md` e `specs/10-telas-e-navegacao.md`.
2. O app é offline. Login real só é necessário na **primeira vez**.

## Passo a passo

1. Crie primeiro a interface `AuthRepository` no `domain` e o `FakeAuthRepository`. Só troque por backend real quando a decisão 🔶 estiver feita.
2. Cada tela tem seu `ViewModel` e `UiState` (`isLoading`, erros por campo, `mensagem`). Validar no ViewModel, não na tela.
3. E-mail: formato válido. Senha: 8 caracteres ou mais, com letra e número. Mostrar a regra antes de digitar.
4. Botão **Entrar** desabilitado enquanto carrega (evita toque duplo) e com indicador de progresso.
5. Mensagens de erro de credencial e de "esqueci a senha" são **genéricas** (não revelam se o e-mail existe).
6. Senha: nunca em log, Room ou `UiState` depois de enviada. Sessão em armazenamento criptografado.
7. Campos com `autofillHints` e `KeyboardType` corretos (e-mail, senha); ação de teclado "Próximo" e "Entrar".
8. Tela inicial = função de `currentSession` (sem sessão: `Login`; com sessão: `Home`). Logout limpa a pilha de navegação.
9. Sem internet em cadastro, recuperação ou primeiro login: mensagem clara, sem travar.
10. Siga também `compose-accessible-screen` e `safe-form`; `@Preview` com `fontScale = 2f`.

## Armadilhas

- Não bloquear o app inteiro quando a sessão "expira" sem internet: só pedir novo login quando houver conexão.
- Não apagar dados locais no logout sem avisar sobre registros pendentes de sincronização.
- Não implementar perfis (ADMIN e FUNCIONARIO) além do que a tarefa pede; as permissões vêm na fase própria.
