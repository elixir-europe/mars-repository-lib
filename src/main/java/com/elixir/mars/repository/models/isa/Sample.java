package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sample {
    @JsonProperty("@id")
    public String id;

    public String name;
    public ArrayList<DerivesFrom> derivesFrom;
    public ArrayList<Characteristic> characteristics;
    public ArrayList<FactorValue> factorValues;
}
