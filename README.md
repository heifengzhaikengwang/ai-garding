# AI自动阅卷 Android App

基于阿里云百炼平台视觉模型的自动阅卷工具。

## 功能

- 自定义截图区域
- 分值按钮坐标配置
- 参考答案输入
- 自动评分和点击

## 快速开始

### 1. 配置API
在应用中输入：
- API Key
- 模型ID
- 参考答案

### 2. 配置区域
- 设置截图区域坐标（左,上,右,下）
- 添加分值按钮（分值、X坐标、Y坐标）

### 3. 开始批改
点击"开始批改"即可自动循环批阅。

## 技术栈

- Android SDK 34
- Java 8
- OkHttp 4.12.0
- 阿里云百炼API

## 权限

- 无障碍服务（自动点击）
- 屏幕录制（截图）
- 存储权限

## 构建

项目已配置 GitHub Actions，每次推送会自动构建 APK。

或者本地构建：
```bash
./gradlew assembleDebug
```

APK 位置：`app/build/outputs/apk/debug/app-debug.apk`
