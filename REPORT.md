# Social network: разбор проекта

Вариант 16. Программа на Java 17 моделирует публикации, реакции и проверку жалоб. Observer передает события слушателям, Visitor выполняет операции над деревом контента.

## Теория: где и почему

Observer реализован интерфейсами Observer<T> и Subject<T>. EventHub хранит подписчиков по типам событий. PostSource публикует POSTED и REPORTED, ReactionSource публикует REACTED. Источникам не нужно знать классы слушателей. Добавление слушателя не требует переписывать источник.

Используется push: обработчик получает готовый неизменяемый Event. Ему не нужно отдельно спрашивать источник о текущем состоянии. EngagementStatistics одновременно слушает реакции и публикует SUMMARY. TrendingTracker получает эти сводки. Это цепочка наблюдателей.

Composite представлен деревом Community -> Group -> Post/Comment/Image/Video. Есть общий Content, два типа контейнеров и четыре типа листьев. Контейнеры используют List.copyOf, поэтому дерево нельзя менять через внешний список.

Visitor<R> содержит перегрузку visit для каждого типа элемента. Вызов content.accept(visitor) сначала выбирает accept конкретного элемента, затем visit(this) передает его конкретный тип посетителю. Это double dispatch. Прямой visitor.visit(content) не скомпилируется: перегрузки для общего Content нет. Обход находится в посетителях, а элементы содержат данные и accept.

EngagementVisitor суммирует лайки. StatisticsVisitor накапливает счетчик всех посещенных узлов. FilterVisitor получает условие через конструктор и возвращает список подходящих элементов. JsonVisitor вручную формирует вложенный JSON и экранирует специальные символы.

ModerationService получает REPORTED, находит соответствующую группу и запускает FilterVisitor только на ней. Идентификаторы найденного контента и количество нарушений отправляются новым событием REVIEWED в ReviewLog.

Main создает объекты и связывает зависимости: это composition root. Сервисы получают зависимости через конструкторы. ProducerRunner получает ExecutorService, завершает его и проверяет результаты задач. EventHub использует CopyOnWriteArrayList, агрегатор и тренды — ConcurrentHashMap, журналы — ConcurrentLinkedQueue. Stateful StatisticsVisitor создается для одного обхода и не разделяется потоками.

При отписке во время рассылки уже полученный снимок списка остается прежним. Поэтому обработчик может получить уже отправляемое событие, но его не будет в последующих снимках. Обработчик может сам подписывать и отписывать слушателей без ConcurrentModificationException.

## Соответствие требованиям

| Требование | Реализация |
|---|---|
| 2 источника | PostSource, ReactionSource и отдельные EventHub |
| Не менее 5 слушателей | Feed, NotificationCenter, EngagementStatistics, TrendingTracker, ModerationService, ReviewLog |
| Собственные generic-интерфейсы | Observer<T>, Subject<T>, Visitor<R> |
| Неизменяемые события с типом | record Event, enum EventType |
| Подписка по типу события | EventHub.subscribe(type, observer) |
| Подписка и отписка в обработчике | EventHub и соответствующие JUnit-тесты |
| Цепочка Observer | ReactionSource -> EngagementStatistics -> TrendingTracker |
| 3 уровня, 2 контейнера, 4 листа | Community -> Group -> Post/Comment/Image/Video |
| Double dispatch | accept(visitor) -> visit(this) |
| Числовой Visitor | EngagementVisitor |
| Visitor с накоплением состояния | StatisticsVisitor.total |
| Фильтр, возвращающий список | FilterVisitor |
| Вложенный JSON | JsonVisitor |
| Событие запускает Visitor на поддереве | ModerationService проверяет одну Group |
| Результат возвращается событием | REVIEWED -> ReviewLog |
| Не менее 3 producer threads | Main, аргумент threads, Executors.newFixedThreadPool |
| Настраиваемое количество потоков | java -cp target/classes social.Main 4 |
| Потокобезопасность без общей блокировки | ConcurrentHashMap, CopyOnWriteArrayList, ConcurrentLinkedQueue |
| Корректное завершение | ProducerRunner: shutdown, awaitTermination, Future.get |
| Composition root и constructor injection | Main, конструкторы сервисов |
| Два отдельных trade-off-коммита | 324a8d9 и 205e9bb |
| Не менее 15 JUnit-тестов | 24 теста в SocialNetworkTest |
| Стресс-тест 4 × 1000 | stressFourProducersPublishExactlyFourThousandEvents |
| Ручные ожидаемые значения | 24 лайка, 8 узлов, один комментарий c1 |
| README и UML | README.md |

## Проверка

Все 24 теста прошли на JUnit 5.11.4 с Java 17. При четырех потоках Main выводит Feed=400, Notifications=800, Live reactions=400, Trending score=400. Состояние дерева — исходный неизменяемый снимок: лайв-события обновляют слушателей, а не этот снимок. Поэтому 24 лайка в дереве и 400 новых реакций — разные показатели.

Нормальное завершение обрабатывает все принятые задачи. Ошибка обработчика или производителя видна вызывающему коду; аварийное прерывание/таймаут не выдается за успешное завершение без потерь.

## Что остается для сдачи

Проверить, что вариант 16 свободен в группе. Загрузить проект в свой GitHub и отправить ссылку вместе с отчетом в LMS. История содержит настоящие коммиты за текущую работу, но не заменяет требуемые две недели последовательной работы. Защита и live coding составляют 50 баллов и выполняются студентом.
