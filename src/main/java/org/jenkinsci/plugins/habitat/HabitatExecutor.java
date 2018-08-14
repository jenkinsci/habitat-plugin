package org.jenkinsci.plugins.habitat;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class HabitatExecutor extends Builder implements SimpleBuildStep {

    private String task;
    private String channel;
    private String directory;
    private String lastBuildFile;
    private String artifact;
    private String origin;
    private String bldrUrl;
    private String authToken;
    private String format; 
    private String searchString;
    private String command;
    private String path;
    private String binary;

    private VirtualChannel slave;


    @DataBoundConstructor
    public HabitatExecutor(
            String task, String directory, String artifact, String channel, String origin, 
	    String bldrUrl, String authToken, String lastBuildFile, String format, 
	    String searchString, String command, String binary, String path
    ) {
        this.setTask(task);
        this.setArtifact(artifact);
        this.setDirectory(directory);
        this.setChannel(channel);
        this.setOrigin(origin);
        this.setBldrUrl(bldrUrl);
        this.setAuthToken(authToken);
        this.setLastBuildFile(lastBuildFile);
        this.setFormat(format);
        this.setsearchString(searchString);
        this.setcommand(command);
	this.setpath(path);
	this.setbinary(binary);
    }

    public String getLastBuildFile() {
        return lastBuildFile;
    }

    @DataBoundSetter
    public void setLastBuildFile(String lastBuildFile) {
        this.lastBuildFile = lastBuildFile;
    }

    public String getpath() {
        return path;
    }

    @DataBoundSetter
    public void setpath(String path) {
        this.path = path;
    }

    public String getbinary() {
        return binary;
    }

    @DataBoundSetter
    public void setbinary(String binary) {
        this.binary = binary;
    }

    public String getcommand() {
        return command;
    }

    @DataBoundSetter
    public void setcommand(String command) {
        this.command = command;
    }

    public String getOrigin() {
        return origin;
    }

    @DataBoundSetter
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getsearchString() {
        return searchString;
    }

    @DataBoundSetter
    public void setsearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getBldrUrl() {
        return bldrUrl;
    }

    @DataBoundSetter
    public void setBldrUrl(String bldrUrl) {
        this.bldrUrl = bldrUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    @DataBoundSetter
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getArtifact() {
        return artifact;
    }

    @DataBoundSetter
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getChannel() {
        return channel;
    }

    @DataBoundSetter
    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getFormat() {
	return format; 
    }

    @DataBoundSetter
    public void setFormat(String format) { 
        this.format = format;
    }

    public String getTask() {
        return task;
    }

    @DataBoundSetter
    public void setTask(String task) {
        this.task = task;
    }

    public String getDirectory() {
        return directory;
    }

    @DataBoundSetter
    public void setDirectory(String directory) {
        this.directory = directory;
    }


    private String command(PrintStream log) throws Exception {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");

        switch (this.getTask().trim()) {
            case "build":
                return this.buildCommand(isWindows);
            case "promote":
                return this.promoteCommand(isWindows, log);
            case "demote":
                return this.demoteCommand(isWindows, log);
            case "upload":
                return this.uploadCommand(isWindows, log);
            case "channels":
                return this.channelsCommand(isWindows, log);
            case "export":
                return this.exportCommand(isWindows, log);
            case "search":
                return this.searchCommand(isWindows, log);
            case "config":
                return this.configCommand(isWindows, log);
            case "exec":
                return this.execCommand(isWindows, log);
            case "binlink":
                return this.binlinkCommand(isWindows, log);
            default:
                throw new Exception("Task not yet implemented");
        }
    }


    private String buildCommand(boolean isWindows) {
        if (isWindows) {
            return String.format("hab studio build %s", this.getDirectory());
        } else {
            return String.format("hab studio build %s", this.getDirectory());
        }
    }


    private String exportCommand(boolean isWindows, PrintStream log) throws Exception {

        //declaring my list of acceptable values for format
	List<String> possibleFormats = Arrays.asList("aci", "cf", "docker", "kubernetes", "mesos", "tar");
	String myformat = this.getFormat();
	if (!possibleFormats.contains(myformat)) {
	    throw new Exception("Format entered is not valid! \n Valid formats: aci, cf, docker, kubernetes, mesos, tar"); 
        }
	
	String lastPackage = this.getLatestPackage(log);
        if (!this.slave.call(new FileExistence(lastPackage))) {
            throw new Exception("Could not find hart file " + lastPackage);
        }

        log.println("Exporting " + lastPackage + " to " + myformat);

        if (isWindows) {
            return String.format("hab pkg export %s %s", myformat, lastPackage);
        } else {
            return String.format("hab pkg export %s %s", myformat, lastPackage);
        }
    }


    private String binlinkCommand(boolean isWindows, PrintStream log) throws Exception {

        String path = this.getpath();
        String binary = this.getbinary();
        String pkgIdent = this.getArtifact();

        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }


        if (binary == null) {
            throw new Exception("You must provide a binary to binlink to");
        }
	
        if (path == null) {
            if (isWindows) {
                return String.format("hab pkg binlink %s %s", pkgIdent, binary);
            } else {
                return String.format("hab pkg binlink %s %s", pkgIdent, binary);
            }
        } else {
            if (isWindows) {
                return String.format("hab pkg binlink -d %s %s %s", path, pkgIdent, binary);
            } else {
                return String.format("hab pkg binlink -d %s %s %s", path, pkgIdent, binary);
            }
        }
    }


    private String searchCommand(boolean isWindows, PrintStream log) throws Exception {

        //declaring my list of acceptable values for format
	String searchString = this.getsearchString();
	if (searchString == null) {
	    throw new Exception("Please enter a string for us to search for!");
        }

        if (isWindows) {
            return String.format("hab pkg search %s", searchString);
        } else {
            return String.format("hab pkg search %s", searchString);
        }
    }


    private String promoteCommand(boolean isWindows, PrintStream log) throws Exception {
        String pkgIdent = this.getArtifact();
        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }

        String channel = this.getChannel();
        if (channel == null) {
            throw new Exception("Channel cannot be null");
        }

        if (isWindows) {
            return String.format("hab pkg promote %s %s", pkgIdent, channel);
        } else {
            return String.format("hab pkg promote %s %s", pkgIdent, channel);
        }
    }


    private String channelsCommand(boolean isWindows, PrintStream log) throws Exception {
        String pkgIdent = this.getArtifact();
        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }

        if (isWindows) {
            return String.format("hab pkg channels %s", pkgIdent);
        } else {
            return String.format("hab pkg channels %s", pkgIdent);
        }
    }


    private String configCommand(boolean isWindows, PrintStream log) throws Exception {
        String pkgIdent = this.getArtifact();
        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }

        if (isWindows) {
            return String.format("hab pkg config %s", pkgIdent);
        } else {
            return String.format("hab pkg config %s", pkgIdent);
        }
    }


    private String execCommand(boolean isWindows, PrintStream log) throws Exception {
        String pkgIdent = this.getArtifact();
        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }

        String command = this.getcommand();
        if (command == null) {
            throw new Exception("Command cannot be null");
        }

        if (isWindows) {
            return String.format("hab pkg exec %s %s", pkgIdent, command);
        } else {
            return String.format("hab pkg exec %s %s", pkgIdent, command);
        }
    }


    private String demoteCommand(boolean isWindows, PrintStream log) throws Exception {
        String pkgIdent = this.getArtifact();
        if (pkgIdent == null) {
            LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
            pkgIdent = lastBuild.getIdent();
        }

        String channel = this.getChannel();
        if (channel == null) {
            throw new Exception("Channel cannot be null");
        }

        if (isWindows) {
            return String.format("hab pkg demote %s %s", pkgIdent, channel);
        } else {
            return String.format("hab pkg demote %s %s", pkgIdent, channel);
        }
    }


    private String uploadCommand(boolean isWindows, PrintStream log) throws Exception {
        String lastPackage = this.getLatestPackage(log);
        log.println("Last Package: " + lastPackage);
        if (!this.slave.call(new FileExistence(lastPackage))) {
            throw new Exception("Could not find hart " + lastPackage);
        }
        if (isWindows) {
            return String.format("hab pkg upload %s", lastPackage);
        } else {
            return String.format("hab pkg upload %s", lastPackage);
        }
    }

    private String getLatestPackage(PrintStream log) throws Exception {
        LastBuild lastBuild = this.slave.call(new LastBuildSlaveRetriever(this.lastBuildPath(log)));
        String artifact = lastBuild.getArtifact();
        log.println("Artifact " + artifact + " found in: " + this.getAbsolutePath(this.getDirectory()));
        return this.slave.call(new FileConstructor(Arrays.asList(this.getAbsolutePath(this.getDirectory()), artifact)));
    }

    private String getAbsolutePath(String file) throws IOException, InterruptedException {
        return this.slave.call(new FilePathFinder(file));
    }

    private String lastBuildPath(PrintStream log) throws Exception {
        if (this.getLastBuildFile() != null) {
            if (this.getDirectory() == null) {
                String dir = this.getLastBuildFile();
                dir = dir.replace("last_build.env", "");
                log.println("Setting Directory: " + dir);
                this.setDirectory(dir);
            }
            return this.getLastBuildFile();
        } else {
            return this.slave.call(new FileFinder(this.getAbsolutePath(this.getDirectory()), "last_build.env"));
        }
    }


    private Map<String, String> getEnv(PrintStream log) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("HAB_NOCOLORING", "true");

        if (this.getOrigin() == null) {
            if (this.getTask().equalsIgnoreCase("build")) {
                throw new Exception("cannot build without specifying an origin");
            }
        } else {
            env.put("HAB_ORIGIN", this.getOrigin());
        }

        if (this.getAuthToken() == null) {
            if (this.needsAuthToken()) {
                throw new Exception("this task needs an auth token to be set");
            }
        } else {
            env.put("HAB_AUTH_TOKEN", this.getAuthToken());
        }

        if (this.getBldrUrl() != null) {
            env.put("HAB_BLDR_URL", this.getBldrUrl());
        }

        return env;
    }

    private boolean needsAuthToken() {
        return this.getTask().equalsIgnoreCase("upload") || this.getTask().equalsIgnoreCase("promote");
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        Map<String, String> otherEnvs = build.getEnvironment(TaskListener.NULL);

        Proc proc = null;
        int exitCode = -1;
        try {
            Map<String, String> env = this.getEnv(log);
            env.putAll(otherEnvs);

            log.println("Build Environment Variables");
            env.forEach((k, v) -> log.println(k + ": " + v));
            this.slave = launcher.getChannel();
            Launcher.ProcStarter starter = launcher.launch().pwd(workspace).envs(env).cmdAsSingleString(this.command(log));
            starter.stdout(log);
            proc = launcher.launch(starter);
            exitCode = proc.join();
        } catch (Exception e) {
            log.println(e.getMessage());
        }
        if (exitCode != 0) {
            throw new IOException("Failed to execute " + this.getTask());
        }

    }

    @Symbol("habitat")
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Habitat Executor";
        }
    }
}
