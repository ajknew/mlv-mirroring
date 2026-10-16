package com.example.usb

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

class AdbCrypto private constructor(val keyPair: KeyPair) {

    val privateKey: PrivateKey get() = keyPair.private
    val publicKey: RSAPublicKey get() = keyPair.public as RSAPublicKey

    /**
     * Signs the 20-byte token sent by adbd.
     * adbd calls RSA_verify(NID_sha1, token, 20, signature, 256, rsa).
     * This requires PKCS#1 v1.5 padding with SHA-1 ASN.1 DigestInfo prefix.
     */
    fun signToken(token: ByteArray): ByteArray {
        return try {
            val digestInfo = ByteArray(SHA1_DIGEST_INFO_PREFIX.size + token.size)
            System.arraycopy(SHA1_DIGEST_INFO_PREFIX, 0, digestInfo, 0, SHA1_DIGEST_INFO_PREFIX.size)
            System.arraycopy(token, 0, digestInfo, SHA1_DIGEST_INFO_PREFIX.size, token.size)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, privateKey)
            cipher.doFinal(digestInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing ADB token: ${e.message}", e)
            ByteArray(256)
        }
    }

    /**
     * Converts RSAPublicKey into the binary 524-byte RSAPublicKey struct expected by Android adbd:
     * struct RSAPublicKey {
     *     uint32_t modulus_size_words; // 64
     *     uint32_t n0inv;              // -1 / N[0] mod 2^32
     *     uint8_t modulus[256];        // Little-endian
     *     uint8_t rr[256];             // (2^4096) mod N in Little-endian
     *     uint32_t exponent;           // 65537
     * };
     */
    fun getPublicKeyAdbBytes(userHost: String = "host@mlv-mirroring"): ByteArray {
        val n = publicKey.modulus
        val e = publicKey.publicExponent

        val buffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)

        // 1. modulus_size_words (64 words = 2048 bits / 32 bits)
        buffer.putInt(64)

        // 2. n0inv = (2^32 - (n mod 2^32)^(-1) mod 2^32) mod 2^32
        val r32 = BigInteger.valueOf(2).pow(32)
        val n0 = n.remainder(r32)
        val n0invBig = try {
            r32.subtract(n0.modInverse(r32)).remainder(r32)
        } catch (ex: Exception) {
            BigInteger.ONE
        }
        buffer.putInt(n0invBig.toInt())

        // 3. modulus in little-endian (256 bytes)
        val nLittleEndian = bigIntToLittleEndianBytes(n, 256)
        buffer.put(nLittleEndian)

        // 4. rr = (2^4096) mod N in little-endian (256 bytes)
        val r4096 = BigInteger.valueOf(2).pow(4096)
        val rr = r4096.remainder(n)
        val rrLittleEndian = bigIntToLittleEndianBytes(rr, 256)
        buffer.put(rrLittleEndian)

        // 5. exponent (usually 65537)
        buffer.putInt(e.toInt())

        val structBytes = buffer.array()
        val base64Key = Base64.encodeToString(structBytes, Base64.NO_WRAP)
        val fullPayload = "$base64Key $userHost\u0000"
        return fullPayload.toByteArray(Charsets.UTF_8)
    }

    private fun bigIntToLittleEndianBytes(bigInt: BigInteger, targetLength: Int): ByteArray {
        var rawBytes = bigInt.toByteArray()
        if (rawBytes.isNotEmpty() && rawBytes[0] == 0.toByte()) {
            rawBytes = rawBytes.copyOfRange(1, rawBytes.size)
        }
        val paddedBigEndian = ByteArray(targetLength)
        val offset = targetLength - rawBytes.size
        if (offset >= 0) {
            System.arraycopy(rawBytes, 0, paddedBigEndian, offset, rawBytes.size)
        } else {
            System.arraycopy(rawBytes, -offset, paddedBigEndian, 0, targetLength)
        }

        // Reverse to Little-Endian
        val littleEndian = ByteArray(targetLength)
        for (i in 0 until targetLength) {
            littleEndian[i] = paddedBigEndian[targetLength - 1 - i]
        }
        return littleEndian
    }

    companion object {
        private const val TAG = "AdbCrypto"
        private const val KEY_FILE_NAME = "mlv_adb_rsa.key"

        // ASN.1 DER prefix for SHA-1 DigestInfo:
        // SEQUENCE { SEQUENCE { OID 1.3.14.3.2.26, NULL }, OCTET STRING (20 bytes) }
        val SHA1_DIGEST_INFO_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
            0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
        )

        @Volatile
        private var instance: AdbCrypto? = null

        fun getInstance(context: Context): AdbCrypto {
            return instance ?: synchronized(this) {
                instance ?: loadOrGenerate(context).also { instance = it }
            }
        }

        private fun loadOrGenerate(context: Context): AdbCrypto {
            val keyFile = File(context.filesDir, KEY_FILE_NAME)
            if (keyFile.exists()) {
                try {
                    val keyBytes = keyFile.readBytes()
                    val keySpec = PKCS8EncodedKeySpec(keyBytes)
                    val kf = KeyFactory.getInstance("RSA")
                    val privKey = kf.generatePrivate(keySpec) as RSAPrivateCrtKey
                    val pubSpec = RSAPublicKeySpec(privKey.modulus, privKey.publicExponent)
                    val pubKey = kf.generatePublic(pubSpec)
                    return AdbCrypto(KeyPair(pubKey, privKey))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load saved RSA key, regenerating: ${e.message}")
                }
            }

            // Generate new 2048-bit RSA KeyPair
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()

            try {
                keyFile.writeBytes(pair.private.encoded)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist RSA private key: ${e.message}")
            }

            return AdbCrypto(pair)
        }
    }
}
