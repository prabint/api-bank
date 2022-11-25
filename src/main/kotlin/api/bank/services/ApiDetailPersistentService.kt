package api.bank.services

import api.bank.models.Constants.FILE_API_DETAIL_PERSISTENT
import api.bank.models.RequestDetailList
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service
@State(
    name = "api_detail_persistent",
    storages = [Storage(FILE_API_DETAIL_PERSISTENT)]
)
class ApiDetailPersistentService : PersistentStateComponent<RequestDetailList> {

    var requestDetailListState: RequestDetailList = RequestDetailList(ArrayList())

    override fun getState(): RequestDetailList {
        return requestDetailListState
    }

    override fun loadState(requestDetailList: RequestDetailList) {
        this.requestDetailListState = requestDetailList
    }

    companion object {
        fun getInstance(project: Project): ApiDetailPersistentService = project.service()
    }
}
