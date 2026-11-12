package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * An ISA annotation value.
 *
 * <p>ISA-JSON allows parameter, characteristic and factor values to be either an ontology
 * annotation object or a plain string or number. Scalar values are read into {@link
 * #annotationValue} so both forms can be processed alike.
 */
@Data
@FieldNameConstants
public class Value {
    public String annotationValue;
    public String termSource;
    public String termAccession;

    public Value() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Value fromJson(final JsonNode node) {
        final Value value = new Value();
        if (node == null || node.isNull()) {
            return value;
        }

        if (node.isObject()) {
            value.annotationValue = text(node.get(Fields.annotationValue));
            value.termSource = text(node.get(Fields.termSource));
            value.termAccession = text(node.get(Fields.termAccession));
        } else {
            value.annotationValue = node.asText();
        }
        return value;
    }

    private static String text(final JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
