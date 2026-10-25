package dev.odoorunner;

import com.intellij.openapi.components.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import javax.swing.SwingUtilities;
import com.intellij.execution.process.OSProcessHandler;
import java.util.*;
import java.util.concurrent.*;

@State(name="OdooRunnerWorkspace", storages=@Storage(StoragePathMacros.WORKSPACE_FILE))
public final class Workspace implements PersistentStateComponent<Workspace.Data>, Disposable {
    public static class Data { public double projectsSplit = 0.30; public List<Installation> installations = new ArrayList<>(); public String defaultPython = "", defaultHttpPort = "8069", defaultDbHost = "localhost", defaultDbUser = "odoo", defaultEventKey = "gevent_port"; }
    public static class Installation {
        public String id = UUID.randomUUID().toString();
        public String name = "Odoo 18", root = "", python = "", script = "odoo-bin";
        public List<Runner> runners = new ArrayList<>();
        @Override public String toString() { return name; }
    }
    public static class Runner {
        public String id = UUID.randomUUID().toString();
        public String name = "Development", httpPort = "8069", eventPort = "8072";
        public String customAddons = "";
        public String eventKey = "gevent_port", addons = "", database = "", dbHost = "localhost", dbPort = "5432", dbUser = "odoo";
        public String launchDatabase = "", installModules = "", updateModules = "", testTags = "", testFile = "", devMode = "", withoutDemo = "";
        public boolean testEnable, stopAfterInit;
        public String workers = "0", extra = "", dataDir = "";
        @Override public String toString() { return name + " · " + httpPort; }
    }
    private final Project project;
    private boolean disposed;
    final DependencyJobs dependencies;
    final Map<String,RunnerLogs> logs=new ConcurrentHashMap<>();
    private final Map<String,LogFileFollower> logFiles=new ConcurrentHashMap<>();
    private final ScheduledExecutorService logReader=Executors.newSingleThreadScheduledExecutor(r->{Thread thread=new Thread(r,"odoo-runner-log-reader");thread.setDaemon(true);return thread;});
    public Workspace(Project project) { this.project = project;dependencies=new DependencyJobs(project,this);logReader.scheduleWithFixedDelay(()->{for(var entry:logFiles.entrySet())try{entry.getValue().poll();var process=processes.get(entry.getKey());if(process==null||process.isProcessTerminated()){for(int n=0;n<4;n++)entry.getValue().poll();logFiles.remove(entry.getKey(),entry.getValue());var capture=logs.get(entry.getKey());if(capture!=null)capture.flush();}}catch(Exception ignored){}},100,100,TimeUnit.MILLISECONDS); }
    void capture(Installation i,Runner r,OSProcessHandler process){
        List<String> secrets=new ArrayList<>();for(String key:RunnerEngine.SECRETS){String value=com.intellij.ide.passwordSafe.PasswordSafe.getInstance().getPassword(RunnerEngine.secretKey(r,key));if(value!=null&&!value.isEmpty())secrets.add(value);}
        RunnerLogs capture=new RunnerLogs(secrets);logs.put(r.id,capture);capture.message("[Runner started: "+r.name+"]");capture.attach(process);
        String file=RunnerWindow.Panel.additional(r.extra).get("logfile");if(file!=null&&!file.isBlank())logFiles.put(r.id,new LogFileFollower(java.nio.file.Path.of(i.root).resolve(file),capture));else logFiles.remove(r.id);
    }
    private Data data = new Data();
    final Map<String, Object> pendingRestarts = new HashMap<>();
    final Map<String, String> messages = new HashMap<>();
    boolean active(Runner r) { var p=processes.get(r.id); return p!=null&&!p.isProcessTerminated(); }
    boolean busy(Runner r) { return pendingRestarts.containsKey(r.id)||active(r); }
    void start(Installation i, Runner r) {
        if (pendingRestarts.containsKey(r.id)) throw new IllegalArgumentException("Restart already pending.");
        if(dependencies.busy(i))throw new IllegalArgumentException("Wait for dependency installation to finish before starting runners.");
        try { RunnerEngine.start(project,this,i,r); } catch(Exception ex) { messages.put(r.id,"Start failed: "+ex.getMessage()); throw new IllegalArgumentException(ex.getMessage(),ex); }
    }
    void stop(Runner r) {
        pendingRestarts.remove(r.id); var process=processes.get(r.id);
        if(process!=null&&!process.isProcessTerminated()) { messages.put(r.id,"Stopping…"); process.destroyProcess(); }
        else messages.remove(r.id);
    }
    void rerun(Installation i, Runner r, boolean requireRunning) {
        if(pendingRestarts.containsKey(r.id)) throw new IllegalArgumentException("Restart already pending.");
        var old=processes.get(r.id);
        if(old==null||old.isProcessTerminated()) { if(requireRunning)throw new IllegalArgumentException("Restart requires a running process. Use Rerun to start it."); start(i,r);return; }
        Object token=new Object(); pendingRestarts.put(r.id,token); messages.put(r.id,"Restarting…"); old.destroyProcess();
        ApplicationManager.getApplication().executeOnPooledThread(()->{
            boolean stopped=old.waitFor(15000);
            SwingUtilities.invokeLater(()->{
                if(disposed||project.isDisposed()||pendingRestarts.get(r.id)!=token)return;
                pendingRestarts.remove(r.id);
                if(!stopped){messages.put(r.id,"Restart cancelled: old process did not exit within 15 seconds.");return;}
                if(processes.get(r.id)!=old){messages.put(r.id,"Restart cancelled: process changed.");return;}
                try{start(i,r);}catch(Exception ex){messages.put(r.id,"Restart failed: "+ex.getMessage());}
            });
        });
    }
    String processStatus(Runner r) {
        if(pendingRestarts.containsKey(r.id))return "Restarting…";
        String message=messages.get(r.id);if(message!=null&&(!message.equals("Stopping…")||active(r)))return message;
        var process=processes.get(r.id);return process==null?"Stopped":!process.isProcessTerminated()?"Running":"Exited ("+process.getExitCode()+")";
    }
    final Map<String, OSProcessHandler> processes = new ConcurrentHashMap<>();
    public Data getState() { return data; }
    public void loadState(Data state) { data = state; }
    public void dispose() { disposed=true;logReader.shutdownNow();logFiles.clear();dependencies.dispose();logs.clear();pendingRestarts.clear();for (OSProcessHandler p : processes.values()) if (!p.isProcessTerminated()) p.destroyProcess(); processes.clear(); }
}
