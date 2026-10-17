# Offline e sincronização

- O Room é a fonte da verdade; o app nunca depende de internet para salvar.
- Registros novos/editados ficam com sincronizado = false.
- WorkManager envia os pendentes quando a conexão voltar.
- Deduplicação pelo uuid; conflito resolvido por atualizadoEm mais recente.
- Exclusão lógica (excluido = true) até o servidor confirmar.
- Pendência: definir o backend (ver "Decisões pendentes").