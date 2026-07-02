package com.virtualwife.app.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

object CertificatePinner {

    private const val HOSTNAME = "your-domain.com"
    private const val PIN_SHA256 = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    fun createPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(HOSTNAME, PIN_SHA256)
            .build()
    }

    fun applyToClient(clientBuilder: OkHttpClient.Builder): OkHttpClient.Builder {
        return clientBuilder.certificatePinner(createPinner())
    }

    fun createSecureClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(createPinner())
            .build()
    }
}
