package com.example.priyect.domain.api

/**
 * Clase que representa una Respuesta del servidor
 *
 * @param email Email del Usuario que tiene una nota compartida
 */
data class ResponseCogniyi(val text : String, val useriD : String, val sessionId : String, val outputStack: List<OutputStackResponse>)