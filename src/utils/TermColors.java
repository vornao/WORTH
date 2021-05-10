package utils;

import java.util.HashMap;

public class TermColors {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final HashMap<String, String> Colors = new HashMap<String, String>() {
        {
            put("black", ANSI_BLACK);
            put("reset", ANSI_RESET);
            put("red", ANSI_RED);
            put("green", ANSI_GREEN);
            put("yellow", ANSI_YELLOW);
            put("cyan", ANSI_CYAN);
            put("blue", ANSI_BLUE);
            put("purple", ANSI_PURPLE);
            put("white", ANSI_WHITE);
        }
    };
}
