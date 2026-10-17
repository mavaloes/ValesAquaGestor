---
name: water-quality-rules
description: Use ao criar ou alterar alertas de qualidade da água (oxigênio, pH, temperatura, amônia, nitrito, transparência, alcalinidade) ou o cálculo de amônia tóxica.
---

# Regras de qualidade da água

## Antes de escrever código

1. Leia `specs/03-regras-negocio.md` (seções 1 a 4.1) e `specs/08-fontes-e-conflitos.md`.
2. **Não invente faixa nem arredonde.** Se o valor não está no spec, pare e pergunte.

## Passo a passo

1. Constantes em `domain/rules/WaterThresholds.kt`, uma `object` por parâmetro, **com a fonte no comentário**:
   ```kotlin
   /** Oxigênio dissolvido para tilápia, em mg/L. */
   object OxygenThresholds {
       /** @see F01 p.59 — adequado a partir de 4,0 */
       const val ADEQUATE_MIN_MG_L = 4.0
       /** @see F13 p.27 — abaixo de 3 não alimentar */
       const val CRITICAL_BELOW_MG_L = 3.0
       /** @see F13 p.27 — abaixo de 2 há risco de mortalidade */
       const val EMERGENCY_BELOW_MG_L = 2.0
   }
   ```
2. O avaliador devolve `WaterStatus(level, messageKey, actionKey)` com `level` em `ADEQUADO, ATENCAO, CRITICO, EMERGENCIA`. Os textos ficam em `strings.xml` (português), nunca no domínio.
3. **Amônia:** nunca compare a amônia total com limite. Calcule a tóxica (`total × %NH3(pH, °C) / 100`) e compare essa. Sem pH ou temperatura, devolva "dado insuficiente" em vez de chutar.
4. Limites: inclusivos no mínimo da faixa (4,0 já é ADEQUADO). Nos limites superiores de pH e temperatura, o valor exato fica na faixa melhor.
5. Variação de pH no dia > 2 e previsão de oxigênio < 4 são alertas **compostos**: ficam em funções próprias, não dentro do avaliador simples.
6. Toda mensagem diz **o que significa e o que fazer** (ex.: "Oxigênio baixo. Não alimente agora. Renove a água e deixe o aerador pronto.").
7. Escreva os testes de `specs/09-casos-de-teste.md` (A, E e F) **antes** da implementação e rode `./gradlew :app:testDebugUnitTest`.

## Armadilhas

- A fonte pode ter número diferente da outra: a decisão já está na seção 3 do `08`. Se mudar a decisão, mude o `08` primeiro.
- Não crie faixa por **fase** do cultivo: nenhuma fonte traz isso.
- Espécie diferente de tilápia: usar as mesmas faixas e mostrar "faixa de referência geral".
