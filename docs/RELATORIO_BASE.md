# Reconhecimento de atividade humana no Galaxy Watch 6 com IA embarcada

## 1. Objetivo

Descrever o reconhecimento local de caminhada, corrida, escadas, escovação de
dentes e palmas a partir de acelerômetro e giroscópio de um smartwatch.

## 2. Dataset

- WISDM Smartphone and Smartwatch Activity and Biometrics Dataset.
- Gary M. Weiss, UCI Machine Learning Repository.
- DOI: 10.24432/C5HK59.
- Licença: CC BY 4.0.
- 51 participantes, 18 atividades e coleta nominal a 20 Hz.
- Neste trabalho: somente dados do smartwatch e classes A, B, C, G e R.

Registrar aqui as contagens efetivamente exibidas pelo notebook.

## 3. Prevenção de vazamento

Explicar que a separação foi feita por participante antes da sincronização e
das janelas: 35 treino, 8 validação e 8 teste. Documentar que normalização e
calibração `int8` utilizaram apenas treino e anexar as listas exportadas em
`participant_split.json`.

## 4. Pré-processamento

- Sincronização temporal de acelerômetro e giroscópio.
- Grade uniforme de 20 Hz.
- Interpolação máxima de 250 ms.
- Janelas de cinco segundos com avanço de um segundo.
- Seis canais na ordem definida no contrato.
- Normalização incorporada ao modelo e calculada somente no treino.

## 5. Modelo

Descrever a CNN 1D, quantidade de parâmetros, early stopping, pesos de classe e
conversão totalmente `int8`. Inserir o tamanho e SHA-256 de `metrics.json`.

## 6. Resultados

Preencher após executar o notebook:

| Modelo | Acurácia | Macro-F1 | Tamanho |
|---|---:|---:|---:|
| Keras |  |  | — |
| TFLite int8 |  |  |  |

Adicionar curvas de treino, matriz de confusão e métricas por classe. Discutir
especialmente confusões entre caminhada e escadas.

## 7. Aplicativo embarcado

O app Wear OS usa `SensorManager` e `SensorEventListener`, solicita os dois
sensores a 20 Hz, reamostra uma janela de 100 × 6, quantiza conforme o tensor,
executa LiteRT localmente e suaviza as três últimas previsões. Não depende de
rede, celular ou servidor para inferência.

## 8. Teste no Galaxy Watch 6

Preencher dispositivo, versão do Wear OS, pulso utilizado, taxa observada,
latência, comportamento de cada classe e problemas encontrados. Separar esses
resultados qualitativos das métricas formais do WISDM.

## 9. Limitações

- O WISDM foi coletado num LG G Watch, não no Galaxy Watch 6.
- Não houve coleta local rotulada para adaptação de domínio.
- A classe C não distingue subida de descida.
- Posição, aperto da pulseira, usuário e estilo de movimento podem alterar as leituras.
- A execução em primeiro plano foi escolhida para uma demonstração acadêmica curta.

## 10. Conclusão

Concluir somente após a avaliação do modelo e o teste físico. Não afirmar
generalização para o Galaxy Watch 6 apenas com base no teste interno do WISDM.

