package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.models.DailyFallsResponse
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallFilter
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.MonthYear
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Path

class FallsViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableFallsList: MutableLiveData<List<Fall>> = MutableLiveData()
    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2NTczMTY0ODViOTFiZjY3ZGZjNjM5ZiIsImlhdCI6MTcxNjk5MDMwOH0.4HxGAUghy9zX-LzXG7ukzY3ugx9Pld_kDGz342E0_Uc"

    private val _addErrorStatus: MutableLiveData<String> = MutableLiveData()
    val addErrorStatus: LiveData<String> = _addErrorStatus

    private val mutableDailyFalls: MutableLiveData<DailyFallsResponse> = MutableLiveData()
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
                    Log.d("FallViewModel", "Response not successful: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("FallViewModel", "API call failed: ${e.message}")
            }
        }
    }

    fun getDailyFalls(userId: String, month: Int, year: Int) {
        RetrofitInstance.fallApi.getDailyFalls("Bearer $token", userId, MonthYear(month,year)).enqueue(object : Callback<DailyFallsResponse> {
            override fun onResponse(call: Call<DailyFallsResponse>, response: Response<DailyFallsResponse>) {
                if (response.isSuccessful) {
                   mutableDailyFalls.value = response.body()
                } else {
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

    fun observeFallsList(): LiveData<List<Fall>> {
        return mutableFallsList
    }

    fun observeDailyFalls(): LiveData<DailyFallsResponse> {
        return mutableDailyFalls
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
