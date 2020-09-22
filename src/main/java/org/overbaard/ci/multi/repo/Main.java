package org.overbaard.ci.multi.repo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.overbaard.ci.multi.repo.directory.utils.SplitLargeFilesInDirectory;
import org.overbaard.ci.multi.repo.generator.GitHubActionGenerator;
import org.overbaard.ci.multi.repo.log.copy.CopyLogArtifacts;
import org.overbaard.ci.multi.repo.directory.utils.BackupMavenArtifacts;
import org.overbaard.ci.multi.repo.directory.utils.OverlayBackedUpMavenArtifacts;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Main {

    private static final Map<String, ToolCommand> COMMANDS;
    static {
        Map<String, ToolCommand> map = new LinkedHashMap<>();
        map.put(GitHubActionGenerator.Command.NAME, new GitHubActionGenerator.Command());
        map.put(CopyLogArtifacts.Command.NAME, new CopyLogArtifacts.Command());
        map.put(BackupMavenArtifacts.Command.NAME, new BackupMavenArtifacts.Command());
        map.put(OverlayBackedUpMavenArtifacts.Command.NAME, new OverlayBackedUpMavenArtifacts.Command());
        map.put(SplitLargeFilesInDirectory.SplitCommand.NAME, new SplitLargeFilesInDirectory.SplitCommand());
        map.put(SplitLargeFilesInDirectory.MergeCommand.NAME, new SplitLargeFilesInDirectory.MergeCommand());
        COMMANDS = Collections.unmodifiableMap(map);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        String cmd = args[0];
        ToolCommand toolCommand = COMMANDS.get(cmd);
        if (toolCommand == null) {
            System.out.println("Unknown command: " + cmd);
            System.exit(1);
        }

        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        toolCommand.invoke(newArgs);
    }


    private static void usage() throws URISyntaxException {
        Usage usage = new Usage();
        URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();

        for (String cmd : COMMANDS.keySet()) {
            String description = COMMANDS.get(cmd).getDescription();
            usage.addArguments(cmd);
            usage.addInstruction(description);
        }

        String headline = usage.getMainUsageHeadline(url);
        System.out.print(usage.usage(headline));
    }
}
