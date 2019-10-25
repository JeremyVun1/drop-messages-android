package com.example.drop_messages_android.api

// All the network models for messaging with our network api
// TODO - simplify all of this

// user authentication
data class SignUpModel(val username: String, val password: String, val email: String)
data class SignInModel(val username: String?, val password: String?, val token: String?)
data class GetTokenModel(val username: String, val password: String)
data class TokenResponseModel(val token: String)
data class InvalidLoginResponseModel(val non_field_errors: Array<String>)

// web socket
// send models
data class AuthenticateSocket(val token: String)
data class ChangeGeolocation(val category: Int, val lat: Float, val long: Float)
data class CreateDrop(val category: Int, val data: String)
data class RequestDrops(val category: Int, val page: Int)
data class RequestDropsRange(val category: Int, val data: String, val page: Int)
data class Upvote(val category: Int, val data: String)
data class Downvote(val category: Int, val data: String)

// response models
data class SocketResponse(val category: String, val data: String)