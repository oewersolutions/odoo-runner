package dev.odoorunner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.ShellStartupOptions;
import javax.swing.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

/** Real PTY sessions owned by the popup, retained while navigating between cards. */
final class OdooShellPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final JLabel title=Design.label("Odoo Shell",17,true);
    private final JLabel details=new JLabel();
    private final CardLayout cards=new VisibleCardLayout();
    private final JPanel sessions=new JPanel(cards);
    private final Map<Workspace.Runner,Session> terminals=new IdentityHashMap<>();
    private Workspace.Runner currentRunner;
    private long nextSession;
    private record Session(String card,String description,ShellTerminalWidget terminal,Disposable lifetime) {}

    OdooShellPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(0,12));this.owner=owner;
        JPanel heading=new JPanel(new BorderLayout(12,8));
        heading.add(owner.button("← Runners",this::backToRunners),BorderLayout.WEST);
        title.setForeground(Design.BLUE);heading.add(title,BorderLayout.CENTER);
        JPanel labels=new JPanel(new BorderLayout(0,6));labels.add(heading,BorderLayout.NORTH);
        labels.add(details,BorderLayout.CENTER);
        labels.add(new JLabel("Interactive database session · Ctrl+D exits Odoo Shell · Back or closing this popup ends the shell session."),BorderLayout.SOUTH);
        add(labels,BorderLayout.NORTH);add(sessions,BorderLayout.CENTER);
    }

    boolean resume(Workspace.Runner runner){
        Session session=terminals.get(runner);
        if(session==null)return false;
        show(runner,session);return true;
    }

    void open(Workspace.Installation installation,Workspace.Runner runner,String database,String command) throws Exception {
        Disposable lifetime=Disposer.newDisposable("Odoo popup shell: "+runner.name);
        try{
            var terminalRunner=LocalTerminalDirectRunner.createTerminalRunner(owner.project);
            var widget=terminalRunner.startShellTerminalWidget(lifetime,new ShellStartupOptions.Builder().workingDirectory(installation.root).build(),false);
            ShellTerminalWidget terminal=ShellTerminalWidget.asShellJediTermWidget(widget);
            if(terminal==null)throw new IllegalStateException("The bundled terminal cannot host this interactive shell.");
            String card="shell-"+nextSession++;
            Session session=new Session(card,installation.name+" · Database: "+database,terminal,lifetime);
            sessions.add(terminal.getComponent(),card);
            terminals.put(runner,session);
            show(runner,session);
            terminal.executeCommand(command);
        }catch(Exception ex){
            Session removed=terminals.remove(runner);
            if(removed!=null)sessions.remove(removed.terminal().getComponent());
            Disposer.dispose(lifetime);throw ex;
        }
    }

    private void show(Workspace.Runner runner,Session session){
        currentRunner=runner;
        title.setText(runner.name+" · Odoo Shell");details.setText(session.description());
        cards.show(sessions,session.card());owner.projectCards.show(owner.projectPages,"shell");
        SwingUtilities.invokeLater(()->session.terminal().getComponent().requestFocusInWindow());
    }

    private void backToRunners(){
        Session session=terminals.remove(currentRunner);
        currentRunner=null;
        if(session!=null){
            Disposer.dispose(session.lifetime());
            sessions.remove(session.terminal().getComponent());
        }
        owner.closeDiagnostics();
    }

    void dispose(){
        for(Session session:terminals.values())Disposer.dispose(session.lifetime());
        terminals.clear();currentRunner=null;sessions.removeAll();
    }
}
