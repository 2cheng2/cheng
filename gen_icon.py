#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
校园服务 应用图标生成器

规格（来自项目视觉规范）：
  深蓝渐变圆角方底  #2E6FD9 -> #1B4C9E
  白色校舍剪影：山形屋顶 + 旗杆 + 门 + 两侧窗 + 左右翼楼 + 地面线
  圆角 112/512

输出各密度 PNG，纯标准库实现（zlib + struct），不依赖 PIL。
"""

import zlib
import struct
import os
import math

# ---------- 配色 ----------
TOP = (0x2E, 0x6F, 0xD9)
BOT = (0x1B, 0x4C, 0x9E)
WHITE = (255, 255, 255)

# 圆角半径比例（相对 512 设计稿）
CORNER_RATIO = 112.0 / 512.0

SS = 4  # 超采样倍数


def lerp(a, b, t):
    return a + (b - a) * t


def rounded_rect_coverage(x, y, w, h, r, px, py):
    """返回点 (px,py) 是否在圆角矩形内（用距离场判断）。"""
    cx = min(max(px, x + r), x + w - r)
    cy = min(max(py, y + r), y + h - r)
    dx = px - cx
    dy = py - cy
    return dx * dx + dy * dy <= r * r or (
        x <= px <= x + w and y <= py <= y + h and
        (x + r <= px <= x + w - r or y + r <= py <= y + h - r)
    )


def in_rounded_square(px, py, w, h, r):
    """精确的圆角矩形内部判定。"""
    if px < 0 or py < 0 or px > w or py > h:
        return False
    # 四个角分别判断
    corners = (
        (r, r, px < r and py < r),
        (w - r, r, px > w - r and py < r),
        (r, h - r, px < r and py > h - r),
        (w - r, h - r, px > w - r and py > h - r),
    )
    for ccx, ccy, active in corners:
        if active:
            return (px - ccx) ** 2 + (py - ccy) ** 2 <= r * r
    return True


def build_shapes():
    """返回白色剪影的形状列表，坐标为 512 设计稿空间。

    每个形状为 ('rect', x0,y0,x1,y1) 或 ('tri', 三点) 或 ('circle', cx,cy,r)
    """
    S = []

    # ---- 地面线 ----
    S.append(('rect', 96, 380, 416, 388))

    # ---- 主体建筑 ----
    S.append(('rect', 176, 240, 336, 380))
    # 山形屋顶（大三角）
    S.append(('tri', (256, 164), (152, 254), (360, 254)))

    # ---- 左右翼楼 ----
    S.append(('rect', 112, 300, 176, 380))
    S.append(('rect', 336, 300, 400, 380))
    # 翼楼小屋顶
    S.append(('tri', (144, 268), (104, 304), (184, 304)))
    S.append(('tri', (368, 268), (328, 304), (408, 304)))

    # ---- 旗杆 + 旗 ----
    S.append(('rect', 252, 104, 260, 170))
    S.append(('tri', (260, 108), (260, 140), (300, 124)))

    return S


def build_cutouts():
    """建筑上被"挖空"的部分，用底色填充以形成门窗。

    这里采用反向思路：先画白色实心建筑，再用底色画门窗。
    由于底色是渐变，所以门窗需要按位置取色。
    """
    C = []
    # 门（圆拱：矩形 + 半圆）
    C.append(('rect', 236, 320, 276, 380))
    C.append(('circle', 256, 320, 20))
    # 两侧窗
    C.append(('rect', 194, 286, 222, 314))
    C.append(('rect', 290, 286, 318, 314))
    return C


def point_in_tri(px, py, p1, p2, p3):
    def sign(a, b, c):
        return (a[0] - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (a[1] - c[1])
    p = (px, py)
    d1 = sign(p, p1, p2)
    d2 = sign(p, p2, p3)
    d3 = sign(p, p3, p1)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)


def inside_shape(shape, px, py):
    if shape[0] == 'rect':
        _, x0, y0, x1, y1 = shape
        return x0 <= px <= x1 and y0 <= py <= y1
    if shape[0] == 'tri':
        _, a, b, c = shape
        return point_in_tri(px, py, a, b, c)
    if shape[0] == 'circle':
        _, cx, cy, r = shape
        return (px - cx) ** 2 + (py - cy) ** 2 <= r * r
    return False


def render(size):
    """渲染指定尺寸的 RGBA 像素列表。"""
    S = build_shapes()
    C = build_cutouts()
    r = size * CORNER_RATIO

    # 以 512 设计稿为基准的缩放
    k = size / 512.0

    out = bytearray(size * size * 4)

    for y in range(size):
        for x in range(size):
            # 超采样累计
            acc_a = 0
            acc_r = acc_g = acc_b = 0.0
            for sy in range(SS):
                for sx in range(SS):
                    px = x + (sx + 0.5) / SS
                    py = y + (sy + 0.5) / SS

                    if not in_rounded_square(px, py, size, size, r):
                        continue

                    # 设计稿坐标
                    dx = px / k
                    dy = py / k

                    # 底色渐变（沿垂直方向）
                    t = max(0.0, min(1.0, (dy - 40) / (512 - 80)))
                    # 轻微的对角倾向，让渐变更自然
                    t = max(0.0, min(1.0, t * 0.86 + (dx / 512.0) * 0.14))
                    br = lerp(TOP[0], BOT[0], t)
                    bg = lerp(TOP[1], BOT[1], t)
                    bb = lerp(TOP[2], BOT[2], t)

                    # 白色剪影
                    solid = False
                    for s in S:
                        if inside_shape(s, dx, dy):
                            solid = True
                            break
                    if solid:
                        for c in C:
                            if inside_shape(c, dx, dy):
                                solid = False
                                break

                    if solid:
                        br, bg, bb = WHITE
                    else:
                        # 剪影外侧加一点极淡的高光，让图标不至于死板
                        pass

                    acc_r += br
                    acc_g += bg
                    acc_b += bb
                    acc_a += 255

            n = SS * SS
            idx = (y * size + x) * 4
            if acc_a == 0:
                out[idx] = out[idx + 1] = out[idx + 2] = out[idx + 3] = 0
            else:
                # acc_a 累计的是"命中次数*255"，用于算覆盖率
                cover = acc_a / (n * 255.0)
                hit = acc_a / 255.0
                out[idx] = int(acc_r / hit)
                out[idx + 1] = int(acc_g / hit)
                out[idx + 2] = int(acc_b / hit)
                out[idx + 3] = int(round(cover * 255))
    return bytes(out)


def write_png(path, size, rgba):
    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)  # filter type 0
        raw += rgba[y * stride:(y + 1) * stride]

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        c += struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)
        return c

    ihdr = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', ihdr)
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')

    with open(path, 'wb') as f:
        f.write(png)


def main():
    base = os.path.dirname(os.path.abspath(__file__))
    res = os.path.join(base, 'app', 'src', 'main', 'res')

    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    for folder, sz in densities.items():
        d = os.path.join(res, folder)
        os.makedirs(d, exist_ok=True)
        p = os.path.join(d, 'ic_launcher.png')
        write_png(p, sz, render(sz))
        print('wrote', p, sz)

    # 商店用大图
    out512 = os.path.join(base, 'appIcon-512.png')
    write_png(out512, 512, render(512))
    print('wrote', out512, 512)


if __name__ == '__main__':
    main()