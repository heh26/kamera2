package com.example.ipcamrecorder.ptz

import android.util.Base64
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

class OnvifClient(val endpoint: String, val username: String?, val password: String?) {
    private val client = OkHttpClient()

    private fun sha1(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(data)
    }

    private fun wsSecurityHeader(): String {
        if (username == null || password == null) return ""
        val nonce = ByteArray(16)
        Random.nextBytes(nonce)
        val created = javax.xml.bind.DatatypeConverter.printDateTime(Calendar.getInstance())
        val digest = sha1(nonce + created.toByteArray() + password.toByteArray())
        val b64Digest = Base64.encodeToString(digest, Base64.NO_WRAP)
        val b64Nonce = Base64.encodeToString(nonce, Base64.NO_WRAP)
        return """<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">""" +
                """<wsse:UsernameToken><wsse:Username>$username</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">$b64Digest</wsse:Password><wsse:Nonce>$b64Nonce</wsse:Nonce><wsu:Created xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">$created</wsu:Created></wsse:UsernameToken></wsse:Security>"""
    }

    fun sendSoap(soapBody: String): String? {
        val header = wsSecurityHeader()
        val full = """<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"><s:Header>$header</s:Header><s:Body>$soapBody</s:Body></s:Envelope>"""
        val mediaType = MediaType.parse("application/soap+xml; charset=utf-8")
        val body = RequestBody.create(mediaType, full)
        val req = Request.Builder().url(endpoint).post(body).build()
        val resp = client.newCall(req).execute()
        return resp.body()?.string()
    }

    fun continuousMove(x: Double, y: Double, zoom: Double) {
        val soap = """<tptz:ContinuousMove xmlns:tptz=\"http://www.onvif.org/ver10/PTZ/wsdl\"><tptz:Velocity><tptz:PanTilt x=\"$x\" y=\"$y\"/><tptz:Zoom x=\"$zoom\"/></tptz:Velocity></tptz:ContinuousMove>"""
        sendSoap(soap)
    }

    fun absoluteMove(pan: Double, tilt: Double, zoom: Double) {
        val soap = """<tptz:AbsoluteMove xmlns:tptz=\"http://www.onvif.org/ver10/PTZ/wsdl\"><tptz:Position><tptz:PanTilt x=\"$pan\" y=\"$tilt\"/><tptz:Zoom x=\"$zoom\"/></tptz:Position></tptz:AbsoluteMove>"""
        sendSoap(soap)
    }

    fun stop() {
        val soap = """<tptz:Stop xmlns:tptz=\"http://www.onvif.org/ver10/PTZ/wsdl\"/>"""
        sendSoap(soap)
    }
}
