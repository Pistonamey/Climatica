package com.example.climatica.models

import java.io.Serializable

data class PollutionList (
    val main:PollutionMain,
    val components:Components
):Serializable