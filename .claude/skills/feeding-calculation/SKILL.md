---
name: feeding-calculation
description: Use ao criar ou alterar o cálculo de ração (tabela da tilápia, fator de temperatura, reajuste semanal, TCA, ganho de peso) ou a tela de arraçoamento.
---

# Cálculo de ração

## Antes de escrever código

1. Leia `specs/03-regras-negocio.md` (seções 4.2 e 4.3) e `specs/09-casos-de-teste.md` (B e C).
2. A tabela é de **tilápia**. Outra espécie: o usuário informa o % do peso vivo (por enquanto).

## Passo a passo

1. Dados de entrada: `quantidadeAtual` do lote (derivada), `pesoMedioAtualG` e a temperatura da água mais recente.
2. `biomassaKg = quantidadeAtual × pesoMedioAtualG / 1000`.
3. Achar a linha da tabela pelo peso médio. Faixas são **[mínimo, máximo)**: 50 g cai em "50 a 150".
4. Aplicar o fator de temperatura (0 · 0,6 · 0,8 · 1,0 · 0,8 · 0). Faixas contínuas: [16,20) [20,25) [25,30) [30,32].
5. `racaoDiariaKg = biomassaKg × pctPV/100 × fatorTemperatura`; dividir pelo número mínimo de refeições da linha.
6. **Travas** (devolver 0 kg com o motivo, nunca bloquear a tela): O₂ < 3 mg/L, temperatura fora de 16 a 32 °C, NH₃ tóxica ≥ 0,2, nitrito ≥ 0,3. Reaproveitar o `WaterEvaluator`, não duplicar limites.
7. **Reajuste semanal:** `ganho = racaoDaSemana / TCAesperada`; `PV ajustado = PV anterior + ganho`; peso médio ajustado → nova linha da tabela. Em alevinos, a cada 3 a 4 dias. A biometria real sempre substitui a estimativa.
8. **TCA real** = ração fornecida ÷ ganho de peso (precisa de `Biometria` e `MovimentacaoLote`).
9. A tela deve pré-preencher quantidade sugerida, horário atual, marca/proteína/mm da última ração, e deixar o trato em até 3 toques.
10. Após cada trato, perguntar sobras: ração parada mais de 10 minutos conta como sobra e reduz a próxima refeição.

## Armadilhas

- Unidades: g × kg. Prefira `Double` em kg dentro do cálculo e converta só na borda.
- **Não copie os exemplos da apostila F14**: eles têm 3 erros de conta (`08`, seção 4). Use os valores do `09`.
- A tabela é um **guia**: o consumo real depende de água, ração e sanidade. Mostrar a quantidade como "sugerida", editável.
