import re

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

missions_val = """
    val missions = repository.missions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()
    
    fun claimMission(mission: com.example.data.model.Mission) {
        viewModelScope.launch {
            val success = repository.claimMissionReward(mission)
            if (success) {
                _toastMessage.emit("Claimed ${mission.rewardCurrency} for ${mission.title}!")
                _showConfetti.value = true
                kotlinx.coroutines.delay(3000)
                _showConfetti.value = false
            } else {
                _toastMessage.emit("Failed to claim reward.")
            }
        }
    }
    
    init {
        viewModelScope.launch {
            repository.initializeMissions()
        }
    }
"""

if "val missions =" not in text:
    text = text.replace("val liveMatchUpdates =", missions_val + "\n    val liveMatchUpdates =")
    
    with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
        f.write(text)

