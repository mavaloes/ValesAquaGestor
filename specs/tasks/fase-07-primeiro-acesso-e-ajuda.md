# Fase 07: Primeiro acesso guiado e ajuda dentro do app

Leia antes: `04-requisitos-ux.md` (seções "Primeiro acesso" e "Ajuda"), `10-telas-e-navegacao.md`, `00-glossario.md`. Pré-requisito: Fases 01 a 06.

- [x] Etapa 1: `Onboarding` com no máximo **3 telas curtas** (o que o app faz, como registrar, como funciona sem internet). Botão **Pular** sempre visível. Salvar "concluído" no aparelho; não mostrar de novo.
- [x] Etapa 2: `SetupFarm`: criar o primeiro tanque e o primeiro lote passo a passo, com exemplo em cada campo. Pode ser pulado e retomado depois a partir da Home.
- [x] Etapa 3: ícone "?" em cada tela, abrindo dica curta com exemplo real (ex.: "Oxigênio: meça de manhã, antes de dar ração. Abaixo de 3 mg/L, não alimente.").
- [x] Etapa 4: tela `Help` com tutoriais curtos (registrar ração, medir a água, fazer biometria) e explicações em linguagem simples usando o glossário (`00`).
- [x] Etapa 5: decisão da tela inicial: com sessão e primeiro acesso não concluído vai para `Onboarding`; concluído vai para `Home` (regra de `10`).
- [x] Etapa 6: testes (decisão da tela inicial; "Pular" salva como concluído).

## Fora do escopo agora

Perfis, sincronização, vídeo, QR Code.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; textos em `strings.xml`; `@Preview` com `fontScale = 2f`.
