# Research: Profile Personalization

## Decision 1: Learning Goal — single ou múltipla seleção?

**Decision**: Seleção única (um objetivo por perfil).

**Rationale**: O spec e a constitution usam linguagem singular ("learning goal") e exemplos que se excluem mutuamente em contexto prático. Simplifica a lógica de personalização da IA. Pode ser revisado em v2 sem schema change (o campo `learning_goals` já armazena String).

**Alternatives considered**: Seleção múltipla (descartada — adiciona complexidade ao prompt da IA sem ganho claro no v1).

**Storage**: Valor único em string: `"travel"`, `"business"`, ou `"casual"`. Campo vazio (`""`) indica "não configurado".

---

## Decision 2: Formato de armazenamento dos temas preferidos

**Decision**: String com valores separados por vírgula no campo `preferred_themes`. Ex.: `"restaurant,airport,shopping"`.

**Rationale**: O campo `preferred_themes` já existe como `String` na `UserProfileEntity`. Comma-separated é compatível com Room sem migration, legível e suficiente para uma lista pequena de IDs sem vírgulas internas.

**Alternatives considered**:
- JSON array (descartado — parsing extra sem vantagem para lista simples).
- Tabela normalizada `user_theme_junction` (descartada — over-engineering para v1 com lista estática).

---

## Decision 3: Lista de temas de conversa (v1)

**Decision**: 10 temas predefinidos, relevantes para brasileiros aprendendo inglês:

| ID | Rótulo (PT-BR) |
|----|----------------|
| `restaurant` | Restaurantes |
| `airport` | Aeroportos e viagens |
| `hotel` | Hotéis e hospedagem |
| `job_interview` | Entrevistas de emprego |
| `shopping` | Compras |
| `tourism` | Turismo e pontos turísticos |
| `health` | Saúde e consultas médicas |
| `work_meetings` | Reuniões de trabalho |
| `social` | Vida social e amizades |
| `technology` | Tecnologia e gadgets |

**Rationale**: Cobre os principais contextos de uso de inglês do público-alvo. Lista estática no app (sem servidor), consistente com a restrição de no-cloud do v1.

**Alternatives considered**: Temas carregados de API (descartado — violaria constraint offline/no-cloud do v1).

---

## Decision 4: Opções de voz da IA

**Decision**: Valores fixos conforme constitution (Princípio I):

| Campo | Opções | Padrão |
|-------|--------|--------|
| Gênero | `feminine`, `masculine` | `feminine` |
| Sotaque | `american`, `british` | `american` |
| Velocidade | `slow`, `normal`, `fast` | `normal` |

**Rationale**: Diretamente definidos na constitution. Já presentes como padrões na `UserProfileEntity` e no `SeedCallback`.

---

## Decision 5: Fluxo de onboarding (primeira configuração)

**Decision**: Adicionar `StartDestination.ProfileSetup(userName: String)` ao sealed class. O `MainViewModel` emite `ProfileSetup` quando o perfil ativo tem `learningGoals.isBlank()`.

**Rationale**: Consistente com o padrão reativo já estabelecido no `MainViewModel`. Quando o usuário salva a meta, o Room emite a atualização via Flow e `MainViewModel` re-emite `Home` automaticamente — sem callbacks explícitos.

**Alternatives considered**:
- HomeScreen verificar e auto-navegar para edição (descartado — lógica de negócio em composable).
- Flag `isOnboarding` passada via argumento de navegação (descartado — mais verboso, sem vantagem).

---

## Decision 6: Tela de edição — localização no grafo de navegação

**Decision**: Rota `"profileEdit"` dentro do NavHost do estado `Home` e também como único destino no NavHost do estado `ProfileSetup`.

**Rationale**: Reutilizar `ProfileEditScreen` nos dois contextos. No onboarding, o backstack não tem `home`, logo o botão voltar é ocultado. Nas configurações, o back normal funciona.

**Alternatives considered**: Tela dedicada apenas em Settings (descartado — não atenderia o fluxo de onboarding do FR-010).
