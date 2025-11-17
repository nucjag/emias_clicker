# Android Auto-Clicker на Kotlin с AccessibilityService

## Описание проекта

Это приложение-кликер для Android, которое:

- Работает в фоне с доступом к Accessibility API
- Показывает висячую кнопку (floating button) для запуска/остановки
- Автоматически кликает по кнопке, читает экран
- Если находит слово "Бабай" — запускает будильник
- Если не находит — делает свайп назад

---

## 1. Создание проекта в Android Studio

1. Откройте Android Studio
2. **File → New → New Project**
3. Выберите **Empty Activity**
4. Укажите:
   - **Name:** AutoClicker
   - **Package name:** com.example.autoclicker
   - **Language:** Kotlin
   - **Minimum SDK:** API 24 (Android 7.0)
5. Нажмите **Finish**

---

## 2. Структура файлов проекта

```
app/
├── src/main/
│   ├── java/com/example/autoclicker/
│   │   ├── MainActivity.kt
│   │   ├── ClickerService.kt
│   │   └── FloatingButtonService.kt
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── floating_button.xml
│   │   ├── xml/
│   │   │   └── accessibility_service_config.xml
│   │   └── values/
│   │       └── strings.xml
│   └── AndroidManifest.xml
└── build.gradle
```

---

## 3. Код файлов

### 3.1 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.autoclicker">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Accessibility Service -->
        <service
            android:name=".ClickerService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Floating Button Service -->
        <service
            android:name=".FloatingButtonService"
            android:enabled="true"
            android:exported="false" />

    </application>

</manifest>
```

---

### 3.2 res/xml/accessibility_service_config.xml

Создайте папку `res/xml/` (если её нет), затем создайте файл:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

---

### 3.3 res/values/strings.xml

```xml
<resources>
    <string name="app_name">AutoClicker</string>
    <string name="accessibility_service_description">Сервис автоматического кликера для поиска слова на экране</string>
    <string name="start_clicker">Запустить кликер</string>
    <string name="stop_clicker">Остановить кликер</string>
</resources>
```

---

### 3.4 res/layout/activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Auto Clicker"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="24dp"/>

    <Button
        android:id="@+id/btnEnableAccessibility"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Включить Accessibility"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/btnShowFloatingButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Показать кнопку"
        android:layout_marginBottom="16dp"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Инструкция:\n1. Включите Accessibility Service\n2. Разрешите наложение поверх других окон\n3. Нажмите висячую кнопку для старта"
        android:textAlignment="center"
        android:layout_marginTop="24dp"/>

</LinearLayout>
```

---

### 3.5 res/layout/floating_button.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

    <ImageButton
        android:id="@+id/floatingButton"
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:background="@android:drawable/ic_menu_mylocation"
        android:contentDescription="Floating Button"
        android:elevation="8dp" />

</FrameLayout>
```

---

### 3.6 MainActivity.kt

```kotlin
package com.example.autoclicker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnableAccessibility: Button = findViewById(R.id.btnEnableAccessibility)
        val btnShowFloatingButton: Button = findViewById(R.id.btnShowFloatingButton)

        // Открыть настройки Accessibility
        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // Показать висячую кнопку
        btnShowFloatingButton.setOnClickListener {
            if (checkOverlayPermission()) {
                startFloatingButtonService()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 100)
        }
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
        Toast.makeText(this, "Висячая кнопка запущена", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (checkOverlayPermission()) {
                startFloatingButtonService()
            } else {
                Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

---

### 3.7 FloatingButtonService.kt

```kotlin
package com.example.autoclicker

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var isClickerRunning = false

    companion object {
        var instance: FloatingButtonService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        // Параметры окна
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        windowManager.addView(floatingView, params)

        // Обработка нажатия на кнопку
        val floatingButton: ImageButton = floatingView.findViewById(R.id.floatingButton)
        
        floatingButton.setOnClickListener {
            isClickerRunning = !isClickerRunning
            if (isClickerRunning) {
                // Запускаем кликер
                ClickerService.instance?.startClicking()
                floatingButton.setBackgroundResource(android.R.drawable.ic_media_pause)
            } else {
                // Останавливаем кликер
                ClickerService.instance?.stopClicking()
                floatingButton.setBackgroundResource(android.R.drawable.ic_menu_mylocation)
            }
        }

        // Перемещение кнопки
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

### 3.8 ClickerService.kt

```kotlin
package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickerService : AccessibilityService() {

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        var instance: ClickerService? = null
        private const val TAG = "ClickerService"
        
        // Координаты кнопки внизу экрана (настроить под ваше приложение)
        private const val BUTTON_X = 540f
        private const val BUTTON_Y = 1800f
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Можно обрабатывать события, если нужно
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    fun startClicking() {
        isRunning = true
        clickCycle()
    }

    fun stopClicking() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        stopAlarm()
    }

    private fun clickCycle() {
        if (!isRunning) return

        // Шаг 1: Кликаем по кнопке внизу
        performClick(BUTTON_X, BUTTON_Y)

        // Шаг 2: Ждём открытия экрана и читаем текст
        handler.postDelayed({
            val screenText = readScreenText()
            Log.d(TAG, "Screen text: $screenText")

            if (screenText.contains("Бабай", ignoreCase = true)) {
                // Слово найдено - кликаем по кнопке и запускаем будильник
                performClick(BUTTON_X, BUTTON_Y)
                playAlarm()
            } else {
                // Слово не найдено - свайп назад
                performSwipeBack()
            }

            // Повторяем цикл
            handler.postDelayed({
                clickCycle()
            }, 1000)

        }, 500)
    }

    private fun performClick(x: Float, y: Float) {
        val clickPath = Path()
        clickPath.moveTo(x, y)
        clickPath.lineTo(x, y + 1)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, 50))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Click at ($x, $y) completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Click cancelled")
            }
        }, null)
    }

    private fun performSwipeBack() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val startX = 50f
        val endX = width * 0.8f
        val middleY = height / 2f

        val swipePath = Path()
        swipePath.moveTo(startX, middleY)
        swipePath.lineTo(endX, middleY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, 300))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Swipe back completed")
            }
        }, null)
    }

    private fun readScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        extractText(rootNode, textBuilder)
        rootNode.recycle()
        return textBuilder.toString()
    }

    private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (node.text != null) {
            builder.append(node.text).append(" ")
        }
        if (node.contentDescription != null) {
            builder.append(node.contentDescription).append(" ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                extractText(it, builder)
                it.recycle()
            }
        }
    }

    private fun playAlarm() {
        try {
            mediaPlayer?.release()
            
            // Используем стандартный звук будильника
            val alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI
            
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                start()
            }
            
            Log.d(TAG, "Alarm started")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm", e)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClicking()
        instance = null
    }
}
```

---

### 3.9 build.gradle (app level)

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.autoclicker'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.autoclicker"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
}
```

---

## 4. Запуск и тестирование

### Шаг 1: Соберите проект

1. В Android Studio нажмите **Build → Make Project**
2. Подключите Android-устройство или запустите эмулятор
3. Нажмите **Run**

### Шаг 2: Включите разрешения

1. Откройте приложение
2. Нажмите **"Включить Accessibility"**
3. В настройках найдите **AutoClicker** и включите его
4. Вернитесь в приложение
5. Нажмите **"Показать кнопку"**
6. Разрешите наложение поверх других окон

### Шаг 3: Использование

1. Откройте целевое приложение
2. Нажмите на висячую кнопку — начнётся цикл кликов
3. Кликер будет искать слово "Бабай" на экране
4. При нахождении — запустит будильник
5. Если не найдёт — сделает свайп назад

---

## 5. Настройка под ваше приложение

### Изменить координаты кнопки

В файле `ClickerService.kt` измените константы:

```kotlin
private const val BUTTON_X = 540f  // X координата кнопки
private const val BUTTON_Y = 1800f // Y координата кнопки
```

**Как узнать координаты:**

1. Включите **Developer Options → Pointer Location**
2. Нажмите на нужную кнопку
3. Запишите координаты X и Y

### Изменить искомое слово

В методе `clickCycle()` измените:

```kotlin
if (screenText.contains("Бабай", ignoreCase = true)) {
```

на ваше слово.

---

## 6. Отладка

### Просмотр логов

В Android Studio откройте **Logcat** и фильтруйте по тегу `ClickerService`:

```
adb logcat -s ClickerService
```

### Проблемы и решения

**Кликер не работает:**

- Убедитесь, что Accessibility Service включён
- Проверьте разрешение на наложение
- Проверьте координаты кнопки

**Текст не читается:**

- Убедитесь, что в `accessibility_service_config.xml` установлен `canRetrieveWindowContent="true"`
- Проверьте, что целевое приложение не блокирует Accessibility

**Будильник не звучит:**

- Проверьте уровень громкости будильника на устройстве
- Убедитесь, что устройство не в беззвучном режиме

---

## 7. Дополнительные возможности

### Добавить свой звук будильника

1. Поместите файл `alarm.mp3` в `res/raw/`
2. Измените в `playAlarm()`:

```kotlin
mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
```

### Настраиваемые параметры

Можно добавить в UI:

- Поле для ввода координат
- Поле для ввода искомого слова
- Задержка между циклами
- Выбор звука будильника

---

## Готово

Теперь у вас есть полностью рабочее приложение-кликер на Kotlin с AccessibilityService. Просто скопируйте код в соответствующие файлы в Android Studio и запустите проект.
