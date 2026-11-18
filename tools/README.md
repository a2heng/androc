# `:tools` — Android tools for the opencode agent

The optional third layer. It turns a working opencode kernel into an on-device
Android IDE: the agent can install packages, build APKs and use phone APIs.

The kernel must run with **zero** tools: missing tools only remove build
skills, never the server.

## Responsibilities

- bundled skills (`android-build`, `android-project`, `termux`, …)
- device API exposed to the agent (clipboard, toast, vibrate, Wi-Fi, …)
- JDK 21, Android SDK/platform, aarch64 `aapt2`, Gradle, Maven seed cache
- `setup-toolchain.sh`, `build.sh`, `check-16kb.py`, cache cleaners
- toolbox/package manifest and offline fetch/verify scripts
- `ToolsController` implementation

## Migration map (phase 3)

| Now | Destination |
|---|---|
| `app/src/main/assets/skills/**` | `tools/src/main/assets/skills/**` |
| `app/src/main/assets/tools/**` | `tools/src/main/assets/tools/**` |
| `app/src/main/assets/toolchain/**` | `tools/src/main/assets/toolchain/**` |
| `app/src/main/java/.../DeviceApi.java` | `tools/.../DeviceApi.java` |
| `scripts/fetch-toolbox.py` (toolchain part) | keep in tools |
| `scripts/make-toolchain.py`, `verify-toolbox.py` | keep in tools |

`toolbox/*.deb` (bash, readline, JDK?) stays with the kernel when it is
required for the shell; only build-chain packages belong to tools. Split the
manifest explicitly in this phase: `bootstrap.txt` → kernel, SDK/Gradle →
tools.

## Must not depend on

`:ui`, Compose, the server process. Tools are invoked by the kernel/agent, not
the other way around.

## Acceptance

- full product: `bash ~/.config/opencode/skills/android-build/scripts/build.sh /path debug` succeeds
- simple product (kernel only): server still starts; build skill reports missing toolchain instead of crashing
- `oc-doctor` distinguishes kernel essentials from toolchain extras
