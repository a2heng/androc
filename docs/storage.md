# 存储：应用目录、外部目录与“能不能放到外面”

本文回答三个问题：

1. 规范允许 Androc 用哪些目录？
2. 能不能用外部目录（`/sdcard`、用户选的文件夹）而不申请全盘权限？
3. 把运行时/工具链放到外面行不行？

结论先行：

> **运行时、JDK/SDK/Gradle、内置组件包必须留在应用私有目录。**
> **项目、导入导出、pack 缓存可以用外部目录，但走 SAF / MediaStore / OBB，
> 不要假设可以直接 `exec` 外部文件。**

---

## 1. 目录选项

| 方案 | 典型路径 | 权限 | 能直接 exec | 卸载后保留 | 适合 |
|---|---|---|---|---|---|
| 内部私有 | `/data/data/<pkg>/files` | 无 | 仅 `nativeLibraryDir` 可直接 exec；其余经 proot | 否 | HOME、runtime、packs |
| 外部私有 | `/sdcard/Android/data/<pkg>/files` | 无 | 否 | 否 | 大文件缓存、日志、导出中转 |
| SAF 目录树 | 用户选，如 `Documents/Androc` | 用户授权 + persistable | 否 | 是 | 项目源、导入/导出 |
| MediaStore | `Downloads/`、`Documents/` | 按集合 | 否 | 是 | 导出 APK、日志、ZIP |
| 全盘文件访问 | `/sdcard` | `MANAGE_EXTERNAL_STORAGE`（特殊授权） | 否 | 是 | 高级用户/ sideload，不作为默认 |
| OBB | `/sdcard/Android/obb/<pkg>` | 无 | 否 | 否 | 大只读资源、离线 pack |
| root / system | 任意 | root | 是 | 是 | 不用于普通分发 |

关键点：

- **`exec` 和目录是两件事**。共享存储通常是 `noexec`，SELinux 也不允许
  从 `/sdcard` 执行 ELF；即使文件可读，也不能跑。
- Android 10+ 的 scoped storage 下，`WRITE_EXTERNAL_STORAGE` 对 target 34
  基本无效；`requestLegacyExternalStorage` 也被忽略。
- `MANAGE_EXTERNAL_STORAGE` 不是普通权限：用户要去系统设置里手动开，
  Google Play 对一般应用限制很严；侧载可以，但要明确警告。

---

## 2. 规范允许的“外部目录”做法

### 2.1 SAF：推荐的外部项目目录

```java
Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
startActivityForResult(i, REQ_TREE);
// onActivityResult:
getContentResolver().takePersistableUriPermission(
    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
```

之后用 `DocumentFile` / `ContentResolver` 读写。特点：

- 用户明确选择目录，不需要全盘权限；
- 权限可持久化，重启后仍可用；
- 但它是**文档 API**，不是普通文件路径；`File`/`exec`/symlink 都不适用；
- 适合“项目源”“导入/导出”，不适合当 `HOME` 或 `PREFIX`。

Androc 的做法：

1. 设置里选“外部项目目录”（SAF）；
2. 导入时把选中的项目**复制/同步**到私有 `workspace/`；
3. 在私有目录里构建；
4. 产物通过 SAF/MediaStore 导出回外部目录。

### 2.2 MediaStore：导出，而不是工作目录

- 导出 APK/日志/ZIP 到 `Downloads` 或 `Documents`；
- 不需要权限（Android 10+ 用 `MediaStore.Downloads`）；
- 不适合频繁读写的项目树。

### 2.3 OBB：大只读 pack

- `/sdcard/Android/obb/<pkg>/` 不需要运行时权限，容量大；
- 适合把 `kernel.pack` / `tools.pack` 放进去，安装时复制到私有目录；
- 卸载会删除；侧载用户可以用 `adb push` 放进去；
- 不能从 OBB 直接执行。

### 2.4 全盘访问：可选的“高级模式”

如果确实要直接读写 `/sdcard/Androc`：

- manifest 声明 `MANAGE_EXTERNAL_STORAGE`；
- 引导用户到 `Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`；
- 仍然不能从 `/sdcard` 执行 ELF；
- 默认关闭，界面明确写“仅侧载/自担风险”；
- Play 分发不要开。

---

## 3. 为什么运行时不能放外面

| 需求 | 外部目录的问题 |
|---|---|
| 执行 `proot`/`opencode` | 共享存储 noexec；SELinux 不允许 |
| rootfs 里的 ELF + `DT_RUNPATH` | 需要固定路径与 bind，靠 proot 提供 |
| symlink、权限位、`chmod +x` | SAF/FUSE 不支持完整 POSIX 语义 |
| Gradle 的临时文件/锁 | 外部目录慢，且可能被其他 App 改 |
| 升级/回滚 | 私有目录可以原子 rename；外部目录不可靠 |

所以：**外部目录只做“入口/出口”，不做运行时。**

---

## 4. 有没有“在外部做”的其他办法

有，分两类。

### 4.1 存储放外部

- SAF 项目目录（推荐）；
- OBB 放 pack；
- MediaStore 导出产物；
- 外部私有目录放缓存（`getExternalFilesDir`）。

### 4.2 计算放外部

- **远程 opencode**：电脑/服务器跑 `opencode serve`，手机只做 UI；
  Androc 的 UI-only 状态就是为这个准备的。
- **电脑构建**：项目在电脑上构建，APK 通过 `adb`/SAF 传回手机；
- **混合**：项目放 SAF/同步盘（Syncthing/Nextcloud），手机导入后本地构建，
  或直接连远程服务器构建；
- **ADB**：`adb push` 到应用私有目录（开发/调试）。

这些都不违反 Android 规范，也不要求全盘权限。

---

## 5. “不超过这个目录”是什么意思

Android **没有**“每个应用目录固定配额”这种限制；能用多少取决于设备剩余空间。
可能相关的是：

| 限制 | 说明 |
|---|---|
| 设备剩余空间 | 真正的上限；安装 pack 前要检查 |
| 应用缓存 | 系统可在低存储时清理 `cacheDir`；不要把唯一副本放缓存 |
| 外部私有目录 | 卸载即删除；用户也可能在文件管理器里清掉 |
| Play 包体限制 | 只影响上架；侧载没有 200 MB 限制 |
| SAF 目录配额 | 由用户/云盘决定，Android 不强制 |

如果要做“自限额度”，在设置里加：

```text
最大运行时占用：2 GB / 4 GB / 不限制
低空间阈值：安装前检查，低于 1 GB 拒绝
```

---

## 6. 推荐落地

1. `HOME`、`runtime`、`runtime-core`、`packs`：**内部私有**；
2. 大文件下载缓存：`getExternalFilesDir("downloads")` 或 OBB；
3. 项目：默认私有 `workspace/`；可选 SAF 目录 + 导入/导出；
4. 导出 APK/日志：MediaStore；
5. `MANAGE_EXTERNAL_STORAGE`：只作为高级开关，默认关闭；
6. 任何外部文件先复制到私有目录，再交给 proot/构建；
7. 不在外部目录执行任何 ELF。
