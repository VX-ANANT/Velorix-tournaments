import re

with open('developer_pfp_contrast.txt') as f:
    lines = f.readlines()

pixels = {}
max_x, max_y = 0, 0
for line in lines[1:]:
    m = re.match(r'(\d+),(\d+):\s*\(([\d\.]+)', line)
    if m:
        x, y, v = int(m.group(1)), int(m.group(2)), float(m.group(3))
        pixels[(x, y)] = v
        if x > max_x: max_x = x
        if y > max_y: max_y = y

ramp = " .'`^\",:;Il!i><~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$"

for y in range(max_y + 1):
    row = ''
    for x in range(max_x + 1):
        v = pixels.get((x, y), 255)
        idx = int((1.0 - (v / 255.0)) * (len(ramp) - 1))
        idx = max(0, min(len(ramp) - 1, idx))
        row += ramp[idx]
    print(repr(row))
