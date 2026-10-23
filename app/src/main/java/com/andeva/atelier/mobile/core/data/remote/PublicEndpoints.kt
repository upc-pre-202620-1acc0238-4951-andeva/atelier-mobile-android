package com.andeva.atelier.mobile.core.data.remote

import okhttp3.Request

/** Rutas que no llevan `Authorization` (docs/api/README.md §4). */
object PublicEndpoints {
    fun isPublic(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.contains("/api/v1/auth/") ||
            path.endsWith("/api/v1/invitations/validate") ||
            path.endsWith("/api/v1/invitations/accept") ||
            (request.method == "GET" && path.endsWith("/api/v1/billing/plans"))
    }
}
