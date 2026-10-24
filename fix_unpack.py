# -*- coding: utf-8 -*-
import io

p = '/root/zhy/build_compose_apk.sh'
s = io.open(p, encoding='utf-8').read()

old = '''AAR_DIR=$BUILD/libs
find /root/.gradle/caches/modules-2/files-2.1 -name '*.aar' ! -path '*aapt2*' | while read aar; do
  name=$(basename "$aar" .aar)
  dir="$AAR_DIR/$name"
  mkdir -p "$dir"
  unzip -qo "$aar" classes.jar -d "$dir" 2>/dev/null || true
  unzip -qo "$aar" 'libs/*.jar' -d "$dir" 2>/dev/null || true
done
find "$AAR_DIR" -name '*.jar' | wc -l | xargs echo "  jar 数:"'''

new = '''AAR_DIR=$BUILD/libs
i=0
find /root/.gradle/caches/modules-2/files-2.1 -name '*.aar' ! -path '*aapt2*' | while read aar; do
  i=$((i+1))
  # 用坐标路径做唯一目录名：.../group/module/version/hash/
  rel=$(dirname "$aar" | sed 's|/root/.gradle/caches/modules-2/files-2.1/||')
  dir="$AAR_DIR/$(echo "$rel" | tr '/' '_')"
  mkdir -p "$dir"
  unzip -qo "$aar" classes.jar -d "$dir" 2>/dev/null || true
  unzip -qo "$aar" 'libs/*.jar' -d "$dir" 2>/dev/null || true
done
find "$AAR_DIR" -name 'classes.jar' -size +1k | wc -l | xargs echo "  有效 classes.jar:"'''

assert old in s, 'aar unpack block not found'
s = s.replace(old, new, 1)

io.open(p, 'w', encoding='utf-8').write(s)
print('unpack collision fixed')