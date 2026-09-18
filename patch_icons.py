import os
import re

directory = "app/src/main/java/com/example/ui/screens/"

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    # Find all Icon( ... ) blocks
    # We will use a simple regex that finds Icon( followed by everything up to the matching closing paren.
    # Actually, simpler: just find all occurrences of `tint = ...` that follow `ic_iconsax` within a reasonable distance (e.g., 200 characters) without another `Icon(` in between.
    
    # Let's replace tint for specific known lines in MatchesScreen, HomeScreen, ProfileScreen, WalletScreen, TournamentDetailsScreen.
    # It's safer to just do a global replace of `tint = MaterialTheme.colorScheme.onSurfaceVariant` with `tint = Color.Unspecified` ONLY IF it's in a block with `ic_iconsax`.

    new_content = ""
    lines = content.split('\n')
    inside_icon = False
    has_iconsax = False
    
    for i in range(len(lines)):
        line = lines[i]
        if "Icon(" in line and "ic_iconsax" in line and "tint =" in line:
            # single line icon
            line = re.sub(r"tint = [^,]+", "tint = Color.Unspecified", line)
        elif "Icon(" in line:
            inside_icon = True
            has_iconsax = "ic_iconsax" in line
        elif inside_icon and "ic_iconsax" in line:
            has_iconsax = True
            
        if inside_icon and has_iconsax and "tint =" in line:
            line = re.sub(r"tint = [^,)]+", "tint = Color.Unspecified", line)
            
        if inside_icon and ")" in line and "(" not in line: # rough heuristic for end of Icon
            inside_icon = False
            has_iconsax = False
            
        new_content += line + "\n"
        
    with open(filepath, "w") as f:
        f.write(new_content)

for filename in os.listdir(directory):
    if filename.endswith(".kt"):
        process_file(os.path.join(directory, filename))
