with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    content = f.read()

# We know exactly where it is:
#         }
#     }
# 
#         })

import re
fixed_content = re.sub(
    r"    \}\n\n        \}\)\n",
    r"    }\n\n",
    content
)

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(fixed_content)
