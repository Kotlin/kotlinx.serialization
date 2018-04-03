package com.example.model_reflective

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class User {

    @SerialName("_id")
    @SerializedName("_id") // Annotation needed for GSON
    @Json(name = "_id")
    var id: String? = null

    var index: Int = 0

    var guid: String? = null

    @SerialName("is_active")
    @SerializedName("is_active") // Annotation needed for GSON
    @Json(name = "is_active")
    var isActive: Boolean = false

    var balance: String? = null

    @SerialName("picture")
    @SerializedName("picture") // Annotation needed for GSON
    @Json(name = "picture")
    var pictureUrl: String? = null

    var age: Int = 0

    var name: Name? = null

    var company: String? = null

    var email: String? = null

    var address: String? = null

    var about: String? = null

    var registered: String? = null

    var latitude: Double = 0.toDouble()

    var longitude: Double = 0.toDouble()

    var tags: List<String>? = null

    var range: List<Int>? = null

    var friends: List<Friend>? = null

    var images: List<Image>? = null

    var greeting: String? = null

    @SerialName("favorite_fruit")
    @SerializedName("favorite_fruit") // Annotation needed for GSON
    @Json(name = "favorite_fruit")
    var favoriteFruit: String? = null

    @SerialName("eye_color")
    @SerializedName("eye_color") // Annotation needed for GSON
    @Json(name = "eye_color")
    var eyeColor: String? = null

    var phone: String? = null
}
