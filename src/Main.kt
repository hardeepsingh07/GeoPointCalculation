import java.io.File

/**
 * Define class purpose here: Distance Calculations
 *
 * Created by hardeepsingh
 * Date: April 18, 2018.
 */
data class GeoPoint(var latitude: Double, var longitude: Double)

data class GeoPointInfo(var pathGeoPoint: GeoPoint, var locationGeoPoint: GeoPoint,
                        var distance: Double, var residual: GeoPoint?) {
    override fun toString(): String {
        return "Path Point: $pathGeoPoint | Location Point : $locationGeoPoint " +
                "| Distance = ${"%.5f".format(distance)} | Residual: $residual"
    }
}

data class GeoCalculationResults(var geoPointInfoList: List<GeoPointInfo>, var rmseLat: Double,
                                 var rmseLng: Double, var rmseDist: Double)

fun main(args: Array<String>) {
    var fileNumber = "17"
    val pathList = mutableListOf<GeoPoint>()
    val locationList = mutableListOf<GeoPoint>()

    //Read path way-points file
    File("path_gm_${fileNumber}_false.txt").useLines { lines ->
        lines.forEach {
            val result = it.split(",")
            pathList.add(GeoPoint(result[0].toDouble(), result[1].toDouble()))
        }
    }

    //Read user movement file
    File("user_location_${fileNumber}_false.txt").useLines { lines ->
        lines.forEach {
            val result = it.split(",")
            locationList.add(GeoPoint(result[0].toDouble(), result[1].toDouble()))
        }
    }
    println("Path List Size: ${pathList.size}")
    println("Location List Size: ${locationList.size}")

    //Find closest Location GEO points from Path file and store it
    val geoPointInfo = mutableListOf<GeoPointInfo>()
    locationList.forEach({
        val location = it
        var shortestDistance = Double.MAX_VALUE
        var shortDistancePoint: GeoPoint? = null
        pathList.forEach({
            val distance = getDistanceInMeters(location, it)
            if (shortestDistance > distance) {
                shortestDistance = distance
                shortDistancePoint = it
            }
        })
        geoPointInfo.add(GeoPointInfo(shortDistancePoint!!, location, "%.5f".format(shortestDistance).toDouble(), null))
    })

    //Get average of Location points with respect to Path points and calculate residual
    println("GeoPointInfo Before Distinct Size: ${geoPointInfo.size}")
    var distinctGeoPointInfo = mutableListOf<GeoPointInfo>()
    pathList.forEach({
        val pathPoint = it
        var averageLat = 0.0
        var averageLng = 0.0
        var dataPoint: GeoPointInfo? = null
        var counter = 0
        geoPointInfo.forEach({
            dataPoint = it
            if(pathPoint == it.pathGeoPoint) {
                averageLat += it.locationGeoPoint.latitude
                averageLng += it.locationGeoPoint.longitude
                counter++
            }
        })
        if(averageLat != 0.0 && averageLng != 0.0) {
            val averageLocationGeoPoint = GeoPoint(averageLat / counter, averageLng / counter)
            val newGeoInfoPointInfo = GeoPointInfo(pathPoint, averageLocationGeoPoint, dataPoint!!.distance,
                    GeoPoint(it.latitude - averageLocationGeoPoint.latitude, it.longitude - averageLocationGeoPoint.longitude))
            distinctGeoPointInfo.add(newGeoInfoPointInfo)
        }
    })
    println("GeoPointInfo After Distinct Size: ${distinctGeoPointInfo.size}")


//
    val (rmseLat, rmseLng, rmseDist) = rootMeanSquareError(distinctGeoPointInfo)
    println("Root Mean Square Error Latitude: $rmseLat")
    println("Root Mean Square Error Longitude: $rmseLng")
    println("Root Mean Square Error Distance: $rmseDist")

    //Create Result Object and Write to Result File
    var geoCalculationResults = GeoCalculationResults(distinctGeoPointInfo, rmseLat, rmseLng, rmseDist)
    File("result_calculation_${fileNumber}_false.txt").printWriter().use { out ->
        geoCalculationResults.geoPointInfoList.forEach({
            out.write(it.toString() + "\n")
        })
        out.write("Root Mean Square Error Latitude: ${geoCalculationResults.rmseLat} \n")
        out.write("Root Mean Square Error Longitude: ${geoCalculationResults.rmseLng} \n")
        out.write("Root Mean Square Error Distance: ${geoCalculationResults.rmseDist}")
    }
    println("Process Finished!!")
}

fun rootMeanSquareError(data: List<GeoPointInfo>): List<Double> {
    var totalLat = 0.0
    var totalLng = 0.0
    var totalDistance = 0.0
    var n = data.size
    data.forEach({
        totalLat += Math.pow(it.residual!!.latitude, 2.0)
        totalLng += Math.pow(it.residual!!.latitude, 2.0)
        totalDistance += Math.pow(it.distance, 2.0)
    })

    return listOf(Math.sqrt(totalLat/n), Math.sqrt(totalLng/n), Math.sqrt(totalDistance/n))
}

fun getDistanceInMeters(point1: GeoPoint, point2: GeoPoint): Double {
    val r = 6371
    val lat1 = point1.latitude
    val lng1 = point1.longitude
    val lat2 = point2.latitude
    val lng2 = point2.longitude
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2))
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c * 1000
}

