/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.Serializable

@Serializable
data class GeoCoordinate(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {

    init {
        require(latitude >= 0) { "latitude must be non-negative" }
        require(longitude >= 0) { "longitude must be non-negative" }
    }
}

