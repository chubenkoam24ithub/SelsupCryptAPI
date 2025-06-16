package selsup.test.ctyptapi.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRequest(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("data") String data) {
}
