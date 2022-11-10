package de.htw.berlin.covidspings

data class JsonSite(
    val referenceName: String,
    val reference: String,
    val totalDepth: Int,
    val variants: Map<String, List<Byte>>
)