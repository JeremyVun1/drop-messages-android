package com.example.drop_messages_android.api

enum class DropRequest(val value: Int) {
    CREATE_DROP(0),
    CHANGE_LOC(1),
    GET_TOP(2),
    GET_NEW(3),
    GET_RANDOM(4),
    GET_RANGE(5),
    GET_MINE(6),
    UPVOTE(7),
    DOWNVOTE(8),
    DISCONNECT(9),
    DELETE(10),
    AUTHENTICATE(11),
    GET_SINGLE(12),
    GET_STUBS(13)
}

enum class DropResponse(val value: String) {
    SOCKET("socket"),
    POST("post"),
    RETRIEVE("retrieve"),
    VOTE("vote"),
    ERROR("error"),
    TOKEN("token"),
    NOTIFICATION("notify"),
    GEOLOC("geoloc"),
    SINGLE("single"),
    STUBS("stubs"),
    UNKNOWN("unknown");

    companion object {
        fun getEnum(x: String) : DropResponse {
            println(x)
            val result = values().firstOrNull { it.value == x}
            if (result == null)
                return UNKNOWN
            else return result
        }
    }
}