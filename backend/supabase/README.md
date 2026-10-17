# Servidor (Supabase)

O app grava tudo no aparelho primeiro (Room) e sincroniza em segundo plano com um projeto Supabase.
Sem servidor configurado, o app continua funcionando com o login de teste (`teste@teste.com` / `Teste1234`)
e sem sincronizar.

## Preparar o projeto

1. Crie um projeto em <https://supabase.com>.
2. Abra **SQL Editor**, cole o conteúdo de [`schema.sql`](schema.sql) e execute (uma vez). Ele cria as tabelas,
   as regras de acesso por fazenda (RLS), o gatilho que resolve conflitos (vence o maior `atualizado_em`) e o gatilho
   que cria a fazenda de quem se cadastra.
3. Em **Authentication → Providers → Email**, deixe o login por e-mail e senha ligado e **desligue "Confirm email"**.
   - O spec pede verificação de e-mail *só recomendada* (não bloquear o uso). O Supabase não envia o link sem
     bloquear: com "Confirm email" ligado, a conta só entra depois de confirmar. Por isso a confirmação fica desligada,
     e o app funciona logo depois do cadastro. Se um dia a verificação passar a ser obrigatória, é só ligar a opção.
4. Em **Authentication → URL Configuration**, ajuste o *Site URL* (a página aberta pelo link de "Esqueci minha senha";
   no MVP o link abre a página do provedor, não uma tela do app).
5. Em **Project Settings → API**, copie a **Project URL** e a chave **anon public**.

## Ligar o app ao projeto

Acrescente ao `local.properties` (arquivo fora do git, na raiz do projeto):

```
supabase.url=https://SEU-PROJETO.supabase.co
supabase.anonKey=SUA-CHAVE-ANON-PUBLICA
```

Depois recompile (`./gradlew :app:assembleDebug`). Com os dois valores preenchidos, o app usa o login do Supabase
e sincroniza; sem eles, volta ao login de teste. A chave `anon` é pública por desenho: quem protege os dados são as
regras de acesso (RLS) do `schema.sql`. **Nunca** coloque a chave `service_role` no app.

## Como o acesso funciona

- Quem se cadastra cria a própria fazenda e vira **ADMIN** (cadastro aberto).
- O ADMIN convida funcionários por e-mail no app (tela *Usuários*). O convite vira uma linha em `usuario` com
  status `CONVIDADO`; quando a pessoa cria a conta **com o mesmo e-mail**, o gatilho `handle_new_user` a coloca na
  fazenda do convite, com o papel combinado.
- Cada usuário só vê e grava a própria fazenda. Só o ADMIN convida, troca papel e desativa (também valido no servidor).
- Usuário desativado perde o acesso na hora (as regras só contam usuário `ATIVO`).
- O mesmo e-mail não deve ser convidado em duas fazendas ao mesmo tempo (vale o primeiro convite encontrado).

## O que não é sincronizado

As faixas da água e a agenda de medição editadas pelo ADMIN ficam em cada aparelho (DataStore). Sincronizá-las é
uma melhoria futura.

## Ainda não testado contra um servidor de verdade

O app foi testado com um servidor simulado (testes de unidade) e com respostas HTTP simuladas. O `schema.sql`
e a conversa real com o Supabase só podem ser conferidos depois de criar o projeto: rode o app em dois aparelhos
(ou em um aparelho e no emulador) e confira o cadastro, o convite e a sincronização.
