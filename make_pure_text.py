import re

with open('pfp_small.txt') as f:
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
ascii_rows = []
for y in range(max_y + 1):
    row = ''
    for x in range(max_x + 1):
        v = pixels.get((x, y), 255)
        idx = int((1.0 - (v / 255.0)) * (len(ramp) - 1))
        idx = max(0, min(len(ramp) - 1, idx))
        row += ramp[idx]
    ascii_rows.append(row)

right_side = [
    'anant@archlinux ------------------------------------------------',
    ' . OS: .................. Arch Linux x86_64, Android 14',
    ' . Host: ................ Hyprland Compositor (Wayland)',
    ' . Kernel: .............. Linux 6.x-zen (Custom Architecture)',
    ' . IDE: ................. Neovim (NvChad), Android Studio',
    ' . Terminal: ............ Kitty + Zsh + Starship Prompt',
    ' . ',
    ' . Languages.Programming: ... Kotlin, Python, Java, Bash, Dart',
    ' . Languages.Web&Data: ...... TypeScript, SQL, C, HTML/CSS',
    ' . Frameworks.Core: ......... Jetpack Compose, Flutter, Node.js',
    ' . Infrastructure: .......... Firebase, Supabase, n8n, Docker',
    ' . ',
    ' . Security.Domains: ........ Web Exploitation, PrivEsc, CTFs',
    ' . Security.Arsenal: ........ Burp Suite, Metasploit, Wireshark',
    ' . Flagship.Project: ........ VeloRix Esports Platform & Engine',
    ' . Hobbies.Hardware: ........ System Tweaking, Custom Kernels',
    ' . ',
    ' - Contact ----------------------------------------------------',
    ' . Email.Personal: ............. service.veloxyra@gmail.com',
    ' . Instagram: .................. @anant_sgh',
    ' . X (Twitter): ................ @Anant__sgh',
    ' . Discord: .................... discord.gg/ghxrpQAAC2',
    ' . ',
    ' - GitHub Telemetry ----------------------------------------------',
    ' . Repos: ... VX-ANANT | Role: ... Architect & Founder',
    ' . Philosophy: ..... "Build it clean. Build it fast. Ship it."'
]

max_len = max(len(ascii_rows), len(right_side))
combined = []
for i in range(max_len):
    l = ascii_rows[i] if i < len(ascii_rows) else ' ' * 38
    r = right_side[i] if i < len(right_side) else ''
    combined.append(f'{l}   {r}')

res = '\n'.join(combined)
with open('pure_terminal_card.txt', 'w') as f:
    f.write(res)
print("Done! Length:", len(combined))
