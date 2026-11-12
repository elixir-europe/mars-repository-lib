package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.Comment;
import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.isa.OtherMaterial;
import com.elixir.mars.repository.models.isa.ProcessSequence;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IsaJsonGraphLookupTest {
    private static final String ISA_JSON = "MARS/test-data/ena-complete.json";

    private static IsaJson isaJson;

    private static Assay assay;

    @BeforeAll
    static void loadIsaJson() throws IOException {
        isaJson = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(Path.of(ISA_JSON).toFile(), IsaJson.class);
        assay = isaJson.getStudies().get(0).getAssays().get(0);
        // Add submission ID as a comment in the assay
        Comment comment = new Comment();
        comment.setName("ena_submission_id");
        comment.setValue("submission-123");
        assay.getComments().add(comment);
    }

    @Test
    void testFindProcessByOutputId() {
        ProcessSequence process =
            IsaJsonGraphLookup.findProcessByOutputId(assay.getProcessSequence(), "#data/334");
        assertEquals("#process/nucleic_acid_sequencing/334", process.getId());
    }

    @Test
    void testFindProcessByOutputId_noProcessFound() {
        assertNull(
            IsaJsonGraphLookup.findProcessByOutputId(
                assay.getProcessSequence(), "#data/does-not-exist"));
    }

    @Test
    void testFindOtherMaterialFromProcessInput() {
        ProcessSequence process = assay.getProcessSequence().get(2);
        OtherMaterial material =
            IsaJsonGraphLookup.findOtherMaterialFromProcessInput(process, assay.getMaterials());
        assertEquals("#other_material/333", material.getId());
    }

    @Test
    void testFindDataFilesFromProcessOutputs() {
        ProcessSequence process = assay.getProcessSequence().get(2);
        List<DataFile> dataFiles = IsaJsonGraphLookup.findDataFilesFromProcessOutputs(process, assay.getDataFiles());
        assertEquals(List.of("#data_file/334", "#data_file/335"), dataFiles.stream().map(DataFile::getId).toList());
    }

    @Test
    void testBuildOtherMaterialsById() {
        Map<String, OtherMaterial> materialsById = IsaJsonGraphLookup.buildOtherMaterialsById(assay.getMaterials());
        assertEquals("#other_material/332", materialsById.get("#other_material/332").getId());
    }

    @Test
    void testNormalizeDataFileId() {
        assertEquals("#data/334", IsaJsonGraphLookup.normalizeDataFileId("#data_file/334"));
        assertEquals("#data/334", IsaJsonGraphLookup.normalizeDataFileId("#data/334"));
        assertEquals("#sample/331", IsaJsonGraphLookup.normalizeDataFileId("#sample/331"));
        assertNull(IsaJsonGraphLookup.normalizeDataFileId(null));
    }

    @Test
    void testFindSubmissionIdForRepository() {
        assertEquals("submission-123", IsaJsonGraphLookup.findSubmissionId(isaJson, "ena"));
        assertNull(IsaJsonGraphLookup.findSubmissionId(isaJson, "eva"));
    }
}
