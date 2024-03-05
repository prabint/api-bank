package api.bank.utils

import api.bank.models.RequestDetail
import api.bank.models.RequestGroup
import api.bank.models.VariableCollection
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import java.io.File

fun getVariableCollectionFromJson(gson: Gson, project: Project): ArrayList<VariableCollection> {
    return gson.fromJson(
        File(ApiBankSettingsPersistentStateComponent.getInstance(project).state.envFilePath).readText(),
        object : TypeToken<ArrayList<VariableCollection>>() {}.type
    )
}

fun saveVariableCollectionAsJsonFile(gson: Gson, project: Project, value: List<VariableCollection>) {
    File(ApiBankSettingsPersistentStateComponent.getInstance(project).state.envFilePath).writeText(gson.toJson(value))
}

fun getRequestDetailsFromJson(gson: Gson, project: Project): List<RequestDetail> {
    return gson
        .fromJson<List<RequestGroup>>(
            File(ApiBankSettingsPersistentStateComponent.getInstance(project).state.requestFilePath).readText(),
            object : TypeToken<List<RequestGroup>>() {}.type
        ).flatMap { it.requests }
}