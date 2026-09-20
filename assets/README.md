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
```

Требования к финальным изображениям:

- PNG с корректной сигнатурой файла;
- квадрат 1024×1024;
- прозрачный или цельный фон;
- без текста внутри изображения;
- одинаковая композиция персонажа на всех трёх уровнях.

Если конкретный файл отсутствует или повреждён, используется `common/fallback.png`. Если отсутствует и он, бот отправляет текстовую карточку питомца и продолжает сценарий.

Исходные отдельные файлы перенесены в каталоги сценариев и больше не дублируются в корне. `all-pets.png` остаётся общим визуальным референсом.
