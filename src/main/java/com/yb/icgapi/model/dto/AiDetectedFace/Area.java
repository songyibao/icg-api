package com.yb.icgapi.model.dto.AiDetectedFace;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Area {
    @JsonProperty("x")
    private Integer x;

    @JsonProperty("y")
    private Integer y;

    @JsonProperty("w")
    private Integer w;

    @JsonProperty("h")
    private Integer h;
}
