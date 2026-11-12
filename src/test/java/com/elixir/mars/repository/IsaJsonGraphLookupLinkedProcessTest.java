package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.Input;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.isa.NextProcess;
import com.elixir.mars.repository.models.isa.Output;
import com.elixir.mars.repository.models.isa.PreviousProcess;
import com.elixir.mars.repository.models.isa.ProcessSequence;
import com.elixir.mars.repository.models.isa.Protocol;
import com.elixir.mars.repository.models.isa.Sample;
import com.elixir.mars.repository.models.isa.Study;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code previousProcess}/{@code nextProcess} fallbacks of {@link IsaJsonGraphLookup}
 * using the PTX Daphnia magna ISA-JSON, whose study chains "sample growth culture" and "sample
 * collection" without an intermediate material. Its assays declare every input and output, so the
 * assay cases clear them to simulate protocol chains without a material in between.
 */
class IsaJsonGraphLookupLinkedProcessTest {
    private static final String PTX_ISA_JSON =
            "MARS/test-data/ISA-PTX-TEST/isajson/i_investigation_PTX_Pilot_Daphnia_magna_valid.json";
    private static final String GROWTH_CULTURE_PROTOCOL = "sample growth culture";
    private static final String SAMPLE_COLLECTION_PROTOCOL = "sample collection";
    private static final String LIBRARY_PREPARATION_PROTOCOL = "library preparation";
    private static final String SEQUENCING_PROTOCOL = "nucleic acid sequencing";

    private Study study;
    private List<ProcessSequence> studyProcesses;
    private Map<String, String> protocolNamesById;
    private Assay rnaSeqAssay;
    private List<ProcessSequence> assayProcesses;

    @BeforeEach
    void setUp() throws Exception {
        study = readIsaJson(PTX_ISA_JSON).getStudies().get(0);
        studyProcesses = study.getProcessSequence();
        protocolNamesById =
                study.getProtocols().stream()
                        .collect(Collectors.toMap(Protocol::getId, Protocol::getName));
        rnaSeqAssay =
                study.getAssays().stream()
                        .filter(assay -> !processesExecuting(assay.getProcessSequence(), SEQUENCING_PROTOCOL).isEmpty())
                        .findFirst()
                        .orElseThrow();
        assayProcesses = rnaSeqAssay.getProcessSequence();
    }

    @Test
    void sampleCollectionWithoutInputsResolvesSourceThroughPreviousProcess() {
        for (final Sample sample : study.getMaterials().getSamples()) {
            final ProcessSequence sampleCollection =
                    IsaJsonGraphLookup.findProcessByOutputId(studyProcesses, sample.getId());

            assertEquals(SAMPLE_COLLECTION_PROTOCOL, protocolName(sampleCollection));
            assertTrue(
                    isEmpty(sampleCollection.getInputs()),
                    "PTX sample collection processes should not declare inputs");
            assertEquals(
                    List.of(sample.getDerivesFrom().get(0).getId()),
                    IsaJsonGraphLookup.findProcessInputIds(studyProcesses, sampleCollection),
                    "Source should be resolved through the preceding growth culture process");

            final List<ProcessSequence> upstream =
                    IsaJsonGraphLookup.findLinkedUpstreamProcesses(studyProcesses, sampleCollection);
            assertEquals(1, upstream.size());
            assertEquals(GROWTH_CULTURE_PROTOCOL, protocolName(upstream.get(0)));
        }
    }

    @Test
    void growthCultureWithoutOutputsResolvesSampleThroughNextProcess() {
        final Map<String, Sample> samplesById =
                study.getMaterials().getSamples().stream()
                        .collect(Collectors.toMap(Sample::getId, sample -> sample));
        final List<ProcessSequence> growthProcesses =
                processesExecuting(studyProcesses, GROWTH_CULTURE_PROTOCOL);
        assertFalse(growthProcesses.isEmpty());

        for (final ProcessSequence growthProcess : growthProcesses) {
            assertTrue(isEmpty(growthProcess.getOutputs()));

            final List<String> outputIds =
                    IsaJsonGraphLookup.findProcessOutputIds(studyProcesses, growthProcess);
            assertEquals(1, outputIds.size());
            assertEquals(
                    growthProcess.getInputs().get(0).getId(),
                    samplesById.get(outputIds.get(0)).getDerivesFrom().get(0).getId(),
                    "Growth culture should resolve to the sample derived from its own source");
        }
    }

    @Test
    void sequencingWithoutInputsResolvesLibraryThroughPreviousProcess() {
        final List<ProcessSequence> sequencingProcesses =
                processesExecuting(assayProcesses, SEQUENCING_PROTOCOL);
        final Map<String, String> libraryIdBySequencingId = new HashMap<>();
        sequencingProcesses.forEach(
                process -> libraryIdBySequencingId.put(process.getId(), process.getInputs().get(0).getId()));

        sequencingProcesses.forEach(process -> process.getInputs().clear());

        for (final ProcessSequence sequencingProcess : sequencingProcesses) {
            assertEquals(
                    libraryIdBySequencingId.get(sequencingProcess.getId()),
                    IsaJsonGraphLookup.findOtherMaterialFromProcessInput(
                                    assayProcesses, sequencingProcess, rnaSeqAssay.getMaterials())
                            .getId());
            assertNull(
                    IsaJsonGraphLookup.findOtherMaterialFromProcessInput(
                            sequencingProcess, rnaSeqAssay.getMaterials()),
                    "Without the process sequence only declared inputs are considered");
            assertTrue(
                    IsaJsonGraphLookup.findLinkedUpstreamProcesses(assayProcesses, sequencingProcess).isEmpty(),
                    "Library preparation produced a library material, so it is not chained to sequencing");
        }
    }

    @Test
    void libraryPreparationWithoutOutputsIsFoundThroughNextProcess() {
        final List<ProcessSequence> libraryPreparations =
                processesExecuting(assayProcesses, LIBRARY_PREPARATION_PROTOCOL);
        final Map<String, String> libraryPreparationIdByLibraryId = new HashMap<>();
        libraryPreparations.forEach(
                process -> libraryPreparationIdByLibraryId.put(process.getOutputs().get(0).getId(), process.getId()));

        libraryPreparations.forEach(process -> process.getOutputs().clear());

        assertEquals(
                libraryPreparations.size(),
                libraryPreparationIdByLibraryId.size(),
                "Each library preparation process should have produced its own library");
        libraryPreparationIdByLibraryId.forEach(
                (libraryId, libraryPreparationId) ->
                        assertEquals(
                                libraryPreparationId,
                                IsaJsonGraphLookup.findProcessByOutputId(assayProcesses, libraryId).getId()));
    }

    @Test
    void libraryPreparationChainedToSequencingWithoutLibraryMaterialResolvesExtract() {
        final Map<String, ProcessSequence> processesById =
                assayProcesses.stream().collect(Collectors.toMap(ProcessSequence::getId, process -> process));
        final List<ProcessSequence> sequencingProcesses =
                processesExecuting(assayProcesses, SEQUENCING_PROTOCOL);
        final Map<String, String> extractIdBySequencingId = new HashMap<>();
        final Map<String, String> libraryPreparationIdBySequencingId = new HashMap<>();
        for (final ProcessSequence sequencingProcess : sequencingProcesses) {
            final ProcessSequence libraryPreparation =
                    processesById.get(sequencingProcess.getPreviousProcess().getId());
            libraryPreparationIdBySequencingId.put(sequencingProcess.getId(), libraryPreparation.getId());
            extractIdBySequencingId.put(
                    sequencingProcess.getId(), libraryPreparation.getInputs().get(0).getId());
        }

        // Library preparation -> sequencing without a library node in between.
        processesExecuting(assayProcesses, LIBRARY_PREPARATION_PROTOCOL)
                .forEach(process -> process.getOutputs().clear());
        sequencingProcesses.forEach(process -> process.getInputs().clear());

        for (final ProcessSequence sequencingProcess : sequencingProcesses) {
            assertEquals(
                    extractIdBySequencingId.get(sequencingProcess.getId()),
                    IsaJsonGraphLookup.findOtherMaterialFromProcessInput(
                                    assayProcesses, sequencingProcess, rnaSeqAssay.getMaterials())
                            .getId());
            assertEquals(
                    List.of(libraryPreparationIdBySequencingId.get(sequencingProcess.getId())),
                    ids(IsaJsonGraphLookup.findLinkedUpstreamProcesses(assayProcesses, sequencingProcess)));
        }
    }

    @Test
    void sequencingWithoutOutputsResolvesDataFilesThroughNextProcess() {
        final ProcessSequence sequencingProcess =
                processesExecuting(assayProcesses, SEQUENCING_PROTOCOL).get(0);
        final List<String> dataFileIds =
                sequencingProcess.getOutputs().stream().map(Output::getId).collect(Collectors.toList());

        // A downstream process that only declares the raw files as its inputs.
        final ProcessSequence qualityControl = new ProcessSequence();
        qualityControl.setId("#process/quality_control_test");
        final PreviousProcess previousProcess = new PreviousProcess();
        previousProcess.setId(sequencingProcess.getId());
        qualityControl.setPreviousProcess(previousProcess);
        qualityControl.setInputs(new ArrayList<>());
        for (final String dataFileId : dataFileIds) {
            final Input input = new Input();
            input.setId(dataFileId);
            qualityControl.getInputs().add(input);
        }
        assayProcesses.add(qualityControl);
        sequencingProcess.getOutputs().clear();

        for (final String dataFileId : dataFileIds) {
            assertEquals(
                    sequencingProcess.getId(),
                    IsaJsonGraphLookup.findProcessByOutputId(assayProcesses, dataFileId).getId());
        }
        assertEquals(
                dataFileIds,
                IsaJsonGraphLookup.findDataFilesFromProcessOutputs(
                                assayProcesses, sequencingProcess, rnaSeqAssay.getDataFiles())
                        .stream()
                        .map(DataFile::getId)
                        .collect(Collectors.toList()));
        assertTrue(
                IsaJsonGraphLookup.findDataFilesFromProcessOutputs(sequencingProcess, rnaSeqAssay.getDataFiles())
                        .isEmpty(),
                "Without the process sequence only declared outputs are considered");
    }

    @Test
    void declaredInputsAndOutputsTakePrecedenceOverLinks() {
        for (final ProcessSequence process : assayProcesses) {
            assertEquals(
                    process.getInputs().stream().map(Input::getId).collect(Collectors.toList()),
                    IsaJsonGraphLookup.findProcessInputIds(assayProcesses, process));
            assertEquals(
                    process.getOutputs().stream().map(Output::getId).collect(Collectors.toList()),
                    IsaJsonGraphLookup.findProcessOutputIds(assayProcesses, process));
            assertTrue(IsaJsonGraphLookup.findLinkedUpstreamProcesses(assayProcesses, process).isEmpty());
        }
    }

    @Test
    void cyclicLinksWithoutMaterialsTerminate() {
        final ProcessSequence first = linkedProcess("#process/first", "#process/second");
        final ProcessSequence second = linkedProcess("#process/second", "#process/first");
        final List<ProcessSequence> processes = List.of(first, second);

        assertTrue(IsaJsonGraphLookup.findProcessInputIds(processes, first).isEmpty());
        assertTrue(IsaJsonGraphLookup.findProcessOutputIds(processes, first).isEmpty());
        assertEquals(
                List.of("#process/second"),
                ids(IsaJsonGraphLookup.findLinkedUpstreamProcesses(processes, first)));
        assertNull(IsaJsonGraphLookup.findProcessByOutputId(processes, "#sample/none"));
    }

    private static ProcessSequence linkedProcess(final String id, final String nextProcessId) {
        final ProcessSequence process = new ProcessSequence();
        process.setId(id);
        final NextProcess nextProcess = new NextProcess();
        nextProcess.setId(nextProcessId);
        process.setNextProcess(nextProcess);
        return process;
    }

    /** The PTX export mixes scalar and ontology-annotation values, which {@code Value} accepts. */
    private static IsaJson readIsaJson(final String path) throws Exception {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new File(path), IsaJson.class);
    }

    private List<ProcessSequence> processesExecuting(
            final List<ProcessSequence> processes, final String protocolName) {
        return processes.stream()
                .filter(process -> protocolName.equals(protocolName(process)))
                .collect(Collectors.toList());
    }

    private String protocolName(final ProcessSequence process) {
        return process.getExecutesProtocol() == null
                ? null
                : protocolNamesById.get(process.getExecutesProtocol().getId());
    }

    private static List<String> ids(final List<ProcessSequence> processes) {
        return processes.stream().map(ProcessSequence::getId).collect(Collectors.toList());
    }

    private static boolean isEmpty(final List<?> items) {
        return items == null || items.isEmpty();
    }
}
