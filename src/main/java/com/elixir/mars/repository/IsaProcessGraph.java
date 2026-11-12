package com.elixir.mars.repository;

import com.elixir.mars.repository.models.isa.Input;
import com.elixir.mars.repository.models.isa.Output;
import com.elixir.mars.repository.models.isa.ProcessSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index of the {@code previousProcess}/{@code nextProcess} links within one ISA process sequence.
 *
 * <p>The ISA process schema allows consecutive protocols to be chained without an intermediate
 * material, e.g. {@code Source -> growth culture -> sample collection -> Sample} where only the
 * first process lists the source as input and only the last lists the sample as output. The
 * processes in between are connected through {@code previousProcess}/{@code nextProcess} only.
 * {@link IsaJsonGraphLookup} builds this index when a process declares no inputs or outputs.
 *
 * <p>Links are honoured in both directions, so a chain where only one side sets the reference
 * still resolves. Processes are keyed by identity because the ISA model classes derive {@code
 * equals}/{@code hashCode} from their full content.
 */
final class IsaProcessGraph {
    private final Map<ProcessSequence, List<ProcessSequence>> predecessors = new IdentityHashMap<>();
    private final Map<ProcessSequence, List<ProcessSequence>> successors = new IdentityHashMap<>();

    IsaProcessGraph(final List<ProcessSequence> processSequence) {
        final Map<String, ProcessSequence> processesById = new HashMap<>();
        for (final ProcessSequence process : processSequence) {
            if (process != null && process.getId() != null) {
                processesById.putIfAbsent(process.getId(), process);
            }
        }

        for (final ProcessSequence process : processSequence) {
            if (process == null) {
                continue;
            }
            if (process.getPreviousProcess() != null) {
                link(processesById.get(process.getPreviousProcess().getId()), process);
            }
            if (process.getNextProcess() != null) {
                link(process, processesById.get(process.getNextProcess().getId()));
            }
        }
    }

    /**
     * Returns the declared inputs of a process or, when it declares none, the items resolved by
     * walking back through {@code previousProcess} links: the outputs of the preceding process, or
     * its inputs when it declares no outputs either.
     */
    List<String> getInputIds(final ProcessSequence process) {
        final Set<String> inputIds = new LinkedHashSet<>();
        collectInputIds(process, inputIds, newVisitedSet());
        return new ArrayList<>(inputIds);
    }

    /**
     * Returns the declared outputs of a process or, when it declares none, the items resolved by
     * walking forward through {@code nextProcess} links: the inputs of the following process, or
     * its outputs when it declares no inputs either.
     */
    List<String> getOutputIds(final ProcessSequence process) {
        final Set<String> outputIds = new LinkedHashSet<>();
        collectOutputIds(process, outputIds, newVisitedSet());
        return new ArrayList<>(outputIds);
    }

    /**
     * Returns the processes chained directly before a process without an intermediate material,
     * closest first. A preceding process that declares outputs produced a separate material and
     * ends the chain.
     */
    List<ProcessSequence> getUpstreamLinkedProcesses(final ProcessSequence process) {
        final List<ProcessSequence> linkedProcesses = new ArrayList<>();
        collectUpstreamLinkedProcesses(process, linkedProcesses, newVisitedSet());
        return linkedProcesses;
    }

    static List<String> declaredInputIds(final ProcessSequence process) {
        final List<String> ids = new ArrayList<>();
        if (process != null && process.getInputs() != null) {
            for (final Input input : process.getInputs()) {
                if (input != null && input.getId() != null) {
                    ids.add(input.getId());
                }
            }
        }
        return ids;
    }

    static List<String> declaredOutputIds(final ProcessSequence process) {
        final List<String> ids = new ArrayList<>();
        if (process != null && process.getOutputs() != null) {
            for (final Output output : process.getOutputs()) {
                if (output != null && output.getId() != null) {
                    ids.add(output.getId());
                }
            }
        }
        return ids;
    }

    private void collectInputIds(
            final ProcessSequence process, final Set<String> inputIds, final Set<ProcessSequence> visited) {
        if (process == null || !visited.add(process)) {
            return;
        }

        final List<String> declared = declaredInputIds(process);
        if (!declared.isEmpty()) {
            inputIds.addAll(declared);
            return;
        }

        for (final ProcessSequence predecessor : getPredecessors(process)) {
            final List<String> predecessorOutputIds = declaredOutputIds(predecessor);
            if (predecessorOutputIds.isEmpty()) {
                collectInputIds(predecessor, inputIds, visited);
            } else {
                inputIds.addAll(predecessorOutputIds);
            }
        }
    }

    private void collectOutputIds(
            final ProcessSequence process, final Set<String> outputIds, final Set<ProcessSequence> visited) {
        if (process == null || !visited.add(process)) {
            return;
        }

        final List<String> declared = declaredOutputIds(process);
        if (!declared.isEmpty()) {
            outputIds.addAll(declared);
            return;
        }

        for (final ProcessSequence successor : getSuccessors(process)) {
            final List<String> successorInputIds = declaredInputIds(successor);
            if (successorInputIds.isEmpty()) {
                collectOutputIds(successor, outputIds, visited);
            } else {
                outputIds.addAll(successorInputIds);
            }
        }
    }

    private void collectUpstreamLinkedProcesses(
            final ProcessSequence process,
            final List<ProcessSequence> linkedProcesses,
            final Set<ProcessSequence> visited) {
        if (process == null || !visited.add(process) || !declaredInputIds(process).isEmpty()) {
            return;
        }

        for (final ProcessSequence predecessor : getPredecessors(process)) {
            if (declaredOutputIds(predecessor).isEmpty() && !visited.contains(predecessor)) {
                linkedProcesses.add(predecessor);
                collectUpstreamLinkedProcesses(predecessor, linkedProcesses, visited);
            }
        }
    }

    private void link(final ProcessSequence previous, final ProcessSequence next) {
        if (previous == null || next == null || previous == next) {
            return;
        }
        addUnique(successors, previous, next);
        addUnique(predecessors, next, previous);
    }

    private List<ProcessSequence> getPredecessors(final ProcessSequence process) {
        return predecessors.getOrDefault(process, Collections.emptyList());
    }

    private List<ProcessSequence> getSuccessors(final ProcessSequence process) {
        return successors.getOrDefault(process, Collections.emptyList());
    }

    private static void addUnique(
            final Map<ProcessSequence, List<ProcessSequence>> links,
            final ProcessSequence key,
            final ProcessSequence value) {
        final List<ProcessSequence> linked = links.computeIfAbsent(key, ignored -> new ArrayList<>());
        for (final ProcessSequence existing : linked) {
            if (existing == value) {
                return;
            }
        }
        linked.add(value);
    }

    private static Set<ProcessSequence> newVisitedSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
