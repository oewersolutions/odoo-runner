package dev.odoorunner;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/** Prepare an interactive shell hosted in the popup's embedded terminal. */
final class OdooShell {
    static void open(RunnerWindow.Panel owner,Workspace.Installation i,Workspace.Runner r){
        if(SystemInfo.isWindows)throw new IllegalArgumentException("Odoo Shell currently requires a POSIX terminal (macOS/Linux).");
        if(owner.workspace.dependencies.busy(i))throw new IllegalArgumentException("Wait for dependency installation to finish first.");
        if(!owner.workspace.getState().installations.contains(i)||!i.runners.contains(r))throw new IllegalArgumentException("Project/runner is no longer available.");
        if(!Files.isDirectory(Path.of(i.root))||!Files.isExecutable(Path.of(i.python))||!Files.isRegularFile(Path.of(i.root).resolve(i.script)))throw new IllegalArgumentException("Check the project source, Python executable and Odoo entry script first.");
        if(owner.shellPanel.resume(r))return;
        String database=r.launchDatabase.isBlank()?r.database:r.launchDatabase;
        if(database.isBlank()||database.contains(",")){database=Messages.showInputDialog(owner.project,"Enter the single database to open in Odoo Shell.","Odoo Shell database",Messages.getQuestionIcon());if(database==null)return;database=database.trim();}
        if(database.isBlank()||database.contains(",")||database.contains("\n")||database.contains("\r"))throw new IllegalArgumentException("Odoo Shell requires one database name.");
        try{
            Path directory=RunnerEngine.directory(owner.project,r);Files.createDirectories(directory);try{Files.setPosixFilePermissions(directory,PosixFilePermissions.fromString("rwx------"));}catch(UnsupportedOperationException ignored){}
            Path config=directory.resolve("odoo-shell.conf");String serverConfig=RunnerEngine.config(owner.project,r,true).lines().filter(line->{int equals=line.indexOf('=');String key=equals<0?line:line.substring(0,equals).trim();return !Set.of("init","update","test_enable","test_tags","test_file","stop_after_init").contains(key);}).collect(java.util.stream.Collectors.joining("\n","","\n"));Files.writeString(config,serverConfig,StandardCharsets.UTF_8);try{Files.setPosixFilePermissions(config,PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
            // Deliberately exclude runner init/update/test arguments from an interactive shell launch.
            List<String> arguments=List.of(i.python,Path.of(i.root).resolve(i.script).toString(),"shell","--config",config.toString(),"--database",database,"--shell-interface=python","--no-http");
            String command=String.join(" ",arguments.stream().map(OdooShell::quote).toList());
            owner.shellPanel.open(i,r,database,command);
        }catch(Exception ex){throw new IllegalArgumentException("Cannot open Odoo Shell: "+ex.getMessage(),ex);}
    }
    private static String quote(String value){return "'"+value.replace("'","'\\''")+"'";}
}
