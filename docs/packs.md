# 市场包与发布

Androc 只从市场取两个包：`kernel-opencode` 和 `android`。包体积大、更新节奏独立，因此与 APK 分开发布。

## 1. 市场结构

| 文件 | 内容 |
|---|---|
| `index.json` | 当前包清单与版本 |
| `index.json.sig` | 对 `index.json` 原始字节的 Ed25519 签名（base64） |
| 包文件 | `<id>-<version>.tar.xz` |

默认索引：

```text
https://raw.githubusercontent.com/a2heng/androc-market/main/index.json
```

默认索引：App 只内置 Ed25519 公钥，验签通过后才解析；自定义镜像索引见 §5。

## 2. 包清单

| id | 内容 | 安装位置 | 使用方 |
|---|---|---|---|
| `kernel-opencode` | `opencode` 二进制 + `rootfs/` | `files/runtime/<upstreamVersion>/` | 本机 OpenCode（proot） |
| `android` | `skills/` + `rootfs/`（JDK 21、Android SDK 36、Gradle 9.9、静态 aarch64 aapt2/adb） | `files/packs/android/<version>/`；skills 复制到两个配置目录 | Android 构建与调试 |

`kernel-opencode` 解压后必须包含可执行的 `opencode`；`rootfs/` 提供 `opencode serve` 与 shell 的 musl 用户态。

## 3. index.json

示例（`size` 为占位）：

```json
{
  "generated": "2026-11-20T00:00:00Z",
  "upstream": { "opencode": "2.3.2" },
  "packs": [
    {
      "id": "kernel-opencode",
      "name": "OpenCode",
      "category": "runtime",
      "kind": "tar",
      "version": "2.3.2-1",
      "upstreamVersion": "2.3.2",
      "abi": "arm64-v8a",
      "installed": { "size": 0, "sha256": "…" },
      "archive": {
        "url": "https://…/kernel-opencode-2.3.2-1.tar.xz",
        "size": 0,
        "sha256": "…"
      }
    },
    {
      "id": "android",
      "name": "Android 开发",
      "category": "android",
      "kind": "tar",
      "version": "36.0-1",
      "upstreamVersion": "36.0",
      "abi": "arm64-v8a",
      "installed": { "size": 0, "sha256": "…" },
      "archive": {
        "url": "https://…/android-36.0-1.tar.xz",
        "size": 0,
        "sha256": "…"
      }
    }
  ]
}
```

| 字段 | 含义 |
|---|---|
| `generated` | 生成时间 |
| `upstream.opencode` | 上游 opencode 版本 |
| `packs[].id` | 包 id，App 按 id 查找 |
| `packs[].kind` | 目前只有 `tar` |
| `packs[].version` | 包版本 |
| `packs[].upstreamVersion` | 包内组件版本；kernel 为 opencode 版本 |
| `packs[].abi` | 仅接受 `arm64-v8a` |
| `packs[].installed.size/sha256` | 未压缩 tar 流的大小与哈希 |
| `packs[].archive.url/size/sha256` | 下载文件（tar.xz）的地址、大小与哈希 |

只做全量包：每次更新下载完整 `archive`。

## 4. 安装流程

| 步骤 | 行为 |
|---|---|
| 1 | 拉取 `index.json` 与 `.sig`，验签后解析 |
| 2 | 按 id 取包，检查 `kind=tar`、`abi=arm64-v8a` |
| 3 | 下载到 `files/market/` 下的临时文件（`.part`），边下边算 SHA-256 |
| 4 | 校验 archive 的 `size` + `sha256`，失败删除临时文件 |
| 5 | XZ 解压为 tar，校验 `installed.size` + `sha256` |
| 6 | 解压到 `<id>-<version>.tmp`，拒绝越界路径，保留符号链接与可执行位 |
| 7 | 替换目标版本目录，删除压缩包与临时文件 |

失败时旧版本保持不变；`kernel-opencode` 额外走健康检查与回滚（见 [`plugins.md`](plugins.md)）。

## 5. 签名与信任

| 项 | 做法 |
|---|---|
| 索引签名 | Ed25519，覆盖 `index.json` 原始字节；`.sig` 为 base64 |
| 公钥 | 编译进 App（`MarketClient.DEFAULT_PUBLIC_KEY`） |
| 下载校验 | 独立校验包的 `sha256` 与 `size` |
| 索引覆盖 | 默认索引强制验签；自定义索引用于镜像/调试，可关闭验签 |
| 信任边界 | 包内是可执行文件，安全等级等同 APK |

## 6. 发布流程

| 步骤 | 产物 |
|---|---|
| 1 | 组装 `kernel-opencode`：opencode 二进制 + rootfs，打成 tar |
| 2 | 组装 `android`：`skills/` + rootfs，打成 tar |
| 3 | 压缩为 `.tar.xz`，计算 archive 的 `size`/`sha256` |
| 4 | 计算未压缩 tar 的 `size`/`sha256`，写入 `installed` |
| 5 | 生成 `index.json` |
| 6 | 用 Ed25519 私钥签 `index.json`，上传包、索引和 `.sig` |

版本规则：

| 变化 | 更新 |
|---|---|
| opencode 升级 | `kernel-opencode` 的 `version` 与 `upstreamVersion` |
| Android 工具或 skills 升级 | `android` 的 `version` |
| 仅索引变化 | `generated`，包文件不变 |

## 7. 排查

| 现象 | 原因 | 处理 |
|---|---|---|
| `HTTP 404` | 索引或包地址失效 | 检查市场发布与 `archive.url` |
| `market index signature rejected` | 索引被改或公钥不匹配 | 重新签名并发布 `.sig` |
| `download hash mismatch` | 下载中断或被替换 | 删除 `files/market` 缓存后重试 |
| `installed hash mismatch` | 压缩包与索引不一致 | 重新生成索引 |
| `unsafe tar entry` | 包内含越界路径 | 重新打包 |
| `unsupported abi` | 设备不是 arm64-v8a | 使用支持该 ABI 的包 |
| `opencode binary missing in pack` | kernel 包缺二进制 | 检查打包根目录 |

## 8. 代码位置

| 责任 | 文件 |
|---|---|
| 索引解析 | `market/src/main/kotlin/com/a2heng/androc/market/MarketIndex.kt` |
| 验签、下载、解压、安装 | `market/src/main/kotlin/com/a2heng/androc/market/MarketClient.kt` |
| android 包安装与 skills 复制 | `app/src/main/java/com/a2heng/androc/PackInstaller.java` |
| opencode 更新、健康检查、回滚 | `app/src/main/java/com/a2heng/androc/RuntimeUpdateService.java` |
| rootfs 定位 | `app/src/main/java/com/a2heng/androc/MuslRootfs.java` |
| 索引地址覆盖 | `app/src/main/java/com/a2heng/androc/MarketConfig.java` |
