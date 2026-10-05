# CreateMoreMoreMachines

Minecraft 1.21.1 / NeoForge mod，[Create](https://github.com/Creators-of-Create/Create) 的机器扩展，建立在 [CreateMoreMachines](https://github.com/Y-Xiao233/CreateMoreMachines) 之上（必须依赖）。

## 环境要求

- **Java 21**（本机已装：Temurin 21.0.12）
- **Gradle 8.8**（仓库自带 wrapper，已缓存于本地）

## 必须依赖：CreateMoreMachines

该 mod 目前**没有公开发布渠道**（无 GitHub Releases、不在 Modrinth / CurseForge），所以开发流程是：

1. 本仓库内附带其源码克隆 `CreateMoreMachines/`（已加入 .gitignore）
2. 首次构建前先把它发布到本地 maven 仓库：

   ```bash
   cd CreateMoreMachines
   ./gradlew publishToMavenLocal
   ```

3. 之后本项目即可通过 `mavenLocal()` 解析 `net.yxiao233:CreateMoreMachines:1.21.1-2.7`
   （注意 artifactId 是大写驼峰；若上游更新，在 `gradle.properties` 里同步修改
   `createmoremachines_version` 并重新执行第 2 步）

运行时依赖通过 `src/main/templates/META-INF/neoforge.mods.toml` 中
`type = "required"` 的 `createmoremachines` 条目强制生效。

## 常用命令

| 命令 | 作用 |
|---|---|
| `./gradlew build` | 构建 jar（输出在 `build/libs/`） |
| `./gradlew runClient` | 启动开发客户端 |
| `./gradlew runData` | 生成数据包（data generation） |
| `./gradlew runServer` | 启动开发服务端 |

## 版本信息

| 项 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219（与 CreateMoreMachines 一致；最新 21.1.x 见 [maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/)，改 `neo_version` 即可升级） |
| Create | 6.0.10（cursemaven） |
| Parchment | 2024.11.17 |
| modid | `createmoremoremachines` |
| 主类 | `net.yxiao233.createmoremoremachines.CreateMoreMoreMachines` |

## IDE

用 IntelliJ IDEA 打开项目根目录即可（会自动执行 ModDevGradle 的 ideSync），
或命令行 `./gradlew genIntellijRuns` / `genEclipseRuns` 生成运行配置。
