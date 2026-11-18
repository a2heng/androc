# Androc repository rules (read before changing paths or commands)

**Design ethos:** the mechanism owns the complexity; the user/agent stays
transparent. One runtime (proot), one authoritative path per thing, no
command-name whitelists, and every failure must be self-healing or resumable.
If a change makes the user learn an internal detail, it is probably wrong.

1. **Read [`docs/design.md`](docs/design.md),
   [`docs/paths.md`](docs/paths.md), [`docs/commands.md`](docs/commands.md),
   [`docs/series.md`](docs/series.md), [`docs/plugins.md`](docs/plugins.md),
   [`docs/model.md`](docs/model.md) and [`docs/connections.md`](docs/connections.md)
   first.** They are the contract for the product model, paths, proot mounts,
   command execution, packaging, plugins, the server default model and
   connections.
2. **Never execute a Termux ELF outside proot.** The only exceptions are the
   leaf tools in `CommandResolver.SAFE_NATIVE` (they are launched with
   `linker64` + `LD_LIBRARY_PATH`). If in doubt, route to proot.
3. **Do not add a command to `SAFE_NATIVE`** unless it never `exec`s another
   program. Interpreters, build tools, package managers and git helpers stay out.
4. **Every startup-essential file** must be in `LocalToolbox.ESSENTIALS`,
   `ESSENTIAL_ROOTS` (fetch script), `bootstrap.txt`, and `docs/paths.md`.
5. **`bootstrap.txt` must contain `bash` and `readline`** so a failed dpkg
   cannot leave a prefix that says `library "libreadline.so.8" not found`.
6. **Never hardcode** `com.a2heng.androc`, `com.termux`, `nativeLibraryDir` or a
   JDK/SDK path in shell scripts. Use `$HOME`, `$PREFIX`, `$TMPDIR`; generate
   `nativeLibraryDir` paths at runtime.
7. **`oc-proot` must stay idempotent** (`OC_ENGINE=proot` does not nest) and
   must preserve the caller's `$PWD` as `proot -w`; forcing `$HOME` makes
   `$PWD/project` look at the wrong directory.
8. **Do not create a second copy of generated wrappers** in `$HOME/bin` once
   the toolbox exists; `LocalRuntime.cleanLegacyHomeBin` removes the old ones.
9. **Build/verify locally with the pinned toolchain:**

   ```sh
   source /d/AndroidDevToolchain/env.sh
   cd /d/005_dev/android-dev-in-android
   python scripts/verify-toolbox.py
   ./gradlew :app:assembleDebug
   ```

10. **Before tagging a release:** `python scripts/verify-toolbox.py` must pass;
    `python scripts/check-opencode-latest.py --strict` must pass and
    `opencode.version` must be the newest upstream release. Release CI runs both;
    do not tag if either fails.
11. **Update both docs and the skills** when changing behavior. Docs are for
    humans/repo review; skills + `$HOME/.config/*/AGENTS.md` are what the agent
    actually loads at startup. The 60-second acceptance test is in
    `docs/paths.md`; run it on a real device (`oc-engine status` must say
    `proot`, `oc-doctor`, `bash -lc 'git --version'`, `python3 --version`,
    `pkg install -y jq`).
12. **`ui/` is the only UI source.** The old `a2heng/opencode-android-ui` repo
    is no longer maintained; never reintroduce a submodule or a second copy. Keep
    the behavior invariants in [`docs/mobile-integration.md`](docs/mobile-integration.md)
    (default model, saved connections, keepalive, path normalization/resolution).
    Only branding, the update repo, Androc's HOME-only file presets and the
    host-injected Termux shell may differ from a standalone UI build.
13. **Relative paths are not optional.** `normPath` only fixes separators;
    a path that does not start with `/` (or a drive) must be completed by
    `AppModel.resolvePath` / `Markdown.resolveFileRef` before it reaches
    `/api/fs/read`. The server resolves bare relative paths against its own
    cwd, which silently opens the wrong file or returns 404.
14. **`RELEASE_NOTES.md` is incremental.** Keep only the version being released
    (relative to the previous tag), in Chinese, with no historical sections and
    no English copy. CI passes the whole file as the GitHub Release body, so a
    stacked history would be published verbatim.

The Windows toolchain lives at `D:\AndroidDevToolchain`; its README explains
the single JDK/SDK/PATH setup.
