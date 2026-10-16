# Fase 08: Perfis (ADMIN e FUNCIONARIO), usuários e configurações

Leia antes: `06-perfis-permissoes.md`, `11-autenticacao.md`, `10-telas-e-navegacao.md`. Pré-requisito: Fases 01 a 07.
Skill: `auth-screens`.

- [ ] Etapa 1: entity `Usuario` (`nome`, `email`, `papel`, `status`, campos de sincronização), DAO e repositório. Acrescentar `criadoPorUserId` nos registros de água, ração, biometria e movimentação.
- [ ] Etapa 2: `PermissionChecker` no domínio (tabela de `06`). A permissão é verificada **no caso de uso**, não só na tela.
- [ ] Etapa 3: menu e telas mostram só o que o perfil pode usar (FUNCIONARIO não vê `Users`, custos nem relatórios).
- [ ] Etapa 4: tela `Users` (ADMIN): listar, convidar por e-mail, trocar perfil, desativar. Sem internet, o convite fica pendente e avisa.
- [ ] Etapa 5: tela `Settings`: faixas e agenda de medição (`MonitoringSchedule`) editáveis pelo ADMIN, perfil do usuário e **Sair** com aviso do número de registros ainda não enviados.
- [ ] Etapa 6: testes: cada permissão da tabela de `06` (permitido e negado) e o aviso de pendências no logout.

## Fora do escopo agora

Custos e relatórios (spec próprio depois), login social, servidor real.

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; um FUNCIONARIO não consegue executar ação de ADMIN nem chamando o caso de uso diretamente.
