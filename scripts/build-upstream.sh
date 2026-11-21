#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
upstream_commit=6c0cc34677c4188575158c9ea5f53c95200be4a0
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

git -C "$scratch" init -q
git -C "$scratch" fetch -q --depth=1 https://github.com/sparky480v/Dark-1 "$upstream_commit"
git -C "$scratch" checkout -q FETCH_HEAD

case "${1:-}" in
    emulator)
        (cd "$scratch" && EN_VERSION=1.0.7 web/emu/build_clang.sh)
        cp "$scratch"/build/emu/{index.html,worklet.js,amb.wasm} "$root/app/src/main/assets/emu/"
        python3 "$root/scripts/local-emulator.py" "$root/app/src/main/assets/emu/index.html"
        ;;
    firmware)
        : "${JIELI_TOOLCHAIN:?Set JIELI_TOOLCHAIN to the JieLi toolchain directory}"
        : "${AC79_SDK:?Set AC79_SDK to the pinned AC79 SDK checkout}"
        git -C "$scratch" apply "$root/firmware/dark1-1.0.7.patch"
        cp "$root"/firmware/{dark1_mirror.c,screen_mirror.c,screen_mirror.h} "$scratch/firmware/src/"
        (cd "$scratch" && ./build.sh --release 1.0.7-fast)
        mkdir -p "$root/build"
        cp "$scratch/build/DARK-1-1.0.7-fast.fwsc" "$root/build/DARK1-BigScreen-v1.0.7-fast.fwsc"
        ;;
    *) echo "Usage: $0 emulator|firmware" >&2; exit 2 ;;
esac
