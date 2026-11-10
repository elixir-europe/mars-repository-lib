package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
public class Materials {
    public ArrayList<Source> sources;
    public ArrayList<Sample> samples;
    public ArrayList<OtherMaterial> otherMaterials;
}
