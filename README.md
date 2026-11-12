# PneumaticCraft: Working Medium（气动工艺：工质）

[PneumaticCraft: Repressurized](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized) 的化工厂附属模组。塔、釜、压缩机柱、廊道和洁净室都靠压力、流量、温度和**工质**运转。每升一个等级，工质就会换一种：空气、油品、氮气、氩气，最后是超临界二氧化碳。

A PneumaticCraft chemical-plant addon. Towers, kettles, columns, galleries and rooms run on pressure, flow, temperature and a working medium. Each grade changes that medium, and the last grade holds supercritical carbon dioxide inside its window.

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264 或更高 |
| PneumaticCraft: Repressurized | 8.2.0 或更高（开发用 8.2.24） |
| JEI | 19 或更高，可选，用于查看结构和配方窗口 |

安装：从 [Releases](https://github.com/nian-ran/PneumaticCraft-Working-Medium/releases) 下载 `workingmedium-<版本>.jar`，放进实例的 `mods` 文件夹。

## 玩法概要

- **六个等级。** G0 是 5 bar，G1 是 10，G2 是 20，G3 是 50（要工业管），G4 是 120（要深冷管）。G5 不追求更高压力，而是守住临界釜的 70–80 bar 和 25–40 °C。弱管接到强干管上会炸，壳体等级取最弱的一块。
- **多方块机器。** 冲击破碎室、旋风分离器、洗选塔、热分釜、蒸馏塔、裂化炉、聚合釜、浸出线、压缩机柱、膨胀机、换热廊、深冷塔、洁净室、临界釜、标定台等。控制器放置时正面朝外，结构沿这个方向生长。塔类和廊道类结构可以加高、加长来提高产出。
- **管网附件。** 减压阀降一档，差额变成热；增压器用四份低压气换一份高压气；泄压阀按设定放空；表头并进管网读数。压缩机列、膨胀机列和调节站的背面只用来取气或送气，不和管网直接连通，所以能真正维持和气源不同的压力。
- **配方窗口。** 每个配方都有压力和温度窗口。离开窗口时，这一炉会报废或者机器停机。出口满了机器就停，不会自动丢弃任何东西。
- **润滑与密封。** 从 G1 起，热机器要消耗润滑液，密封槽里要放密封，否则会漏气。等级越高的密封越耐用（G2 1000 刻，每高一级多 500 刻）。润滑液接进任意流体口都会进润滑罐。
- **自动化。** 物品输入口会把密封、催化剂、电极、无人机程序片、叶片泵和滑轨送进专用槽。接在控制器上的漏斗只会从输出格取走物品、往输入格放入物品。配方机器控制器可以接比较器读本批进度，储气库、压缩机列和调节站读壳体压力；表头、表头口、泄压阀和减压阀/增压器也能输出比较器信号。一批的全部产物都放得下才会开工，拆掉控制器会掉出里面的物品和整桶流体。气体口只接受气体，流体口只接受液体。热口可以接热管。
- **认证与升级。** 标定台可以反复写出稳态认证；对已成形的机器使用后，耗气和密封磨损各减少 25%。流体循环回路里的叶片泵、物品廊里的滑轨会按等级提高吞吐量。充气站和无人机巢会把已经充满的物品送到输出格。
- **副产物去处。** 酸雾可以用碱液中和成盐水，含尘蒸汽分成水和石粉，副金属溶液电积出铜和金并回收盐酸，炉渣和滤饼能回收铁粒，氢气能在联合循环岛里发电产气。火炬可以烧掉各种废气。四张废片能烧结回层压基板，金属灰在惰性气体下烧结能得到两份催化剂。沥青是一条更快的路。
- **产业链。** 矿石洗选和浸出可以做到双倍或三倍矿；原油经热压、蒸馏、裂化和聚合得到燃料、蜡、单体和工程塑料；深冷塔分出氮气、氩气、氧气和液体二氧化碳，供洁净室和临界釜使用。
- **无人机。** 编程片 `workingmedium_read_gauge` 读取表头，`workingmedium_hold_window` 在机器处于窗口内时停留。
- **游戏内资料。** 合成「工艺规程」查看全部说明。安装 JEI 后，可以看到每台多方块的最小结构示意和完整的配方窗口。

## 从源码构建

需要 Java 21。

```bash
./gradlew test
./gradlew build
```

Windows 上用 `gradlew.bat`。构建出的 jar 在 `build/libs/`。

贴图和模型由 `python3 tools/gen-assets.py` 生成，音效由 `python3 tools/gen-sounds.py` 生成。贴图、模型和音效都是本模组原创，没有复制气动工艺官方的资源。

## 许可

[GNU General Public License v3.0](LICENSE)。
