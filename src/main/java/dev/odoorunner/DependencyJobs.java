package dev.odoorunner;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.project.Project;
import java.nio.file.*;
import java.util.*;

/** Installs only after the user presses Install in the dependency form. */
final class DependencyJobs {
    private final Project project;private final Workspace workspace;
    private final Map<String,OSProcessHandler> processes=new HashMap<>();
    private final Map<String,RunnerLogs> output=new HashMap<>();
    DependencyJobs(Project project,Workspace workspace){this.project=project;this.workspace=workspace;}
    boolean busy(Workspace.Installation i){var process=processes.get(i.id);return process!=null&&!process.isProcessTerminated();}
    RunnerLogs logs(Workspace.Installation i){return output.computeIfAbsent(i.id,key->new RunnerLogs(List.of()));}
    void install(Workspace.Installation i,Path requirements){
        if(!requirements.isAbsolute()||!Files.isRegularFile(requirements))throw new IllegalArgumentException("Select an existing requirements file.");
        launch(i,List.of("--requirement",requirements.toString()));
    }
    void installPackages(Workspace.Installation i,String text){
        List<String> packages=new ArrayList<>();for(String value:text.split("\\s+")){if(value.isBlank())continue;if(!value.matches("[A-Za-z0-9][A-Za-z0-9._-]*(\\[[A-Za-z0-9_,.-]+\\])?((==|~=|>=|<=|!=|>|<)[A-Za-z0-9.*+!_-]+(,(==|~=|>=|<=|!=|>|<)[A-Za-z0-9.*+!_-]+)*)?"))throw new IllegalArgumentException("Use package names with optional versions/extras, e.g. requests==2.32.5. Options/URLs are not accepted here.");packages.add(value);}
        if(packages.isEmpty())throw new IllegalArgumentException("Add at least one package.");launch(i,packages);
    }
    private void launch(Workspace.Installation i,List<String> arguments){
        if(busy(i))throw new IllegalArgumentException("Dependency installation is already running.");
        if(i.runners.stream().anyMatch(workspace::busy))throw new IllegalArgumentException("Stop this project's runners before installing dependencies.");
        Path python=Path.of(i.python);Path environment=python.getParent()==null?null:python.getParent().getParent();
        if(!workspace.getState().installations.contains(i)||!python.isAbsolute()||!Files.isExecutable(python)||environment==null||!Files.isRegularFile(environment.resolve("pyvenv.cfg")))throw new IllegalArgumentException("Select an existing virtualenv Python in project setup first. System Python installs are not supported.");
        try{
            GeneralCommandLine command=new GeneralCommandLine(i.python,"-m","pip","--require-virtualenv","install","--disable-pip-version-check","--no-input").withWorkDirectory(i.root).withEnvironment("PYTHONUNBUFFERED","1").withEnvironment("PYTHONNOUSERSITE","1").withEnvironment("PIP_TARGET","").withEnvironment("PIP_PREFIX","").withEnvironment("PIP_USER","false");
            command.addParameters(arguments);RunnerLogs logs=logs(i);logs.message("[Installing dependencies into selected virtualenv: "+i.python+"]");
            OSProcessHandler process=new KillableColoredProcessHandler(command);logs.attach(process);processes.put(i.id,process);process.startNotify();
        }catch(Exception ex){logs(i).message("ERROR [Cannot start pip: "+ex.getMessage()+"]");throw new IllegalArgumentException(ex.getMessage(),ex);}
    }
    String status(Workspace.Installation i){var process=processes.get(i.id);return process==null?"Ready":!process.isProcessTerminated()?"Installing…":process.getExitCode()!=null&&process.getExitCode()==0?"Installation completed":"Installation stopped/failed (exit "+process.getExitCode()+")";}
    void cancel(Workspace.Installation i){var process=processes.get(i.id);if(process!=null&&!process.isProcessTerminated())process.destroyProcess();}
    void dispose(){for(var process:processes.values())if(!process.isProcessTerminated())process.destroyProcess();processes.clear();output.clear();}
}
