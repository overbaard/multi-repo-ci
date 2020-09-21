package org.overbaard.ci.multi.repo.config.component;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.overbaard.ci.multi.repo.config.BaseParser;
import org.yaml.snakeyaml.Yaml;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfigParser extends BaseParser {
    private final Path yamlFile;
    private final String componentName;
    private final Set<String> jobKeys = new HashSet<>();

    private ComponentJobsConfigParser(Path yamlFile, String componentName) {
        this.yamlFile = yamlFile;
        this.componentName = componentName;
    }

    public static ComponentJobsConfigParser create(Path yamlFile) {
        String fileName = yamlFile.getFileName().toString();
        fileName = fileName.substring(0, fileName.indexOf("."));
        return new ComponentJobsConfigParser(yamlFile, fileName);
    }

    public ComponentJobsConfig parse() throws Exception {
        System.out.println("Parsing component job config: " + yamlFile);
        Map<String, Object> input = null;
        try {
            Yaml yaml = new Yaml();
            input = yaml.load(new BufferedInputStream(new FileInputStream(yamlFile.toFile())));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Object envInput = input.remove("env");
        Object jobsInput = input.remove("jobs");
        Object buildJobInput = input.remove("build-job");
        Object javaVersionInput = input.remove("java-version");
        Object endJobInput = input.remove("end-job");
        if (input.size() > 0) {
            throw new IllegalStateException("Unknown entries: " + input.keySet());
        }
        if (jobsInput == null) {
            throw new IllegalStateException("No 'jobs' entry");
        }
        if (buildJobInput == null) {
            throw new IllegalStateException("No 'build-job' entry");
        }

        if (buildJobInput instanceof String == false) {
            throw new IllegalStateException("'build-job' entry is not a String: " + buildJobInput);
        }
        String buildJob = (String) buildJobInput;

        Map<String, String> mainEnv = parseEnv(envInput);
        String javaVersion = parseJavaVersion(javaVersionInput);

        Map<String, ComponentJobConfig> jobs = parseJobs(buildJob, javaVersion, mainEnv, jobsInput);
        if (jobs.size() == 0) {
            throw new IllegalStateException("'jobs' entry is empty");
        }

        if (jobs.get(buildJob) == null) {
            throw new IllegalStateException("No job called '" + buildJob +
                    "' referenced by 'build-job' entry: " + buildJob);
        }

        Map<String, Object> endJob = preParseEndJob(endJobInput);
        ComponentEndJobConfig endJobConfig = parseEndJob(endJob, jobs, javaVersion, mainEnv);

        return new ComponentJobsConfig(componentName, createJobName(buildJob), new ArrayList<>(jobs.values()), endJobConfig);
    }

    private Map<String, ComponentJobConfig> parseJobs(String buildJob, String javaVersion, Map<String, String> mainEnv, Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, ComponentJobConfig> jobs = new LinkedHashMap<>();
        Map<String, Object> map = (Map)input;
        for (String key : map.keySet()) {
            ComponentJobConfig job = parseJob(buildJob, javaVersion, mainEnv, key, map.get(key));
            jobs.put(key, job);
        }
        return jobs;
    }

    private ComponentJobConfig parseJob(String buildJob, String javaVersion, Map<String, String> mainEnv, String jobKey, Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, Object> map = (Map)input;
        String name = createJobName(jobKey);
        Map<String, String> jobEnv = new HashMap<>();
        List<String> needs = new ArrayList<>();
        List<JobRunElementConfig> runElements = null;
        for (String key : map.keySet()) {
            switch (key) {
                case "env": {
                    jobEnv = parseEnv(map.get(key));
                    break;
                }
                case "needs":
                    needs = parseNeeds(map.get(key));
                    break;
                case "run": {
                    runElements = parseJobRunElements(map.get(key));
                    break;
                }
                case "java-version": {
                    javaVersion = parseJavaVersion(map.get(key));
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown entry: " + key);
            }
        }
        jobKeys.add(jobKey);


        //Merge the main env entries and the job env entries, making sure the job ones come last
        jobEnv = mergeEnv(mainEnv, jobEnv);
        if (runElements == null) {
            throw new IllegalStateException("Null 'run'");
        }


        return new ComponentJobConfig(name, jobKey.equals(buildJob), jobEnv, javaVersion, needs, runElements);
    }

    private ComponentEndJobConfig parseEndJob(Map<String, Object> map, Map<String, ComponentJobConfig> jobs, String javaVersion, Map<String, String> mainEnv) {
        if (map == null) {
            return null;
        }
        String name = createJobName("ob-end-job-" + componentName);
        Map<String, String> jobEnv = new HashMap<>();
        List<String> needs = new ArrayList<>();
        for (ComponentJobConfig job : jobs.values()) {
           needs.add(job.getName());
        }

        List<Map<String, Object>> steps = null;
        String ifCondition = null;
        for (String key : map.keySet()) {
            switch (key) {
                case "env": {
                    jobEnv = parseEnv(map.get(key));
                    break;
                }
                case "if": {
                    Object o = map.get(key);
                    if (!(o instanceof String)) {
                        throw new IllegalStateException("'if' must be a string");
                    }
                    ifCondition = (String) o;
                }
                break;
                case "steps": {
                    steps = (List<Map<String, Object>>)map.get(key);
                    break;
                }
                case "java-version": {
                    javaVersion = parseJavaVersion(map.get(key));
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown entry: " + key);
            }
        }


        //Merge the main env entries and the job env entries, making sure the job ones come last
        jobEnv = mergeEnv(mainEnv, jobEnv);
        if (steps == null || steps.size() == 0) {
            throw new IllegalStateException("No 'steps'");
        }


        return new ComponentEndJobConfig(name, jobEnv, javaVersion, needs, ifCondition, steps);
    }

    private Map<String, String> mergeEnv(Map<String, String> mainEnv, Map<String, String> jobEnv) {
        Map<String, String> merge = new LinkedHashMap<>();
        merge.putAll(mainEnv);
        merge.putAll(jobEnv);
        return merge;
    }

    private List<String> parseNeeds(Object input) {
        if (input instanceof List == false) {
            throw new IllegalStateException("Not an instance of List");
        }
        List<Object> list = (List) input;
        List<String> needs = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String == false) {
                throw new IllegalStateException(o + " is not a string");
            }
            String jobKey = (String) o;
            if (!jobKeys.contains(jobKey)) {
                throw new IllegalStateException("Unknown job '" + jobKey + "' in 'needs'. If it is in the file, " +
                        "make sure to add them in the correct order for resolution to happen.");
            }
            needs.add(createJobName(jobKey));
        }
        return needs;
    }

    private List<JobRunElementConfig> parseJobRunElements(Object input) {
        if (input instanceof List == false) {
            throw new IllegalStateException("Not an instance of List");
        }
        List<Object> list = (List)input;
        List<JobRunElementConfig> jobRunElements = new ArrayList<>();
        for (Object o : list) {
            JobRunElementConfig jobRunElement = parseJobRunElement(o);
            jobRunElements.add(jobRunElement);
        }
        return jobRunElements;
    }

    private JobRunElementConfig parseJobRunElement(Object input) {
        if (input instanceof Map == false) {
            throw new IllegalStateException("Not an instance of Map");
        }
        Map<String, String> map = (Map) input;
        if (map.size() != 1) {
            throw new IllegalStateException("Only one entry allowed per 'run' element");
        }
        String key = map.keySet().iterator().next();
        JobRunElementConfig.Type type = null;
        switch (key) {
            case "mvn": {
                type = JobRunElementConfig.Type.MVN;
                break;
            }
            case "shell":
                type = JobRunElementConfig.Type.SHELL;
                break;
            default:
                throw new IllegalStateException("Unknown type for 'run' element");
        }

        Object value = map.get(key);
        if (value instanceof String == false) {
            throw new IllegalStateException(value + " is not a String");
        }
        String command = (String) value;
        if (command.trim().startsWith("mvn ")) {
            if (type == JobRunElementConfig.Type.SHELL) {
                throw new IllegalStateException("If you want to run mvn commands, that should happen in a 'mvn' run entry");
            } else if (type == JobRunElementConfig.Type.MVN) {
                throw new IllegalStateException("In a 'mvn' run entry, don't include the mvn command. e.g.\n" +
                        "\t\t - mvn: install\n" +
                        "gets translated to:\n" +
                        "\t\t $mvn install");
            }
        }
        return new JobRunElementConfig(type, command);
    }

    private String createJobName(String jobKey) {
        return componentName + "-" + jobKey;
    }
}
