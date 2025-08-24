package com.projects.todos.data

import com.google.gson.annotations.SerializedName

data class DummyData(
    @SerializedName("tags")
    val tags: List<DummyTag>,
    
    @SerializedName("tasks")
    val tasks: List<DummyTask>
)

data class DummyTag(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("colorHex")
    val colorHex: String,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)

data class DummyTask(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("isCompleted")
    val isCompleted: Boolean,
    
    @SerializedName("isFavorite")
    val isFavorite: Boolean,
    
    @SerializedName("tagName")
    val tagName: String,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("updatedAt")
    val updatedAt: Long
)
