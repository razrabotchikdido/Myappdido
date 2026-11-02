package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {

    const val KAABA_LATITUDE = 21.422477
    const val KAABA_LONGITUDE = 39.826181

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    data class QiblaInfo(
        val bearingDegrees: Float, // 0..360 from true North
        val distanceKm: Int,
        val compassDirectionRu: String
    )

    fun calculateQibla(latitude: Double, longitude: Double): QiblaInfo {
        val lat1 = latitude * DEG_TO_RAD
        val lat2 = KAABA_LATITUDE * DEG_TO_RAD
        val dLon = (KAABA_LONGITUDE - longitude) * DEG_TO_RAD

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = (atan2(y, x) * RAD_TO_DEG).toFloat()
        bearing = (bearing + 360f) % 360f

        // Distance in km via Haversine
        val dLat = (KAABA_LATITUDE - latitude) * DEG_TO_RAD
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = (6371.0 * c).toInt()

        val directionRu = getCompassDirectionRu(bearing)

        return QiblaInfo(
            bearingDegrees = bearing,
            distanceKm = distanceKm,
            compassDirectionRu = directionRu
        )
    }

    private fun getCompassDirectionRu(bearing: Float): String = when {
        bearing in 337.5..360.0 || bearing in 0.0..22.5 -> "Север (С)"
        bearing in 22.5..67.5 -> "Северо-Восток (СВ)"
        bearing in 67.5..112.5 -> "Восток (В)"
        bearing in 112.5..157.5 -> "Юго-Восток (ЮВ)"
        bearing in 157.5..202.5 -> "Юг (Ю)"
        bearing in 202.5..247.5 -> "Юго-Запад (ЮЗ)"
        bearing in 247.5..292.5 -> "Запад (З)"
        else -> "Северо-Запад (СЗ)"
    }
}
