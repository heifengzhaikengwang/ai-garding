# 构建说明

## 方法1: Android Studio（推荐）

1. 安装 Android Studio
2. File → Open → 选择项目文件夹
3. Android Studio 会自动下载 Gradle Wrapper
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

## 方法2: 命令行

需要先安装 Gradle，然后运行：

```bash
gradle wrapper
./gradlew assembleDebug
```

## 方法3: GitHub Actions（自动构建）

推送到 GitHub 后，GitHub Actions 会自动构建 APK。

APK 文件将在 Actions 运行完成后可下载。

## 重要文件

确保以下文件存在：
- gradle/wrapper/gradle-wrapper.jar（需要从 Gradle 官网下载）
- gradlew（Unix/Linux/Mac）
- gradlew.bat（Windows）
- gradle/wrapper/gradle-wrapper.properties

如果 gradle-wrapper.jar 缺失，运行：
```bash
gradle wrapper
```

## 直接下载 gradle-wrapper.jar

从以下地址下载：
https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar

放入 `gradle/wrapper/` 目录。
