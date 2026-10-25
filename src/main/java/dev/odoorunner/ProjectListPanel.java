package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/** Real project-row icon controls; refreshes labels without replacing focused buttons. */
final class ProjectListPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final ScrollForm rows=new ScrollForm();
    private final JTextField search=new RoundTextField();
    private final List<ProjectRow> entries=new ArrayList<>();
    ProjectListPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(12,16));this.owner=owner;setBorder(BorderFactory.createEmptyBorder(8,4,8,12));
        JPanel header=new JPanel(new BorderLayout(8,18));

        JPanel tools=new JPanel(new BorderLayout(6,12));JButton add=owner.button("＋ Add project",owner::addProject);add.setText("Add project");add.setIcon(UiIcons.PLUS);Design.primary(add);tools.add(Design.label("PROJECTS",12,true),BorderLayout.WEST);tools.add(add,BorderLayout.EAST);
        JPanel listing=new JPanel(new BorderLayout(8,12));listing.add(tools,BorderLayout.NORTH);search.setToolTipText("Search project names and source paths");search.putClientProperty("JTextField.placeholderText","Search projects…");listing.add(search);header.add(listing);add(header,BorderLayout.NORTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){filter();}public void removeUpdate(javax.swing.event.DocumentEvent e){filter();}public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}});
        add(new HiddenScrollPane(rows));setMinimumSize(new Dimension(250,120));
    }
    private void filter(){String query=search.getText().trim().toLowerCase(Locale.ROOT);for(int n=0;n<entries.size();n++){var row=entries.get(n);boolean visible=query.isBlank()||row.installation.name.toLowerCase(Locale.ROOT).contains(query)||row.installation.root.toLowerCase(Locale.ROOT).contains(query);row.setVisible(visible);if(n*2+1<rows.getComponentCount())rows.getComponent(n*2+1).setVisible(visible);}rows.revalidate();rows.repaint();}
    void select(Workspace.Installation selected){for(var row:entries)row.refresh();}
    void refresh(){
        var definitions=owner.workspace.getState().installations;boolean changed=entries.size()!=definitions.size();if(!changed)for(int n=0;n<entries.size();n++)if(entries.get(n).installation!=definitions.get(n))changed=true;
        if(changed){rows.removeAll();entries.clear();for(var i:definitions){var row=new ProjectRow(i);entries.add(row);rows.section(row);}rows.revalidate();rows.repaint();filter();}for(var row:entries)row.refresh();
    }
    private final class ProjectRow extends Design.Card {
        String lastState="";
        final Workspace.Installation installation;final JLabel name=new JLabel(),info=new JLabel(),ports=new JLabel();
        ProjectRow(Workspace.Installation i){
            super(new BorderLayout(10,6),14);installation=i;setBorder(BorderFactory.createEmptyBorder(10,8,10,8));
            JPanel labels=new JPanel(new GridLayout(0,1,2,2));labels.setOpaque(false);name.setFont(name.getFont().deriveFont(Font.BOLD));labels.add(name);labels.add(info);labels.add(ports);add(labels);add(Design.badge(UiIcons.CUBE),BorderLayout.WEST);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,1,0));actions.setOpaque(false);
            actions.add(UiIcons.button(owner,UiIcons.EDIT,"Edit project",()->{if(owner.switchProject(i))owner.editProject();}));
            actions.add(UiIcons.button(owner,UiIcons.PIP,"Install pip dependencies into selected virtualenv",()->{if(owner.switchProject(i))owner.openDependencies(i);}));
            actions.add(UiIcons.button(owner,UiIcons.DELETE,"Delete project definition",()->{if(owner.switchProject(i))owner.removeProject();}));add(actions,BorderLayout.SOUTH);ModernUi.apply(this);
            UiIcons.clickable(this,()->owner.switchProject(i));setToolTipText(i.root);
        }
        public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        void refresh(){var active=installation.runners.stream().filter(owner.workspace::active).toList();String fingerprint=installation.name+installation.runners.size()+active.stream().map(r->r.httpPort+"/"+r.eventPort).collect(Collectors.joining(","))+(owner.installation()==installation);if(fingerprint.equals(lastState))return;lastState=fingerprint;name.setText(installation.name);info.setText(installation.runners.size()+" runners · "+active.size()+" active");String text=active.stream().map(r->r.httpPort+" / "+r.eventPort).collect(Collectors.joining(", "));ports.setText(text.isBlank()?"No active ports":text);ports.setToolTipText(text);
            boolean selected=owner.installation()==installation;outline=selected?Design.BLUE:Design.BORDER;name.setForeground(Design.BLUE);info.setForeground(Design.MUTED);ports.setForeground(Design.MUTED);repaint();
        }
    }
}
