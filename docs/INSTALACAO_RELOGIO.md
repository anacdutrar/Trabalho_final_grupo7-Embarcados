# Compilação e instalação no Galaxy Watch 6

Esta etapa deve ser feita pelo integrante que possui o relógio.

## 1. Preparar o computador

1. Instale a versão estável atual do Android Studio.
2. No SDK Manager, instale Android SDK Platform 35 e Android SDK Platform-Tools.
3. Confirme que o Android Studio está usando seu JDK integrado, versão 17 ou superior.
4. Abra a pasta `wearos-app` como projeto.
5. Aguarde a sincronização do Gradle. O primeiro build requer internet para
   baixar o Android Gradle Plugin, Kotlin e LiteRT.

O projeto usa Gradle Wrapper 8.9; não instale Gradle separadamente.

## 2. Adicionar o modelo aprovado

Copie somente o modelo que passou por `VALIDACAO_MODELO.md`:

```text
app/src/main/assets/wisdm_har_int8.tflite
```

Não renomeie o arquivo e não copie um modelo float no lugar do `int8`.

## 3. Testes e build

No terminal do Android Studio, na raiz `wearos-app`:

### Windows

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

O APK esperado é:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 4. Ativar depuração no relógio

1. No relógio, abra Configurações > Sobre o relógio > Informações do software.
2. Toque sete vezes em Número da versão para liberar opções do desenvolvedor.
3. Ative Depuração ADB e Depuração sem fio.
4. Deixe computador e relógio na mesma rede Wi-Fi.
5. Em “Emparelhar novo dispositivo”, anote IP, porta e código.

Use o terminal com o `adb` do Android SDK:

```powershell
adb pair IP:PORTA_DE_PAREAMENTO
adb connect IP:PORTA_DE_CONEXAO
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

A porta de conexão geralmente é diferente da porta de pareamento.

## 5. Teste físico no Galaxy Watch6 Classic

1. Coloque o relógio no pulso dominante.
2. Abra o aplicativo WISDM Watch.
3. Aguarde aproximadamente cinco segundos para preencher a primeira janela.
4. Observe a atividade identificada, exibida em verde na parte superior.
5. Execute cada uma das cinco atividades durante pelo menos 20 segundos.
6. Registre a atividade realizada e a classificação apresentada.
7. Repita os testes pelo menos três vezes para cada atividade.

O aplicativo realiza inferências locais aproximadamente a cada segundo.

A tela permanece acesa enquanto o aplicativo estiver visível, facilitando
os testes. Essa configuração aumenta o consumo de bateria.

O reconhecimento utiliza acelerômetro e giroscópio, sem necessidade
de conexão com a internet durante a inferência.

Referência oficial:
https://developer.android.com/training/wearables/get-started/debug-wifi
