package com.elixir.mars.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.receipt.MarsReceipt;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MarsReceiptProviderTest {
    private static final String ISA_JSON = "MARS/test-data/ena-complete.json";
    private static IsaJson isaJson;

    @BeforeAll
    static void loadIsaJson() throws IOException {
        isaJson = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(Path.of(ISA_JSON).toFile(), IsaJson.class);
    }

    @Test
    void testResetMarsReceipt() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        MarsReceipt initial = provider.getMarsReceipt();
        assertEquals("test-repository", initial.targetRepository);
        assertTrue(initial.accessions.isEmpty());
        assertTrue(initial.errors.isEmpty());
        assertTrue(initial.info.isEmpty());

        provider.build(null, null, null, null, null, List.of("info"), List.of("error"));
        provider.resetMarsReceipt();
        MarsReceipt reset = provider.getMarsReceipt();
        assertNotEquals(initial.errors, reset.errors);
        assertNotEquals(initial.info, reset.info);
        assertTrue(reset.accessions.isEmpty());
    }

    @Test
    void testSetMarsReceiptAccessions() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        Assay assay = isaJson.getStudies().get(0).getAssays().get(0);
        ReceiptAccessionsMap studiesMap = new ReceiptAccessionsMap() {
            {
                isaItemName = Assay.Fields.id;
                accessionMap = Map.of(assay.getId(), "PRJ123456");
            }
        };

        ReceiptAccessionsMap dataFilesMap = new ReceiptAccessionsMap() {
            {
                isaItemName = DataFile.Fields.id;
                accessionMap = new HashMap<>();
            }
        };
        int idx = 0;
        for (DataFile df : assay.getDataFiles()) {
            dataFilesMap.accessionMap.put(df.getId(), "ERZ" + idx);
            idx++;
        }
        provider.build(studiesMap, null, null, null, dataFilesMap, null, null);
        MarsReceipt receipt = provider.getMarsReceipt();
        assertEquals("test-repository", receipt.targetRepository);
        assertEquals(1 + assay.getDataFiles().size(), receipt.accessions.size());
        assertTrue(receipt.getErrors().isEmpty());
        assertTrue(receipt.getInfo().isEmpty());
    }

    @Test
    void testGetAccessionMapEntry_fieldDoesNotExist() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        ReceiptAccessionsMap studiesMap = new ReceiptAccessionsMap("missingField", "value");

        provider.build(studiesMap, null, null, null, null, null, null);

        MarsReceipt receipt = provider.getMarsReceipt();
        assertEquals(1, receipt.errors.size());
        assertEquals(
                "Cannot find an item of Study with the key missingField in the ISA-JSON input",
                receipt.errors.get(0).message);
        assertTrue(receipt.accessions.isEmpty());
    }

    private static final class TestMarsReceiptProvider extends MarsReceiptProvider {
        TestMarsReceiptProvider(String repository) {
            super(repository);
        }

        void build(
                ReceiptAccessionsMap studies,
                ReceiptAccessionsMap samples,
                ReceiptAccessionsMap sources,
                ReceiptAccessionsMap otherMaterials,
                ReceiptAccessionsMap dataFiles,
                List<String> info,
                List<String> errors) {
            buildMarsReceipt(studies, samples, sources, otherMaterials, dataFiles, info, errors, isaJson);
        }

        @Override
        public String convertMarsReceiptToJson() {
            return "";
        }
    }
}
