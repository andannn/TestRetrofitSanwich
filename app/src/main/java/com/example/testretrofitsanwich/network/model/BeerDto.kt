package com.example.testretrofitsanwich.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BeerDto(
    val id: Int,
    val name: String,
    val tagline: String,
    val description: String,
    @SerialName("first_brewed") val firstBrewed: String,
    @SerialName("image_url") val imageUrl: String?
)