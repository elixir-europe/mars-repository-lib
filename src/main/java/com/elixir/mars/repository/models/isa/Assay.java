package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
public class Assay {
    @JsonProperty("@id")
    public String id;

    public String filename;
    public MeasurementType measurementType;
    public TechnologyType technologyType;
    public String technologyPlatform;
    public ArrayList<CharacteristicCategory> characteristicCategories;
    public Materials materials;
    public ArrayList<ProcessSequence> processSequence;
    public ArrayList<DataFile> dataFiles;
    public ArrayList<Comment> comments;
    public ArrayList<Object> unitCategories;
}
