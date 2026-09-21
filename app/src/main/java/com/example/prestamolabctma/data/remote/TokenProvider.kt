package com.example.prestamolabctma.data.remote

interface TokenProvider {
    fun getToken(): String?
}

class SimpleTokenProvider : TokenProvider {
    private var token: String? = null

    override fun getToken(): String? = token

    fun setToken(newToken: String?) {
        token = newToken
    }
}
