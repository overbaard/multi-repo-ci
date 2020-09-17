package org.overbaard.ci.multi.repo.config.repo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.overbaard.ci.multi.repo.config.BaseParser;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RepoConfigParser extends BaseParser {
    private final Path yamlFile;

    private RepoConfigParser(Path yamlFile) {
        this.yamlFile = yamlFile;
    }

    public static RepoConfigParser create(Path yamlFile) {
        return new RepoConfigParser(yamlFile);
    }

    public RepoConfig parse() throws Exception {
        if (Files.exists(yamlFile)) {
            System.out.println("Parsing repository config: " + yamlFile);
        } else {
            System.err.println("No " + yamlFile + " found. Proceeding without a global repo config");
            return new RepoConfig();
        }
        Map<String, Object> input = null;
        try {
            Yaml yaml = new Yaml();
            input = yaml.load(new BufferedInputStream(new FileInputStream(yamlFile.toFile())));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Object envInput = input.remove("env");
        Object javaVersionInput = input.remove("java-version");
        Object issueReportingInput = input.remove("issue-reporting");
        Object endJobInput = input.remove("end-job");

        if (input.size() > 0) {
            throw new IllegalStateException("Unknown entries: " + input.keySet());
        }

        Map<String, String> env = parseEnv(envInput);
        String javaVersion = parseJavaVersion(javaVersionInput);

        boolean commentsReporting = RepoConfig.DEFAULT_COMMENTS_REPORTING;
        String successLabel = null;
        String failureLabel = null;

        if (issueReportingInput != null) {
            if (!(issueReportingInput instanceof Map)) {
                throw new IllegalStateException("'issue-reporting' must be an object");
            }
            Map<String, Object> issueReporting = (Map<String, Object>) issueReportingInput;
            Object commentsInput = issueReporting.remove("comments");
            Object labelsInput = issueReporting.remove("labels");

            if (input.size() > 0) {
                throw new IllegalStateException("Unknown 'issue-reporting' entries: " + issueReporting.keySet());
            }

            if (commentsInput != null) {
                if (!(commentsInput instanceof Boolean)) {
                    throw new IllegalStateException("'comments' must be either true or false (without quotes)");
                }
                commentsReporting = (Boolean) commentsInput;
            }


            if (labelsInput != null) {
                if (!(labelsInput instanceof Map)) {
                    throw new IllegalStateException("'labels' must be an object");
                }
                Map<String, Object> labels = (Map<String, Object>) labelsInput;
                Object successInput = labels.remove("success");
                Object failureInput = labels.remove("failure");
                if (labels.size() > 0) {
                    throw new IllegalStateException("Unknown 'labels' entries: " + issueReporting.keySet());
                }
                if (successInput == null) {
                    throw new IllegalStateException("Missing 'success' entry for 'labels'");
                }
                if (failureInput == null) {
                    throw new IllegalStateException("Missing 'failure' entry for 'labels'");
                }
                if (!(successInput instanceof String)) {
                    throw new IllegalStateException("'success' must be a string");
                }
                if (!(failureInput instanceof String)) {
                    throw new IllegalStateException("'failure' must be a string");
                }
                successLabel = (String) successInput;
                failureLabel = (String) failureInput;
            }
        }

        Map<String, Object> endJob = parseEndJob(endJobInput);

        return new RepoConfig(env, javaVersion, commentsReporting, successLabel, failureLabel, endJob);
    }

    private Map<String, Object> parseEndJob(Object input) {
        if (input == null) {
            return null;
        }
        if (!(input instanceof Map)) {
            throw new IllegalStateException("end-job must be an object");
        }
        Map<String, Object> endJob = (Map<String, Object>) input;
        if (endJob.get("name") != null) {
            throw new IllegalStateException("end-job should not have 'name'");
        }
        if (endJob.get("runs-on") != null) {
            throw new IllegalStateException("end-job should not have 'runs-on'");
        }
        if (endJob.get("needs") != null) {
            throw new IllegalStateException("end-job should not have 'needs'");
        }
        if (endJob.get("env") == null) {
            endJob.put("env", new LinkedHashMap<>());
        }
        if (endJob.get("steps") == null) {
            throw new IllegalStateException("end-job should have a 'steps'");
        }
        Object steps = endJob.get("steps");
        if (!(steps instanceof List)) {
            throw new IllegalStateException("end-job 'steps' should be a list");
        }
        return endJob;
    }
}
