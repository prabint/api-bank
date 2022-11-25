package ui.utils

import api.bank.utils.toVariableRepresentation

object TestData {
    private const val varContentType = "content_type"

    internal const val varBaseUrl = "baseurl"

    internal val uuidRegex =
        Regex("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$")

    private const val BASE_URL = "https://jsonplaceholder.typicode.com"

    internal val unFormattedBody =
        """ {"body":"${"body".toVariableRepresentation()}", "isReal":${"isReal".toVariableRepresentation()}, "count":${"count".toVariableRepresentation()}, "token":"${"UUID".toVariableRepresentation()}" }"""

    internal val formattedBody = """
            {
              "body": "${"body".toVariableRepresentation()}",
              "isReal": ${"isReal".toVariableRepresentation()},
              "count": ${"count".toVariableRepresentation()},
              "token": "${"UUID".toVariableRepresentation()}"
            }
        """.trimIndent()

    internal val variablesData = listOf(
        listOf("body", "Hello World"),
        listOf("isReal", "true"),
        listOf("count", "599"),
        listOf(varContentType, "application/json; charset=UTF-8"),
        listOf(varBaseUrl, BASE_URL),
    )

    internal val headerData = listOf(listOf("Content-type", varContentType.toVariableRepresentation()))
}
