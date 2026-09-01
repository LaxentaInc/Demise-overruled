import net.minecraft.client.main.Main;

import java.io.File;

public class Start {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println("DEBUG ARGS: " + java.util.Arrays.toString(args));
            // forward arguments directly from gradle or ide run configuration
            Main.main(args);
        } else {
            // resolve local minecraft assets directory for standalone ide execution
            String assetsDir = System.getProperty("user.home") + "/AppData/Roaming/.minecraft/assets";
            if (!new File(assetsDir).exists()) {
                assetsDir = "assets";
            }

            Main.main(new String[]{
                    "--version", "demise",
                    "--username", "hesophere",
                    "--accessToken", "0",
                    "--assetsDir", assetsDir,
                    "--assetIndex", "1.8",
                    "--userProperties", "{}"
            });
        }
    }
}