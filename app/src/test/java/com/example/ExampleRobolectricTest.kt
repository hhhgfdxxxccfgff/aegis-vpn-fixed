package com.example

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.vpn.data.SampleProfiles
import com.example.vpn.model.SswanConfig
import com.example.vpn.protocol.Ikev2ProposalParser
import com.example.vpn.security.CertificateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Aegis VPN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aegis VPN", appName)
    }

    @Test
    fun `sswan configuration parser and serializer roundtrip`() {
        val originalJson = """
        {
          "uuid": "test-uuid-1234",
          "name": "Corporate IKEv2",
          "type": "ikev2-cert",
          "remote": {
            "addr": "198.51.100.1",
            "port": 500
          },
          "local": {
            "p12": "${SampleProfiles.SAMPLE_P12_BASE64}",
            "id": "vpn-client@aegis.net"
          },
          "rsa-pss": "true",
          "ike-proposal": "aes256-sha256-modp2048",
          "esp-proposal": "aes128gcm16"
        }
        """.trimIndent()

        val config = SswanConfig.fromJson(originalJson)
        assertEquals("test-uuid-1234", config.uuid)
        assertEquals("Corporate IKEv2", config.name)
        assertEquals("198.51.100.1", config.remoteAddr)
        assertEquals(500, config.remotePort)
        assertTrue(config.rsaPss)
        assertEquals("aes256-sha256-modp2048", config.ikeProposal)
        assertEquals("aes128gcm16", config.espProposal)

        val generatedJson = config.toJson()
        val reloaded = SswanConfig.fromJson(generatedJson)
        assertEquals(config.uuid, reloaded.uuid)
        assertEquals(config.name, reloaded.name)
        assertEquals(config.remoteAddr, reloaded.remoteAddr)
    }

    @Test
    fun `pkcs12 certificate parsing and password authentication`() {
        val p12Bytes = Base64.decode(SampleProfiles.SAMPLE_P12_BASE64, Base64.DEFAULT)

        // Valid password test
        assertTrue(CertificateManager.testPassword(p12Bytes, "aegis123".toCharArray()))

        // Wrong password test
        assertFalse(CertificateManager.testPassword(p12Bytes, "wrong_password".toCharArray()))

        // Parse certificate with correct password
        val parsed = CertificateManager.parsePkcs12(p12Bytes, "aegis123".toCharArray())
        assertNotNull(parsed.userCert)
        assertNotNull(parsed.privateKey)
        assertTrue(parsed.subjectDn.contains("vpn-client.aegis.net"))
        assertTrue(parsed.keyAlgorithm.contains("RSA"))
        assertNotNull(parsed.fingerprintSha256)
    }

    @Test
    fun `proposal descriptions match RFC specifications`() {
        val ikeDesc = Ikev2ProposalParser.describeIkeProposal("aes256-sha256-modp2048")
        assertTrue(ikeDesc.contains("AES-256-CBC"))
        assertTrue(ikeDesc.contains("MODP2048"))

        val espDesc = Ikev2ProposalParser.describeEspProposal("aes128gcm16")
        assertTrue(espDesc.contains("AES-128-GCM-16"))
    }

    @Test
    fun `testUserSswanFile parses and requires no password`() {
        val file = java.io.File("public/assets/aistudio/android_62b2cb11.txt").let {
            if (it.exists()) it else java.io.File("../public/assets/aistudio/android_62b2cb11.txt")
        }
        val fileContent = file.readText()
        val config = SswanConfig.fromJson(fileContent)
        val p12Bytes = Base64.decode(config.p12Base64, Base64.DEFAULT)
        assertFalse(CertificateManager.requiresPassword(p12Bytes))

        val parsed = CertificateManager.parsePkcs12(p12Bytes, CharArray(0))
        assertTrue(parsed.subjectDn.contains("android"))
        assertEquals("3.142.134.245", config.remoteAddr)
        assertTrue(config.rsaPss)
    }
}
