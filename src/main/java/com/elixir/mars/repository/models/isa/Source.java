package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class Source {
    @JsonProperty("@id")
    public String id;

    public String name;
    public ArrayList<Characteristic> characteristics;
}
