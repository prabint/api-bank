package api.bank.repository

import api.bank.models.RequestDetail
import api.bank.models.ResponseDetail

interface CoreRepository {
    suspend fun executeRequest(requestDetail: RequestDetail, variables: List<Array<String>>): ResponseDetail
}
