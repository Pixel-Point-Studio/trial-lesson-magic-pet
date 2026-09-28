# Ассеты Magic Pet

Каталог использует фиксированные пути:

```text
assets/study/level-1.png
assets/study/level-2.png
assets/study/level-3.png
assets/sport/level-1.png
assets/sport/level-2.png
assets/sport/level-3.png
assets/blog/level-1.png
assets/blog/level-2.png
assets/blog/level-3.png
assets/common/fallback.png
assets/bot/cover.png
assets/bot/avatar.png
```

Требования к финальным изображениям:

- PNG с корректной сигнатурой файла;
- квадрат 1024×1024;
- прозрачный или цельный фон;
- без текста внутри изображения;
- одинаковая композиция персонажа на всех трёх уровнях.

Дополнительные изображения Telegram:

- `bot/cover.png` — обложка блока «Что умеет этот бот?», ровно 640×360;
- `bot/avatar.png` — квадратная эмблема с безопасной центральной композицией для круглого кадрирования.

Если конкретный файл отсутствует или повреждён, используется `common/fallback.png`. Если отсутствует и он, бот отправляет текстовую карточку питомца и продолжает сценарий.

`all-pets.png` дублирует актуальную общую обложку. Предыдущий детский комплект сохранён в `legacy-cute/` и приложением не используется. Единый стиль и промпт описаны в `ART-DIRECTION.md`.
