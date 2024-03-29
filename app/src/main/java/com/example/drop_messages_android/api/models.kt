package com.example.drop_messages_android.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

// All the network models for messaging with our network api
// TODO - simplify all of this

// user authentication
data class SignUpModel(val username: String, val password: String, val email: String)
data class SignInModel(val username: String?, val password: String?, val token: String?)
data class GetTokenModel(val username: String, val password: String)
data class TokenResponseModel(val token: String)
data class InvalidLoginResponseModel(val non_field_errors: Array<String>)

// client models
@Parcelize
data class UserModel(val username: String?, val password: String?, var token: String?, var location: Geolocation?) : Parcelable

@Parcelize
data class Geolocation(var lat: Double, var long: Double) : Parcelable {
    fun formattedString(dp: Int = 2): String {
        return "${"%.${dp}f".format(lat)},${"%.${dp}f".format(lat)}"
    }

    fun isValid(): Boolean {
        return (lat >= -90 && lat <= 90 && long >= -180 && long <= 180)
    }
}

@Parcelize
data class DropMessage(val id: Int, val lat: Float, val long: Float, val message: String, val date: String, val votes: Int, val seen: Int, val author: String) : Parcelable

data class PostDataResponse(val echo: DropMessage, val result: Boolean, val meta: String)

data class GeolocationResponse(val result: Boolean, val lat: Float, val long: Float)

data class DropMessageStub(val id: String, val author: String, val lat: Float, val long: Float)

// web socket
// send models
data class AuthenticateSocket(val category: Int = DropRequest.AUTHENTICATE.value, val token: String, val lat: Float, val long: Float)
data class ChangeGeolocation(val category: Int = DropRequest.CHANGE_LOC.value, val lat: Float, val long: Float)
data class CreateDrop(val category: Int = DropRequest.CREATE_DROP.value, val data: String)
data class RequestDrops(val category: Int, val page: Int)
data class RequestDropsRange(val category: Int = DropRequest.GET_RANGE.value, val data: String, val page: Int)
data class Upvote(val category: Int = DropRequest.UPVOTE.value, val data: String)
data class Downvote(val category: Int = DropRequest.DOWNVOTE.value, val data: String)
data class CloseSocket(val category: Int = DropRequest.DISCONNECT.value)
data class Delete(val category: Int = DropRequest.DELETE.value, val data: String)
data class RequestStubs(val category: Int = DropRequest.GET_STUBS.value)
data class RequestSingle(val category: Int = DropRequest.GET_SINGLE.value, val data: String)

// response models
data class SocketResponse(val category: String, val data: String)