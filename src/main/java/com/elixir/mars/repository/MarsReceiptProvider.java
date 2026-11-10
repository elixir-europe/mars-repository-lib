package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.isa.Study;
import com.elixir.mars.repository.models.receipt.MarsAccession;
import com.elixir.mars.repository.models.receipt.MarsError;
import com.elixir.mars.repository.models.receipt.MarsErrorType;
import com.elixir.mars.repository.models.receipt.MarsInfo;
import com.elixir.mars.repository.models.receipt.MarsMessage;
import com.elixir.mars.repository.models.receipt.MarsPath;
import com.elixir.mars.repository.models.receipt.MarsReceipt;
import com.elixir.mars.repository.models.receipt.MarsWhere;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public abstract class MarsReceiptProvider {

    private final String targetRepository;
    private final ThreadLocal<ReceiptState> state = new ThreadLocal<>();

    private class ReceiptAccessionMap {
        public String isaItemName;
        public String isaFieldKey;
        public String isaFieldValue;
        public String accession;
    }

    private static final class ReceiptState {
        private final String targetRepository;
        private final MarsMessage message = MarsMessage.builder().build();
        private final List<MarsAccession> accessions = new ArrayList<>();
        private boolean buildCompleted;
        private boolean buildFailed;

        private ReceiptState(final String targetRepository) {
            this.targetRepository = targetRepository;
        }

        private MarsReceipt toReceipt() {
            return MarsReceipt.builder()
                    .targetRepository(targetRepository)
                    .errors(List.copyOf(message.errors))
                    .info(List.copyOf(message.info))
                    .accessions(List.copyOf(accessions))
                    .build();
        }
    }

    public MarsReceiptProvider(final String targetRepository) {
        this.targetRepository = targetRepository;
        state.set(new ReceiptState(targetRepository));
    }

    public abstract String convertMarsReceiptToJson();

    /**
     * Resets receipt state for existing callers.
     *
     * @deprecated Receipt state is initialized per thread and cleaned after
     *     getMarsReceipt(); retained temporarily for existing consumers.
     */
    @Deprecated
    public void resetMarsReceipt() {
        state.set(new ReceiptState(targetRepository));
    }

    private ReceiptState currentState() {
        ReceiptState current = state.get();
        if (current == null) {
            throw new IllegalStateException("Receipt state is not active on this thread");
        }
        return current;
    }

    private ReceiptState stateForBuild() {
        ReceiptState current = state.get();
        if (current == null || current.buildCompleted || current.buildFailed) {
            current = new ReceiptState(targetRepository);
            state.set(current);
        }
        return current;
    }

    /**
     * @deprecated Use the receipt returned by `buildMarsReceipt(...)`.
     */
    @Deprecated
    public MarsReceipt getMarsReceipt() {
        ReceiptState current = state.get();
        if (current == null) {
            throw new IllegalStateException("No active receipt state on this thread");
        }

        MarsReceipt result = current.toReceipt();
        state.remove();
        return result;
    }

    /**
     * Builds a receipt using the existing request/build API.
     */
    protected MarsReceipt buildMarsReceipt(
            final ReceiptAccessionsMap studiesAccessionsMap,
            final ReceiptAccessionsMap samplesAccessionsMap,
            final ReceiptAccessionsMap sourcesAccessionsMap,
            final ReceiptAccessionsMap otherMaterialsAccessionsMap,
            final ReceiptAccessionsMap dataFilesAccessionsMap,
            final List<String> info,
            final List<String> errors,
            final IsaJson isaJson) {
        ReceiptState current = stateForBuild();
        try {
            setMarsReceiptErrors(MarsErrorType.INVALID_METADATA,
                    Optional.ofNullable(errors).orElse(List.of()).toArray(String[]::new));
            setMarsReceiptInfo(Optional.ofNullable(info).orElse(List.of()).toArray(String[]::new));
            setMarsAccessions(
                    studiesAccessionsMap,
                    samplesAccessionsMap,
                    sourcesAccessionsMap,
                    otherMaterialsAccessionsMap,
                    dataFilesAccessionsMap,
                    isaJson);
            current.buildCompleted = true;
            return current.toReceipt();
        } catch (RuntimeException exception) {
            current.buildFailed = true;
            throw exception;
        }
    }

    protected void setMarsReceiptErrors(MarsErrorType type, final MarsError... errors) {
        for (MarsError error : Optional.ofNullable(errors).filter(error -> error != null).get()) {
            currentState().message.errors
                    .add(
                            MarsError.builder()
                                    .message(error.message)
                                    .path(error.path)
                                    .type(type)
                                    .build());
        }
    }

    protected void setMarsReceiptErrors(MarsErrorType type, final String... errors) {
        for (String error : Optional.ofNullable(errors).orElse(new String[]{"Null message received"})) {
            currentState().message.errors
                    .add(
                            MarsError.builder()
                                    .message(error)
                                    .type(type)
                                    .build());
        }
    }

    protected void setMarsReceiptInfo(final String... info) {
        for (String infoItem : Optional.ofNullable(info).orElse(new String[0])) {
            currentState().message.info
                    .add(
                            MarsInfo.builder()
                                    .message(infoItem)
                                    .build());
        }
    }

    protected void setMarsAccessions(
            final ReceiptAccessionsMap studiesAccessionsMap,
            final ReceiptAccessionsMap samplesAccessionsMap,
            final ReceiptAccessionsMap sourcesAccessionsMap,
            final ReceiptAccessionsMap otherMaterialsAccessionsMap,
            final ReceiptAccessionsMap dataFilesAccessionsMap,
            final IsaJson isaJson) {
        Optional.ofNullable(isaJson.studies)
                .orElse(new ArrayList<>())
                .forEach(
                        study -> {
                            if (studiesAccessionsMap != null) {
                                if (studiesAccessionsMap.isaItemName.equals(Assay.Fields.id)) {
                                    Optional.ofNullable(study.assays)
                                            .orElse(new ArrayList<>())
                                            .forEach(
                                                    assay -> {
                                                        ReceiptAccessionMap assayAccessionMap =
                                                                getAccessionMapEntry(studiesAccessionsMap, assay, currentState().message);
                                                        if (assayAccessionMap.accession != null) {
                                                            currentState().accessions.add(
                                                                    getAssayStudyMarsAccession(
                                                                            Map.entry(Study.Fields.title, study.title),
                                                                            assayAccessionMap));
                                                        }
                                                    });
                                } else {
                                    ReceiptAccessionMap studyAccessionMap =
                                            getAccessionMapEntry(studiesAccessionsMap, study, currentState().message);
                                    if (studyAccessionMap.accession != null) {
                                        currentState().accessions.add(getStudyMarsAccession(studyAccessionMap));
                                    }
                                }
                                ReceiptAccessionMap studyAccessionMap = new ReceiptAccessionMap();
                                studyAccessionMap.isaFieldKey = Study.Fields.title;
                                studyAccessionMap.isaFieldValue = study.title;
                                if (samplesAccessionsMap != null && study.materials != null) {
                                    Optional.ofNullable(study.materials.samples)
                                            .orElse(new ArrayList<>())
                                            .forEach(
                                                    sample -> {
                                                        ReceiptAccessionMap sampleAccessionMap = getAccessionMapEntry(
                                                                samplesAccessionsMap, sample, currentState().message);
                                                        if (sampleAccessionMap.accession != null) {
                                                            currentState().accessions.add(getSampleMarsAccession(studyAccessionMap, sampleAccessionMap));
                                                        }
                                                    });
                                }
                                if (sourcesAccessionsMap != null && study.materials != null) {
                                    Optional.ofNullable(study.materials.sources)
                                            .orElse(new ArrayList<>())
                                            .forEach(
                                                    source -> {
                                                        ReceiptAccessionMap sourceAccessionMap = getAccessionMapEntry(
                                                                sourcesAccessionsMap, source, currentState().message);
                                                        if (sourceAccessionMap.accession != null) {
                                                            currentState().accessions.add(getSourceMarsAccession(studyAccessionMap, sourceAccessionMap));
                                                        }
                                                    });
                                }
                                if (otherMaterialsAccessionsMap != null || dataFilesAccessionsMap != null) {
                                    Optional.ofNullable(study.assays)
                                            .orElse(new ArrayList<>())
                                            .forEach(
                                                    assay -> {
                                                        if (otherMaterialsAccessionsMap != null && assay.materials != null) {
                                                            Optional.ofNullable(assay.materials.otherMaterials)
                                                                    .orElse(new ArrayList<>())
                                                                    .forEach(
                                                                            otherMaterial -> {
                                                                                ReceiptAccessionMap otherMaterialAccessionMap = getAccessionMapEntry(
                                                                                        otherMaterialsAccessionsMap, otherMaterial, currentState().message);
                                                                                if (otherMaterialAccessionMap.accession != null) {
                                                                                    currentState().accessions.add(getOtherMaterialMarsAccession(
                                                                                            studyAccessionMap,
                                                                                            assay.id,
                                                                                            otherMaterialAccessionMap));
                                                                                }
                                                                            });
                                                        }
                                                        if (dataFilesAccessionsMap != null) {
                                                            Optional.ofNullable(assay.dataFiles)
                                                                    .orElse(new ArrayList<>())
                                                                    .forEach(
                                                                            dataFile -> {
                                                                                ReceiptAccessionMap dataFileAccessionMap = getAccessionMapEntry(
                                                                                        dataFilesAccessionsMap, dataFile, currentState().message);
                                                                                if (dataFileAccessionMap.accession != null) {
                                                                                    currentState().accessions.add(getDataFileMarsAccession(
                                                                                            studyAccessionMap,
                                                                                            assay.id,
                                                                                            dataFileAccessionMap));
                                                                                }
                                                                            });
                                                        }
                                                    });
                                }
                            }
                        });
    }

    // -------------------------
    // | Making Mars item path |
    // -------------------------

    public MarsPath[] getStudyMarsPath(final Entry<String, String> isaStudyKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(
                                MarsWhere.builder()
                                        .key(isaStudyKeyValue.getKey())
                                        .value(isaStudyKeyValue.getValue())
                                        .build())
                        .build()
        };
    }

    public MarsPath[] getAssayMarsPath(
            final Entry<String, String> isaStudyKeyValue,
            final Entry<String, String> isaAssayKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(
                                MarsWhere.builder()
                                        .key(isaStudyKeyValue.getKey())
                                        .value(isaStudyKeyValue.getValue())
                                        .build())
                        .build(),
                MarsPath.builder()
                        .key("assays")
                        .where(
                                MarsWhere.builder()
                                        .key(isaAssayKeyValue.getKey())
                                        .value(isaAssayKeyValue.getValue())
                                        .build())
                        .build()
        };
    }

    public MarsPath[] getSampleMarsPath(
            final Entry<String, String> isaStudyKeyValue,
            final Entry<String, String> isaSampleKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(MarsWhere.builder()
                                .key(isaStudyKeyValue.getKey())
                                .value(isaStudyKeyValue.getValue())
                                .build())
                        .build(),
                MarsPath.builder().key("materials").build(),
                MarsPath.builder()
                        .key("samples")
                        .where(MarsWhere.builder()
                                .key(isaSampleKeyValue.getKey())
                                .value(isaSampleKeyValue.getValue())
                                .build())
                        .build(),
        };
    }

    public MarsPath[] getSourceMarsPath(
            final Entry<String, String> isaStudyKeyValue,
            final Entry<String, String> isaSourceKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(MarsWhere.builder()
                                .key(isaStudyKeyValue.getKey())
                                .value(isaStudyKeyValue.getValue())
                                .build())
                        .build(),
                MarsPath.builder().key("materials").build(),
                MarsPath.builder()
                        .key("sources")
                        .where(MarsWhere.builder()
                                .key(isaSourceKeyValue.getKey())
                                .value(isaSourceKeyValue.getValue())
                                .build())
                        .build()
        };
    }

    public MarsPath[] getOtherMaterialMarsPath(
            final Entry<String, String> isaStudyKeyValue,
            final String assayId,
            final Entry<String, String> isaOtherMaterialKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(MarsWhere.builder()
                                .key(isaStudyKeyValue.getKey())
                                .value(isaStudyKeyValue.getValue())
                                .build())
                        .build(),
                MarsPath.builder()
                        .key("assays")
                        .where(MarsWhere.builder()
                                .key("@id")
                                .value(assayId)
                                .build())
                        .build(),
                MarsPath.builder().key("materials").build(),
                MarsPath.builder()
                        .key("otherMaterials")
                        .where(MarsWhere.builder()
                                .key(isaOtherMaterialKeyValue.getKey())
                                .value(isaOtherMaterialKeyValue.getValue())
                                .build())
                        .build()
        };
    }

    public MarsPath[] getDataFileMarsPath(
            final Entry<String, String> isaStudyKeyValue,
            final String assayId,
            final Entry<String, String> isaDataFileKeyValue) {
        return new MarsPath[]{
                MarsPath.builder()
                        .key("studies")
                        .where(MarsWhere.builder()
                                .key(isaStudyKeyValue.getKey())
                                .value(isaStudyKeyValue.getValue())
                                .build())
                        .build(),
                MarsPath.builder()
                        .key("assays")
                        .where(MarsWhere.builder()
                                .key("@id")
                                .value(assayId)
                                .build())
                        .build(),
                MarsPath.builder()
                        .key("dataFiles")
                        .where(MarsWhere.builder()
                                .key(isaDataFileKeyValue.getKey())
                                .value(isaDataFileKeyValue.getValue())
                                .build())
                        .build()
        };
    }

    // ---------------------------------
    // | Making Mars accession objects |
    // ---------------------------------

    protected MarsAccession getStudyMarsAccession(final ReceiptAccessionMap studyAccessionMap) {
        return MarsAccession.builder()
                .path(List.of(getStudyMarsPath(Map.entry(studyAccessionMap.isaFieldKey, studyAccessionMap.isaFieldValue))))
                .value(studyAccessionMap.accession)
                .build();
    }

    protected MarsAccession getSampleMarsAccession(
            final ReceiptAccessionMap studyAccessionMap,
            final ReceiptAccessionMap sampleAccessionMap) {
        return MarsAccession.builder()
                .path(List.of(getSampleMarsPath(Map.entry(studyAccessionMap.isaFieldKey, studyAccessionMap.isaFieldValue),
                        Map.entry(sampleAccessionMap.isaFieldKey, sampleAccessionMap.isaFieldValue))))
                .value(sampleAccessionMap.accession)
                .build();
    }

    protected MarsAccession getAssayStudyMarsAccession(
            final Entry<String, String> isaStudyKeyValue,
            final ReceiptAccessionMap assayAccessionMap) {
        return MarsAccession.builder()
                .path(
                        List.of(
                                getAssayMarsPath(
                                        isaStudyKeyValue,
                                        Map.entry(assayAccessionMap.isaFieldKey, assayAccessionMap.isaFieldValue))))
                .value(assayAccessionMap.accession)
                .build();
    }

    protected MarsAccession getSourceMarsAccession(
            final ReceiptAccessionMap studyAccessionMap,
            final ReceiptAccessionMap sourceAccessionMap) {
        return MarsAccession.builder()
                .path(List.of(getSourceMarsPath(Map.entry(studyAccessionMap.isaFieldKey, studyAccessionMap.isaFieldValue),
                        Map.entry(sourceAccessionMap.isaFieldKey, sourceAccessionMap.isaFieldValue))))
                .value(sourceAccessionMap.accession)
                .build();
    }

    protected MarsAccession getOtherMaterialMarsAccession(
            final ReceiptAccessionMap studyAccessionMap,
            final String assayId,
            final ReceiptAccessionMap otherMaterialAccessionMap) {
        return MarsAccession.builder()
                .path(List.of(getOtherMaterialMarsPath(
                        Map.entry(studyAccessionMap.isaFieldKey, studyAccessionMap.isaFieldValue),
                        assayId,
                        Map.entry(otherMaterialAccessionMap.isaFieldKey, otherMaterialAccessionMap.isaFieldValue))))
                .value(otherMaterialAccessionMap.accession)
                .build();
    }

    protected MarsAccession getDataFileMarsAccession(
            final ReceiptAccessionMap studyAccessionMap,
            final String assayId,
            final ReceiptAccessionMap dataFileAccessionMap) {
        return MarsAccession.builder()
                .path(List.of(getDataFileMarsPath(Map.entry(studyAccessionMap.isaFieldKey, studyAccessionMap.isaFieldValue),
                        assayId,
                        Map.entry(dataFileAccessionMap.isaFieldKey, dataFileAccessionMap.isaFieldValue))))
                .value(dataFileAccessionMap.accession)
                .build();
    }

    // ---------------------------------

    private <T> ReceiptAccessionMap getAccessionMapEntry(
            final ReceiptAccessionsMap accessionsMap,
            final T item,
            final MarsMessage marsMessage) {
        try {
            return new ReceiptAccessionMap() {
                {
                    Field field = item.getClass().getField(accessionsMap.isaItemName);
                    if (field.isAnnotationPresent(JsonProperty.class)) {
                        isaFieldKey = field.getAnnotation(JsonProperty.class).value();
                    } else {
                        isaFieldKey = accessionsMap.isaItemName;
                    }
                    isaItemName = accessionsMap.isaItemName;
                    isaFieldValue = field.get(item).toString();
                    accession = accessionsMap.accessionMap.get(isaFieldValue);
                }
            };
        } catch (NoSuchFieldException | IllegalAccessException e) {
            currentState().message.errors
                    .add(
                            MarsError.builder()
                                    .message(
                                            String.format("Cannot find an item of %s with the key %s in the ISA-JSON input",
                                                    item.getClass().getSimpleName(),
                                                    accessionsMap.isaItemName))
                                    .type(MarsErrorType.INVALID_METADATA)
                                    .build());
            return new ReceiptAccessionMap();
        }
    }
}
