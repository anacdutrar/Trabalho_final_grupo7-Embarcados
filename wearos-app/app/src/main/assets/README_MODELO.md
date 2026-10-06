# Modelo ainda nao instalado

Depois de executar e validar o notebook, copie o arquivo exportado para esta
pasta com o nome exato:

`wisdm_har_int8.tflite`

O app valida em tempo de execucao se o modelo possui entrada `int8[1,100,6]`
e saida `int8[1,5]`. Sem esse arquivo, o app abre normalmente e explica que o
modelo precisa ser instalado.

