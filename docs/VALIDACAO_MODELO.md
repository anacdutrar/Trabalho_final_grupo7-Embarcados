# Checklist de validação do modelo

Não copie o `.tflite` para o aplicativo até completar esta lista.

## Integridade e vazamento

- [ ] O notebook foi executado do início ao fim numa sessão limpa do Colab.
- [ ] Foram encontrados 51 arquivos de acelerômetro e 51 de giroscópio do relógio.
- [ ] Permaneceram somente as classes A, B, C, G e R.
- [ ] Existem exatamente 35 participantes no treino, 8 na validação e 8 no teste.
- [ ] As três interseções entre listas de participantes são vazias.
- [ ] Todas as cinco classes aparecem nas três partições.
- [ ] Nenhuma janela cruza participante, atividade, partição ou lacuna maior que 250 ms.
- [ ] Média e desvio foram calculados somente com `X_train`.
- [ ] Early stopping usou validação, nunca teste.
- [ ] O conjunto representativo da quantização contém somente índices de treino.

Essas condições também são verificadas por `assert`. Se uma falhar, o pacote
final não deve ser aceito apenas removendo o `assert`; a causa deve ser
investigada.

## Resultado

- [ ] Acurácia e macro-F1 do Keras foram registradas.
- [ ] Acurácia e macro-F1 do TFLite `int8` foram registradas.
- [ ] A queda de macro-F1 após quantização não ultrapassa 0,05.
- [ ] A matriz de confusão e o recall de cada classe foram analisados.
- [ ] O modelo supera claramente o baseline majoritário.
- [ ] Macro-F1 TFLite é pelo menos 0,80, ou um resultado inferior foi
      justificado formalmente pelo grupo.

## Contrato exportado

- [ ] Entrada `int8[1,100,6]`.
- [ ] Saída `int8[1,5]`.
- [ ] `model_contract.json` registra canais, taxa, janela e quantização.
- [ ] `participant_split.json` registra as listas e a semente.
- [ ] `metrics.json` registra métricas, tamanho e SHA-256 do modelo.
- [ ] O SHA-256 do arquivo copiado ao app coincide com o de `metrics.json`.

## Decisão

Preencher antes da integração:

```text
Data:
Responsável pela execução:
TensorFlow:
Macro-F1 Keras:
Macro-F1 TFLite:
SHA-256:
Modelo aprovado? SIM / NÃO
Justificativa:
```

