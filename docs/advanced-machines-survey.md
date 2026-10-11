# 还有哪些机器值得做"高级版"——调研报告

> 只报告，不实现。本报告是继粉碎轮（crushing_wheel）、动力锯（mechanical_saw）、
> 鼓风机（encased_fan）之后，对本 mod 下一台"高级机器"候选的调研结论。
>
> 核实状态标记：
> **【亲验】** = 本次会话用 `javap` 对 Create 6.0.10 jar（curse.maven:create-328085:7963363）
> 反汇编 / 反编译核对，或直接读取仓库文件；
> **【调研引用】** = 来自定稿前的两份调研报告，本次未复核；
> **【未核】** = 尚未核实。

## 结论速览

| 优先级 | 候选机器 | 提速机制 | 关键钩子 | 核实状态 | 预估成本 |
| --- | --- | --- | --- | --- | --- |
| 1 | 动力钻 Mechanical Drill | 破坏速度 × (1 + 并行数/10) | `BlockBreakingKineticBlockEntity#getBreakSpeed()` protected | 【亲验】 | 低（几乎照抄 saw 的破坏加速） |
| 2 | 石磨 Millstone | 磨制时长 ÷ 速度倍率 | `MillstoneBlockEntity#getProcessingSpeed()` public、`timer` public | 【亲验】 | 低 |
| 新增 | CMM 分级蒸汽机 Steam Engine | 每级发电容量/转速提升 | CMM 完整参考实现 + 每级配置项已预埋，但被全等级拉黑（见启用状态核查） | 【亲验】 | 低～中（不可复用其注册，需照 saw 先例重写） |
| 新增 | CMM 分级储液罐 Fluid Tank | 每级流体容量提升 | CMM 完整参考实现 + 配套流体已上线，但被全等级拉黑（见启用状态核查） | 【亲验】 | 低～中（同上） |
| 3 | 机械臂 Arm | 缩短动作计时 / 提升单次搬运 | `phase` 字段存在但 **package-private** | 【亲验】 | 中～高（条件候选，立项前需再核） |
| 4 | 软管滑轮 Hose Pulley | 提升抽/注液速率 | `HosePulleyFluidHandler` public、速率藏在 `FluidFillingBehaviour`/`FluidDrainingBehaviour` | 【亲验（类签名）】+【未核（行为内部）】 | 中 |
| 5 | 水车 / 大型水车 Water Wheel | 提升发电容量 | 容量走 `BlockStressValues.CAPACITIES` 外部注册 | 【亲验】 | 中 |
| 6 | 物品库 Item Vault | 更大存储（仓储向，非速度向） | `ItemVaultBlockEntity.inventory` protected，多方块容器 | 【亲验（类签名）】 | 中 |
| 不建议 | 物品排水器 Item Drain | 加快排水 | `itemMovementPerTick()` **private** | 【亲验】 | 成本高于收益 |

## 候选详情

### ① 动力钻 Mechanical Drill（优先级最高）

- **证据**：
  - 【亲验】`javap`：`com.simibubi.create.content.kinetics.drill.DrillBlockEntity extends
    com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity`，
    基类持有 `protected float getBreakSpeed()`。
  - 【亲验】本仓库 `CMMMechanicalSawBlockEntity#getBreakSpeed()`
    （`src/main/java/net/tomorrow325/createmoremoremachines/api/content/mechanical_saw/CMMMechanicalSawBlockEntity.java:142`）
    已经在编译并出货的同一条钩子上做了 (1 + 并行数/10) 的破坏加速——钻头可直接照搬。
- **做法**：注册模式与 saw 完全一致（`AdvancedMachineType.create("mechanical_drill", AllBlocks.MECHANICAL_DRILL, …)`）；
  竖直朝下的钻头继承 vanilla `DrillBlockEntity`，只覆盖 `getBreakSpeed()`；配置沿用"每级一段"模式，
  建议只配并行数（复用 processing_multiple 语义），破坏速度公式 (1 + 并行数/10) 与 saw 一致。
- **注意**：装配机上移动的钻头走 `DrillMovementBehaviour extends BlockBreakingMovementBehaviour`
  （【亲验】），那条路径不经 `getBreakSpeed()`，高级钻不需要、也不应顺手改它；如要覆盖需另行立项。

### ② 石磨 Millstone

- **证据**：【亲验】`javap`：`MillstoneBlockEntity#getProcessingSpeed()` 是 **public**，
  且 `public int timer` 直接暴露——`tick()` 中每 tick 按 `getProcessingSpeed()` 递减 `timer`，
  归零即出料。子类覆盖 `getProcessingSpeed()` 即可让磨制时长按倍率缩短，无需触碰倒计时逻辑。
- **做法**：与鼓风机同款"速度倍率"语义（≤1 原速、-1 跟随 CMM 同级），是最贴近现有
  speed 条目模式的机器；`inputInv/outputInv/capability` 均 public，容量并行（每次磨多份）另议，非必需。
- **成本**：低。一个 BE 子类 + 一个 resolver + tooltip/lang/贴图，机械重复度最高。

### ③ 机械臂 Arm（条件候选——两份调研结论相反，裁决如下）

- **证据**：
  - 【亲验】`javap`：`com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity$Phase`
    枚举存在（SEARCH_INPUTS / MOVE_TO_INPUT / SEARCH_OUTPUTS / MOVE_TO_OUTPUT / DANCING），
    `ArmBlockEntity.phase` 字段存在。
  - 【亲验·新增】但该字段是 **package-private**（`javap -p` 显示无修饰符），且动作计时核心
    `tickMovementProgress()` 是 **private**——子类既读不到 `phase` 也调不到计时推进，
    覆盖 `tick()` 虽可行，却无法复用父类状态机内部状态。
- **裁决**：报告2 将机械臂列为第 3 优先级，报告1 不建议（未给理由）。本次 javap 证实：
  能做的只有"整段复制状态机"或"AT/mixin 打开 package-private/private 成员"，而本 mod
  目前没有 mixin 设施（鼓风机调研结论）。故降级为**条件候选**：立项前先决定是否接受
  引入 AT/mixin，或愿意整类复制 ArmBlockEntity（约 600 行）。
- **可做的替代面**（若接受成本）：`searchForItem()` / `searchForDestination()` /
  `depositItem()` / `collectItem()` / `getDistributableAmount()` 均为 protected（【亲验】），
  "单次搬运量提升"（类似 saw 的批量语义）比"动作提速"便宜得多——这是机械臂的推荐切入角度。

### ④ 软管滑轮 Hose Pulley

- **证据**：
  - 【亲验】`javap`：`com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler`
    是 public 类，`fill/drain` 委托给 `FluidFillingBehaviour` / `FluidDrainingBehaviour`，
    且 `getInternalTank()` public。
  - 【未核】速率数值藏在这两个 Behaviour 的内部（每 tick 搜索/搬运量），本次未反汇编。
- **做法**：机制层面成立——高级版按倍率提升抽液/注液速率或扩大单次搜索范围；
  立项前需先核 `FluidFillingBehaviour` / `FluidDrainingBehaviour` 的速率入口可见性。

### ⑤ 水车 / 大型水车 Water Wheel

- **证据**：
  - 【亲验】`javap`：`WaterWheelBlockEntity extends GeneratingKineticBlockEntity`。
  - 【亲验】发电容量不在方块内写死，而是走 `BlockStressValues.CAPACITIES` 外部注册；
    上游 CMM 已提供现成入口 `CMMBlockStressValues#setCapacities(double)`
    （`CreateMoreMachines/src/main/java/net/yxiao233/createmoremachines/api/registry/CMMBlockStressValues.java`，
    本次文件读取确认），本 mod 已在用同文件的 `setImpact`。
- **做法**：高级水车 = 同方块 + 更高 CAPACITIES 注册值，注册链一行即可，无 BE 子类也能做
  （视觉可按等级换材质，需 per-tier partial，参考粉碎轮做法）。
- **注意**：发电类机器的"倍率"语义与加工类不同（容量 vs 速度），配置条目命名建议
  `{tier}_water_wheel_capacity`，与现有条目并列而非复用。

### ⑥ 物品库 Item Vault

- **证据**：【亲验】`javap`：`com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity`
  为多方块容器（`IMultiBlockEntityContainer.Inventory`），`inventory` protected，
  容量由方块数量决定。
- **做法**：对应 CMM `fluid_tank` 的高级化思路（单格容量/上限提升），仓储向、非速度向；
  价值中等，建议排在加工类机器之后。

### ⑦ CMM 分级蒸汽机 Steam Engine（补查新增，条件候选）

- **证据**（均为文件读取，2026-10-11 补查）：
  - CMM 有完整实现：`CMMSteamEngineBlock` / `CMMSteamEngineBlockEntity`
    （`CreateMoreMachines/.../api/content/steam_engine/`），且
    `BuiltInAdvancedMachineTypes.STEAM_ENGINE = create("steam_engine", AllBlocks.STEAM_ENGINE, CMMRegistryEntry.getSteamEngines())`
    （`api/registry/BuiltInAdvancedMachineTypes.java:19`）。
  - 但 `CMMTierPlugin.onPostRegisterTier()`（`common/plugin/CMMTierPlugin.java:50`）无条件执行
    `STEAM_ENGINE.withoutAll()`，源码注释即 `//Temp disabled`——所有等级的注册门槛
    `CMMTier.shouldRegistry` 对黑名单类型一律返回 false（`api/registry/CMMTier.java:488-508`），
    `CMMTierManager` 的注册路径全部以它为门槛（`:395`），故运行时一台都不会出现。
  - 每级配置项已预埋：`TierConfigBase` 持有 `STEAM_ENGINE_GENERATED_SPEED`、`STEAM_ENGINE_CAPACITY`
    （`api/config/TierConfigBase.java:16-17`）——代码与配置链路完整，只差注册被放行。
- **做法**：本插件无法"重新启用"它（黑名单不可解除，见核查章节），只能照 mechanical_saw 的先例
  自行子类化 `AllBlocks.STEAM_ENGINE` 重写一套；CMM 的实现是最好的参考件。
- **风险**：`Temp disabled` 意味着上游大概率计划回归。**立项前先向 Y-Xiao233 确认禁用原因**
  （是未完工、有 bug 还是设计取舍）与重启计划，避免两个 mod 出现重复的高级蒸汽机。

### ⑧ CMM 分级储液罐 Fluid Tank（补查新增，条件候选）

- **证据**（均为文件读取，2026-10-11 补查）：
  - CMM 有完整实现：`CMMFluidTankBlock` / `CMMFluidTankBlockEntity` / `FluidTankGenerator` /
    `FluidTankMovementBehavior`（`CMMTierManager.java:349-378` 的注册链），类型定义在
    `BuiltInAdvancedMachineTypes.FLUID_TANK`（`BuiltInAdvancedMachineTypes.java:11`）。
  - 但 `CMMTierPlugin.java:51` 对四个普通等级 `FLUID_TANK.without(BRASS, NETHERITE, END, BEYOND)`，
    `:54` 又 `CREATIVE.without(FLUID_TANK)`——**五个等级全部拉黑**，比蒸汽机禁得更彻底。
  - 配套内容已上线：每级 `FLUID_TANK_CAPABILITY` / `FLUID_CAPABILITY` 配置
    （`TierConfigBase.java:9,15`，lang 里有对应 tooltip 键）；两个自定义流体 binder、pure_water
    已注册（`CMMRegistryEntry.java:65-66`）且被 `CMMMixingRecipeProvider` 的混合配方实际使用——
    **流体链路在游戏中是活的，唯独装它们的分级容器缺席**。
  - `MixinBoilerData`（已载入 `createmoremachines.mixins.json`）为 `CMMFluidTankBlockEntity`
    提供锅炉判定并配套蒸汽机，但因该 BE 永不注册而运行时惰性——储液罐与蒸汽机在 CMM 里是
    一对设计（罐当锅炉、机挂罐上），补齐时应一起立项。
- **做法**：同 ⑦，照 saw 先例重写；`AllBlocks.FLUID_TANK` 为 base，每级容量走
  `fluid_capability` 配置语义（-1 跟随 CMM）。
- **风险**：与 ⑦ 相同的上游协调问题；另外储液罐涉及流体容量与多方块结构，工作量高于蒸汽机。

## 不建议

### 物品排水器 Item Drain

- 【亲验】`javap`：`FILLING_TIME` 是 `public static final`（可读），但实际排水速率
  `itemMovementPerTick()` 是 **private**、入口 `tryInsertingFromSide` 也是 **private**——
  只能整类复制或 mixin，成本高于收益（与调研1 的 javap 结论一致，本次复核无误）。

## CMM 内置机器启用状态核查（2026-10-11 补查，回应"可能存在只有代码没有实际使用的机器"）

对上游 CMM 全部 95 个 Java 文件逐一对照注册链路核查，结论：**九种内置类型里有三种
"代码完整、配置齐全，但运行时永远不会注册"**，原报告把它们计入"CMM 已覆盖"是错误的。

| 类型 | 状态 | 证据 |
| --- | --- | --- |
| saw | 全等级拉黑 | `CMMTierPlugin.java:52` `SAW.withoutAll()`（缺口已由本插件 mechanical_saw 以新类型名补齐，无真空） |
| steam_engine | 全等级拉黑 | `CMMTierPlugin.java:50` `STEAM_ENGINE.withoutAll()`，注释 `//Temp disabled` |
| fluid_tank | 全等级拉黑 | `CMMTierPlugin.java:51,54` 四等级 + CREATIVE 分别 `without` |
| basin / depot / deployer / mixer / press / spout | 正常出货 | 无任何 `without` 调用；`CMMTierManager` 各注册链完整 |

- **禁用机制**：`without()` 把类型加入每级黑名单（`api/registry/CMMTier.java:107-117`），
  注册门槛 `shouldRegistry` 对黑名单类型一律 false（`CMMTier.java:488-508`）；
  `CMMTierManager` 的全部注册路径（`:136/:162/:201/:244/:282/:310/:352/:395`）都以它为前置。
- **黑名单不可解除**：`CMMTier.with()`（`CMMTier.java:122-125`）遇到已在黑名单的类型直接抛
  `UnsupportedOperationException`，且没有移除黑名单的 API——下游插件**无法重新启用**，
  只能像本插件对锯子那样自行重写。
- **附带发现（上游逻辑瑕疵）**：`shouldRegistry` 的白名单分支是死逻辑——类型不在黑名单时，
  最后的 `return !black` 恒为 true，即使白名单存在且不含该类型也会放行。实际生效的只有黑名单。
- **其余 CMM 代码完整性核查（全部有实际接线，无其他死机器）**：
  - 7 个 mixin 全部登记在 `createmoremachines.mixins.json`；
  - 5 个 datagen provider 全部带 `@RecipeGen` 注解，经 `CreateMoreMachines.gatherData`
    的注解反射装载（`CreateMoreMachines.java:88-101`）；
  - spout 的五个行为经 `CMMSpoutingBehaviours.registerDefaults()` 注册
    （`FMLCommonSetupEvent` → `CreateMoreMachines.java:84`）；
  - 自定义流体 binder / pure_water 被 `CMMMixingRecipeProvider` 配方使用；
  - 语言文件 block/fluid 键与注册实体一一对应，无幽灵机器。

## 避免重复覆盖清单

- CMM 内置**且实际出货**（不要重复做）：basin、depot、deployer、mixer、press、spout。
- CMM 有代码但被全等级拉黑：saw（本插件已补齐）、steam_engine、fluid_tank（后两者见候选 ⑦⑧，
  立项前先与上游确认）。
- 本插件已覆盖：crushing_wheel、mechanical_saw、encased_fan（本次新增）。
- 因此候选清单里没有 basin/depot 等的再变体。

## 核实方法说明

- 反汇编命令：`javap -c/-p -cp <create-328085-7963363.jar> <类全名>`（JDK 21，Ms-21.0.12）。
- 仓库文件核对以 `src/main/java/net/tomorrow325/createmoremoremachines/...` 与
  `CreateMoreMachines/src/main/java/net/yxiao233/createmoremachines/...` 的当前工作区为准。
- 2026-10-11 启用状态补查采用注册链路对照法：以 `BuiltInAdvancedMachineTypes` /
  `CMMTierPlugin` / `CMMTier.shouldRegistry` / `CMMTierManager` 为入口，逐一核对 CMM 全部
  95 个 Java 文件的引用关系（文件读取，非运行时验证）。
- 【未核】项在立项前应按同样方法先做 javap 核实，再进入定稿流程。
