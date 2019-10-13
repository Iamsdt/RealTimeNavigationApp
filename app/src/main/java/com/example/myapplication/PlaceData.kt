package com.example.myapplication

data class PlaceData(
    val center: List<Double>,
    val context: List<Context>,
    val geometry: Geometry,
    val id: String,
    val place_name: String,
    val place_type: List<String>,
    val properties: Properties,
    val relevance: Double,
    val text: String,
    val type: String
)

data class Context(
    val id: String,
    val short_code: String,
    val text: String,
    val wikidata: String
)

data class Geometry(
    val coordinates: List<Double>,
    val type: String
)

data class Properties(
    val address: String,
    val category: String,
    val landmark: Boolean
)