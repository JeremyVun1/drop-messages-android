package com.example.drop_messages_android.network

// All the network models for messaging with our network api
// TODO - simplify the API

// user authentication
data class SignUpModel(val username: String, val password: String, val email: String)
data class GetTokenModel(val username: String, val password: String)
data class TokenResponseModel(val token: String)

// web socket connection and response
data class WebSocketConnectModel(val token: String)
data class WebSocketResponseModel(val category: String, val data: String)

// web socket requests
data class ChangeGeolocationModel(val category: Int, val lat: Float, val long: Float)
data class CreateDropModel(val category: Int, val data: String)
data class RequestDropsModel(val category: Int, val page: Int)
data class RequestDropsRangeModel(val category: Int, val data: String, val page: Int)
data class UpvoteModel(val category: Int, val data: String)