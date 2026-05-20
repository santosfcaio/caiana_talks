# Implementation Plan: Profile Personalization

**Branch**: `002-profile-personalization` | **Date**: 2026-05-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-profile-personalization/spec.md`

## Summary

Adicionar a tela de configuração de preferências do perfil, permitindo que cada usuário defina sua meta de aprendizado (viagem, negócios, conversa casual), temas de conversa preferidos (seleção múltipla de lista estática) e configurações de voz da IA (gênero, sotaque, velocidade). O schema Room já contém todos os campos necessários desde a feature 001; esta feature adiciona enumerações de domínio, o caminho de escrita no DAO/Repository, o `ProfileEditViewModel`, o `ProfileEditScreen` e a lógica de onboarding no `MainViewModel`.

## Technical Context

**Language/Version**: Kotlin 1.9+

**Primary Dependencies**: Jetpack Compose (UI), Jetpack Navigation Compose (routing), Room (leitura e escrita de `UserProfileEntity`), Hilt (injeção de dependências), Kotlin Coroutines + Flow

**Storage**: Room database — tabela `user_profiles` existente. Nenhuma migration necessária (colunas já existem desde a feature 001).

**Testing**: JUnit4, Robolectric (ViewModel unit tests), Compose UI Testing

**Target Platform**: Android (minimum SDK conforme `build.gradle` do projeto)

**Project Type**: Mobile app (Android, Kotlin + Jetpack Compose)

**Performance Goals**: Tela de edição renderiza em menos de 300ms; save persiste e navega sem jank visível

**Constraints**: Zero chamadas de rede; toda persistência é local; UI em português brasileiro

**Scale/Scope**: 1 nova tela (`ProfileEditScreen`), 1 novo ViewModel, expansão de DAO/Repository, modificação de `MainViewModel` e `AppNavGraph`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Notas |
|-----------|--------|-------|
| I. Voice-First Interface | PASS | Tela de configuração usa toque — explicitamente permitido para telas de configuração/settings. As preferências de voz configuradas aqui alimentam o Princípio I nas features de conversa. |
| II. Token-Efficient AI | PASS | Zero chamadas à IA nesta feature. |
| III. Pedagogical Effectiveness | PASS | Meta de aprendizado e temas preferidos serão insumo do system prompt da IA em features futuras. |
| IV. Brazilian Portuguese → English Only | PASS | Toda a UI da tela de preferências é em português brasileiro (FR-008). |
| V. Dual-Speaker Mode | PASS | Cada perfil tem preferências isoladas (FR-011). O modo dual-speaker usa dois perfis distintos — isolação garantida. |
| VI. Personalization & Progress Tracking | PASS | Implementação direta do Princípio VI: meta, temas e configuração de voz. |

**Post-Phase-1 re-check**: Todos os gates continuam verdes. Nenhuma violação introduzida pelo design.

## Project Structure

### Documentation (this feature)

```text
specs/002-profile-personalization/
├── plan.md              # Este arquivo
├── research.md          # Phase 0 — decisões técnicas
├── data-model.md        # Phase 1 — schema, domínio, contratos internos
├── quickstart.md        # Phase 1 — guia de teste manual
└── tasks.md             # Phase 2 output (/speckit-tasks — não criado aqui)
```

### Source Code — arquivos novos

```text
app/src/main/java/com/caiana/talks/
├── domain/
│   └── model/
│       ├── LearningGoal.kt           # enum: TRAVEL, BUSINESS, CASUAL
│       ├── ConversationTheme.kt      # enum: 10 temas predefinidos
│       ├── VoiceGender.kt            # enum: FEMININE, MASCULINE
│       ├── VoiceAccent.kt            # enum: AMERICAN, BRITISH
│       └── SpeechRate.kt             # enum: SLOW, NORMAL, FAST
└── ui/
    └── profileedit/
        ├── ProfileEditScreen.kt      # Compose screen — seções de meta, temas e voz
        └── ProfileEditViewModel.kt   # StateFlow<ProfileEditUiState>, save logic

app/src/test/java/com/caiana/talks/
└── ui/
    └── ProfileEditViewModelTest.kt
```

### Source Code — arquivos modificados

```text
app/src/main/java/com/caiana/talks/
├── data/
│   ├── local/db/
│   │   └── UserProfileDao.kt         # + @Update suspend fun update(profile)
│   └── repository/
│       └── UserRepository.kt         # + suspend fun updateProfile(profile)
└── ui/
    ├── main/
    │   └── MainViewModel.kt          # + StartDestination.ProfileSetup(userName)
    ├── navigation/
    │   └── AppNavGraph.kt            # + rota "profileEdit", handling de ProfileSetup
    └── settings/
        └── SettingsScreen.kt         # + botão "Editar preferências"
```

**Structure Decision**: Single Android application module (`app/`). MVVM padrão. Enumerações de domínio ficam em `domain/model/` para separar a camada de apresentação da camada de dados — a UI referencia o domínio, e o DAO/Repository mapeia Entity ↔ Domain via funções de extensão. Sem módulos adicionais; o projeto permanece com um único módulo `:app`.
