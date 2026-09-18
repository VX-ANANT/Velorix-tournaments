#!/bin/bash
sed -i 's/Box(/Box(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(Brush.verticalGradient(listOf(Color(0xFF09090B), Color(0xFF18181B)))),/g' app/src/main/java/com/example/ui/screens/HomeScreen.kt
