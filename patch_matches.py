import re
with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

# Add necessary imports
if "import androidx.compose.foundation.pager.HorizontalPager" not in text:
    text = text.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.pager.HorizontalPager\nimport androidx.compose.foundation.pager.rememberPagerState\nimport androidx.compose.runtime.rememberCoroutineScope\nimport kotlinx.coroutines.launch")

target_tabs = """    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("UPCOMING", "COMPLETED")"""

replacement_tabs = """    val tabs = listOf("UPCOMING", "COMPLETED")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = pagerState.currentPage"""

text = text.replace(target_tabs, replacement_tabs)

target_tab_row = """        // Custom selector TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.testTag("match_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        if (selectedTabIndex == 0) {"""

replacement_tab_row = """        // Custom selector TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        coroutineScope.launch { 
                            pagerState.animateScrollToPage(index) 
                        } 
                    },
                    modifier = Modifier.testTag("match_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {"""

text = text.replace(target_tab_row, replacement_tab_row)

target_else = """            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredUpcoming) { match ->
                        UpcomingJoinedRow(match = match, onClick = { onNavigateToTournament(match.id) })
                    }
                }
            }
        } else {
            // COMPLETED HISTORY"""

replacement_else = """            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredUpcoming) { match ->
                        UpcomingJoinedRow(match = match, onClick = { onNavigateToTournament(match.id) })
                    }
                }
            }
        } else if (page == 1) {
            // COMPLETED HISTORY"""

text = text.replace(target_else, replacement_else)

target_end = """            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredCompleted) { match ->
                        CompletedRow(match)
                    }
                }
            }
        }
    }
}

@Composable"""

replacement_end = """            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredCompleted) { match ->
                        CompletedRow(match)
                    }
                }
            }
        }
        }
    }
}

@Composable"""

text = text.replace(target_end, replacement_end)

with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "w") as f:
    f.write(text)
print("HorizontalPager applied")
