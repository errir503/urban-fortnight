package net.caffeinemc.sodium;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jemalloc.JEmalloc;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("Sodium");
    
    @Override
    public void onPreLaunch() {
        tryLoadRenderdoc();
        checkJemalloc();
    }
    
    private static void tryLoadRenderdoc() {
        if (System.getProperty("sodium.load_renderdoc") != null) {
            LOGGER.info("Loading renderdoc...");
            try {
                System.loadLibrary("renderdoc");
            } catch (Throwable t) {
                LOGGER.warn("Unable to load renderdoc (is it in your path?)", t);
            }
        }
    }

    private static void checkJemalloc() {
        // LWJGL 3.2.3 ships Jemalloc 5.2.0 which seems to be broken on Windows and suffers from critical memory leak problems
        // Using the system allocator prevents memory leaks and other problems
        // See changelog here: https://github.com/jemalloc/jemalloc/releases/tag/5.2.1
        if (Platform.get() == Platform.WINDOWS && isVersionWithinRange(JEmalloc.JEMALLOC_VERSION, "5.0.0", "5.2.0")) {
            LOGGER.info("Switching memory allocators to work around memory leaks present with Jemalloc 5.0.0 through 5.2.0 on Windows...");

            if (!Objects.equals(Configuration.MEMORY_ALLOCATOR.get(), "system")) {
                Configuration.MEMORY_ALLOCATOR.set("system");
            }
        }
    }

    private static boolean isVersionWithinRange(String curStr, String minStr, String maxStr) {
        Version cur, min, max;

        try {
            cur = Version.parse(curStr);
            min = Version.parse(minStr);
            max = Version.parse(maxStr);
        } catch (VersionParsingException e) {
            LOGGER.warn("Unable to parse version string", e);
            return false;
        }

        return cur.compareTo(min) >= 0 && cur.compareTo(max) <= 0;
    }
}
