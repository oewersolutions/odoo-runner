package dev.odoorunner;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Enumerates manifests without importing or executing addon code. */
final class ModulePickerPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Select","Module","Source path"},0){
        public Class<?> getColumnClass(int c){return c==0?Boolean.class:String.class;}
        public boolean isCellEditable(int r,int c){return c==0;}
    };
    private final JTable table=new JTable(model);
    private final JTextField search=new RoundTextField();
    private final JLabel status=new JLabel();
    private final JButton apply;
    private JTextField target;
    private String initial="";
    private int revision;
    private SwingWorker<Map<String,String>,Void> worker;
    ModulePickerPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(8,8));this.owner=owner;
        JPanel top=new JPanel(new BorderLayout(6,6));top.add(new JLabel("Select modules from configured addon folders"),BorderLayout.NORTH);top.add(search);add(top,BorderLayout.NORTH);
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);table.setRowSorter(sorter);table.setRowHeight(28);table.getColumnModel().getColumn(0).setMaxWidth(65);add(new HiddenScrollPane(table));
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}private void filter(){String query=search.getText().trim();sorter.setRowFilter(query.isBlank()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(query),1,2));}});
        apply=owner.button("Apply selection",()->{if(table.isEditing())table.getCellEditor().stopCellEditing();target.setText(selected());owner.closeModulePicker();});
        JPanel bottom=new JPanel(new BorderLayout());bottom.add(status,BorderLayout.NORTH);bottom.add(owner.row(apply,owner.button("Cancel",()->{if(canLeave())owner.closeModulePicker();}),owner.button("Select visible",()->setVisibleSelection(true)),owner.button("Clear visible",()->setVisibleSelection(false))),BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
    }
    private void setVisibleSelection(boolean value){if(!table.isEnabled())return;for(int n=0;n<table.getRowCount();n++)model.setValueAt(value,table.convertRowIndexToModel(n),0);}
    private String selected(){List<String> names=new ArrayList<>();for(int n=0;n<model.getRowCount();n++)if(Boolean.TRUE.equals(model.getValueAt(n,0)))names.add(model.getValueAt(n,1).toString());return String.join(",",names);}
    void begin(JTextField target,List<Path> roots){
        this.target=target;int token=++revision;if(worker!=null)worker.cancel(true);search.setText("");model.setRowCount(0);apply.setEnabled(false);table.setEnabled(false);status.setText("Scanning addon manifests…");
        Set<String> selected=new LinkedHashSet<>();for(String name:target.getText().split(","))if(!name.isBlank())selected.add(name.trim());
        for(String name:selected)model.addRow(new Object[]{true,name,"Waiting for scan"});initial=selected();
        worker=new SwingWorker<>(){
            protected Map<String,String> doInBackground()throws Exception{
                Map<String,String> found=new TreeMap<>();
                for(Path root:roots){if(isCancelled())return found;try(var children=Files.list(root)){
                    var iterator=children.iterator();while(iterator.hasNext()){if(isCancelled())return found;Path child=iterator.next();String name=child.getFileName().toString();if(!name.matches("[A-Za-z_][A-Za-z0-9_]*"))continue;
                        if(Files.isRegularFile(child.resolve("__manifest__.py"))||Files.isRegularFile(child.resolve("__openerp__.py")))found.putIfAbsent(name,root.toString());
                    }
                }}return found;
            }
            protected void done(){if(token!=revision||isCancelled())return;try{
                var found=get();model.setRowCount(0);for(var entry:found.entrySet())model.addRow(new Object[]{selected.contains(entry.getKey()),entry.getKey(),entry.getValue()});for(String name:selected)if(!found.containsKey(name))model.addRow(new Object[]{true,name,name.equals("all")?"All modules (Odoo CLI special value)":"Not found in configured addon folders"});
                initial=selected();apply.setEnabled(true);table.setEnabled(true);status.setText(found.size()+" modules found. First configured path wins duplicate module names.");
            }catch(Exception ex){status.setText("Scan failed: "+(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()));}}
        };worker.execute();
    }
    boolean canLeave(){if(table.isEditing())table.getCellEditor().stopCellEditing();return selected().equals(initial)||com.intellij.openapi.ui.Messages.showYesNoDialog(owner.project,"Discard unapplied module selections?","Module selection",com.intellij.openapi.ui.Messages.getQuestionIcon())==com.intellij.openapi.ui.Messages.YES;}
    void dispose(){revision++;if(worker!=null)worker.cancel(true);}
}
