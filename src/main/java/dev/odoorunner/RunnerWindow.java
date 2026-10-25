package dev.odoorunner;

import com.intellij.openapi.project.Project;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.BrowserUtil;
import javax.swing.*;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public final class RunnerWindow {
    static final class Panel extends JPanel {
        final Project project;
        final Workspace workspace;
        final JComboBox<Workspace.Installation> installations = new RoundComboBox<>();
        final JComboBox<Workspace.Runner> runners = new RoundComboBox<>();
        final StatusMessage status = new StatusMessage();
        final CardLayout projectCards = new VisibleCardLayout();
        final JPanel projectPages = new JPanel(projectCards);
        final CardLayout mainSectionCards=new VisibleCardLayout();
        final JPanel mainSection=new JPanel(mainSectionCards);
        final ProjectSetupPanel projectSetup;
        final RunnerListPanel runnerList;
        final ProjectListPanel projectSidebar;
        final JSplitPane projectsSplit;
        final RunnerDiagnosticsPanel diagnostics;
        final DependencyPanel dependencyPanel;
        final OdooShellPanel shellPanel;
        final DatabaseBrowserPanel databaseBrowser;
        final RunnerPerformancePanel performance;
        boolean setupVisible, setupEditing, modulePickerVisible;
        final AddonPathsPanel addonPaths,customAddonPaths;
        final DeploymentOptionsPanel deployment=new DeploymentOptionsPanel();
        final ModulePickerPanel modulePicker;
        final Map<String,JTextField> launch = new LinkedHashMap<>();
        final JCheckBox testEnable = new JCheckBox("Run tests (--test-enable)");
        final JCheckBox stopAfterInit = new JCheckBox("Stop after initialization (--stop-after-init)");
        final DefaultTableModel optionRows = new DefaultTableModel(new String[]{"odoo.conf option", "Value"},0);
        final JTable optionTable = new JTable(optionRows);
        final JLabel projectTitle = new JLabel();
        final JLabel editorRunnerName=Design.label("",19,true),editorRunnerDetails=Design.label("",13,false),editorRunnerStatus=new JLabel();
        final JTextArea sourceSummary = new JTextArea();
        final Map<String,JLabel> sourcePathLabels=new LinkedHashMap<>();
        long layoutBusyUntil;
        boolean creatingRunner;
        final javax.swing.Timer refreshTimer;
        final javax.swing.Timer diagnosticsTimer;
        JButton runnerSave;
        String savedSnapshot = "";
        Workspace.Runner editingRunner;
        final Map<String,JTextField> fields = new LinkedHashMap<>();
        final Map<String,JPasswordField> secrets = new LinkedHashMap<>();
        final JCheckBox clearSecrets = new JCheckBox("Clear saved passwords on Save");
        final JTextArea extra = new JTextArea(10,40);
        final JComboBox<String> eventKey = new RoundComboBox<>(new String[]{"gevent_port", "longpolling_port"});
        boolean loading;
        Panel(Project project) {
            super(new BorderLayout(8,8)); this.project=project; workspace=project.getService(Workspace.class);
            Rectangle usable=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            setPreferredSize(new Dimension(Math.min(1280,Math.max(640,usable.width-80)),Math.min(860,Math.max(480,usable.height-100))));
            setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

            addonPaths=new AddonPathsPanel(this,"Standard / enterprise addon folders (in search order)");
            customAddonPaths=new AddonPathsPanel(this,"Custom addon folders (in search order)");
            modulePicker=new ModulePickerPanel(this);projectPages.add(modulePicker,"modules");
            projectSetup=new ProjectSetupPanel(this,this::saveProjectSetup,this::cancelProjectSetup);
            projectPages.add(projectSetup,"setup");
            runnerList=new RunnerListPanel(this);projectPages.add(runnerList,"runners");
            projectSidebar=new ProjectListPanel(this);
            diagnostics=new RunnerDiagnosticsPanel(this);dependencyPanel=new DependencyPanel(this);projectPages.add(dependencyPanel,"dependencies");

            projectPages.add(diagnostics,"diagnostics");
            shellPanel=new OdooShellPanel(this);projectPages.add(shellPanel,"shell");
            databaseBrowser=new DatabaseBrowserPanel(this);projectPages.add(databaseBrowser,"database");
            JPanel empty=new JPanel(new BorderLayout());empty.add(new JLabel("Select a project or choose Add project.",SwingConstants.CENTER));projectPages.add(empty,"empty");
            projectPages.setMinimumSize(new Dimension(360,120));
            mainSection.add(projectPages,"projects");
            projectsSplit=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,projectSidebar,mainSection);
            projectsSplit.setContinuousLayout(true);projectsSplit.setResizeWeight(0.30);projectsSplit.setDividerSize(7);projectsSplit.setBorder(BorderFactory.createEmptyBorder());
            projectsSplit.addHierarchyListener(event->{if((event.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0&&projectsSplit.isShowing()){
                double ratio=workspace.getState().projectsSplit;if(!Double.isFinite(ratio)||ratio<0.1||ratio>0.9)ratio=0.30;
                projectsSplit.setDividerLocation(ratio);
            }});
            projectsSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,event->{layoutBusyUntil=System.nanoTime()+150_000_000L;if(projectsSplit.isShowing()&&projectsSplit.getWidth()>projectsSplit.getDividerSize())workspace.getState().projectsSplit=(double)projectsSplit.getDividerLocation()/(projectsSplit.getWidth()-projectsSplit.getDividerSize());});
            add(projectsSplit);
            editorRunnerName.setForeground(Design.BLUE);projectTitle.setForeground(Design.BLUE);
            JPanel editor=new JPanel(new BorderLayout(8,8));
            JPanel top=new JPanel(new BorderLayout(10,12));
            top.add(row(button("← Runners",this::backToRunners),projectTitle),BorderLayout.NORTH);
            JPanel summary=new JPanel(new BorderLayout(14,8));summary.add(Design.badge(UiIcons.DATABASE),BorderLayout.WEST);
            JPanel identity=new JPanel(new BorderLayout(0,6));JPanel nameLine=new JPanel(new BorderLayout(12,0));
            JPanel titleLine=new JPanel();titleLine.setLayout(new BoxLayout(titleLine,BoxLayout.X_AXIS));titleLine.add(editorRunnerName);titleLine.add(Box.createHorizontalStrut(8));titleLine.add(editorRunnerStatus);nameLine.add(titleLine);
            JPanel tools=new JPanel();tools.setLayout(new BoxLayout(tools,BoxLayout.X_AXIS));
            runnerSave=UiIcons.button(this,UiIcons.SAVE,"Save runner changes",this::save);runnerSave.setForeground(Design.BLUE);runnerSave.setVisible(false);
            JButton remove=UiIcons.button(this,UiIcons.DELETE,"Remove runner",this::removeRunner);remove.setForeground(Design.DANGER);
            for(JButton action:List.of(runnerSave,UiIcons.button(this,UiIcons.PLAY,"Run runner",this::run),UiIcons.button(this,UiIcons.STOP,"Stop runner",this::stop),UiIcons.button(this,UiIcons.GLOBE,"Open in browser",this::browse),UiIcons.button(this,UiIcons.TERMINAL,"Preview command",this::previewCommand),UiIcons.button(this,UiIcons.LOG,"Preview odoo.conf",this::preview),remove)){tools.add(action);tools.add(Box.createHorizontalStrut(6));}
            nameLine.add(tools,BorderLayout.EAST);identity.add(nameLine,BorderLayout.NORTH);identity.add(editorRunnerDetails);summary.add(identity);top.add(summary);
            editor.add(top,BorderLayout.NORTH);
            for(String key:List.of("Runner name","HTTP port","Event port","Addons paths (comma separated)","Database name(s)","Database host","Database port","Database user","Workers","Data directory"))fields.put(key,new RoundTextField());
            for(String key:RunnerEngine.SECRETS)secrets.put(key,new RoundPasswordField());
            JTabbedPane configs=new JTabbedPane();configs.addTab("odoo-bin · Launch & tests",launchPanel());configs.addTab("odoo.conf · Server configuration",configPanel());performance=new RunnerPerformancePanel(this);configs.addTab("Performance",performance);editor.add(configs);
            top.add(status,BorderLayout.SOUTH);projectPages.add(editor,"form");
            runners.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);if(value instanceof Workspace.Runner r)setText(r.name+" · HTTP "+r.httpPort+" · event "+r.eventPort+" · "+processStatus(r));return this;}});
            installations.addActionListener(e->{if(!loading)refreshRunners();});
            runners.addActionListener(e->{if(!loading){if(!canLeave()){loading=true;runners.setSelectedItem(editingRunner);loading=false;return;}load();}});
            loading=true;for(var i:workspace.getState().installations)installations.addItem(i);loading=false;refreshRunners();refreshProjects();if(installation()!=null)openProject(installation());else projectCards.show(projectPages,"empty");
            refreshTimer=new javax.swing.Timer(1000,e->{if(System.nanoTime()<layoutBusyUntil)return;refreshProjects();if(runnerList.isShowing())runnerList.refresh();if(dependencyPanel.isShowing())dependencyPanel.refresh();});refreshTimer.start();
            diagnosticsTimer=new javax.swing.Timer(100,e->{if(System.nanoTime()<layoutBusyUntil)return;if(diagnostics.isShowing())diagnostics.refresh();if(editorRunnerName.isShowing())refreshRunnerSave();if(dependencyPanel.isShowing())dependencyPanel.refresh();});diagnosticsTimer.start();
            ModernUi.apply(this);
        }
        void dispose(){performance.dispose();databaseBrowser.dispose();shellPanel.dispose();refreshTimer.stop();diagnosticsTimer.stop();projectSetup.dispose();modulePicker.dispose();}
        JComponent configPanel(){
            ScrollForm content=new ScrollForm();
            JPanel general=ScrollForm.group("Runner");ScrollForm.field(general,"Runner name",fields.get("Runner name"));content.section(general);
            JPanel database=ScrollForm.group("Database configuration");
            for(String key:List.of("Database name(s)","Database host","Database port","Database user"))ScrollForm.field(database,key,fields.get(key));
            ScrollForm.field(database,"Database password (blank keeps saved)",secrets.get("db_password"));
            deployment.addTo(database,this,"dbfilter","db_sslmode","db_maxconn","db_template","list_db");content.section(database);
            JPanel directories=ScrollForm.group("Addons and directories");
            GridBagConstraints full=new GridBagConstraints();full.gridx=0;full.gridy=0;full.gridwidth=2;full.weightx=1;full.fill=GridBagConstraints.HORIZONTAL;full.insets=new Insets(6,8,6,8);directories.add(addonPaths,full);full.gridy=1;directories.add(customAddonPaths,full);
            // Addon lists span both columns; directory fields follow underneath them.
            JPanel pathFields=ScrollForm.group("Data and PostgreSQL tools");ScrollForm.field(pathFields,"Data directory",pathPicker(fields.get("Data directory"),true));deployment.addTo(pathFields,this,"pg_path");
            full.gridy=2;directories.add(pathFields,full);content.section(directories);
            JPanel network=ScrollForm.group("HTTP, event ports and reverse proxy");
            ScrollForm.field(network,"HTTP port",new AvailablePortPicker(this,fields.get("HTTP port")));ScrollForm.field(network,"Event port",new AvailablePortPicker(this,fields.get("Event port")));ScrollForm.field(network,"Event option",eventKey);
            deployment.addTo(network,this,"http_interface","proxy_mode");content.section(network);
            JPanel workers=ScrollForm.group("Workers and resource limits");ScrollForm.field(workers,"Workers",fields.get("Workers"));deployment.addTo(workers,this,"max_cron_threads","limit_memory_soft","limit_memory_hard","limit_time_cpu","limit_time_real","limit_request");content.section(workers);
            JPanel logs=ScrollForm.group("Logs");deployment.addTo(logs,this,"logfile","log_handler","log_db");content.section(logs);
            JPanel mail=ScrollForm.group("Outgoing mail (SMTP)");deployment.addTo(mail,this,"smtp_server","smtp_port","smtp_user","smtp_ssl","email_from");ScrollForm.field(mail,"SMTP password (blank keeps saved)",secrets.get("smtp_password"));content.section(mail);
            JPanel security=ScrollForm.group("Master password and saved credentials");ScrollForm.field(security,"Master password (blank keeps saved)",secrets.get("admin_passwd"));ScrollForm.field(security,"Password Safe",clearSecrets);content.section(security);
            JPanel options=ScrollForm.group("Additional options");
            JPanel table=new JPanel(new BorderLayout());optionTable.setRowHeight(28);table.add(optionTable.getTableHeader(),BorderLayout.NORTH);table.add(optionTable);table.add(row(button("Add option",()->optionRows.addRow(new Object[]{"",""})),button("Remove option",()->{int index=optionTable.getSelectedRow();if(index>=0)optionRows.removeRow(index);})),BorderLayout.SOUTH);
            ScrollForm.field(options,"Key / value",table);content.section(options);
            return new HiddenScrollPane(content);
        }
        JComponent launchPanel(){
            ScrollForm content=new ScrollForm();
            sourceSummary.setEditable(false);sourceSummary.setOpaque(false);sourceSummary.setLineWrap(true);sourceSummary.setWrapStyleWord(true);
            JPanel source=ScrollForm.group("Python and Odoo source");
            JPanel paths=new JPanel(new GridBagLayout());int sourceRow=0;for(String key:List.of("Project paths","Python","Entry script","Working directory")){GridBagConstraints c=new GridBagConstraints();c.gridy=sourceRow++;c.gridx=0;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(3,0,3,18);paths.add(new JLabel(key),c);JLabel value=new JLabel();value.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));sourcePathLabels.put(key,value);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;paths.add(value,c);}
            JPanel sourceBody=new JPanel(new BorderLayout(16,0));sourceBody.add(paths);JTextArea pathHint=new JTextArea("Change these paths\nusing Edit project.");pathHint.setEditable(false);pathHint.setOpaque(false);pathHint.setFont(pathHint.getFont().deriveFont(12f));sourceBody.add(pathHint,BorderLayout.EAST);GridBagConstraints full=new GridBagConstraints();full.gridx=0;full.gridy=0;full.gridwidth=2;full.weightx=1;full.fill=GridBagConstraints.HORIZONTAL;source.add(sourceBody,full);content.section(source);
            JPanel modules=ScrollForm.group("Database and module operations"),tests=ScrollForm.group("Tests"),development=ScrollForm.group("Development and startup");
            for(String key:List.of("Database override (--database)","Install modules (--init)","Update modules (--update)","Test tags / module:class.method (--test-tags)","Test file (--test-file)","Development features (--dev)","Skip demo modules (--without-demo)")){
                var input=new RoundTextField();launch.put(key,input);JComponent control=input;
                if(key.equals("Install modules (--init)")||key.equals("Update modules (--update)")){input.setEditable(false);JPanel picker=new JPanel(new BorderLayout(4,0));picker.add(input);picker.add(button("Select modules…",()->openModulePicker(input)),BorderLayout.EAST);control=picker;}
                JPanel group=key.startsWith("Test")?tests:key.startsWith("Development")||key.startsWith("Skip")?development:modules;
                String label=key.startsWith("Test tags")?"Test tags / module:class.method":key.replaceAll(" \\(.*\\)$","");String hint=key.startsWith("Database")?"Specify a database name to override the default.":key.startsWith("Test tags")?"Run specific tests using tags or module:class.method format.":key.startsWith("Test file")?"Path to a specific test file.":key.startsWith("Development")?"Comma-separated development features.":key.startsWith("Skip")?"Comma-separated modules to skip demo data for.":"";
                if(!hint.isEmpty()){JPanel withHint=new JPanel(new BorderLayout(0,3));withHint.add(control);withHint.add(Design.label(hint,11,false),BorderLayout.SOUTH);control=withHint;}ScrollForm.field(group,label,control);
            }
            ScrollForm.field(tests,"Test execution",testEnable);ScrollForm.field(development,"Startup mode",stopAfterInit);
            content.section(modules);content.section(tests);content.section(development);
            JTextArea help=new JTextArea("Launch arguments override matching odoo.conf values. Tests run during module install/update. Saved install/update/test options run again on each launch. Choose options supported by your Odoo version.");help.setEditable(false);help.setLineWrap(true);help.setWrapStyleWord(true);help.setOpaque(false);
            JPanel notes=ScrollForm.group("Launch notes");ScrollForm.field(notes,"",help);content.section(notes);return new HiddenScrollPane(content);
        }
        void openDiagnostics(Workspace.Runner r,boolean issues){diagnostics.showRunner(r,issues);projectCards.show(projectPages,"diagnostics");}
        void closeDiagnostics(){runnerList.showProject(installation());projectCards.show(projectPages,"runners");}
        void openDependencies(Workspace.Installation i){dependencyPanel.begin(i);projectCards.show(projectPages,"dependencies");}
        void closeDependencies(){runnerList.showProject(installation());projectCards.show(projectPages,"runners");}
        void refreshProjects(){projectSidebar.refresh();}
        boolean switchProject(Workspace.Installation selected){
            if(setupVisible){if(!projectSetup.canLeave())return false;setupVisible=false;}
            else if((modulePickerVisible&&!modulePicker.canLeave())||!canLeave())return false;
            modulePickerVisible=false;
            load();openProject(selected);return true;
        }
        void openProject(Workspace.Installation i){mainSectionCards.show(mainSection,"projects");installations.setSelectedItem(i);updateProjectHeading();runnerList.showProject(i);projectSidebar.select(i);projectCards.show(projectPages,"runners");}
        void openRunnerSettings(Workspace.Runner selected){
            if(!canLeave())return;loading=true;runners.setSelectedItem(selected);loading=false;load();projectTitle.setText("/ "+installation().name+" / "+selected.name);editorRunnerName.setText(selected.name);boolean running=workspace.active(selected);editorRunnerStatus.setText(running?"Active":"Inactive");editorRunnerStatus.setIcon(UiIcons.dot(running?new Color(55,170,95):new Color(145,145,150)));editorRunnerDetails.setText("HTTP "+selected.httpPort+" · Event "+selected.eventPort+" · Database: "+(selected.launchDatabase.isBlank()?selected.database:selected.launchDatabase));projectCards.show(projectPages,"form");
        }
        void openModulePicker(JTextField target){
            List<Path> roots=new ArrayList<>(addonPaths.validated(Path.of(installation().root)));roots.addAll(customAddonPaths.validated(Path.of(installation().root)));
            if(roots.isEmpty())throw new IllegalArgumentException("Add addon folders in odoo.conf configuration first.");
            modulePicker.begin(target,roots);modulePickerVisible=true;projectCards.show(projectPages,"modules");
        }
        void closeModulePicker(){modulePickerVisible=false;modulePicker.dispose();projectCards.show(projectPages,"form");}
        void backToRunners(){if(!canLeave())return;load();runnerList.showProject(installation());projectCards.show(projectPages,"runners");}
        void updateProjectHeading(){var i=installation();if(i==null)return;projectTitle.setText(i.name);String[] sourceValues={i.root,i.python,Path.of(i.root).resolve(i.script).toString(),i.root};int sourceIndex=0;for(var label:sourcePathLabels.values()){label.setText(sourceValues[sourceIndex]);label.setToolTipText(sourceValues[sourceIndex++]);}sourceSummary.setText("Python: "+i.python+"\nEntry script: "+Path.of(i.root).resolve(i.script)+"\nWorking directory: "+i.root+"\nChange these paths using Edit project.\n");}
        String processStatus(Workspace.Runner r){return workspace.processStatus(r);}
        String snapshot(){StringBuilder b=new StringBuilder();b.append(addonPaths.value()).append('\0').append(customAddonPaths.value()).append('\0').append(deployment.values()).append('\0');fields.values().forEach(f->b.append(f.getText()).append('\0'));launch.values().forEach(f->b.append(f.getText()).append('\0'));b.append(testEnable.isSelected()).append(stopAfterInit.isSelected()).append(eventKey.getSelectedItem()).append(clearSecrets.isSelected());for(int n=0;n<optionRows.getRowCount();n++)b.append(optionRows.getValueAt(n,0)).append('\0').append(optionRows.getValueAt(n,1)).append('\0');return b.toString();}
        boolean runnerDirty(){
            if(editingRunner==null)return false;
            for(var field:secrets.values()){char[] value=field.getPassword();boolean changed=value.length>0;Arrays.fill(value,'\0');if(changed)return true;}
            if(!snapshot().equals(savedSnapshot))return true;
            if(optionTable.isEditing()){int row=optionTable.getEditingRow(),column=optionTable.getEditingColumn();if(row>=0&&column>=0&&!Objects.equals(Objects.toString(optionTable.getCellEditor().getCellEditorValue(),""),Objects.toString(optionRows.getValueAt(row,column),"")))return true;}
            return false;
        }
        void refreshRunnerSave(){if(runnerSave==null)return;boolean dirty=runnerDirty();if(runnerSave.isVisible()!=dirty){runnerSave.setVisible(dirty);runnerSave.getParent().revalidate();runnerSave.getParent().repaint();}}
        boolean canLeave(){if(optionTable.isEditing())optionTable.getCellEditor().stopCellEditing();boolean secretChanged=secrets.values().stream().anyMatch(f->f.getPassword().length>0);return editingRunner==null||(!secretChanged&&snapshot().equals(savedSnapshot))||Messages.showYesNoDialog(project,"Discard unsaved runner changes? Choose No to stay and save them.","Unsaved runner",Messages.getQuestionIcon())==Messages.YES;}
        boolean canClose(){return setupVisible?projectSetup.canLeave():(!modulePickerVisible||modulePicker.canLeave())&&canLeave();}
        void back(){if(!canLeave())return;load();refreshProjects();projectCards.show(projectPages,"empty");}
        void readOptionTable(){if(optionTable.isEditing()&&!optionTable.getCellEditor().stopCellEditing())throw new IllegalArgumentException("Finish editing the option first.");StringBuilder out=new StringBuilder();for(int n=0;n<optionRows.getRowCount();n++){String key=Objects.toString(optionRows.getValueAt(n,0),"").trim(),value=Objects.toString(optionRows.getValueAt(n,1),"").trim();if(key.isBlank()&&value.isBlank())continue;if(key.contains("\n")||key.contains("\r")||value.contains("\n")||value.contains("\r"))throw new IllegalArgumentException("Additional options must be single-line values.");out.append(key).append(" = ").append(value).append('\n');}extra.setText(out.toString());}
        JPanel row(Component... components) { JPanel p=new JPanel(new WrapLayout()); for(var c:components)p.add(c); return p; }
        JButton button(String text, Runnable action) { JButton b=new RoundButton(text);b.putClientProperty("odoo.button",true);b.setIcon(switch(text){case "← Runners","Runners"->UiIcons.BACK;case "Save"->UiIcons.SAVE;case "Run"->UiIcons.PLAY;case "Stop"->UiIcons.STOP;case "Preview command"->UiIcons.TERMINAL;case "Preview odoo.conf"->UiIcons.LOG;case "Open browser"->UiIcons.GLOBE;case "Select modules…","Browse…"->UiIcons.FOLDER;default->null;});b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));if(text.equals("← Runners")||text.equals("Runners")){b.setText("Runners");b.setForeground(Design.BLUE);}
            b.setIconTextGap(9);if(text.equals("Run")||text.equals("Stop"))b.putClientProperty("odoo.compact",true);if(text.equals("Save")||text.equals("Run"))Design.primary(b);ModernUi.sizeButton(b); b.addActionListener(e->{try{action.run();}catch(Exception ex){status.message("Error",Objects.toString(ex.getMessage(),ex.toString()));Messages.showErrorDialog(project,Objects.toString(ex.getMessage(),ex.toString()),"Odoo Runner");}}); return b; }
        Workspace.Installation installation(){return (Workspace.Installation)installations.getSelectedItem();}
        Workspace.Runner runner(){return (Workspace.Runner)runners.getSelectedItem();}
        void refreshRunners(){if(optionTable.isEditing())optionTable.getCellEditor().cancelCellEditing();loading=true;runners.removeAllItems();if(installation()!=null)for(var r:installation().runners)runners.addItem(r);loading=false;load();}
        void load(){if(optionTable.isEditing())optionTable.getCellEditor().cancelCellEditing();editingRunner=runner();optionRows.setRowCount(0);addonPaths.load("");customAddonPaths.load("");deployment.load(new LinkedHashMap<>());secrets.values().forEach(f->f.setText(""));clearSecrets.setSelected(false);var r=runner(); if(r==null){fields.values().forEach(f->f.setText(""));extra.setText("");launch.values().forEach(f->f.setText(""));testEnable.setSelected(false);stopAfterInit.setSelected(false);savedSnapshot=snapshot();refreshRunnerSave();return;}
            String[] values={r.name,r.httpPort,r.eventPort,r.addons,r.database,r.dbHost,r.dbPort,r.dbUser,r.workers,r.dataDir};int n=0;for(var f:fields.values())f.setText(values[n++]);extra.setText(r.extra);eventKey.setSelectedItem(r.eventKey);
            addonPaths.load(r.addons);customAddonPaths.load(r.customAddons);var savedOptions=additional(r.extra);deployment.load(savedOptions);
            for(var entry:savedOptions.entrySet())optionRows.addRow(new Object[]{entry.getKey(),entry.getValue()});
            String[] args={r.launchDatabase,r.installModules,r.updateModules,r.testTags,r.testFile,r.devMode,r.withoutDemo};int arg=0;for(var field:launch.values())field.setText(args[arg++]);testEnable.setSelected(r.testEnable);stopAfterInit.setSelected(r.stopAfterInit);savedSnapshot=snapshot();refreshRunnerSave();
            status.message("Info",processStatus(r)+" · HTTP "+r.httpPort+" · Event "+r.eventPort);status.setToolTipText(directory().resolve("odoo.conf").toString());
        }
        String value(String key){return fields.get(key).getText().trim();}
        void save(){var r=runner();if(r==null)throw new IllegalArgumentException("Add a project and runner first.");
            if(active(r))throw new IllegalArgumentException("Stop this runner before editing its configuration.");
            if(!value("Data directory").isBlank()&&!Path.of(value("Data directory")).isAbsolute())throw new IllegalArgumentException("Data directory must be absolute, or blank for an isolated default.");
            if(port(value("HTTP port"))==port(value("Event port")))throw new IllegalArgumentException("HTTP and event ports must differ.");port(value("Database port"));int workers=Integer.parseInt(value("Workers"));if(workers<0)throw new IllegalArgumentException("Workers cannot be negative.");
            if(value("Runner name").isBlank())throw new IllegalArgumentException("Runner name is required.");
            // Validate both editors before changing saved state.
            readOptionTable();var advanced=additional(extra.getText());deployment.validateValues();for(var entry:deployment.values().entrySet()){if(advanced.containsKey(entry.getKey()))throw new IllegalArgumentException("Option is already configured in the deployment form: "+entry.getKey());advanced.put(entry.getKey(),entry.getValue());}
            StringBuilder combined=new StringBuilder();advanced.forEach((key,value)->combined.append(key).append(" = ").append(value).append('\n'));extra.setText(combined.toString());
            String standard=addonPaths.normalized(Path.of(installation().root)),custom=customAddonPaths.normalized(Path.of(installation().root));
            for(var field:fields.values())if(field.getText().contains("\n")||field.getText().contains("\r"))throw new IllegalArgumentException("Configuration values must be single-line.");
            for(var field:launch.values())if(field.getText().contains("\n")||field.getText().contains("\r"))throw new IllegalArgumentException("Launch values must be single-line.");
            var args=launch.values().stream().map(f->f.getText().trim()).toList();
            r.launchDatabase=args.get(0);r.installModules=args.get(1);r.updateModules=args.get(2);r.testTags=args.get(3);r.testFile=args.get(4);r.devMode=args.get(5);r.withoutDemo=args.get(6);r.testEnable=testEnable.isSelected();r.stopAfterInit=stopAfterInit.isSelected();
            r.name=value("Runner name");r.httpPort=value("HTTP port");r.eventPort=value("Event port");r.addons=standard;r.customAddons=custom;fields.get("Addons paths (comma separated)").setText(standard);r.database=value("Database name(s)");r.dbHost=value("Database host");r.dbPort=value("Database port");r.dbUser=value("Database user");r.workers=value("Workers");r.dataDir=value("Data directory");r.eventKey=Objects.toString(eventKey.getSelectedItem());r.extra=extra.getText();for(var entry:secrets.entrySet()){char[] password=entry.getValue().getPassword();try{if(clearSecrets.isSelected()||password.length>0)PasswordSafe.getInstance().setPassword(secretKey(r,entry.getKey()),clearSecrets.isSelected()?null:new String(password));}finally{Arrays.fill(password,'\0');entry.getValue().setText("");}}clearSecrets.setSelected(false);runners.repaint();savedSnapshot=snapshot();refreshRunnerSave();refreshProjects();status.message("Success","Runner configuration saved.");
        }
        void addProject(){if(!canClose())return;mainSectionCards.show(mainSection,"projects");modulePickerVisible=false;load();setupEditing=false;setupVisible=true;projectSetup.begin(null);projectCards.show(projectPages,"setup");}
        void editProject(){if(!canClose())return;modulePickerVisible=false;var i=installation();if(i==null)return;if(workspace.dependencies.busy(i))throw new IllegalArgumentException("Wait for dependency installation to finish or cancel it first.");if(i.runners.stream().anyMatch(this::active))throw new IllegalArgumentException("Stop this project's runners before editing its source or interpreter.");load();setupEditing=true;setupVisible=true;projectSetup.begin(i);projectCards.show(projectPages,"setup");}
        void saveProjectSetup(Workspace.Installation i){
            if(workspace.dependencies.busy(i))throw new IllegalArgumentException("Wait for dependency installation to finish or cancel it first.");if(i.runners.stream().anyMatch(this::active))throw new IllegalArgumentException("Stop this project's runners before changing setup.");
            if(!workspace.getState().installations.contains(i)){workspace.getState().installations.add(i);installations.addItem(i);}
            setupVisible=false;openProject(i);refreshRunners();updateProjectHeading();refreshProjects();
        }
        void cancelProjectSetup(){setupVisible=false;if(installation()!=null){updateProjectHeading();runnerList.showProject(installation());projectCards.show(projectPages,"runners");}else projectCards.show(projectPages,"empty");}
        JPanel pathPicker(JTextField field,boolean directory){JPanel panel=new JPanel(new BorderLayout(6,0));panel.add(field);panel.add(button("Browse…",()->{var chosen=PathBrowser.choose(project,field.getText(),directory,false);if(!chosen.isEmpty())field.setText(chosen.get(0).toString());}),BorderLayout.EAST);return panel;}
        boolean active(Workspace.Runner r){return r!=null&&workspace.busy(r);}
        void removeProject(){if(!canClose())return;var i=installation();if(i==null)return;if(workspace.dependencies.busy(i))throw new IllegalArgumentException("Wait for dependency installation to finish or cancel it first.");if(i.runners.stream().anyMatch(this::active))throw new IllegalArgumentException("Stop this project's runners first.");if(Messages.showYesNoDialog(project,"Remove saved project and runner definitions? Generated files remain on disk.","Remove Odoo project",Messages.getQuestionIcon())==Messages.YES){setupVisible=false;modulePickerVisible=false;workspace.getState().installations.remove(i);installations.removeItem(i);refreshProjects();if(installation()!=null)openProject(installation());else projectCards.show(projectPages,"empty");}}
        void removeRunner(){if(!canLeave())return;var r=runner();if(r==null)return;if(active(r))throw new IllegalArgumentException("Stop the runner first.");if(Messages.showYesNoDialog(project,"Remove runner definition? Its generated files remain on disk.","Remove runner",Messages.getQuestionIcon())==Messages.YES){installation().runners.remove(r);refreshRunners();refreshProjects();runnerList.showProject(installation());projectCards.show(projectPages,"runners");}}
        void addRunner(){
            if(creatingRunner)return;if(!canLeave())return;var selected=installation();if(selected==null)throw new IllegalArgumentException("Add a project first.");
            Set<Integer> used=new HashSet<>();for(var i:workspace.getState().installations)for(var r:i.runners){used.add(port(r.httpPort));used.add(port(r.eventPort));}
            int first=port(workspace.getState().defaultHttpPort);creatingRunner=true;status.message("Info","Finding an unused HTTP/event port pair…");
            new SwingWorker<int[],Void>(){
                protected int[] doInBackground(){for(int candidate=first;candidate<=65532;candidate++){if(used.contains(candidate)||used.contains(candidate+3))continue;try(java.net.ServerSocket http=new java.net.ServerSocket();java.net.ServerSocket event=new java.net.ServerSocket()){http.setReuseAddress(false);event.setReuseAddress(false);http.bind(new java.net.InetSocketAddress("127.0.0.1",candidate));event.bind(new java.net.InetSocketAddress("127.0.0.1",candidate+3));return new int[]{candidate,candidate+3};}catch(java.io.IOException ignored){}}throw new IllegalArgumentException("No available port pair found above "+first);}
                protected void done(){creatingRunner=false;if(project.isDisposed()||installation()!=selected||!workspace.getState().installations.contains(selected))return;try{int[] ports=get();if(setupVisible||modulePickerVisible||!canLeave())return;for(var i:workspace.getState().installations)for(var old:i.runners)if(old.httpPort.equals(""+ports[0])||old.httpPort.equals(""+ports[1])||old.eventPort.equals(""+ports[0])||old.eventPort.equals(""+ports[1]))throw new IllegalArgumentException("Port assignments changed during discovery. Choose Add runner again.");
                    var r=new Workspace.Runner();r.httpPort=""+ports[0];r.eventPort=""+ports[1];r.name="Runner "+(selected.runners.size()+1);r.addons=String.join(",",List.of(Path.of(selected.root,"odoo","addons"),Path.of(selected.root,"addons")).stream().filter(Files::isDirectory).map(Path::toString).toList());r.dbHost=workspace.getState().defaultDbHost;r.dbUser=workspace.getState().defaultDbUser;r.eventKey=workspace.getState().defaultEventKey;selected.runners.add(r);loading=true;runners.addItem(r);runners.setSelectedItem(r);loading=false;load();refreshProjects();runnerList.showProject(selected);openRunnerSettings(r);
                }catch(Exception ex){status.message("Error",Objects.toString(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage(),"Cannot create runner"));}}
            }.execute();
        }
        static int port(String value){int p=Integer.parseInt(value);if(p<1||p>65535)throw new IllegalArgumentException("Ports must be between 1 and 65535.");return p;}
        static Map<String,String> additional(String text){Map<String,String> out=new LinkedHashMap<>();Set<String> managed=Set.of("http_port","xmlrpc_port","gevent_port","longpolling_port","addons_path","db_name","db_host","db_port","db_user","workers","data_dir");for(String line:text.split("\\R")){String s=line.trim();if(s.isEmpty()||s.startsWith("#")||s.startsWith(";"))continue;int eq=s.indexOf('=');if(eq<1)throw new IllegalArgumentException("Expected key = value: "+s);String key=s.substring(0,eq).trim().toLowerCase(Locale.ROOT);if(!key.matches("[a-z][a-z0-9_]*")||managed.contains(key)||out.containsKey(key))throw new IllegalArgumentException("Invalid, duplicate or UI-managed option: "+key);if(Set.of("db_password","admin_passwd","smtp_password").contains(key))throw new IllegalArgumentException("Use the password fields for secrets so they stay out of project XML.");out.put(key,s.substring(eq+1).trim());}return out;}
        Path directory(){return RunnerEngine.directory(project,Objects.requireNonNull(runner(),"Select a runner first."));}
        CredentialAttributes secretKey(Workspace.Runner r,String key){return RunnerEngine.secretKey(r,key);}
        String config(boolean includeSecrets){return RunnerEngine.config(project,runner(),includeSecrets);}
        void preview(){if(!active(runner()))save();JTextArea area=new JTextArea(config(false),22,65);area.setEditable(false);JOptionPane.showMessageDialog(this,new JScrollPane(area),"Generated odoo.conf",JOptionPane.PLAIN_MESSAGE);}
        void run(){if(active(runner()))throw new IllegalArgumentException("This runner is already running or restarting.");save();workspace.start(installation(),runner());refreshProjects();runnerList.refresh();status.message("Success","Process started for "+runner().name+". Check Log for startup progress.");}
        GeneralCommandLine command(Path conf){return RunnerEngine.command(installation(),runner(),conf);}
        void previewCommand(){if(!active(runner()))save();showText("odoo-bin command",command(directory().resolve("odoo.conf")).getCommandLineString());}
        void showText(String title,String text){JTextArea area=new JTextArea(text,14,75);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);JOptionPane.showMessageDialog(this,new JScrollPane(area),title,JOptionPane.PLAIN_MESSAGE);}
        void stop(){var r=runner();if(r!=null){workspace.stop(r);runnerList.refresh();status.message("Warning","Stop requested for "+r.name);}}
        void browse(){var r=runner();if(r!=null)BrowserUtil.browse("http://127.0.0.1:"+port(r.httpPort));}
    }
}
