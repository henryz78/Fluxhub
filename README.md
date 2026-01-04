# FluxHub

<p align="center">
  <img src="artworks/banner.jpg" alt="FluxHub Banner" width="100%">
</p>

<p align="center">
  <b>基于 iOS 26 Liquid Glass 风格的现代化 AI 聊天应用</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-green?logo=android" alt="Android 24+">
  <img src="https://img.shields.io/badge/Kotlin-2.1-purple?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Latest-blue?logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue" alt="GPL-3.0 License">
</p>

---

## ✨ 特性

### 🎨 Liquid Glass UI
- 采用 iOS 26 风格的毛玻璃效果界面
- 实时背景模糊、色散和高光效果
- 流畅的 60fps 渲染性能

### 💬 智能对话
- 流式输出 - 实时显示 AI 回复
- 多轮对话 - 完整的上下文记忆
- 思考过程 - 可视化 AI 推理步骤

### 🤖 多模型支持
- 兼容 OpenAI API
- 支持 DeepSeek、Claude、Gemini 等
- 快速切换模型，无需重启

### 📝 Markdown 渲染
- 代码高亮（多语言）
- 一键复制代码块
- 表格、列表、引用支持

### 📱 原生体验
- 纯 Kotlin + Jetpack Compose
- Material Design 3
- 适配深色模式

---

## 📸 功能概览

| 功能 | 描述 |
|------|------|
| 🏠 首页 | 统计信息、快捷提示词、最近对话 |
| 💬 对话 | 消息气泡、思考过程、操作按钮 |
| ⚙️ 设置 | API 配置、模型选择、个人资料 |
| 🔄 模型切换 | 聊天界面快速切换模型 |

---

## 🚀 快速开始

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 21
- Android SDK 36（API Level 36）

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/HenryZ-0302/Fluxhub.git
cd Fluxhub

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 安装到设备
./gradlew installDebug
```

### 配置说明

1. 打开应用，进入「设置」页面
2. 填写 **Base URL**（如 `https://api.openai.com/v1`）
3. 填写 **API Key**
4. 选择或输入模型名称（如 `gpt-4o`）
5. 保存设置，开始对话！

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI |
| Material 3 | 设计系统 |
| Room | 本地数据库 |
| DataStore | 设置存储 |
| OkHttp SSE | 流式请求 |
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Liquid Glass 效果 |

---

## 📁 项目结构

```
app/src/main/java/com/liquidglass/fluxhub/
├── chat/           # 聊天相关页面
│   ├── ChatScreen.kt
│   ├── HomeScreen.kt
│   ├── SettingsScreen.kt
│   └── ChatViewModel.kt
├── components/     # Liquid Glass UI 组件
├── data/           # 数据层（Room、DataStore）
├── ui/components/  # 通用 UI 组件
│   ├── message/    # 消息组件
│   └── richtext/   # Markdown 渲染
└── utils/          # 工具类
```

---

## 🗺️ 开发路线图

- [x] 基础聊天功能
- [x] Liquid Glass UI 效果
- [x] 流式输出
- [x] 思考过程展示
- [x] 模型快速切换
- [x] 首页统计和快捷功能
- [ ] 多 Provider 支持
- [ ] 历史对话搜索
- [ ] 数据备份恢复
- [ ] 消息分支

---

## 📄 许可证

本项目基于 **GPL-3.0 许可证** 开源。

Liquid Glass 效果库来自 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)，采用 Apache-2.0 许可证。

---

## 🙏 致谢

- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) - Liquid Glass 效果库
- [RikkaHub](https://github.com/rikkahub/rikkahub) - 部分功能实现参考
- [Lucide Icons](https://lucide.dev/) - 图标库
