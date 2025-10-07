# FixTconstructPickaxe

这是一个为 Minecraft 1.20.1 设计的模组（Mod），旨在修复匠魂（Tinkers' Construct）中的一个问题：匠魂的镐子无法正确地被其他需要检查工具等级和类型的模组识别。

## 解决了什么问题？

在许多模组中，对方块（Block）的采集行为通常会检查玩家手中的工具是否是 `PickaxeItem`（镐子物品）的实例，并获取其挖掘等级（Tier）。

然而，匠魂的工具（例如镐子）为了实现其高度的可定制性，并没有直接继承原版的 `PickaxeItem` 类。这导致当其他模组通过 `instanceof PickaxeItem` 来判断工具类型时，匠魂的镐子无法被识别，从而可能导致一些兼容性问题或功能失效。

本模组通过底层的字节码修改技术（ASM Coremod），在游戏加载时动态地修改所有继承自 `net.minecraft.world.level.block.Block` 的类的 `canHarvestBlock` 方法。它将硬编码的 `instanceof PickaxeItem` 和 `(PickaxeItem)item` 类型转换替换为更通用的工具判断逻辑，使得匠魂的镐子也能被正确识别，从而解决了这一兼容性问题。

## 技术实现

本模组采用了一种底层的 Coremod（核心模组）方案，通过在游戏启动时注入一个启动插件（Launch Plugin），来动态修改所有方块（`Block`）的工具采集逻辑。这种方式避免了使用大量 Mixin 注解，实现了对整个游戏的全局性修复。

### ASM 字节码修改核心

所有核心逻辑均在 `FixTconstructPickaxePlugin` 的 `processClass` 方法中实现。当一个继承自 `net.minecraft.world.level.block.Block` 的类被加载时，插件会使用 ASM 库对 `canHarvestBlock` 方法的字节码指令流进行精确的“外科手术式”修改。

**1. 识别目标字节码**

插件首先会扫描 `canHarvestBlock` 方法，寻找原版游戏中写死的工具检查逻辑。以下是该逻辑的一个典型示例：

```
// ... (获取 ItemStack 和 Item)
// 【问题所在】只认识原版镐子
INSTANCEOF net/minecraft/world/item/PickaxeItem
IFEQ E  

// 【问题所在】强制类型转换和特定方法调用
ALOAD Item   // 加载 Item
CHECKCAST net/minecraft/world/item/PickaxeItem
ASTORE tieredItem // tieredItem 变量现在是 PickaxeItem 类型

ALOAD tieredItem // 加载 PickaxeItem
INVOKEVIRTUAL net/minecraft/world/item/PickaxeItem.m_43314_()Lnet/minecraft/world/item/Tier; // 调用 getTier() 方法
// 栈顶现在是 Tier 对象
...
```

**2. 栈操作与变量保存 (准备工作)**

为了让我们后续的 `getTiered` 方法能获取到完整的 `ItemStack` 对象（而不是 `Item`），插件会在调用 `getItem()` 之前，插入 `DUP` 和 `ASTORE` 指令，将 `ItemStack` 对象的引用复制并保存到一个新的局部变量中。

**3. 替换工具类型检查 (`INSTANCEOF`)**

插件会精确定位到 `INSTANCEOF net/minecraft/world/item/PickaxeItem` 这条指令，并将其替换为一个全新的方法调用指令：`INVOKESTATIC com/mx_wj/fixtconstructpickaxe/core/EventUtils.isPickaxe(...)`。这一步将检查逻辑通用化。

**4. 替换并优化挖掘等级获取逻辑 (关键步骤)**

这是整个修改中最核心的部分，分为\*\*“替换”**和**“移除”\*\*两步：

* **第一步：替换 `CHECKCAST`**

    * 插件找到 `CHECKCAST net/minecraft/world/item/PickaxeItem` 指令。
    * 它将这条指令**替换**为静态方法调用 `INVOKESTATIC com/mx_wj/fixtconstructpickaxe/core/EventUtils.getTiered(...)`。
    * 同时，在此之前，原先加载 `Item` 的 `ALOAD Item` 指令，会被修改为加载**第 2 步**中保存的 `ItemStack` 变量的指令，以满足 `getTiered` 方法的参数要求。
    * **此时，字节码变为：**
      ```
      // ...
      ALOAD ItemStack
      INVOKESTATIC com/mx_wj/fixtconstructpickaxe/core/EventUtils.getTiered(...) // 调用 getTiered()
      ASTORE tieredItem // tieredItem 变量现在直接是 Tier 类型了！
      ```

* **第二步：移除冗余的 `getTier()` 调用**

    * 此时 `tieredItem` 变量里已经直接是 `Tier` 对象了。原版代码后续的 `ALOAD tieredItem` 和 `INVOKEVIRTUAL PickaxeItem.m_43314_()` (`getTier()`) 就完全是多余且错误的。
    * 因此，插件会继续扫描，找到这条 `INVOKEVIRTUAL ... PickaxeItem.m_43314_()` 指令，并将其从方法的指令列表中**彻底移除**。

**最终效果对比**

* **修改前**:
  ```
  CHECKCAST PickaxeItem  // 转换成 PickaxeItem
  ASTORE tieredItem
  ALOAD tieredItem
  INVOKEVIRTUAL PickaxeItem.getTier() // 再从 PickaxeItem 获取 Tier
  ```
* **修改后**:
  ```
  INVOKESTATIC EventUtils.getTiered() // 直接获取 Tier
  ASTORE tieredItem
  ALOAD tieredItem  // 直接加载 Tier，后面不再有 getTier() 调用
  ```

通过这一系列字节码替换和移除操作，代码逻辑被重构，使得后续获取挖掘等级 (`Tier.m_6604_()`) 的代码可以直接使用 `tieredItem` 变量，使得匠魂的镐子也能被正确识别。

### 4. 通用工具判断逻辑

上述修改将原有的硬编码检查逻辑重定向到了 `EventUtils` 类中的通用方法，从而实现了兼容性。

-   **`isPickaxe(Item item)`**:
    -   这个方法替代了 `instanceof PickaxeItem` 检查。
    -   它不仅判断一个物品是否为 `PickaxeItem` 的实例，还会检查该物品是否拥有 `ItemTags.PICKAXES` 标签。这使得任何将自己正确注册到标签下的模组镐子（包括匠魂的）都能被识别。

-   **`getTiered(ItemStack itemStack)`**:
    -   这个方法用于安全地获取工具的挖掘等级（`Tier`）。
    -   如果物品是原版的 `PickaxeItem`，它会正常返回其挖掘等级。
    -   如果物品实现了匠魂的 `IModifiable` 接口，它会通过匠魂的 `ToolStack` API 来获取工具的 `HARVEST_TIER` 等级。
    -   如果工具不属于以上任何一种，则返回一个不会导致崩溃的空等级（`EMPTY_TIER`）。

通过以上步骤，该模组在不使用任何 Mixin 注解的情况下，实现了对游戏中部分模组方块收获逻辑的通用性修复，从解决了匠魂工具因不继承 `PickaxeItem` 而导致的兼容性问题。

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