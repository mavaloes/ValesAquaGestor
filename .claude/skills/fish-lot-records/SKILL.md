---
name: fish-lot-records
description: Use ao criar telas, ViewModels ou DAOs de Lote, Biometria, Mortalidade/Despesca e Arraçoamento, ou ao calcular quantidade atual, biomassa e mortalidade.
---

# Registros do lote (Lote, Biometria, MovimentacaoLote, Arracoamento)

Modelo: `specs/02-dominio.md`. Ele nasce das 7 planilhas de campo do Manual Embrapa 2024 (F01 p.150-156). Se o produtor já anota assim no papel, o app deve ter os mesmos campos na mesma ordem.

## Passo a passo

1. **Quantidade e biomassa nunca são gravadas.** Calcule com `quantidadeInicial` menos as movimentações. Assim não há número "errado" depois de uma edição offline.
2. **Biometria:**
   - Entrada por pesagens: peso com peixes, tara (balde com água ou puçá molhado) e nº de peixes por pesagem. Gravar só os totais.
   - Amostra sugerida: lote até ~2.000 peixes → 10%; maior → cerca de 200 (F03 p.106). Avisar se a amostra for menor.
   - Ao salvar, atualizar `Lote.pesoMedioAtualG` e sugerir o novo valor de ração.
3. **Mortalidade:** registro diário rápido (quantidade + sinais observados). Mortalidade acumulada do ciclo > 10% → alerta "chame um técnico" (F02 p.69).
4. **Despesca:** parcial (venda, classificação, ajuste de densidade) ou final. A final encerra o lote.
5. **Arraçoamento:** campos da Planilha 4 (horário, marca, % proteína, mm, quantidade, comportamento dos peixes). "Ração suspensa" é uma opção, com motivo.
6. **Povoamento:** mostrar a capacidade do viveiro (`CapacityCalculator`) e avisar se o nº de peixes passa dela.
7. **Sincronização:** cada registro novo ou editado recebe `uuid`, `atualizadoEm` e `sincronizado = false`. Excluir = `excluido = true`.

## Padrões de tela (requisitos 3, 5 e 11)

- Até 3 toques para registrar um trato ou uma mortalidade, a partir da tela inicial.
- Unidade visível ao lado do campo. Valor improvável gera **aviso** com "Salvar mesmo assim" (ex.: peso médio 10 vezes maior que o da última biometria).
- Confirmar antes de excluir, dizendo o que será apagado.
- Depois de salvar: "Registro salvo" ou "Sem conexão: dados guardados no aparelho".
- Teste a tela com fonte 200% (`fontScale = 2f`).
