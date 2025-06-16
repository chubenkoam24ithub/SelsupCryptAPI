package selsup.test.ctyptapi.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Product(
        @JsonProperty("cis") String cis, // Код идентификации
        @JsonProperty("gtin") String gtin, // Глобальный номер товарной позиции
        @JsonProperty("certificate_document") String certificateDocument,
        @JsonProperty("certificate_document_date") String certificateDocumentDate, // ISO дата
        @JsonProperty("certificate_document_number") String certificateDocumentNumber) {
}
