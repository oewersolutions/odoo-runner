package dev.odoorunner;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.util.List;

final class AvailablePortPicker extends JPanel {
    private final JComboBox<String> choices=new RoundComboBox<>();
    private final JTextField value;
    private final RunnerWindow.Panel owner;
    private final JButton refresh;
    private boolean syncing;
    private int revision;
    AvailablePortPicker(RunnerWindow.Panel owner,JTextField value){
        super(new BorderLayout(4,0));this.owner=owner;this.value=value;choices.setEditable(true);add(choices);
        refresh=owner.button("Find available",this::scan);add(refresh,BorderLayout.EAST);
        JTextField editor=(JTextField)choices.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(listener(()->{if(!syncing){syncing=true;value.setText(editor.getText());syncing=false;}}));
        value.getDocument().addDocumentListener(listener(()->{if(!syncing){syncing=true;choices.setSelectedItem(value.getText());syncing=false;}}));
        refresh.setToolTipText("Find up to 40 free loopback ports near the configured starting HTTP port. You can also enter a port.");
    }
    private DocumentListener listener(Runnable action){return new DocumentListener(){public void insertUpdate(DocumentEvent e){action.run();}public void removeUpdate(DocumentEvent e){action.run();}public void changedUpdate(DocumentEvent e){action.run();}};}
    private void scan(){
        int token=++revision;var runner=owner.runner();if(runner==null)throw new IllegalArgumentException("Select a runner first.");
        Set<Integer> excluded=new HashSet<>();for(var i:owner.workspace.getState().installations)for(var r:i.runners)if(r!=runner){excluded.add(RunnerWindow.Panel.port(r.httpPort));excluded.add(RunnerWindow.Panel.port(r.eventPort));}
        for(String key:List.of("HTTP port","Event port")){JTextField other=owner.fields.get(key);if(other!=value)try{excluded.add(Integer.parseInt(other.getText()));}catch(NumberFormatException ignored){}}
        int first=RunnerWindow.Panel.port(owner.workspace.getState().defaultHttpPort);String original=value.getText();refresh.setEnabled(false);
        new SwingWorker<List<String>,Void>(){
            protected List<String> doInBackground(){List<String> available=new ArrayList<>();for(int port=first;port<=65535&&port<first+500&&available.size()<40;port++){if(excluded.contains(port))continue;try(ServerSocket socket=new ServerSocket()){socket.setReuseAddress(false);socket.bind(new InetSocketAddress("127.0.0.1",port));available.add(""+port);}catch(Exception ignored){}}return available;}
            protected void done(){refresh.setEnabled(true);if(token!=revision||owner.runner()!=runner||!value.getText().equals(original))return;try{syncing=true;choices.removeAllItems();for(String port:get())choices.addItem(port);choices.setSelectedItem(original);choices.setToolTipText(choices.getItemCount()+" ports available at last scan; launch rechecks them.");choices.showPopup();}catch(Exception ex){choices.setToolTipText("Cannot scan ports: "+ex.getMessage());}finally{syncing=false;}}
        }.execute();
    }
}
