package com.grimnej.lmcomment.workflow

import com.grimnej.lmcomment.config.DemoConfiguration
import com.grimnej.lmcomment.relay.GenerationRequest
import com.grimnej.lmcomment.relay.GenerationResponse
import com.grimnej.lmcomment.relay.RelayClient

internal fun interface GenerationGateway {
    suspend fun generate(
        configuration: DemoConfiguration,
        request: GenerationRequest,
    ): GenerationResponse
}

internal object RelayGenerationGateway : GenerationGateway {
    override suspend fun generate(
        configuration: DemoConfiguration,
        request: GenerationRequest,
    ): GenerationResponse = RelayClient(
        relayBaseUrl = configuration.relayBaseUrl,
        demoToken = configuration.demoToken,
    ).generate(request)
}
