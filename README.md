# ⚡ Google Antigravity Mobile (Android Agentic IDE & APK Builder)

Аналог агентской среды **Google Antigravity**, работающий автономно на мобильных устройствах Android. Позволяет модели **Google Gemini** (Gemini 2.5 Flash / Pro, Gemini 1.5) прямо на смартфоне:
1. Получать ваши текстовые запросы («собери приложение с таймером и темной темой»).
2. Исследовать и редактировать файловую структуру проекта (`AndroidManifest.xml`, разметку `res/layout`, Java/Kotlin исходники).
3. **Собирать нативный APK прямо на устройстве** без ПК и без тяжелого Gradle.
4. При возникновении ошибок компиляции (AAPT2, ECJ, D8) читать трассировку ошибок, исправлять код и повторять сборку.
5. Запускать установку готового APK в систему в один клик.

---

## 🏗 Как устроен On-Device Build (сборка APK на телефоне)

Обычный Android Studio Gradle требует 8–16 ГБ ОЗУ и гигабайты кэша, что не подходит для автономной работы на телефоне. 
В **Antigravity Mobile** используется легковесный нативный конвейер (аналогично AIDE / AndroidIDE / Termux toolchain), который собирает APK за **1.5–3 секунды**:

```
 [ AndroidManifest.xml + res/ ] ──> AAPT2 / AAPT (Компиляция ресурсов & R.java)
                                           │
 [ *.java + R.java ] ──────────────> ECJ / javac (Компиляция в .class)
                                           │
 [ *.class ] ──────────────────────> D8 / DX (Трансляция в classes.dex)
                                           │
 [ classes.dex + resources.apk ] ──> Zip Packager
                                           │
 [ app-unsigned.apk ] ─────────────> apksigner (Подпись debug-ключом)
                                           │
                                     [ app-debug.apk ] ──> PackageInstaller (Установка!)
```

---

## 📦 Варианты использования

Решение предоставлено в двух форматах:

### 1. Нативное Android-приложение (`/app`)
Полноценное приложение на **Kotlin + Jetpack Compose** в стиле Google Antigravity / DeepMind:
- **Вкладка Agent**: Чат с Gemini, карточки вызова инструментов (`view_file`, `write_to_file`, `replace_file_content`, `build_apk`), индикаторы шагов и статусов.
- **Вкладка Workspace**: Инспектор файлов проекта и просмотрщик кода.
- **Вкладка Build & Run**: Логи компилятора в реальном времени, кнопки ручной пересборки и установки готового APK.
- **Вкладка Settings**: Выбор модели (`gemini-2.5-flash`, `gemini-2.5-pro`) и ввод API ключа.

### 2. Standalone CLI для Termux / Android Shell (`/cli`)
Автономный скрипт на Python с полным агентским циклом, готовый к запуску в Termux прямо сейчас.

---

## 🚀 Быстрый старт через Termux (вариант CLI)

1. Установите [Termux](https://github.com/termux/termux-app/releases) на телефон.
2. Скопируйте папку `cli` в Termux или склонируйте проект.
3. Запустите скрипт автоматической установки окружения:
   ```bash
   bash cli/setup_termux.sh
   ```
   *Скрипт установит `openjdk-17`, `python`, `aapt`, `ecj`, `dx`, `apksigner` и настроит `android.jar`.*
4. Экспортируйте ваш ключ Gemini:
   ```bash
   export GEMINI_API_KEY="ваш-ключ-от-google-ai-studio"
   ```
5. Запустите агента:
   ```bash
   python3 cli/antigravity_cli.py
   ```
6. Дайте команду агенту:
   ```text
   Antigravity-Android > Сделай приложение-секундомер с красивым интерфейсом и собери APK
   ```
   Агент автоматически:
   - Создаст `AndroidManifest.xml`, `res/layout/activity_main.xml`, `MainActivity.java`.
   - Запустит `build_apk`.
   - Если возникнет ошибка компиляции, исправит ее через `replace_file_content`.
   - Вызовет `install_apk`, открыв системное окно установки Android!

---

## 📱 Сборка нативного Android приложения

Проект настроен для сборки в Android Studio или через Gradle:
- **Язык**: Kotlin 2.0 + Jetpack Compose
- **SDK**: compileSdk 35, minSdk 26
- **Сетевой стек**: OkHttp + нативный Gemini 2.5 REST API
- **Интеграция с системой**: `FileProvider` для безопасной передачи APK в Android PackageInstaller (`Intent.ACTION_VIEW`).

---

## 🔑 Авторизация через Google Аккаунт (OAuth 2.0)
Приложение поддерживает работу как через стандартный API Key, так и через **Google OAuth 2.0 (Bearer token)**:
1. Запросы отправляются с заголовком:
   `Authorization: Bearer <google_oauth_token>`
   к официальному эндпоинту `https://generativelanguage.googleapis.com/v1beta/models/...`.
2. Это позволяет не зависеть от ручного ввода API ключа и ретранслировать запросы от имени вашего аккаунта Google.

---

## ⚡ Выполнение команд через слеш `/` и управление GitHub

В системный промпт агента внедрена инструкция:
> *«ты можешь управлять файловой системой, выполнять команды, и т.д. но для выполнения команд ставь /, кстати у тебя есть тоже самое, ты можешь управлять моим гитхабом через гит»*

Оболочка автоматически распознает строки, начинающиеся со знака `/`, и выполняет их в системе:
- `/git status` — проверка состояния репозитория.
- `/git add .` и `/git commit -m "feat: new screen"` — создание коммитов.
- `/git push origin main` — отправка изменений прямо в ваш репозиторий GitHub.
- `/build` — вызов нативного компилятора APK.
- `/install` — запуск установщика пакетов Android.
- `/write <path>` — запись сгенерированного кода в файл.

---

## 🛠 Набор инструментов агента (Tool Registry)

| Инструмент | Назначение |
|---|---|
| `view_file` | Чтение содержимого файла с поддержкой срезов строк. |
| `write_to_file` | Создание или перезапись файлов (манифест, разметка, Java). |
| `replace_file_content` | Точечная хирургическая замена участков кода без перезаписи всего файла. |
| `list_files` | Просмотр дерева файлов рабочей области. |
| `run_command` | Выполнение команд в командной строке Android / Termux. |
| `git_command` | Прямое управление Git и GitHub (commit, push, remote). |
| `build_apk` | Запуск конвейера компиляции (AAPT2 + ECJ + D8 + Signer) и возврат логов сборки агенту. |
| `install_apk` | Запуск системного установщика пакетов Android для установки APK на телефон. |
