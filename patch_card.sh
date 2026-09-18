#!/bin/bash
sed -i 's/val PinkishRedAccent.*/val PinkishRedAccent = Color(0xFF8B5CF6)/g' app/src/main/java/com/example/ui/components/TournamentCard.kt
sed -i 's/val DeepSpaceBlack.*/val DeepSpaceBlack = Color(0xFF09090B)/g' app/src/main/java/com/example/ui/components/TournamentCard.kt
sed -i 's/val CardSurfaceLight.*/val CardSurfaceLight = Color(0xFF18181B)/g' app/src/main/java/com/example/ui/components/TournamentCard.kt
