# -*- coding: utf-8 -*-
import io
import re

p = '/root/zhy/app/src/main/java/GJJX/zzzwt/ltd/BottomNavBar.java'
s = io.open(p, encoding='utf-8').read()

pat = re.compile(
    r'(\w+) = new SpringAnim\(([^)]*)\),\s*\n\s*\(v, vel\) -> \{([^}]*)\}, null\);',
    re.M)


def repl(m):
    name = m.group(1)
    args = m.group(2)
    body = m.group(3).rstrip().rstrip(';').strip()
    tmpl = (
        '{NAME} = new SpringAnim({ARGS},\n'
        '                new SpringAnim.OnUpdate() {{\n'
        '                    @Override\n'
        '                    public void onUpdate(float v, float vel) {{\n'
        '                        {BODY};\n'
        '                    }}\n'
        '                }}, null);'
    )
    return tmpl.replace('{NAME}', name).replace('{ARGS}', args).replace('{BODY}', body)


s2, n = pat.subn(repl, s)
print('replaced', n)
io.open(p, 'w', encoding='utf-8').write(s2)