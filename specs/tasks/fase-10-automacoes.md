# Fase 10: Automações (QR Code e sensores)

Leia antes: `07-automacoes.md`, `03-regras-negocio.md`. Pré-requisito: Fases 01 a 06 (a sincronização pode vir antes ou depois).

- [ ] Etapa 1: gerar o QR Code de cada tanque (contém o `uuid` do tanque) para imprimir ou compartilhar.
- [ ] Etapa 2: leitura do QR Code pela câmera: abre direto o `TankDetail`. Se a câmera for negada ou o código for inválido, mensagem clara e opção de escolher o tanque na lista.
- [ ] Etapa 3: interface `WaterSensorSource` no domínio e `ManualWaterSensorSource` (digitado) como primeira implementação. Nenhum modelo de sensor real nesta fase (🔶 aguardando protocolo: Bluetooth ou Wi-Fi).
- [ ] Etapa 4: se houver leitura de sensor, preencher o formulário de água automaticamente, **sempre editável** e marcada como "vinda do sensor".
- [ ] Etapa 5: testes (QR válido e inválido; leitura do sensor vira rascunho editável).

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; app funciona normalmente sem câmera e sem sensor.
