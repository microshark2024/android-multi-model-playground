package com.example.multimodelplayground

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Chat : NavKey
@Serializable data object ImageGen : NavKey
@Serializable data object VideoGen : NavKey
@Serializable data object Config : NavKey
