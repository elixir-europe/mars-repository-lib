package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
public class Protocol {
    @JsonProperty("@id")
    public String id;

    public String name;
    public ProtocolType protocolType;
    public String description;
    public String uri;
    public String version;
    public ArrayList<Parameter> parameters;
    public ArrayList<Component> components;
}
