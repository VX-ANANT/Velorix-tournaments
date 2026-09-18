import subprocess

# Let's check if we can trace or use convert to extract SVG
subprocess.run(["convert", "wallet-passes-app.png", "wallet_passes.svg"])
