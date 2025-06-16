package selsup.test.ctyptapi.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentRequest(
        @JsonProperty("document") Document document,
        @JsonProperty("signature") String signature) {
}
