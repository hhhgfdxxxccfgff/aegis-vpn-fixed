package com.example.vpn.protocol

import android.net.ipsec.ike.ChildSaProposal
import android.net.ipsec.ike.IkeSaProposal
import android.net.ipsec.ike.SaProposal
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Parses strongSwan / IKEv2 proposal strings into Android SDK SaProposal objects.
 */
object Ikev2ProposalParser {

    /**
     * Parses IKE SA proposal string (e.g. "aes256-sha256-modp2048").
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun buildIkeSaProposal(proposalStr: String): IkeSaProposal {
        val builder = IkeSaProposal.Builder()
        val parts = proposalStr.lowercase().split("-")

        // Defaults if unspecified
        var encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_CBC
        var keyLen = SaProposal.KEY_LEN_AES_256
        var integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128
        var prfAlg = SaProposal.PSEUDORANDOM_FUNCTION_SHA2_256
        var dhGroup = SaProposal.DH_GROUP_2048_BIT_MODP

        for (part in parts) {
            when {
                part == "aes256" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_CBC
                    keyLen = SaProposal.KEY_LEN_AES_256
                }
                part == "aes128" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_CBC
                    keyLen = SaProposal.KEY_LEN_AES_128
                }
                part == "aes256gcm16" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16
                    keyLen = SaProposal.KEY_LEN_AES_256
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_NONE
                }
                part == "aes128gcm16" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16
                    keyLen = SaProposal.KEY_LEN_AES_128
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_NONE
                }
                part == "sha256" -> {
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128
                    prfAlg = SaProposal.PSEUDORANDOM_FUNCTION_SHA2_256
                }
                part == "sha384" -> {
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192
                    prfAlg = SaProposal.PSEUDORANDOM_FUNCTION_SHA2_384
                }
                part == "sha512" -> {
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256
                    prfAlg = SaProposal.PSEUDORANDOM_FUNCTION_SHA2_512
                }
                part == "sha1" -> {
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96
                    prfAlg = SaProposal.PSEUDORANDOM_FUNCTION_HMAC_SHA1
                }
                part == "modp2048" || part == "dh14" -> dhGroup = SaProposal.DH_GROUP_2048_BIT_MODP
                part == "modp1536" || part == "dh5" -> dhGroup = SaProposal.DH_GROUP_1536_BIT_MODP
                part == "modp3072" || part == "dh15" -> dhGroup = SaProposal.DH_GROUP_3072_BIT_MODP
                part == "modp4096" || part == "dh16" -> dhGroup = SaProposal.DH_GROUP_4096_BIT_MODP
                part == "curve25519" || part == "ecp256" -> dhGroup = SaProposal.DH_GROUP_CURVE_25519
            }
        }

        builder.addEncryptionAlgorithm(encAlg, keyLen)
        if (integAlg != SaProposal.INTEGRITY_ALGORITHM_NONE) {
            builder.addIntegrityAlgorithm(integAlg)
        }
        builder.addPseudorandomFunction(prfAlg)
        builder.addDhGroup(dhGroup)

        return builder.build()
    }

    /**
     * Parses Child SA (ESP) proposal string (e.g. "aes128gcm16").
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun buildChildSaProposal(proposalStr: String): ChildSaProposal {
        val builder = ChildSaProposal.Builder()
        val parts = proposalStr.lowercase().split("-")

        var encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16
        var keyLen = SaProposal.KEY_LEN_AES_128
        var integAlg = SaProposal.INTEGRITY_ALGORITHM_NONE

        for (part in parts) {
            when {
                part == "aes128gcm16" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16
                    keyLen = SaProposal.KEY_LEN_AES_128
                }
                part == "aes256gcm16" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16
                    keyLen = SaProposal.KEY_LEN_AES_256
                }
                part == "aes256" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_CBC
                    keyLen = SaProposal.KEY_LEN_AES_256
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128
                }
                part == "aes128" -> {
                    encAlg = SaProposal.ENCRYPTION_ALGORITHM_AES_CBC
                    keyLen = SaProposal.KEY_LEN_AES_128
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128
                }
                part == "sha256" -> {
                    integAlg = SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128
                }
            }
        }

        builder.addEncryptionAlgorithm(encAlg, keyLen)
        if (integAlg != SaProposal.INTEGRITY_ALGORITHM_NONE) {
            builder.addIntegrityAlgorithm(integAlg)
        }
        return builder.build()
    }

    fun describeIkeProposal(proposalStr: String): String {
        return when (proposalStr.lowercase()) {
            "aes256-sha256-modp2048" -> "AES-256-CBC / HMAC-SHA256 / MODP2048 (Group 14)"
            "aes128-sha256-modp2048" -> "AES-128-CBC / HMAC-SHA256 / MODP2048 (Group 14)"
            "aes256gcm16-sha256-modp2048" -> "AES-256-GCM-16 / PRF-SHA256 / MODP2048"
            else -> proposalStr.uppercase()
        }
    }

    fun describeEspProposal(proposalStr: String): String {
        return when (proposalStr.lowercase()) {
            "aes128gcm16" -> "AES-128-GCM-16 (AEAD, 128-bit key, 16-byte ICV)"
            "aes256gcm16" -> "AES-256-GCM-16 (AEAD, 256-bit key, 16-byte ICV)"
            "aes256-sha256" -> "AES-256-CBC with HMAC-SHA-256-128"
            else -> proposalStr.uppercase()
        }
    }
}
