import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.project.webapp.datas.UserActivity
import kotlinx.coroutines.flow.asStateFlow

class ActivityViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private val _activities = MutableStateFlow<List<UserActivity>>(emptyList())
    val activities: StateFlow<List<UserActivity>> = _activities.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchActivities(userType: String, userId: String) {
        Log.d("ActivityViewModel", "Fetching activities → userId: $userId, userType: $userType")

        listener?.remove()
        _errorMessage.value = null

        listener = db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereEqualTo("userType", userType)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> {
                        Log.e("ActivityViewModel", "Firestore error", exception)
                        _errorMessage.value = "Failed to load activities. Check your connection."
                        _activities.value = emptyList()
                    }
                    snapshot == null || snapshot.isEmpty -> {
                        Log.d("ActivityViewModel", "No activities found")
                        _activities.value = emptyList()
                    }
                    else -> {
                        Log.d("ActivityViewModel", "Fetched ${snapshot.size()} activities")
                        val list = snapshot.toObjects(UserActivity::class.java)
                        _activities.value = list
                    }
                }
            }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}