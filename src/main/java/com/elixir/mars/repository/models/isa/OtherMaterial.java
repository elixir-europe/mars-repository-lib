package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtherMaterial {
  @JsonProperty("@id")
  public String id; // other_material-333

  public String name;
  public String type; // = library name
  public ArrayList<Characteristic> characteristics; // -> get characteristics
  public ArrayList<DerivesFrom> derivesFrom;
}
