# 离线打包脚本

这些脚本生成那些体积大、被 git 忽略的资源，让 APK 不联网也能用：

```text
app/src/main/jniLibs/arm64-v8a/libopencode2_bin.so   # fetch-opencode.py
app/src/full/assets/toolbox/*.deb                    # fetch-toolbox.py
app/src/full/assets/toolchain/toolchain.zip          # make-toolchain.py
```

在仓库根目录执行：

```sh
python scripts/check-opencode-latest.py   # 发版前必须 --strict
python scripts/fetch-opencode.py          # 读取 opencode.version
python scripts/fetch-opencode.py --latest # 升到最新并改写 opencode.version
python scripts/fetch-toolbox.py
python scripts/make-toolchain.py
./gradlew :app:assembleDebug
```

`opencode.version` 是内置版本的唯一来源；Gradle、下载脚本和 release CI 都读它。
发版前 `check-opencode-latest.py --strict` 必须通过，否则不要打 tag。

## 镜像

`fetch-toolbox.py` 默认用清华 TUNA 的 Termux 镜像，失败时依次回退到中科大、
北外和官方 `packages.termux.dev`。也可以手动指定：

```sh
python scripts/fetch-toolbox.py --repo https://mirrors.ustc.edu.cn/termux/apt/termux-main
```

在受限网络下下载 GitHub 内容时，在环境变量里设代理（Python 的 `urllib` 认
`HTTPS_PROXY`；地址换成你自己的）：

```sh
HTTPS_PROXY=http://127.0.0.1:<port> python scripts/fetch-opencode.py
```

## WebView 垫片

v2 UI 需要一些「不更新的 WebView 没有」的 JavaScript API
（`URL.canParse`、`Promise.try`、`toSorted` 等）。两个经典脚本在 SPA bundle
之前执行，由 `MainActivity.injectEarlyScript` 注入 `<head>`：

- `app/src/full/assets/oc-corejs.js`：只含所需模块的 core-js 打包产物
  （见 `scripts/make-webview-shims.js`）；
- `app/src/full/assets/oc-early.js`：`/provider` 缓存、picker 路径，以及
  core-js 覆盖不到的 `AbortSignal` 垫片（core-js 只管 JS，不管 DOM）。

改了 `MODULES` 之后要重新生成 polyfill（第一次会下载 `core-js-builder`
和 `esbuild` 到临时目录）：

```sh
node scripts/make-webview-shims.js
```

不用模拟器也能检查两个脚本（删除 Node 里的现代 API，期待 polyfill 把它们补回来）：

```sh
node scripts/test-webview-shims.js
```

## 说明

- `fetch-toolbox.py` 从 Termux 的 `Packages.gz` 索引里解析依赖，把每个 `.deb`
  下载成 `<package>.deb`，并为 App 的离线安装器写出 `bootstrap.txt` +
  `packages.txt`。
- `openjdk-21` 会带出音频/X11 依赖；包含它们是为了让 `dpkg` 能配置 JDK。
  需要更小的构建时，可以从 `DEFAULT_SET` 里去掉 `openjdk-21` 以后再装，
  但那样 APK 就不再是完全离线。
- `make-toolchain.py` 打包 Android platform 36、aarch64 aapt2 35.0.2、
  Gradle 9.9.0 和本地 Gradle 依赖缓存。缓存是可选的；没有缓存时，除非项目
  依赖已经缓存过，否则 Gradle 需要联网。
- 生成的目录都被 git 忽略。不要把 GPL 二进制提交进 MIT 源码树。
