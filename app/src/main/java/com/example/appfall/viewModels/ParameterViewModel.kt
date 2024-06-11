package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.appfall.data.models.UpdateSupervisorEmailRequest
import com.example.appfall.data.models.UpdateSupervisorPasswordRequest
import com.example.appfall.data.models.UpdateSupervisorNameRequest
import com.example.appfall.data.models.UpdateSupervisorResponse
import com.example.appfall.retrofit.RetrofitInstance
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ParameterViewModel(application: Application) : AndroidViewModel(application) {
    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2NTczMTY0ODViOTFiZjY3ZGZjNjM5ZiIsImlhdCI6MTcxNjk5MDMwOH0.4HxGAUghy9zX-LzXG7ukzY3ugx9Pld_kDGz342E0_Uc"

    private val _updateStatus: MutableLiveData<Boolean> = MutableLiveData()
    val updateStatus: LiveData<Boolean> = _updateStatus

    private val _updateErrorStatus: MutableLiveData<String> = MutableLiveData()
    val updateErrorStatus: LiveData<String> = _updateErrorStatus

    fun updateSupervisorName(name: String) {
        val request = UpdateSupervisorNameRequest(name)
        RetrofitInstance.fallApi.updateSupervisorName("Bearer $token", request).enqueue(object : Callback<UpdateSupervisorResponse> {
            override fun onResponse(call: Call<UpdateSupervisorResponse>, response: Response<UpdateSupervisorResponse>) {
                if (response.isSuccessful) {
                    _updateStatus.value = true
                    Log.d("ParameterViewModel","${response.body()}")
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("ParameterViewModel", "Failed to update name: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateSupervisorResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de la mise à jour du nom"
                _updateErrorStatus.postValue(errorMessage)
                Log.e("ParameterViewModel", "Failed to update name", t)
            }
        })
    }

    fun updateSupervisorEmail(email: String) {
        val request = UpdateSupervisorEmailRequest(email)
        RetrofitInstance.fallApi.updateSupervisorEmail("Bearer $token", request).enqueue(object : Callback<UpdateSupervisorResponse> {
            override fun onResponse(call: Call<UpdateSupervisorResponse>, response: Response<UpdateSupervisorResponse>) {
                if (response.isSuccessful) {
                    _updateStatus.value = true
                    Log.d("ParameterViewModel","${response.body()}")
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("ParameterViewModel", "Failed to update email: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateSupervisorResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de la mise à jour de l'email"
                _updateErrorStatus.postValue(errorMessage)
                Log.e("ParameterViewModel", "Failed to update email", t)
            }
        })
    }

    fun updateSupervisorPassword(password: String) {
        val request = UpdateSupervisorPasswordRequest(password)
        RetrofitInstance.fallApi.updateSupervisorPassword("Bearer $token", request).enqueue(object : Callback<UpdateSupervisorResponse> {
            override fun onResponse(call: Call<UpdateSupervisorResponse>, response: Response<UpdateSupervisorResponse>) {
                if (response.isSuccessful) {
                    _updateStatus.value = true
                    Log.d("ParameterViewModel","${response.body()}")
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("ParameterViewModel", "Failed to update password: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateSupervisorResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de la mise à jour du mot de passe"
                _updateErrorStatus.postValue(errorMessage)
                Log.e("ParameterViewModel", "Failed to update password", t)
            }
        })
    }

    private fun handleErrorResponse(errorBody: ResponseBody?) {
        val errorMessage = errorBody?.string()?.let { errorContent ->
            try {
                val jsonObject = JSONObject(errorContent)
                val nestedMessage = jsonObject.getJSONObject("message").getString("message")
                nestedMessage
            } catch (e: Exception) {
                "Une erreur s'est produite lors de la mise à jour"
            }
        } ?: "Une erreur s'est produite lors de la mise à jour"
        _updateErrorStatus.postValue(errorMessage)
    }
}