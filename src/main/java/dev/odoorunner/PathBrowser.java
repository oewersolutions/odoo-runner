package dev.odoorunner;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Explicit navigation/search/path entry independent of OS chooser capabilities. */
final class PathBrowser extends DialogWrapper {
    private final JPanel content=new JPanel(new BorderLayout(8,8));
    private final JTextField address=new RoundTextField(),search=new RoundTextField();
    private final DefaultListModel<Path> model=new DefaultListModel<>();
    private final JList<Path> files=new JList<>(model);
    private final JLabel status=new JLabel(" ");
    private final List<Path> history=new ArrayList<>();
    private final List<Path> entries=new ArrayList<>();
    private final JButton back=new RoundButton(UiIcons.BACK),forward=new RoundButton(UiIcons.FORWARD);
    private final boolean directories;
    private List<Path> result=List.of();
    private Path current;
    private int historyIndex=-1,revision;
    private SwingWorker<List<Path>,Void> scan;
    private boolean closed;
    static List<Path> choose(Project project,String initial,boolean directories,boolean multiple){PathBrowser browser=new PathBrowser(project,initial,directories,multiple);browser.show();return browser.result;}
    private PathBrowser(Project project,String initial,boolean directories,boolean multiple){
        super(project,true);this.directories=directories;setTitle(directories?"Choose folders":"Choose a file");setOKButtonText("Choose");
        content.setPreferredSize(new Dimension(860,560));
        JPanel navigation=new JPanel(new BorderLayout(6,6)),buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,3,0));
        back.setToolTipText("Back");forward.setToolTipText("Forward");back.getAccessibleContext().setAccessibleName("Back");forward.getAccessibleContext().setAccessibleName("Forward");
        back.addActionListener(e->history(-1));forward.addActionListener(e->history(1));buttons.add(back);buttons.add(forward);
        JButton up=new RoundButton("Up");up.addActionListener(e->{if(current!=null&&current.getParent()!=null)navigate(current.getParent(),true);});buttons.add(up);
        JButton home=new RoundButton("Home");home.addActionListener(e->navigate(Path.of(System.getProperty("user.home")),true));buttons.add(home);
        JPanel pathRow=new JPanel(new BorderLayout(4,0));pathRow.add(address);JButton go=new RoundButton("Go");go.addActionListener(e->go());address.addActionListener(e->go());pathRow.add(go,BorderLayout.EAST);
        navigation.add(buttons,BorderLayout.WEST);navigation.add(pathRow);search.setToolTipText("Search names/paths in the current folder; paste a path above and press Enter to navigate.");
        JPanel top=new JPanel(new BorderLayout(6,6));top.add(navigation,BorderLayout.NORTH);JPanel searchRow=new JPanel(new BorderLayout(6,0));searchRow.add(new JLabel("Search this folder"),BorderLayout.WEST);searchRow.add(search);top.add(searchRow);content.add(top,BorderLayout.NORTH);
        files.setSelectionMode(multiple?ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:ListSelectionModel.SINGLE_SELECTION);files.setFixedCellHeight(28);
        files.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);if(value instanceof Path path){setText((Files.isDirectory(path)?"▸  ":"    ")+path.getFileName());setToolTipText(path.toString());}return this;}});
        files.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2){Path selected=files.getSelectedValue();if(selected!=null){if(Files.isDirectory(selected))navigate(selected,true);else if(!directories)doOKAction();}}}});
        files.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"enterFolder");files.getActionMap().put("enterFolder",new AbstractAction(){public void actionPerformed(ActionEvent e){Path selected=files.getSelectedValue();if(selected!=null&&Files.isDirectory(selected))navigate(selected,true);else doOKAction();}});
        content.add(new HiddenScrollPane(files));content.add(status,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});
        ModernUi.apply(content);init();ModernUi.sizeButton(getButton(getOKAction()));ModernUi.sizeButton(getButton(getCancelAction()));setResizable(true);
        Path start=Path.of(System.getProperty("user.home"));try{if(initial!=null&&!initial.isBlank()){Path path=expand(initial.trim());if(Files.isDirectory(path))start=path;else if(path.getParent()!=null&&Files.isDirectory(path.getParent()))start=path.getParent();}}catch(InvalidPathException ignored){}
        navigate(start,true);
    }
    private Path expand(String text){String home=System.getProperty("user.home");if(text.equals("~"))text=home;else if(text.startsWith("~/")||text.startsWith("~\\"))text=home+text.substring(1);Path path=Path.of(text);return (path.isAbsolute()?path:(current==null?Path.of(home):current).resolve(path)).normalize();}
    private void go(){try{Path path=expand(address.getText().trim());if(Files.isDirectory(path))navigate(path,true);else if(!directories&&Files.isRegularFile(path)){navigate(path.getParent(),true);address.setText(path.toString());}else status.setText("That path is not an existing "+(directories?"folder.":"file or folder."));}catch(Exception ex){status.setText("Invalid path: "+ex.getMessage());}}
    private void history(int direction){int next=historyIndex+direction;if(next<0||next>=history.size())return;if(!Files.isDirectory(history.get(next))){status.setText("History folder is unavailable.");return;}historyIndex=next;navigate(history.get(next),false);}
    private void navigate(Path path,boolean record){
        if(!Files.isDirectory(path)){status.setText("Folder is unavailable: "+path);return;}current=path.toAbsolutePath().normalize();
        if(record){while(history.size()>historyIndex+1)history.remove(history.size()-1);if(historyIndex<0||!history.get(historyIndex).equals(current)){history.add(current);historyIndex++;}}
        back.setEnabled(historyIndex>0);forward.setEnabled(historyIndex+1<history.size());address.setText(current.toString());search.setText("");entries.clear();model.clear();setOKActionEnabled(false);status.setText("Loading…");int token=++revision;Path folder=current;if(scan!=null)scan.cancel(true);
        scan=new SwingWorker<>(){protected List<Path> doInBackground()throws Exception{try(var stream=Files.list(folder)){return stream.filter(p->!directories||Files.isDirectory(p)).limit(20001).sorted(Comparator.<Path,Boolean>comparing(p->!Files.isDirectory(p)).thenComparing(p->p.getFileName().toString(),String.CASE_INSENSITIVE_ORDER)).toList();}}
            protected void done(){if(closed||token!=revision||isCancelled())return;try{entries.addAll(get());boolean limited=entries.size()>20000;if(limited)entries.remove(entries.size()-1);filter();status.setText(limited?"First 20,000 entries shown; narrow the path to search a smaller folder.":entries.size()+" entries. Search filters this folder; path field navigates directly.");setOKActionEnabled(true);}catch(Exception ex){status.setText("Cannot read folder: "+(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()));}}
        };scan.execute();
    }
    private void filter(){String query=search.getText().trim().toLowerCase(Locale.ROOT);model.clear();for(Path path:entries)if(query.isEmpty()||path.getFileName().toString().toLowerCase(Locale.ROOT).contains(query)||path.toString().toLowerCase(Locale.ROOT).contains(query))model.addElement(path);}
    @Override protected void doOKAction(){
        if(!isOKActionEnabled())return;List<Path> chosen=files.getSelectedValuesList();
        try{String typed=address.getText().trim();Path typedPath=typed.isBlank()?current:expand(typed);if(typedPath!=null&&!typedPath.equals(current)){if((directories&&Files.isDirectory(typedPath))||(!directories&&Files.isRegularFile(typedPath)))chosen=List.of(typedPath);else{status.setText("The typed path is not a selectable "+(directories?"folder.":"file."));return;}}
            else if(chosen.isEmpty()&&directories&&current!=null)chosen=List.of(current);
            if(chosen.isEmpty()){status.setText("Select a "+(directories?"folder":"file")+" first.");return;}
            for(Path path:chosen)if(directories?!Files.isDirectory(path):!Files.isRegularFile(path)){status.setText("Select "+(directories?"folders":"files")+" only.");return;}
            result=List.copyOf(chosen);super.doOKAction();
        }catch(Exception ex){status.setText("Invalid selection: "+ex.getMessage());}
    }
    @Override protected JComponent createCenterPanel(){return content;}
    @Override protected void dispose(){closed=true;revision++;if(scan!=null)scan.cancel(true);super.dispose();}
}
