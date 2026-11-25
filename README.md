# 紧凑人造太阳 · Compact Mekanism Sun

把 Mekanism Sun（`mekanismsun`）里 **7×7×7 多方块「人造太阳」压成一台单方块机器**的 Mekanism 附属。

纯ai生成,兼容性未测试

## 依赖

| 组件 | 版本要求 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.253 或更高 |
| Mekanism | 10.7.11 或更高（开发时为 10.7.19.85） |
| **Mekanism Sun**（`mekanismsun`） | **1.3.1 或更高（必需）** |

`mekanismsun` 是硬依赖：本 mod 直接使用它注册的氦（Helium）化学品，未安装时游戏会在加载阶段报缺依赖。

## 安装

1. 安装 NeoForge 21.1.253+（Minecraft 1.21.1）。
2. 把以下三个 jar 放进 `mods/`：
   - `Mekanism-1.21.1-10.7.19.85.jar`（或更高）
   - `mekanismsun-1.21.1-1.3.1.jar`（或更高）
   - `compactmekanismsun-neoforge-1.21.1-0.1.0.jar`
3. **客户端和服务端都要装**（`neoforge.mods.toml` 里依赖声明为 `side = "BOTH"`）。

## 机器

注册名：`compactmekanismsun:compact_artificial_sun`（创造模式标签「紧凑人造太阳」）

| 面 | 默认用途 |
|---|---|
| 顶 | 输入氢 |
| 底 | 输出氦 |
| 其余面 | 输出能量 |

全部可用 Mekanism 的侧面配置器改。工作时方块光照为 15（依据 `active` 状态自动切换）。

### 数值

**能量单位是 J（Joule），不是 FE。** Mekanism 的能量容器内部一律按 Joule 存，上游配置项填的也是 J；
界面默认按 FE 显示，换算走 `general.feConversionRate`（默认 **2.5**，即 `J = FE × 2.5`，`1 FE = 2.5 J`）。
所以配置里填 2,048,000,000，能量信息页上显示的是 819,200,000 FE —— 这是换算，不是数值不符。

| 项 | 默认值 | 配置键 |
|---|---|---|
| 氢容量 | 256,000 mB（256 B） | `fuelCapacity` |
| 氦容量 | 256,000 mB（256 B） | `wasteCapacity` |
| 每 mB 氢产能 | 2,048,000,000 J（= 819,200,000 FE） | `energyPerHydrogen` |
| 内部能量缓存 | 4,096,000,000,000 J（= 1.6384 T FE），就是配置值本身 | `energyCapacity` |
| 燃烧速率上限 | 100 mB/t | `maxBurnRate` |

换算成发电量（默认配置，同一行给出两种单位）：

| 燃烧速率 | 输出（J） | 界面读数（FE） |
|---|---|---|
| 1 mB/t（默认） | 2.048 GJ/t | 819.2 M FE/t |
| 10 mB/t | 20.48 GJ/t | 8.192 G FE/t |
| 100 mB/t（满档） | 204.8 GJ/t | 81.92 G FE/t |

氦按 **1 mB 氢 → 1 mB 氦** 产出。

### 停机条件

以下任意一条成立，机器就停止燃烧（不会白烧氢）：

- 氢槽空
- 能量缓存满
- **氦槽满**（产物出不去，继续烧只会浪费氢）
- 燃烧速率设为 0
- **红石信号不满足**（见下）

### 红石控制

Mekanism 标准的红石控制 tab，三种模式：**禁用 / 高电平 / 低电平 / 脉冲**。

默认「禁用」= 一直允许燃烧。切换成「高电平」后，只有收到红石信号才烧；「低电平」反之。

> 太阳模式不受红石控制——它是独立开关，否则切断红石就没法调时间了。

### 界面

- 左侧三个表：氢、氦、能量
- 信息屏两行：**运行状态** / **设定速率**
- 文本框：输入燃烧速率（范围 `0 ~ maxBurnRate`，回车或勾号确认）
- 按钮：太阳模式开关
- 左侧 tab：侧面配置、运输配置、太阳模式、**温度**、**能量**

温度与能量 tab 沿用 Mekanism 原生的灰色浮层样式，与其它机器一致。

### 温度

机器有一个「堆芯温度」：

- 环境温度 300 K，满速燃烧时趋近 5000 K
- 按**实际燃烧量**计算（不是设定值），停机后自然降温
- 一阶惯性过渡，不会突变
- 会写进存档，重进不会重置

温度目前只作为状态显示，不影响产出。

### 太阳模式

开启后有「正午 / 夜晚」两档，直接把世界时间钉住，并停止下雨。

> 多人服务器注意：这台机器改的是**整个维度**的时间和天气。默认关闭，需要在界面里手动开启。

## 配置

文件：`config/compactmekanismsun/general.toml`

配置类型是 **SERVER**，所以：

- 单人游戏：`saves/<存档名>/serverconfig/compactmekanismsun/general.toml`（改根目录的 `config/` 不生效）
- 专用服务器：`<服务器目录>/serverconfig/compactmekanismsun/general.toml`

## 已知限制

- **没有 CC:Tweaked 计算机集成**。Mekanism 的 `@ComputerMethod` 依赖编译期注解处理器生成工厂类，本项目未配置，手写绑定也无法在本机验证。
- **没有自定义音效**，开关机沿用 Mekanism 富集机的声音。
- 纹理为脚本程序化生成（复刻上游人造太阳外壳的配色与结构），不是美术资源。
- 与 `compactmekanismmachines`、`compactmekanismmachinesplus` 无冲突（方块与注册名互不重叠）。

## 从源码构建

```bash
JAVA_HOME=<JDK 21> ./gradlew build
```

产物：`build/libs/compactmekanismsun-neoforge-1.21.1-0.1.0.jar`

构建需要 `mekanismsun` 的 jar 作为 `compileOnly` 依赖，路径由 `gradle.properties` 的 `mekanismsun_jar` 指定（当前 `libs/mekanismsun-1.21.1-1.3.1.jar`，相对 `mcmod/mekext/`；工作区根目录的副本已归档到 `refmods/`）。

## 许可

MIT
