#!/bin/bash
# 下载最新 artifact（仅下载解压，安装需在 Android shell 做）
TOKEN="${GH_TOKEN:?set GH_TOKEN}"
REPO=2cheng2/cheng
OUT=/sdcard/zhy

RID=$(curl -sL -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs?per_page=1&status=success" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['workflow_runs'][0]['id'])")
echo "run: $RID"

AID=$(curl -sL -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs/$RID/artifacts" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['artifacts'][0]['id'])")
echo "artifact: $AID"

curl -sL -H "Authorization: token $TOKEN" \
  -o "$OUT/artifact.zip" \
  "https://api.github.com/repos/$REPO/actions/artifacts/$AID/zip"
ls -la "$OUT/artifact.zip"

rm -rf "$OUT/apk-out"
mkdir -p "$OUT/apk-out"
cd "$OUT/apk-out"
unzip -o "$OUT/artifact.zip"
ls -la