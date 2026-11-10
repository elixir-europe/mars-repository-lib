package com.elixir.mars.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MarsReceiptProviderTest {
    private static final String ISA_JSON = "MARS/test-data/biosamples-input-isa-multi.json";
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
        assertTrue(initial.errors.isEmpty());
        assertTrue(initial.info.isEmpty());
        assertTrue(reset.accessions.isEmpty());
    }

    @Test
    void testBuildMarsReceiptResetsAllStateBetweenRequests() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        Assay assay = isaJson.getStudies().get(0).getAssays().get(0);
        ReceiptAccessionsMap studiesMap = new ReceiptAccessionsMap() {
            {
                isaItemName = Assay.Fields.id;
                accessionMap = Map.of(assay.getId(), "PRJ123456");
            }
        };

        provider.build(
                studiesMap,
                null,
                null,
                null,
                null,
                List.of("first info"),
                List.of("first error"));
        assertTrue(provider.getMarsReceipt().accessions.size() > 0);

        provider.build(
                null,
                null,
                null,
                null,
                null,
                List.of("second info"),
                List.of("second error"));
        MarsReceipt second = provider.getMarsReceipt();
        assertTrue(second.accessions.isEmpty());
        assertEquals(1, second.errors.size());
        assertEquals("second error", second.errors.get(0).message);
        assertEquals(1, second.info.size());
        assertEquals("second info", second.info.get(0).message);

        provider.build(null, null, null, null, null, null, null);
        MarsReceipt empty = provider.getMarsReceipt();
        assertTrue(empty.accessions.isEmpty());
        assertTrue(empty.errors.isEmpty());
        assertTrue(empty.info.isEmpty());
        assertEquals("test-repository", empty.targetRepository);
    }

    @Test
    void testFailedBuildStartsFreshStateOnNextBuild() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");

        assertThrows(NullPointerException.class, () -> provider.buildWithIsaJson(null));

        provider.build(null, null, null, null, null, List.of("recovered"), null);
        MarsReceipt recovered = provider.getMarsReceipt();
        assertEquals("recovered", recovered.info.get(0).message);
        assertTrue(recovered.errors.isEmpty());
    }

    @Test
    void testConcurrentBuildOnSameProviderUsesThreadLocalState() throws Exception {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(
                    () -> provider.buildAndGet(
                            null, null, null, null, null, List.of("first-info"), null));
            var second = executor.submit(
                    () -> provider.buildAndGet(
                            null, null, null, null, null, List.of("second-info"), null));

            assertEquals("first-info", first.get(5, TimeUnit.SECONDS).info.get(0).message);
            assertEquals("second-info", second.get(5, TimeUnit.SECONDS).info.get(0).message);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testSeparateProvidersCanBuildConcurrently() throws Exception {
        TestMarsReceiptProvider firstProvider = new TestMarsReceiptProvider("first-repository");
        TestMarsReceiptProvider secondProvider = new TestMarsReceiptProvider("second-repository");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(
                    () -> firstProvider.buildAndGet(
                            null, null, null, null, null, List.of("first-info"), null));
            var second = executor.submit(
                    () -> secondProvider.buildAndGet(
                            null, null, null, null, null, List.of("second-info"), null));

            assertEquals("first-repository", first.get(5, TimeUnit.SECONDS).targetRepository);
            assertEquals("second-repository", second.get(5, TimeUnit.SECONDS).targetRepository);
            assertEquals("first-info", first.get().info.get(0).message);
            assertEquals("second-info", second.get().info.get(0).message);
        } finally {
            executor.shutdownNow();
        }
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

        void buildWithIsaJson(IsaJson input) {
            buildMarsReceipt(null, null, null, null, null, null, null, input);
        }

        MarsReceipt buildAndGet(
                ReceiptAccessionsMap studies,
                ReceiptAccessionsMap samples,
                ReceiptAccessionsMap sources,
                ReceiptAccessionsMap otherMaterials,
                ReceiptAccessionsMap dataFiles,
                List<String> info,
                List<String> errors) {
            build(studies, samples, sources, otherMaterials, dataFiles, info, errors);
            return getMarsReceipt();
        }

        @Override
        public String convertMarsReceiptToJson() {
            return "";
        }
    }
}
