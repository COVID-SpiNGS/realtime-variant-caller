package de.htw.berlin.covidspings

data class Site(
    val referenceName: String,
    val reference: Byte,
    var totalDepth: Int,
) {
    val variants = mutableMapOf<Byte, MutableList<Byte>>()
}


