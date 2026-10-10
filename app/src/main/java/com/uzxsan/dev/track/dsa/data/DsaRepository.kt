package com.uzxsan.dev.track.dsa.data

import kotlinx.coroutines.flow.Flow

class DsaRepository(private val dao: DsaDao) {
    fun observeAll(): Flow<List<DsaProblem>> = dao.observeAll()

    suspend fun save(problem: DsaProblem) {
        dao.upsert(problem)
    }

    suspend fun delete(problem: DsaProblem) = dao.delete(problem)
}
