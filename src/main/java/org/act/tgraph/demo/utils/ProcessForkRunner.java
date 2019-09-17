package org.act.tgraph.demo.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessForkRunner {
    private String cmd;
    private String workingDirectory;
    private List<String> argList = new ArrayList<>();
    private Map<String, String> envMap = new HashMap<>();

    public ProcessForkRunner(String cmd, String workingDirectory) {
        this.cmd = cmd;
        this.workingDirectory = workingDirectory;
    }

    public String getCmd() {
        return cmd;
    }

    public ProcessForkRunner addArg(String argument) {
        this.argList.add(argument);
        return this;
    }

    public Process startProcess() throws IOException {
        List<String> argumentsList = new ArrayList<>();
        argumentsList.add(this.cmd);
        argumentsList.addAll(argList);

        ProcessBuilder processBuilder = new ProcessBuilder(argumentsList.toArray(new String[0]));
        processBuilder.environment().putAll(envMap);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(this.workingDirectory));
        return processBuilder.start();
    }

    public void addEnv(String key, String value) {
        envMap.put(key, value);
    }
}
