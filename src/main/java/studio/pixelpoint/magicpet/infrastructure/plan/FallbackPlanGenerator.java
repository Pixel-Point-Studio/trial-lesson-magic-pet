package studio.pixelpoint.magicpet.infrastructure.plan;

import studio.pixelpoint.magicpet.application.port.PlanGenerator;
import studio.pixelpoint.magicpet.domain.GeneratedPlan;
import studio.pixelpoint.magicpet.domain.PlanTask;
import studio.pixelpoint.magicpet.domain.Scenario;

import java.util.List;

public final class FallbackPlanGenerator implements PlanGenerator {
    @Override
    public GeneratedPlan generate(Scenario scenario, String goalText) {
        return switch (scenario) {
            case STUDY -> new GeneratedPlan("Три маленьких шага к цели «" + goalText + "»", List.of(
                    task("Сформулируй результат", "Запиши одним предложением, что именно ты хочешь уметь."),
                    task("Сделай первый подход", "Удели цели две минуты: прочитай, реши или повтори один небольшой фрагмент."),
                    task("Назначь продолжение", "Выбери время для следующего короткого занятия и добавь его в календарь.")));
            case SPORT -> new GeneratedPlan("Безопасный старт к цели «" + goalText + "»", List.of(
                    task("Проверь самочувствие", "Спокойно оцени своё состояние и подготовь воду; при дискомфорте остановись."),
                    task("Мягко разомнись", "Сделай две минуты комфортных движений без боли и без предельной нагрузки."),
                    task("Запланируй тренировку", "Выбери реалистичное время для следующей безопасной активности.")));
            case BLOG -> new GeneratedPlan("Творческий маршрут к цели «" + goalText + "»", List.of(
                    task("Найди тему", "Запиши одну мысль, которой тебе действительно хочется поделиться."),
                    task("Создай набросок", "Придумай заголовок и первые два предложения будущей публикации."),
                    task("Подготовь публикацию", "Выбери формат и время, когда закончишь и опубликуешь материал.")));
        };
    }

    private static PlanTask task(String title, String description) {
        return new PlanTask(title, description);
    }
}
