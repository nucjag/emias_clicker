# Быстрый старт: Android Auto-Clicker

## Что делает приложение

✅ Висит в фоне и показывает плавающую кнопку
✅ По нажатию на кнопку начинает цикл:

- Кликает по фиксированной точке экрана
- Читает текст на открывшемся экране
- Если находит слово "Бабай" → кликает кнопку + запускает будильник
- Если не находит → свайп назад
- Цикл повторяется

---

## Быстрая установка (5 минут)

### 1. Создайте проект

```
Android Studio → New Project → Empty Activity
Name: AutoClicker
Language: Kotlin
Minimum SDK: API 24
```

### 2. Скопируйте 9 файлов из полного гайда

- AndroidManifest.xml
- accessibility_service_config.xml (в res/xml/)
- strings.xml
- activity_main.xml
- floating_button.xml
- MainActivity.kt
- ClickerService.kt
- FloatingButtonService.kt
- build.gradle

### 3. Соберите и запустите

```
Build → Make Project
Run
```

### 4. Настройте разрешения

1. Нажмите "Включить Accessibility"
2. Включите AutoClicker в списке
3. Нажмите "Показать кнопку"
4. Разрешите наложение поверх других окон

### 5. Используйте

- Откройте целевое приложение
- Нажмите на плавающую кнопку
- Кликер запущен!

---

## Настройка координат кнопки

В `ClickerService.kt` найдите:

```kotlin
private const val BUTTON_X = 540f  // ← Измените X
private const val BUTTON_Y = 1800f // ← Измените Y
```

**Как узнать координаты:**

1. Настройки → Для разработчиков → Показывать касания
2. Включите "Pointer location"
3. Нажмите на нужную кнопку в приложении
4. Запишите координаты X и Y

---

## Изменить искомое слово

В `ClickerService.kt` в методе `clickCycle()`:

```kotlin
if (screenText.contains("Бабай", ignoreCase = true)) {
    // ↑ Замените "Бабай" на нужное слово
```

---

## Структура кода

### MainActivity.kt

- Главный экран приложения
- Кнопки для открытия настроек
- Запрос разрешений

### FloatingButtonService.kt

- Создаёт плавающую кнопку
- Можно перемещать по экрану
- Запускает/останавливает кликер

### ClickerService.kt (ГЛАВНЫЙ ФАЙЛ)

- Наследуется от AccessibilityService
- Выполняет клики через `dispatchGesture()`
- Читает текст через `rootInActiveWindow`
- Делает свайпы
- Запускает будильник через MediaPlayer

---

## Основные методы

### Клик по координатам

```kotlin
private fun performClick(x: Float, y: Float) {
    val clickPath = Path()
    clickPath.moveTo(x, y)
    clickPath.lineTo(x, y + 1)
    
    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(
        GestureDescription.StrokeDescription(clickPath, 0, 50)
    )
    
    dispatchGesture(gestureBuilder.build(), null, null)
}
```

### Свайп назад

```kotlin
private fun performSwipeBack() {
    val swipePath = Path()
    swipePath.moveTo(50f, middleY)
    swipePath.lineTo(width * 0.8f, middleY)
    
    val gestureBuilder = GestureDescription.Builder()
    gestureBuilder.addStroke(
        GestureDescription.StrokeDescription(swipePath, 0, 300)
    )
    
    dispatchGesture(gestureBuilder.build(), null, null)
}
```

### Чтение текста с экрана

```kotlin
private fun readScreenText(): String {
    val rootNode = rootInActiveWindow ?: return ""
    val textBuilder = StringBuilder()
    extractText(rootNode, textBuilder)
    return textBuilder.toString()
}

private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
    if (node.text != null) {
        builder.append(node.text).append(" ")
    }
    for (i in 0 until node.childCount) {
        node.getChild(i)?.let { extractText(it, builder) }
    }
}
```

### Запуск будильника

```kotlin
private fun playAlarm() {
    val alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI
    mediaPlayer = MediaPlayer.create(this, alarmUri)
    mediaPlayer?.apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
        )
        isLooping = true
        start()
    }
}
```

---

## Отладка

### Просмотр логов

```bash
adb logcat -s ClickerService
```

### Частые проблемы

**Кликер не работает:**

- ✅ Проверьте, что Accessibility включён
- ✅ Проверьте разрешение на наложение
- ✅ Убедитесь, что координаты правильные

**Не читает текст:**

- ✅ В accessibility_service_config.xml должно быть: `canRetrieveWindowContent="true"`
- ✅ Некоторые приложения блокируют Accessibility

**Будильник не звучит:**

- ✅ Проверьте громкость будильника
- ✅ Устройство не должно быть в беззвучном режиме

---

## Минимальные требования

- **Android 7.0** (API 24) или выше
- **Разрешения:**
  - Accessibility Service
  - Display over other apps (SYSTEM_ALERT_WINDOW)

---

## Следующие шаги

### Улучшения

1. Добавить UI для настройки координат
2. Добавить поле ввода для искомого слова
3. Настраиваемая задержка между циклами
4. Выбор собственного звука будильника
5. Сохранение настроек в SharedPreferences
6. Логирование действий в файл
7. Уведомления о найденном слове

---

## Итого

Вы получили полностью рабочий MVP кликера на Kotlin с:

- ✅ AccessibilityService для автоматизации
- ✅ Плавающей кнопкой для управления
- ✅ Чтением текста с экрана
- ✅ Кликами и свайпами
- ✅ Запуском будильника

**Время разработки:** 2-3 часа для MVP
**Сложность:** Средняя
**Язык:** Kotlin
**Фреймворк:** Android SDK + AccessibilityService
