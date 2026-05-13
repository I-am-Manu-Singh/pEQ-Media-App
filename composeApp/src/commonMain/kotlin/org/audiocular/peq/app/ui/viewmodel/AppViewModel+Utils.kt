package org.audiocular.peq.app.ui.viewmodel

import kotlinx.coroutines.launch

fun AppViewModel.showToast(message: String) {

    viewModelScope.launch {

        snackbarFlow.emit(message)
    }
}