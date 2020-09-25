package org.overbaard.ci.multi.repo.config.repo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        List<String> runsOn = parseRunsOn(input.remove("runs-on"));
        if (runsOn == null) {
            runsOn = RepoConfig.DEFAULT_RUNS_ON;
        }

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

        Map<String, Object> endJob = preParseEndJob(endJobInput);
        endJob.put("env", mergeEnv(env, (Map<String, String>)endJob.get("env")));
        // Understand this better
//        if (endJob.get("java-version") == null) {
//            endJob.put("java-version", javaVersion);
//        }
        if (endJob.get("runs-on") == null) {
            List<String> ro = runsOn != null ? runsOn : RepoConfig.DEFAULT_RUNS_ON;
            endJob.put("runs-on", ro);
        }

        return new RepoConfig(env, javaVersion, runsOn, commentsReporting, successLabel, failureLabel, endJob);
    }
}
