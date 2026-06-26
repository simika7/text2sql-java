package io.github.qifan777.server.integration.a2a;

import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class A2AJavaMigrationTest {

    @Test
    void createsJsonRpcAgentCardWithStreamingCapabilities() {
        A2AConfiguration configuration = new A2AConfiguration();

        AgentCard agentCard = configuration.agentCard();

        assertThat(agentCard.name()).isEqualTo("sqlAgent");
        assertThat(agentCard.url()).isEqualTo("http://localhost:3500/api/a2a/jsonrpc");
        assertThat(agentCard.defaultInputModes()).containsExactly("text/plain");
        assertThat(agentCard.defaultOutputModes()).containsExactly("text/plain");
        assertThat(agentCard.capabilities().streaming()).isTrue();
        assertThat(agentCard.capabilities().pushNotifications()).isTrue();
        assertThat(agentCard.capabilities().stateTransitionHistory()).isTrue();
        assertThat(agentCard.skills()).singleElement()
                .satisfies(skill -> {
                    assertThat(skill.id()).isEqualTo("sql");
                    assertThat(skill.tags()).containsExactly("sql", "query");
                });
        assertThat(agentCard.additionalInterfaces()).singleElement()
                .satisfies(agentInterface -> {
                    assertThat(agentInterface.transport()).isEqualTo("JSONRPC");
                    assertThat(agentInterface.url()).isEqualTo("http://localhost:3500/api/a2a/jsonrpc");
                });
    }

    @Test
    void exposesJsonRpcTransportMetadataWithJavaControllerAnnotations() {
        JSONRPCTransportMetadata metadata = new JSONRPCTransportMetadata();

        assertThat(metadata.getTransportProtocol()).isEqualTo(TransportProtocol.JSONRPC.toString());
        Set<String> controllerAnnotations = Arrays.stream(A2AController.class.getDeclaredAnnotations())
                .map(annotation -> annotation.annotationType().getName())
                .collect(Collectors.toSet());
        assertThat(controllerAnnotations)
                .contains("org.springframework.web.bind.annotation.RestController");
        assertThat(GraphAgentExecutor.class.getSimpleName()).isEqualTo("GraphAgentExecutor");
    }
}
