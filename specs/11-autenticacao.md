# Autenticação (login padrão com e-mail e senha)

Telas: `10-telas-e-navegacao.md`. Perfis: `06-perfis-permissoes.md`. Requisitos de UX: `04-requisitos-ux.md`.

## Princípio: o app continua 100% offline

- **Cadastro, "Esqueci a senha" e o primeiro login precisam de internet** (dependem do servidor).
- Depois do primeiro login, a **sessão fica salva no aparelho**. Abrir o app, registrar ração, água e biometria **não exige internet nem novo login**.
- Sem internet nessas telas, mostrar: "Sem conexão. Conecte-se à internet para entrar pela primeira vez." (nunca um erro técnico).

## Telas (padrão)

### Login
- Campos: **E-mail** e **Senha** (com botão mostrar/ocultar).
- Botão principal grande: **Entrar**.
- Links: **Esqueci minha senha** e **Criar conta**.
- Mostrar carregando no botão enquanto valida; bloquear toque duplo.
- Erro de credencial sempre genérico: "E-mail ou senha incorretos." (não revelar qual dos dois está errado).

### Cadastro (Criar conta)
- Campos: **Nome**, **E-mail**, **Senha**, **Confirmar senha**.
- Regras da senha: mínimo de 8 caracteres, com letra e número. Mostrar a regra **antes** de digitar, e o que falta enquanto digita.
- Quem se cadastra vira **ADMIN** da sua fazenda (🔶). Funcionários são convidados pelo ADMIN (ver `06`).
- Após cadastrar: ir para a configuração inicial guiada (`04`, "Primeiro acesso").
- Verificação de e-mail: enviar o link de confirmação, mas **não bloquear** o uso (decisão 🔶, ver abaixo).

### Esqueci minha senha
- Campo: **E-mail**. Botão: **Enviar link**.
- Resposta sempre igual, exista o e-mail ou não: "Se esse e-mail estiver cadastrado, enviamos um link para criar uma nova senha." (evita descobrir quem tem conta).
- O link de redefinição abre a página do provedor de autenticação (não é tela do app no MVP).

### Sair (logout)
- Em Configurações. Confirmar: "Sair da conta? Registros não enviados continuam guardados no aparelho."
- Se houver registros com `sincronizado = false`, **avisar a quantidade** antes de sair.

## Regras de segurança

- Nunca gravar a senha no aparelho (nem no Room). Guardar só o token da sessão, em armazenamento criptografado (EncryptedSharedPreferences ou Keystore).
- A senha nunca aparece em log nem em `UiState` depois de enviada.
- Campo senha: `autofillHints` (e-mail e senha) para o gerenciador de senhas funcionar; teclado de e-mail no campo de e-mail.
- Limite de tentativas: tratado pelo servidor; o app mostra "Muitas tentativas. Tente de novo em alguns minutos."
- Sessão expirada com internet: pedir login de novo **sem apagar** os dados locais.

## Arquitetura

- `AuthRepository` (interface no `domain`): `signIn`, `signUp`, `sendPasswordReset`, `signOut`, `currentSession: Flow<Session?>`.
- Implementação real depende do backend (🔶, ver abaixo). Para construir as telas **já agora**, criar `FakeAuthRepository` (aceita `teste@teste.com` / `Teste1234`) escolhido por flag de build.
- `Session(userId, nome, email, papel)`. Tela inicial decide o destino a partir de `currentSession`.
- Validação de campos no ViewModel, exposta no `UiState` (mesmo padrão da skill `safe-form`):
  `emailError`, `senhaError`, `confirmarSenhaError`, `isLoading`, `mensagem`.

## Mensagens (todas em `strings.xml`)

| Situação | Texto |
|---|---|
| E-mail inválido | "Digite um e-mail válido, como nome@exemplo.com." |
| Senha curta | "A senha precisa de pelo menos 8 caracteres, com letra e número." |
| Senhas diferentes | "As senhas não são iguais." |
| Credencial errada | "E-mail ou senha incorretos." |
| E-mail já cadastrado | "Já existe uma conta com esse e-mail. Tente entrar." |
| Sem internet | "Sem conexão. Conecte-se à internet para continuar." |
| Link enviado | "Se esse e-mail estiver cadastrado, enviamos um link para criar uma nova senha." |
| Conta criada | "Conta criada! Vamos configurar sua fazenda." |

## Decisões que dependem de você (🔶)

1. **Backend de autenticação:** **Supabase** (decidido na Fase 09): login e sincronização no mesmo projeto. Sem `supabase.url` e `supabase.anonKey` em `local.properties`, o app usa o `FakeAuthRepository`. Passo a passo em `backend/supabase/README.md`.
2. **Quem pode se cadastrar:** **qualquer pessoa** vira ADMIN de uma fazenda nova (decidido); funcionários entram por convite do ADMIN.
3. **Verificação de e-mail:** **só recomendada** (decidido). O Supabase não envia o link sem bloquear a conta, então "Confirm email" fica desligado no projeto; ligar a opção torna a verificação obrigatória.
4. **Login social (Google):** fora do MVP. Entra depois, se quiser.
