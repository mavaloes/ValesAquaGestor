# Fase 08: Perfis (ADMIN e FUNCIONARIO), usuários e configurações

Leia antes: `06-perfis-permissoes.md`, `11-autenticacao.md`, `10-telas-e-navegacao.md`. Pré-requisito: Fases 01 a 07.
Skill: `auth-screens`.

- [x] Etapa 1: entity `Usuario` (`nome`, `email`, `papel`, `status`, campos de sincronização), DAO e repositório. Acrescentar `criadoPorUserId` nos registros de água, ração, biometria e movimentação.
- [x] Etapa 2: `PermissionChecker` no domínio (tabela de `06`). A permissão é verificada **no caso de uso**, não só na tela.
- [x] Etapa 3: menu e telas mostram só o que o perfil pode usar (FUNCIONARIO não vê `Users`, custos nem relatórios).
- [x] Etapa 4: tela `Users` (ADMIN): listar, convidar por e-mail, trocar perfil, desativar. Sem internet, o convite fica pendente e avisa.
- [x] Etapa 5: tela `Settings`: faixas e agenda de medição (`MonitoringSchedule`) editáveis pelo ADMIN, perfil do usuário e **Sair** com aviso do número de registros ainda não enviados.
- [x] Etapa 6: testes: cada permissão da tabela de `06` (permitido e negado) e o aviso de pendências no logout.

## Fora do escopo agora

Custos e relatórios (spec próprio depois), login social, servidor real.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; um FUNCIONARIO não consegue executar ação de ADMIN nem chamando o caso de uso diretamente.

## Decisões desta fase

- **Faixas editáveis:** os 25 limites de oxigênio, pH, temperatura, amônia tóxica, nitrito, transparência, alcalinidade e dureza (`WaterRanges`). Os padrões continuam sendo as constantes com fonte; os ajustes ficam no DataStore. A validação só exige ordem, valor positivo e pH de 0 a 14 (nenhum mínimo ou máximo inventado). Os limites dos alertas compostos (variação do pH, carga de ração, mortalidade) **não** são editáveis, porque os textos deles citam o número.
- **Configurações:** abre para os dois perfis (conta e Sair); faixas e agenda só aparecem para o ADMIN e são barradas no caso de uso `ManageSettings`.
- **Convites (FakeAuth):** o convite fica no Room como `CONVIDADO` (`sincronizado = false`). No FakeAuth, quem foi convidado cria a conta com o e-mail do convite e entra com o perfil e o `userId` dele; quem foi desativado não entra. O envio real do e-mail é da Fase 09.
- **Banco versão 2:** `Migration(1, 2)` testada com `MigrationTestHelper` (`MigrationTest`); os schemas exportados entram como assets do build debug para o Robolectric achá-los.

