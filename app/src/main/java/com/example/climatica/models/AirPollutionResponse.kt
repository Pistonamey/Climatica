package com.example.climatica.models

import java.io.Serializable

data class AirPollutionResponse (
    val coord: Coord,
    val list:List<PollutionList>
        ):Serializable