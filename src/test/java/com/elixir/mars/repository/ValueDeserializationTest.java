package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.ParameterValue;
import com.elixir.mars.repository.models.isa.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** ISA-JSON values may be ontology annotation objects or plain scalars. */
class ValueDeserializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsOntologyAnnotationObject() throws Exception {
        final Value value =
                objectMapper.readValue(
                        "{\"annotationValue\":\"leaf\",\"termSource\":\"PO\",\"termAccession\":\"PO:0025034\"}",
                        Value.class);
        assertEquals("leaf", value.getAnnotationValue());
        assertEquals("PO", value.getTermSource());
        assertEquals("PO:0025034", value.getTermAccession());
    }

    @Test
    void readsScalarString() throws Exception {
        final Value value = objectMapper.readValue("\"not applicable\"", Value.class);
        assertEquals("not applicable", value.getAnnotationValue());
        assertNull(value.getTermSource());
    }

    @Test
    void readsScalarNumberInsideParameterValue() throws Exception {
        final ParameterValue parameterValue =
                objectMapper.readValue(
                        "{\"category\":{\"@id\":\"#parameter/1\"},\"value\":20}", ParameterValue.class);
        assertEquals("20", parameterValue.getValue().getAnnotationValue());
    }

    @Test
    void roundTripsObjectForm() throws Exception {
        final Value value = objectMapper.readValue("{\"annotationValue\":\"leaf\"}", Value.class);
        assertEquals(value, objectMapper.readValue(objectMapper.writeValueAsString(value), Value.class));
    }
}
