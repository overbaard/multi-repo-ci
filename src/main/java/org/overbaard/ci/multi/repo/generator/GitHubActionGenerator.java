package org.overbaard.ci.multi.repo.generator;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.overbaard.ci.multi.repo.Main;
import org.overbaard.ci.multi.repo.ToolCommand;
import org.overbaard.ci.multi.repo.Usage;
import org.overbaard.ci.multi.repo.config.component.BaseComponentJobConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentEndJobConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.component.ComponentJobConfig;
import org.overbaard.ci.multi.repo.config.component.JobRunElementConfig;
import org.overbaard.ci.multi.repo.config.repo.RepoConfig;
import org.overbaard.ci.multi.repo.config.repo.RepoConfigParser;
import org.overbaard.ci.multi.repo.config.trigger.Component;
import org.overbaard.ci.multi.repo.config.trigger.Dependency;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfig;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfigParser;
import org.overbaard.ci.multi.repo.directory.utils.SplitLargeFilesInDirectory;
import org.overbaard.ci.multi.repo.log.copy.CopyLogArtifacts;
import org.overbaard.ci.multi.repo.directory.utils.BackupMavenArtifacts;
import org.overbaard.ci.multi.repo.directory.utils.OverlayBackedUpMavenArtifacts;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitHubActionGenerator {
    public static final String TOKEN_NAME = "secrets.OB_MULTI_CI_PAT";
    public static final String OB_ISSUE_ID_VAR_NAME = "OB_ISSUE_ID";
    public static final String OB_PROJECT_VERSION_VAR_NAME = "OB_PROJECT_VERSION";
    public static final String OB_END_JOB_MAVEN_DEPENDENCY_VERSIONS_VAR_NAME = "OB_MAVEN_DEPENDENCY_VERSIONS";
    public static final String OB_ARTIFACTS_DIRECTORY_VAR_NAME = "OB_ARTIFACTS_DIR";
    public static final String OB_ARTIFACTS_DIRECTORY_NAME = "artifacts";
    public static final String OB_STATUS_VAR_NAME = "OB_STATUS_TEXT";
    public static final String OB_STATUS_RELATIVE_PATH = OB_ARTIFACTS_DIRECTORY_NAME + "/status-text.txt";
    public static final String OB_ISSUE_DATA_JSON_VAR_NAME = "OB_ISSUE_DATA_JSON";
    public static final String OB_ISSUE_DATA_JSON = "issue-data.json";
    final static String STATUS_OUTPUT_JOB_NAME = "ob-ci-read-status-output";
    final static String STATUS_OUTPUT_OUTPUT_VAR_NAME = "status-output";
    final static String STATUS_OUTPUT_OUTPUT_REF = "needs." + STATUS_OUTPUT_JOB_NAME + ".outputs." + STATUS_OUTPUT_OUTPUT_VAR_NAME;

    public final static Path ISSUE_DATA_JSON_PATH = Paths.get(OB_ISSUE_DATA_JSON);


    private static final String ARG_WORKFLOW_DIR = "--workflow-dir";
    private static final String ARG_YAML = "--yaml";
    private static final String ARG_ISSUE = "--issue";
    private static final String ARG_BRANCH = "--branch";

    private static final String REV_PARSE_STEP_ID = "git-rev-parse";
    private static final String REV_PARSE_STEP_OUTPUT = "git-sha";

    private static final String DEFAULT_JAVA_VERSION = "11";

    static final String CI_TOOLS_CHECKOUT_FOLDER = ".ci-tools";
    static final Path REPO_CONFIG_FILE = Paths.get(".repo-config/config.yml");
    static final Path COMPONENT_JOBS_DIR = Paths.get(".repo-config/component-jobs");
    static final String REPO_BACKUPS = "repo-backups";
    static final Path MAVEN_REPO_BACKUPS_ROOT = Paths.get(CI_TOOLS_CHECKOUT_FOLDER + "/" + REPO_BACKUPS);
    final static Path MAVEN_REPO;
    public static final String TOOL_JAR_NAME = "multi-repo-ci-tool.jar";
    public static final String CANCEL_PREVIOUS_RUNS_JOB_NAME = "cancel-previous-runs";

    static {
        if (System.getenv("HOME") == null) {
            throw new IllegalStateException("No HOME env var set!");
        }
        MAVEN_REPO = Paths.get(System.getenv("HOME"), ".m2/repository");
    }




    private final Map<String, Object> workflow = new LinkedHashMap<>();
    private final Map<String, ComponentJobsConfig> componentJobsConfigs = new HashMap<>();
    final Map<String, Object> jobs = new LinkedHashMap<>();
    private final Map<String, String> buildJobNamesByComponent = new LinkedHashMap<>();
    private final Path workflowFile;
    private final Path yamlConfig;
    private final String branchName;
    private final int issueNumber;
    private String jobLogsArtifactName;
    private boolean hasDebugComponents;

    private GitHubActionGenerator(Path workflowFile, Path yamlConfig, String branchName, int issueNumber) {
        this.workflowFile = workflowFile;
        this.yamlConfig = yamlConfig;
        this.branchName = branchName;
        this.issueNumber = issueNumber;
    }

    static void generate(String[] args) throws Exception {
        GitHubActionGenerator generator = create(args);
        generator.generate();
    }

    private static GitHubActionGenerator create(String[] args) throws Exception {
        System.out.println("Starting cross-component job generation");
        Path yamlConfig = null;
        String branchName = null;
        String issueNumberString = null;
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
                    issueNumberString = arg.substring(ARG_ISSUE.length() + 1);
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


        if (workflowDir == null || yamlConfig == null || branchName == null || issueNumberString == null) {
            if (workflowDir == null) {
                System.err.println(ARG_WORKFLOW_DIR + " was not specified!");
            }
            if (yamlConfig == null) {
                System.err.println(ARG_YAML + " was not specified!");
            }
            if (issueNumberString == null) {
                System.err.println(ARG_ISSUE + " was not specified!");
            }
            if (branchName == null) {
                System.err.println(ARG_BRANCH + " was not specified!");
            }
            usage();
            System.exit(1);
        }

        int issueNumber = 0;
        try {
            issueNumber = Integer.parseInt(issueNumberString);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Issue number '" + issueNumberString + "' is not an integer");
        }

        Path workflowFile = workflowDir.resolve("ci-" + issueNumberString + ".yml");
        return new GitHubActionGenerator(workflowFile, yamlConfig, branchName, issueNumber);
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

        String headline = usage.getCommandUsageHeadline(url, GitHubActionGenerator.Command.NAME);
        System.out.print(usage.usage(headline));
    }

    private void generate() throws Exception {
        RepoConfig repoConfig = RepoConfigParser.create(REPO_CONFIG_FILE).parse();
        TriggerConfig triggerConfig = TriggerConfigParser.create(yamlConfig).parse();
        System.out.println("Wil create workflow file at " + workflowFile.toAbsolutePath());

        setupWorkFlowHeaderSection(repoConfig, triggerConfig);
        setupJobs(repoConfig, triggerConfig);

        if (!hasDebugComponents) {
            setupWorkflowEndJob(repoConfig);
            setupReadStatusOutputJob(repoConfig);
            setupStatusReportingJobs(repoConfig, triggerConfig);
            setupCleanupJob(triggerConfig);
        }

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Fix below - additional configuration
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        String output = yaml.dump(workflow);
        System.out.println("-----------------");
        System.out.println(output);
        System.out.println("-----------------");
        Files.write(workflowFile, output.getBytes(StandardCharsets.UTF_8));
    }


    private void setupWorkFlowHeaderSection(RepoConfig repoConfig, TriggerConfig triggerConfig) {
        workflow.put("name", triggerConfig.getName());
        workflow.put("on", Collections.singletonMap("push", Collections.singletonMap("branches", branchName)));

        Map<String, Object> env = new HashMap<>();
        for (String key : repoConfig.getEnv().keySet()) {
            env.put(key, repoConfig.getEnv().get(key));
        }
        env.put(OB_ISSUE_ID_VAR_NAME, issueNumber);
        for (String key : triggerConfig.getEnv().keySet()) {
            Object value = triggerConfig.getEnv().get(key);
            Object existing = env.get(key);
            if (existing != null) {
                System.out.println("Overriding '" + key + "' from repository config with value from issue. " +
                        "Original: " + existing + "; Replacement: " + value);
            }
            env.put(key, triggerConfig.getEnv().get(key));
        }

        if (env.size() > 0) {
            workflow.put("env", env);
        }
    }

    private void setupJobs(RepoConfig repoConfig, TriggerConfig triggerConfig) throws Exception {

        this.jobLogsArtifactName = createJobLogsArtifactName(triggerConfig);

        jobs.put(CANCEL_PREVIOUS_RUNS_JOB_NAME, new CancelPreviousRunsJobBuilder(branchName).build());

        for (Component component : triggerConfig.getComponents()) {
            Path componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yml");
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found");
                componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yaml");
            }
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found. Setting up default job for component: " + component.getName());
                setupDefaultComponentBuildJob(repoConfig, component);
            } else {
                System.out.println("using " + componentJobsFile + " to add job(s) for component: " + component.getName());
                setupComponentBuildJobsFromFile(repoConfig, component, componentJobsFile);
            }
        }
        workflow.put("jobs", jobs);
    }


    private String createJobLogsArtifactName(TriggerConfig triggerConfig) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String timestamp = simpleDateFormat.format(new Date());
        String jobLogsArtifactName = triggerConfig.getName() + "-logs-" + timestamp;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jobLogsArtifactName.length(); i++) {
            char c = jobLogsArtifactName.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            } else if (Character.isDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private void setupDefaultComponentBuildJob(RepoConfig repoConfig, Component component) {
        DefaultComponentJobContext context = new DefaultComponentJobContext(repoConfig, component);
        Map<String, Object> job = setupJob(context);
        jobs.put(getComponentBuildJobId(component.getName()), job);
        if (context.isBuildJob()) {
            buildJobNamesByComponent.put(component.getName(), getComponentBuildJobId(context.getJobName()));
        }

    }

    private void setupComponentBuildJobsFromFile(RepoConfig repoConfig, Component component, Path componentJobsFile) throws Exception {
        ComponentJobsConfig config = ComponentJobsConfigParser.create(componentJobsFile).parse();
        if (component.getMavenOpts() != null) {
            throw new IllegalStateException(component.getName() +
                    " defines mavenOpts but has a component job file at " + componentJobsFile +
                    ". Remove mavenOpts and configure the job in the component job file.");
        }
        componentJobsConfigs.put(component.getName(), config);
        List<ComponentJobConfig> componentJobConfigs = config.getJobs();
        for (ComponentJobConfig componentJobConfig : componentJobConfigs) {
            boolean buildJob = config.getBuildJobName().equals(componentJobConfig.getName());
            if (!component.isDebug() || buildJob) {
                setupComponentBuildJobFromConfig(repoConfig, component, config.getBuildJobName(), componentJobConfig);
            }
        }

        if (config.getEndJob() != null) {
            ConfiguredComponentJobContext context = new ConfiguredComponentJobContext(repoConfig, component, config.getBuildJobName(), config.getEndJob());
            Map<String, Object> job = setupJob(context);
            jobs.put((String)job.get("name"), job);
        }
    }

    private void setupComponentBuildJobFromConfig(RepoConfig repoConfig, Component component, String buildJobName, BaseComponentJobConfig componentJobConfig) {
        ConfiguredComponentJobContext context = new ConfiguredComponentJobContext(repoConfig, component, buildJobName, componentJobConfig);
        Map<String, Object> job = setupJob(context);
        jobs.put(componentJobConfig.getName(), job);
        if (context.isBuildJob()) {
            buildJobNamesByComponent.put(component.getName(), context.getJobName());
        }
    }

    private Map<String, Object> setupJob(ComponentJobContext context) {
        Component component = context.getComponent();

        final String myVersionEnvVarName = getInternalVersionEnvVarName(component.getName());

        String jobName = context.getJobName();

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("name", jobName);
        job.put("runs-on", context.getRunsOn());

        context.addIfClause(job);

        Map<String, String> env = context.createEnv();
        if (env.size() > 0) {
            job.put("env", env);
        }

        List<String> needs = context.createNeeds();
        if (needs.size() > 0) {
            job.put("needs", needs);
        }

        if (context.isBuildJob()) {
            Map<String, String> outputs = new HashMap<>();
            outputs.put(myVersionEnvVarName, "${{steps.grab-version.outputs." + myVersionEnvVarName + "}}");
            outputs.put(REV_PARSE_STEP_OUTPUT, "${{steps." + REV_PARSE_STEP_ID + ".outputs." + REV_PARSE_STEP_OUTPUT + "}}");
            job.put("outputs", outputs);
        }

        List<Object> steps = new ArrayList<>();
        // Get the repo of the component we want to build
        steps.add(
                new CheckoutStepBuilder()
                        .setRepo(component.getOrg(), component.getName())
                        .setBranch(component.getBranch())
                        .build());
        // Get this repo so that we have the tooling contained in this project. We will run various of these later.
        // This is also used for sharing files between jobs
        steps.add(
                new CheckoutStepBuilder()
                        .setPath(CI_TOOLS_CHECKOUT_FOLDER)
                        .setBranch(branchName)
                        .build());
        steps.add(
                new CacheMavenRepoStepBuilder()
                        .build());
        steps.add(
                new SetupJavaStepBuilder()
                        .setVersion(context.getJavaVersion())
                        .build());

        if (context.hasDependencies()) {
            // Get the maven artifact backups
            steps.add(
                    new GitCommandStepBuilder()
                            .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                            .setRebase()
                            .build());

            steps.add(
                    new RunMultiRepoCiToolCommandStepBuilder()
                            .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/" + TOOL_JAR_NAME)
                            .setCommand(OverlayBackedUpMavenArtifacts.Command.NAME)
                            .addArgs(MAVEN_REPO.toString(), MAVEN_REPO_BACKUPS_ROOT.toString())
                            .build());
        }

        if (context.isBuildJob()) {
            steps.add(
                    new GrabProjectVersionStepBuilder()
                            .setComponentName(component.getName())
                            .setEnvVarName(getInternalVersionEnvVarName(component.getName()))
                            .build());
            steps.add(
                new GitRevParseIntoOutputVariableStepBuilder(REV_PARSE_STEP_ID, REV_PARSE_STEP_OUTPUT)
                    .setComponentName(component.getName())
                    .build());
        }

        addIpv6LocalhostHostEntryIfRunningOnGitHub(steps, context.getRunsOn());

        steps.addAll(context.createBuildSteps());

        if (context.getComponent().isDebug()) {
            hasDebugComponents = true;
            steps.add(new TmateDebugStepBuilder().build());
        }

        if (context.isBuildJob()) {
            backupMavenArtifactsProducedByBuild(context, steps);
        }

        // Copy across the build artifacts to the folder and upload the 'root' folder
        final String projectLogsDir = ".project-build-logs";
        final String jobLogsDir = projectLogsDir + "/" + jobName;
        steps.add(
                new RunMultiRepoCiToolCommandStepBuilder()
                        .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/" + TOOL_JAR_NAME)
                        .setCommand(CopyLogArtifacts.Command.NAME)
                        .addArgs(".", jobLogsDir)
                        .setIfCondition(IfCondition.FAILURE)
                        .build());
        steps.add(
                new ZipFolderStepBuilder()
                        .setIfCondition(IfCondition.FAILURE)
                        .setContainingDirectory(projectLogsDir)
                        .setChildDirectoryToZip(jobName)
                        .removeDirectory()
                        .build());
        steps.add(
                new UploadArtifactStepBuilder()
                        .setName(jobLogsArtifactName)
                        .setPath(projectLogsDir)
                        .setIfCondition(IfCondition.FAILURE)
                        .build()
        );

        job.put("steps", steps);

        return job;
    }

    private void backupMavenArtifactsProducedByBuild(ComponentJobContext context, List<Object> steps) {
        Path rootPom = Paths.get("pom.xml");
        Path backupPath = MAVEN_REPO_BACKUPS_ROOT.resolve(context.component.getName());

        // Back up the parts of the maven repo we built
        steps.add(
                new RunMultiRepoCiToolCommandStepBuilder()
                        .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/" + TOOL_JAR_NAME)
                        .setCommand(BackupMavenArtifacts.Command.NAME)
                        .addArgs(rootPom.toAbsolutePath().toString(), MAVEN_REPO.toString(), backupPath.toAbsolutePath().toString())
                        .setIfCondition(IfCondition.SUCCESS)
                        .build());

        // Commit the changes and push
        steps.add(
                new GitCommandStepBuilder()
                        .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                        .setStandardUserAndEmail()
                        .addFiles("-A")
                        .setCommitMessage("Back up the maven artifacts created by " + context.getComponent().getName())
                        .setPush()
                        .setIfCondition(IfCondition.SUCCESS)
                        .build());
    }

    private void setupWorkflowEndJob(RepoConfig repoConfig) {
        Map<String, Object> job = repoConfig.getEndJob();
        if (job == null) {
            return;
        }
        String jobName = "ob-ci-end-job";

        // Copy the job so that the ordering is better
        Map<String, Object> jobCopy = new LinkedHashMap<>();
        jobCopy.put("name", jobName);
        // Copy the list to avoid yaml use anchors/references
        jobCopy.put("runs-on", new ArrayList<>(repoConfig.getRunsOn()));
        jobCopy.put("needs", new ArrayList<>(jobs.keySet()));

        // RepoConfigParser has ensured there is always an env entry
        Map<String, String> env = new HashMap<>((Map<String, String>)job.get("env"));
        jobCopy.put("env", env);
        addComponentVersionEnvVars(env);

        env.put(OB_ARTIFACTS_DIRECTORY_VAR_NAME, OB_ARTIFACTS_DIRECTORY_NAME);
        env.put(OB_STATUS_VAR_NAME, OB_STATUS_RELATIVE_PATH);
        env.put(OB_ISSUE_DATA_JSON_VAR_NAME, OB_ISSUE_DATA_JSON);

        for (String key : job.keySet()) {
            if (!key.equals("env") && !key.equals("steps")) {
                jobCopy.put(key, job.get(key));
            }
        }

        List<Object> steps = new ArrayList();

        // Add boiler plate steps

        steps.add(new AbsolutePathVariableStepBuilder(OB_ARTIFACTS_DIRECTORY_VAR_NAME).build());
        steps.add(new AbsolutePathVariableStepBuilder(OB_STATUS_VAR_NAME).build());

        steps.add(new CheckoutStepBuilder()
                .setBranch(branchName)
                .build());
        steps.add(
                new SetupJavaStepBuilder()
                        .setVersion(
                                repoConfig.getJavaVersion() != null ? repoConfig.getJavaVersion() : DEFAULT_JAVA_VERSION)
                        .build());
        steps.add(
                new GitCommandStepBuilder()
                        .setRebase()
                        .build());
        steps.add(
                Collections.singletonMap(
                        "run",
                        BashUtils.createDirectoryIfNotExist("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                        + "touch ${" + OB_STATUS_VAR_NAME + "}\n"));

        addIpv6LocalhostHostEntryIfRunningOnGitHub(steps, (List)jobCopy.get("runs-on"));

        steps.add(
                new RunMultiRepoCiToolCommandStepBuilder()
                        .setJar(TOOL_JAR_NAME)
                        .setCommand(SplitLargeFilesInDirectory.MergeCommand.NAME)
                        .addArgs("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                        .build());


        // RepoConfigParser has validated the job format already
        List<Object> jobSteps = (List<Object>)job.get("steps");
        steps.addAll(jobSteps);
        jobCopy.put("steps", steps);

        // Make sure we split any large files that people might have copied into the artifacts directory
        steps.add(
                new RunMultiRepoCiToolCommandStepBuilder()
                        .setJar(TOOL_JAR_NAME)
                        .setCommand(SplitLargeFilesInDirectory.SplitCommand.NAME)
                        .addArgs("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                        .build());

        // Push the changes to the artifacts
        steps.add(
                new GitCommandStepBuilder()
                        .setStandardUserAndEmail()
                        .addFiles("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                        .setCommitMessage("Store any artifacts copied to \\${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "} by " + jobName)
                        .setPush()
                        .setIfCondition(IfCondition.SUCCESS)
                        .build());

        jobs.put(jobName, jobCopy);
    }


    private void setupReadStatusOutputJob(RepoConfig repoConfig) {
        String jobName = STATUS_OUTPUT_JOB_NAME;

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("name", jobName);
        // Copy the list to avoid yaml use anchors/references
        job.put("runs-on", new ArrayList<>(repoConfig.DEFAULT_RUNS_ON));
        job.put("needs", new ArrayList<>(jobs.keySet()));
        job.put("if", IfCondition.ALWAYS.getValue());

        // RepoConfigParser has ensured there is always an env entry
        Map<String, String> env = new HashMap<>();
        job.put("env", env);
        env.put(OB_STATUS_VAR_NAME, OB_STATUS_RELATIVE_PATH);
        job.put("outputs",
                Collections.singletonMap(STATUS_OUTPUT_OUTPUT_VAR_NAME,
                        String.format("${{steps.%s.outputs.%s}}", STATUS_OUTPUT_OUTPUT_VAR_NAME, STATUS_OUTPUT_OUTPUT_VAR_NAME)));

        List<Object> steps = new ArrayList();

        // Add boiler plate steps
        steps.add(new AbsolutePathVariableStepBuilder(OB_STATUS_VAR_NAME).build());

        steps.add(new CheckoutStepBuilder()
                .setBranch(branchName)
                .build());
        steps.add(
                new GitCommandStepBuilder()
                        .setRebase()
                        .build());
        StringBuilder run = new StringBuilder();
        run.append("[[ -f \"${" + OB_STATUS_VAR_NAME + "}\" ]] && TMP=\"$(cat ${" + OB_STATUS_VAR_NAME + "})\"\n");
        // Escape newline and other characters as recommended in https://github.com/actions/toolkit/issues/403
        // Otherwise we just get the first line in the output variable
        run.append("TMP=\"${TMP//'%'/'%25'}\"\n");
        run.append("TMP=\"${TMP//$'\\n'/'%0A'}\"\n");
        run.append("TMP=\"${TMP//$'\\r'/'%0D'}\"\n");
        run.append("echo \"::set-output name=" + STATUS_OUTPUT_OUTPUT_VAR_NAME + "::${TMP}\"\n");

        Map<String, Object> readStatusStep = new LinkedHashMap<>();
        readStatusStep.put("id", STATUS_OUTPUT_OUTPUT_VAR_NAME);
        readStatusStep.put("run", run.toString());

        steps.add(readStatusStep);

        job.put("steps", steps);

        jobs.put(jobName, job);
    }

    private void setupStatusReportingJobs(RepoConfig repoConfig, TriggerConfig triggerConfig) {
        if (!repoConfig.isCommentsReporting() && repoConfig.getSuccessLabel() == null || repoConfig.getFailureLabel() == null) {
            return;
        }
        // Let the Job builder do the proper formatting of the message
        // It currently uses the github-script action which uses JavaScript
        // so it is quite 'specialised'
        Map<String, String> jobNamesAndVersionVariables = new LinkedHashMap<>();
        for (String buildJobName : buildJobNamesByComponent.values()) {

            String hash = String.format("needs.%s.outputs.%s",
                    buildJobName,
                    REV_PARSE_STEP_OUTPUT);
            jobNamesAndVersionVariables.put(buildJobName, hash);
        }

        // Grab the previous jobs here, since each of the setup methods will modify the current map
        Set<String> needs = jobs.keySet();
        setupStatusReportingJob(repoConfig, needs, jobNamesAndVersionVariables, true);
        setupStatusReportingJob(repoConfig, needs, jobNamesAndVersionVariables, false);
    }

    private void setupStatusReportingJob(RepoConfig repoConfig, Set<String> needs, Map<String, String> jobNamesAndVersionVariables, boolean success) {
        String jobName = "ob-ci-status-" + (success ? "success" : "failure");
        IssueStatusReportJobBuilder jobBuilder =
                new IssueStatusReportJobBuilder(jobName, issueNumber, success);
        jobBuilder.setNeeds(needs);
        jobBuilder.setJobNamesAndVersionVariables(jobNamesAndVersionVariables);
        jobBuilder.setComment(success ? "The workflow passed!" : "The workflow failed.");
        jobBuilder.setAddLabel(success ? repoConfig.getSuccessLabel() : repoConfig.getFailureLabel());
        jobBuilder.setRemoveLabel(success ? repoConfig.getFailureLabel() : repoConfig.getSuccessLabel());
        jobBuilder.setStatusOutputVariableAndRef("status_output", STATUS_OUTPUT_OUTPUT_REF);
        jobs.put(jobName, jobBuilder.build());
    }

    private void setupCleanupJob(TriggerConfig triggerConfig) {
        Map<String, Object> job = new LinkedHashMap<>();
        String name = "ob-ci-cleanup";
        job.put("name", name);
        job.put("runs-on", "ubuntu-latest");
        job.put("needs", new ArrayList<>(jobs.keySet()));
        job.put("if", IfCondition.ALWAYS.getValue());
        List<Object> steps = new ArrayList<>();
        job.put("steps", steps);

        steps.add(
                new CheckoutStepBuilder()
                        .setPath(CI_TOOLS_CHECKOUT_FOLDER)
                        .setBranch(branchName)
                        .build());
        steps.add(
                new GitCommandStepBuilder()
                        .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                        .setDeleteRemoteBranch()
                        .build());

        jobs.put(name, job);
    }

    private void addComponentVersionEnvVars(Map<String, String> env) {
        for (Map.Entry<String, String> entry : buildJobNamesByComponent.entrySet()) {
            String componentName = entry.getKey();
            String buildJobName = entry.getValue();
            env.put(getEndUserVersionEnvVarName(componentName), "${{ " + formatOutputVersionVariableName(buildJobName, componentName) + "}}");
        }
    }

    private void addIpv6LocalhostHostEntryIfRunningOnGitHub(List<Object> steps, List<String> runsOn) {
        if (runsOn.equals(RepoConfig.DEFAULT_RUNS_ON)) {
            // If it is the default runs on we're most likely running on GitHub which does not
            // have an IPv6 localhost mapping
            // We don't want to add that if running on a custom runner
            steps.add(new Ipv6LocalhostStepBuilder().build());
        }
    }

    private String getComponentBuildJobId(String name) {
        return name + "-build";
    }

    private String getInternalVersionEnvVarName(String name) {
        return "version_" + name.replace("-", "_");
    }

    private String getEndUserVersionEnvVarName(String name) {
        return "OB_" + getInternalVersionEnvVarName(name).toUpperCase();
    }

    private String formatOutputVersionVariableName(String buildJobName, String componentName) {
        return String.format("needs.%s.outputs.%s",
                buildJobName,
                getInternalVersionEnvVarName(componentName));
    }

    private abstract class ComponentJobContext {
        protected final RepoConfig repoConfig;
        protected final Component component;
        protected final Map<String, ComponentDependencyContext> dependencyContexts;

        public ComponentJobContext(RepoConfig repoConfig, Component component) {
            this.repoConfig = repoConfig;
            this.component = component;
            Map<String, ComponentDependencyContext> dependencyContexts = new LinkedHashMap<>();
            if (component.getDependencies().size() > 0) {
                for (Dependency dep : component.getDependencies()) {
                    String depComponentName = dep.getName();
                    ComponentJobsConfig componentJobsConfig = componentJobsConfigs.get(depComponentName);
                    String buildJob;
                    if (componentJobsConfig == null) {
                        buildJob = getComponentBuildJobId(depComponentName);
                    } else {
                        buildJob = componentJobsConfig.getBuildJobName();
                    }
                    ComponentDependencyContext depCtx = new ComponentDependencyContext(dep, buildJob);
                    dependencyContexts.put(depComponentName, depCtx);
                }
            }
            this.dependencyContexts = Collections.unmodifiableMap(dependencyContexts);
        }

        public abstract String getJobName();

        public String getJavaVersion() {
            String javaVersion = DEFAULT_JAVA_VERSION;
            if (repoConfig.getJavaVersion() != null) {
                javaVersion = repoConfig.getJavaVersion();
            }
            return javaVersion;
        }

        public Component getComponent() {
            return component;
        }

        protected List<String> createNeeds() {
            List<String> needs = new ArrayList<>();
            if (isBuildJob()) {
                needs.add(CANCEL_PREVIOUS_RUNS_JOB_NAME);
            }
            for (ComponentDependencyContext depCtx : dependencyContexts.values()) {
                needs.add(depCtx.buildJobName);
            }
            return needs;
        }

        public boolean hasDependencies() {
            // Build jobs get the cancel previous runs job added
            int limit = isBuildJob() ? 1 : 0;
            return createNeeds().size() > limit;
        }

        abstract List<Map<String, Object>> createBuildSteps();

        protected String getDependencyVersionMavenProperties(boolean useOutputVars) {
            StringBuilder sb = new StringBuilder();
            for (ComponentDependencyContext depCtx : dependencyContexts.values()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                final String versionVarName;
                if (useOutputVars) {
                    versionVarName = "${{" + depCtx.versionOutputVarName + "}}";
                } else {
                    versionVarName = "${" + getEndUserVersionEnvVarName(depCtx.dependency.getName()) + "}";
                }

                sb.append("-D" + depCtx.dependency.getProperty() + "=" + versionVarName);
            }
            return sb.toString();
        }

        public Map<String, String> createEnv() {
            Map<String, String> env = new LinkedHashMap<>();
            addComponentVersionEnvVars(env);
            env.put(OB_ISSUE_DATA_JSON_VAR_NAME, CI_TOOLS_CHECKOUT_FOLDER + "/" + OB_ISSUE_DATA_JSON);
            return env;
        }

        protected boolean isBuildJob() {
            return true;
        }

        public void addIfClause(Map<String, Object> job) {
            // Default is to do nothing
        }

        public List<String> getRunsOn() {
            // Copy the list to avoid yaml use anchors/references
            return new ArrayList<>(repoConfig.getRunsOn());
        }
    }

    private class DefaultComponentJobContext extends ComponentJobContext {
        public DefaultComponentJobContext(RepoConfig repoConfig, Component component) {
            super(repoConfig, component);
        }

        @Override
        public String getJobName() {
            return component.getName();
        }

        @Override
        List<Map<String, Object>> createBuildSteps() {
            return Collections.singletonList(
                    new MavenBuildStepBuilder()
                            .setOptions(getMavenOptions(component))
                            .build());
        }

        private String getMavenOptions(Component component) {
            StringBuilder sb = new StringBuilder();
            if (component.getMavenOpts() != null) {
                sb.append(component.getMavenOpts());
                sb.append(" ");
            }
            sb.append(getDependencyVersionMavenProperties(false));
            return sb.toString();
        }

        public String getJavaVersion() {
            String javaVersion = super.getJavaVersion();

            if (component.getJavaVersion() != null) {
                javaVersion = component.getJavaVersion();
            }
            return javaVersion;
        }
    }

    private class ConfiguredComponentJobContext extends ComponentJobContext {
        private final String buildJobName;
        private final BaseComponentJobConfig componentJobConfig;

        public ConfiguredComponentJobContext(RepoConfig repoConfig, Component component, String buildJobName, BaseComponentJobConfig componentJobConfig) {
            super(repoConfig, component);
            this.buildJobName = buildJobName;
            this.componentJobConfig = componentJobConfig;
        }

        @Override
        public String getJobName() {
            return componentJobConfig.getName();
        }

        @Override
        public void addIfClause(Map<String, Object> job) {
            if (componentJobConfig.isEndJob()) {
                String ifCondition = ((ComponentEndJobConfig) componentJobConfig).getIfCondition();
                if (ifCondition != null) {
                    job.put("if", ifCondition);
                }
            }
        }

        @Override
        protected List<String> createNeeds() {
            List<String> needs = super.createNeeds();
            for (String need : componentJobConfig.getNeeds()) {
                needs.add(need);
            }
            return needs;
        }

        @Override
        List<Map<String, Object>> createBuildSteps() {
            List<Map<String, Object>> steps = new ArrayList<>();

            steps.add(new AbsolutePathVariableStepBuilder(OB_ARTIFACTS_DIRECTORY_VAR_NAME).build());
            steps.add(new AbsolutePathVariableStepBuilder(OB_STATUS_VAR_NAME).build());

            if (isBuildJob()) {
                // Ensure the artifacts directory is there
                // It will be available to later jobs via the pushed git branch
                Map<String, Object> artifactsDir = new LinkedHashMap<>();
                artifactsDir.put("name", "Ensure artifacts dir is there");
                artifactsDir.put("run",
                        BashUtils.createDirectoryIfNotExist("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}") +
                                "touch ${" + OB_STATUS_VAR_NAME + "}\n");
                steps.add(artifactsDir);
            }

            // Merge any split files
            steps.add(
                    new RunMultiRepoCiToolCommandStepBuilder()
                            .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/" + TOOL_JAR_NAME)
                            .setCommand(SplitLargeFilesInDirectory.MergeCommand.NAME)
                            .addArgs("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                            .build());


            if (componentJobConfig.isEndJob()) {
                steps.addAll(((ComponentEndJobConfig)componentJobConfig).getSteps());
            } else {
                List<JobRunElementConfig> runElementConfigs = ((ComponentJobConfig)componentJobConfig).getRunElements();
                Map<String, Object> build = new HashMap<>();
                build.put("name", "Maven Build");
                StringBuilder sb = new StringBuilder();
                for (JobRunElementConfig cfg : runElementConfigs) {
                    if (cfg.getType() == JobRunElementConfig.Type.SHELL) {
                        sb.append(cfg.getCommand());
                        sb.append("\n");
                    } else {
                        sb.append("mvn -B ");
                        sb.append(cfg.getCommand());
                        sb.append(" ");
                        sb.append(getDependencyVersionMavenProperties(false));
                        sb.append("\n");
                    }
                }
                build.put("run", sb.toString());
                steps.add(build);
            }

            // Make sure we split any large files that people might have copied into the artifacts directory
            steps.add(
                    new RunMultiRepoCiToolCommandStepBuilder()
                        .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/" + TOOL_JAR_NAME)
                        .setCommand(SplitLargeFilesInDirectory.SplitCommand.NAME)
                        .addArgs("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                        .build());

            if (!isBuildJob()) {
                // For build jobs this will be handled by the main boiler plate steps
                steps.add(
                        new GitCommandStepBuilder()
                                .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                                .setStandardUserAndEmail()
                                .addFiles("${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "}")
                                .setCommitMessage("Store any artifacts copied to \\${" + OB_ARTIFACTS_DIRECTORY_VAR_NAME + "} by " + getJobName())
                                .setPush()
                                .setIfCondition(IfCondition.SUCCESS)
                                .build());

            }
            return steps;
        }

        @Override
        public Map<String, String> createEnv() {
            Map<String, String> env = new LinkedHashMap<>();
            env.putAll(super.createEnv());
            env.putAll(componentJobConfig.getJobEnv());
            env.put(OB_ARTIFACTS_DIRECTORY_VAR_NAME, CI_TOOLS_CHECKOUT_FOLDER + "/" + OB_ARTIFACTS_DIRECTORY_NAME);
            env.put(OB_STATUS_VAR_NAME, CI_TOOLS_CHECKOUT_FOLDER + "/" + OB_STATUS_RELATIVE_PATH);
            if (!isBuildJob()) {
                String var = "${{ " + formatOutputVersionVariableName(buildJobName, component.getName() + " }}");
                env.put(OB_PROJECT_VERSION_VAR_NAME, var);
            }
            addComponentVersionEnvVars(env);
            env.put(OB_END_JOB_MAVEN_DEPENDENCY_VERSIONS_VAR_NAME, getDependencyVersionMavenProperties(true));
            return env;
        }

        public String getJavaVersion() {
            String javaVersion = super.getJavaVersion();
            // Let the user override the java version specified in the job config
            if (componentJobConfig.getJavaVersion() != null) {
                javaVersion = componentJobConfig.getJavaVersion();
            }
            if (component.getJavaVersion() != null) {
                javaVersion = component.getJavaVersion();
            }
            return javaVersion;
        }

        @Override
        protected boolean isBuildJob() {
            return componentJobConfig.isBuildJob();
        }

        @Override
        public List<String> getRunsOn() {
            if (componentJobConfig.getRunsOn() != null) {
                return componentJobConfig.getRunsOn();
            }
            return super.getRunsOn();
        }
    }

    private class ComponentDependencyContext {
        final Dependency dependency;
        final String buildJobName;
        final String versionOutputVarName;


        public ComponentDependencyContext(Dependency dependency, String buildJobName) {
            this.dependency = dependency;
            this.buildJobName = buildJobName;
            this.versionOutputVarName = formatOutputVersionVariableName(buildJobName, dependency.getName());
        }
    }

    public static class Command implements ToolCommand {
        public static final String NAME = "generate-workflow";

        @Override
        public String getDescription() {
            return "Generates a GitHub workflow YAML from the trigger issue input";
        }

        @Override
        public void invoke(String[] args) throws Exception {
            generate(args);
        }
    }
}
