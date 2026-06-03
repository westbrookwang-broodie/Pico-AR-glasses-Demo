# 统一视觉风格规范

所有面板共用一套风格，保证多个场景之间观感一致。具体实现见 `assets/ui-kit/SpatialUiKit.kt`，本文件是规范说明与取值依据。

## 设计原则

- **悬浮卡片感**：面板/控件用 `backgroundMaterial()` 的半透明材质背景 + 圆角，贴合 PICO 空间窗口的玻璃质感。
- **可交互元素必带悬停反馈**：任何 `clickable` 的元素都加 `Modifier.spatialHoverEffect()`，眼镜端注视/指向时有反馈。
- **层级清晰**：标题（大号粗体）> 副标题/正文（中号常规，弱化色）> 辅助说明（小号灰色）。
- **留白充足**：空间界面阅读距离远，间距比手机端更大（外边距 32–40dp，元素间距 16–24dp）。

## 调色板（统一取值）

| 名称 | 值 | 用途 |
|------|-----|------|
| TextDark | `Color(0xFF1A1A1A)` | 标题、正文主色 |
| TextGray | `Color(0xFF666666)` | 副标题、辅助说明 |
| NoteBlue | `Color(0xFFBBD2FF)` | 便签/标签：信息类 |
| NotePink | `Color(0xFFFFC1C8)` | 便签/标签：强调/疑问类 |
| NoteGreen | `Color(0xFFC2EBB8)` | 便签/标签：成功/新增类 |

> 也可改用 PICO 的 `Vibrant` 主题色（`Color.Vibrant.withVibrant(Vibrant.UltraDark)` 等），与 `PicoTheme` 自动适配明暗。固定色值适合需要稳定品牌色的便签；正文优先用 Vibrant 以跟随主题。

## 字号与字重

| 角色 | fontSize | fontWeight | lineHeight |
|------|----------|------------|------------|
| 面板大标题 | 30sp | W700 | 38sp |
| 内容标题 | 22–26sp | W600/W700 | 30–34sp |
| 正文 | 15–16sp | W400/W500 | 22–24sp |
| 辅助/状态 | 14sp | W400 | 18sp |
| 按钮文字 | 16sp | W600 | — |

## 间距

- 面板外边距：`padding(32.dp)`（密集内容）到 `padding(40.dp)`（首页/Hub）。
- 标题与内容间：`Spacer(Modifier.height(16.dp))`。
- 同级元素间：`Arrangement.spacedBy(16.dp~24.dp)`。
- 按钮内边距：`padding(horizontal = 24.dp, vertical = 12.dp)`。
- 圆角：面板/卡片 12–16dp，胶囊按钮 10dp，状态条 20dp，关闭按钮用 `CircleShape`。

## 面板结构约定

每个面板统一为：顶部标题栏（标题 + 可选关闭按钮）→ 内容区 → 底部操作区（前进/返回/关闭按钮）。用 `PanelScaffold` 统一封装，避免每个面板各写一套标题栏。

便签/标签用统一的 `StickyNote`（固定尺寸 + 调色板颜色 + 居中粗体文字 + 悬停反馈）。

按钮统一用 `SpatialPanelButton`（胶囊 + 材质背景 + 悬停）；关闭用 `SpatialCloseButton`（圆形 ✕）。
