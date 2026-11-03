package com.mika.data.repository

import com.mika.domain.data.model.update.UpdateData
import com.mika.domain.repository.UpdateRepository
import com.mika.domain.utils.Resource
import com.mika.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class UpdateRepositoryImpl(
    private val youTube: YouTube,
) : UpdateRepository {
    override fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>> =
        flow {
            youTube
                .checkForGithubReleaseUpdate()
                .onSuccess { response ->
                    emit(
                        Resource.Success(
                            UpdateData(
                                tagName = response.tagName ?: "",
                                releaseTime = response.publishedAt ?: "",
                                body = response.body ?: "",
                            ),
                        ),
                    )
                }.onFailure {
                    emit(Resource.Error<UpdateData>(it.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.Default)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            youTube
                .checkForFdroidUpdate()
                .onSuccess { response ->
                    val latestVersion = response.packages.maxBy { it.versionCode }
                    emit(
                        Resource.Success(
                            UpdateData(
                                tagName = latestVersion.versionName,
                                releaseTime = null,
                                body =
                                    $$"""
                                    ### Update via F-Droid, changelogs: 
                                    - https://github.com/Mika/D-Spot/blob/dev/fastlane/metadata/android/en-US/changelogs/$${latestVersion.versionCode}.txt
                                    """.trimIndent(),
                            ),
                        ),
                    )
                }.onFailure {
                    emit(Resource.Error<UpdateData>(it.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.Default)

    override fun getFdroidSigningKeys(): Flow<Resource<List<String>>> =
        flow {
            youTube
                .getFdroidSigningKeys()
                .onSuccess { emit(Resource.Success(it)) }
                .onFailure { emit(Resource.Error<List<String>>(it.message ?: "Unknown error")) }
        }.flowOn(Dispatchers.Default)
}