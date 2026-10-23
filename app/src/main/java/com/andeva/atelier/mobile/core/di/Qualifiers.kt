package com.andeva.atelier.mobile.core.di

import javax.inject.Qualifier

/** Cliente sin TokenAuthenticator para `/auth/refresh` y `/auth/sign-out` (evita el bucle de refresh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/** Cliente sin cabeceras de Atelier para subir fotos a Supabase Storage. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseClient
