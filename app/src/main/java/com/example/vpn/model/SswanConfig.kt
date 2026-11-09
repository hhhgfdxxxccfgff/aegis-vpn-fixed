package com.example.vpn.model

import org.json.JSONObject

/**
 * StrongSwan .sswan configuration parser and serializer.
 */
data class SswanConfig(
    val uuid: String,
    val name: String,
    val type: String,
    val remoteAddr: String,
    val remotePort: Int,
    val p12Base64: String,
    val localId: String,
    val rsaPss: Boolean,
    val ikeProposal: String,
    val espProposal: String
) {
    companion object {
        fun fromJson(jsonStr: String): SswanConfig {
            val root = JSONObject(jsonStr)
            val uuid = root.optString("uuid", java.util.UUID.randomUUID().toString())
            val name = root.optString("name", "IKEv2 VPN")
            val type = root.optString("type", "ikev2-cert")

            val remoteObj = root.optJSONObject("remote")
            val remoteAddr = remoteObj?.optString("addr", "") ?: root.optString("remote_addr", "")
            val remotePort = remoteObj?.optInt("port", 500) ?: 500

            val localObj = root.optJSONObject("local")
            val rawP12 = localObj?.optString("p12", "") ?: root.optString("p12", "")
            val p12Base64 = rawP12.replace("\n", "").replace("\r", "").replace(" ", "").trim()
            val localId = localObj?.optString("id", "") ?: ""

            val rsaPssVal = localObj?.opt("rsa-pss") ?: root.opt("rsa-pss")
            val rsaPss = when (rsaPssVal) {
                is Boolean -> rsaPssVal
                is String -> rsaPssVal.equals("true", ignoreCase = true)
                else -> true
            }

            val ikeProposal = root.optString("ike-proposal", "aes256-sha256-modp2048")
            val espProposal = root.optString("esp-proposal", "aes128gcm16")

            if (remoteAddr.isBlank()) {
                throw IllegalArgumentException("Remote server address ('remote.addr') is missing from configuration.")
            }

            return SswanConfig(
                uuid = uuid,
                name = name,
                type = type,
                remoteAddr = remoteAddr,
                remotePort = remotePort,
                p12Base64 = p12Base64,
                localId = localId,
                rsaPss = rsaPss,
                ikeProposal = ikeProposal,
                espProposal = espProposal
            )
        }
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("uuid", uuid)
        root.put("name", name)
        root.put("type", type)

        val remote = JSONObject()
        remote.put("addr", remoteAddr)
        if (remotePort != 500) {
            remote.put("port", remotePort)
        }
        root.put("remote", remote)

        val local = JSONObject()
        local.put("p12", p12Base64)
        if (localId.isNotBlank()) {
            local.put("id", localId)
        }
        root.put("local", local)

        root.put("rsa-pss", if (rsaPss) "true" else "false")
        root.put("ike-proposal", ikeProposal)
        root.put("esp-proposal", espProposal)

        return root.toString(2)
    }
}
