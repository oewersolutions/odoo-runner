package dev.odoorunner;

import com.google.gson.*;
import com.intellij.openapi.ui.Messages;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Samples only the selected runner process tree and one selected PostgreSQL database. */
final class RunnerPerformancePanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final JLabel scope=new JLabel(),processSummary=new JLabel(),databaseSummary=new JLabel(),databaseDetails=new JLabel();
    private final StatusMessage dbStatus=new StatusMessage();
    private final PerformanceGraph cpu=new PerformanceGraph("Runner CPU","% of one core",100),memory=new PerformanceGraph("Runner memory (RSS)","MiB",64),transactions=new PerformanceGraph("Database transactions","transactions/s",1),connections=new PerformanceGraph("Database connections","connections",5);
    private final javax.swing.Timer timer;
    private Workspace.Runner runner;
    private Workspace.Installation installation;
    private String database="",selection="";
    private boolean cpuBusy,dbBusy,disposed;
    private int generation,ticks;
    private Map<Long,Long> previousCpu=new HashMap<>();
    private long previousTime,rootPid=-1;
    private JsonObject previousDb;
    private DatabaseBridge bridge;
    private volatile Process memoryProcess;
    private record Sample(long root,long time,Map<Long,Long> durations,Long rss,int count,String error){}

    RunnerPerformancePanel(RunnerWindow.Panel owner){
        super(new BorderLayout(0,10));this.owner=owner;setBorder(BorderFactory.createEmptyBorder(12,10,12,10));
        JPanel heading=new JPanel(new BorderLayout(10,8));scope.setForeground(Design.BLUE);heading.add(scope);
        heading.add(owner.button("Choose database",this::chooseDatabase),BorderLayout.EAST);
        JPanel labels=new JPanel(new GridLayout(0,1,0,6));labels.add(processSummary);labels.add(databaseSummary);labels.add(databaseDetails);
        JPanel top=new JPanel(new BorderLayout(0,10));top.add(heading,BorderLayout.NORTH);top.add(labels);top.add(dbStatus,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        JPanel graphs=new JPanel(new GridLayout(2,2,12,12));graphs.add(cpu);graphs.add(memory);graphs.add(transactions);graphs.add(connections);add(graphs);
        JTextArea note=new JTextArea("CPU and RSS: selected runner + descendants only. 100% CPU = one core; RSS sums shared pages more than once. Database counters: selected database, including its other clients. Sampling pauses when this tab is hidden.");note.setOpaque(false);note.setEditable(false);note.setLineWrap(true);note.setWrapStyleWord(true);note.setFont(UIManager.getFont("Label.font"));note.setRows(2);add(note,BorderLayout.SOUTH);
        timer=new javax.swing.Timer(2000,e->tick());timer.setCoalesce(true);
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0){if(isShowing()&&!disposed){reset();timer.start();tick();}else stop();}});
    }
    private void reset(){
        stop();runner=owner.runner();installation=owner.installation();selection=runner==null?"":runner.id;
        String configured=runner==null?"":RunnerWindow.Panel.additional(runner.extra).getOrDefault("db_name",runner.database);
        database=runner==null?"":runner.launchDatabase.isBlank()?configured:runner.launchDatabase;
        if(database.contains(",")||database.contains("\n")||database.contains("\r"))database="";
        cpu.clear();memory.clear();transactions.clear();connections.clear();previousCpu.clear();previousTime=0;previousDb=null;rootPid=-1;ticks=0;
        scope.setText(runner==null?"Select a runner":"Saved runner: "+runner.name+" · Database: "+(database.isBlank()?"Choose one database":database));
        processSummary.setText("Waiting for runner process metrics…");databaseSummary.setText("Waiting for database metrics…");databaseDetails.setText("Database sample interval: 6 seconds · Runner interval: 2 seconds");dbStatus.setVisible(false);
    }
    private void chooseDatabase(){
        String value=Messages.showInputDialog(owner.project,"Database to monitor for this runner (saved configuration stays unchanged).","Performance database",Messages.getQuestionIcon());
        if(value==null)return;value=value.trim();if(value.isBlank()||value.contains(",")||value.contains("\n")||value.contains("\r")){dbStatus.message("Warning","Enter one database name.");return;}
        if(bridge!=null)bridge.cancel();generation++;cpuBusy=false;dbBusy=false;bridge=null;database=value;previousDb=null;transactions.clear();connections.clear();ticks=0;
        scope.setText("Saved runner: "+runner.name+" · Database: "+database);dbStatus.message("Info","Connecting to selected database…");tick();
    }
    private void tick(){
        if(disposed||!isShowing())return;
        if(owner.runner()==null||!owner.runner().id.equals(selection)){reset();timer.start();}
        if(runner==null||installation==null)return;
        if(!cpuBusy)sampleProcess();if(ticks++%3==0&&!dbBusy)sampleDatabase();
    }
    private void sampleProcess(){
        var handler=owner.workspace.processes.get(runner.id);ProcessHandle handle=handler==null||handler.isProcessTerminated()?null:handler.getProcess().toHandle();
        cpuBusy=true;int token=generation;
        new SwingWorker<Sample,Void>(){
            protected Sample doInBackground(){
                if(handle==null||!handle.isAlive())return new Sample(-1,System.nanoTime(),Map.of(),0L,0,"");
                try{
                    List<ProcessHandle> processes=new ArrayList<>();processes.add(handle);try(var children=handle.descendants()){children.filter(ProcessHandle::isAlive).limit(4096).forEach(processes::add);}
                    Map<Long,Long> durations=new HashMap<>();for(ProcessHandle process:processes)process.info().totalCpuDuration().ifPresent(duration->durations.put(process.pid(),duration.toNanos()));
                    Long rss=null;
                    if(processes.size()<=512&&!System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows")){
                        String pids=String.join(",",processes.stream().map(process->Long.toString(process.pid())).toList());
                        Process ps=new ProcessBuilder("/bin/ps","-p",pids,"-o","rss=").redirectError(ProcessBuilder.Redirect.DISCARD).start();memoryProcess=ps;
                        try{if(ps.waitFor(2,TimeUnit.SECONDS)&&ps.exitValue()==0){String output=new String(ps.getInputStream().readNBytes(262144),StandardCharsets.UTF_8);long total=0;for(String row:output.trim().split("\\s+"))if(!row.isBlank())total+=Long.parseLong(row);rss=total*1024;}}finally{if(ps.isAlive())ps.destroyForcibly();memoryProcess=null;}
                    }
                    return new Sample(handle.pid(),System.nanoTime(),durations,rss,processes.size(),"");
                }catch(Exception ex){return new Sample(handle.pid(),System.nanoTime(),Map.of(),null,0,"Process metrics unavailable.");}
            }
            protected void done(){if(token!=generation||disposed)return;cpuBusy=false;try{applyProcess(get());}catch(Exception ex){processSummary.setText("Process metrics unavailable.");cpu.sample(null,"Unavailable");memory.sample(null,"Unavailable");}}
        }.execute();
    }
    private void applyProcess(Sample sample){
        if(sample.root()<0){cpu.sample(null,"Runner stopped");memory.sample(null,"Runner stopped");processSummary.setText("Runner stopped · no active process to monitor");previousCpu.clear();previousTime=0;rootPid=-1;return;}
        Double usage=null;if(sample.root()==rootPid&&previousTime>0&&sample.time()>previousTime&&!sample.durations().isEmpty()){
            long used=0;boolean matched=false;for(var entry:sample.durations().entrySet()){Long before=previousCpu.get(entry.getKey());if(before!=null&&entry.getValue()>=before){used+=entry.getValue()-before;matched=true;}}if(matched)usage=100.0*used/(sample.time()-previousTime);
        }
        previousCpu=sample.durations();previousTime=sample.time();rootPid=sample.root();
        cpu.sample(usage,usage==null?"Warming up / unavailable":String.format(Locale.ROOT,"%.1f%%",usage));memory.sample(sample.rss()==null?null:sample.rss()/1048576.0,sample.rss()==null?"RSS unavailable":String.format(Locale.ROOT,"%.1f MiB",sample.rss()/1048576.0));
        processSummary.setText("PID "+sample.root()+" · "+sample.count()+" runner processes"+(sample.error().isBlank()?"":" · "+sample.error()));
    }
    private void sampleDatabase(){
        if(database.isBlank()){
            databaseSummary.setText("No single database selected · choose a database to monitor");transactions.sample(null,"Choose database");connections.sample(null,"Choose database");return;
        }
        if(owner.workspace.dependencies.busy(installation)){dbStatus.message("Info","Database sampling waits for dependency installation.");return;}
        JsonObject request;
        try{request=DatabaseBridge.request(runner,database,"metrics");}catch(Exception ex){dbStatus.message("Error","Cannot read saved database connection settings.");return;}
        DatabaseBridge job=new DatabaseBridge();bridge=job;dbBusy=true;int token=generation;Workspace.Installation selected=installation;
        new SwingWorker<JsonObject,Void>(){
            protected JsonObject doInBackground()throws Exception{return job.execute(selected,request);}
            protected void done(){
                if(token!=generation||disposed)return;dbBusy=false;bridge=null;
                try{applyDatabase(get());}catch(Exception ex){Throwable cause=ex.getCause()==null?ex:ex.getCause();dbStatus.message("Warning",Objects.toString(cause.getMessage(),"Database metrics unavailable."));databaseSummary.setText("Database metrics unavailable · previous values are stale");transactions.sample(null,"Unavailable");connections.sample(null,"Unavailable");previousDb=null;}
            }
        }.execute();
    }
    private void applyDatabase(JsonObject metrics){
        Double rate=null,cache=null;
        if(previousDb!=null&&Objects.equals(metrics.get("reset"),previousDb.get("reset"))){
            double elapsed=metrics.get("time").getAsDouble()-previousDb.get("time").getAsDouble();
            long commits=delta(metrics,"commits"),rollbacks=delta(metrics,"rollbacks"),hits=delta(metrics,"hits"),reads=delta(metrics,"reads");
            if(elapsed>0&&commits>=0&&rollbacks>=0)rate=(commits+rollbacks)/elapsed;
            if(hits>=0&&reads>=0&&hits+reads>0)cache=100.0*hits/(hits+reads);
        }
        previousDb=metrics;
        transactions.sample(rate,rate==null?"Warming up after first sample":String.format(Locale.ROOT,"%.2f transactions/s",rate));
        connections.sample(metrics.get("connections").getAsDouble(),metrics.get("connections").getAsLong()+" connections");
        databaseSummary.setText("Database: "+database+" · "+String.format(Locale.ROOT,"%.1f MiB",metrics.get("size").getAsDouble()/1048576.0)+" · Active "+metrics.get("active").getAsLong()+" · Idle "+metrics.get("idle").getAsLong()+" · Idle in transaction "+metrics.get("idleTransaction").getAsLong());
        databaseDetails.setText("Lock waits "+metrics.get("lockWaits").getAsLong()+" · Deadlocks since statistics reset "+metrics.get("deadlocks").getAsLong()+" · Interval cache hits "+(cache==null?"N/A (no block activity)":String.format(Locale.ROOT,"%.1f%%",cache))+" · Updated "+java.time.LocalTime.now().withNano(0));
        long hidden=metrics.get("hiddenStates").getAsLong();
        dbStatus.message(hidden>0?"Warning":"Info",hidden>0?hidden+" session states are hidden by database permissions; state counts may be incomplete.":"Statistics are scoped to this database. Monitoring connections are excluded from connection counts; transaction counters can include monitoring overhead.");
    }
    private long delta(JsonObject current,String name){return current.get(name).getAsLong()-previousDb.get(name).getAsLong();}
    private void stop(){timer.stop();generation++;cpuBusy=false;dbBusy=false;if(bridge!=null)bridge.cancel();bridge=null;Process ps=memoryProcess;if(ps!=null)ps.destroyForcibly();}
    void dispose(){disposed=true;stop();}
}
