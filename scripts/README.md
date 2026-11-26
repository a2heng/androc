# 构建脚本

这些脚本生成被 git 忽略的大资源，或做发版校验。

```text
app/src/main/jniLibs/arm64-v8a/libopencode2_bin.so   # fetch-opencode.py
app/src/oc/assets/runtime/rootfs.tar.xz          # fetch-runtime.py
```

在仓库根目录执行：

```sh
python scripts/fetch-opencode.py           # 按 opencode.version 下载
python scripts/fetch-opencode.py --latest  # 升到最新并改写 opencode.version
python scripts/fetch-runtime.py            # 从市场 OC 包提取 rootfs
python scripts/check-opencode-latest.py    # 发版前加 --strict
./gradlew :app:assembleThinDebug :app:assembleBundleDebug
```

`fetch-runtime.py` 默认读市场索引，下载 `kernel-opencode` 包，只取其中的
`rootfs/` 重新压缩；本地调试可用 `OPENCODE_PACK_FILE=<本地 tar.xz>`。

其它：

- `check-16kb.py`、`check-elf-align.py`：检查 ELF / APK 的 16 KB 对齐。
- `make-webview-shims.js`、`test-webview-shims.js`：WebView 兼容垫片。
- `fixtures/`：测试夹具。
