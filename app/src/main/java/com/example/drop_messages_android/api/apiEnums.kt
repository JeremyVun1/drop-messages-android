package com.example.drop_messages_android.api

enum class RequestCategory(val category: Int) {
    CREATE_DROP(0),
    CHANGE_LOC(1),
    GET_TOP(2),
    GET_NEW(3),
    GET_RANDOM(4),
    GET_RANGE(5),
    GET_MINE(6),
    UPVOTE(7),
    DOWNVOTE(8)
}

enum class ResponseCategory(val category: String) {
    SOCKET("socket"),
    POST("post"),
    RETRIEVE("retrieve"),
    ERROR("error"),
    NOTIFICATION("notification")
}