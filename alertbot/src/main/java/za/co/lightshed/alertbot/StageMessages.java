package za.co.lightshed.alertbot;

import za.co.lightshed.common.StageChange;

/** Turns a stage change into the public message text. */
public final class StageMessages {
    private StageMessages() {}

    public static String format(StageChange c) {
        if (c.current() == 0) {
            return "LightShed update: loadshedding has been suspended (was Stage " + c.previous() + ").";
        }
        if (c.previous() == 0) {
            return "LightShed update: loadshedding is back at Stage " + c.current() + ".";
        }
        String direction = c.current() > c.previous() ? "increased" : "reduced";
        return "LightShed update: loadshedding " + direction + " from Stage " + c.previous()
                + " to Stage " + c.current() + ".";
    }
}
