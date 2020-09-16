package org.overbaard.ci.multi.repo.config;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.overbaard.ci.multi.repo.config.repo.RepoConfig;
import org.overbaard.ci.multi.repo.config.repo.RepoConfigParser;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RepoConfigParserTest {

    @Test
    public void testParseNoConfig() throws Exception {
        RepoConfig repoConfig = RepoConfigParser.create(Paths.get("not/there/config.yml")).parse();
        Assert.assertNotNull(repoConfig);

        Assert.assertNotNull(repoConfig.getEnv());
        Assert.assertEquals(0, repoConfig.getEnv().size());

        Assert.assertNull(repoConfig.getJavaVersion());
        Assert.assertTrue(repoConfig.isCommentsReporting());
        Assert.assertNull(repoConfig.getSuccessLabel());
        Assert.assertNull(repoConfig.getFailureLabel());
    }

    @Test
    public void testParseYaml() throws Exception {
        URL url = this.getClass().getResource("repo-config-test.yml");
        Path path = Paths.get(url.toURI());
        RepoConfig repoConfig = RepoConfigParser.create(path).parse();

        Assert.assertNotNull(repoConfig);
        Assert.assertNotNull(repoConfig.getEnv());
        Assert.assertEquals(2, repoConfig.getEnv().size());
        Assert.assertEquals("abc def", repoConfig.getEnv().get("OPT"));
        Assert.assertEquals("Test", repoConfig.getEnv().get("MY_VAR"));

        Assert.assertEquals("14", repoConfig.getJavaVersion());
        Assert.assertFalse(repoConfig.isCommentsReporting());
        Assert.assertEquals("Pass-Label", repoConfig.getSuccessLabel());
        Assert.assertEquals("Fail-Label", repoConfig.getFailureLabel());

    }
}
