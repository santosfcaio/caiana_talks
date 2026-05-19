# Quickstart: Profile Personalization

## Como testar a feature

### Pré-requisito
Feature 001 (seleção de perfil) deve estar funcional. O app já deve apresentar a tela de seleção de "Caio" ou "Ana" no primeiro launch.

### Fluxo de onboarding (primeira configuração)
1. Limpe os dados do app (`adb shell pm clear com.caiana.talks`) ou desinstale/reinstale.
2. Abra o app → tela de seleção de perfil.
3. Toque em "Caio".
4. **Esperado**: App navega diretamente para a tela de configurações de perfil (onboarding), pois `learning_goals` está em branco.
5. Selecione a meta "Negócios".
6. Selecione 2 temas (ex.: "Reuniões de trabalho", "Tecnologia e gadgets").
7. Configure voz: masculino, americano, rápido.
8. Toque em "Salvar".
9. **Esperado**: App navega para a HomeScreen com "Olá, Caio!". Não há botão Voltar — a tela de edição saiu do backstack.

### Fluxo de edição posterior (via Settings)
1. Na HomeScreen, toque no ícone de configurações (⚙️).
2. Em Configurações, toque em "Editar preferências".
3. **Esperado**: Tela de edição exibe as preferências salvas anteriormente.
4. Altere a velocidade para "Lento".
5. Toque em "Salvar".
6. **Esperado**: Volta para Configurações. Ao reabrir "Editar preferências", a velocidade mostra "Lento".

### Verificação de persistência
1. Feche o app completamente (remova da lista de recentes).
2. Reabra o app.
3. **Esperado**: App vai direto para HomeScreen (meta já está configurada) sem mostrar a tela de onboarding novamente.

### Verificação de isolamento de perfis
1. Em Configurações, toque em "Trocar perfil".
2. Selecione "Ana".
3. **Esperado**: Tela de onboarding aparece (Ana ainda não tem meta configurada).
4. Configure metas diferentes das de Caio.
5. Volte para o perfil de Caio.
6. **Esperado**: Preferências de Caio estão intactas e diferentes das de Ana.

## Comandos úteis

```bash
# Verificar conteúdo do banco Room no emulador
adb shell
run-as com.caiana.talks
sqlite3 databases/caiana_talks_db
SELECT id, name, learning_goals, preferred_themes, ai_voice_gender, ai_speech_rate FROM user_profiles;

# Reset completo do app
adb shell pm clear com.caiana.talks

# Rodar testes unitários
./gradlew :app:testDebugUnitTest

# Build debug
./gradlew :app:assembleDebug
```
