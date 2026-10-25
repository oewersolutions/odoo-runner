package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Ordered, multiple addon roots. Standard and custom roots share Odoo's addons_path. */
final class AddonPathsPanel extends JPanel {
    private final DefaultListModel<String> paths=new DefaultListModel<>();
    private final JList<String> list=new JList<>(paths);
    AddonPathsPanel(RunnerWindow.Panel owner,String title){
        super(new BorderLayout(4,4));setBorder(BorderFactory.createTitledBorder(title));
        list.setVisibleRowCount(4);list.setFixedCellHeight(28);
        list.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> entries,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(entries,value,index,selected,focus);setToolTipText(Objects.toString(value,""));setBorder(BorderFactory.createEmptyBorder(4,8,4,8));return this;}});
        HiddenScrollPane folders=new HiddenScrollPane(list);folders.setPreferredSize(new Dimension(160,120));folders.setMinimumSize(new Dimension(100,112));add(folders);
        add(owner.row(owner.button("Add folders…",()->{
            for(Path folder:PathBrowser.choose(owner.project,owner.installation()==null?"":owner.installation().root,true,true))addPath(folder);
        }),owner.button("Remove",()->{int[] indices=list.getSelectedIndices();for(int n=indices.length-1;n>=0;n--)paths.remove(indices[n]);}),small(owner,"↑",()->move(-1)),small(owner,"↓",()->move(1))),BorderLayout.SOUTH);
    }
    private JButton small(RunnerWindow.Panel owner,String text,Runnable action){JButton button=owner.button(text,action);button.putClientProperty("odoo.compact",true);ModernUi.sizeButton(button);return button;}
    private void addPath(Path path){String value=path.toAbsolutePath().normalize().toString();if(value.contains(",")||value.contains("\n")||value.contains("\r"))throw new IllegalArgumentException("Odoo addon paths cannot contain commas or line breaks.");if(!paths.contains(value))paths.addElement(value);}
    private void move(int delta){int index=list.getSelectedIndex(),next=index+delta;if(index<0||next<0||next>=paths.size())return;String value=paths.remove(index);paths.add(next,value);list.setSelectedIndex(next);}
    void load(String text){paths.clear();for(String value:text.split(","))if(!value.isBlank())paths.addElement(value.trim());}
    String value(){return String.join(",",Collections.list(paths.elements()));}
    List<Path> validated(Path source){List<Path> result=new ArrayList<>();for(String value:Collections.list(paths.elements())){Path path=source.resolve(value).normalize().toAbsolutePath();if(!Files.isDirectory(path))throw new IllegalArgumentException("Addon folder does not exist: "+path);result.add(path);}return result;}
    String normalized(Path source){return String.join(",",validated(source).stream().map(Path::toString).toList());}
}
