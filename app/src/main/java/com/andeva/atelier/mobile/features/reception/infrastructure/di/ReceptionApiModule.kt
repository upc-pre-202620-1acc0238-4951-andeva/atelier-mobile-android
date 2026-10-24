package com.andeva.atelier.mobile.features.reception.infrastructure.di

import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AppointmentService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CustomerService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReceptionApiModule {
    @Provides
    @Singleton
    fun provideCustomerService(retrofit: Retrofit): CustomerService = retrofit.create(CustomerService::class.java)

    @Provides
    @Singleton
    fun provideVehicleService(retrofit: Retrofit): VehicleService = retrofit.create(VehicleService::class.java)

    @Provides
    @Singleton
    fun provideAppointmentService(retrofit: Retrofit): AppointmentService = retrofit.create(AppointmentService::class.java)
}
