package com.global.ct.frameinventory.frame;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FieldChange(
    @JsonProperty("old") String oldValue,
    @JsonProperty("new") String newValue
) {
}
