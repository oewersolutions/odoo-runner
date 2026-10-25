package dev.odoorunner;

import com.google.gson.*;
import com.intellij.openapi.ui.Messages;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** In-place PostgreSQL tables, storage breakdown, paged records and explicit single-row updates. */
final class DatabaseBrowserPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final JLabel title=Design.label("Database",17,true),summary=new JLabel(),page=new JLabel();

    private final StatusMessage status=new StatusMessage();
    private final CardLayout cards=new VisibleCardLayout();
    private final JPanel pages=new JPanel(cards);
    private final JTextField tableSearch=new RoundTextField(),recordSearch=new RoundTextField();
    private final JComboBox<String> filterColumn=new RoundComboBox<>();
    private final JTable tables=new JTable(),records=new JTable();
    private final List<JsonObject> tableData=new ArrayList<>();
    private final JCheckBox systemObjects=new JCheckBox("System objects");
    private final JComboBox<String> schemaFilter=new RoundComboBox<>(),typeFilter=new RoundComboBox<>();
    private final JLabel tableCount=new JLabel(),selectedTable=new JLabel("Select a table to see its storage breakdown.");
    private boolean updatingFilters;
    private final javax.swing.Timer tableFilterTimer=new javax.swing.Timer(150,e->filterTables());
    private final DefaultTableModel tableModel=new DefaultTableModel(new String[]{"Name","Technical name","Odoo model (inferred)","Schema","Type","Estimated rows","Data","TOAST","Indexes","Total"},0){
        public boolean isCellEditable(int r,int c){return false;}
        public Class<?> getColumnClass(int c){return c<5?String.class:Long.class;}
    };
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(tableModel);
    private final JButton openTable,refreshTables,search,previous,next,edit,refreshRecords;
    private final List<JsonObject> rowData=new ArrayList<>();
    private JsonArray columns=new JsonArray();
    private Workspace.Installation installation;
    private Workspace.Runner runner;
    private String database,schema,table,appliedColumn="",appliedFilter="";
    private int offset,generation;
    private boolean busy,hasMore,closed;
    private DatabaseBridge bridge;

    DatabaseBrowserPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(0,10));this.owner=owner;
        title.setForeground(Design.BLUE);
        JPanel heading=new JPanel(new BorderLayout(12,0));
        heading.add(owner.button("← Runners",this::back),BorderLayout.WEST);heading.add(title);
        JPanel header=new JPanel(new BorderLayout(0,8));header.add(heading,BorderLayout.NORTH);header.add(summary,BorderLayout.CENTER);header.add(status,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);

        tables.setModel(tableModel);tables.setRowSorter(sorter);tables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tables.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);tables.setRowHeight(36);tables.setIntercellSpacing(new Dimension(12,4));
        tables.getTableHeader().setReorderingAllowed(false);tables.getTableHeader().setFont(tables.getTableHeader().getFont().deriveFont(Font.BOLD));
        tables.getTableHeader().setPreferredSize(new Dimension(100,36));
        DefaultTableCellRenderer sizes=new DefaultTableCellRenderer(){
            protected void setValue(Object value){setText(value instanceof Number number?bytes(number.longValue()):"—");setHorizontalAlignment(RIGHT);}
        };
        for(int c=6;c<10;c++)tables.getColumnModel().getColumn(c).setCellRenderer(sizes);
        tables.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer(){protected void setValue(Object value){setText(value instanceof Number n&&n.longValue()>=0?String.format("%,d",n.longValue()):"Unknown");setHorizontalAlignment(RIGHT);}});
        tables.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                super.getTableCellRendererComponent(t,value,selected,focus,row,column);setFont(new Font(Font.MONOSPACED,Font.PLAIN,t.getFont().getSize()));
                if(!selected)setForeground(Design.BLUE);setToolTipText(Objects.toString(value,""));return this;
            }
        });
        tables.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer(){
            protected void setValue(Object value){setText(Objects.toString(value,"").isBlank()?"—":value.toString());setToolTipText("Conventional Odoo model-to-table match from ir_model. Custom _table overrides cannot be resolved from this metadata.");}
        });
        int[] widths={210,280,190,110,145,120,105,105,105,105};
        for(int c=0;c<widths.length;c++){tables.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);tables.getColumnModel().getColumn(c).setMinWidth(85);}
        tableFilterTimer.setRepeats(false);
        tableSearch.setToolTipText("Search display name, SQL technical name, schema or Odoo model, e.g. res_partner or res.partner");
        tableSearch.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){tableFilterTimer.restart();}public void removeUpdate(DocumentEvent e){tableFilterTimer.restart();}public void changedUpdate(DocumentEvent e){tableFilterTimer.restart();}});
        tableSearch.addActionListener(e->{tableFilterTimer.stop();filterTables();});
        openTable=owner.button("Open table",this::openSelectedTable);
        refreshTables=UiIcons.button(owner,UiIcons.RESTART,"Refresh all table names and storage sizes",this::loadTables);
        systemObjects.setToolTipText("Include PostgreSQL catalog, information schema and internal tables. Other sessions' temporary schemas are excluded.");
        systemObjects.addActionListener(e->{if(!busy)loadTables();});
        schemaFilter.addItem("All schemas");typeFilter.addItem("All types");
        schemaFilter.addActionListener(e->{if(!updatingFilters)filterTables();});typeFilter.addActionListener(e->{if(!updatingFilters)filterTables();});
        JPanel tableTools=new JPanel(new BorderLayout(8,8));
        JPanel searchRow=new JPanel(new BorderLayout(8,0));searchRow.add(new JLabel("Search tables"),BorderLayout.WEST);searchRow.add(tableSearch);
        searchRow.add(owner.row(openTable,refreshTables),BorderLayout.EAST);tableTools.add(searchRow,BorderLayout.NORTH);
        JPanel filters=new JPanel(new GridLayout(1,2,8,0));filters.add(schemaFilter);filters.add(typeFilter);
        JPanel filterRow=new JPanel(new BorderLayout(12,0));filterRow.add(filters);filterRow.add(systemObjects,BorderLayout.EAST);tableTools.add(filterRow,BorderLayout.SOUTH);
        JPanel tablePage=new JPanel(new BorderLayout(0,10));tablePage.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));tablePage.add(tableTools,BorderLayout.NORTH);tablePage.add(new HiddenScrollPane(tables));
        JPanel footer=new JPanel(new GridLayout(0,1,0,6));footer.add(tableCount);footer.add(selectedTable);
        JLabel sizeHint=new JLabel("Shift+scroll for more columns · Data + TOAST + indexes = total · Partition sizes are per relation.");
        sizeHint.setToolTipText("Data includes free-space/visibility maps. TOAST includes overflow storage and its indexes. Row counts are estimates; views/foreign tables may have no local storage.");footer.add(sizeHint);
        tablePage.add(footer,BorderLayout.SOUTH);pages.add(tablePage,"tables");
        tables.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())updateTableSelection();});
        tables.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e)&&!busy)openSelectedTable();}});
        bindEnter(tables,this::openSelectedTable);

        records.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);records.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);records.setCellSelectionEnabled(false);
        records.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            protected void setValue(Object value){setText(value==null?"NULL":value.toString().replace('\n',' '));setToolTipText(value==null?"SQL NULL":value.toString().substring(0,Math.min(2000,value.toString().length())));}
        });
        search=owner.button("Search",()->{appliedColumn=filterColumn.getSelectedIndex()<=0?"":Objects.toString(filterColumn.getSelectedItem(),"");appliedFilter=recordSearch.getText();offset=0;loadRecords();});
        recordSearch.addActionListener(e->{if(!busy)search.doClick();});
        previous=UiIcons.button(owner,UiIcons.BACK,"Previous 100 records",()->loadRecords(Math.max(0,offset-100)));
        next=UiIcons.button(owner,UiIcons.FORWARD,"Next 100 records",()->loadRecords(offset+100));
        edit=UiIcons.button(owner,UiIcons.EDIT,"View / edit selected record",this::editSelected);
        refreshRecords=UiIcons.button(owner,UiIcons.RESTART,"Reload this page",this::loadRecords);
        JPanel filter=new JPanel(new BorderLayout(8,0));filter.add(filterColumn,BorderLayout.WEST);filterColumn.setPreferredSize(new Dimension(200,32));filter.add(recordSearch);filter.add(search,BorderLayout.EAST);
        JPanel recordTools=new JPanel(new BorderLayout(8,8));recordTools.add(owner.row(owner.button("← Tables",this::showTables),page,previous,next,edit,refreshRecords),BorderLayout.NORTH);recordTools.add(filter,BorderLayout.SOUTH);
        JPanel recordPage=new JPanel(new BorderLayout(0,10));recordPage.add(recordTools,BorderLayout.NORTH);recordPage.add(new HiddenScrollPane(records));
        recordPage.add(new JLabel("100 records per page · Shift+scroll moves across columns · Double-click a row to view or edit · NULL is shown explicitly."),BorderLayout.SOUTH);pages.add(recordPage,"records");
        records.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e)&&!busy)editSelected();}});bindEnter(records,this::editSelected);
        add(pages);ModernUi.apply(this);updateControls();
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0&&!isShowing()){closed=true;cancel();}});
    }

    void begin(Workspace.Installation installation,Workspace.Runner runner){
        if(owner.workspace.dependencies.busy(installation))throw new IllegalArgumentException("Wait for dependency installation to finish first.");
        if(!Files.isExecutable(Path.of(installation.python)))throw new IllegalArgumentException("Check the project's Python executable first.");
        String configured=RunnerWindow.Panel.additional(runner.extra).getOrDefault("db_name",runner.database);
        String db=runner.launchDatabase.isBlank()?configured:runner.launchDatabase;
        if(db.isBlank()||db.contains(",")){db=Messages.showInputDialog(owner.project,"Enter one database to browse.","Runner database",Messages.getQuestionIcon());if(db==null)return;db=db.trim();}
        if(db.isBlank()||db.contains(",")||db.contains("\n")||db.contains("\r"))throw new IllegalArgumentException("Choose one database name.");
        cancel();closed=false;this.installation=installation;this.runner=runner;this.database=db;schema=null;table=null;
        tableData.clear();tableModel.setRowCount(0);tableSearch.setText("");rowData.clear();records.setModel(readOnlyModel(new String[0]));
        title.setText(runner.name+" · Database: "+database);summary.setText("Connecting with saved runner database settings…");
        cards.show(pages,"tables");owner.projectCards.show(owner.projectPages,"database");loadTables();
    }
    private void back(){cancel();closed=true;owner.closeDiagnostics();}
    private void showTables(){if(busy)return;cards.show(pages,"tables");title.setText(runner.name+" · Database: "+database);status.message("Info","Select a table to browse its records. Storage figures reflect the last table refresh.");}
    private void filterTables(){
        if(updatingFilters)return;
        String needle=tableSearch.getText().trim().toLowerCase(Locale.ROOT);
        String selectedSchema=schemaFilter.getSelectedIndex()>0?Objects.toString(schemaFilter.getSelectedItem(),""):"";
        String selectedType=typeFilter.getSelectedIndex()>0?Objects.toString(typeFilter.getSelectedItem(),""):"";
        sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>(){
            public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){
                if(!selectedSchema.isEmpty()&&!entry.getStringValue(3).equals(selectedSchema))return false;
                if(!selectedType.isEmpty()&&!entry.getStringValue(4).equals(selectedType))return false;
                if(needle.isEmpty())return true;
                for(int c=0;c<5;c++)if(entry.getStringValue(c).toLowerCase(Locale.ROOT).contains(needle))return true;
                return false;
            }
        });
        tableCount.setText(tables.getRowCount()+" of "+tableModel.getRowCount()+" database objects"+(tables.getRowCount()==0?" · No matches. Clear the search or adjust the filters.":""));
        updateTableSelection();
    }
    private void updateTableSelection(){
        int row=tables.getSelectedRow();openTable.setEnabled(!busy&&row>=0);
        if(row<0){selectedTable.setText("Select a table to see its storage breakdown.");return;}
        int model=tables.convertRowIndexToModel(row);if(model<0||model>=tableData.size())return;JsonObject object=tableData.get(model);
        selectedTable.setText(object.get("schema").getAsString()+"."+object.get("table").getAsString()+" · Data "+bytes(object.get("data").getAsLong())+" · TOAST "+bytes(object.get("toast").getAsLong())+" · Indexes "+bytes(object.get("indexes").getAsLong())+" · Total "+bytes(object.get("total").getAsLong()));
        selectedTable.setToolTipText(selectedTable.getText());
    }
    private void loadTables(){
        if(busy)return;JsonObject request=request("tables");request.addProperty("system",systemObjects.isSelected());
        run(request,result->{
            tableData.clear();tableModel.getDataVector().clear();
            SortedSet<String> schemas=new TreeSet<>(),types=new TreeSet<>();
            for(JsonElement item:result.getAsJsonArray("tables")){
                JsonObject r=item.getAsJsonObject();tableData.add(r);String ns=r.get("schema").getAsString(),kind=r.get("kind").getAsString();schemas.add(ns);types.add(kind);
                Object[] values={r.get("name").getAsString(),ns+"."+r.get("table").getAsString(),r.get("models").getAsString(),ns,kind,r.get("estimated").getAsLong(),r.get("data").getAsLong(),r.get("toast").getAsLong(),r.get("indexes").getAsLong(),r.get("total").getAsLong()};
                tableModel.getDataVector().add(new Vector<>(Arrays.asList(values)));
            }
            updatingFilters=true;
            Object oldSchema=schemaFilter.getSelectedItem(),oldType=typeFilter.getSelectedItem();
            schemaFilter.removeAllItems();schemaFilter.addItem("All schemas");for(String ns:schemas)schemaFilter.addItem(ns);
            typeFilter.removeAllItems();typeFilter.addItem("All types");for(String kind:types)typeFilter.addItem(kind);
            if(schemas.contains(oldSchema))schemaFilter.setSelectedItem(oldSchema);if(types.contains(oldType))typeFilter.setSelectedItem(oldType);
            updatingFilters=false;tableModel.fireTableDataChanged();filterTables();
            summary.setText(installation.name+" · Database size: "+bytes(result.get("databaseBytes").getAsLong())+" · "+tableModel.getRowCount()+" objects loaded");
            status.message("Success","All matching database tables and views loaded; no table-count cutoff. System objects "+(systemObjects.isSelected()?"included.":"hidden (enable System objects to include them)."));
        },null);
    }
    private void openSelectedTable(){
        if(busy||tables.getSelectedRow()<0)return;int selected=tables.convertRowIndexToModel(tables.getSelectedRow());JsonObject object=tableData.get(selected);
        schema=object.get("schema").getAsString();table=object.get("table").getAsString();offset=0;appliedColumn="";appliedFilter="";recordSearch.setText("");filterColumn.removeAllItems();loadRecords();
    }
    private void loadRecords(){loadRecords(offset);}
    private void loadRecords(int requestedOffset){
        if(busy||table==null)return;
        JsonObject request=request("records");request.addProperty("offset",requestedOffset);request.addProperty("column",appliedColumn);request.addProperty("filter",appliedFilter);
        run(request,result->{
            columns=result.getAsJsonArray("columns");rowData.clear();
            List<String> names=new ArrayList<>();for(JsonElement col:columns)names.add(col.getAsJsonObject().get("name").getAsString());
            DefaultTableModel model=readOnlyModel(names.toArray(String[]::new));
            for(JsonElement row:result.getAsJsonArray("rows")){
                JsonObject r=row.getAsJsonObject();rowData.add(r);Object[] values=new Object[names.size()];JsonArray cells=r.getAsJsonArray("values");for(int n=0;n<values.length;n++)values[n]=cells.get(n).isJsonNull()?null:cells.get(n).getAsString();model.addRow(values);
            }
            records.setModel(model);for(int c=0;c<records.getColumnCount();c++)records.getColumnModel().getColumn(c).setPreferredWidth(180);
            filterColumn.removeAllItems();filterColumn.addItem("All columns");for(String name:names)filterColumn.addItem(name);filterColumn.setSelectedItem(appliedColumn.isEmpty()?"All columns":appliedColumn);
            hasMore=result.get("hasMore").getAsBoolean();offset=result.get("offset").getAsInt();
            page.setText(rowData.isEmpty()?"No records":"Records "+(offset+1)+"–"+(offset+rowData.size()));title.setText(database+" / "+schema+"."+table);
            boolean pk=false,editable=false;for(JsonElement col:columns){pk|=col.getAsJsonObject().get("pk").getAsBoolean();editable|=col.getAsJsonObject().get("editable").getAsBoolean();}
            status.message(pk&&editable?"Success":"Info",pk&&editable?"Records loaded. Select a row to view or edit; changes require Save.":"Read-only records: this object has no editable primary-key-backed fields, or is a view/system/foreign table. Column search is available.");
            cards.show(pages,"records");
        },null);
    }
    private JsonObject request(String action){JsonObject request=DatabaseBridge.request(runner,database,action);if(table!=null){request.addProperty("schema",schema);request.addProperty("table",table);}return request;}
    private void run(JsonObject request,Consumer<JsonObject> success,Runnable failure){
        busy=true;updateControls();status.message("Info",request.get("action").getAsString().equals("update")?"Saving record…":"Loading database…");
        int token=++generation;DatabaseBridge job=new DatabaseBridge();bridge=job;Workspace.Installation selected=installation;
        new SwingWorker<JsonObject,Void>(){
            protected JsonObject doInBackground()throws Exception{return job.execute(selected,request);}
            protected void done(){
                if(token!=generation||closed)return;busy=false;bridge=null;
                try{success.accept(get());}catch(Exception ex){Throwable cause=ex.getCause()==null?ex:ex.getCause();lastError=Objects.toString(cause.getMessage(),"Database request failed.");status.message("Error",lastError);if(failure!=null)failure.run();}
                finally{updateControls();}
            }
        }.execute();
    }
    private void updateControls(){for(JButton b:List.of(openTable,refreshTables,search,edit,refreshRecords))b.setEnabled(!busy);previous.setEnabled(!busy&&offset>0);next.setEnabled(!busy&&hasMore);systemObjects.setEnabled(!busy);updateTableSelection();}
    private void cancel(){generation++;busy=false;if(bridge!=null)bridge.cancel();bridge=null;}
    void dispose(){tableFilterTimer.stop();closed=true;cancel();}
    private static DefaultTableModel readOnlyModel(String[] names){return new DefaultTableModel(names,0){public boolean isCellEditable(int row,int column){return false;}};}
    private static String bytes(long value){if(value<1024)return value+" B";double size=value;String[] units={"B","KiB","MiB","GiB","TiB"};int unit=0;while(size>=1024&&unit<units.length-1){size/=1024;unit++;}return String.format(java.util.Locale.ROOT,"%.1f %s",size,units[unit]);}
    private static void bindEnter(JTable table,Runnable action){table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"open-record");table.getActionMap().put("open-record",new AbstractAction(){public void actionPerformed(ActionEvent e){action.run();}});}
    private void editSelected(){
        if(busy||records.getSelectedRow()<0)return;
        JsonObject row=rowData.get(records.convertRowIndexToModel(records.getSelectedRow()));new RecordEditor(row).show();
    }
    private final class RecordEditor extends com.intellij.openapi.ui.DialogWrapper {
        private final JsonObject row;
        private final Map<String,JTextArea> values=new LinkedHashMap<>();
        private final Map<String,JCheckBox> nulls=new LinkedHashMap<>();
        private final StatusMessage message=new StatusMessage();
        private boolean saving;
        private final boolean writable;
        RecordEditor(JsonObject row){
            super(owner.project,true);this.row=row;
            boolean key=false,editable=false;for(JsonElement col:columns){key|=col.getAsJsonObject().get("pk").getAsBoolean();editable|=col.getAsJsonObject().get("editable").getAsBoolean();}writable=key&&editable&&!row.get("truncated").getAsBoolean();
            setTitle(schema+"."+table+" · Record");setOKButtonText("Save record");init();setResizable(true);
            setOKActionEnabled(writable&&!values.isEmpty());
        }
        protected JComponent createCenterPanel(){
            JPanel panel=new JPanel(new BorderLayout(0,10));panel.setPreferredSize(new Dimension(760,540));
            JPanel notice=new JPanel(new BorderLayout(0,5));
            notice.add(new JLabel(writable?"Direct PostgreSQL edit: bypasses Odoo validation, computed fields and access rules.":"Read-only object, no editable fields/primary key, or a value exceeds 100,000 characters."),BorderLayout.NORTH);
            notice.add(message);panel.add(notice,BorderLayout.NORTH);ScrollForm form=new ScrollForm();JPanel group=ScrollForm.group("Database record");
            JsonArray original=row.getAsJsonArray("values");int index=0;
            for(JsonElement item:columns){
                JsonObject col=item.getAsJsonObject();String name=col.get("name").getAsString();JsonElement value=original.get(index++);
                JTextArea field=new JTextArea(value.isJsonNull()?"":value.getAsString(),2,30);field.setLineWrap(true);field.setWrapStyleWord(false);
                boolean allowed=writable&&col.get("editable").getAsBoolean();field.setEditable(allowed);
                JCheckBox nil=new JCheckBox("SQL NULL",value.isJsonNull());nil.setEnabled(allowed&&!col.get("notNull").getAsBoolean());
                field.setEnabled(!nil.isSelected());nil.addActionListener(e->field.setEnabled(!nil.isSelected()));
                JPanel control=new JPanel(new BorderLayout(6,3));JScrollPane scroll=new HiddenScrollPane(field);scroll.setPreferredSize(new Dimension(320,56));control.add(scroll);control.add(nil,BorderLayout.EAST);
                String label=name+(col.get("pk").getAsBoolean()?" (primary key)":"");control.setToolTipText(col.get("type").getAsString()+(allowed?"":" · read-only"));
                ScrollForm.field(group,label,control);if(allowed){values.put(name,field);nulls.put(name,nil);}
            }
            form.section(group);panel.add(new HiddenScrollPane(form));ModernUi.apply(panel);return panel;
        }
        protected void doOKAction(){
            if(saving||!writable)return;JsonObject changes=new JsonObject();JsonArray original=row.getAsJsonArray("values");
            for(int n=0;n<columns.size();n++){
                String name=columns.get(n).getAsJsonObject().get("name").getAsString();if(!values.containsKey(name))continue;
                String after=nulls.get(name).isSelected()?null:values.get(name).getText();JsonElement before=original.get(n);
                if(!Objects.equals(after,before.isJsonNull()?null:before.getAsString())){if(after==null)changes.add(name,JsonNull.INSTANCE);else changes.addProperty(name,after);}
            }
            if(changes.size()==0){message.message("Info","No changes to save.");return;}
            JsonObject request=request("update");request.add("changes",changes);request.add("original",original.deepCopy());request.add("xmin",row.get("xmin"));
            saving=true;setOKActionEnabled(false);setCancelButtonText("Saving…");message.message("Info","Saving changes…");
            run(request,result->{saving=false;super.doOKAction();status.message("Success","Record saved.");loadRecords();},()->{
                saving=false;setCancelButtonText("Cancel");setOKActionEnabled(true);message.message("Error",lastError);
            });
        }
        public void doCancelAction(){if(!saving)super.doCancelAction();}
    }
    private String lastError="";
}
