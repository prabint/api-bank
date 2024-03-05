package api.bank.utils

import api.bank.models.Constants.FILE_API_DETAIL_PERSISTENT
import api.bank.models.Constants.FILE_VARIABLE_COLLECTION_PERSISTENT
import api.bank.models.RequestDetail
import api.bank.models.RequestGroup
import api.bank.models.VariableCollection
import api.bank.settings.ApiBankSettingsStateComponent
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML
import java.io.File
import java.util.*

fun migrateRequestDetailsXmlJson(project: Project, rootFile: File, gson: Gson) {
    val jsonFile = File(ApiBankSettingsStateComponent.getInstance(project).state.requestFilePath)
    if (jsonFile.createNewFile()) {
        jsonFile.writeText("[]")
    } else {
        return
    }

    val xmlFileName = "api_detail_persistent.xml"

    if (!File(rootFile, xmlFileName).exists()) {
        return
    }

    val result = ArrayList<RequestDetail>()

    val xmlString = File(rootFile, xmlFileName).readText()
    val jsonObject = XML.toJSONObject(xmlString)
    val requestDetails = jsonObject.getJSONObject("project")
        .getJSONObject("component")
        .getJSONObject("option")
        .getJSONObject("list")
        .getJSONArray("RequestDetail")

    for (detail in requestDetails) {
        val options: JSONArray = (detail as JSONObject).getJSONArray("option") as JSONArray

        var requestId: String = UUID.randomUUID().toString()
        var requestBody: String? = null
        var requestUrl = ""
        var requestMethod = ""
        var requestName = ""
        val requestHeader = ArrayList<Array<String>>()

        for (i in 0..<options.length()) {
            val name = options.getJSONObject(i).getString("name")

            when (name) {
                "id" -> requestId = options.getJSONObject(i).getString("value")
                "name" -> requestName = options.getJSONObject(i).getString("value")
                "body" -> requestBody = options.getJSONObject(i).getString("value")
                "url" -> requestUrl = options.getJSONObject(i).getString("value")
                "method" -> requestMethod = options.getJSONObject(i).getString("value")
                "header" -> {
                    val headerOptions = (options.getJSONObject(i).getJSONObject("list").get("array"))
                    if (headerOptions is JSONArray) {
                        for (j in 0..<headerOptions.length()) {
                            val innerOptions = (headerOptions.get(j) as JSONObject).getJSONArray("option")
                            val first = (innerOptions.get(0) as JSONObject).getString("value")
                            val second = (innerOptions.get(1) as JSONObject).getString("value")
                            requestHeader.add(arrayOf(first, second))
                        }
                    } else if (headerOptions is JSONObject) {
                        val innerOptions = headerOptions.getJSONArray("option")
                        val first = (innerOptions.get(0) as JSONObject).getString("value")
                        val second = (innerOptions.get(1) as JSONObject).getString("value")
                        requestHeader.add(arrayOf(first, second))
                    }
                }
            }
        }

        val requestDetail = RequestDetail(
            id = requestId,
            name = requestName,
            url = requestUrl,
            method = requestMethod,
            header = requestHeader,
            body = requestBody
        )

        result.add(requestDetail)
    }

    val requestGroup = listOf(
        RequestGroup(
            groupName = "Requests Collection",
            requests = result
        )
    )

    File(rootFile, FILE_API_DETAIL_PERSISTENT).writeText(gson.toJson(requestGroup))
    File(rootFile, xmlFileName).renameTo(File(rootFile, "$xmlFileName.old"))
    println("Migration complete")
}

fun migrateEnvVarXmlJson(project: Project, rootFile: File, gson: Gson) {
    val jsonFile = File(ApiBankSettingsStateComponent.getInstance(project).state.envFilePath)
    if (jsonFile.createNewFile()) {
        jsonFile.writeText("[]")
    } else {
        return
    }

    val xmlFileName = "variable_collection_persistent.xml"

    if (!File(rootFile, xmlFileName).exists()) {
        return
    }

    val result = ArrayList<VariableCollection>()

    val xmlString = File(rootFile, xmlFileName).readText()
    val jsonObject = XML.toJSONObject(xmlString)
    val envVars = jsonObject.getJSONObject("project")
        .getJSONObject("component")
        .getJSONObject("option")
        .getJSONObject("list")
        .getJSONArray("VariableCollection")

    for (envVar in envVars) {
        val options: JSONArray = (envVar as JSONObject).getJSONArray("option") as JSONArray

        var id: String? = UUID.randomUUID().toString()
        var isActive = false
        val variableItems: ArrayList<Array<String>> = ArrayList()
        var name = ""

        for (i in 0..<options.length()) {
            val optionName = options.getJSONObject(i).getString("name")

            when (optionName) {
                "id" -> id = options.getJSONObject(i).getString("value")
                "name" -> name = options.getJSONObject(i).getString("value")
                "active" -> isActive = options.getJSONObject(i).getBoolean("value")
                "variableItems" -> {
                    val headerOptions = (options.getJSONObject(i).getJSONObject("list").get("array"))
                    if (headerOptions is JSONArray) {
                        for (j in 0..<headerOptions.length()) {
                            val innerOptions = (headerOptions.get(j) as JSONObject).getJSONArray("option")
                            val first = (innerOptions.get(0) as JSONObject).getString("value")
                            val second = (innerOptions.get(1) as JSONObject).getString("value")
                            variableItems.add(arrayOf(first, second))
                        }
                    } else if (headerOptions is JSONObject) {
                        val innerOptions = headerOptions.getJSONArray("option")
                        val first = (innerOptions.get(0) as JSONObject).getString("value")
                        val second = (innerOptions.get(1) as JSONObject).getString("value")
                        variableItems.add(arrayOf(first, second))
                    }
                }
            }
        }

        val variableCollection = VariableCollection(
            id = id,
            name = name,
            variableItems = variableItems,
            isActive = isActive
        )

        result.add(variableCollection)
    }

    File(rootFile, FILE_VARIABLE_COLLECTION_PERSISTENT).writeText(gson.toJson(result))
    File(rootFile, xmlFileName).renameTo(File(rootFile, "$xmlFileName.old"))
    println("EnvVar Migration complete")
}
