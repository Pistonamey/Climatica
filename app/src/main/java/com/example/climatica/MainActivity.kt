package com.example.climatica

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.climatica.models.AirPollutionResponse
import com.example.climatica.models.WeatherResponse
import com.example.climatica.network.AirPollutionService
import com.example.climatica.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    private lateinit var msharedPreferences:SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        msharedPreferences=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)

        val city_Weather:Button=findViewById(R.id.city_name_button)
        city_Weather.setOnClickListener {
            city_weather_page()
        }

        setupUI()

        //if location is not enabled, go to location settings
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your Location Provider is turned off, Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            //intent to the enable location settings
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }//if location is enabled, display toast
        else {
            //get the permissions(if they are checked or not)
            Dexter.withContext(this).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    //check if all the permissions are granted
                    if (p0!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }

                    //check if any permission is denied
                    if (p0!!.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity,
                            "You have denied location Permission, Please give enable the app to use your location.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogPermissions()
                }
            }).onSameThread().check()
        }
    }

    private fun city_weather_page() {
        val intent=Intent(this, CityWeatherPage::class.java)
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    //latitude and longitude
    val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val myLastLocation: Location = p0.lastLocation!!
            val latitude = myLastLocation.latitude


            val longitude = myLastLocation.longitude

            getLocationWeatherDetails(latitude,longitude)
        }
    }

    //Retrofit Call function
     private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this)) {

            //Retrofit implementation for the BASE_URL
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            //make a service based on the retrofit
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            //make a listcall based on the service
            val listCall: Call<WeatherResponse> =
                service.getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID,)

            showCustomProgressDialog()

            //Performing in the background
            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response!!.isSuccessful) {
                        hideProgressDialog()
                        //Get the whole response body (the whole string) of the API
                        val weatherList: WeatherResponse = response.body()!!


                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        // Save the converted string to shared preferences
                        val editor = msharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        //setup the UI
                        //setupUI()
                        getLocationAirPollutionDetails(latitude,longitude)

                    } else {
                        //check if the code is bad connection or something else
                        val rc = response.code()
                        when (rc) {
                            400 -> {

                            }
                            404 -> {

                            }
                            else -> {

                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {


                    hideProgressDialog()

                }

            })

            Toast.makeText(
                this@MainActivity,
                "You Internet connection is available",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "No Internet Connection available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }





    private fun getLocationAirPollutionDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this)) {

            //Retrofit implementation for the BASE_URL
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            //make a service based on the retrofit
            val service: AirPollutionService =
                retrofit.create<AirPollutionService>(AirPollutionService::class.java)

            //make a listcall based on the service
            val listCall: Call<AirPollutionResponse> =
                service.getAirPollution(latitude, longitude, Constants.APP_ID,)

            showCustomProgressDialog()

            //Performing in the background
            listCall.enqueue(object : Callback<AirPollutionResponse> {
                override fun onResponse(
                    call: Call<AirPollutionResponse>,
                    response: Response<AirPollutionResponse>
                ) {
                    if (response!!.isSuccessful) {
                        hideProgressDialog()
                        //Get the whole response body (the whole string) of the API
                        val airPollutionList: AirPollutionResponse = response.body()!!


                        val airPollutionResponseJsonString = Gson().toJson(airPollutionList)
                        // Save the converted string to shared preferences
                        val editor = msharedPreferences.edit()
                        editor.putString(Constants.AIR_POLLUTION_RESPONSE_DATA, airPollutionResponseJsonString)
                        editor.apply()


                        //setup the UI
                        setupUI()

                    } else {
                        //check if the code is bad connection or something else
                        val rc = response.code()
                        when (rc) {
                            400 -> {

                            }
                            404 -> {

                            }
                            else -> {

                            }
                        }
                    }
                }

                override fun onFailure(call: Call<AirPollutionResponse>, t: Throwable) {


                    hideProgressDialog()

                }

            })

            Toast.makeText(
                this@MainActivity,
                "You Internet connection is available",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "No Internet Connection available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    //after user denied the access and we really need the permission
    private fun showRationalDialogPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned permissions Off")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    //function for checking location enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        /* Set the screen content from a layout resource .
        The resource will be inflated , adding all top-level views to the scre*/
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        // Start the dialog and display it on screen .
        mProgressDialog!!.show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                requestLocationData()
                true
            }else-> super.onOptionsItemSelected(item)
        }

    }

    //hide the progressbar function
    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }

    }


    fun setupUI() {

        val weatherResponseJsonString=msharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        val airPollutionResponseJsonString=msharedPreferences.getString(Constants.AIR_POLLUTION_RESPONSE_DATA,"")


        if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)

            for(i in weatherList.weather.indices){

                val tv_main:TextView = findViewById(R.id.tv_main)
                tv_main.text=weatherList.weather[i].main
                val tv_description:TextView=findViewById(R.id.tv_main_description)
                tv_description.text=weatherList.weather[i].description
                val tv_temp:TextView=findViewById(R.id.tv_temp)
                tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                val tv_humidity:TextView=findViewById(R.id.tv_humidity)
                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                val tv_min:TextView=findViewById(R.id.tv_min)
                tv_min.text = weatherList.main.temp_min.toString() + " min"
                val tv_max:TextView=findViewById(R.id.tv_max)
                tv_max.text = weatherList.main.temp_max.toString() + " max"
                val tv_speed:TextView=findViewById(R.id.tv_speed)
                tv_speed.text = weatherList.wind.speed.toString()
                val tv_name:TextView=findViewById(R.id.tv_name)
                tv_name.text = weatherList.name
                val tv_country:TextView=findViewById(R.id.tv_country)
                tv_country.text = weatherList.sys.country
                val tv_sunrise_time:TextView=findViewById(R.id.tv_sunrise_time)
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
                val tv_sunset_time:TextView=findViewById(R.id.tv_sunset_time)
                tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

                val iv_main:ImageView=findViewById(R.id.iv_main)
                when(weatherList.weather[i].icon){
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }

        }

        if(!airPollutionResponseJsonString.isNullOrEmpty()){
            val airPollutionList=Gson().fromJson(airPollutionResponseJsonString,AirPollutionResponse::class.java)



                val co_levels:TextView=findViewById(R.id.co_levels)
                co_levels.text = airPollutionList.list[0].components.co.toString()
                val no_levels:TextView=findViewById(R.id.no_levels)
                no_levels.text = airPollutionList.list[0].components.no.toString()
                val no2_levels:TextView=findViewById(R.id.no2_levels)
                no2_levels.text = airPollutionList.list[0].components.no2.toString()
                val o3_levels:TextView=findViewById(R.id.O3_levels)
                o3_levels.text = airPollutionList.list[0].components.o3.toString()
                val so2_levels:TextView=findViewById(R.id.so2_levels)
                so2_levels.text = airPollutionList.list[0].components.so2.toString()
                val pm2_5_levels:TextView=findViewById(R.id.pm2_5_levels)
                pm2_5_levels.text = airPollutionList.list[0].components.pm2_5.toString()
                val nh3_levels:TextView=findViewById(R.id.nh3_levels)
                nh3_levels.text = airPollutionList.list[0].components.nh3.toString()
                val pm_10_levels:TextView=findViewById(R.id.pm10_levels)
                pm_10_levels.text = airPollutionList.list[0].components.nh3.toString()


        }


    }

    private fun getUnit(value: String): String? {

        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}