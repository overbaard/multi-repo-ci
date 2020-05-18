package org.overbaard.ci.multi.repo;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Main {

    private static final String ARG_WORKFLOW_DIR = "--workflow-dir";
    private static final String ARG_YAML = "--yaml";
    private static final String ARG_ISSUE = "--issue";
    private static final String ARG_BRANCH = "--branch";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting cross-component job generation");
        Path yamlConfig = null;
        String branchName = null;
        String issueNumber = null;
        Path workflowDir = null;
        for (String arg : args) {
            try {
                if (arg.startsWith(ARG_WORKFLOW_DIR)) {
                    String val = arg.substring(ARG_WORKFLOW_DIR.length() + 1);
                    workflowDir = Paths.get(val);
                    if (!Files.exists(workflowDir) || !Files.isDirectory(workflowDir)) {
                        System.err.println(workflowDir + " is not a directory");
                        usage();
                        System.exit(1);
                    }

                } else if (arg.startsWith(ARG_YAML)) {
                    String val = arg.substring(ARG_YAML.length() + 1);
                    yamlConfig = Paths.get(val);
                    if (!Files.exists(yamlConfig) || Files.isDirectory(yamlConfig)) {
                        System.err.println(yamlConfig + " is not a file");
                        usage();
                        System.exit(1);
                    }
                } else if (arg.startsWith(ARG_ISSUE)) {
                    issueNumber = arg.substring(ARG_ISSUE.length() + 1);
                } else if (arg.startsWith(ARG_BRANCH)) {
                    branchName = arg.substring(ARG_BRANCH.length() + 1);
                } else {
                    System.err.println("Unknown argument " + arg);
                    usage();
                    System.exit(1);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Argument " + arg + " expects a value");
            }
        }

        if (workflowDir == null || yamlConfig == null || branchName == null || issueNumber == null) {
            if (workflowDir == null) {
                System.err.println(ARG_WORKFLOW_DIR + " was not specified!");
            }
            if (yamlConfig == null) {
                System.err.println(ARG_YAML + " was not specified!");
            }
            if (issueNumber == null) {
                System.err.println(ARG_ISSUE + " was not specified!");
            }
            if (branchName == null) {
                System.err.println(ARG_BRANCH + " was not specified!");
            }
            usage();
            System.exit(1);
        }

        GitHubActionGenerator.create(workflowDir, yamlConfig, branchName, issueNumber).generate();
    }


    private static void usage() throws URISyntaxException {
        Usage usage = new Usage();
        URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();

        usage.addArguments(ARG_WORKFLOW_DIR + "=<file>");
        usage.addInstruction("File system path to directory to output the created workflow yaml");

        usage.addArguments(ARG_YAML + "=<file>");
        usage.addInstruction("File system path to file containing the configuration for the created job");

        usage.addArguments(ARG_ISSUE + "=<issue number>");
        usage.addInstruction("The number of the issue that triggered this job");

        usage.addArguments(ARG_BRANCH + "=<branch name>");
        usage.addInstruction("The branch that is used to trigger the workflow");

        String headline = usage.getUsageHeadline(url);
        System.out.print(usage.usage(headline));
    }
}
