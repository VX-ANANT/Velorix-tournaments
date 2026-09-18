import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

# Add selectedFee
if "var selectedFee by remember" not in text:
    text = text.replace('var selectedCategory by remember { mutableStateOf("All") }', 'var selectedCategory by remember { mutableStateOf("All") }\n    var selectedFee by remember { mutableStateOf("All") }')

# Replace Category row with combined filters
old_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category.toUpperCase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }"""

new_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category.toUpperCase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    listOf("All", "Free", "Paid").forEach { fee ->
                        val isSelected = selectedFee == fee
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedFee = fee }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = fee.toUpperCase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }"""

text = text.replace(old_row, new_row)

old_filter_logic = """val matchesSearch = searchQuery.isBlank() || t.title.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch"""
new_filter_logic = """val matchesSearch = searchQuery.isBlank() || t.title.contains(searchQuery, ignoreCase = true)
                val matchesFee = selectedFee == "All" || (selectedFee == "Free" && t.entryFee == 0.0) || (selectedFee == "Paid" && t.entryFee > 0.0)
                matchesCategory && matchesSearch && matchesFee"""
                
text = text.replace(old_filter_logic, new_filter_logic)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)

