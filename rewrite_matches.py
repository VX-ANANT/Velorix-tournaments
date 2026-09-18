import re
with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

text = text.replace("var selectedTabIndex by remember { mutableStateOf(0) }", """val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = pagerState.currentPage""")

text = re.sub(
    r"onClick = \{ selectedTabIndex = index \}",
    r"onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }",
    text
)

text = text.replace(
    """if (selectedTabIndex == 0) {""",
    """HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {"""
)

# Then we need to fix the `} else if (page == 1) {` to be `} else {`
text = text.replace("} else if (page == 1) {", "} else {")

# And we need to add a closing brace for HorizontalPager at the very end.
text = text.replace(
"""                LazyColumn(
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

@Composable""",
"""                LazyColumn(
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
)

# We also need to change the inner `Modifier.weight(1f)` to `Modifier.fillMaxSize()` inside the HorizontalPager children.
text = text.replace(
"""                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),""",
"""                Box(
                    modifier = Modifier
                        .fillMaxSize(),"""
)

text = text.replace(
"""                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),""",
"""                Box(
                    modifier = Modifier.fillMaxSize(),"""
)

with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "w") as f:
    f.write(text)
print("done")
