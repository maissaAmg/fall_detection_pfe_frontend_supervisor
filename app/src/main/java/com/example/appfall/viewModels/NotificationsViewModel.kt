package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.models.TopicSubscription
import com.example.appfall.data.models.TopicSubscriptionResponse
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _subscriptionStatus: MutableLiveData<String> = MutableLiveData()
    val subscriptionStatus: LiveData<String> = _subscriptionStatus

    private val _subscriptionResponse: MutableLiveData<TopicSubscriptionResponse?> = MutableLiveData()
    val subscriptionResponse: LiveData<TopicSubscriptionResponse?> = _subscriptionResponse

    fun subscribeToTopic(subscriptionRequest: TopicSubscription) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.fallApi.subscribeToTopic(subscriptionRequest).execute()
                }
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    _subscriptionResponse.postValue(responseBody)
                    _subscriptionStatus.postValue("Subscription successful: ${responseBody?.message}")
                } else {
                    handleErrorResponse(response.errorBody())
                }
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "API call failed: ${e.message}")
                _subscriptionStatus.postValue("Subscription failed: ${e.message}")
            }
        }
    }

    private fun handleErrorResponse(errorBody: okhttp3.ResponseBody?) {
        val errorMessage = errorBody?.string()?.let { errorContent ->
            try {
                val jsonObject = JSONObject(errorContent)
                jsonObject.getString("message") // Assuming "message" is the key in your error response
            } catch (e: Exception) {
                "An error occurred during subscription"
            }
        } ?: "An error occurred during subscription"
        _subscriptionStatus.postValue(errorMessage)
    }
}
