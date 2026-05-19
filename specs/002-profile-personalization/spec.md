# Feature Specification: Profile Personalization

**Feature Branch**: `002-profile-personalization`

**Created**: 2026-05-19

**Status**: Draft

**Input**: User description: "Criar a feature 'profile personalization', em que o usuario irá configurar suas preferências. Ela está bem descrita no constitution.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Definir Metas de Aprendizado (Priority: P1)

O usuário acessa as configurações do seu perfil e escolhe qual é o seu principal objetivo de aprendizado de inglês: viagem, negócios ou conversa casual. Essa meta orienta o tom e o vocabulário das sessões de conversa da IA.

**Why this priority**: É a informação mais impactante para a qualidade pedagógica das sessões. Sem ela, a IA não consegue adaptar o conteúdo ao contexto real do usuário.

**Independent Test**: Pode ser testado de forma isolada abrindo a tela de perfil, selecionando uma meta e verificando que ela é salva e exibida corretamente.

**Acceptance Scenarios**:

1. **Given** o usuário está na tela de configurações do perfil, **When** ele seleciona "Viagem" como meta de aprendizado, **Then** a meta é salva no perfil e exibida como ativa na próxima abertura da tela.
2. **Given** o usuário já possui uma meta salva, **When** ele troca a meta para "Negócios", **Then** a nova meta substitui a anterior e fica persistida.
3. **Given** o usuário não selecionou nenhuma meta ainda, **When** abre a tela de configurações, **Then** nenhuma opção aparece pré-selecionada e o campo é obrigatório para avançar.

---

### User Story 2 - Configurar Temas de Conversa Preferidos (Priority: P2)

O usuário escolhe temas de conversa que deseja praticar nas próximas sessões (ex.: restaurantes, viagens de avião, entrevistas de emprego, compras). Esses temas são sugeridos pela IA ao iniciar uma nova sessão.

**Why this priority**: Permite personalização das sessões sem exigir que o usuário configure algo a cada conversa. Aumenta o engajamento com conteúdo relevante.

**Independent Test**: Pode ser testado selecionando temas na tela de perfil e verificando que eles ficam salvos, independentemente do restante do fluxo de conversa.

**Acceptance Scenarios**:

1. **Given** o usuário está na tela de configurações do perfil, **When** ele seleciona dois ou mais temas de conversa, **Then** todos os temas selecionados são salvos e exibidos como ativos.
2. **Given** o usuário já tem temas salvos, **When** ele desmarca um tema, **Then** o tema é removido da lista de preferências.
3. **Given** nenhum tema está selecionado, **When** o usuário salva o perfil, **Then** o sistema aceita o perfil sem temas obrigatórios (temas são opcionais).

---

### User Story 3 - Configurar Voz da IA (Priority: P3)

O usuário configura as preferências de voz que a IA usará para responder durante as sessões: gênero (feminino ou masculino), sotaque (inglês americano acessível ao brasileiro ou inglês britânico) e velocidade de fala (lenta, normal ou rápida).

**Why this priority**: Melhora a experiência de imersão e acessibilidade. Usuários iniciantes precisam de velocidade lenta; o gênero e sotaque são preferência pessoal.

**Independent Test**: Pode ser testado salvando as preferências de voz e verificando que elas ficam persistidas no perfil, independentemente de qualquer sessão de conversa.

**Acceptance Scenarios**:

1. **Given** o usuário está na tela de configurações do perfil, **When** ele seleciona voz feminina, sotaque americano e velocidade lenta, **Then** todas as três preferências são salvas e exibidas corretamente na tela.
2. **Given** preferências de voz já foram salvas, **When** o usuário altera apenas a velocidade para "rápida", **Then** somente a velocidade é atualizada; gênero e sotaque permanecem inalterados.
3. **Given** o usuário nunca configurou preferências de voz, **When** abre a tela pela primeira vez, **Then** as opções padrão são: voz feminina, sotaque americano, velocidade normal.

---

### Edge Cases

- O que acontece se o usuário fechar o app durante a edição do perfil sem salvar? As alterações não concluídas devem ser descartadas.
- O que acontece se o usuário tentar salvar um perfil sem selecionar uma meta de aprendizado? O sistema deve bloquear o salvamento e indicar que o campo é obrigatório.
- O que acontece se os dados do perfil no armazenamento local estiverem corrompidos? O sistema deve exibir valores padrão e permitir que o usuário reconfigure.
- O que acontece quando dois perfis diferentes (ex.: João e Maria, do modo dual-speaker) têm preferências de voz distintas? Cada perfil deve manter suas próprias preferências isoladas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE permitir que cada perfil de usuário configure independentemente sua meta de aprendizado (viagem, negócios, conversa casual).
- **FR-002**: O sistema DEVE permitir que o usuário selecione zero ou mais temas de conversa preferidos a partir de uma lista pré-definida.
- **FR-003**: O sistema DEVE permitir que o usuário configure o gênero da voz da IA (feminino ou masculino).
- **FR-004**: O sistema DEVE permitir que o usuário configure o sotaque da voz da IA (inglês americano acessível ao brasileiro ou inglês britânico).
- **FR-005**: O sistema DEVE permitir que o usuário configure a velocidade da voz da IA (lenta, normal ou rápida).
- **FR-006**: O sistema DEVE persistir todas as preferências do perfil localmente no dispositivo.
- **FR-007**: O sistema DEVE aplicar as preferências salvas automaticamente ao iniciar uma nova sessão de conversa, sem exigir reconfiguração.
- **FR-008**: A tela de configurações de perfil DEVE ser exibida inteiramente em português brasileiro.
- **FR-009**: O sistema DEVE exibir valores padrão (voz feminina, sotaque americano, velocidade normal) para usuários que ainda não configuraram preferências de voz.
- **FR-010**: O sistema DEVE exigir que a meta de aprendizado seja selecionada antes de permitir o uso das funcionalidades de conversa.
- **FR-011**: Cada perfil de usuário DEVE ter suas preferências armazenadas de forma isolada dos demais perfis cadastrados no dispositivo.

### Key Entities

- **UserProfile**: Perfil de um usuário, identificado pelo nome. Contém: meta de aprendizado, lista de temas preferidos e preferências de voz.
- **LearningGoal**: Enumeração com os valores possíveis para meta de aprendizado (viagem, negócios, conversa casual).
- **ConversationTheme**: Tema de conversa disponível para seleção (ex.: restaurantes, aeroportos, entrevistas de emprego). Um perfil pode ter múltiplos temas.
- **VoicePreference**: Conjunto de configurações de voz do perfil: gênero, sotaque e velocidade de fala.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário consegue configurar completamente suas preferências (meta, temas e voz) em menos de 2 minutos na primeira utilização.
- **SC-002**: 100% das preferências salvas são recuperadas corretamente após fechar e reabrir o aplicativo.
- **SC-003**: Todos os textos e rótulos da tela de configurações de perfil estão em português brasileiro, sem nenhum texto em inglês visível ao usuário.
- **SC-004**: As preferências de dois perfis distintos no mesmo dispositivo nunca se sobrepõem ou interferem entre si.
- **SC-005**: O fluxo de configuração do perfil pode ser completado sem nenhuma instrução externa, com taxa de conclusão acima de 90% em primeiro uso.

## Assumptions

- O usuário já está identificado por um perfil selecionado na tela de seleção de usuário (feature 001). A tela de personalização é acessada a partir desse perfil.
- A lista de temas de conversa é definida estaticamente no aplicativo (não carregada de servidor externo) no v1.
- As opções de meta de aprendizado são fixas: viagem, negócios e conversa casual. Não há opção de meta personalizada no v1.
- O armazenamento é exclusivamente local; nenhuma sincronização com nuvem ocorre nesta feature.
- A tela de configurações de perfil é acessível tanto no primeiro uso (onboarding) quanto em qualquer momento posterior pelo menu do perfil.
- A validação de que a meta de aprendizado é obrigatória ocorre apenas ao tentar iniciar uma sessão de conversa, não ao salvar o perfil.
