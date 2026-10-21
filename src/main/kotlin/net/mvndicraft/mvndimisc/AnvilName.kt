package net.mvndicraft.mvndimisc

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

internal object AnvilName {
    private const val RESET = "<reset>"
    private val legacyCode = Regex("[&§](#[0-9a-fA-F]{6}|[0-9a-fA-Fk-oK-OrR])")
    private val legacyTags = listOf(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white", "obfuscated", "bold", "strikethrough", "underlined", "italic"
    )
    private const val LEGACY_CODES = "0123456789abcdefklmno"
    private val miniMessage = MiniMessage.builder().tags(TagResolver.resolver(
        StandardTags.color(), StandardTags.decorations(), StandardTags.reset(),
        StandardTags.gradient(), StandardTags.rainbow()
    )).build()

    fun parse(name: String): Component {
        val markup = legacyCode.replace(name) { match ->
            val code = match.groupValues[1].lowercase()
            when {
                code == "r" -> RESET
                code.startsWith('#') -> "$RESET<$code>"
                else -> {
                    val index = LEGACY_CODES.indexOf(code)
                    val reset = if (index < 16) RESET else ""
                    "$reset<${legacyTags[index]}>"
                }
            }
        }
        return miniMessage.deserialize(markup).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }
}
