package com.example.testretrofitsanwich.data.model

import com.example.testretrofitsanwich.network.model.BeerDto

data class BeerModel(
    val id: Int,
    val name: String,
    val tagline: String,
    val description: String,
    val firstBrewed: String,
    val imageUrl: String?
)

fun BeerDto.toModel() = BeerModel(
    id = id,
    name = name,
    tagline = tagline,
    description = description,
    firstBrewed = firstBrewed,
    imageUrl = imageUrl,
)