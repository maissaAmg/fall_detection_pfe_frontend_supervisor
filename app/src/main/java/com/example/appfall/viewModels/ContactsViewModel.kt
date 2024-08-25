package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.models.ConnectedSupervisor
import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val mutableContactsList: MutableLiveData<List<ConnectedSupervisor>?> = MutableLiveData()
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    private val isListEmpty: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var token: String

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                token = it.token
                getContacts()
            }
        }
    }

    fun getContacts() {
        isLoading.postValue(true)
        RetrofitInstance.fallApi.getContacts("Bearer $token").enqueue(object : Callback<ConnectedSupervisorsResponse> {
            override fun onResponse(call: Call<ConnectedSupervisorsResponse>, response: Response<ConnectedSupervisorsResponse>) {
                val contacts = response.body()?.connectedUsers ?: null
                mutableContactsList.postValue(contacts)
                if (contacts != null) {
                    isListEmpty.postValue(contacts.isEmpty())
                }
                isLoading.postValue(false)
            }

            override fun onFailure(call: Call<ConnectedSupervisorsResponse>, t: Throwable) {
                Log.d("ContactsViewModel", t.message.toString())
                mutableContactsList.postValue(emptyList())
                isListEmpty.postValue(true)
                isLoading.postValue(false)
            }
        })
    }

    fun observeContactsList(): MutableLiveData<List<ConnectedSupervisor>?> {
        return mutableContactsList
    }

    fun observeLoading(): LiveData<Boolean> {
        return isLoading
    }

    fun observeIsListEmpty(): LiveData<Boolean> {
        return isListEmpty
    }
}

