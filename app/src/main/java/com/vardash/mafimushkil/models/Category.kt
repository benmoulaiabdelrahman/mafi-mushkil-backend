package com.vardash.mafimushkil.models

import com.google.firebase.firestore.PropertyName

data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true
)
