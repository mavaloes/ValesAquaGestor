# Fase 09: Sincronização com o servidor

Leia antes: `05-offline-sync.md`, `11-autenticacao.md`, `02-dominio.md`. Pré-requisito: Fases 01 a 08 e **backend escolhido** (decisão 🔶 de `11`).

- [x] Etapa 1: implementação real do `AuthRepository` no backend escolhido (troca o `FakeAuthRepository`; sessão e recuperação de senha de verdade).
- [x] Etapa 2: API/serviço de sincronização para todas as entities, usando `uuid` como identidade.
- [x] Etapa 3: `SyncWorker` (WorkManager) que envia registros com `sincronizado = false` e baixa mudanças quando a conexão volta. Com tentativa de novo e espera crescente.
- [x] Etapa 4: regras: deduplicar por `uuid`; conflito resolvido pelo maior `atualizadoEm`; exclusão lógica só some depois do servidor confirmar.
- [x] Etapa 5: indicador discreto de estado ("Tudo sincronizado", "3 registros aguardando conexão") e mensagem "Sem conexão: dados guardados no aparelho".
- [x] Etapa 6: testes: casos G1 a G3 de `09-casos-de-teste.md`, envio duplicado, queda de conexão no meio do envio, dois aparelhos editando o mesmo registro.

## Fora do escopo agora

QR Code e sensores (Fase 10), login social.

## Critério de pronto

Nenhum registro duplicado nem perdido nos testes; app continua totalmente utilizável sem conexão.

## Decisões desta fase

- **Backend:** Supabase (login + PostgREST), cadastro aberto (quem se cadastra vira ADMIN da própria fazenda), verificação de e-mail só recomendada. O SQL do servidor e o passo a passo estão em `backend/supabase/`. Sem `supabase.url`/`supabase.anonKey` em `local.properties`, o app usa o login de teste e não sincroniza (o indicador some).
- **Cliente:** OkHttp + `kotlinx.serialization` direto no HTTP (sem SDK do Supabase): menos dependências e testável com servidor HTTP simulado.
- **Regras no servidor:** upsert por `uuid`; um gatilho ignora versão com `atualizado_em` menor ou igual (vence o maior); `servidor_em` (relógio do servidor) pagina o download; acesso por fazenda via RLS; só o ADMIN mexe em usuários; nenhuma tabela concede `delete`.
- **Envio/download:** envia pais antes de filhos, em lotes de 100, e só marca como enviado o que o servidor confirmou (e só se o registro não mudou desde então). Baixa por tabela, retomando um minuto antes da última marca. Registro excluído e confirmado sai do banco; se outra tabela ainda aponta para ele, fica como excluído.
- **Quando roda:** `SyncCoordinator` pede uma rodada ao entrar na conta e a cada mudança dos pendentes; há também uma rodada periódica (15 min, o mínimo do WorkManager). Tentativa de novo com espera crescente (30 s, dobrando).
- **Sessão expirada:** renovação recusada de vez apaga as chaves e a sessão (volta ao Login); os dados locais não são apagados.
- **Não sincronizado:** faixas da água e agenda de medição (ficam por aparelho).
- **Limitação conhecida:** os dados locais não são separados por conta. Se outra conta de outra fazenda entrar no mesmo aparelho, os registros da anterior continuam no banco local. Separar por fazenda fica para uma fase de manutenção.

