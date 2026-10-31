package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistConfig
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IptvViewModel(
    private val repository: IptvRepository
) : ViewModel() {

    val playlistConfig: StateFlow<PlaylistConfig> = repository.playlistConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaylistConfig()
        )

    val rawLiveChannels: StateFlow<List<ChannelEntity>> = repository.getLiveChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val movieChannels: StateFlow<List<ChannelEntity>> = repository.getMovieChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val seriesChannels: StateFlow<List<ChannelEntity>> = repository.getSeriesChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedLiveCategory = MutableStateFlow("Tümü")
    val selectedLiveCategory = _selectedLiveCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage = _loadingMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _currentPlayingChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentPlayingChannel = _currentPlayingChannel.asStateFlow()

    // Multi-View State
    private val _isMultiViewActive = MutableStateFlow(false)
    val isMultiViewActive = _isMultiViewActive.asStateFlow()

    private val _multiViewMode = MutableStateFlow(2) // 2 or 4 channels
    val multiViewMode = _multiViewMode.asStateFlow()

    private val _multiViewSlots = MutableStateFlow<List<ChannelEntity?>>(listOf(null, null, null, null))
    val multiViewSlots = _multiViewSlots.asStateFlow()

    private val _activeAudioSlotIndex = MutableStateFlow(0)
    val activeAudioSlotIndex = _activeAudioSlotIndex.asStateFlow()

    // Filtered Live channels based on Category and Search
    val filteredLiveChannels: StateFlow<List<ChannelEntity>> = combine(
        rawLiveChannels,
        _selectedLiveCategory,
        _searchQuery
    ) { channels, category, query ->
        channels.filter { channel ->
            val matchesCategory = when (category) {
                "Tümü" -> true
                "Favoriler" -> channel.isFavorite
                else -> channel.groupTitle.equals(category, ignoreCase = true)
            }
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                channel.name.contains(query, ignoreCase = true) ||
                        channel.groupTitle.contains(query, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectLiveCategory(category: String) {
        _selectedLiveCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun playChannel(channel: ChannelEntity) {
        _currentPlayingChannel.value = channel
    }

    fun stopPlayback() {
        _currentPlayingChannel.value = null
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite)
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.deleteChannel(channel.id)
        }
    }

    fun restoreDeletedChannels() {
        viewModelScope.launch {
            repository.restoreDeletedChannels()
        }
    }

    fun moveChannelUp(channel: ChannelEntity) {
        val list = rawLiveChannels.value.toMutableList()
        val index = list.indexOfFirst { it.id == channel.id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            viewModelScope.launch {
                repository.reorderChannels(list)
            }
        }
    }

    fun moveChannelDown(channel: ChannelEntity) {
        val list = rawLiveChannels.value.toMutableList()
        val index = list.indexOfFirst { it.id == channel.id }
        if (index != -1 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            viewModelScope.launch {
                repository.reorderChannels(list)
            }
        }
    }

    // URL Playlist loader
    fun loadFromUrl(url: String, expirationMillis: Long, name: String = "M3U Playlist") {
        if (url.isBlank()) {
            _errorMessage.value = "Lütfen geçerli bir URL girin."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Playlist indiriliyor ve kanallar ayrıştırılıyor..."
            _errorMessage.value = null

            val result = repository.loadFromUrl(url, expirationMillis, name)
            _isLoading.value = false
            result.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Playlist yüklenirken hata oluştu"
            }
        }
    }

    // File Playlist loader
    fun loadFromFile(uri: Uri, context: Context, expirationMillis: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Yerel dosya okunuyor ve kanallar aktarılıyor..."
            _errorMessage.value = null

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _isLoading.value = false
                    _errorMessage.value = "Seçilen dosya açılamadı."
                    return@launch
                }
                val fileName = uri.lastPathSegment ?: "m3u_file.m3u"
                val result = repository.loadFromFile(inputStream, fileName, expirationMillis)
                _isLoading.value = false
                result.onFailure {
                    _errorMessage.value = it.localizedMessage ?: "Dosya ayrıştırılamadı"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Dosya okunurken bir hata meydana geldi"
            }
        }
    }

    // Xtream Codes Login
    fun loginXtream(server: String, user: String, pass: String, expirationMillis: Long) {
        if (server.isBlank() || user.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Lütfen tüm Xtream Codes alanlarını doldurun."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Xtream Codes sunucusuna bağlanılıyor..."
            _errorMessage.value = null

            val result = repository.loginXtream(server, user, pass, expirationMillis)
            _isLoading.value = false
            result.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Xtream Codes bağlantısı başarısız oldu"
            }
        }
    }

    // Demo Streams Loader
    fun loadDemoStreams() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Yasal örnek test akışları hazırlanıyor..."
            _errorMessage.value = null
            repository.loadSampleDemoStreams()
            _isLoading.value = false
        }
    }

    // Multi-View actions
    fun openMultiView() {
        val live = rawLiveChannels.value
        val slots = mutableListOf<ChannelEntity?>()
        val count = _multiViewMode.value
        for (i in 0 until 4) {
            if (i < count && i < live.size) {
                slots.add(live[i])
            } else {
                slots.add(null)
            }
        }
        _multiViewSlots.value = slots
        _activeAudioSlotIndex.value = 0
        _isMultiViewActive.value = true
    }

    fun closeMultiView() {
        _isMultiViewActive.value = false
    }

    fun setMultiViewMode(count: Int) {
        _multiViewMode.value = count
        val live = rawLiveChannels.value
        val currentSlots = _multiViewSlots.value.toMutableList()
        while (currentSlots.size < 4) currentSlots.add(null)

        for (i in 0 until count) {
            if (currentSlots[i] == null && i < live.size) {
                currentSlots[i] = live[i]
            }
        }
        _multiViewSlots.value = currentSlots
        if (_activeAudioSlotIndex.value >= count) {
            _activeAudioSlotIndex.value = 0
        }
    }

    fun setActiveAudioSlot(index: Int) {
        _activeAudioSlotIndex.value = index
    }

    fun setMultiViewSlotChannel(slotIndex: Int, channel: ChannelEntity) {
        val current = _multiViewSlots.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = channel
            _multiViewSlots.value = current
        }
    }

    fun updateSubscriptionExpiration(newExpirationMillis: Long) {
        viewModelScope.launch {
            repository.playlistConfig // trigger save
            // update in repository
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentPlayingChannel.value = null
            _isMultiViewActive.value = false
            repository.clearAll()
        }
    }
}
