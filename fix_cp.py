# -*- coding: utf-8 -*-
import io

p = '/root/zhy/build_compose_apk.sh'
s = io.open(p, encoding='utf-8').read()

# javac 加上所有 aar classes.jar 到 classpath（在收集步骤之后已有 AAR_DIR）
old = '''SRCS=$(find "$BASE/app/src/main/java" "$BUILD/gen" -name '*.java')
mkdir -p "$BUILD/classes"
javac -encoding UTF-8 \\
  --release 17 \\
  -classpath "$SDK_JAR" \\
  -d "$BUILD/classes" \\
  -nowarn $SRCS 2>&1 | grep -E 'error' | head -10
[ -d "$BUILD/classes/$PKG" ] || { echo "javac 失败"; exit 1; }'''

new = '''SRCS=$(find "$BASE/app/src/main/java" "$BUILD/gen" -name '*.java')
mkdir -p "$BUILD/classes"
CP="$SDK_JAR"
for j in $(find "$AAR_DIR" -name 'classes.jar'); do
  CP="$CP:$j"
done
javac -encoding UTF-8 \\
  --release 17 \\
  -classpath "$CP" \\
  -d "$BUILD/classes" \\
  -nowarn $SRCS 2>&1 | grep -E 'error' | head -10
[ -d "$BUILD/classes/$PKG" ] || { echo "javac 失败"; exit 1; }'''

assert old in s, 'javac block not found'
s = s.replace(old, new, 1)

io.open(p, 'w', encoding='utf-8').write(s)
print('javac classpath fixed')