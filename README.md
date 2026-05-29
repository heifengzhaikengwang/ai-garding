# AI自动阅卷 - Android原生App

基于阿里云百炼平台视觉模型的自动阅卷打分工具。通过屏幕截图+AI评分+自动点击，实现无人值守的批量阅卷。

## 功能特点

- 🤖 **AI智能评分**：基于阿里云百炼Qwen-VL模型进行图像内容分析和评分
- 📸 **自动截图**：使用MediaProjection API实现屏幕截图
- 👆 **自动点击**：通过无障碍服务实现自动点击评分按钮
- 🔄 **循环批阅**：自动处理下一份答卷，实现批量阅卷

## 工作原理

```
1. 截图答案区(自动) → 2. 发给AI评分(百炼API) → 3. 点击分值按钮(自动) → 4. 等待跳转 → 下一份答卷(自动循环)
```

## 使用步骤

### 第一步：安装App

使用Android Studio打开本项目，连接手机或模拟器，点击Run安装。

### 第二步：开启无障碍服务

1. 打开手机 `设置` → `无障碍`
2. 找到 `AI自动阅卷` 并开启

### 第三步：配置API Key

在使用前需要配置阿里云百炼API Key：

1. 登录阿里云百炼平台
2. 创建API Key
3. 在应用配置中输入API Key

### 第四步：开始阅卷

1. 打开阅卷系统网页或应用
2. 点击"开始阅卷"按钮
3. 授权屏幕录制权限
4. 系统自动开始批阅

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/aigarding/
│   │   ├── MainActivity.java          # 主界面和流程控制
│   │   ├── service/
│   │   │   └── AutoClickService.java  # 无障碍服务
│   │   └── utils/
│   │       ├── AIAPI.java             # 阿里云百炼API交互
│   │       ├── ConfigManager.java     # 配置管理
│   │       └── ScreenCapture.java     # 屏幕截图工具
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml      # 主界面布局
│   │   ├── values/
│   │   │   ├── strings.xml            # 字符串资源
│   │   │   ├── colors.xml             # 颜色资源
│   │   │   └── styles.xml             # 样式资源
│   │   └── xml/
│   │       ├── accessibility_service_config.xml  # 无障碍服务配置
│   │       ├── data_extraction_rules.xml        # 数据提取规则
│   │       └── backup_rules.xml                # 备份规则
│   └── AndroidManifest.xml            # 应用清单
├── build.gradle                       # 模块构建配置
└── proguard-rules.pro                 # ProGuard规则
```

## 技术栈

- **语言**: Java 8
- **框架**: Android SDK 34
- **网络**: OkHttp 4.12.0
- **AI服务**: 阿里云百炼平台 (Qwen-VL-Plus)

## 权限说明

| 权限 | 用途 |
|------|------|
| `WRITE_EXTERNAL_STORAGE` | 存储截图文件 |
| `READ_EXTERNAL_STORAGE` | 读取截图文件 |
| `SYSTEM_ALERT_WINDOW` | 悬浮窗显示 |
| `FOREGROUND_SERVICE` | 前台服务 |
| `BIND_ACCESSIBILITY_SERVICE` | 无障碍服务 |

## 注意事项

1. 使用前需确保已正确配置阿里云百炼API Key
2. 首次使用需要授权屏幕录制权限
3. 需要开启无障碍服务才能实现自动点击功能
4. 建议在稳定的网络环境下使用
5. 本工具仅供学习和研究使用

## License

MIT License