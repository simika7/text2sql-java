package io.github.qifan777.server.integration.a2a;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.InternalError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.github.qifan777.server.agent.DataAgentSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GraphAgentExecutor implements AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(GraphAgentExecutor.class);

    private final StateGraph stateGraph;
    private final TaskStore taskStore;
    private final MemorySaver saver = new MemorySaver();

    public GraphAgentExecutor(StateGraph stateGraph, TaskStore taskStore) {
        this.stateGraph = stateGraph;
        this.taskStore = taskStore;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        AtomicInteger artifactNum = new AtomicInteger();
        AtomicBoolean workStarted = new AtomicBoolean(false);

        try {
            CompiledGraph compiledGraph = stateGraph.compile(CompileConfig.builder()
                    .saverConfig(SaverConfig.builder().register(saver).build())
                    .interruptBefore(DataAgentSpec.Graph.Node.INTERRUPT_NODE)
                    .build());

            Message message = context.getMessage();
            Task existingTask = existingTask(message);
            if (existingTask != null) {
                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(existingTask.getId())
                        .build();
                Map<String, Object> stateUpdate = new HashMap<>();
                stateUpdate.put(
                        DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED,
                        metadataValue(message, DataAgentSpec.MessageMetadataKey.CONFIRMATION_APPROVED)
                );
                stateUpdate.put(
                        DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK,
                        metadataValue(message, DataAgentSpec.MessageMetadataKey.CONFIRMATION_FEEDBACK)
                );
                RunnableConfig resumedConfig = compiledGraph.updateState(
                        runnableConfig,
                        stateUpdate
                );
                taskUpdater.startWork();
                workStarted.set(true);
                compiledGraph.stream(null, resumedConfig)
                        .doOnNext(nodeOutput -> handleNodeOutput(taskUpdater, artifactNum, nodeOutput))
                        .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                        .blockLast();
                return;
            }

            Task newTask = newTask(context, message);
            if (eventQueue != null) {
                eventQueue.enqueueEvent(newTask);
            }

            String input = firstText(message);
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(newTask.getId())
                    .build();
            Map<String, Object> graphInput = new HashMap<>();
            graphInput.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, input);
            graphInput.put(
                    DataAgentSpec.Graph.StateKey.Input.DATABASE_ID,
                    metadataValue(message, DataAgentSpec.MessageMetadataKey.DATABASE_ID)
            );

            taskUpdater.startWork();
            workStarted.set(true);
            compiledGraph.stream(graphInput, runnableConfig)
                    .doOnNext(nodeOutput -> handleNodeOutput(taskUpdater, artifactNum, nodeOutput))
                    .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                    .blockLast();
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (workStarted.get()) {
                taskUpdater.fail();
            }
            log.error("Graph execution failed", exception);
            throw new InternalError(exception.getMessage());
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
    }

    private Task existingTask(Message message) {
        String taskId = message.getTaskId();
        if (taskId == null || taskId.isEmpty()) {
            return null;
        }
        return taskStore.get(taskId);
    }

    private void handleNodeOutput(TaskUpdater taskUpdater, AtomicInteger artifactNum, NodeOutput nodeOutput) {
        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
            if (streamingOutput.getOutputType() == OutputType.GRAPH_NODE_STREAMING) {
                org.springframework.ai.chat.messages.Message message = streamingOutput.message();
                if (message != null) {
                    taskUpdater.addArtifact(
                            List.of(new TextPart(message.getText())),
                            String.valueOf(artifactNum.incrementAndGet()),
                            streamingOutput.node(),
                            Map.of("outputType", streamingOutput.getOutputType())
                    );
                }
                return;
            }
            if (streamingOutput.getOutputType() == OutputType.GRAPH_NODE_FINISHED) {
                taskUpdater.addArtifact(
                        List.of(new DataPart(streamingOutput.state().data())),
                        String.valueOf(artifactNum.incrementAndGet()),
                        streamingOutput.node(),
                        Map.of("outputType", streamingOutput.getOutputType())
                );
            }
            return;
        }

        taskUpdater.addArtifact(
                List.of(new DataPart(nodeOutput.state().data())),
                String.valueOf(artifactNum.incrementAndGet()),
                nodeOutput.node(),
                Map.of()
        );
    }

    private Task newTask(RequestContext context, Message request) {
        String contextId = context.getContextId();
        if (contextId == null || contextId.isEmpty()) {
            contextId = request.getContextId();
        }
        if (contextId == null || contextId.isEmpty()) {
            contextId = UUID.randomUUID().toString();
        }

        String id = context.getTaskId();
        if (id == null || id.isEmpty()) {
            id = request.getTaskId();
        }
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        return new Task(id, contextId, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }

    private void onComplete(CompiledGraph compiledGraph, TaskUpdater taskUpdater, RunnableConfig runnableConfig) {
        String nextNode = compiledGraph.stateOf(runnableConfig)
                .map(stateSnapshot -> stateSnapshot.next())
                .orElse(null);
        if (Objects.equals(nextNode, DataAgentSpec.Graph.Node.INTERRUPT_NODE)) {
            taskUpdater.requiresInput();
            return;
        }
        taskUpdater.complete();
    }

    private Object metadataValue(Message message, String key) {
        Map<String, Object> metadata = message.getMetadata();
        return metadata == null ? null : metadata.get(key);
    }

    private String firstText(Message message) {
        List<?> parts = message.getParts() == null ? List.of() : message.getParts();
        return parts.stream()
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::getText)
                .findFirst()
                .orElse("");
    }
}
