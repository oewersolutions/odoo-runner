package dev.odoorunner;

import com.intellij.execution.process.*;
import com.intellij.openapi.util.Key;
import java.util.*;
import java.util.regex.Pattern;

/** Session-only, bounded line capture; process callbacks may arrive off the UI thread. */
final class RunnerLogs {
    private final ArrayDeque<String> lines=new ArrayDeque<>(),issues=new ArrayDeque<>();
    private final Map<String,StringBuilder> pending=new HashMap<>();
    private final Map<String,String> continuation=new HashMap<>();
    private final ArrayDeque<Entry> entries=new ArrayDeque<>();
    private long sequence;
    private final List<String> secrets;
    private long revision;
    private int issueCount;
    private static final Pattern LEVEL=Pattern.compile("\\b(DEBUG|INFO|WARNING|WARN|ERROR|CRITICAL|FATAL)\\b");
    RunnerLogs(List<String> secrets){this.secrets=List.copyOf(secrets);}
    synchronized void accept(String stream,String text){StringBuilder buffer=pending.computeIfAbsent(stream,key->new StringBuilder());buffer.append(text);int end;while((end=buffer.indexOf("\n"))>=0){String line=buffer.substring(0,end);buffer.delete(0,end+1);line(stream,line);}if(buffer.length()>16384){line(stream,buffer.substring(0,16384));buffer.delete(0,16384);}}
    private void line(String stream,String value){
        value=value.replaceAll("(https?://)[^\\s/@]+:[^\\s/@]+@","$1<redacted>@").replaceAll("\\x1B\\[[0-?]*[ -/]*[@-~]","").replace("\r","");for(String secret:secrets)if(!secret.isEmpty())value=value.replace(secret,"<redacted>");
        var match=LEVEL.matcher(value);String severity;
        if(match.find()){String level=match.group();severity=level.equals("WARN")||level.equals("WARNING")?"WARNING":Set.of("ERROR","CRITICAL","FATAL").contains(level)?"ERROR":level;continuation.put(stream,severity);}
        else{severity=value.startsWith("Traceback (most recent call last)")?"ERROR":continuation.getOrDefault(stream,"INFO");continuation.put(stream,severity);}
        boolean issue=severity.equals("WARNING")||severity.equals("ERROR");
        entries.addLast(new Entry(++sequence,severity,stream,value));while(entries.size()>2500)entries.removeFirst();
        lines.addLast(value);while(lines.size()>2500)lines.removeFirst();if(issue){issues.addLast(value);while(issues.size()>1200)issues.removeFirst();issueCount++;}revision++;

    }
    synchronized void message(String value){line("plugin",value);}
    synchronized void flush(){for(var entry:pending.entrySet())if(!entry.getValue().isEmpty()){line(entry.getKey(),entry.getValue().toString());entry.getValue().setLength(0);}}
    synchronized int issueCount(){return issueCount;}
    synchronized long revision(){return revision;}
    synchronized List<Entry> entries(){return List.copyOf(entries);}
    record Entry(long line,String severity,String source,String text){}
    synchronized Snapshot snapshot(){return new Snapshot(revision,String.join("\n",lines),String.join("\n",issues),issueCount);}
    record Snapshot(long revision,String output,String issues,int issueCount){}
    void attach(OSProcessHandler process){process.addProcessListener(new ProcessAdapter(){public void onTextAvailable(ProcessEvent event,Key type){accept(type.toString(),event.getText());}public void processTerminated(ProcessEvent event){flush();message("[Process exited: "+event.getExitCode()+"]");}});}
}
