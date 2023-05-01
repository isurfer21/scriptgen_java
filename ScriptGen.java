import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ScriptGen {
    private static final String VERSION = "1.0.0";
    private static final String HELP = "Usage: java ScriptGen [options]\n\n" +
            "Options:\n" +
            "  -i, --interpreter INTERPRETER   Specify the interpreter to use\n" +
            "  -s, --script-path SCRIPT_PATH   Specify the path to the script\n" +
            "  -h, --help                      Show this help message\n" +
            "  -v, --version                   Show version information\n\n"+
            "Examples:\n" +
            "  java ScriptGen -h                            View help menu\n" +
            "  java ScriptGen -v                            View version info\n" +
            "  java ScriptGen -i node -s sample.js          Using Node as the interpreter\n" +
            "  java ScriptGen -i python -s sample.py        Using Python as the interpreter\n" +
            "  java ScriptGen -i 'java -jar' -s sample.jar  Using Java as the interpreter\n\n" +
            "Note: The --interpreter and --script options must be used together.\n";

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        if (options.containsKey("help")) {
            System.out.println(HELP);
        } else if (options.containsKey("version")) {
            System.out.println("ScriptGen (v" + VERSION + ")");
        } else if (options.containsKey("interpreter") && options.containsKey("script-path")) {
            String interpreter = options.get("interpreter");
            String scriptPath = options.get("script-path");
            String scriptName = baseName(fileName(scriptPath));
            createFiles(interpreter, scriptPath, scriptName);
        } else {
            System.err.println("Error: Missing required options");
            System.err.println(HELP);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                case "--interpreter":
                    options.put("interpreter", args[++i]);
                    break;
                case "-s":
                case "--script-path":
                    options.put("script-path", args[++i]);
                    break;
                case "-h":
                case "--help":
                    options.put("help", "");
                    break;
                case "-v":
                case "--version":
                    options.put("version", "");
                    break;
                default:
                    System.err.println("Error: Unknown option: " + args[i]);
                    System.err.println(HELP);
                    System.exit(1);
            }
        }
        return options;
    }

    private static String fileName(String scriptPath) {
        int index = scriptPath.lastIndexOf('/');
        if (index == -1) {
            index = scriptPath.lastIndexOf('\\');
        }
        if (index == -1) {
            return scriptPath;
        } else {
            return scriptPath.substring(index + 1);
        }
    }

    private static String baseName(String filename) {
        String fname = filename;
        int pos = fname.lastIndexOf(".");
        if (pos > 0) {
            fname = fname.substring(0, pos);
        }
        return fname;
    }

    private static void createFiles(String interpreter, String scriptPath, String scriptName) throws IOException {
        String cmdContent = "@ECHO off \nSETLOCAL \nSET dp0=%~dp0 \n \nSET \"_prog=" + interpreter + "\" \nSET PATHEXT=%PATHEXT:;." + interpreter + ";=;% \n \nENDLOCAL & GOTO #_undefined_# 2>NUL || title %COMSPEC% & \"%_prog%\"  \"%dp0%\\" + scriptPath + "\" %*";
        Files.write(Paths.get(scriptName + ".cmd"), cmdContent.getBytes());

        String ps1Content = "#!/usr/bin/env pwsh \n$basedir=Split-Path $MyInvocation.MyCommand.Definition -Parent \n \n$exe=\"\" \nif ($PSVersionTable.PSVersion -lt \"6.0\" -or $IsWindows) { \n  # Fix case when both the Windows and Linux builds of Node \n  # are installed in the same directory \n  $exe=\".exe\" \n} \n$ret=0 \n \n# Support pipeline input \nif ($MyInvocation.ExpectingInput) { \n  $input | & \"" + interpreter + "$exe\"  \"$basedir/" + scriptPath + "\" $args \n} else { \n  & \"" + interpreter + "$exe\"  \"$basedir/" + scriptPath + "\" $args \n} \n$ret=$LASTEXITCODE \n \nexit $ret \n";
        Files.write(Paths.get(scriptName + ".ps1"), ps1Content.getBytes());

        String shContent = "#!/bin/sh \nbasedir=$(dirname \"$(echo \"$0\" | sed -e 's,\\,/,g')\") \n \ncase `uname` in \n    *CYGWIN*|*MINGW*|*MSYS*) basedir=`cygpath -w \"$basedir\"`;; \nesac \n \nexec " + interpreter + "  \"$basedir/" + scriptPath + "\" \"$@\" \n";
        Files.write(Paths.get(scriptName + ".sh"), shContent.getBytes());
    }
}
