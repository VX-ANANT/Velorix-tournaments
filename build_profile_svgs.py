import re
import html

# 1. Read ASCII pixels
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

# Right side lines exactly matching the aesthetic of Andrew6rant's terminal neofetch
# Formatted with styled spans
right_side = [
    [("header", "anant@archlinux"), ("sep", " ------------------------------------------------")],
    [("label", " . OS:"), ("dot", " .................. "), ("val", "Arch Linux x86_64, Android 14")],
    [("label", " . Host:"), ("dot", " ................ "), ("val", "Hyprland Compositor (Wayland)")],
    [("label", " . Kernel:"), ("dot", " .............. "), ("val", "Linux 6.x-zen (Custom Architecture)")],
    [("label", " . IDE:"), ("dot", " ................. "), ("val", "Neovim (NvChad), Android Studio")],
    [("label", " . Terminal:"), ("dot", " ............ "), ("val", "Kitty + Zsh + Starship Prompt")],
    [("dot", " . ")],
    [("category", " . Languages.Programming:"), ("dot", " ... "), ("val", "Kotlin, Python, Java, Bash, Dart")],
    [("category", " . Languages.Web&Data:"), ("dot", " ...... "), ("val", "TypeScript, SQL, C, HTML/CSS")],
    [("category", " . Frameworks.Core:"), ("dot", " ......... "), ("val", "Jetpack Compose, Flutter, Node.js")],
    [("category", " . Infrastructure:"), ("dot", " .......... "), ("val", "Firebase, Supabase, n8n, Docker")],
    [("dot", " . ")],
    [("category", " . Security.Domains:"), ("dot", " ........ "), ("val", "Web Exploitation, PrivEsc, CTFs")],
    [("category", " . Security.Arsenal:"), ("dot", " ........ "), ("val", "Burp Suite, Metasploit, Wireshark, Nmap")],
    [("category", " . Flagship.Project:"), ("dot", " ........ "), ("val", "VeloRix Esports Platform & Engine")],
    [("category", " . Hobbies.Hardware:"), ("dot", " ........ "), ("val", "System Tweaking, Custom Kernels")],
    [("dot", " . ")],
    [("header", " - Contact"), ("sep", " ----------------------------------------------------")],
    [("label", " . Email.Personal:"), ("dot", " ............. "), ("val", "service.veloxyra@gmail.com")],
    [("label", " . Instagram:"), ("dot", " .................. "), ("val", "@anant_sgh")],
    [("label", " . X (Twitter):"), ("dot", " ................ "), ("val", "@Anant__sgh")],
    [("label", " . Discord:"), ("dot", " .................... "), ("val", "discord.gg/ghxrpQAAC2")],
    [("dot", " . ")],
    [("header", " - GitHub Telemetry"), ("sep", " ----------------------------------------------")],
    [("label", " . Repos:"), ("dot", " ... "), ("val_hi", "VX-ANANT"), ("dot", " | "), ("label", "Role:"), ("dot", " ... "), ("val_hi", "Architect & Founder")],
    [("label", " . Philosophy:"), ("dot", " ..... "), ("val_green", "\"Build it clean. Build it fast. Ship it.\"")]
]

def generate_svg(theme):
    if theme == "dark":
        bg_card = "#0D1117"
        bg_border = "#30363D"
        color_ascii = "#8B949E"
        color_header = "#58A6FF"
        color_sep = "#30363D"
        color_label = "#79C0FF"
        color_category = "#FFA657"
        color_dot = "#484F58"
        color_val = "#C9D1D9"
        color_val_hi = "#7EE787"
        color_val_green = "#7EE787"
        color_stats = "#FFA657"
    else:
        bg_card = "#F6F8FA"
        bg_border = "#D0D7DE"
        color_ascii = "#57606A"
        color_header = "#0969DA"
        color_sep = "#D0D7DE"
        color_label = "#0550AE"
        color_category = "#BC4C00"
        color_dot = "#8C959F"
        color_val = "#24292F"
        color_val_hi = "#1A7F37"
        color_val_green = "#116329"
        color_stats = "#953800"

    width = 980
    height = 510
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}">
  <defs>
    <style>
      .mono {{
        font-family: 'Fira Code', 'Courier New', 'SFMono-Regular', Consolas, monospace;
        font-size: 13px;
        white-space: pre;
      }}
      .ascii {{ fill: {color_ascii}; }}
      .header {{ fill: {color_header}; font-weight: bold; }}
      .sep {{ fill: {color_sep}; }}
      .label {{ fill: {color_label}; font-weight: 600; }}
      .category {{ fill: {color_category}; font-weight: 600; }}
      .dot {{ fill: {color_dot}; }}
      .val {{ fill: {color_val}; }}
      .val_hi {{ fill: {color_val_hi}; font-weight: bold; }}
      .val_green {{ fill: {color_val_green}; font-style: italic; }}
    </style>
  </defs>

  <!-- Background Card -->
  <rect x="2" y="2" width="{width - 4}" height="{height - 4}" rx="14" ry="14" fill="{bg_card}" stroke="{bg_border}" stroke-width="1.5"/>

  <!-- Left Column: Character ASCII Art -->
  <g class="mono ascii" transform="translate(26, 32)">
'''
    # 26 lines of ASCII
    line_height = 17.5
    for idx, row in enumerate(ascii_rows):
        safe_row = html.escape(row)
        y = idx * line_height
        svg += f'    <text x="0" y="{y:.1f}">{safe_row}</text>\n'

    svg += '''  </g>

  <!-- Right Column: Terminal Details -->
  <g class="mono" transform="translate(370, 32)">
'''
    for idx, parts in enumerate(right_side):
        y = idx * line_height
        line_svg = f'    <text x="0" y="{y:.1f}">'
        for span_type, text in parts:
            safe_text = html.escape(text)
            line_svg += f'<tspan class="{span_type}">{safe_text}</tspan>'
        line_svg += '</text>\n'
        svg += line_svg

    svg += '''  </g>
</svg>'''
    return svg

with open('dark_mode.svg', 'w') as f:
    f.write(generate_svg('dark'))

with open('light_mode.svg', 'w') as f:
    f.write(generate_svg('light'))

print("dark_mode.svg and light_mode.svg generated successfully!")
