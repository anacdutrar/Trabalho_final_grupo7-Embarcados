# Watch HAR — reconhecimento de atividades no Galaxy Watch 6

Projeto acadêmico de IA embarcada para classificar cinco atividades usando
acelerômetro e giroscópio do relógio:

| Código | Atividade |
|---|---|
| A | Caminhada |
| B | Corrida |
| C | Escadas |
| G | Escovar os dentes |
| R | Bater palmas |

O treinamento usa somente os dados de smartwatch do
[WISDM Smartphone and Smartwatch Activity and Biometrics Dataset](https://archive.ics.uci.edu/dataset/507/wisdm%2Bsmartphone%2Band%2Bsmartwatch%2Bactivity%2Band%2Bbiometrics%2Bdataset).
O modelo é executado localmente no Galaxy Watch 6 por um aplicativo Wear OS.


## Fluxo de trabalho

1. Envie `notebook/WISDM_Watch_HAR_FINAL_GalaxyWatch.ipynb` ao Google Colab.
2. Execute as células em ordem, sem pular os gates de validação.
3. Revise macro-F1, métricas por classe, matriz de confusão e o aviso final.
4. Baixe `watch_har_export.zip` e preserve o ZIP como evidência da execução.
5. Extraia `wisdm_har_int8.tflite` em
   `wearos-app/app/src/main/assets/`.
6. Abra `wearos-app` no Android Studio, sincronize o Gradle e rode os testes.
7. Compile e instale no relógio conforme `docs/INSTALACAO_RELOGIO.md`.

O nome do modelo deve permanecer exatamente:

```text
wearos-app/app/src/main/assets/wisdm_har_int8.tflite
```

## Por que existe Gradle?

Gradle é o sistema de build do Android. Ele baixa as dependências declaradas,
compila Kotlin, agrega manifesto/recursos/modelo e gera o APK. O projeto inclui
o Gradle Wrapper (`gradlew` e `gradlew.bat`); não se instala Gradle à parte.
Ainda são necessários um JDK 17 e o Android SDK, normalmente fornecidos e
configurados pelo Android Studio.

Nenhuma extensão do VS Code é necessária. Extensões de Kotlin, Java e Gradle
são opcionais e não substituem o toolchain Android.

## Contrato entre notebook e app

- taxa solicitada: 20 Hz;
- janela: 5 segundos, 100 instantes;
- avanço: 1 segundo;
- entrada: `int8[1,100,6]`;
- canais: `accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z`;
- saída: `int8[1,5]` na ordem `A, B, C, G, R`;
- normalização: incorporada ao grafo pelo notebook;
- lacuna máxima interpolável: 250 ms.

O aplicativo utiliza os parâmetros de quantização definidos no código Kotlin, que devem coincidir com os valores de `model_contract.json`.

## Implementação Wear OS

O aplicativo foi desenvolvido em Kotlin, utilizando Jetpack Compose.

- Dispositivo testado: Samsung Galaxy Watch6 Classic.
- Inferência local com LiteRT `CompiledModel` na CPU.
- Leitura do acelerômetro e giroscópio com `SensorManager`.
- Reamostragem dos sensores para 20 Hz.
- Classificação a cada segundo, utilizando janelas de 5 segundos.
- Exibição da atividade identificada e dos valores dos sensores.
- Tela mantida acesa durante a utilização do aplicativo.

O aplicativo utiliza o modelo `wisdm_har_int8.tflite` exportado
pelo treinamento em Python.

## Limitação científica

O WISDM foi coletado com um LG G Watch no pulso dominante, enquanto a
demonstração usa um Galaxy Watch 6. Unidades e sensores são compatíveis, mas
isso não elimina a mudança de dispositivo e de população. Sem coleta rotulada
local, as métricas formais valem para participantes não vistos do WISDM; o
teste no Watch 6 é uma demonstração de transferência de domínio.

O dataset é distribuído sob CC BY 4.0. A fonte e o criador devem ser citados no
relatório: Gary M. Weiss, UCI Machine Learning Repository, DOI
`10.24432/C5HK59`.

