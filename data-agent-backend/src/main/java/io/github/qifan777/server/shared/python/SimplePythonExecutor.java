package io.github.qifan777.server.shared.python;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SimplePythonExecutor {

    private static final String DEFAULT_DOCKER_IMAGE = "continuumio/anaconda3:latest";
    private static final String DEFAULT_MEMORY_LIMIT = "512m";

    private SimplePythonExecutor() {
    }

    public static PythonExecutionResult execute(String pythonCode, String inputJson) {
        return execute(pythonCode, inputJson, 60);
    }

    public static PythonExecutionResult execute(String pythonCode, String inputJson, long timeoutSec) {
        File workDir = null;
        try {
            workDir = Files.createTempDirectory("ai_python_exec_").toFile();
            File scriptFile = new File(workDir, "script.py");
            File dataFile = new File(workDir, "input.json");
            Files.writeString(scriptFile.toPath(), pythonCode, StandardCharsets.UTF_8);
            Files.writeString(dataFile.toPath(), inputJson, StandardCharsets.UTF_8);

            if (!isDockerAvailable()) {
                return new PythonExecutionResult(
                        false,
                        "",
                        "Docker is required for Python sandbox execution but was not found. Please install/start Docker."
                );
            }

            String configuredImage = System.getenv("DATA_AGENT_PYTHON_DOCKER_IMAGE");
            String image = configuredImage == null || configuredImage.trim().isEmpty()
                    ? DEFAULT_DOCKER_IMAGE
                    : configuredImage.trim();
            String configuredMemory = System.getenv("DATA_AGENT_PYTHON_MEMORY_LIMIT");
            String memoryLimit = configuredMemory == null || configuredMemory.trim().isEmpty()
                    ? DEFAULT_MEMORY_LIMIT
                    : configuredMemory.trim();

            PythonExecutionResult dockerResult = runCommand(
                    List.of(
                            "docker", "run", "--rm", "-i",
                            "--network", "none",
                            "--cpus", "1",
                            "--memory", memoryLimit,
                            "--pids-limit", "128",
                            "--security-opt", "no-new-privileges",
                            "-v", workDir.getAbsolutePath() + ":/work:ro",
                            "-w", "/work",
                            image,
                            "python",
                            "/work/script.py"
                    ),
                    dataFile,
                    timeoutSec
            );

            if (dockerResult.isSuccess()) {
                return dockerResult;
            }
            return new PythonExecutionResult(
                    false,
                    dockerResult.getOutput(),
                    dockerResult.getError() + "\n\nSandbox image: " + image
            );
        } catch (Exception e) {
            return new PythonExecutionResult(false, "", e.getMessage() == null ? "Unknown error occurred" : e.getMessage());
        } finally {
            deleteRecursively(workDir);
        }
    }

    private static PythonExecutionResult runCommand(List<String> command, File inputFile, long timeoutSec) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectInput(inputFile)
                .start();

        boolean completed = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            return new PythonExecutionResult(false, "", "Execution timed out after " + timeoutSec + " seconds.");
        }

        int exitCode = process.exitValue();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        return new PythonExecutionResult(exitCode == 0, stdout, stderr);
    }

    private static boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
