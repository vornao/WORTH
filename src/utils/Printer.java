package utils;

public class Printer {
    /** utility to print fancy ANSI terminal colors. */
    public static void println(String message, String color){
        System.out.println(Const.Colors.get(color) + message + Const.Colors.get("reset"));
    }
    public static void print(String message, String color){
        System.out.print(Const.Colors.get(color) + message + Const.Colors.get("reset"));
    }

}
