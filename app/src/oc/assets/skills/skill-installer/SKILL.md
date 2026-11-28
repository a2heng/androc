---
name: Skill Installer
description: 在这台手机上从 git 仓库、zip 文件或本地目录安装、列出或删除 opencode 技能。只在用户明确要求安装/列出/删除技能，或要把新能力做成可复用技能时使用；只是聊天、读代码、讨论方案时不要加载。
---

# 技能安装器

> 手机环境：命令在 proot 内执行，用绝对路径；不要假设有桌面工具。
> 网络可能不稳定，失败先诊断再重试。

OpenCode 会在 `~/.config/opencode/skills/<id>/SKILL.md` 中发现技能。
本技能会同时安装到普通配置目录和应用的原生配置目录
（`~/.config/opencode-android/skills`）。

## 何时使用

- 用户说“安装这个技能”、“从 GitHub 添加一个技能”、“把这个做成技能”。
- 你需要一套可复用的流程，并想为后续会话安装它。
- 用户想列出或删除已安装的技能。

## 安全

技能是**代理将要遵循的指令**，其中可以包含脚本。
只从用户信任的来源安装。未经用户确认，绝不要安装随意的 URL。

## 安装

```sh
bash <skill-dir>/scripts/install.sh <git-url|zip-url|local-dir> [name]
```

示例：

```sh
# GitHub repo (shallow clone)
bash <skill-dir>/scripts/install.sh https://github.com/user/my-skill

# A subdirectory of a repo: name it explicitly
bash <skill-dir>/scripts/install.sh https://github.com/user/skills/tree/main/git-release git-release

# Zip file
bash <skill-dir>/scripts/install.sh https://example.com/skill.zip my-skill

# Local directory
bash <skill-dir>/scripts/install.sh /path/to/skill
```

脚本会找到 `SKILL.md`，校验 `name` + `description`，复制整个技能目录
（包括 `scripts/`、`references/`、`assets/`），并打印安装路径。用 `--force`
覆盖已存在的技能。

安装后，开始一个**新会话**（或重启服务器），以便 OpenCode 重新发现技能列表。

## 列出

```sh
bash <skill-dir>/scripts/list.sh
```

## 删除

```sh
bash <skill-dir>/scripts/remove.sh <skill-id>
```

这会从两个配置目录中删除该技能。它绝不会触碰
`android-build` 或 `skill-installer`。
