# -*- coding: utf-8 -*-
import io

p = '/root/zhy/build_compose_apk.sh'
s = io.open(p, encoding='utf-8').read()

# aapt(1) 语法修正：--gen -> -m -J
s = s.replace("--gen \"$BUILD/gen\"", "-m -J \"$BUILD/gen\"")

# javac 17：--release 替代 -source/-target/-bootclasspath
old_javac = (
    'javac -encoding UTF-8 -source 17 -target 17 \\\n'
    '  -bootclasspath "$SDK_JAR" \\\n'
    '  -classpath "$SDK_JAR" \\'
)
new_javac = (
    'javac -encoding UTF-8 \\\n'
    '  --release 17 \\\n'
    '  -classpath "$SDK_JAR" \\'
)
assert old_javac in s, 'javac block not found'
s = s.replace(old_javac, new_javac, 1)

io.open(p, 'w', encoding='utf-8').write(s)
print('fixed both')