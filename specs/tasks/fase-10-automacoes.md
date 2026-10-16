# Fase 10: Automações (QR Code e sensores)

Leia antes: `07-automacoes.md`, `03-regras-negocio.md`. Pré-requisito: Fases 01 a 06 (a sincronização pode vir antes ou depois).

- [x] Etapa 1: gerar o QR Code de cada tanque (contém o `uuid` do tanque) para imprimir ou compartilhar.
- [x] Etapa 2: leitura do QR Code pela câmera: abre direto o `TankDetail`. Se a câmera for negada ou o código for inválido, mensagem clara e opção de escolher o tanque na lista.
- [x] Etapa 3: interface `WaterSensorSource` no domínio e `ManualWaterSensorSource` (digitado) como primeira implementação. Nenhum modelo de sensor real nesta fase (🔶 aguardando protocolo: Bluetooth ou Wi-Fi).
- [x] Etapa 4: se houver leitura de sensor, preencher o formulário de água automaticamente, **sempre editável** e marcada como "vinda do sensor".
- [x] Etapa 5: testes (QR válido e inválido; leitura do sensor vira rascunho editável).

## Critério de pronto

`assembleDebug` e `testDebugUnitTest` passam; app funciona normalmente sem câmera e sem sensor.

## Decisões desta fase

- **Conteúdo do QR Code:** só o `uuid` do tanque (`TankQrCode`). Leitura: o texto precisa ser um `uuid` válido (senão "não é QR Code de tanque") e o tanque precisa existir neste aparelho (senão "esse tanque não está neste aparelho").
- **Bibliotecas:** ZXing `core` 3.5.4 gera o código (correção de erro H, porque é impresso e suja); `zxing-android-embedded` 4.3.0 lê pela câmera, sem serviços do Google, então funciona offline. A permissão da câmera é pedida pelo próprio app, para poder mostrar a mensagem e a opção de escolher na lista quando for negada. CameraX + ML Kit ficou descartado: mais código e dependência para o mesmo resultado.
- **Câmera é opcional:** `uses-feature` com `required="false"`; sem câmera, a tela de leitura explica e leva à lista.
- **Compartilhar e imprimir:** o PNG (QR Code + nome do tanque embaixo) sai pelo cache do app via `FileProvider`; impressão com `androidx.print`.
- **Sensor:** `WaterSensorSource.takePendingReading(tankId)` entrega a leitura uma vez. `ManualWaterSensorSource` (digitada) é a única implementação; nada na interface entrega leituras ainda, então o formulário de água abre vazio como sempre. Bluetooth ou Wi-Fi entram como outra implementação quando o protocolo (🔶) for escolhido.
- **Marca "vinda do sensor":** só na tela, enquanto o campo não é alterado. O registro salvo não guarda a origem (guardar exigiria nova migração do banco).

