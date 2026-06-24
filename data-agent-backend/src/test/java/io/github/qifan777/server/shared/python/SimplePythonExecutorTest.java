package io.github.qifan777.server.shared.python;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class SimplePythonExecutorTest {

    @Test
    void runCommandConsumesLargeStdoutWhileProcessRuns() throws Exception {
        File inputFile = Files.createTempFile("python-exec-input-", ".json").toFile();
        try {
            PythonExecutionResult result = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    SimplePythonExecutor.runCommand(javaCommand(LargeStdout.class), inputFile, 5)
            );

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOutput()).hasSize(200_000);
            assertThat(result.getError()).isEmpty();
        } finally {
            deleteTempFile(inputFile);
        }
    }

    @Test
    void runCommandDestroysTimedOutProcess() throws Exception {
        File inputFile = Files.createTempFile("python-exec-input-", ".json").toFile();
        try {
            PythonExecutionResult result = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    SimplePythonExecutor.runCommand(javaCommand(Sleeper.class), inputFile, 1)
            );

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isEqualTo("Execution timed out after 1 seconds.");
        } finally {
            deleteTempFile(inputFile);
        }
    }

    private static List<String> javaCommand(Class<?> mainClass) {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        return List.of(java, "-cp", System.getProperty("java.class.path"), mainClass.getName());
    }

    private static void deleteTempFile(File file) throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.deleteIfExists(file.toPath());
                return;
            } catch (java.nio.file.FileSystemException ex) {
                Thread.sleep(100);
            }
        }
        Files.deleteIfExists(file.toPath());
    }

    public static class LargeStdout {
        public static void main(String[] args) {
            System.out.print("x".repeat(200_000));
        }
    }

    public static class Sleeper {
        public static void main(String[] args) throws Exception {
            Thread.sleep(30_000);
        }
    }
}
