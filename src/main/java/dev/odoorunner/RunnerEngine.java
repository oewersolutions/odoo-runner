package dev.odoorunner;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.*;
import com.intellij.openapi.project.Project;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/** Process operations use saved definitions, independently of the popup editor selection. */
final class RunnerEngine {
    static final java.util.List<String> SECRETS = java.util.List.of("db_password", "admin_passwd", "smtp_password");
    static Path directory(Project project, Workspace.Runner runner) {
        return Path.of(Objects.requireNonNull(project.getBasePath(), "Open a local project first."), ".odoo-runner", runner.id);
    }
    static CredentialAttributes secretKey(Workspace.Runner runner, String key) {
        return new CredentialAttributes("Odoo Runner / " + runner.id + " / " + key);
    }
    static String mergedAddons(Workspace.Runner r){LinkedHashSet<String> paths=new LinkedHashSet<>();for(String text:java.util.List.of(r.addons,r.customAddons))for(String path:text.split(","))if(!path.isBlank())paths.add(path.trim());return String.join(",",paths);}
    static String config(Project project, Workspace.Runner r, boolean includeSecrets) {
        Map<String,String> options = new LinkedHashMap<>();
        options.put("http_interface", "127.0.0.1"); options.put("http_port", r.httpPort); options.put(r.eventKey, r.eventPort);
        options.put("addons_path", mergedAddons(r)); options.put("db_name", r.database); options.put("db_host", r.dbHost);
        options.put("db_port", r.dbPort); options.put("db_user", r.dbUser); options.put("workers", r.workers);
        options.put("data_dir", r.dataDir.isBlank() ? directory(project,r).resolve("data").toString() : r.dataDir);
        options.putAll(RunnerWindow.Panel.additional(r.extra));
        for (String key : SECRETS) {
            String password = PasswordSafe.getInstance().getPassword(secretKey(r,key));
            if (password != null && !password.isEmpty()) options.put(key, includeSecrets ? password : "<stored in Password Safe>");
        }
        StringBuilder text = new StringBuilder("[options]\n");
        options.forEach((key,value) -> {
            if (!value.isBlank()) {
                if (value.contains("\n") || value.contains("\r")) throw new IllegalArgumentException("Multiline values are unsupported: " + key);
                text.append(key).append(" = ").append(value).append('\n');
            }
        });
        return text.toString();
    }
    static GeneralCommandLine command(Workspace.Installation i, Workspace.Runner r, Path conf) {
        GeneralCommandLine command = new GeneralCommandLine(i.python, Path.of(i.root).resolve(i.script).toString(), "--config", conf.toString())
            .withWorkDirectory(i.root).withCharset(StandardCharsets.UTF_8).withEnvironment("PYTHONUNBUFFERED", "1");
        String[] names = {"--database", "--init", "--update", "--test-tags", "--test-file", "--dev", "--without-demo"};
        String[] values = {r.launchDatabase, r.installModules, r.updateModules, r.testTags, r.testFile, r.devMode, r.withoutDemo};
        for (int n=0;n<names.length;n++) if (!values[n].isBlank()) command.addParameter(names[n]+"="+values[n]);
        if (r.testEnable) command.addParameter("--test-enable");
        if (r.stopAfterInit) command.addParameter("--stop-after-init");
        return command;
    }
    static void start(Project project, Workspace workspace, Workspace.Installation i, Workspace.Runner r) throws Exception {
        if (project.isDisposed() || !workspace.getState().installations.contains(i) || !i.runners.contains(r)) throw new IllegalArgumentException("Project or runner no longer available.");
        if (workspace.active(r)) throw new IllegalArgumentException("This runner is already running.");
        int http=RunnerWindow.Panel.port(r.httpPort), event=RunnerWindow.Panel.port(r.eventPort);
        if (http==event) throw new IllegalArgumentException("HTTP and event ports must differ.");
        for (var other : workspace.getState().installations) for (var candidate : other.runners)
            if (candidate!=r && workspace.active(candidate) && (Set.of(http,event).contains(RunnerWindow.Panel.port(candidate.httpPort)) || Set.of(http,event).contains(RunnerWindow.Panel.port(candidate.eventPort))))
                throw new IllegalArgumentException("Ports conflict with running runner: " + candidate.name);
        for (int port : new int[]{http,event}) try (ServerSocket socket=new ServerSocket()) { socket.setReuseAddress(false); socket.bind(new InetSocketAddress("127.0.0.1",port)); }
        Path dir=directory(project,r); Files.createDirectories(dir);
        try { Files.setPosixFilePermissions(dir,PosixFilePermissions.fromString("rwx------")); } catch (UnsupportedOperationException ignored) { }
        Path conf=dir.resolve("odoo.conf"); Files.writeString(conf,config(project,r,true),StandardCharsets.UTF_8);
        try { Files.setPosixFilePermissions(conf,PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
        OSProcessHandler process=new KillableColoredProcessHandler(command(i,r,conf));
        workspace.processes.put(r.id,process);
        try {
            workspace.capture(i,r,process);
            ConsoleView console=TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole(); console.attachToProcess(process);
            RunContentDescriptor descriptor=new RunContentDescriptor(console,process,console.getComponent(),i.name+" / "+r.name);
            RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(),descriptor);
            process.startNotify(); workspace.messages.remove(r.id);
        } catch (Exception ex) { process.destroyProcess(); throw ex; }
    }
}
