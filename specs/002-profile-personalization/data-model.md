# Data Model: Profile Personalization

## Existing Schema (sem alteração)

A `UserProfileEntity` já possui todos os campos necessários. Nenhuma migration de schema é necessária.

```
user_profiles
├── id             INTEGER  PRIMARY KEY
├── name           TEXT     NOT NULL
├── learning_goals TEXT     NOT NULL  DEFAULT ''   -- valor único: 'travel'|'business'|'casual'
├── preferred_themes TEXT   NOT NULL  DEFAULT ''   -- CSV: 'restaurant,airport,shopping'
├── ai_voice_gender  TEXT   NOT NULL  DEFAULT 'feminine'   -- 'feminine'|'masculine'
├── ai_voice_accent  TEXT   NOT NULL  DEFAULT 'american'   -- 'american'|'british'
└── ai_speech_rate   TEXT   NOT NULL  DEFAULT 'normal'     -- 'slow'|'normal'|'fast'
```

---

## Domain Enumerations (novas, no source)

```
LearningGoal
├── TRAVEL   ("travel",  "Viagem")
├── BUSINESS ("business","Negócios")
└── CASUAL   ("casual",  "Conversa casual")

VoiceGender
├── FEMININE  ("feminine",  "Feminino")
└── MASCULINE ("masculine", "Masculino")

VoiceAccent
├── AMERICAN ("american", "Inglês americano")
└── BRITISH  ("british",  "Inglês britânico")

SpeechRate
├── SLOW   ("slow",   "Lento")
├── NORMAL ("normal", "Normal")
└── FAST   ("fast",   "Rápido")

ConversationTheme
├── RESTAURANT   ("restaurant",   "Restaurantes")
├── AIRPORT      ("airport",      "Aeroportos e viagens")
├── HOTEL        ("hotel",        "Hotéis e hospedagem")
├── JOB_INTERVIEW("job_interview","Entrevistas de emprego")
├── SHOPPING     ("shopping",     "Compras")
├── TOURISM      ("tourism",      "Turismo e pontos turísticos")
├── HEALTH       ("health",       "Saúde e consultas médicas")
├── WORK_MEETINGS("work_meetings","Reuniões de trabalho")
├── SOCIAL       ("social",       "Vida social e amizades")
└── TECHNOLOGY   ("technology",   "Tecnologia e gadgets")
```

---

## Domain Model (UI layer)

```
ProfilePreferences
├── learningGoal    : LearningGoal?           -- null = não configurado
├── selectedThemes  : Set<ConversationTheme>  -- pode ser vazio
└── voicePreference : VoicePreference

VoicePreference
├── gender  : VoiceGender
├── accent  : VoiceAccent
└── rate    : SpeechRate
```

---

## Mapeamento Entity ↔ Domain

| Entity field       | Domain field                        | Conversão                                  |
|--------------------|-------------------------------------|--------------------------------------------|
| `learning_goals`   | `ProfilePreferences.learningGoal`   | String → `LearningGoal.entries.find { it.id == value }` |
| `preferred_themes` | `ProfilePreferences.selectedThemes` | CSV split → `Set<ConversationTheme>`       |
| `ai_voice_gender`  | `VoicePreference.gender`            | String → `VoiceGender`                     |
| `ai_voice_accent`  | `VoicePreference.accent`            | String → `VoiceAccent`                     |
| `ai_speech_rate`   | `VoicePreference.rate`              | String → `SpeechRate`                      |

---

## DAO — Novos métodos necessários

```
UserProfileDao (additions)
├── @Update  fun update(profile: UserProfileEntity): suspend
└── @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getById(id: Int): Flow<UserProfileEntity?>   -- já existe
```

---

## Repository — Novos métodos

```
UserRepository (additions)
└── suspend fun updateProfile(profile: UserProfileEntity)

UserRepositoryImpl (additions)
└── override suspend fun updateProfile(profile: UserProfileEntity) = dao.update(profile)
```

---

## Estado de UI (ProfileEditViewModel)

```
ProfileEditUiState
├── isLoading        : Boolean
├── profileId        : Int
├── profileName      : String
├── learningGoal     : LearningGoal?
├── selectedThemes   : Set<ConversationTheme>
├── voiceGender      : VoiceGender
├── voiceAccent      : VoiceAccent
├── speechRate       : SpeechRate
└── isSaved          : Boolean      -- trigger para navegação pós-save

ProfileEditViewModel
├── uiState    : StateFlow<ProfileEditUiState>
├── setLearningGoal(goal: LearningGoal)
├── toggleTheme(theme: ConversationTheme)
├── setVoiceGender(gender: VoiceGender)
├── setVoiceAccent(accent: VoiceAccent)
├── setSpeechRate(rate: SpeechRate)
└── savePreferences()     -- suspend → atualiza Room → Flow re-emite → MainViewModel navega
```
