package wtf.demise.utils.render.shader.impl;

import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.render.VideoBackground;

public class MainMenu implements InstanceAccess {
    public static void draw(long initTime) {
        // render live animated wallpaper video background covering the full screen
        VideoBackground.draw();
    }
}
