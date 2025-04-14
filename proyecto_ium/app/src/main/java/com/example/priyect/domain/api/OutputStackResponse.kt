package com.example.priyect.domain.api

class OutputStackResponse(text: String,
                          data : Map<String,String>,
                          traceId : String,
                          source : String) {
    var text: String
    var data : Map<String,String>
    var traceId : String
    var source : String

    init {
        this.text = text
        this.data = data
        this.traceId = traceId
        this.source = source
    }
}