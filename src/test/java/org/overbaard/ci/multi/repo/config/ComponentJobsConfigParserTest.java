package org.overbaard.ci.multi.repo.config;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.overbaard.ci.multi.repo.config.component.ComponentEndJobConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.component.ComponentJobConfig;
import org.overbaard.ci.multi.repo.config.component.JobRunElementConfig;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfigParserTest {
    @Test
    public void testParseYamlWithGlobalSettings() throws Exception {
        URL url = this.getClass().getResource("component-job-test-global-settings.yml");
        Path path = Paths.get(url.toURI());
        ComponentJobsConfig config = ComponentJobsConfigParser.create(path).parse();

        Assert.assertNotNull(config);

        String buildJob = config.getBuildJobName();
        Assert.assertEquals("component-job-test-global-settings-build", buildJob);

        List<ComponentJobConfig> jobs = config.getJobs();
        Assert.assertEquals(2, jobs.size());

        ComponentJobConfig build = jobs.get(0);
        Assert.assertTrue(build.isBuildJob());
        Assert.assertFalse(build.isEndJob());
        Assert.assertEquals("component-job-test-global-settings-build", build.getName());
        Assert.assertEquals(0, build.getNeeds().size());
        Assert.assertEquals("13", build.getJavaVersion());
        Map<String, String> buildEnv = build.getJobEnv();
        List<String> buildEnvKeys = new ArrayList<>(buildEnv.keySet());
        Assert.assertEquals(Arrays.asList("P1", "P2", "P3", "P4"), buildEnvKeys);
        Assert.assertEquals("1", buildEnv.get("P1"));
        Assert.assertEquals("22-22", buildEnv.get("P2"));
        Assert.assertEquals("333", buildEnv.get("P3"));
        Assert.assertEquals("4444", buildEnv.get("P4"));
        Assert.assertEquals(Collections.singletonList("Top"), build.getRunsOn());
        List<JobRunElementConfig> buildRun = build.getRunElements();
        Assert.assertEquals(2, buildRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.MVN, buildRun.get(0).getType());
        Assert.assertEquals("install {MAVEN_BUILD_PARAMS}", buildRun.get(0).getCommand());
        Assert.assertEquals(JobRunElementConfig.Type.SHELL, buildRun.get(1).getType());
        Assert.assertEquals("echo hi", buildRun.get(1).getCommand());


        ComponentJobConfig ts = jobs.get(1);
        Assert.assertFalse(ts.isBuildJob());
        Assert.assertFalse(ts.isEndJob());
        Assert.assertEquals("component-job-test-global-settings-ts", ts.getName());
        Assert.assertEquals(1, ts.getNeeds().size());
        Assert.assertEquals("16", ts.getJavaVersion());
        Assert.assertEquals("component-job-test-global-settings-build", ts.getNeeds().get(0));
        Map<String, String> tsEnv = ts.getJobEnv();
        Assert.assertEquals(2, tsEnv.size());
        List<String> tsEnvKeys = new ArrayList<>(tsEnv.keySet());
        Assert.assertEquals(Arrays.asList("P1", "P2"), tsEnvKeys);
        Assert.assertEquals("1", tsEnv.get("P1"));
        Assert.assertEquals("22", tsEnv.get("P2"));
        Assert.assertEquals(Arrays.asList("l1", "l2"), ts.getRunsOn());
        List<JobRunElementConfig> tsRun = ts.getRunElements();
        Assert.assertEquals(1, tsRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.MVN, tsRun.get(0).getType());
        Assert.assertEquals("package -pl tests ${MAVEN_SMOKE_TEST_PARAMS}", tsRun.get(0).getCommand());

        ComponentEndJobConfig endJob = config.getEndJob();
        Assert.assertFalse(endJob.isBuildJob());
        Assert.assertTrue(endJob.isEndJob());
        Assert.assertEquals(Collections.singletonList("Top"), endJob.getRunsOn());
        Assert.assertEquals("13", endJob.getJavaVersion());
        Assert.assertEquals(Arrays.asList("P1", "P2"), new ArrayList<>(endJob.getJobEnv().keySet()));
        Assert.assertEquals("1", endJob.getJobEnv().get("P1"));
        Assert.assertEquals("end", endJob.getJobEnv().get("P2"));

    }

    @Test
    public void testParseYamlWithNoGlobalSettings() throws Exception {
        URL url = this.getClass().getResource("component-job-test-no-global-settings.yml");
        Path path = Paths.get(url.toURI());
        ComponentJobsConfig config = ComponentJobsConfigParser.create(path).parse();

        Assert.assertNotNull(config);

        String buildJob = config.getBuildJobName();
        Assert.assertEquals("component-job-test-no-global-settings-build", buildJob);

        List<ComponentJobConfig> jobs = config.getJobs();
        Assert.assertEquals(2, jobs.size());

        ComponentJobConfig build = jobs.get(0);
        Assert.assertTrue(build.isBuildJob());
        Assert.assertFalse(build.isEndJob());
        Assert.assertEquals("component-job-test-no-global-settings-build", build.getName());
        Assert.assertEquals(0, build.getNeeds().size());
        Assert.assertNull(build.getJavaVersion());
        Map<String, String> buildEnv = build.getJobEnv();
        List<String> buildEnvKeys = new ArrayList<>(buildEnv.keySet());
        Assert.assertEquals(0, buildEnvKeys.size());
        Assert.assertNull(build.getRunsOn());
        List<JobRunElementConfig> buildRun = build.getRunElements();
        Assert.assertEquals(1, buildRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.SHELL, buildRun.get(0).getType());
        Assert.assertEquals("echo hi", buildRun.get(0).getCommand());


        ComponentJobConfig ts = jobs.get(1);
        Assert.assertFalse(ts.isBuildJob());
        Assert.assertFalse(ts.isEndJob());
        Assert.assertEquals("component-job-test-no-global-settings-ts", ts.getName());
        Assert.assertEquals(1, ts.getNeeds().size());
        Assert.assertEquals("16", ts.getJavaVersion());
        Assert.assertEquals("component-job-test-no-global-settings-build", ts.getNeeds().get(0));
        Map<String, String> tsEnv = ts.getJobEnv();
        Assert.assertEquals(0, tsEnv.size());
        Assert.assertEquals(Arrays.asList("l2", "l3"), ts.getRunsOn());
        List<JobRunElementConfig> tsRun = ts.getRunElements();
        Assert.assertEquals(1, tsRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.MVN, tsRun.get(0).getType());
        Assert.assertEquals("package -pl tests ${MAVEN_SMOKE_TEST_PARAMS}", tsRun.get(0).getCommand());

        ComponentEndJobConfig endJob = config.getEndJob();
        Assert.assertFalse(endJob.isBuildJob());
        Assert.assertTrue(endJob.isEndJob());
        Assert.assertEquals(Collections.singletonList("l3"), endJob.getRunsOn());
        Assert.assertEquals("11", endJob.getJavaVersion());
        Assert.assertEquals(0, endJob.getJobEnv().size());
    }
}
