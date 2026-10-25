package dev.odoorunner;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** One existing right-panel page; incremental output and independently filtered severity tabs. */
final class RunnerDiagnosticsPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private Workspace.Runner runner;private RunnerLogs capture;private long revision=-1,lastLine;private boolean logFilterChanged;
    private final JLabel title=new JLabel(),notice=new JLabel();
    private final JTextField search=new RoundTextField();
    private final JTextArea output=new JTextArea();
    private final JLabel summary=new JLabel();
    private final List<JTable> tables=new ArrayList<>();
    private final JButton copySelected,copyFiltered;
    private final JCheckBox live=new JCheckBox("Live updates",true);
    private final JCheckBox follow=new JCheckBox("Follow new output",true);
    private final JTabbedPane tabs=new JTabbedPane();
    private final DefaultTableModel rows=new DefaultTableModel(new String[]{"Line","Level","Source","Message"},0){public boolean isCellEditable(int row,int column){return false;}public Class<?> getColumnClass(int c){return c==0?Long.class:String.class;}};
    private final List<TableRowSorter<DefaultTableModel>> sorters=new ArrayList<>();
    RunnerDiagnosticsPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(12,12));this.owner=owner;setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        title.setFont(title.getFont().deriveFont(Font.BOLD,18f));
        JPanel header=new JPanel(new BorderLayout(8,8));header.add(owner.button("← Runners",owner::closeDiagnostics),BorderLayout.WEST);header.add(title);
        JPanel filter=new JPanel(new BorderLayout(8,0));filter.add(new JLabel("Filter lines"),BorderLayout.WEST);filter.add(search);search.setToolTipText("Filter warning/error rows by message or source; matches literal text.");
        JPanel top=new JPanel(new BorderLayout(8,12));top.add(header,BorderLayout.NORTH);top.add(filter);top.add(summary,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        tabs.addTab("Warnings",table("WARNING"));tabs.addTab("Errors",table("ERROR"));
        output.setEditable(false);((javax.swing.text.DefaultCaret)output.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);output.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));output.setLineWrap(false);tabs.addTab("Log",new HiddenScrollPane(output));add(tabs);
        copySelected=owner.button("Copy selected",()->copy(false));copyFiltered=owner.button("Copy filtered",()->copy(true));
        JPanel footer=new JPanel(new BorderLayout(8,8));footer.add(notice,BorderLayout.NORTH);footer.add(owner.row(copySelected,copyFiltered,live,follow));add(footer,BorderLayout.SOUTH);
        output.addCaretListener(e->updateCopyButtons());tabs.addChangeListener(e->updateNotice());live.addActionListener(e->{if(live.isSelected()){revision=-1;refresh();}});
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
    }
    private JComponent table(String severity){
        JTable table=new JTable(rows);table.setRowHeight(30);table.setShowGrid(false);table.setIntercellSpacing(new Dimension(0,0));table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);tables.add(table);table.getColumnModel().getColumn(0).setMaxWidth(75);table.getColumnModel().getColumn(1).setMaxWidth(95);table.getColumnModel().getColumn(2).setMaxWidth(140);
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(rows);table.setRowSorter(sorter);sorters.add(sorter);
        table.setDefaultRenderer(String.class,new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){super.getTableCellRendererComponent(t,value,selected,focus,row,column);setToolTipText(Objects.toString(value,""));setBorder(BorderFactory.createEmptyBorder(4,8,4,8));return this;}});
        JTextArea detail=new JTextArea(5,60);detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()){detail.setText(selectedLines(table,false));detail.setCaretPosition(0);updateNotice();}});
        String shortcut="copyDiagnosticRows";table.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),shortcut);table.getActionMap().put(shortcut,new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){clipboard(selectedLines(table,false));}});
        JPopupMenu menu=new JPopupMenu();JMenuItem selected=new JMenuItem("Copy selected messages");selected.addActionListener(e->clipboard(selectedLines(table,false)));menu.add(selected);JMenuItem all=new JMenuItem("Copy filtered messages");all.addActionListener(e->clipboard(selectedLines(table,true)));menu.add(all);table.setComponentPopupMenu(menu);
        JPanel wrapper=new JPanel(new BorderLayout(8,8));JPanel list=new JPanel(new BorderLayout());list.add(table.getTableHeader(),BorderLayout.NORTH);list.add(new HiddenScrollPane(table));wrapper.add(list);JPanel preview=new JPanel(new BorderLayout(4,4));preview.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));preview.add(new JLabel("Message details · select one or more rows above"),BorderLayout.NORTH);preview.add(new HiddenScrollPane(detail));wrapper.add(preview,BorderLayout.SOUTH);return wrapper;
    }
    private void filter(){String query=search.getText().toLowerCase(Locale.ROOT);for(int n=0;n<sorters.size();n++){String severity=n==0?"WARNING":"ERROR";sorters.get(n).setRowFilter(new RowFilter<>(){public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){return entry.getStringValue(1).equals(severity)&&(query.isBlank()||entry.getStringValue(2).toLowerCase(Locale.ROOT).contains(query)||entry.getStringValue(3).toLowerCase(Locale.ROOT).contains(query));}});}logFilterChanged=true;revision=-1;refresh();updateNotice();}
    void showRunner(Workspace.Runner selected,boolean issues){runner=selected;capture=null;revision=-1;lastLine=0;rows.setRowCount(0);output.setText("");search.setText("");title.setText(selected.name+" · Diagnostics");tabs.setSelectedIndex(issues?0:2);filter();refresh();}
    void refresh(){
        if(runner==null||!live.isSelected())return;RunnerLogs current=owner.workspace.logs.get(runner.id);if(current==null){summary.setText(owner.workspace.processStatus(runner)+" · No captured session");notice.setText("No output captured yet. Start the runner to collect logs.");updateCopyButtons();return;}
        if(capture!=current){capture=current;revision=-1;lastLine=0;rows.setRowCount(0);output.setText("");}
        long next=capture.revision();if(next==revision)return;revision=next;var entries=capture.entries();if(entries.isEmpty())return;
        long first=entries.get(0).line();boolean rebuild=lastLine>0&&first>lastLine+1;
        if(rebuild){rows.setRowCount(0);output.setText("");lastLine=0;}
        // Trim dropped output from the front, then append only new lines instead of replacing the document.
        if(lastLine>0&&output.getDocument().getLength()>0){long storedFirst=lastLine-output.getLineCount()+2;int remove=(int)Math.max(0,first-storedFirst);if(remove>0)try{output.getDocument().remove(0,output.getLineStartOffset(Math.min(remove,output.getLineCount()-1)));}catch(Exception ignored){}}
        int dropped=0;while(dropped<rows.getRowCount()&&((Long)rows.getValueAt(dropped,0))<first)dropped++;if(dropped>0){rows.getDataVector().subList(0,dropped).clear();rows.fireTableRowsDeleted(0,dropped-1);}
        int firstAdded=rows.getRowCount();
        StringBuilder batch=new StringBuilder();for(var entry:entries)if(entry.line()>lastLine){batch.append(entry.text()).append('\n');if(entry.severity().equals("WARNING")||entry.severity().equals("ERROR"))rows.getDataVector().add(new Vector<>(List.of(entry.line(),entry.severity(),entry.source(),entry.text())));lastLine=entry.line();}
        if(rows.getRowCount()>firstAdded)rows.fireTableRowsInserted(firstAdded,rows.getRowCount()-1);
        int caret=output.getCaretPosition();if(search.getText().isBlank()&&!logFilterChanged)output.append(batch.toString());else{String query=search.getText().toLowerCase(Locale.ROOT);StringBuilder matching=new StringBuilder();for(var entry:entries)if(entry.text().toLowerCase(Locale.ROOT).contains(query)||entry.source().toLowerCase(Locale.ROOT).contains(query))matching.append(entry.text()).append('\n');output.setText(matching.toString());}logFilterChanged=false;output.setCaretPosition(follow.isSelected()?output.getDocument().getLength():Math.min(caret,output.getDocument().getLength()));updateNotice();
    }
    private void updateNotice(){
        if(sorters.size()<2)return;int warnings=0,errors=0;for(int n=0;n<rows.getRowCount();n++)if(rows.getValueAt(n,1).equals("WARNING"))warnings++;else errors++;
        tabs.setTitleAt(0,"Warnings ("+warnings+")");tabs.setTitleAt(1,"Errors ("+errors+")");
        if(runner!=null)summary.setText(owner.workspace.processStatus(runner)+" · "+warnings+" warning lines · "+errors+" error lines · latest captured session");
        int tab=tabs.getSelectedIndex();if(tab<2){int shown=sorters.get(Math.max(0,tab)).getViewRowCount();notice.setText(shown==0?(search.getText().isBlank()?"No "+(tab==0?"warnings":"errors")+" in retained output.":"No matching messages. Change or clear the filter."):shown+" matching lines · select rows for full text / copy · history limited to 2,500 log lines");}else notice.setText("Captured stdout/stderr and configured log file · filter matches literal text · bounded session history");updateCopyButtons();
    }
    private String selectedLines(JTable table,boolean all){StringBuilder text=new StringBuilder();int[] selected=all?java.util.stream.IntStream.range(0,table.getRowCount()).toArray():table.getSelectedRows();for(int row:selected){int index=table.convertRowIndexToModel(row);text.append('#').append(rows.getValueAt(index,0)).append(" [").append(rows.getValueAt(index,1)).append("] ").append(rows.getValueAt(index,2)).append(" · ").append(rows.getValueAt(index,3)).append('\n');}return text.toString();}
    private void copy(boolean all){int tab=tabs.getSelectedIndex();String text=tab==2?(all?output.getText():output.getSelectedText()):selectedLines(tables.get(tab),all);clipboard(text);}
    private void clipboard(String text){if(text==null||text.isEmpty())return;com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(new java.awt.datatransfer.StringSelection(text));notice.setText("Copied to clipboard.");}
    private void updateCopyButtons(){if(copySelected==null||copyFiltered==null)return;int tab=tabs.getSelectedIndex();if(tab<0)return;copySelected.setEnabled(tab==2?output.getSelectedText()!=null:tab<tables.size()&&tables.get(tab).getSelectedRowCount()>0);copyFiltered.setEnabled(tab==2?!output.getText().isBlank():tab<tables.size()&&tables.get(tab).getRowCount()>0);}
}
