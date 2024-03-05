package api.bank.services

import api.bank.models.VariableCollection
import api.bank.settings.ApiBankSettingsStateComponent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import java.io.File

fun getEnvFromJson(gson: Gson, project: Project): ArrayList<VariableCollection> {
    return gson.fromJson(
        File(ApiBankSettingsStateComponent.getInstance(project).state.envFilePath).readText(),
        object : TypeToken<ArrayList<VariableCollection>>() {}.type
    )
}

fun saveEnvToJsonFile(gson: Gson, project: Project, value: List<VariableCollection>) {
    File(ApiBankSettingsStateComponent.getInstance(project).state.envFilePath).writeText(gson.toJson(value))
}