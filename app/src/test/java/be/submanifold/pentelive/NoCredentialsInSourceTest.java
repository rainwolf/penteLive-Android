package be.submanifold.pentelive;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Regression guard: no request in the app source may carry the name2/password2 credentials
 * in a URL or a hand-built body, nor build a Cookie header by hand. The shared cookie store
 * (net.SharedCookies) and net.AuthedHttp authenticate the requests instead.
 */
public class NoCredentialsInSourceTest {

    private static final String[] FORBIDDEN = {
            "password2=", "name2=", "writeCreds", "&password=", "setRequestProperty(\"Cookie\"",
    };
    /** Only redacts these parameters from logs; it sends nothing. */
    private static final String EXEMPT = "RedactingLog.java";

    @Test
    public void mainSourceSendsNoCredentialsInUrlsOrHandBuiltCookies() throws IOException {
        // Local unit tests run with the module directory (app/) as the working directory.
        File root = new File("src/main/java");
        assertTrue("source root not found: " + root.getAbsolutePath(), root.isDirectory());
        List<String> hits = new ArrayList<>();
        scan(root, hits);
        assertTrue("credentials or hand-built Cookie headers in source:\n" + String.join("\n", hits),
                hits.isEmpty());
    }

    private static void scan(File dir, List<String> hits) throws IOException {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scan(file, hits);
            } else if (file.getName().endsWith(".java") && !file.getName().equals(EXEMPT)) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    for (String forbidden : FORBIDDEN) {
                        if (lines.get(i).contains(forbidden)) {
                            hits.add(file.getPath() + ":" + (i + 1) + ": " + lines.get(i).trim());
                        }
                    }
                }
            }
        }
    }
}
