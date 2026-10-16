# Fase 09: Sincronização com o servidor

Leia antes: `05-offline-sync.md`, `11-autenticacao.md`, `02-dominio.md`. Pré-requisito: Fases 01 a 08 e **backend escolhido** (decisão 🔶 de `11`).

- [ ] Etapa 1: implementação real do `AuthRepository` no backend escolhido (troca o `FakeAuthRepository`; sessão e recuperação de senha de verdade).
- [ ] Etapa 2: API/serviço de sincronização para todas as entities, usando `uuid` como identidade.
- [ ] Etapa 3: `SyncWorker` (WorkManager) que envia registros com `sincronizado = false` e baixa mudanças quando a conexão volta. Com tentativa de novo e espera crescente.
- [ ] Etapa 4: regras: deduplicar por `uuid`; conflito resolvido pelo maior `atualizadoEm`; exclusão lógica só some depois do servidor confirmar.
- [ ] Etapa 5: indicador discreto de estado ("Tudo sincronizado", "3 registros aguardando conexão") e mensagem "Sem conexão: dados guardados no aparelho".
- [ ] Etapa 6: testes: casos G1 a G3 de `09-casos-de-teste.md`, envio duplicado, queda de conexão no meio do envio, dois aparelhos editando o mesmo registro.

## Fora do escopo agora

QR Code e sensores (Fase 10), login social.

## Critério de pronto

Nenhum registro duplicado nem perdido nos testes; app continua totalmente utilizável sem conexão.
