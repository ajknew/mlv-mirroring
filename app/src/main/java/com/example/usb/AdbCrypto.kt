package com.example.usb

import android.content.Context
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey

class AdbCrypto private constructor(val keyPair: KeyPair) {

    val privateKey: PrivateKey get() = keyPair.private
    val publicKey: RSAPublicKey get() = keyPair.public as RSAPublicKey

    fun signToken(token: ByteArray): ByteArray {
        // Standard Android ADB signs with SHA1withRSA PKCS#1 v1.5
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(privateKey)
        signature.update(token)
        return signature.sign()
    }

    fun getPublicKeyAdbBytes(userHost: String = "host@mlv-mirroring"): ByteArray {
        val pubEncoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        val fullString = "$pubEncoded $userHost\u0000"
        return fullString.toByteArray(Charsets.UTF_8)
    }

    companion object {
        @Volatile
        private var instance: AdbCrypto? = null

        fun getInstance(context: Context): AdbCrypto {
            return instance ?: synchronized(this) {
                instance ?: loadOrGenerate(context).also { instance = it }
            }
        }

        private fun loadOrGenerate(context: Context): AdbCrypto {
            return try {
                val keyGen = KeyPairGenerator.getInstance("RSA")
                keyGen.initialize(2048)
                val keyPair = keyGen.generateKeyPair()
                AdbCrypto(keyPair)
            } catch (e: Exception) {
                val keyGen = KeyPairGenerator.getInstance("RSA")
                keyGen.initialize(2048)
                AdbCrypto(keyGen.generateKeyPair())
            }
        }
    }
}
