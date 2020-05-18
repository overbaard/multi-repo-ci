package org.overbaard.ci.multi.repo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.overbaard.ci.multi.repo.generator.GitHubActionGenerator;
import org.overbaard.ci.multi.repo.log.copy.CopyLogArtifacts;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Main {

    private static final Map<String, String> COMMANDS;
    static {
        Map<String, String> map = new HashMap<>();
        map.put(GitHubActionGenerator.GENERATE_WORKFLOW, "Generates a GitHub workflow YAML from the trigger issue input");
        map.put(CopyLogArtifacts.COPY_LOGS, "Copies across the log files to the artifacts");
        COMMANDS = Collections.unmodifiableMap(map);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        String cmd = args[0];
        if (COMMANDS.get(cmd) == null) {
            System.out.println("Unknown command: " + cmd);
            System.exit(1);
        }

        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (cmd) {
            case GitHubActionGenerator.GENERATE_WORKFLOW: {
                GitHubActionGenerator.generate(newArgs);
                break;
            }
            case CopyLogArtifacts.COPY_LOGS: {
                CopyLogArtifacts.copy(newArgs);
                break;
            }
            default:
                System.out.println("Unknown command: " + cmd);
                System.exit(1);
        }

    }


    private static void usage() throws URISyntaxException {
        Usage usage = new Usage();
        URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();

        for (String cmd : COMMANDS.keySet()) {
            String description = COMMANDS.get(cmd);
            usage.addArguments(cmd);
            usage.addInstruction(description);
        }

        String headline = usage.getMainUsageHeadline(url);
        System.out.print(usage.usage(headline));
    }
}
