package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.UpdateSupervisorNameRequest
import com.example.appfall.data.models.UpdateSupervisorPasswordRequest
import com.example.appfall.data.models.UpdateSupervisorResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2NTczMTY0ODViOTFiZjY3ZGZjNjM5ZiIsImlhdCI6MTcxNjk5MDMwOH0.4HxGAUghy9zX-LzXG7ukzY3ugx9Pld_kDGz342E0_Uc"

    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()

    private val _loginResponse: MutableLiveData<LoginResponse> = MutableLiveData()
    val loginResponse: LiveData<LoginResponse> = _loginResponse

    private val _userAddStatus: MutableLiveData<Boolean> = MutableLiveData()
    val userAddStatus: LiveData<Boolean> = _userAddStatus

    private val _addErrorStatus: MutableLiveData<String> = MutableLiveData()
    val addErrorStatus: LiveData<String> = _addErrorStatus

    private val _localUser: MutableLiveData<UserDaoModel?> = MutableLiveData()
    val localUser: MutableLiveData<UserDaoModel?> = _localUser

    private val _updateNameResponse: MutableLiveData<UpdateSupervisorResponse> = MutableLiveData()
    val updateNameResponse: LiveData<UpdateSupervisorResponse> = _updateNameResponse

    private val _updatePasswordResponse: MutableLiveData<UpdateSupervisorResponse> = MutableLiveData()
    val updatePasswordResponse: LiveData<UpdateSupervisorResponse> = _updatePasswordResponse


    fun addUser(user: User) {

        RetrofitInstance.fallApi.addUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                    viewModelScope.launch(Dispatchers.IO) {
                        val userDaoModel = UserDaoModel(
                            phone = user.phone,
                            name = user.name,
                            token = _loginResponse.value?.data?.accessToken ?: ""
                        )
                        try {
                            userDao.addUser(userDaoModel)
                            _userAddStatus.postValue(true)
                        } catch (e: Exception) {
                            _userAddStatus.postValue(false)
                            handleErrorResponse(response.errorBody())
                            Log.e("UserViewModel", "Failed to add user to local database", e)
                        }
                    }
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to add user to remote server: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de l'authentification"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", "Failed to add user to remote server: $errorMessage", t)
            }
        })
    }

    fun loginUser(user: UserCredential) {
        RetrofitInstance.fallApi.loginUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                    viewModelScope.launch(Dispatchers.IO) {
                        val userDaoModel = UserDaoModel(
                            phone = _loginResponse.value?.data?.phone ?: "",
                            name = _loginResponse.value?.data?.name ?: "", //user.name
                            token = _loginResponse.value?.data?.accessToken ?: ""
                        )
                        try {
                            userDao.addUser(userDaoModel)
                            _userAddStatus.postValue(true)
                        } catch (e: Exception) {
                            _userAddStatus.postValue(false)
                            handleErrorResponse(response.errorBody())
                            Log.e("UserViewModel", "Failed to add user to local database", e)
                        }
                    }
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to login: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de l'authentification"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", "Failed to login", t)
            }
        })
    }

    fun getLocalUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val localUser = userDao.getUser()
            _localUser.postValue(localUser)
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUser()
        }
    }

    fun updateName(name: String) {
        val request = UpdateSupervisorNameRequest(name)
        RetrofitInstance.fallApi.updateSupervisorName("Bearer $token", request).enqueue(object : Callback<UpdateSupervisorResponse> {
            override fun onResponse(call: Call<UpdateSupervisorResponse>, response: Response<UpdateSupervisorResponse>) {
                if (response.isSuccessful) {
                    _updateNameResponse.value = response.body()
                    Log.d("UserViewModel", "${response.body()}")
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to update name: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateSupervisorResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de la mise à jour du nom"
                _addErrorStatus.postValue(errorMessage)
                Log.e("ParameterViewModel", "Failed to update name", t)
            }
        })
    }

    fun updatePassword(password: String) {
        val request = UpdateSupervisorPasswordRequest(password)
        RetrofitInstance.fallApi.updateSupervisorPassword("Bearer $token", request).enqueue(object : Callback<UpdateSupervisorResponse> {
            override fun onResponse(call: Call<UpdateSupervisorResponse>, response: Response<UpdateSupervisorResponse>) {
                if (response.isSuccessful) {
                    _updatePasswordResponse.value = response.body()
                    Log.d("UserViewModel", "${response.body()}")
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to update password: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateSupervisorResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de la mise à jour du mot de passe"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", "Failed to update password", t)
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
                "Une erreur s'est produite lors de l'authentification"
            }
        } ?: "Une erreur s'est produite lors de l'authentification"
        _addErrorStatus.postValue(errorMessage)
    }
}