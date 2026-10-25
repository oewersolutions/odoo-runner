package dev.odoorunner;

import javax.swing.*;
import java.util.*;
import java.nio.file.*;

/** Grouped controls retain existing advanced-options persistence and unknown values. */
final class DeploymentOptionsPanel {
    private final Map<String,JTextField> text=new LinkedHashMap<>();
    private final Map<String,JComboBox<String>> choices=new LinkedHashMap<>();
    private static final Set<String> NUMERIC=Set.of("db_maxconn","max_cron_threads","limit_memory_soft","limit_memory_hard","limit_time_cpu","limit_time_real","limit_request","smtp_port");
    DeploymentOptionsPanel(){
        for(String key:java.util.List.of("dbfilter","http_interface","db_maxconn","db_template","pg_path","max_cron_threads","limit_memory_soft","limit_memory_hard","limit_time_cpu","limit_time_real","limit_request","logfile","log_handler","log_db","smtp_server","smtp_port","smtp_user","email_from")){
            var field=new RoundTextField();field.setToolTipText("Blank uses the Odoo default. Memory limits: bytes. Time limits: seconds.");text.put(key,field);
        }
        for(String key:java.util.List.of("proxy_mode","list_db","db_sslmode","smtp_ssl")){
            String[] values=key.equals("db_sslmode")?new String[]{"","disable","allow","prefer","require","verify-ca","verify-full"}:new String[]{"","True","False"};var input=new RoundComboBox<>(values);input.setEditable(true);input.setToolTipText("Blank keeps the Odoo default. Existing custom values are preserved.");choices.put(key,input);
        }
    }
    void addTo(JPanel group,RunnerWindow.Panel owner,String...keys){
        for(String key:keys){JComponent input=choices.containsKey(key)?choices.get(key):text.get(key);
            if(key.equals("pg_path"))input=owner.pathPicker(text.get(key),true);
            ScrollForm.field(group,key.equals("pg_path")?"PostgreSQL binaries (pg_path)":key,input);
        }
    }
    void load(Map<String,String> options){text.forEach((key,field)->field.setText(options.remove(key)));choices.forEach((key,field)->field.setSelectedItem(options.containsKey(key)?options.remove(key):""));}
    Map<String,String> values(){Map<String,String> out=new LinkedHashMap<>();text.forEach((key,field)->{if(!field.getText().isBlank())out.put(key,field.getText().trim());});choices.forEach((key,field)->{String value=Objects.toString(field.getEditor().getItem(),"").trim();if(!value.isBlank())out.put(key,value);});return out;}
    void validateValues(){values().forEach((key,value)->{
        if(value.contains("\n")||value.contains("\r"))throw new IllegalArgumentException("Multiline option: "+key);
        if(NUMERIC.contains(key)){try{if(Long.parseLong(value)<0)throw new NumberFormatException();}catch(NumberFormatException ex){throw new IllegalArgumentException(key+" must be a nonnegative integer.");}}
        if(key.equals("pg_path")&&(!Path.of(value).isAbsolute()||!Files.isDirectory(Path.of(value))))throw new IllegalArgumentException("pg_path must be an existing absolute directory containing PostgreSQL binaries.");
    });}
}
