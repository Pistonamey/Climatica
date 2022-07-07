package com.example.climatica.network

import com.example.climatica.models.GeocodingResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {

    @GET("1.0/direct")
    fun getGeocoding(
    @Query("q") q:String,
    @Query("limit") limit:Int,
    @Query("appid") appid:String?,

    ): Call<GeocodingResponse>
}