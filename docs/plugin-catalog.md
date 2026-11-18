# 插件目录（远期设计）

状态：`[远期]`。本文只记录想法和第一个落地样例，不代表已经实现。

决定：**Android 是一个插件**；写作是第一个非 Android 插件。

---

## 1. 插件清单

| 插件 | 状态 | Skills | 工具链 / payload | 说明 |
|---|---|---|---|---|
| **OpenCode 运行时** | `[进行中]` | — | opencode、proot、Termux | 不可卸载，可独立更新 |
| **Android** | `[进行中]` | 全部 android-* | JDK/SDK/aapt2/Gradle/adb/AI | 一个 Android 插件，内部组件独立更新 |
| **写作 Writing** | `[第一个实现]` | outline/draft/edit/style/cite/export | typst、字体、模板；可选 pandoc | 文本为主、payload 小、无需 Android 权限 |
| 研究 Research | `[远期]` | research/summarize/cite/compare | pdftotext、ripgrep、python、ONNX embeddings、SQLite | 与本地 AI 互补 |
| 数据分析 Data | `[远期]` | analyze/clean/chart/report | Python、pandas、DuckDB、matplotlib | 手机端中小数据集 |
| 媒体 Media | `[远期]` | transcribe/subtitle/clip/audio-clean | ffmpeg、whisper.cpp、sox | 大 payload，独立更新 |
| Web 开发 | `[远期]` | scaffold/ui/api/test/deploy | bun/node、Vite、Playwright | 与 Android 插件分开 |
| 办公文档 Office | `[远期]` | docx/xlsx/pptx/pdf/merge | pandoc、LibreOffice headless | 写作插件的自然扩展 |
| 手机自动化 | `[远期]` | automate/scrape/test-device | adb、uiautomator | 可复用 Android 插件的 adb |
| 安全审计 | `[远期]` | scan/secrets/deps/report | semgrep、bandit、osv-scanner | 结构化报告 |
| DevOps | `[远期]` | ssh/deploy/logs/backup | ssh、rsync、ansible、kubectl | 远程服务器 |
| 翻译 | `[远期]` | translate/glossary/style | 本地/远程模型、TTS | 与写作共享术语表 |
| 音乐 | `[远期]` | compose/arrange/mix | MIDI、sox、ffmpeg | 可选 |

原则：一个插件 = 一组 Skill + 它需要的工具链；工具链大头作为 payload，
插件 APK 只带契约、manifest、skills 和安装请求。

---

## 2. 第一个实现：Writing

### 2.1 插件信息

```json
{
  "schema": 1,
  "id": "writing",
  "name": "写作",
  "category": "other",
  "version": "1.0.0",
  "minAppVersion": 8,
  "skills": [
    "writing-brief",
    "writing-research",
    "writing-outline",
    "writing-draft",
    "writing-critique",
    "writing-revise",
    "writing-edit",
    "writing-factcheck-cite",
    "writing-export"
  ],
  "toolchain": ["typst", "fonts", "templates"],
  "homepage": "https://github.com/a2heng/androc-plugin-writing",
  "updateSource": "https://github.com/a2heng/androc-packs/releases/latest/download/plugins.json"
}
```

### 2.2 参考工作流（不自己发明）

写作插件不重新发明流程，直接学习已经跑通的公开工作流：

| 参考 | 流程 | 可借鉴 |
|---|---|---|
| **Buchbinder**（MIT，OpenCode 插件） | manifest → specify → clarify → research → outline → task-manager → draft → critique → revise → edit(3-pass) → review → publish | 阶段化、质量门、8 种 critique、三遍编辑、checkpoint、pandoc 导出 |
| **Bookwright**（MIT，Claude skill） | idea/manifesto → voice → structure/TOC → bibliography → outline → 写作三角色（draft / develop-line / copyedit）→ acquiring editor | 先定“为什么写/给谁看/什么声音”，再写；大纲是研究与写作的交汇点 |
| 通用非虚构 | Brief → Research → Outline → Draft → Fact-check/Cite → Revise → Line edit → Copy edit → Proofread → Publish | 起草和编辑分离；引用和事实检查单独成阶段 |
| 通用虚构 | Snowflake / Save the Cat / Hero's Journey | 角色、场景节拍、伏笔/回收；作为可选 track |

结论：**写作插件的骨架 = Buchbinder 的阶段 + Bookwright 的 manifesto/voice/three-role 编辑**，先做短/中篇，再做书。

### 2.3 Skills（v1）

| Skill | 阶段 | 作用 |
|---|---|---|
| `writing-brief` | Brief / manifesto | 目的、读者、范围、语气、非目标；未确认不进入下一阶段 |
| `writing-research` | Research | 资料、访谈、来源、引用键；产出 `research/` |
| `writing-outline` | Outline | 章节/段落结构、每节目标、证据、字数；大纲门通过才写 |
| `writing-draft` | Draft | 按大纲逐节写；一次只加载当前节 + 相邻上下文 |
| `writing-critique` | Critique | alpha（结构）/ beta（读者体验）/ cold-read（无上下文） |
| `writing-revise` | Revise | 按 critique 逐条修改；记录 revision log |
| `writing-edit` | Edit | 三遍：line edit → copy edit → proofread |
| `writing-factcheck-cite` | Verify | 事实检查、引用完整性、参考文献 |
| `writing-export` | Publish | Markdown/Typst → PDF/EPUB/DOCX/HTML |

可选扩展：`writing-style`（voice/style sheet）、`writing-continuity`（长篇连续性）、`writing-review`（整篇 QA）。

### 2.4 项目产物

```text
writing-project/
├─ brief.md          # 目的/读者/范围/语气/非目标
├─ voice.md          # 风格、禁用词、术语表
├─ research/         # 来源、笔记、引用
├─ outline.md        # 章节/段落结构
├─ drafts/           # 分节草稿
├─ critique/         # 评审意见
├─ revisions/        # 修改记录
├─ references.bib    # 引用
└─ output/           # PDF/EPUB/DOCX
```

### 2.5 工具链 / payload

第一阶段只做 **Typst**，因为它是一个静态二进制，体积小、渲染快：

```text
writing payload v1
├─ bin/typst                  # aarch64 musl，proot 内执行
├─ templates/
│  ├─ article.typ
│  ├─ report.typ
│  └─ book.typ
├─ fonts/                     # 可选，按需下载
└─ manifest.json
```

- `typst` 从官方 release 下载，校验 sha256；
- 模板/字体是数据文件；
- 可选第二阶段加 `pandoc`（DOCX/EPUB 更强，但体积大）。

运行方式：

```sh
typst compile --font-path ~/.local/share/androc/fonts doc.typ doc.pdf
```

全部在 runtime 插件的 proot 内执行，基础 UI 不 exec 任何二进制。

### 2.6 安装与更新

- `writing` 插件 APK：manifest + skills + 安装请求；
- runtime 插件收到 `installPayload("writing.typst", url, sha256)`：
  - 下载 `.part` → 校验 → 解压到 `$HOME/.local/share/androc/writing/`；
  - 失败回滚；
- 插件更新：APK 更新 skills/contract；payload 更新 typst/字体，不需要新 APK；
- 卸载：删 skills + payload；不动项目、会话、其他插件。

### 2.7 验收

1. 安装 writing 插件；
2. 新建会话，让 agent 写一篇 1000 字文章；
3. agent 调用 `writing-export` 生成 PDF；
4. 用系统分享/文件页打开 PDF；
5. 卸载 writing，Android 插件和会话不受影响；
6. 上游 typst 发新版，只更新 payload，不重装 APK。

---

## 3. 远期插件的落地顺序

1. **Writing**（第一个非 Android 样例，验证 skill + payload + 独立更新）；
2. **Research/RAG**（复用 AI 插件和 SQLite）；
3. **Data**（Python 生态，payload 较大）；
4. **Media**（ffmpeg/whisper，体积大、价值高）；
5. **Web dev**（Node/bun 生态）；
6. 其他按需求。

每个插件都按同一套流程：

```text
独立仓库 → :plugin（Service/manifest/skills）
        → :payload（下载/校验/模板）
        → CI（构建、测试、发版）
        → androc-packs 聚合 manifest
        → 基础 UI 显示安装/更新/卸载
```

---

## 4. 参考来源

- [Thomashighbaugh/buchbinder](https://github.com/Thomashighbaugh/buchbinder)（MIT）
  — OpenCode 的书籍写作工作流：12 阶段、质量门、critique、三遍编辑、pandoc。
- [AdrianMastronardi/bookwright](https://github.com/AdrianMastronardi/bookwright)（MIT）
  — 非虚构七阶段：manifesto/voice/TOC/bibliography/outline/three-role/assessment。
- 通用写作/编辑流程：outline → draft → critique → revise → line edit →
  copy edit → proofread → publish。

原则：**学工作流，不抄实现。** 我们的 skills/脚本自己写，保持 MIT 兼容；
引用只作为设计参考，不把对方的代码复制进仓库。

---

## 5. 与当前代码的关系

- 当前 `:tools` 里的 Android 技能最终归入 `android` 插件；
- 写作插件不依赖 Android 插件，只依赖 runtime 插件；
- 在 runtime 插件迁移完成前，写作插件可以先做“skills + 本地 typst 脚本”的
  试验版，但正式版必须走 payload/独立 APK；
- 远期设计先写在这里，避免实现时再次走偏。
