package com.elixir.mars.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.receipt.MarsAssayComment;
import com.elixir.mars.repository.models.receipt.MarsReceipt;
import com.elixir.mars.repository.models.receipt.MarsSubmissionStatus;
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
    private static final String ISA_JSON = "MARS/test-data/ena-complete.json";
    private static IsaJson isaJson;

    @BeforeAll
    static void loadIsaJson() throws IOException {
        isaJson = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(Path.of(ISA_JSON).toFile(), IsaJson.class);
    }

    @Test
    void testBuildMarsReceiptReturnsCompleteReceipt() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");

        MarsReceipt receipt = provider.build(null, null, null, null, null, List.of("info"), List.of("error"), null, MarsSubmissionStatus.FAILURE);

        assertEquals("test-repository", receipt.targetRepository);
        assertEquals(MarsSubmissionStatus.FAILURE, receipt.submissionStatus);
        assertEquals("error", receipt.errors.get(0).message);
        assertEquals("info", receipt.info.get(0).message);
        assertTrue(receipt.accessions.isEmpty());
    }

    @Test
    void testBuildMarsReceiptAddsAssayCommentWithAssayPath() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        Assay assay = isaJson.getStudies().get(0).getAssays().get(0);
        MarsReceipt receipt = provider.build(null, null, null, null, null, null, null, List.of(
                new MarsReceiptProvider.AssayCommentInput("test-repository_name", "value", assay.getId())), null);

        assertEquals(1, receipt.assayComments.size());
        MarsAssayComment comment = receipt.assayComments.get(0);
        assertEquals("test-repository_name", comment.name);
        assertEquals("value", comment.value);
        assertEquals(2, comment.path.size());
        assertEquals("studies", comment.path.get(0).key);
        assertEquals("assays", comment.path.get(1).key);
        assertEquals("@id", comment.path.get(1).where.key);
        assertEquals(assay.getId(), comment.path.get(1).where.value);
    }

    @Test
    void testBuildMarsReceiptAddsAssayCommentWithoutPathWhenAssayIdIsAbsent() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        MarsReceipt receipt = provider.build(null, null, null, null, null, null, null, List.of(
                new MarsReceiptProvider.AssayCommentInput("test-repository_name", "value", null)), null);

        assertEquals(1, receipt.assayComments.size());
        assertEquals("test-repository_name", receipt.assayComments.get(0).name);
        assertEquals("value", receipt.assayComments.get(0).value);
        assertNull(receipt.assayComments.get(0).path);
    }

    @Test
    void testEachBuildStartsWithFreshInvocationLocalState() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        Assay assay = isaJson.getStudies().get(0).getAssays().get(0);
        ReceiptAccessionsMap studiesMap = new ReceiptAccessionsMap() {
            {
                isaItemName = Assay.Fields.id;
                accessionMap = Map.of(assay.getId(), "PRJ123456");
            }
        };

        MarsReceipt first = provider.build(
                studiesMap, null, null, null, null, List.of("first info"), List.of("first error"), null, MarsSubmissionStatus.FAILURE);
        MarsReceipt second = provider.build(
                null, null, null, null, null, List.of("second info"), List.of("second error"), null, MarsSubmissionStatus.FAILURE);
        MarsReceipt empty = provider.build(null, null, null, null, null, null, null, null, null);

        assertTrue(first.accessions.size() > 0);
        assertEquals(MarsSubmissionStatus.FAILURE, first.submissionStatus);
        assertEquals("second error", second.errors.get(0).message);
        assertEquals(MarsSubmissionStatus.FAILURE, second.submissionStatus);
        assertEquals("second info", second.info.get(0).message);
        assertTrue(second.accessions.isEmpty());
        assertTrue(empty.errors.isEmpty());
        assertTrue(empty.info.isEmpty());
        assertTrue(empty.accessions.isEmpty());
        assertNull(empty.submissionStatus);
    }

    @Test
    void testFailedBuildDoesNotAffectNextBuild() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");

        assertThrows(NullPointerException.class, () -> provider.buildWithIsaJson(null));

        MarsReceipt recovered = provider.build(null, null, null, null, null, List.of("recovered"), null, null, MarsSubmissionStatus.PENDING);
        assertEquals("recovered", recovered.info.get(0).message);
        assertEquals(MarsSubmissionStatus.PENDING, recovered.submissionStatus);
        assertTrue(recovered.errors.isEmpty());
    }

    @Test
    void testConcurrentBuildsOnSameProviderUseIndependentLocalState() throws Exception {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(
                    () -> provider.build(null, null, null, null, null, List.of("first-info"), null, null, MarsSubmissionStatus.PENDING));
            var second = executor.submit(
                    () -> provider.build(null, null, null, null, null, List.of("second-info"), null, null, MarsSubmissionStatus.PENDING));

            MarsReceipt firstReceipt = first.get(5, TimeUnit.SECONDS);
            MarsReceipt secondReceipt = second.get(5, TimeUnit.SECONDS);
            assertEquals("first-info", firstReceipt.info.get(0).message);
            assertEquals(MarsSubmissionStatus.PENDING, firstReceipt.submissionStatus);
            assertEquals("second-info", secondReceipt.info.get(0).message);
            assertEquals(MarsSubmissionStatus.PENDING, secondReceipt.submissionStatus);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testReceiptCanBeConsumedOnAnotherThreadAfterBuild() throws Exception {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            MarsReceipt receipt = provider.build(null, null, null, null, null, List.of("cross-thread"), null, null, MarsSubmissionStatus.PENDING);
            assertEquals(MarsSubmissionStatus.PENDING, receipt.submissionStatus);
            var result = executor.submit(() -> receipt.info.get(0).message);
            assertEquals("cross-thread", result.get(5, TimeUnit.SECONDS));
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
        for (DataFile dataFile : assay.getDataFiles()) {
            dataFilesMap.accessionMap.put(dataFile.getId(), "ERZ" + idx);
            idx++;
        }

        MarsReceipt receipt = provider.build(studiesMap, null, null, null, dataFilesMap, null, null, null, MarsSubmissionStatus.SUCCESS);
        assertEquals("test-repository", receipt.targetRepository);
        assertEquals(MarsSubmissionStatus.SUCCESS, receipt.submissionStatus);
        assertEquals(1 + assay.getDataFiles().size(), receipt.accessions.size());
        assertTrue(receipt.errors.isEmpty());
        assertTrue(receipt.info.isEmpty());
    }

    @Test
    void testMissingAccessionMapFieldIsReportedInReturnedReceipt() {
        TestMarsReceiptProvider provider = new TestMarsReceiptProvider("test-repository");
        ReceiptAccessionsMap studiesMap = new ReceiptAccessionsMap("missingField", "value");

        MarsReceipt receipt = provider.build(studiesMap, null, null, null, null, null, null, null, MarsSubmissionStatus.FAILURE);

        assertEquals(1, receipt.errors.size());
        assertEquals(MarsSubmissionStatus.FAILURE, receipt.submissionStatus);
        assertEquals(
                "Cannot find an item of Study with the key missingField in the ISA-JSON input",
                receipt.errors.get(0).message);
        assertTrue(receipt.accessions.isEmpty());
    }

    private static final class TestMarsReceiptProvider extends MarsReceiptProvider {
        TestMarsReceiptProvider(String repository) {
            super(repository);
        }

        MarsReceipt build(
                ReceiptAccessionsMap studies,
                ReceiptAccessionsMap samples,
                ReceiptAccessionsMap sources,
                ReceiptAccessionsMap otherMaterials,
                ReceiptAccessionsMap dataFiles,
                List<String> info,
                List<String> errors,
                List<AssayCommentInput> comments,
                MarsSubmissionStatus status) {
            return buildMarsReceipt(
                    studies, samples, sources, otherMaterials, dataFiles,
                    info, errors, comments, status, isaJson);
        }

        MarsReceipt buildWithIsaJson(IsaJson input) {
            return buildMarsReceipt(
                    null, null, null, null, null, null, null, null, null, input);
        }
    }
}
