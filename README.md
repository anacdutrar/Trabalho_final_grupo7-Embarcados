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

## Análises complementares adicionadas ao projeto
As seções abaixo complementam o fluxo original sem substituir o modelo, o contrato do aplicativo ou o pacote principal watch_har_export.zip.

## Análise Exploratória dos Dados — EDA

O notebook passou a incluir uma etapa de análise exploratória antes do treinamento.
A EDA verifica:
- qualidade e integridade dos dados;
- distribuição das cinco atividades;
- distribuição das janelas entre treino, validação e teste;
- exemplos temporais do acelerômetro e do giroscópio;
- magnitude dos sinais por atividade;
- correlação entre os seis canais;
- média e desvio utilizados na normalização.
- 
As análises que utilizam os valores dos sensores são realizadas principalmente sobre o conjunto de treino, preservando o conjunto de teste para a avaliação final.
A divisão permanece por participante:
Partição	Participantes	Janelas
Treino	35	30.032
Validação	8	7.000
Teste	8	6.810

Essa separação evita que dados da mesma pessoa apareçam simultaneamente em treino e teste.

## Comparação de arquiteturas

Como o Galaxy Watch 6 possui mais memória e capacidade de processamento do que um microcontrolador típico, foram testadas arquiteturas com diferentes níveis de complexidade.

Todas foram treinadas e avaliadas nas mesmas condições:

- mesmos participantes de treino, validação e teste;
- mesma entrada `100 × 6`;
- acelerômetro + giroscópio;
- 20 Hz;
- janela de 5 segundos;
- mesma normalização;
- mesmo conjunto de teste;
- mesma estratégia de quantização INT8.

Foram comparados:

- MLP baseline;
- CNN 1D compacta;
- CNN 1D média;
- CNN 1D profunda.

### Resultados da comparação

| Modelo | Parâmetros | Keras Accuracy | Keras Macro-F1 | TFLite FP32 Accuracy | Tamanho FP32 | TFLite INT8 Accuracy | Macro-F1 INT8 | Tamanho INT8 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **CNN compacta** | **2.157** | **83,98%** | **0,8380** | **83,98%** | **≈14,66 KB** | **83,73%** | **0,8355** | **≈10,70 KB** |
| MLP baseline | 40.709 | 77,21% | 0,7701 | 77,21% | ≈162,90 KB | 77,14% | 0,7696 | ≈46,74 KB |
| CNN média | 21.797 | 82,88% | 0,8290 | 82,88% | ≈92,88 KB | 82,88% | 0,8290 | ≈35,22 KB |
| **CNN profunda** | **55.941** | **84,08%** | **0,8392** | **84,08%** | **≈228,32 KB** | **84,67%** | **0,8452** | **≈75,98 KB** |



