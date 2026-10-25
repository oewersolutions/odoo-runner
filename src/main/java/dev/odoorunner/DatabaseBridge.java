package dev.odoorunner;

import com.google.gson.*;
import com.intellij.ide.passwordSafe.PasswordSafe;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Uses the selected Odoo environment's psycopg2; connection secrets travel via stdin only. */
final class DatabaseBridge {
    private volatile Process process;
    private volatile boolean cancelled;
    static JsonObject request(Workspace.Runner runner,String database,String action){
        Map<String,String> options=new LinkedHashMap<>();
        options.put("db_host",runner.dbHost);options.put("db_port",runner.dbPort);options.put("db_user",runner.dbUser);
        options.putAll(RunnerWindow.Panel.additional(runner.extra));
        String password=PasswordSafe.getInstance().getPassword(RunnerEngine.secretKey(runner,"db_password"));
        if(password!=null&&!password.isEmpty())options.put("db_password",password);
        JsonObject connection=new JsonObject();
        for(String name:List.of("host","port","user","password","sslmode")){
            String value=options.get("db_"+name);if(value!=null)connection.addProperty(name,value);
        }
        JsonObject request=new JsonObject();request.add("connection",connection);request.addProperty("database",database);request.addProperty("action",action);return request;
    }
    JsonObject execute(Workspace.Installation installation,JsonObject request)throws Exception{
        String script;
        try(InputStream resource=DatabaseBridge.class.getResourceAsStream("/database/browser.py")){
            if(resource==null)throw new IOException("Database bridge resource is missing.");
            script=new String(resource.readAllBytes(),StandardCharsets.UTF_8);
        }
        if(cancelled)throw new IOException("Database request cancelled.");
        Process child=new ProcessBuilder(installation.python,"-u","-c",script).directory(new File(installation.root))
            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        process=child;
        if(cancelled){child.destroyForcibly();throw new IOException("Database request cancelled.");}
        CompletableFuture<byte[]> output=CompletableFuture.supplyAsync(()->{
            try(InputStream stream=child.getInputStream()){
                byte[] bytes=stream.readNBytes(16*1024*1024+1);
                if(bytes.length>16*1024*1024){child.destroyForcibly();throw new IOException("Result exceeds 16 MB. Narrow the filter or select a smaller table.");}
                return bytes;
            }catch(IOException ex){throw new CompletionException(ex);}
        });
        try{
            try(OutputStream input=child.getOutputStream()){input.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            if(!child.waitFor(40,TimeUnit.SECONDS))throw new IOException("Database request timed out. Reload before retrying an edit; its save outcome may be unknown.");
            byte[] bytes=output.get(3,TimeUnit.SECONDS);
            if(child.exitValue()!=0)throw new IOException("Project Python could not run the database browser. Check the interpreter and installed dependencies.");
            JsonObject response=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!response.get("ok").getAsBoolean())throw new IOException(response.get("error").getAsString());
            return response.getAsJsonObject("result");
        }finally{if(child.isAlive())child.destroyForcibly();process=null;}
    }
    void cancel(){cancelled=true;Process child=process;if(child!=null)child.destroyForcibly();}
}
