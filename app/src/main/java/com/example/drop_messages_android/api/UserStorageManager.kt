package com.example.drop_messages_android.api

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object UserStorageManager {
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val PREFERENCES_NAME = "Login"

    private var keyStore : KeyStore? = null

    init {
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore!!.load(null)
    }

    private suspend fun createSecretKey(alias: String) : SecretKey? {
        try {
            val keyGen =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

            keyGen.init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            )

            return keyGen.generateKey()
        } catch (ex: Exception) {
            Log.e("ENCRYPT", Log.getStackTraceString(ex))
            return null
        }
    }

    private suspend fun getSecretKey(alias: String) : SecretKey? {
        val secretKeyEntry = keyStore!!.getEntry(alias, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    @SuppressLint("ApplySharedPref")
    suspend fun encrypt(alias: String, text: String, context: Context) : String? {
        try {
            // check if we have keys already to initialise the cipher
            val cipher = Cipher.getInstance(TRANSFORMATION)
            if (!keyStore!!.containsAlias(alias))
                cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(alias))
            else
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias))

            //store our IV in android preferences
            val iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
            val sp = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            sp.edit().putString("${alias}_iv", iv).commit()

            val encryptedBytes= cipher.doFinal(text.toByteArray(Charsets.UTF_8))

            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (ex: Exception) {
            Log.e("ENCRYPT", Log.getStackTraceString(ex))
            return null
        }
    }

    suspend fun decrypt(alias: String, text:String, context: Context) : String? {
        try {
            val sp = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val ivStr = sp.getString("${alias}_iv", null)
            if (ivStr == null) {
                println("iv could not be fetched!")
                return null
            }

            val ivBytes = Base64.decode(ivStr, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), spec)

            val decryptedBytes = cipher.doFinal(Base64.decode(text, Base64.NO_WRAP))
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (ex: Exception) {
            Log.e("ENCRYPT", Log.getStackTraceString(ex))
            return null
        }
    }
}