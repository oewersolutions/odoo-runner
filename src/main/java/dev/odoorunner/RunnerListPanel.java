package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Real per-row controls, including keyboard-focusable buttons, rather than table renderers. */
final class RunnerListPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final JLabel title=Design.label("",19,true),total=Design.label("0",16,true),activeCount=Design.label("0",16,true);
    private final JPanel rows=new ScrollableRows();
    private final List<RunnerRow> entries=new ArrayList<>();
    private Workspace.Installation installation;
    RunnerListPanel(RunnerWindow.Panel owner) {
        super(new BorderLayout(8,8));this.owner=owner;
        setBorder(BorderFactory.createEmptyBorder(6,8,6,0));title.setForeground(Design.BLUE);
        Design.Card summary=new Design.Card(new BorderLayout(10,6),16);summary.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        JPanel identity=new JPanel(new BorderLayout(14,4));identity.setOpaque(false);identity.add(Design.badge(UiIcons.CUBE),BorderLayout.WEST);JPanel heading=new JPanel(new GridLayout(0,1,0,3));heading.setOpaque(false);heading.add(title);heading.add(Design.label("Manage runners for this project",13,false));identity.add(heading);summary.add(identity);
        JPanel stats=new JPanel(new GridLayout(1,2,10,0));stats.setOpaque(false);stats.add(stat("Total runners",total));stats.add(stat("Active runners",activeCount));summary.add(stats,BorderLayout.EAST);add(summary,BorderLayout.NORTH);
        Design.Card body=new Design.Card(new BorderLayout(10,16),16);body.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));JPanel header=new JPanel(new BorderLayout(12,4));header.setOpaque(false);JPanel labels=new JPanel(new GridLayout(0,1,0,4));labels.setOpaque(false);labels.add(Design.label("Runners",17,true));labels.add(Design.label("Manage Odoo instances for the selected project",13,false));header.add(labels);JButton add=owner.button("＋ Add runner",owner::addRunner);add.setText("Add runner");add.setIcon(UiIcons.PLUS);Design.primary(add);header.add(owner.row(add),BorderLayout.EAST);body.add(header,BorderLayout.NORTH);
        rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));body.add(new HiddenScrollPane(rows));
        Design.Card help=new Design.Card(new BorderLayout(),12);help.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));JPanel notes=new JPanel(new GridLayout(0,1,0,5));notes.setOpaque(false);notes.add(Design.label("Click a runner to edit its configuration.",13,true));notes.add(Design.label("Restart starts inactive runners or restarts active ones.",12,false));help.add(notes);body.add(help,BorderLayout.SOUTH);add(body);
    }
    private JComponent stat(String label,JLabel count){Design.Card card=new Design.Card(new BorderLayout(8,5),12);card.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));card.add(Design.label(label,12,false),BorderLayout.NORTH);card.add(count);card.setPreferredSize(new Dimension(112,50));return card;}
    void showProject(Workspace.Installation selected) { installation=selected; refresh(); }
    void refresh() {
        if(installation==null)return;
        title.setText(installation.name);total.setText(""+installation.runners.size());activeCount.setText(""+installation.runners.stream().filter(owner.workspace::active).count());
        boolean changed=rows.getComponentCount()==0||entries.size()!=installation.runners.size();
        if(!changed)for(int n=0;n<entries.size();n++)if(entries.get(n).runner!=installation.runners.get(n))changed=true;
        if(changed){
            rows.removeAll();entries.clear();
            if(installation.runners.isEmpty())rows.add(owner.row(new JLabel("No runners yet. Choose Add runner to configure the first instance.")));
            for(var runner:installation.runners){RunnerRow row=new RunnerRow(runner);entries.add(row);rows.add(row);rows.add(Box.createVerticalStrut(12));ModernUi.apply(row);}
            rows.add(Box.createVerticalGlue());rows.revalidate();rows.repaint();
        }
        for(var row:entries)row.refresh();
    }
    private static final class ScrollableRows extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(640,500);}
        public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 24;}
        public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(24,visible.height-24);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private final class RunnerRow extends Design.Card {
        String lastState="";
        final Workspace.Runner runner;
        final JLabel name=new JLabel(), details=new JLabel(), state=new JLabel();
        final JButton stop,restart,log,shell,database;
        RunnerRow(Workspace.Runner runner){
            super(new BorderLayout(16,8),14);this.runner=runner;setBorder(BorderFactory.createEmptyBorder(10,12,10,12));add(Design.badge(UiIcons.SERVER),BorderLayout.WEST);
            JPanel labels=new JPanel(new BorderLayout(0,8));labels.setOpaque(false);name.setFont(name.getFont().deriveFont(Font.BOLD,16f));JPanel nameRow=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));nameRow.setOpaque(false);nameRow.add(name);nameRow.add(state);labels.add(nameRow,BorderLayout.NORTH);labels.add(details);add(labels);
            stop=UiIcons.button(owner,UiIcons.STOP,"Stop runner",()->{owner.workspace.stop(runner);refresh();});
            restart=UiIcons.button(owner,UiIcons.RESTART,"Start / restart runner with saved settings",()->{owner.workspace.rerun(installation,runner,false);refresh();});
            log=UiIcons.button(owner,UiIcons.LOG,"Runner log",()->owner.openDiagnostics(runner,false));
            shell=UiIcons.button(owner,UiIcons.TERMINAL,"Open Odoo Shell",()->OdooShell.open(owner,installation,runner));
            database=UiIcons.button(owner,UiIcons.DATABASE,"Browse database tables and records",()->owner.databaseBrowser.begin(installation,runner));
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));actions.setOpaque(false);actions.add(stop);actions.add(restart);actions.add(log);actions.add(shell);actions.add(database);add(actions,BorderLayout.EAST);setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
            UiIcons.clickable(this,()->owner.openRunnerSettings(runner));
        }
        void refresh(){
            String fingerprint=runner.name+runner.httpPort+runner.eventPort+runner.launchDatabase+runner.database+owner.workspace.processStatus(runner);if(fingerprint.equals(lastState))return;lastState=fingerprint;
            name.setForeground(Design.BLUE);name.setText(runner.name);details.setText("HTTP "+runner.httpPort+" · Event "+runner.eventPort+" · Database: "+(runner.launchDatabase.isBlank()?runner.database:runner.launchDatabase));
            String text=owner.workspace.processStatus(runner);boolean activeNow=owner.workspace.active(runner),changing=owner.workspace.pendingRestarts.containsKey(runner.id)||text.equals("Stopping…");
            state.setText(changing?text:activeNow?"Active":"Inactive");state.setToolTipText(text);Color statusColor=changing?new Color(210,150,35):activeNow?new Color(55,170,95):new Color(145,145,150);state.setIcon(UiIcons.dot(statusColor));state.setForeground(statusColor);
            boolean pending=owner.workspace.pendingRestarts.containsKey(runner.id),active=owner.workspace.active(runner);
            stop.setEnabled(active||pending);restart.setEnabled(!pending);restart.setIcon(active?UiIcons.RESTART:UiIcons.PLAY);restart.setToolTipText(active?"Restart runner":"Start runner");
            var capture=owner.workspace.logs.get(runner.id);int count=capture==null?0:capture.issueCount();log.setToolTipText("Log, warnings and errors ("+count+" issue lines)");
        }
    }
}
