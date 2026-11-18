# `:kernel` — opencode on the phone

Owns everything that makes `opencode` run inside an Android app. Nothing here
draws UI or builds Android projects.

## Responsibilities

- `opencode` musl binary + musl loader (from `jniLibs`)
- proot, bind table, `PREFIX`, `HOME`, `TMPDIR`, `/etc/hosts`
- toolbox essentials (`bash`, `readline`, …), offline repair, `pkg/apt`
- server process, port, password, foreground service, health/watchdog
- standard pairing (`POST /api/pair` → `/auth/connect/<code>`)
- workspace, logs, transient-cache pruning, runtime updates
- optional local discovery provider (`DiscoveryContract`)

## Public API (target)

```java
OpenCodeKernel kernel = OpenCodeKernel.create(context, KernelConfig.builder()
        .port(4096)
        .opencodeVersion(BuildConfig.OPENCODE_VERSION)
        .build());
kernel.start();
RuntimeConnection c = kernel.connection();   // null until READY
String link = kernel.pairingLink();           // official app / other clients
```

`OpenCodeKernel` implements `RuntimeController` from `:contracts`.

## Migration map (phase 2)

Move from `app/src/main/java/com/opencode/launcher/`:

| File | Note |
|---|---|
| `LocalRuntime.java` | server start/stop, HOME, pairing; replace `BuildConfig` with `KernelConfig` |
| `LocalToolbox.java` | prefix repair, `.deb` merge, essentials |
| `CommandResolver.java` | generated `oc-*` helpers |
| `LocalServerService.java` | foreground service |
| `ToolboxService.java` | first install / self-test |
| `ToolboxServer.java` | `/doctor`, `/repair` endpoints |
| `ServerWatch.java` | health watchdog |
| `RuntimeUpdateService.java` | opencode binary updates |
| `LocalNetworkProxy.java` | DNS/proxy shim |
| `LocalServerInfoProvider.java` | optional discovery (`DiscoveryContract`) |
| `NativeRuntimeConfig.java` | proot env |
| `BootReceiver.java` | restart after boot |
| `AppLog.java` | shared logger (app may keep using it) |

Manifest entries move with it: `LocalServerService`, `ToolboxService`,
`RuntimeUpdateService`, `BootReceiver`, `LocalServerInfoProvider`, watchdog.

Assets may stay in the full app module: Android merges every module's assets
into one `AssetManager`, so kernel code can read `assets/toolbox/...` no matter
which module ships it.

## Must not depend on

`:ui`, `:tools`, Compose, Android SDK/Gradle, `com.a2heng.androc` branding.

## Acceptance

- `oc-engine status` → `proot`
- `oc-doctor` has no `MISSING`
- `bash -lc 'git --version'`, `python3 --version`, `pkg install -y jq`
- official opencode Android app connects with a copied pairing link
- the same kernel AAR powers the simple and full products without code forks
