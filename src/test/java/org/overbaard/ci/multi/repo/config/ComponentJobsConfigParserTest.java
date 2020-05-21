package org.overbaard.ci.multi.repo.config;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfig;
import org.overbaard.ci.multi.repo.config.component.ComponentJobsConfigParser;
import org.overbaard.ci.multi.repo.config.component.JobConfig;
import org.overbaard.ci.multi.repo.config.component.JobRunElementConfig;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfigParserTest {
    @Test
    public void testParseYaml() throws Exception {
        URL url = this.getClass().getResource("component-job-test.yml");
        Path path = Paths.get(url.toURI());
        ComponentJobsConfig config = ComponentJobsConfigParser.create(path).parse();

        Assert.assertNotNull(config);

        Set<String> exportedJobs = config.getExportedJobs();
        Assert.assertEquals(1, exportedJobs.size());
        Assert.assertTrue(exportedJobs.contains("component-job-test-build"));

        List<JobConfig> jobs = config.getJobs();
        Assert.assertEquals(2, jobs.size());

        JobConfig build = jobs.get(0);
        Assert.assertEquals("component-job-test-build", build.getName());
        Assert.assertEquals(0, build.getNeeds().size());
        Map<String, String> buildEnv = build.getJobEnv();
        List<String> buildEnvKeys = new ArrayList<>(buildEnv.keySet());
        Assert.assertEquals(Arrays.asList("P1", "P2", "P3", "P4"), buildEnvKeys);
        Assert.assertEquals("1", buildEnv.get("P1"));
        Assert.assertEquals("22-22", buildEnv.get("P2"));
        Assert.assertEquals("333", buildEnv.get("P3"));
        Assert.assertEquals("4444", buildEnv.get("P4"));
        List<JobRunElementConfig> buildRun = build.getRunElements();
        Assert.assertEquals(2, buildRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.MVN, buildRun.get(0).getType());
        Assert.assertEquals("install {MAVEN_BUILD_PARAMS}", buildRun.get(0).getCommand());
        Assert.assertEquals(JobRunElementConfig.Type.SHELL, buildRun.get(1).getType());
        Assert.assertEquals("echo hi", buildRun.get(1).getCommand());


        JobConfig ts = jobs.get(1);
        Assert.assertEquals("component-job-test-ts", ts.getName());
        Assert.assertEquals(1, ts.getNeeds().size());
        Assert.assertEquals("component-job-test-build", ts.getNeeds().get(0));
        Map<String, String> tsEnv = ts.getJobEnv();
        Assert.assertEquals(2, tsEnv.size());
        List<String> tsEnvKeys = new ArrayList<>(tsEnv.keySet());
        Assert.assertEquals(Arrays.asList("P1", "P2"), tsEnvKeys);
        Assert.assertEquals("1", tsEnv.get("P1"));
        Assert.assertEquals("22", tsEnv.get("P2"));
        List<JobRunElementConfig> tsRun = ts.getRunElements();
        Assert.assertEquals(1, tsRun.size());
        Assert.assertEquals(JobRunElementConfig.Type.MVN, tsRun.get(0).getType());
        Assert.assertEquals("package -pl tests ${MAVEN_SMOKE_TEST_PARAMS}", tsRun.get(0).getCommand());

    }


}
