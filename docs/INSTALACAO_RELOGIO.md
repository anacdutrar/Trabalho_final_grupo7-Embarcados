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

## 5. Teste físico

1. Coloque o relógio no pulso dominante, como no WISDM.
2. Abra Watch HAR e pressione Iniciar.
3. Aguarde cinco segundos para preencher a primeira janela.
4. Execute cada atividade por pelo menos 20 segundos.
5. Registre classe mostrada, confiança, estabilidade e eventuais erros.
6. Repita ao menos três vezes por atividade.

O MVP mantém a tela ativa durante a coleta e para os sensores quando o app sai
da tela. Ele não é um monitor contínuo em segundo plano.

Referência oficial: https://developer.android.com/training/wearables/get-started/debug-wifi

