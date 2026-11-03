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
- ⚡ **性能优化**：颜色查找带缓存、性能统计默认关闭，字幕渲染路径无额外开销
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
- Minecraft 26.1 (Fabric) - 位于 `26.1/` 目录
- Minecraft 26.1.2 (Fabric) - 位于 `26.1.2/` 目录
- Minecraft 26.2 (Fabric) - 位于 `26.2/` 目录
- Minecraft 26.3 (Fabric) - 位于 `26.3/` 目录

## 26.x

26.x 起 Minecraft 改用 Mojang 官方映射（不再提供 Yarn），并且渲染层换成了 `GuiGraphicsExtractor` + `Matrix3x2fStack`。因此 `26.x/` 下的四个移植与 1.x 分支不是同一套源码：

- 源码目录合并为 `src/main/java`（不再 split client source set）
- 使用 loom `1.19-SNAPSHOT`（不 remap，直接以 Mojang 名运行）
- 需要 **JDK 25** 与 **Gradle 9.8.0 及以上**
- 字幕 HUD 的注入点从 `SubtitlesHud.render` 换成 `SubtitleOverlay.extractRenderState`
- 按键一律用 `new KeyMapping(name, -1, category)` 构造，避免 `org.lwjgl.glfw` 与 `InputConstants.Type` 在 26.2/26.3 的差异

## NeoForge
`neoforge/` 下的移植仍为未修改的 MDK 模板（示例方块、示例物品、示例标签页等），功能尚未实现，暂不发布。

## 构建
1.x 需要 JDK 21，26.x 需要 JDK 25。

```shell
./gradlew build
```

产物位于 `build/libs/`。若 wrapper 因网络策略无法下载 Gradle 发行版，也可用本机 Gradle 直接构建：

```shell
gradle build
```

26.x 目录还必须使用 Gradle 9.8.0 或更高版本（loom 1.19.3 的硬性要求），例如：

```shell
gradle build --no-configuration-cache
```

## 安装方法
1. 安装 Fabric Loader
2. 将模组 JAR 文件放入 `mods` 目录
3. 启动游戏即可

## 配置文件
配置文件位于 `.minecraft/config/subtitle_highlight.json`

## 数据包支持
创建 `data/subtitle_highlight/subtitle_types.json` 自定义字幕颜色配置

默认以配置界面（`config/subtitle_highlight.json`）中的颜色为准，只有配置界面没有定义的颜色才回退到数据包，所以改配置界面的颜色会立刻生效。

如果希望数据包反过来覆盖配置界面，可在配置界面的「通用」分类中打开「数据包颜色优先」（对应配置项 `datapackPriority`，默认 `false`）。

## 开源协议
本项目基于 **GNU Lesser General Public License v3.0** 发布。

你可以自由使用、修改、分发，但必须：
1. 保留原作者版权声明
2. 修改后分发需公开源码并继续使用 LGPLv3
3. 不得限制用户替换、修改此模组
