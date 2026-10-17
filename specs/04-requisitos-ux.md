# Requisitos de UX

## Primeiro acesso (1)
- Tutorial de no máximo 3 telas curtas, com botão "Pular" sempre visível.
- Configuração guiada: criar o primeiro tanque e o primeiro lote.

## Interface (2)
- Botões grandes, sempre com ícone + texto.
- Menu inferior com no máximo 4 itens.

## Poucos toques (3)
- Registrar alimentação: no máximo 3 toques a partir da tela inicial
  (tanque → quantidade → salvar).
- Campos vêm preenchidos: data/hora atual, último tipo de ração usado.

## Informação compreensível (4)
- Todo valor medido mostra: valor + status (adequado/atenção/crítico) + ação sugerida.
- Exemplo: "Oxigênio: 3 mg/L, baixo para tilápia. Ligue o aerador."
- Os níveis (ADEQUADO, ATENCAO, CRITICO, EMERGENCIA) e as ações estão em 03-regras-negocio.md, seção 1 e 2.

## Prevenção de erros (5)
- Unidade ao lado de cada campo; campos obrigatórios marcados.
- Aviso (não bloqueio) para valores improváveis, ex.: pH > 10.
- Excluir sempre pede confirmação.

## Ajuda (6)
- Ícone "?" em cada tela abrindo dica curta com exemplo.

## Acessibilidade (10)
- Área de toque mínima de 48dp; contraste mínimo 4,5:1.
- Texto em sp (respeita ampliação); contentDescription em todo ícone.

## Feedback (11)
- Toda ação importante mostra mensagem: "Registro salvo",
  "Sem conexão: dados guardados no aparelho", "Verifique a quantidade".

## Rotina diária (12)
- Tela inicial lista: medições pendentes, alimentações previstas, alertas.