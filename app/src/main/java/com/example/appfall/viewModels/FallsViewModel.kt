package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.models.DailyFallsRequest
import com.example.appfall.data.models.DailyFallsResponse
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallFilter
import com.example.appfall.data.models.isPausedRequest
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FallsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()

    private val mutableFallsList: MutableLiveData<List<Fall>?> = MutableLiveData()
    private val _addErrorStatus: MutableLiveData<String> = MutableLiveData()
    val addErrorStatus: LiveData<String> = _addErrorStatus

    private val _disconnectStatus: MutableLiveData<String> = MutableLiveData()
    val disconnectStatus: LiveData<String> = _disconnectStatus

    private val _pauseStatus: MutableLiveData<String> = MutableLiveData()
    val pauseStatus: LiveData<String> = _pauseStatus

    private val _dailyFallsData: MutableLiveData<DailyFallsResponse?> = MutableLiveData()
    val dailyFallsData: MutableLiveData<DailyFallsResponse?> = _dailyFallsData

    private lateinit var token: String

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                token = it.token
            }
        }
    }

    fun getFalls(userId: String, filter: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.fallApi.getFallsByUser("Bearer $token", userId, FallFilter(filter)).execute()
                }
                if (response.isSuccessful) {
                    val fallResponse = response.body()
                    fallResponse?.let {
                        mutableFallsList.postValue(it.data)
                        Log.d("FallsViewModel", "Données de chute reçues: $it")
                    } ?: run {
                        Log.d("FallViewModel", "Response body is null")
                    }
                } else {
                    mutableFallsList.postValue(null)
                    Log.d("FallViewModel", "Response not successful: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("FallViewModel", "API call failed: ${e.message}")
            }
        }
    }

    private suspend fun ensureTokenInitialized() {
        withContext(Dispatchers.IO) {
            if (!::token.isInitialized) {
                val user = userDao.getUser()
                user?.let {
                    token = it.token
                } ?: throw UninitializedPropertyAccessException("Token has not been initialized")
            }
        }
    }

    fun getDailyFalls(month: Int, year: Int) {
        viewModelScope.launch {
            ensureTokenInitialized()
            RetrofitInstance.fallApi.getDailyFalls("Bearer $token", DailyFallsRequest(month, year)).enqueue(object : Callback<DailyFallsResponse> {
                override fun onResponse(call: Call<DailyFallsResponse>, response: Response<DailyFallsResponse>) {
                    if (response.isSuccessful) {
                        val dailyFallsResponse = response.body()
                        _dailyFallsData.value = dailyFallsResponse
                    } else {

                        _dailyFallsData.value = null
                        handleErrorResponse(response.errorBody())
                        Log.e("FallViewModel", "Failed to retrieve daily falls: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<DailyFallsResponse>, t: Throwable) {
                    val errorMessage = t.message ?: "Une erreur s'est produite lors de la récupération des données"
                    _addErrorStatus.postValue(errorMessage)
                    Log.e("FallViewModel", "Failed to retrieve daily falls: $errorMessage", t)
                }
            })
        }
    }

    fun disconnect(contactId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.fallApi.disconnect("Bearer $token", contactId).execute()
                }
                if (response.isSuccessful) {
                    val disconnectedResponse = response.body()
                    disconnectedResponse?.let {
                        Log.d("FallsViewModel", "Déconnexion réussie: $it")
                        _disconnectStatus.postValue(it.message)
                    } ?: run {
                        Log.d("FallViewModel", "Response body is null")
                        _disconnectStatus.postValue("Le corps de la réponse est nul")
                    }
                } else {
                    Log.d("FallViewModel", "Response not successful: ${response.code()}")
                    handleErrorResponse(response.errorBody())
                }
            } catch (e: Exception) {
                Log.e("FallViewModel", "API call failed: ${e.message}")
                _disconnectStatus.postValue("Une erreur s'est produite lors de la déconnexion")
            }
        }
    }

    fun pause(contactId: String, isPaused: Boolean) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    Log.d("Pause", "${contactId} + ${token} + ${isPaused.toString()}")
                    RetrofitInstance.fallApi.pause("Bearer $token", contactId, isPausedRequest((isPaused))).execute()
                }
                if (response.isSuccessful) {
                    val pausedResponse = response.body()
                    pausedResponse?.let {
                        Log.d("FallsViewModel", "Suivi en pause: $it")
                        _pauseStatus.postValue(it.message)
                    } ?: run {
                        Log.d("FallViewModel", "Response body is null")
                        _pauseStatus.postValue("Le corps de la réponse est nul")
                    }
                } else {
                    Log.d("FallViewModel", "Response not successful: ${response.code()}")
                    handleErrorResponse(response.errorBody())
                }
            } catch (e: Exception) {
                Log.e("FallViewModel", "API call failed: ${e.message}")
                _pauseStatus.postValue("Une erreur s'est produite lors de l'opération de suspension de suivi")
            }
        }
    }

    fun observeFallsList(): MutableLiveData<List<Fall>?> {
        return mutableFallsList
    }

    fun observeDailyFalls(): MutableLiveData<DailyFallsResponse?> {
        return _dailyFallsData
    }

    fun observeDisconnectStatus(): LiveData<String> {
        return _disconnectStatus
    }

    fun observePauseStatus(): LiveData<String> {
        return _pauseStatus
    }

    private fun handleErrorResponse(errorBody: ResponseBody?) {
        val errorMessage = errorBody?.string()?.let { errorContent ->
            try {
                val jsonObject = JSONObject(errorContent)
                val nestedMessage = jsonObject.getJSONObject("message").getString("message")
                nestedMessage
            } catch (e: Exception) {
                "Une erreur s'est produite lors de l'authentification"
            }
        } ?: "Une erreur s'est produite lors de l'authentification"
        _addErrorStatus.postValue(errorMessage)
    }
}
