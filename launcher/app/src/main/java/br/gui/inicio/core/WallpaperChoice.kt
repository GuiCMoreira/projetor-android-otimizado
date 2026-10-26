package br.gui.inicio.core

/** Papel de parede escolhido: degradê desenhado, foto incluída no app ou foto enviada pelo computador. */
sealed interface WallpaperChoice {
    data class Gradient(val index: Int) : WallpaperChoice
    data class Asset(val name: String) : WallpaperChoice
    data class UserFile(val name: String) : WallpaperChoice

    fun serialize(): String = when (this) {
        is Gradient -> "grad:$index"
        is Asset -> "asset:$name"
        is UserFile -> "file:$name"
    }

    companion object {
        const val GRADIENT_COUNT = 4
        val DEFAULT: WallpaperChoice = Gradient(0)

        private val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,80}\\.(jpg|jpeg|png|webp)", RegexOption.IGNORE_CASE)

        /** Só nome de arquivo simples: sem barra, sem "..", começando por letra ou número. */
        fun isSafeName(name: String): Boolean = SAFE_NAME.matches(name) && ".." !in name

        fun parse(raw: String?): WallpaperChoice {
            val parts = raw?.split(":", limit = 2)
            if (parts == null || parts.size != 2) return DEFAULT
            val (kind, value) = parts
            return when (kind) {
                "grad" -> value.toIntOrNull()?.takeIf { it in 0 until GRADIENT_COUNT }?.let { Gradient(it) } ?: DEFAULT
                "asset" -> if (isSafeName(value)) Asset(value) else DEFAULT
                "file" -> if (isSafeName(value)) UserFile(value) else DEFAULT
                else -> DEFAULT
            }
        }
    }
}
