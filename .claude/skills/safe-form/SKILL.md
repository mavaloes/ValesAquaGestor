---
name: safe-form
description: Use ao criar formulários de cadastro ou registro (alimentação, água, lote).
---

# Formulário seguro

1. Cada campo mostra rótulo, unidade e se é obrigatório.
2. Validação no ViewModel, exposta no UiState (ex.: `quantidadeError: String?`).
3. Valor improvável gera aviso com opção "Salvar mesmo assim".
4. Botão Salvar só habilita com os obrigatórios preenchidos.
5. Exclusão: diálogo de confirmação explicando o que será apagado.
6. Ao salvar: Snackbar "Registro salvo" (ou "Sem conexão: dados guardados no aparelho").