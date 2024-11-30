package com.sksamuel.elastic4s.requests.synonyms

import com.fasterxml.jackson.annotation.JsonProperty

case class UpdateSynonymsSetResponse(
    result: String,
    @JsonProperty("reload_analyzers_details") reloadAnalyzersDetails: ReloadAnalyzersDetails
)
