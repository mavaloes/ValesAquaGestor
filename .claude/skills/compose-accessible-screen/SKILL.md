---
name: compose-accessible-screen
description: Use ao criar qualquer tela, botão ou componente de UI em Compose neste projeto.
---

# Tela acessível e simples

1. Consulte `specs/04-requisitos-ux.md`.
2. Botões: altura mínima de 48dp, ícone + texto.
3. Textos em `sp`; nenhum texto fixo em `dp`.
4. Todo ícone tem `contentDescription` vindo de `strings.xml`.
5. Cores só do tema Material 3 (garante contraste).
6. Toda ação importante dispara um feedback (Snackbar) com texto de `strings.xml`.
7. Valor medido = valor + status + ação sugerida (use o componente `StatusCard`).
8. Teste mentalmente: a tarefa principal cabe em 3 toques?