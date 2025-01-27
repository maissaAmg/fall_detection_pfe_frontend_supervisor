package com.example.appfall.retrofit

import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.models.FallFilter
import com.example.appfall.data.models.FallResponse
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FallAPI {
    @GET("supervisors/getContacts")
    fun getContacts(@Header("Authorization") token: String): Call<ConnectedSupervisorsResponse>


    @POST("supervisors/addSupervisor")
    fun addUser(
        @Body request: User,
    ): Call<LoginResponse>

    @POST("login/loginSupervisor")
    fun loginUser(
        @Body request: UserCredential,
    ): Call<LoginResponse>

    @POST("falls/getFallsByUser/{userId}")
    fun getFallsByUser(@Header("Authorization") token: String,
                       @Path("userId") userId: String, @Body request: FallFilter
    ): Call<FallResponse>


}
