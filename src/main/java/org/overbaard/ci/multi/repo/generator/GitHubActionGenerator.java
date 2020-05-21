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
import org.overbaard.ci.multi.repo.Usage;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.component.JobConfig;
import org.overbaard.ci.multi.repo.config.component.JobRunElementConfig;
import org.overbaard.ci.multi.repo.config.trigger.Dependency;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfig;
import org.overbaard.ci.multi.repo.config.trigger.TriggerConfigParser;
import org.overbaard.ci.multi.repo.config.trigger.Component;
import org.overbaard.ci.multi.repo.log.copy.CopyLogArtifacts;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitHubActionGenerator {
    public static final String GENERATE_WORKFLOW = "generate-workflow";

    private static final String ARG_WORKFLOW_DIR = "--workflow-dir";
    private static final String ARG_YAML = "--yaml";
    private static final String ARG_ISSUE = "--issue";
    private static final String ARG_BRANCH = "--branch";


    static final String CI_TOOLS_CHECKOUT_FOLDER = ".ci-tools";
    static final String PROJECT_VERSIONS_DIRECTORY = ".project_versions";
    static final Path COMPONENT_JOBS_DIR = Paths.get(".repo-config/component-jobs");
    private final Map<String, Object> workflow = new LinkedHashMap<>();
    private final Map<String, ComponentJobsConfig> componentJobsConfigs = new HashMap<>();
    private final Path workflowFile;
    private final Path yamlConfig;
    private final String branchName;
    private final String issueNumber;
    private String jobLogsArtifactName;

    private GitHubActionGenerator(Path workflowFile, Path yamlConfig, String branchName, String issueNumber) {
        this.workflowFile = workflowFile;
        this.yamlConfig = yamlConfig;
        this.branchName = branchName;
        this.issueNumber = issueNumber;
    }

    public Path getYamlConfig() {
        return yamlConfig;
    }

    public static void generate(String[] args) throws Exception {
        GitHubActionGenerator generator = create(args);
        generator.generate();
    }

    private static GitHubActionGenerator create(String[] args) throws Exception {
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

        Path workflowFile = workflowDir.resolve("ci-" + issueNumber + ".yml");
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
        TriggerConfig triggerConfig = TriggerConfigParser.create(yamlConfig).parse();
        System.out.println("Wil create workflow file at " + workflowFile.toAbsolutePath());

        setupWorkFlowHeaderSection(triggerConfig);
        setupJobs(triggerConfig);

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

    private void setupWorkFlowHeaderSection(TriggerConfig triggerConfig) {
        workflow.put("name", triggerConfig.getName());
        workflow.put("on", Collections.singletonMap("push", Collections.singletonMap("branches", branchName)));

        if (triggerConfig.getEnv().size() > 0) {
            Map<String, Object> env = new HashMap<>();
            for (String key : triggerConfig.getEnv().keySet()) {
                env.put(key, triggerConfig.getEnv().get(key));
            }
            workflow.put("env", env);
        }
    }

    private void setupJobs(TriggerConfig triggerConfig) throws Exception {

        this.jobLogsArtifactName = createJobLogsArtifactName(triggerConfig);

        final Map<String, Object> componentJobs = new LinkedHashMap<>();

        for (Component component : triggerConfig.getComponents()) {
            Path componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yml");
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found");
                componentJobsFile = COMPONENT_JOBS_DIR.resolve(component.getName() + ".yaml");
            }
            if (!Files.exists(componentJobsFile)) {
                System.out.println("No " + componentJobsFile + " found. Setting up default job for component: " + component.getName());
                setupDefaultComponentBuildJob(componentJobs, component);
            } else {
                System.out.println("using " + componentJobsFile + " to add job(s) for component: " + component.getName());
                setupComponentBuildJobsFromFile(componentJobs, component, componentJobsFile);
            }
        }
        workflow.put("jobs", componentJobs);
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

    private void setupDefaultComponentBuildJob(Map<String, Object> componentJobs, Component component) {
        DefaultComponentJobContext context = new DefaultComponentJobContext(component);
        Map<String, Object> job = setupJob(context);
        componentJobs.put(getComponentBuildId(component.getName()), job);
    }

    private void setupComponentBuildJobsFromFile(Map<String, Object> componentJobs, Component component, Path componentJobsFile) throws Exception {
        ComponentJobsConfig config = ComponentJobsConfigParser.create(componentJobsFile).parse();
        if (component.getMavenOpts() != null) {
            throw new IllegalStateException(component.getName() +
                    " defines mavenOpts but has a component job file at " + componentJobsFile +
                    ". Remove mavenOpts and configure the job in the compponent job file.");
        }
        componentJobsConfigs.put(component.getName(), config);
        List<JobConfig> jobConfigs = config.getJobs();
        for (JobConfig jobConfig : jobConfigs) {
            if (!component.isDebug() || config.getExportedJobs().contains(jobConfig.getName())) {
                setupComponentBuildJobFromConfig(componentJobs, component, jobConfig);
            }
        }
    }

    private void setupComponentBuildJobFromConfig(Map<String, Object> componentJobs, Component component, JobConfig jobConfig) {
        ConfiguredComponentJobContext context = new ConfiguredComponentJobContext(component, jobConfig);
        Map<String, Object> job = setupJob(context);
        componentJobs.put(jobConfig.getName(), job);
    }

    private Map<String, Object> setupJob(ComponentJobContext context) {
        Component component = context.getComponent();

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

        List<Object> steps = new ArrayList<>();
        // Get the repo of the component we want to build
        steps.add(
                new CheckoutBuilder()
                        .setRepo(component.getOrg(), component.getName())
                        .setBranch(component.getBranch())
                        .build());
        // Get this repo so that we have a copy of the .github/CopyLogArtifacts.java file for jbang to run
        steps.add(
                new CheckoutBuilder()
                        .setPath(CI_TOOLS_CHECKOUT_FOLDER)
                        .build());
        steps.add(
                new CacheMavenRepoBuilder()
                        .build());
        steps.add(
                new SetupJavaBuilder()
                        .setVersion("11")
                        .build());

            for (Dependency dependency : component.getDependencies()) {
            steps.add(
                    new DownloadArtifactBuilder()
                            .setPath(PROJECT_VERSIONS_DIRECTORY)
                            .setName(getVersionArtifactName(dependency.getName()))
                            .build());
            steps.add(
                    new ReadFileIntoEnvVarBuilder()
                            .setPath(PROJECT_VERSIONS_DIRECTORY + "/" + dependency.getName())
                            .setEnvVarName(getVersionEnvVarName(dependency.getName()))
                            .build());
        }

        final String versionFileName = PROJECT_VERSIONS_DIRECTORY + "/" + component.getName();
        steps.add(
                new GrabProjectVersionBuilder()
                        .setFileName(versionFileName)
                        .build());
        steps.add(
                new UploadArtifactBuilder()
                        .setName(getVersionArtifactName(component.getName()))
                        .setPath(versionFileName)
                        .build());

        // Make sure that localhost maps to ::1 in the hosts file
        steps.add(new Ipv6LocalhostBuilder().build());

        steps.addAll(context.createBuildSteps());

        if (context.getComponent().isDebug()) {
            steps.add(new TmateDebugBuilder().build());
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
                new UploadArtifactBuilder()
                        .setName(jobLogsArtifactName)
                        .setPath(projectLogsDir)
                        .setIfCondition(IfCondition.FAILURE)
                        .build()
        );

        job.put("steps", steps);

        return job;
    }


    private String getComponentBuildId(String name) {
        return name + "-build";
    }

    private String getVersionArtifactName(String name) {
        return "version-" + name;
    }

    private String getVersionEnvVarName(String name) {
        return "VERSION_" + name.replace("-", "_");
    }

    private abstract class ComponentJobContext {
        protected final Component component;

        public ComponentJobContext(Component component) {
            this.component = component;
        }

        public abstract String getJobName();

        public Component getComponent() {
            return component;
        }

        protected List<String> createNeeds() {
            List<String> needs = new ArrayList<>();
            if (component.getDependencies().size() > 0) {
                for (Dependency dep : component.getDependencies()) {
                    String depComponentName = dep.getName();
                    ComponentJobsConfig componentJobsConfig = componentJobsConfigs.get(depComponentName);
                    if (componentJobsConfig == null) {
                        needs.add(getComponentBuildId(depComponentName));
                    } else {
                        Set<String> exportedJobs = componentJobsConfig.getExportedJobs();
                        if (exportedJobs.size() == 0) {
                            throw new IllegalStateException(component.getName() + " has a 'needs' dependency on " +
                                    "the custom configured component '" + depComponentName + "', which is not exporting" +
                                    "any of its jobs to depend upon.");
                        }
                        needs.addAll(exportedJobs);
                    }
                }
            }
            return needs;
        }

        abstract List<Map<String, Object>> createBuildSteps();

        protected String getDependencyVersionMavenProperties() {
            StringBuilder sb = new StringBuilder();
            for (Dependency dep : component.getDependencies()) {
                sb.append(" ");
                sb.append("-D" + dep.getProperty() + "=\"${" + getVersionEnvVarName(dep.getName()) + "}\"");
            }
            return sb.toString();
        }

        public Map<String, String> createEnv() {
            return Collections.emptyMap();
        }
    }

    private class DefaultComponentJobContext extends ComponentJobContext {
        public DefaultComponentJobContext(Component component) {
            super(component);
        }

        @Override
        public String getJobName() {
            return component.getName();
        }

        @Override
        List<Map<String, Object>> createBuildSteps() {
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
    }

    private class ConfiguredComponentJobContext extends ComponentJobContext {
        private final JobConfig jobConfig;

        public ConfiguredComponentJobContext(Component component, JobConfig jobConfig) {
            super(component);
            this.jobConfig = jobConfig;
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
        List<Map<String, Object>> createBuildSteps() {
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
            return jobConfig.getJobEnv();
        }
    }
}
