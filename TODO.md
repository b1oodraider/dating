# TODO: тестовое покрытие (делаю сам)

Из ревью тестов. Все пункты — обычные тесты в стиле уже существующих,
ничего экзотического.

## 1. Юнит-тесты AuthService (`AuthServiceTest`)

Сейчас покрыты только два негативных сценария login. Добавить (моки как в
существующих тестах):

- [ ] `register`: happy path — юзер сохранён, `profileCreator.createInitialProfile`
      вызван, событие опубликовано (`verify(events).publishEvent(...)`)
- [ ] `register`: email занят (`existsByEmail` → true) → `IllegalStateException`,
      `save` НЕ вызывался
- [ ] `register`: гонка — `saveAndFlush` кидает `DataIntegrityViolationException`
      → наружу летит `IllegalStateException`
- [ ] `refresh`: happy path — старый токен отозван (`revoke`), новый сохранён,
      вернулась пара токенов
- [ ] `refresh`: неизвестный токен → `BadCredentialsException`
- [ ] `refresh`: **reuse-detection** — токен уже revoked → отозваны ВСЕ активные
      токены пользователя (`findAllByUserIdAndRevokedFalse` + `revoke` на каждом)
      и `BadCredentialsException`
- [ ] `refresh`: просроченный (не revoked) токен → `BadCredentialsException`,
      массового отзыва НЕТ
- [ ] `logout`: неизвестный токен — не падает (молча игнорирует)

Подсказка: `RefreshToken` создаётся конструктором, `isRevoked`/`isActive`
проверяются на реальном объекте — мокать сам токен не нужно.

## 2. Тесты profile-модуля

Ни одного теста нет. Минимум (интеграционные через MockMvc, по образцу
`AuthFlowIntegrationTest`, либо юнит на `ProfileService` с мок-репозиторием):

- [ ] `GET /api/profiles/me` без профиля в БД → 404 (сейчас профиль создаётся
      при регистрации, но `NotFoundException`-ветка не покрыта)
- [ ] `PUT /api/profiles/me` — happy path: поля обновились
- [ ] `PUT /api/profiles/me` с пустым `displayName` → 400
- [ ] `GET /api/profiles/{id}` с несуществующим id → 404

## 3. Тесты валидации DTO (register/login → 400)

Проверяют связку `@Valid` + `GlobalExceptionHandler.handleMethodArgumentNotValidException`:

- [ ] register с невалидным email → 400
- [ ] register с паролем короче 8 → 400
- [ ] register с пустым displayName → 400

Подсказка: это можно сделать лёгким слайс-тестом `@WebMvcTest(AuthController.class)`
с мок-`AuthService` — без Testcontainers, быстро. Но придётся замокать/отключить
security-фильтр. Если возиться неохота — добавь кейсы в `AuthFlowIntegrationTest`.

## 4. RollbackTest: проверить откат profiles

Тест проверяет откат `users` и `event_publication`, но профиль создаётся в той же
транзакции (`profileCreator.createInitialProfile`):

- [ ] добавить count по таблице `profiles` до/после — по аналогии с `users`
