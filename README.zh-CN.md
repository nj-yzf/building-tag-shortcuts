# Building Tag Shortcuts

这是一个用于提高 JOSM 建筑编辑效率的小插件。

## 功能

- `1` ... `9`：给当前选中对象设置 `building:levels=1` ... `building:levels=9`
- 按住一个数字键再按另一个数字键：设置两位数楼层  
  例如按下 `1` 时会先设置 `building:levels=1`，继续按住 `1` 再按 `8`，会更新为 `building:levels=18`
- 第二位支持 `0`；相同数字可通过主键盘和小键盘组合输入，例如主键盘 `1` 加小键盘 `1` 设置为 `11`
- `Ctrl+1` ... `Ctrl+9`：默认设置 `name=1栋` ... `name=9栋`
- 按住 `Ctrl` 和第一位数字，再按第二位数字：设置两位数栋号  
  例如按下 `Ctrl+2` 时会先设置 `name=2栋`，继续按住 `Ctrl+2` 再按 `3`，会更新为 `name=23栋`
- 栋号两位数输入同样支持第二位为 `0`，也支持主键盘/小键盘组合输入相同数字
- `Ctrl+Shift+D`：在 `building=*` 和 `building:part=*` 之间切换，并保留原值
- `Ctrl+Shift+Q`：打开建筑高度工具窗口

`Ctrl+Shift+D` 支持多选智能处理：

- 所有选中对象都是 `building=*`：全部转为 `building:part=*`
- 所有选中对象都是 `building:part=*`：全部转为 `building=*`
- 选区中既有 `building=*` 又有 `building:part=*`：只转换 `building=*` 对象，已有 `building:part=*` 的对象不变
- 没有 `building` 或 `building:part`：添加 `building:part=yes`
- 同一个对象同时有两个标签：只删除 `building=*`

建筑高度工具包含两块区域：

- 简单模式：按“每层高度 × building:levels”写入或更新 `height`
- 分段模式：按“下段层数/下段层高 + 上段层数/上段层高”计算总高度；下段层数、上段层数、总层数三个值只要填两个，就可以自动补全另一个

仅在简单模式中：

- 如果对象有 `building:min_level`，会额外写入 `min_height=每层高度*building:min_level`
- 如果对象有 `roof:height`，则 `height=每层高度*building:levels+roof:height`

层高输入框默认 `3.6` m，支持鼠标滚轮调整，每滚动一格变化 `0.1` m。

高度工具窗口使用紧凑数字输入框，并会在中文语言环境下显示中文标签。

栋号后缀可以在 JOSM 首选项中配置。默认可选值为 `栋`、`幢`、`号楼`，默认使用 `栋`。

设置页会作为 JOSM 首选项左侧的一级菜单出现：`编辑 -> 首选项 -> Building Tag Shortcuts`。

## 快捷键说明

JOSM 原本把直接数字键 `1..9` 分配给视角/缩放类操作。本插件会在焦点不在文字输入框时优先捕获这些数字键，用于设置楼层。

`Ctrl+Shift+D` 和 `Ctrl+Shift+Q` 是从 JOSM 快捷键清单中核对后选择的组合，没有发现精确冲突。

## 构建

需要 JDK 11 或更高版本，推荐 Java 17 LTS。

如果本机没有 JDK，可以运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-jdk.ps1
```

构建插件：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

输出文件：

```text
dist\building-tag-shortcuts.jar
```

## 安装

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

安装后重启 JOSM。如果需要，进入：

```text
编辑 -> 首选项 -> 插件
```

确认 `building-tag-shortcuts` 已启用。

插件菜单也在 JOSM 的 `数据/Data -> Building Tag Shortcuts` 下。

## 设置

打开：

```text
编辑 -> 首选项 -> Building Tag Shortcuts
```

可以添加或删除栋号后缀，并选择当前默认后缀。这个设置页后续也可以继续容纳其他插件设置。
