# FixTconstructPickaxe

这是一个为 Minecraft 1.20.1 设计的模组（Mod），旨在修复匠魂（Tinkers' Construct）中的一个问题：匠魂的镐子无法正确地被其他需要检查工具等级和类型的模组识别。

## 解决了什么问题？

在原版 Minecraft 和许多模组中，对方块（Block）的采集行为通常会检查玩家手中的工具是否是 `PickaxeItem`（镐子物品）的实例，并获取其挖掘等级（Tier）。

然而，匠魂的工具（例如镐子）为了实现其高度的可定制性，并没有直接继承原版的 `PickaxeItem` 类。这导致当其他模组通过 `instanceof PickaxeItem` 来判断工具类型时，匠魂的镐子无法被识别，从而可能导致一些兼容性问题或功能失效。

本模组通过底层的字节码修改技术（ASM Coremod），在游戏加载时动态地修改所有继承自 `net.minecraft.world.level.block.Block` 的类的 `canHarvestBlock` 方法。它将硬编码的 `instanceof PickaxeItem` 和 `(PickaxeItem)item` 类型转换替换为更通用的工具判断逻辑，使得匠魂的镐子也能被正确识别，从而解决了这一兼容性问题。

## 技术实现

本模组使用了 Mixin Launch Service 在游戏早期进行字节码操作。

  * **`FixTconstructPickaxePlugin.java`**: 这是核心的启动插件，它负责扫描所有加载的类。
  * **`FixTconstructPickaxeClassInfo.java`**: 一个自定义的类信息工具，用于在非常早期的加载阶段获取并分析类的继承关系，因为它需要在 Mixin 系统完全初始化之前工作。
  * **字节码修改**: 当插件找到一个 `Block` 的子类时，它会遍历其方法。如果找到了 `canHarvestBlock` 方法，它会：
    1.  将 `INSTANCEOF net/minecraft/world/item/PickaxeItem` 的检查指令替换为调用 `com.mx_wj.fixtconstructpickaxe.core.EventUtils.isPickaxe` 方法。
    2.  将 `CHECKCAST net/minecraft/world/item/PickaxeItem` 的类型转换指令替换为调用 `com.mx_wj.fixtconstructpickaxe.core.EventUtils.getTiered` 方法。
    3.  移除对原版 `PickaxeItem.getTier()` 的调用。

通过这种方式，它在不使用 Mixin `(@Inject, @Redirect)` 的情况下，实现了对大量类的通用兼容性修复。

## 如何构建

本项目使用 Gradle 进行构建。

1.  确保你已经安装了 JDK 17 或更高版本。

2.  在项目根目录中打开你的命令行工具。

3.  运行以下命令来构建项目：

      * 在 Windows 上:
        ```bash
        gradlew build
        ```
      * 在 Linux 或 macOS 上:
        ```bash
        ./gradlew build
        ```

4.  构建成功后，你可以在 `build/libs/` 目录下找到生成的 JAR 文件。

## 依赖

  * Minecraft Forge for 1.20.1 (版本 47.4.6 或更高)
  * Tinkers' Construct (匠魂)

## 许可证 (License)

本项目采用 **MIT 许可证**。

这意味着你可以自由地使用、复制、修改、分发本软件。但你必须在你的衍生作品中包含原始的版权声明和许可证文本。

详细信息请参阅 [LICENSE.txt](https://www.google.com/search?q=LICENSE.txt) 文件。