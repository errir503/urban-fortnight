package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;

public class DriverWorkarounds {
    public static void init() {
        DriverWorkarounds.Issue1486.apply();
    }

    private static class Issue1486 {
        private static final String URL = "https://github.com/CaffeineMC/sodium-fabric/issues/1486";

        private static boolean isCurrentDriverApplicable() {
            var operatingSystem = Util.getOperatingSystem();

            if (operatingSystem == Util.OperatingSystem.WINDOWS) {
                var vendor = GL11C.glGetString(GL11C.GL_VENDOR);

                if (vendor != null && vendor.contains("NVIDIA")) {
                    return true;
                }
            }

            return false;
        }

        public static void apply() {
            var options = SodiumClientMod.options();
            var active = isCurrentDriverApplicable();

            if (options.workarounds.issue1486_hideWindowTitleToEvadeNvidiaDrivers != active) {
                options.workarounds.issue1486_hideWindowTitleToEvadeNvidiaDrivers = active;

                try {
                    options.writeChanges();
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't save workaround settings", e);
                }

                if (active) {
                    var choice = TinyFileDialogs.tinyfd_messageBox("Sodium Renderer", """
                        Heads up! The game needs to restart in order to apply a workaround. You will only see this message once.
                        
                        Details: The currently installed version of the NVIDIA graphics driver is incompatible with Sodium.
                        
                        To prevent crashes and other issues, we need to hide the current version of Minecraft from the graphics driver. This will not affect your system configuration.
                        
                        Additional information will be opened in your web browser after closing this message.
                        """, "ok", "info", true);

                    if (choice) {
                        Util.getOperatingSystem()
                                .open(URL);
                    }

                    System.exit(0);
                }
            }
        }
    }
}
