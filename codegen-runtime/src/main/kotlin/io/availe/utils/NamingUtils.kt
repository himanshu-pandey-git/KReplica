package io.availe.utils

import java.util.regex.Pattern

object NamingUtils {
    private val CAMEL_CASE_SPLIT_REGEX = Pattern.compile("(?<=[a-z])(?=[A-Z])")

    private fun toPascalCase(input: String): String {
        val words = if (input.contains('_')) {
            input.split('_')
        } else {
            CAMEL_CASE_SPLIT_REGEX.split(input).toList()
        }

        return words.filter { it.isNotEmpty() }
            .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
    }

    fun generateValueClassName(prefix: String, propertyName: String): String {
        val propertyAsPascalCase = toPascalCase(propertyName)
        return "$prefix$propertyAsPascalCase"
    }
}