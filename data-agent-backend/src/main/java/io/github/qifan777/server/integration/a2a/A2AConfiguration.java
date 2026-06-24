package io.github.qifan777.server.integration.a2a;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class A2AConfiguration {

    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("sqlAgent")
                .description("Professional SQL generation Agent")
                .defaultInputModes(List.of("text/plain"))
                .defaultOutputModes(List.of("text/plain"))
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .skills(List.of(new AgentSkill.Builder()
                        .id("sql")
                        .name("sql generator")
                        .description("Generate a SQL query")
                        .tags(List.of("sql", "query"))
                        .build()))
                .url("http://localhost:3500/api/a2a/jsonrpc")
                .additionalInterfaces(List.of(new AgentInterface("JSONRPC", "http://localhost:3500/api/a2a/jsonrpc")))
                .version("1.0")
                .build();
    }

    @Bean
    public InMemoryTaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    public InMemoryPushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    public PushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
        return new BasePushNotificationSender(pushNotificationConfigStore);
    }

    @Bean
    public QueueManager queueManager(InMemoryTaskStore taskStore) {
        return new InMemoryQueueManager(taskStore);
    }

    @Bean
    public Integer agentCompletionTimeoutSeconds(
            @Value("${a2a.blocking.agent.timeout.seconds:30}") int timeout
    ) {
        return timeout;
    }

    @Bean
    public JSONRPCHandler jsonRpcHandler(
            AgentCard agentCard,
            AgentExecutor agentExecutor,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            PushNotificationSender pushNotificationSender
    ) {
        ExecutorService pool = Executors.newFixedThreadPool(5);
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                agentExecutor,
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                pushNotificationSender,
                pool
        );
        return new JSONRPCHandler(agentCard, requestHandler, pool);
    }
}
