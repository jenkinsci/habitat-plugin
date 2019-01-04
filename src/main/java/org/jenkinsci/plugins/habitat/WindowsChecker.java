package org.jenkinsci.plugins.habitat;

import jenkins.security.MasterToSlaveCallable;

import java.io.File;

public class WindowsChecker extends MasterToSlaveCallable<Boolean, RuntimeException> {

    public WindowsChecker() { }

    /**
     * Returns Boolean - true if OS is windows.
     */
    @Override
    public Boolean call() throws RuntimeException {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
}
