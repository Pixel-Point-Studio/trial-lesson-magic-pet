# Ответы МОПа — Walkthrough 10

Не показывать файл ученику. Для каждой задачи сначала дать человеческую подсказку, затем указать блок `ISSUE`, и только в точке спасения назвать строку решения.

## S1 — другой Pet

- Исправление: `Scenario.SPORT` → `Scenario.STUDY` в `StudyLesson.select`.
- Объяснение: аргумент сообщает готовому действию, какой путь сохранить.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.studyWrongPet'`.
- Спасение: попросить сравнить значение с методом `scenario()` в начале файла.

## S2 — имя не сохраняется

- Исправление: `pet.namePet(user, "Руни")` → `pet.namePet(user, name)`.
- Объяснение: `name` содержит текст пользователя, фиксированная строка — нет.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.studySavesCustomName'`.
- Спасение: показать параметр `String name`, не диктуя всю строку.

## S3 — «Мой Pet»

- Исправление: добавить `new Button("action:show_pet", "🐾 Мой Pet")` и условие с `pet.showPet(user)`.
- Объяснение: внутреннее имя связывает нажатие с действием.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.studyShowsPet'`.
- Спасение: дать скопировать форму кнопки из `SportLesson`.

## S4 — переименование

- Исправление: кнопка `action:rename_pet`; при нажатии `beginPetRename`; в `WAITING_PET_RENAME` — `renamePet(user, message.text())`.
- Объяснение: первое действие просит текст, второе сохраняет следующий ответ.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.studyRenamesPet'`.
- Спасение: показать три готовых имени методов и попросить соединить их.

## I1 — уровень на 100 XP

- Исправление: `experience > 100` → `experience >= 100`.
- Объяснение: граница 100 должна входить в новый уровень.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.sportLevelsAtOneHundred'`.
- Спасение: спросить, истинно ли `100 > 100`.

## I2 — старая картинка

- Исправление: `pet.showPet(refreshed, 1)` → `pet.showPet(refreshed, result.level())`.
- Объяснение: фиксированное число всегда выбирает первую картинку.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.sportShowsCurrentLevelImage'`.
- Спасение: показать `result.level()` в соседнем сообщении повышения.

## I3 — подсказка

- Исправление: кнопка `action:hint` и условие с `pet.showHint(user)`.
- Объяснение: готовый метод выбирает подсказку, не меняя задачу и XP.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.sportShowsHint'`.
- Спасение: использовать структуру I4 как образец.

## I4 — прогресс уровня

- Исправление: кнопка `action:level_progress` и условие с `pet.showLevelProgress(user)`.
- Объяснение: вычисление уже скрыто в готовом продуктовом действии.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.sportShowsLevelProgress'`.
- Спасение: использовать структуру I3 как образец.

## B1 — неправильный список

- Исправление: `pet.showTasks(user, true)` → `pet.showTasks(user, false)`.
- Объяснение: параметр отвечает на вопрос «показывать выполненные?».
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.blogShowsActiveTasks'`.
- Спасение: сравнить с обработчиком `action:completed_tasks` в `StudentBot`.

## B2 — удаление начисляет XP

- Исправление: `completeTask(...)` → `deleteTask(user, taskId)`.
- Объяснение: завершение награждает, удаление только убирает запись.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.blogDeletesWithoutXp'`.
- Спасение: попросить прочитать названия двух методов вслух.

## B3 — подтверждение

- Исправление: прямой `deleteTask` → `confirmTaskDeletion(user, taskId)` для `task:confirm_delete:`.
- Объяснение: первое нажатие только показывает выбор; ID сохраняется внутри внутренних имён кнопок.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.blogConfirmsDeletion'`.
- Спасение: показать готовый метод фасада.

## B4 — весь план

- Исправление: кнопка `action:show_plan` и условие с `pet.showPlan(user)`.
- Объяснение: метод читает сохранённый план, но ничего не меняет.
- Проверка: `./gradlew test --tests '*LessonRouteContractTest.blogShowsFullPlan'`.
- Спасение: дать скопировать форму кнопки из `StudyLesson`.
