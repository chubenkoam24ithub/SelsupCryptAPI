package selsup.test.ctyptapi.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Document(
        @JsonProperty("participant_inn") String participantInn,
        @JsonProperty("document_date") String documentDate, // ISO дата: 2025-06-16
        @JsonProperty("document_number") String documentNumber,
        @JsonProperty("production_date") String productionDate, // ISO дата: 2025-06-15
        @JsonProperty("production_type") String productionType, // LOCAL для РФ
        @JsonProperty("products") Product[] products) {
    public Document {
        if (productionType == null) productionType = "LOCAL";
    }
}
