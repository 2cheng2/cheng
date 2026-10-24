#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检测截图底部是否存在底栏（纯标准库，无 PIL）"""
import zlib, struct, sys


def load_png(path):
    d = open(path, 'rb').read()
    pos = 8
    idat = b''
    w = h = ct = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        tag = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if tag == b'IHDR':
            w, h, bd, ct = struct.unpack('>IIBB', data[:10])
        elif tag == b'IDAT':
            idat += data
        pos += 12 + ln

    raw = zlib.decompress(idat)
    bpp = 4 if ct == 6 else 3
    stride = w * bpp
    prev = bytearray(stride)
    rows = []
    y = 0
    p = 0
    while y < h and p < len(raw):
        ft = raw[p]
        p += 1
        cur = bytearray(raw[p:p + stride])
        p += stride
        if ft == 1:
            for i in range(bpp, stride):
                cur[i] = (cur[i] + cur[i - bpp]) & 255
        elif ft == 2:
            for i in range(stride):
                cur[i] = (cur[i] + prev[i]) & 255
        elif ft == 3:
            for i in range(stride):
                a = cur[i - bpp] if i >= bpp else 0
                cur[i] = (cur[i] + ((a + prev[i]) >> 1)) & 255
        elif ft == 4:
            for i in range(stride):
                a = cur[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa = abs(b - c)
                pb = abs(a - c)
                pc = abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                cur[i] = (cur[i] + pr) & 255
        rows.append(bytes(cur))
        prev = cur
        y += 1
    return w, h, bpp, rows


def px(rows, bpp, x, y):
    o = x * bpp
    r = rows[y]
    return (r[o], r[o + 1], r[o + 2])


def detect(path):
    w, h, bpp, rows = load_png(path)

    # 底栏区域：底部 14dp 边距 + 64dp 高度
    # 1080x2400 屏幕，dp≈3x，底栏约在 y = h-42-192 .. h-42
    y_top = h - 240
    y_bot = h - 50

    # 判据1：找蓝色像素（激活项 #1a5fb4 ≈ 26,95,180）
    blue = 0
    # 判据2：找底栏容器色 (252,252,253) 的连续水平带
    band = 0
    for yy in range(y_top, y_bot):
        row_hit = 0
        for xx in range(60, w - 60, 3):
            R, G, B = px(rows, bpp, xx, yy)
            if abs(R - 26) < 70 and abs(G - 95) < 70 and abs(B - 180) < 70:
                blue += 1
            if R > 245 and G > 245 and B > 245:
                row_hit += 1
        if row_hit > (w / 3) / 2:
            band += 1

    return blue, band, (w, h)


if __name__ == '__main__':
    for p in sys.argv[1:]:
        try:
            blue, band, size = detect(p)
            has = 'YES' if (blue > 200 or band > 100) else 'no'
            print(f'{p}: size={size} blue={blue} band={band} -> BAR {has}')
        except Exception as e:
            print(f'{p}: ERROR {e}')
