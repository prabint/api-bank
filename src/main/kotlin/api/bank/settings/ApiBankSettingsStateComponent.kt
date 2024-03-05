package api.bank.settings

import api.bank.models.Constants
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
@State(
    name = "ApiBankSettingsState",
    storages = [Storage(Constants.FILE_SETTINGS)]
)
class ApiBankSettingsStateComponent(project: Project) : PersistentStateComponent<ApiBankSettingsState> {
    private var settingsState = ApiBankSettingsState(
        requestFilePath = Paths
            .get(project.basePath!!, ".idea", Constants.FILE_API_DETAIL_PERSISTENT)
            .toString(),
        envFilePath = Paths
            .get(project.basePath!!, ".idea", Constants.FILE_VARIABLE_COLLECTION_PERSISTENT)
            .toString()
    )

    override fun getState() = settingsState

    override fun loadState(state: ApiBankSettingsState) {
        settingsState = state
    }

    companion object {
        fun getInstance(project: Project): ApiBankSettingsStateComponent = project.service()
    }
}
