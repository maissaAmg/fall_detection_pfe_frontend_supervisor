package com.example.appfall.retrofit

import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.models.DailyFallsRequest
import com.example.appfall.data.models.DailyFallsResponse
import com.example.appfall.data.models.DisconnectedResponse
import com.example.appfall.data.models.FallFilter
import com.example.appfall.data.models.FallResponse
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.MonthYear
import com.example.appfall.data.models.PausedResponse
import com.example.appfall.data.models.TopicSubscription
import com.example.appfall.data.models.TopicSubscriptionResponse
import com.example.appfall.data.models.UpdateSupervisorEmailRequest
import com.example.appfall.data.models.UpdateSupervisorPasswordRequest
import com.example.appfall.data.models.UpdateSupervisorNameRequest
import com.example.appfall.data.models.UpdateSupervisorResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import com.example.appfall.data.models.isPausedRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @POST("falls/getDailyFalls")
    fun getDailyFalls(@Header("Authorization") token: String,
                      @Body request: DailyFallsRequest): Call<DailyFallsResponse>

    @PUT("supervisors/updateSupervisor")
    fun updateSupervisorName(
        @Header("Authorization") token: String,
        @Body request: UpdateSupervisorNameRequest
    ): Call<UpdateSupervisorResponse>

    @PUT("supervisors/updateSupervisor")
    fun updateSupervisorEmail(
        @Header("Authorization") token: String,
        @Body request: UpdateSupervisorEmailRequest
    ): Call<UpdateSupervisorResponse>

    @PUT("supervisors/updateSupervisor")
    fun updateSupervisorPassword(
        @Header("Authorization") token: String,
        @Body request: UpdateSupervisorPasswordRequest
    ): Call<UpdateSupervisorResponse>

    @GET("/notifications/disconnect/{contactId}")
    fun disconnect(@Header("Authorization") token: String,
                   @Path("contactId") contactId: String ): Call<DisconnectedResponse>

    @PUT("/notifications/pauseNotification/{contactId}")
    fun pause(@Header("Authorization") token: String,
                   @Path("contactId") contactId: String,
                   @Body request: isPausedRequest): Call<PausedResponse>

    @POST("notifications/subscribeToTopic")
    fun subscribeToTopic(
        @Body request: TopicSubscription,
    ): Call<TopicSubscriptionResponse>
}
