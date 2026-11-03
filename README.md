# Subtitle Highlight

**当前版本为官方高版本移植**

Minecraft 字幕高亮模组 - 为字幕添加颜色和样式，提升游戏体验

## 项目信息
- **模组 ID**: `subtitle_highlight`
- **主包名**: `Yeah_Zero.Subtitle_Highlight`
- **作者**: Yeah-Zero，Qituo，Yifei
- **协议**: GNU LGPLv3

## 功能特性
- 🎨 **字幕颜色自定义**：根据声音类型自动着色字幕
- ⚡ **性能优化**：视锥体检查、对象池模式，流畅运行
- 📦 **数据驱动配置**：支持通过数据包自定义字幕颜色
- 🔧 **命令系统**：支持 `/subtitle_highlight` 命令进行配置管理
- 🚀 **模组 API**：提供 `SubtitleAPI` 供其他模组扩展功能
- 💾 **配置分享**：支持配置导出/导入
- ⌨️ **快捷键支持**：快速调整字幕设置

## 支持版本
- Minecraft 1.21.11 (Fabric) - 主项目
- Minecraft 1.21.10 (Fabric) - 位于 `1.21.10/` 目录
- Minecraft 1.21.4 (Fabric) - 位于 `1.21.4/` 目录
- Minecraft 1.21.1 (Fabric) - 位于 `1.21.1/` 目录
- Minecraft 1.20.6 (Fabric) - 位于 `1.20.6/` 目录

## NeoForge
`neoforge/` 下的移植仍为未修改的 MDK 模板（示例方块、示例物品、示例标签页等），功能尚未实现，暂不发布。

## 构建
需要 JDK 21。

```shell
./gradlew build
```

产物位于 `build/libs/`。若 wrapper 因网络策略无法下载 Gradle 发行版，也可用本机 Gradle 9.x 直接构建：

```shell
gradle build
```

## 安装方法
1. 安装 Fabric Loader
2. 将模组 JAR 文件放入 `mods` 目录
3. 启动游戏即可

## 配置文件
配置文件位于 `.minecraft/config/subtitle_highlight.json`

## 数据包支持
创建 `data/subtitle_highlight/subtitle_types.json` 自定义字幕颜色配置

数据包颜色优先级高于配置界面：仅当数据包未定义某个字幕类型时，才会使用 `config/subtitle_highlight.json` 中的颜色。内置数据包已覆盖全部类型，因此若不覆盖或移除内置数据包，配置界面中的颜色项不会生效。

## 开源协议
本项目基于 **GNU Lesser General Public License v3.0** 发布。

你可以自由使用、修改、分发，但必须：
1. 保留原作者版权声明
2. 修改后分发需公开源码并继续使用 LGPLv3
3. 不得限制用户替换、修改此模组
