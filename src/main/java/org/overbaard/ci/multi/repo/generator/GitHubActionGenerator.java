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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.overbaard.ci.multi.repo.Main;
import org.overbaard.ci.multi.repo.Usage;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.component.JobConfig;
import org.overbaard.ci.multi.repo.config.component.JobRunElementConfig;
import org.overbaard.ci.multi.repo.config.repo.RepoConfig;
import org.overbaard.ci.multi.repo.config.repo.RepoConfigParser;
import org.overbaard.ci.multi.repo.config.trigger.Component;
import org.overbaard.ci.multi.repo.config.trigger.Dependency;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfig;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfigParser;
import org.overbaard.ci.multi.repo.log.copy.CopyLogArtifacts;
import org.overbaard.ci.multi.repo.maven.backup.BackupMavenArtifacts;
import org.overbaard.ci.multi.repo.maven.backup.OverlayBackedUpMavenArtifacts;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitHubActionGenerator {
    public static final String GENERATE_WORKFLOW = "generate-workflow";
    public static final String TOKEN_NAME = "secrets.OB_MULTI_CI_PAT";

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
    static final Path MAVEN_REPO_BACKUPS_ROOT = Paths.get(CI_TOOLS_CHECKOUT_FOLDER + "/repo-backups");
    final static Path MAVEN_REPO;
    static {
        if (System.getenv("HOME") == null) {
            throw new IllegalStateException("No HOME env var set!");
        }
        MAVEN_REPO = Paths.get(System.getenv("HOME"), ".m2/repository");
    }




    private final Map<String, Object> workflow = new LinkedHashMap<>();
    private final Map<String, ComponentJobsConfig> componentJobsConfigs = new HashMap<>();
    private final Set<String> buildJobNames = new LinkedHashSet<>();
    private final Path workflowFile;
    private final Path yamlConfig;
    private final String branchName;
    private final int issueNumber;
    private String jobLogsArtifactName;

    private GitHubActionGenerator(Path workflowFile, Path yamlConfig, String branchName, int issueNumber) {
        this.workflowFile = workflowFile;
        this.yamlConfig = yamlConfig;
        this.branchName = branchName;
        this.issueNumber = issueNumber;
    }

    public Path getYamlConfig() {
        return yamlConfig;
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

        String headline = usage.getCommandUsageHeadline(url, GENERATE_WORKFLOW);
        System.out.print(usage.usage(headline));
    }

    private void generate() throws Exception {
        RepoConfig repoConfig = RepoConfigParser.create(REPO_CONFIG_FILE).parse();
        TriggerConfig triggerConfig = TriggerConfigParser.create(yamlConfig).parse();
        System.out.println("Wil create workflow file at " + workflowFile.toAbsolutePath());

        setupWorkFlowHeaderSection(repoConfig, triggerConfig);
        setupJobs(repoConfig, triggerConfig);

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

        final Map<String, Object> jobs = new LinkedHashMap<>();

        for (Component component : triggerConfig.getComponents()) {
            Path componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yml");
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found");
                componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yaml");
            }
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found. Setting up default job for component: " + component.getName());
                setupDefaultComponentBuildJob(jobs, repoConfig, component);
            } else {
                System.out.println("using " + componentJobsFile + " to add job(s) for component: " + component.getName());
                setupComponentBuildJobsFromFile(jobs, repoConfig, component, componentJobsFile);
            }
        }
        jobs.put("job-status-report", setupReportingJob(jobs.keySet(), repoConfig));
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

    private void setupDefaultComponentBuildJob(Map<String, Object> componentJobs, RepoConfig repoConfig, Component component) {
        DefaultComponentJobContext context = new DefaultComponentJobContext(repoConfig, component);
        Map<String, Object> job = setupJob(context);
        componentJobs.put(getComponentBuildJobId(component.getName()), job);
    }

    private void setupComponentBuildJobsFromFile(Map<String, Object> componentJobs, RepoConfig repoConfig, Component component, Path componentJobsFile) throws Exception {
        ComponentJobsConfig config = ComponentJobsConfigParser.create(componentJobsFile).parse();
        if (component.getMavenOpts() != null) {
            throw new IllegalStateException(component.getName() +
                    " defines mavenOpts but has a component job file at " + componentJobsFile +
                    ". Remove mavenOpts and configure the job in the compponent job file.");
        }
        componentJobsConfigs.put(component.getName(), config);
        List<JobConfig> jobConfigs = config.getJobs();
        for (JobConfig jobConfig : jobConfigs) {
            boolean buildJob = config.getBuildJob().equals(jobConfig.getName());
            if (!component.isDebug() || config.getBuildJob().equals(jobConfig.getName())) {
                setupComponentBuildJobFromConfig(componentJobs, repoConfig, component, jobConfig, buildJob);
            }
        }
    }

    private void setupComponentBuildJobFromConfig(Map<String, Object> componentJobs, RepoConfig repoConfig, Component component, JobConfig jobConfig, boolean buildStep) {
        ConfiguredComponentJobContext context = new ConfiguredComponentJobContext(repoConfig, component, jobConfig, buildStep);
        Map<String, Object> job = setupJob(context);
        componentJobs.put(jobConfig.getName(), job);
    }

    private Map<String, Object> setupJob(ComponentJobContext context) {
        Component component = context.getComponent();

        final String myVersionEnvVarName = getVersionEnvVarName(component.getName());

        Map<String, Object> job = new LinkedHashMap<>();
        String jobName = context.getJobName();
        job.put("name", jobName);
        job.put("runs-on", "ubuntu-latest");

        Map<String, String> env = context.createEnv();
        if (env.size() > 0) {
            job.put("env", env);
        }

        List<String> needs = context.createNeeds();
        if (needs.size() > 0) {
            job.put("needs", needs);
        }

        if (context.isBuildStep()) {
            Map<String, String> outputs = new HashMap<>();
            outputs.put(myVersionEnvVarName, "${{steps.grab-version.outputs." + myVersionEnvVarName + "}}");
            outputs.put(REV_PARSE_STEP_OUTPUT, "${{steps." + REV_PARSE_STEP_ID + ".outputs." + REV_PARSE_STEP_OUTPUT + "}}");
            job.put("outputs", outputs);

            buildJobNames.add(context.getJobName());
        }

        List<Object> steps = new ArrayList<>();
        // Get the repo of the component we want to build
        steps.add(
                new CheckoutBuilder()
                        .setRepo(component.getOrg(), component.getName())
                        .setBranch(component.getBranch())
                        .build());
        // Get this repo so that we have the tooling contained in this project. We will run various of these later.
        steps.add(
                new CheckoutBuilder()
                        .setPath(CI_TOOLS_CHECKOUT_FOLDER)
                        .setBranch(branchName)
                        .build());
        steps.add(
                new CacheMavenRepoBuilder()
                        .build());
        steps.add(
                new SetupJavaBuilder()
                        .setVersion(context.getJavaVersion())
                        .build());

        if (needs.size() > 0) {
            // Get the maven artifact backups
            steps.add(
                    new GitCommandBuilder()
                            .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                            .setRebase()
                            .build());

            steps.add(
                    new RunMultiRepoCiToolCommandBuilder()
                            .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/multi-repo-ci-tool.jar")
                            .setCommand(OverlayBackedUpMavenArtifacts.OVERLAY_BACKED_UP_MAVEN_ARTIFACTS)
                            .addArgs(MAVEN_REPO.toString(), MAVEN_REPO_BACKUPS_ROOT.toString())
                            .build());
        }

        if (context.isBuildStep()) {
            steps.add(
                    new GrabProjectVersionBuilder()
                            .setEnvVarName(getVersionEnvVarName(component.getName()))
                            .build());
            steps.add(
                new GitRevParseIntoOutputVariableBuilder(REV_PARSE_STEP_ID, REV_PARSE_STEP_OUTPUT)
                    .build());
        }

        // Make sure that localhost maps to ::1 in the hosts file
        steps.add(new Ipv6LocalhostBuilder().build());

        steps.addAll(context.createBuildJobs());

        if (context.getComponent().isDebug()) {
            steps.add(new TmateDebugBuilder().build());
        }

        if (context.isBuildStep()) {
            backupMavenArtifactsProducedByBuild(context, steps);
        }

        // Copy across the build artifacts to the folder and upload the 'root' folder
        final String projectLogsDir = ".project-build-logs";
        final String jobLogsDir = projectLogsDir + "/" + jobName;
        steps.add(
                new RunMultiRepoCiToolCommandBuilder()
                        .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/multi-repo-ci-tool.jar")
                        .setCommand(CopyLogArtifacts.COPY_LOGS)
                        .addArgs(".", jobLogsDir)
                        .setIfCondition(IfCondition.FAILURE)
                        .build());
        steps.add(
                new ZipFolderBuilder()
                        .setIfCondition(IfCondition.FAILURE)
                        .setContainingDirectory(projectLogsDir)
                        .setChildDirectoryToZip(jobName)
                        .removeDirectory()
                        .build());
        steps.add(
                new UploadArtifactBuilder()
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
                new RunMultiRepoCiToolCommandBuilder()
                        .setJar(CI_TOOLS_CHECKOUT_FOLDER + "/multi-repo-ci-tool.jar")
                        .setCommand(BackupMavenArtifacts.BACKUP_MAVEN_ARTIFACTS)
                        .addArgs(rootPom.toAbsolutePath().toString(), MAVEN_REPO.toString(), backupPath.toAbsolutePath().toString())
                        .setIfCondition(IfCondition.SUCCESS)
                        .build());

        // Commit the changes and push
        steps.add(
                new GitCommandBuilder()
                        .setWorkingDirectory(CI_TOOLS_CHECKOUT_FOLDER)
                        .setUserAndEmail("CI Action", "ci@example.com")
                        .addFiles("-A")
                        .setCommitMessage("Back up the maven artifacts created by " + context.getComponent().getName())
                        .setPush()
                        .setIfCondition(IfCondition.SUCCESS)
                        .build());
    }

    private Map<String, Object> setupReportingJob(Set<String> allJobNames, RepoConfig repoConfig) {

        // Let the Job builder do the proper formatting of the message
        // It currently uses the github-script action which uses JavaScript
        // so it is quite 'specialised'
        Map<String, String> jobNamesAndVersionVariables = new LinkedHashMap<>();
        for (String buildJobName : buildJobNames) {
            String hash = String.format("needs.%s.outputs.%s",
                    buildJobName,
                    REV_PARSE_STEP_OUTPUT);
            jobNamesAndVersionVariables.put(buildJobName, hash);
        }

        if (repoConfig.isCommentsReporting() || repoConfig.getSuccessLabel() != null || repoConfig.getFailureLabel() != null ) {
            IssueStatusReportJobBuilder jobBuilder = new IssueStatusReportJobBuilder(issueNumber);
            jobBuilder.setNeeds(allJobNames);
            jobBuilder.setJobNamesAndVersionVariables(jobNamesAndVersionVariables);
            jobBuilder.setSuccessLabel(repoConfig.getSuccessLabel());
            jobBuilder.setSuccessMessage("The job passed!");
            jobBuilder.setFailureLabel(repoConfig.getFailureLabel());
            jobBuilder.setFailureMessage("The job failed");
            return jobBuilder.build();
        }
        return Collections.emptyMap();
    }

    private String getComponentBuildJobId(String name) {
        return name + "-build";
    }

    private String getVersionEnvVarName(String name) {
        return "version_" + name.replace("-", "_");
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
                        buildJob = componentJobsConfig.getBuildJob();
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
            for (ComponentDependencyContext depCtx : dependencyContexts.values()) {
                needs.add(depCtx.buildJobName);
            }
            return needs;
        }

        abstract List<Map<String, Object>> createBuildJobs();

        protected String getDependencyVersionMavenProperties() {
            StringBuilder sb = new StringBuilder();
            for (ComponentDependencyContext depCtx : dependencyContexts.values()) {
                sb.append(" ");

                sb.append("-D" + depCtx.dependency.getProperty() + "=\"${{" + depCtx.versionVarName + "}}\"");
            }
            return sb.toString();
        }

        public Map<String, String> createEnv() {
            return Collections.emptyMap();
        }

        protected boolean isBuildStep() {
            return true;
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
        List<Map<String, Object>> createBuildJobs() {
            return Collections.singletonList(
                    new MavenBuildBuilder()
                            .setOptions(getMavenOptions(component))
                            .build());
        }

        private String getMavenOptions(Component component) {
            StringBuilder sb = new StringBuilder();
            if (component.getMavenOpts() != null) {
                sb.append(component.getMavenOpts());
                sb.append(" ");
            }
            sb.append(getDependencyVersionMavenProperties());
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
        private final JobConfig jobConfig;
        private final boolean buildStep;

        public ConfiguredComponentJobContext(RepoConfig repoConfig, Component component, JobConfig jobConfig, boolean buildStep) {
            super(repoConfig, component);
            this.jobConfig = jobConfig;
            this.buildStep = buildStep;
        }

        @Override
        public String getJobName() {
            return jobConfig.getName();
        }

        @Override
        protected List<String> createNeeds() {
            List<String> needs = super.createNeeds();
            for (String need : jobConfig.getNeeds()) {
                needs.add(need);
            }
            return needs;
        }

        @Override
        List<Map<String, Object>> createBuildJobs() {
            List<JobRunElementConfig> runElementConfigs = jobConfig.getRunElements();
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
                    sb.append(getDependencyVersionMavenProperties());
                    sb.append("\n");
                }
            }
            build.put("run", sb.toString());
            return Collections.singletonList(build);
        }

        @Override
        public Map<String, String> createEnv() {
            Map<String, String> env = new HashMap<>();
            env.putAll(super.createEnv());
            env.putAll(jobConfig.getJobEnv());
            return env;
        }

        public String getJavaVersion() {
            String javaVersion = super.getJavaVersion();
            // Let the user override the java version specified in the job config
            if (jobConfig.getJavaVersion() != null) {
                javaVersion = jobConfig.getJavaVersion();
            }
            if (component.getJavaVersion() != null) {
                javaVersion = component.getJavaVersion();
            }
            return javaVersion;
        }

        @Override
        protected boolean isBuildStep() {
            return buildStep;
        }
    }

    private class ComponentDependencyContext {
        final Dependency dependency;
        final String buildJobName;
        final String versionVarName;

        public ComponentDependencyContext(Dependency dependency, String buildJobName) {
            this.dependency = dependency;
            this.buildJobName = buildJobName;
            this.versionVarName =
                    String.format("needs.%s.outputs.%s",
                            buildJobName,
                            getVersionEnvVarName(dependency.getName()));
        }
    }
}
