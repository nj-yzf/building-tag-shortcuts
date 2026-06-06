# 构建与安装说明

## 1. 安装 JDK

需要 JDK 11 或更高版本。JOSM 文档说明 Java 11 可用，Java 17 LTS 更推荐。

如果不想手动配置 Java，可以运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-jdk.ps1
```

脚本会把便携版 JDK 17 下载到 `.tools\jdk-17`，不会修改系统 Java 设置。

## 2. 构建插件

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

脚本会在缺少 `josm-tested.jar` 时自动下载到 `lib\`，然后编译插件并生成：

```text
dist\building-tag-shortcuts.jar
```

## 3. 安装到 JOSM

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

脚本会复制插件到：

```text
%APPDATA%\JOSM\plugins\building-tag-shortcuts.jar
```

安装后重启 JOSM。

因为这是手动放入的插件 jar，如未自动启用，打开：

```text
编辑 -> 首选项 -> 插件
```

勾选 `building-tag-shortcuts`。

## 4. 使用

- 选中一个或多个 OSM 对象
- 按 `1..9` 设置一位数 `building:levels`
- 按住第一位数字再按第二位数字，设置两位数楼层，例如按下 `1` 时先设置 `building:levels=1`，继续按住 `1` 再按 `8` 更新为 `building:levels=18`
- 第二位可以是 `0`；`11` 这类重复数字可通过主键盘和小键盘组合输入
- 按 `Ctrl+1..9` 使用当前后缀设置栋号，例如默认设置 `name=1栋`
- 按住 `Ctrl` 和第一位数字，再按第二位数字，设置两位数栋号，例如先设置 `name=2栋`，再更新为 `name=23栋`
- 按 `Ctrl+Shift+D` 切换 `building` 和 `building:part`
- 按 `Ctrl+Shift+Q` 打开建筑高度工具窗口
- 出错时可直接使用 JOSM 撤销

`Ctrl+Shift+D` 规则：

- 全部是 `building=*` -> 全部转为 `building:part=*`
- 全部是 `building:part=*` -> 全部转为 `building=*`
- 同时有 `building=*` 和 `building:part=*` -> 只转换 `building=*` 对象
- 没有建筑标签 -> 添加 `building:part=yes`
- 同一个对象同时有两个标签 -> 删除 `building=*`

建筑高度工具包含：

- 简单模式：使用自定义每层高度，根据已有 `building:levels` 计算并写入 `height`
- 分段模式：使用下段层数/下段层高、上段层数/上段层高计算总高度；下段层数、上段层数、总层数三者只需填写两个，即可补全第三个

简单模式还会处理可选标签：

- 有 `building:min_level` 时，写入 `min_height=每层高度*building:min_level`
- 有 `roof:height` 时，把 `roof:height` 加入最终 `height`

层高输入框默认 `3.6` m，支持鼠标滚轮以 `0.1` m 为步长调整。

高度工具窗口会根据语言环境显示中文或英文，并尽量使用紧凑布局。

## 5. 设置

打开：

```text
编辑 -> 首选项 -> Building Tag Shortcuts
```

当前设置页支持配置栋号后缀。默认列表为 `栋`、`幢`、`号楼`，默认使用 `栋`。这个页面是插件的通用设置区域，以后可以继续添加其他设置项。

它应当显示为首选项左侧的一级菜单，而不是只能在“高级首选项”里编辑原始键值。

## 6. 快捷键备注

直接数字键 `1..9` 原本属于 JOSM 视角/缩放操作。插件会在焦点不在文字输入框时优先捕获这些按键。

`Ctrl+Shift+D` 和 `Ctrl+Shift+Q` 仍可在 JOSM 的键盘快捷键设置中查看或重新分配。
