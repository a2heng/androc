# 轻量 Linux 运行时：不重写 Termux，做 Rootfs 管理

状态：`[研究/远期]`。目标：**比 Termux 轻很多，同时是完整 Linux 用户态，并且尽量高效。**

---

## 1. 先说结论

不要 fork/reimplement Termux。Termux 重，是因为它带的是：

- bionic 用户态 + 一整套 Termux 补丁；
- `pkg/apt` 与大量 `.deb`；
- JDK、git、python 等完整包；
- 为了 Android 兼容保留的 bionic `linker64` 路径。

更合适的路线：

> **做一个 distro-agnostic 的 Rootfs Manager；默认 Alpine，Android 构建需要时用
> Termux bionic 子环境；有 root 时可选 chroot 快路径。**

这样：

- 基础 Linux：Alpine minirootfs，约 8–10 MB，`apk` 按需装包；
- 完整度：shell、coreutils、apk、git、python、服务都能有；
- 效率：普通命令在 Alpine/proot；Android 构建用 bionic 原生工具链；
- 不再维护一份巨大的 Termux 前缀作为唯一选择。

---

## 2. 为什么 Termux 重（实测）

当前 debug 安装（`com.a2heng.androc.debug`）：

| 路径 | 体积 | 归属 |
|---|---:|---|
| `files/usr`（Termux 前缀） | **1.1 GB** | 混合 |
| `files/usr/lib/jvm`（JDK 21） | **244 MB** | **Android 插件** |
| `files/home/toolchain`（SDK/Gradle/Maven） | **643 MB** | **Android 插件** |
| `files/usr/bin`（node/clang/cmake/git…） | 239 MB | 混合（node/clang/cmake 属 Android/工具） |
| `files/usr/share`（man/doc/locale…） | 98 MB | 可按需 |
| `files/usr/lib/python3.14` | 43 MB | **Kernel** |
| `files/usr/bin/git` | 3.4 MB | **Kernel** |
| opencode 二进制（jniLibs） | 237 MB | **Kernel** |

结论：

- **JDK + SDK/Gradle 是最大头**（约 887 MB），应该整体归 Android 插件；
- `node/clang/cmake` 也不是 kernel 必需，归 Android/工具插件；
- Kernel 真正需要的是：OpenCode + git + Python + bash/coreutils/脚本工具，
  大约 **300–400 MB**（其中 OpenCode 237 MB）；
- 所以“完整分离”能立刻把基础安装从 1.1 GB 降到 300–400 MB；
- 再换 Alpine 作为 kernel rootfs，可以进一步降到 ~300 MB 左右（含 OpenCode）。

---

## 3. 方案对比

| 方案 | 体积 | 完整 Linux | 效率 | root | 结论 |
|---|---:|---|---|---|---|
| Android `toybox` + `/system/bin/sh` | 0 | 否 | 最高 | 否 | 只能做极简 shell |
| 静态 BusyBox + 少量静态工具 | 1–10 MB | 否（无包管理） | 高 | 否 | 轻，但不完整 |
| **Alpine minirootfs + proot + apk** | **8–10 MB 起** | **是** | 中（proot 开销） | 否 | **推荐基础** |
| Debian/Ubuntu minbase + proot | 30–100 MB | 是 | 中 | 否 | 更重，glibc 兼容好 |
| Termux bionic + proot（当前） | 1.1 GB | 是 | 中高（bionic） | 否 | 保留给 Android 构建 |
| root + chroot | 同 rootfs | 是 | 最高 | 是 | 可选快路径 |
| 自建 Buildroot/Yocto | 最小 | 取决于配置 | 高 | 否/是 | 维护成本高 |

“完整 Linux”指：POSIX shell、coreutils、包管理器、git/python、可装服务；
不要求 bionic/Android 工具链。

---

## 4. 推荐架构：Rootfs Manager

```text
runtime 插件
├─ proot 引擎（APK 内，必须）
├─ RootfsManager
│  ├─ alpine/          # 默认：轻量、apk
│  ├─ termux-bionic/   # Android 构建：JDK/aapt2 已验证
│  └─ debian/          # 可选：glibc 兼容
├─ rootfs.json         # distro、packages、mounts、env
└─ session → rootfs    # 每个会话/插件选择一个 rootfs
```

规则：

- **不是按命令名路由**（不回到 `PROOT_COMMANDS` 老路）；
- 整个会话跑在选定的 rootfs 里；切换插件/会话 = 切换 rootfs；
- 插件 manifest 声明 `rootfs: alpine` 或 `termux-bionic`；
- Android 插件声明 `termux-bionic`，写作插件声明 `alpine`；
- rootfs 由 runtime 插件安装/校验/回滚，插件只给清单。

### 4.1 分层归属（按你的划分）

| 组件 | 归属 | rootfs |
|---|---|---|
| OpenCode、git、Python、bash/coreutils、grep/sed/awk、curl/wget、ssh | **Kernel** | Alpine |
| JDK 21、Android SDK、aapt2、Gradle、adb、clang/node/cmake | **Android 插件** | termux-bionic |
| typst、字体、模板 | Writing 插件 | Alpine（共享） |
| ONNX/TFLite/llama.cpp 模型 | AI 插件 | Alpine 或 Android |
| 社区 skill | 各自插件 | 按依赖选择 |

这样：

- **kernel 不再带 JDK/SDK**，基础体积从 1.1 GB 降到 300–400 MB；
- Android 插件只在安装时带 887 MB 左右的大头；
- Writing 复用同一个 Alpine rootfs，不重复装 bash/git/python；
- 同一个 `$HOME/workspace`、`$HOME/.config/opencode/skills` 绑定到所有 rootfs，
  项目/技能不复制。

### 4.2 Termux 能不能替代

| 场景 | 结论 |
|---|---|
| Kernel（OpenCode/git/Python/脚本） | **能**：Alpine + apk 更小、完整 |
| Android 构建（JDK/aapt2） | **暂不替代**：JDK/aapt2 是 bionic/glibc，Termux bionic 已验证；作为 Android 插件的专用 rootfs |
| 极端轻量、不需要包管理 | 静态 BusyBox，但不完整 |
| 有 root | chroot 快路径，rootfs 不变 |

所以不是“重写 Termux”，而是**把 Termux 降级为 Android 构建专用 rootfs**，
kernel 换成 Alpine；两者共享 HOME/workspace，但库和二进制各自隔离。

---

## 5. 效率

proot 的 syscall 翻译是主要开销；不同 rootfs 的差异在用户态：

| 场景 | 建议 | 原因 |
|---|---|---|
| 写作/研究/一般 shell | Alpine + proot | 体积小、启动快、apk 按需 |
| Android 构建 | Termux bionic + proot | JDK/aapt2 已验证；bionic 原生 |
| 高频小命令 | 同一个 rootfs 内；尽量合并脚本 | 减少 proot 启动次数 |
| 有 root | 可选 chroot 后端 | 去掉 proot 翻译 |
| 大文件 I/O | 绑定 `$HOME` 到 rootfs | 避免复制 |

不要用“静态 BusyBox 跑一部分、proot 跑另一部分”的混合命令路由；那会把
路径/linker/命令分类问题重新引回来。

### 5.1 必须实测的基准

在同一台手机上比较：

```sh
# 启动
time proot -r <rootfs> /bin/sh -c true
# 小文件遍历
time find /usr -type f | wc -l
# 文本
time grep -r "needle" /usr/share >/dev/null
# git
time git -C <repo> status
# python
time python3 -c 'import json,sys'
# 构建
time ./gradlew :app:assembleDebug --offline
```

记录：体积、启动、CPU、内存、Gradle 时间。没有数据不拍板。

---

## 6. 体积预算

| 层 | 内容 | 目标 |
|---|---|---|
| proot 引擎 | proot + loader + musl loader | 2–3 MB |
| Alpine base | minirootfs + apk + ca-certificates | 8–15 MB |
| 常用 Linux | bash/coreutils/git/python | 80–150 MB |
| Android 构建 | JDK/SDK/Gradle/aapt2 | 600–900 MB（按需） |
| 写作 | typst + 模板/字体 | 30–80 MB |
| 研究 | pdftotext/ripgrep/embeddings | 100–200 MB |

相比现在 1.1 GB 前缀 + 266 MB `.deb`，基础用户态可以降到 **几十 MB**；
大头只在安装 Android 插件时出现。

---

## 7. 兼容性风险

| 风险 | 说明 | 对策 |
|---|---|---|
| `aapt2` | Android SDK 的 aapt2 是 bionic/glibc 二进制，Alpine musl 不能直接跑 | Android 插件用 termux-bionic rootfs；或测试 gcompat/静态 aapt2 |
| JDK | Alpine OpenJDK 是 musl；Gradle 可用，但 Android 工具链兼容性要测 | 先保留 Termux bionic JDK |
| DNS | `apk` 需要 `/etc/resolv.conf` | bind 真实 resolv.conf |
| TLS | `apk` 需要 ca-certificates | 预装 |
| 16 KB | musl/新二进制对齐 | 打包前跑 `check-16kb.py` |
| proot | 性能/稳定性 | 基准 + 失败回退 |
| 许可 | Alpine/BusyBox/apk 各自许可 | 作为下载产物分发，保留 LICENSE |
| 存储 | 多个 rootfs 占空间 | 只装需要的；可卸载 |

---

## 8. 落地阶段

| 阶段 | 内容 | 验证 |
|---|---|---|
| A | RootfsManager + Alpine minirootfs + proot + apk | 终端能 `apk add bash git python3` |
| B | 基准：Alpine vs Termux bionic | 体积/启动/Gradle 数据 |
| C | Android 插件用 termux-bionic；写作插件用 Alpine | 构建成功 + 写作导出 PDF |
| D | 可选 root chroot 快路径 | 同接口、无 root 也能跑 |
| E | rootfs 按需下载/卸载/离线导入 | 不装 Android 时基础 < 200 MB |

---

## 9. 一句话回答

**可以做得比 Termux 轻很多，也保持完整 Linux 用户态；关键是按你的划分拆开：**

- **Kernel**：OpenCode + git + Python + bash/coreutils/脚本 → Alpine rootfs；
- **Android**：JDK + SDK + Gradle + aapt2 + adb → Termux bionic rootfs，按需安装；
- **Writing/AI/社区**：复用 Alpine 或各自 rootfs；
- 有 root → 可选 chroot；
- 不重写 Termux，只做 Rootfs Manager。

这样基础安装从 1.1 GB 降到约 300–400 MB，Android 的大头只在需要时出现。
