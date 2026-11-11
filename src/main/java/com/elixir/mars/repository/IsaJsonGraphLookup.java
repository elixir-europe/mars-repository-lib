package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.Comment;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.isa.Study;
import com.elixir.mars.repository.models.isa.Input;
import com.elixir.mars.repository.models.isa.Materials;
import com.elixir.mars.repository.models.isa.OtherMaterial;
import com.elixir.mars.repository.models.isa.ProcessSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared ISA-JSON graph helpers for repository service conversions.
 *
 * <p>Repository metadata and receipt conversion all need to resolve the same process/data-file
 * lineage. Keeping the lookup rules here avoids subtle drift, especially around ISA's
 * #data_file/... versus #data/... ID forms.
 *
 * <p>Declared process {@code inputs}/{@code outputs} always take precedence. A process that
 * declares none is resolved through its {@code previousProcess}/{@code nextProcess} links within
 * the process sequence it belongs to, so protocol chains without an intermediate material (e.g.
 * {@code growth culture -> sample collection}) still resolve.
 */
public final class IsaJsonGraphLookup {
    private static final String DATA_FILE_ID_PREFIX = "#data_file/";
    private static final String DATA_ID_PREFIX = "#data/";

    private IsaJsonGraphLookup() {
    }

    /** Finds a repository submission identifier stored as {@code <repo_id>_submission_id}. */
    public static String findSubmissionId(final IsaJson isaJson, final String repositoryId) {
        if (isaJson == null || isaJson.getStudies() == null || repositoryId == null) {
            return null;
        }

        final String expectedName = repositoryId + "_submission_id";
        for (final Study study : isaJson.getStudies()) {
            if (study == null || study.getAssays() == null) {
                continue;
            }
            for (final Assay assay : study.getAssays()) {
                if (assay == null || assay.getComments() == null) {
                    continue;
                }
                for (final Comment comment : assay.getComments()) {
                    if (comment == null || comment.getName() == null) {
                        continue;
                    }
                    if (expectedName.equals(comment.getName())) {
                        return comment.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the process that produced the given output material or data file.
     *
     * <p>ISA process outputs and assay data files can use different ID prefixes for the same
     * object, so data file IDs are normalized before comparison.
     *
     * <p>Declared outputs are matched first. If no process declares the item, a process without
     * declared outputs is matched when its outputs resolved through {@code nextProcess} links (see
     * {@link #findProcessOutputIds}) contain it.
     */
    public static ProcessSequence findProcessByOutputId(
            final List<ProcessSequence> processSequence, final String outputId) {
        if (processSequence == null || outputId == null) {
            return null;
        }

        final String normalizedOutputId = normalizeDataFileId(outputId);

        for (final ProcessSequence process : processSequence) {
            if (containsNormalizedId(IsaProcessGraph.declaredOutputIds(process), normalizedOutputId)) {
                return process;
            }
        }

        // Fallback for processes chained to their successor without an intermediate material.
        final IsaProcessGraph graph = new IsaProcessGraph(processSequence);
        for (final ProcessSequence process : processSequence) {
            if (process != null
                    && IsaProcessGraph.declaredOutputIds(process).isEmpty()
                    && containsNormalizedId(graph.getOutputIds(process), normalizedOutputId)) {
                return process;
            }
        }

        return null;
    }

    /**
     * Finds the first assay other material referenced by a process's declared inputs.
     *
     * <p>In ISA, a process input stores only an item ID. This helper resolves that ID against the
     * assay materials collection and returns the matching other material, if one exists. Use
     * {@link #findOtherMaterialFromProcessInput(List, ProcessSequence, Materials)} to also resolve
     * inputs through {@code previousProcess} links.
     */
    public static OtherMaterial findOtherMaterialFromProcessInput(
            final ProcessSequence process, final Materials materials) {
        return findOtherMaterialFromProcessInput(null, process, materials);
    }

    /**
     * Finds the first assay other material referenced by a process input, resolving the inputs
     * through {@code previousProcess} links in {@code processSequence} when the process declares
     * none (see {@link #findProcessInputIds}).
     */
    public static OtherMaterial findOtherMaterialFromProcessInput(
            final List<ProcessSequence> processSequence,
            final ProcessSequence process,
            final Materials materials) {
        if (process == null) {
            return null;
        }

        final Map<String, OtherMaterial> otherMaterialsById = buildOtherMaterialsById(materials);
        for (final String inputId : findProcessInputIds(processSequence, process)) {
            final OtherMaterial otherMaterial = otherMaterialsById.get(inputId);
            if (otherMaterial != null) {
                return otherMaterial;
            }
        }

        return null;
    }

    /**
     * Finds assay data files referenced by a process's declared outputs.
     *
     * <p>Process outputs can point to data files using the process-level ID form. This helper
     * normalizes those IDs before matching them against the assay data file list. Use
     * {@link #findDataFilesFromProcessOutputs(List, ProcessSequence, List)} to also resolve
     * outputs through {@code nextProcess} links.
     */
    public static List<DataFile> findDataFilesFromProcessOutputs(
            final ProcessSequence process, final List<DataFile> assayDataFiles) {
        return findDataFilesFromProcessOutputs(null, process, assayDataFiles);
    }

    /**
     * Finds assay data files referenced by a process's outputs, resolving the outputs through
     * {@code nextProcess} links in {@code processSequence} when the process declares none (see
     * {@link #findProcessOutputIds}).
     */
    public static List<DataFile> findDataFilesFromProcessOutputs(
            final List<ProcessSequence> processSequence,
            final ProcessSequence process,
            final List<DataFile> assayDataFiles) {
        final List<DataFile> dataFiles = new ArrayList<>();
        if (process == null || assayDataFiles == null) {
            return dataFiles;
        }

        for (final String outputId : findProcessOutputIds(processSequence, process)) {
            final String normalizedOutputId = normalizeDataFileId(outputId);
            for (final DataFile dataFile : assayDataFiles) {
                if (dataFile == null || dataFile.getId() == null) {
                    continue;
                }

                if (normalizeDataFileId(dataFile.getId()).equals(normalizedOutputId)) {
                    dataFiles.add(dataFile);
                }
            }
        }

        return dataFiles;
    }

    /**
     * Returns the IDs of the items that went into a process.
     *
     * <p>These are the declared inputs or, when the process declares none, the items resolved by
     * walking back through {@code previousProcess} links in {@code processSequence}: the outputs of
     * the preceding process, or its inputs when it declares no outputs either. Without a process
     * sequence only declared inputs are returned.
     */
    public static List<String> findProcessInputIds(
            final List<ProcessSequence> processSequence, final ProcessSequence process) {
        final List<String> declared = IsaProcessGraph.declaredInputIds(process);
        if (!declared.isEmpty() || process == null || processSequence == null) {
            return declared;
        }
        return new IsaProcessGraph(processSequence).getInputIds(process);
    }

    /**
     * Returns the IDs of the items produced by a process.
     *
     * <p>These are the declared outputs or, when the process declares none, the items resolved by
     * walking forward through {@code nextProcess} links in {@code processSequence}: the inputs of
     * the following process, or its outputs when it declares no inputs either. Without a process
     * sequence only declared outputs are returned.
     */
    public static List<String> findProcessOutputIds(
            final List<ProcessSequence> processSequence, final ProcessSequence process) {
        final List<String> declared = IsaProcessGraph.declaredOutputIds(process);
        if (!declared.isEmpty() || process == null || processSequence == null) {
            return declared;
        }
        return new IsaProcessGraph(processSequence).getOutputIds(process);
    }

    /**
     * Returns the processes chained directly before a process through {@code
     * previousProcess}/{@code nextProcess} links without an intermediate material, closest first.
     *
     * <p>Such processes act on the same material as {@code process}, so their parameters describe
     * the same step (e.g. a growth protocol chained to the sample collection that outputs the
     * sample). A preceding process that declares outputs produced a separate material and ends the
     * chain; a process with declared inputs has no such chain.
     */
    public static List<ProcessSequence> findLinkedUpstreamProcesses(
            final List<ProcessSequence> processSequence, final ProcessSequence process) {
        if (process == null
                || processSequence == null
                || !IsaProcessGraph.declaredInputIds(process).isEmpty()) {
            return Collections.emptyList();
        }
        return new IsaProcessGraph(processSequence).getUpstreamLinkedProcesses(process);
    }

    /**
     * Builds a lookup map of assay other materials keyed by their ISA IDs.
     */
    public static Map<String, OtherMaterial> buildOtherMaterialsById(final Materials materials) {
        final Map<String, OtherMaterial> otherMaterialsById = new HashMap<>();
        if (materials == null || materials.getOtherMaterials() == null) {
            return otherMaterialsById;
        }

        for (final OtherMaterial otherMaterial : materials.getOtherMaterials()) {
            if (otherMaterial != null && otherMaterial.getId() != null) {
                otherMaterialsById.put(otherMaterial.getId(), otherMaterial);
            }
        }

        return otherMaterialsById;
    }

    /**
     * Treat ISA #data_file/... and process #data/... references as equivalent.
     */
    public static String normalizeDataFileId(final String id) {
        return id == null ? null : id.replace(DATA_FILE_ID_PREFIX, DATA_ID_PREFIX);
    }

    private static boolean containsNormalizedId(final List<String> ids, final String normalizedId) {
        for (final String id : ids) {
            if (normalizeDataFileId(id).equals(normalizedId)) {
                return true;
            }
        }
        return false;
    }
}
