#!/usr/bin/env bash
# 通过 adb forward 访问 App 内的只读调试接口（127.0.0.1:42010）。
#
# 用法：
#   scripts/oc-debug.sh [serial] state
#   scripts/oc-debug.sh [serial] git <目录> <git 参数...>
#   scripts/oc-debug.sh [serial] shell <命令...>
#
# 例：
#   scripts/oc-debug.sh 192.168.0.101:43203 git \
#     /data/data/com.a2heng.androc/files/home/workspace log --oneline -5
set -euo pipefail

SERIAL="${1:-192.168.0.101:43203}"
ACTION="${2:-state}"
PORT=42010

adb -s "$SERIAL" forward "tcp:$PORT" "tcp:$PORT" >/dev/null
BASE="http://127.0.0.1:$PORT"

case "$ACTION" in
  state)
    curl -s "$BASE/state"
    ;;
  git)
    shift 2
    DIR="$1"; shift
    curl -s -G "$BASE/git" --data-urlencode "dir=$DIR" --data-urlencode "args=$*"
    ;;
  shell)
    shift 2
    curl -s -G "$BASE/shell" --data-urlencode "cmd=$*"
    ;;
  health)
    curl -s "$BASE/health"
    ;;
  *)
    echo "usage: $0 [serial] <state|health|git <dir> <git args...>|shell <cmd...>>" >&2
    exit 2
    ;;
esac
echo
