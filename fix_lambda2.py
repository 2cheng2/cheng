# -*- coding: utf-8 -*-
import io

p = '/root/zhy/app/src/main/java/GJJX/zzzwt/ltd/BottomNavBar.java'
s = io.open(p, encoding='utf-8').read()

pairs = [
    ('valueSpring',   '1.0f, 1000f, vThreshold', 'indOffset = v; invalidate();'),
    ('pressSpring',   '1.0f, 1000f, 0.001f',     'pressProgress = v; invalidate();'),
    ('scaleXSpring',  '0.6f, 250f, 0.001f',      'scaleXSpringVal = v; invalidate();'),
    ('scaleYSpring',  '0.7f, 250f, 0.001f',      'scaleYSpringVal = v; invalidate();'),
    ('velocitySpring','0.5f, 300f, 0.05f',       'vNormSpring = v; invalidate();'),
]

count = 0
for name, args, body in pairs:
    old = (name + ' = new SpringAnim(' + args + ',\n'
           '                (v, vel) -> { ' + body + ' }, null);')
    # 宽松匹配：行首空白任意
    idx = s.find(old)
    if idx < 0:
        print('MISS', name)
        continue
    new = (name + ' = new SpringAnim(' + args + ',\n'
           '                new SpringAnim.OnUpdate() {\n'
           '                    @Override\n'
           '                    public void onUpdate(float v, float vel) {\n'
           '                        ' + body + '\n'
           '                    }\n'
           '                }, null);')
    s = s[:idx] + new + s[idx + len(old):]
    count += 1

print('replaced', count)
io.open(p, 'w', encoding='utf-8').write(s)