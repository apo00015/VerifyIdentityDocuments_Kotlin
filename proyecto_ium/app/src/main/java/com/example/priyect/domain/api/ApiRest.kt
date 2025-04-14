package com.example.priyect.domain.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que representa la API-REST para realizar llamadas al chatbot
 */
interface ApiRest {
    /**
     * Método para enviar un mensaje al chatbot
     *
     * @param body Body a pasar
     */
    @POST(".")
    suspend fun postResponse(@Body body: BodyCognigy) : Response<ResponseCogniyi>

}