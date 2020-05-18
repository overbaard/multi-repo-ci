package org.overbaard.ci.multi.repo;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BashUtils {
    static String createDirectoryIfNotExist(String directoryName) {
        return "mkdir -p " + directoryName + "\n";
    }
}
