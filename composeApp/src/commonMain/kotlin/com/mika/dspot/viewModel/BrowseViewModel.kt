package com.mika.dspot.viewModel

import androidx.lifecycle.viewModelScope
import com.mika.domain.data.model.home.BrowsePage
import com.mika.domain.repository.HomeRepository
import com.mika.domain.utils.Resource
import com.mika.dspot.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.error

class BrowseViewModel(
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<BrowseUIState>(BrowseUIState.Loading)
    val uiState: StateFlow<BrowseUIState> get() = _uiState

    /** Cover art per mood/genre category params, filled in lazily as tiles enter the viewport. */
    private val _moodArtwork: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    val moodArtwork: StateFlow<Map<String, String>> get() = _moodArtwork.asStateFlow()
    private val requestedMoodArtwork = mutableSetOf<String>()

    fun getBrowsePage(
        browseId: String,
        params: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = BrowseUIState.Loading
            homeRepository.getBrowsePage(browseId, params).collect { resource ->
                val page = resource.data
                _uiState.value =
                    if (resource is Resource.Success && page != null && (page.contents.isNotEmpty() || page.moods.isNotEmpty())) {
                        BrowseUIState.Success(page)
                    } else {
                        BrowseUIState.Error(resource.message ?: getString(Res.string.error))
                    }
            }
        }
    }

    /**
     * Resolve the cover for one mood/genre category. Only one request per [params] is ever made —
     * [requestedMoodArtwork] dedupes re-entries as a tile scrolls in and out of view.
     */
    fun loadMoodArtwork(params: String) {
        if (!requestedMoodArtwork.add(params)) return
        viewModelScope.launch {
            homeRepository.getMoodCategoryArtwork(params).collect { url ->
                if (url != null) {
                    _moodArtwork.update { it + (params to url) }
                }
            }
        }
    }
}

sealed class BrowseUIState {
    data class Success(
        val page: BrowsePage,
    ) : BrowseUIState()

    data class Error(
        val message: String,
    ) : BrowseUIState()

    object Loading : BrowseUIState()
}
