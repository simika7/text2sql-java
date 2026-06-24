package io.github.qifan777.server.agent.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptManagerTest {

    @Test
    void constructsPromptTemplatesForAllKotlinPropertyNames() {
        PromptManager promptManager = new PromptManager(
                resource("intent {name}"),
                resource("rewrite {name}"),
                resource("agent {name}"),
                resource("enhance {name}"),
                resource("feasible {name}"),
                resource("mix {name}"),
                resource("semantic {name}"),
                resource("sql {name}"),
                resource("planner {name}"),
                resource("report {name}"),
                resource("fix {name}"),
                resource("python {name}"),
                resource("analyze {name}"),
                resource("business {name}"),
                resource("model {name}"),
                resource("view {name}")
        );

        assertThat(promptManager.intentRecognitionPromptTemplate.render(Map.of("name", "ok")))
                .isEqualTo("intent ok");
        assertThat(promptManager.getEvidenceQueryRewritePromptTemplate()).isNotNull();
        assertThat(promptManager.getAgentKnowledgePromptTemplate()).isNotNull();
        assertThat(promptManager.getQueryEnhancementPromptTemplate()).isNotNull();
        assertThat(promptManager.getFeasibilityAssessmentPromptTemplate()).isNotNull();
        assertThat(promptManager.getMixSelectorPromptTemplate()).isNotNull();
        assertThat(promptManager.getSemanticConsistencyPromptTemplate()).isNotNull();
        assertThat(promptManager.getNewSqlGeneratorPromptTemplate()).isNotNull();
        assertThat(promptManager.getPlannerPromptTemplate()).isNotNull();
        assertThat(promptManager.getReportGeneratorPlainPromptTemplate()).isNotNull();
        assertThat(promptManager.getSqlErrorFixerPromptTemplate()).isNotNull();
        assertThat(promptManager.getPythonGeneratorPromptTemplate()).isNotNull();
        assertThat(promptManager.getPythonAnalyzePromptTemplate()).isNotNull();
        assertThat(promptManager.getBusinessKnowledgePromptTemplate()).isNotNull();
        assertThat(promptManager.getSemanticModelPromptTemplate()).isNotNull();
        assertThat(promptManager.getDataViewAnalyzePromptTemplate()).isNotNull();
    }

    private Resource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
