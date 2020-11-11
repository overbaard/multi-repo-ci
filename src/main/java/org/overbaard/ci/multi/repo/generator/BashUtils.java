package org.overbaard.ci.multi.repo.generator;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BashUtils {
    static String createDirectoryIfNotExist(String directoryName) {
        return "mkdir -p " + directoryName + "\n";
    }

    static String setEnvVar(String envVar, String value) {
        return String.format("echo \"%s=%s\" >> $GITHUB_ENV\n", envVar, value);
    }

}
