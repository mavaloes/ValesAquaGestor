# Perfis e permissões

Login, cadastro e sessão: `11-autenticacao.md`.

| Recurso | FUNCIONARIO | ADMIN |
|---|---|---|
| Registrar alimentação e mortalidade | Sim | Sim |
| Registrar qualidade da água e biometria | Sim | Sim |
| Ver tanques, lotes e alertas | Sim | Sim |
| Usuários (convidar, remover, trocar perfil) | Não | Sim |
| Custos, configurações e relatórios | Não | Sim |

- O menu mostra só o que o perfil pode usar (não mostrar item desabilitado).
- Quem cria a conta no app vira ADMIN da própria fazenda (🔶, ver `11`).
- A permissão é checada **no domínio** (caso de uso), não só escondida na tela.
- O perfil vem da `Session`; os registros salvos guardam quem os criou (`criadoPorUserId`) a partir da fase de perfis.
