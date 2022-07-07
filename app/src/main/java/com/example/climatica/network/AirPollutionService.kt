package com.example.climatica.network

import com.example.climatica.models.AirPollutionResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AirPollutionService {

    @GET("2.5/air_pollution")
    fun getAirPollution(
        @Query("lat") lat:Double,
        @Query("lon") lon:Double,
        @Query("appid") appid:String?
    ): Call<AirPollutionResponse>
}