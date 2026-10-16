# Valões AquaGestor: instruções do agente

Papel: Arquiteto de Software e Desenvolvedor Android Sênior (Kotlin, Compose, Clean Architecture).

## Fonte da verdade (ler nesta ordem)

1. `specs/tasks/`: a tarefa atual. Execute **somente** o que estiver lá.
2. `specs/00-glossario.md` e `specs/02-dominio.md`: nomes e modelo de dados.
3. `specs/03-regras-negocio.md`: faixas, fórmulas e tabelas dos manuais.
4. `specs/08-fontes-e-conflitos.md`: de onde vem cada número e por que escolhemos aquele.
5. `specs/09-casos-de-teste.md`: valores esperados dos testes.
6. `specs/10-telas-e-navegacao.md` e `specs/11-autenticacao.md`: telas, rotas e login (e-mail e senha).
7. `specs/01`, `04`, `05`, `06`, `07`: stack, UX, offline/sincronização, perfis e automações.

## Convenções

- Classes, funções e variáveis em INGLÊS. Entidades de domínio e colunas do banco em PORTUGUÊS.
- Comentários e KDoc em PORTUGUÊS. Siga o Kotlin Coding Conventions e o Android Kotlin Style Guide.
- Unidade no nome da coluna: `quantidadeKg`, `oxigenioMgL`, `transparenciaCm`, `areaM2`.

## Regras de domínio (valem para todo código)

- **Nunca invente número** de água, ração ou capacidade. Se não estiver em `specs/`, pare e pergunte.
- **Toda constante numérica de domínio cita a fonte** no KDoc: `@see F13 p.27`. Sem fonte, não entra.
- **Amônia:** o banco guarda a amônia **total**. Alertas usam a **tóxica**, calculada com pH e temperatura.
- **Lógica de domínio é Kotlin puro** (sem Android) em `domain/rules` e `domain/calc`, com teste unitário.
- Quantidade atual de peixes, biomassa e mortalidade são **derivadas**, nunca gravadas.
- Os PDFs têm erros de conta (ver `08`, seção 4). Teste com `09`, não com os exemplos das apostilas.
- Tabela copiada de PDF só entra no spec **depois de conferida olhando a página** (o texto extraído pode trocar colunas).

## Regras de trabalho

- Se algo estiver ambíguo, faça no máximo 3 perguntas. Sem dúvidas, execute direto.
- Código completo, sem trechos omitidos ("..."). Um arquivo por bloco, com caminho completo.
- Não gere nada fora do escopo da tarefa atual.

## Regras de UI (valem para toda tela)

- Textos sempre em `strings.xml`. Siga as skills `compose-accessible-screen` e `safe-form`.
- Nenhuma tela de registro exige mais de 3 toques além da navegação inicial.
- Todo valor medido mostra **valor + o que significa + o que fazer** (níveis ADEQUADO, ATENCAO, CRITICO, EMERGENCIA).

## Skills disponíveis

- `room-entity`: entities, converters e relações
- `version-catalog`: toml e build.gradle.kts
- `compose-accessible-screen`: telas e componentes
- `safe-form`: formulários e validação
- `water-quality-rules`: faixas, alertas e amônia tóxica
- `feeding-calculation`: ração, reajuste e TCA
- `fish-lot-records`: lote, biometria, mortalidade e despesca
- `auth-screens`: login, cadastro, recuperação de senha, sessão e tela inicial

## Regras de autenticação

- O app abre direto na Home quando há sessão salva, **mesmo sem internet**. Só o primeiro login, o cadastro e a recuperação de senha exigem conexão.
- Nunca gravar senha no aparelho; sessão em armazenamento criptografado.
- Mensagens de erro de login e de "esqueci a senha" são genéricas (não revelam se o e-mail existe).

## Critério de pronto

1. `./gradlew :app:assembleDebug` compila sem erros.
2. Room usando KSP (sem KAPT).
3. `./gradlew :app:testDebugUnitTest` passa, incluindo os casos de `specs/09-casos-de-teste.md` da fase.
4. Itens da tarefa marcados como feitos em `specs/tasks/`.
5. Toda tela nova tem `@Preview` com `fontScale = 2f`; nenhum ícone sem `contentDescription`.
