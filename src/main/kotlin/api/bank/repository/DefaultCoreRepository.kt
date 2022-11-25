package api.bank.repository

import api.bank.models.RequestDetail
import api.bank.models.ResponseDetail
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.toVariableRepresentation
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Single
class DefaultCoreRepository constructor(
    private val dispatcherProvider: DispatcherProvider,
) : CoreRepository {

    override suspend fun executeRequest(
        requestDetail: RequestDetail,
        variables: List<Array<String>>
    ): ResponseDetail {
        return withContext(dispatcherProvider.io) {
            val updatedDetail = substituteVariables(requestDetail, variables)
            val urlConnection: HttpURLConnection = URL(updatedDetail.url).openConnection() as HttpURLConnection

            return@withContext try {
                urlConnection.requestMethod = updatedDetail.method
                setHeader(urlConnection, updatedDetail)

                if (updatedDetail.method == "POST" || updatedDetail.method == "PUT") {
                    urlConnection.doOutput = true
                    urlConnection.outputStream.bufferedWriter().use { out -> out.write(updatedDetail.body!!) }
                }

                when {
                    urlConnection.responseCode >= 400 -> {
                        val body = urlConnection.errorStream.bufferedReader().use { reader -> reader.readText() }
                        ResponseDetail(body, urlConnection.responseCode, urlConnection.responseMessage)
                    }

                    else -> {
                        val body = urlConnection.inputStream.bufferedReader().use { reader -> reader.readText() }
                        ResponseDetail(body, urlConnection.responseCode, urlConnection.responseMessage)
                    }
                }
            } finally {
                urlConnection.disconnect()
            }
        }
    }

    private fun substituteVariables(
        requestDetail: RequestDetail,
        variableItems: List<Array<String>>
    ): RequestDetail {
        val variables = variableItems.toMutableList()

        addPluginProvidedVariables(variables)

        val newDetail = requestDetail.copy()
        var url = requestDetail.url
        var body = requestDetail.body

        for ((key, value) in variables) {
            val keyAsVariable = key.toVariableRepresentation()

            url = url?.replace(keyAsVariable, value)
            body = body?.replace(keyAsVariable, value)
        }

        val headers = ArrayList<Array<String>>()
        newDetail.header.forEach { headers.add(arrayOf(it[0], it[1])) }

        for ((key, value) in variables) {
            val keyAsVariable = key.toVariableRepresentation()

            for (header in headers) {
                header[0] = header[0].replace(keyAsVariable, value)
                header[1] = header[1].replace(keyAsVariable, value)
            }
        }

        return newDetail.copy(
            url = url,
            body = body,
            header = headers
        )
    }

    private fun setHeader(urlConnection: HttpURLConnection, requestDetail: RequestDetail) {
        val headers = requestDetail.header
        for (header in headers) {
            if (header.isEmpty() || header[0].isBlank() || header[1].isBlank()) {
                continue
            }
            urlConnection.setRequestProperty(header[0], header[1])
        }
    }

    private fun addPluginProvidedVariables(variables: MutableList<Array<String>>) {
        variables.add(
            arrayOf("UUID", UUID.randomUUID().toString()),
        )
    }
}
