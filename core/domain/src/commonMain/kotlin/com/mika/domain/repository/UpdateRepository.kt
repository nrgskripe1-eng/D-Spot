package com.mika.domain.repository

import com.mika.domain.data.model.update.UpdateData
import com.mika.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>>
    fun checkForFdroidUpdate(): Flow<Resource<UpdateData>>

    fun getFdroidSigningKeys(): Flow<Resource<List<String>>>
}