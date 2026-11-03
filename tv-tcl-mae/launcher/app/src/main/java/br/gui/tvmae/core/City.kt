package br.gui.tvmae.core

/** Cidade do clima, guardada como texto nas preferências. */
data class City(val name: String, val region: String, val lat: Double, val lon: Double) {
    fun serialize(): String = listOf(name, region, lat.toString(), lon.toString()).joinToString(SEP)

    companion object {
        private const val SEP = "|"

        fun parse(raw: String?): City? {
            val p = raw?.split(SEP) ?: return null
            if (p.size != 4 || p[0].isBlank()) return null
            val lat = p[2].toDoubleOrNull() ?: return null
            val lon = p[3].toDoubleOrNull() ?: return null
            return City(p[0], p[1], lat, lon)
        }
    }
}
