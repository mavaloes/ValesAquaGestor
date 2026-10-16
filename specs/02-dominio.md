# Domínio (MVP, v2)

> Substitui o `02-dominio.md` anterior. Já inclui os campos de sincronização.
> Base: as 7 planilhas de campo do Manual Embrapa 2024 (F01 p.150-156) e os manuais SENAR.
> Itens 🔶 = decisão de projeto, explicada em `08-fontes-e-conflitos.md`.

## Decisões já tomadas (mantidas)

- Datas e horas: `Long` (epoch millis) no Room; `Instant`/`LocalDateTime` no domain.
- Enums salvos como `String` via TypeConverter.
- IDs: `Long` com autoGenerate (chave local). O `uuid` é a identidade usada na sincronização.
- Exclusão sempre **lógica** (`excluido = true`); por isso todas as FKs usam `onDelete = RESTRICT`.
- Índice em toda coluna de FK.
- Índice **único** em `uuid` em toda entidade: o mesmo registro nunca aparece duas vezes, no aparelho nem na sincronização (caso G1 de `09`).

## Campos de sincronização (todas as entidades)

`uuid: String` · `atualizadoEm: Long` · `excluido: Boolean` · `sincronizado: Boolean`

## O que mudou em relação ao prompt original (resumo)

| Item | Antes | Agora | Por quê |
|---|---|---|---|
| Tanque | só `volumeM3` | + `areaM2`, `profundidadeMediaM`, aeração, renovação de água | Viveiro e açude têm capacidade calculada por **área** (F03 p.15); só tanque-rede usa volume (F12 p.14) |
| `TipoTanque` | REDONDO, ESCAVADO, ALVENARIA | + ACUDE, TANQUE_REDE 🔶 | Os manuais descrevem viveiro escavado, açude e tanque-rede (F12 p.10-14) |
| Lote | sem fase | + `fase`, peso inicial, status, origem | 3 fases: alevinagem, recria, engorda (F03 p.16); fazenda real usa berçário, recria, terminação (F15 p.14) |
| QualidadeAgua | O₂, pH, temp., `amoniaPpm` | + transparência, nitrito, alcalinidade, dureza; **amônia total** (a tóxica é calculada) | Planilhas 2 e 3 (F01 p.151-152); kit mede amônia total (F13 p.36) |
| Arracoamento | `tipoRacao` | marca, % proteína, mm, comportamento, sobra, suspensão | Planilha 4 (F01 p.153) |
| Biometria | não existia | **nova** | Planilha 6 (F01 p.155); base do ajuste de ração e da TCA |
| MovimentacaoLote | não existia | **nova** (mortalidade, despesca, transferência) | Planilha 7 (F01 p.156); sem ela não existe "peixes vivos" nem TCA |

## Entidades

### 1. Tanque
`id, nome, tipo (ESCAVADO, ACUDE, TANQUE_REDE, ALVENARIA, REDONDO), areaM2?, profundidadeMediaM?, volumeM3?, usaAerador, aeradorPotenciaHp?, renovacaoAguaPctDia, status (ATIVO, INATIVO)`

- Para ESCAVADO e ACUDE, `areaM2` é obrigatória (base da capacidade). Para os demais, `volumeM3` é obrigatório.
- `volumeM3`: se vier em litros, converter na entrada (decisão original mantida). Se houver área e profundidade, `volumeM3 = areaM2 × profundidadeMediaM`.
- A "condição de criação" (1 a 4 da tabela F03 p.15) é **derivada**: `usaAerador` × (`renovacaoAguaPctDia` ≥ 5).

### 2. Lote
`id, tanqueId, especie (TILAPIA, TAMBAQUI, PIRARUCU, OUTRA), fase (ALEVINAGEM, RECRIA, ENGORDA), quantidadeInicial, pesoMedioInicialG, dataPovoamento, pesoMedioAtualG, origemAlevinos?, status (ATIVO, ENCERRADO), dataEncerramento?`

Derivados (não gravar): `quantidadeAtual`, `biomassaKg`, `mortalidadeAcumuladaPct`, `tcaReal`.

```
quantidadeAtual = quantidadeInicial − Σ(MORTALIDADE + DESPESCA_PARCIAL + DESPESCA_FINAL + TRANSFERENCIA)
biomassaKg      = quantidadeAtual × pesoMedioAtualG / 1000
```

- Ao salvar uma Biometria, `pesoMedioAtualG` é atualizado.
- Ao registrar DESPESCA_FINAL, o lote vira ENCERRADO.
- Tabela de ração completa: só TILAPIA no MVP. TAMBAQUI e PIRARUCU já têm tabela nas fontes (F01 p.78-79) para uma fase seguinte. OUTRA = o usuário informa o % do peso vivo.

### 3. QualidadeAgua
`id, tanqueId, dataHora, momento (MANHA, MEIO_DIA, TARDE, NOITE), transparenciaCm?, temperaturaC?, oxigenioMgL?, ph?, amoniaTotalMgL?, nitritoMgL?, alcalinidadeMgL?, durezaMgL?, diaNublado`

- Todas as medidas são opcionais (medição diária e semanal usam a mesma tabela). Exigir **pelo menos uma**.
- `momento` importa: oxigênio e pH têm leitura de manhã e de fim de tarde (F01 p.66; F13 p.27).
- A amônia **tóxica não é gravada**: é calculada (`03-regras-negocio.md`, seção 4.1).
- "ppm" e "mg/L" são a mesma unidade (F07 p.7); usar sempre **mg/L** na tela.

### 4. Arracoamento
`id, loteId, dataHora, quantidadeKg, suspenso, motivoSuspensao? (OXIGENIO_BAIXO, AMONIA_ALTA, NITRITO_ALTO, TEMPERATURA, SOBRA_RACAO, DOENCA, OUTRO), marcaRacao?, proteinaBrutaPct?, tamanhoPelletMm?, comportamento (VORAZ, NORMAL, LENTO, BOQUEJANDO, SEM_APETITE), sobraRacao`

- `quantidadeKg` é a ração **ofertada**; quando `suspenso`, vale 0.
- A Planilha 4 registra "ração suspensa" e "peixes boquejando na superfície" (F01 p.153); o app trata isso como dado, não como texto livre.

### 5. Biometria (nova)
`id, loteId, dataHora, pesoAmostraKg (peixes, já sem a tara), numeroPeixesAmostra, observacaoSaude?`

- `pesoMedioG = pesoAmostraKg / numeroPeixesAmostra × 1000`.
- A tela pode ter várias pesagens (balde + água, peixes por balde, como na Planilha 6) e gravar só os totais.
- Tamanho da amostra: lote até ~2.000 peixes → cerca de 10%; lote maior → cerca de 200 peixes (F03 p.106).

### 6. MovimentacaoLote (nova)
`id, loteId, dataHora, tipo (MORTALIDADE, DESPESCA_PARCIAL, DESPESCA_FINAL, TRANSFERENCIA), quantidade, pesoTotalKg?, tanqueDestinoId?, observacao?`

- Despesca parcial inclui retirada para ajustar densidade, classificar ou vender (F01 p.156).
- 🔶 Ponto em aberto: na TRANSFERENCIA, criar automaticamente um lote no tanque de destino? A fazenda real redistribui peixes entre tanques por peso (F15 p.14). No MVP só se registra a baixa; decidir na fase de telas.

## Relacionamentos

- Tanque 1—N Lote · Tanque 1—N QualidadeAgua
- Lote 1—N Arracoamento · Lote 1—N Biometria · Lote 1—N MovimentacaoLote
- Classes `@Relation`: `TanqueComLotes`, `LoteComArracoamentos`, `LoteCompleto` (biometrias + movimentações).

## Entidades futuras (não implementar agora)

- `PreparoViveiro` (desinfecção, calagem, adubação, povoamento): Planilha 1, F01 p.150.
- `RegistroSanitario` (sinais de doença, tratamento, quarentena de 45 a 90 dias): Planilha 5, F01 p.154; F02 p.35.
- `EstoqueRacao` e `Custo`: a ração é de 60% a 70% do custo (F14 p.9).
- `Usuario` (FUNCIONARIO, ADMIN).
