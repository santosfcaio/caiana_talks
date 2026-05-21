# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

### Estrutura de source
- `ui/profileselection/` — tela de seleção de perfil
- `ui/profileedit/` — tela de edição de preferências do perfil
- `ui/home/` — tela inicial (inclui botões "Iniciar conversa" e "Co-practice")
- `ui/settings/` — tela de configurações
- `ui/conversation/` — tela de conversa por voz, tela de setup de co-prática, tela de resumo de sessão
- `ui/stats/` — tela de estatísticas do usuário
- `ui/navigation/` — grafo de navegação (Jetpack Navigation Compose)
- `domain/model/` — enumerações e modelos de domínio (`LearningGoal`, `ConversationTheme`, `VoiceGender`, `VoiceAccent`, `SpeechRate`, `ProfilePreferences`, `AiPersona`, `SessionMode`, `SessionStatus`, `ConversationError`, `AiStreamEvent`, `AiResponseMeta`, `SessionResult`)
- `data/local/db/` — Room database v3 (sessions + conversation_turns + corrections), DAOs e entidades
- `data/local/preferences/` — DataStore de preferências do usuário
- `data/conversation/` — `SentenceChunker`, `RollingWindow`, `SystemPromptBuilder`, `SpeechRecognizerService`, `TextToSpeechService`, `VoiceSelector`, `SseEventParser`, `AiResponseParser`, `SessionDurationPolicy`
- `data/remote/` — `ConversationAiClient` (OpenRouter.ai via OkHttp SSE — `OpenRouterConversationAiClient`; defaults from `BuildConfig.OPENROUTER_API_KEY` / `OPENROUTER_MODEL`, overridable from Settings)
- `data/repository/` — `UserRepository`, `ConversationRepository`, `StatsRepository`
- `di/` — módulos Hilt (`ConversationModule`, `DatabaseModule`, `RepositoryModule`)

## Testes

Para rodar todos os testes unitários:
```powershell
.\gradlew test
```

Para rodar apenas os testes de debug (mais rápido):
```powershell
.\gradlew testDebugUnitTest
```

Para rodar um teste específico:
```powershell
.\gradlew testDebugUnitTest --tests "com.caiana.talks.ui.ProfileEditViewModelTest"
```

## Emulador Android

Para iniciar o emulador e abrir o app com as últimas alterações, execute os comandos abaixo **em sequência no PowerShell**:

```powershell
# 1. Iniciar o emulador
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
Start-Process -FilePath $emulator -ArgumentList "-avd CaianaTalks_Pixel6 -no-snapshot-load" -WindowStyle Normal

# 2. Aguardar o boot (rode após ~20 segundos)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb wait-for-device shell getprop sys.boot_completed

# 3. Build e instalar o app
.\gradlew installDebug

# 4. Abrir o app
& $adb shell am start -n "com.caiana.talks/.MainActivity"
```

Para fechar o emulador:
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb emu kill
```

## Detalhamento

Para decisões de design e princípios do produto, consulte a constituição em
`.specify/memory/constitution.md`.
