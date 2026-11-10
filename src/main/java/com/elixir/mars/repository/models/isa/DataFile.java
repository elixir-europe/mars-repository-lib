package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
public class DataFile {
    @JsonProperty("@id")
    public String id;

    public String name;
    public String type;
    public ArrayList<Comment> comments;
}
