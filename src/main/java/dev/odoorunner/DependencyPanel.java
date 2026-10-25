package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;

/** Requirements and named package workflows share the same selected-venv guard. */
final class DependencyPanel extends JPanel {
    private final RunnerWindow.Panel owner;private Workspace.Installation installation;
    private final JTextField requirements=new RoundTextField(),packageName=new RoundTextField();
    private final JTextArea target=new JTextArea(),details=new JTextArea(),packages=new JTextArea(3,40),output=new JTextArea(8,60);
    private final JLabel status=new JLabel();private final JButton install,cancel,installPackages,search,addPackage;
    private long revision=-1;private PyPiLookup.Result found;private int lookupRevision;
    DependencyPanel(RunnerWindow.Panel owner){
        super(new BorderLayout(12,12));this.owner=owner;setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel header=new JPanel(new BorderLayout(12,0));header.add(owner.button("← Runners",owner::closeDependencies),BorderLayout.WEST);header.add(Design.label("Python dependencies",18,true));add(header,BorderLayout.NORTH);
        ScrollForm form=new ScrollForm();JPanel environment=ScrollForm.group("Selected virtual environment");target.setEditable(false);target.setOpaque(false);target.setLineWrap(true);target.setWrapStyleWord(true);ScrollForm.field(environment,"Python executable",target);form.section(environment);
        JPanel file=ScrollForm.group("Install from requirements");ScrollForm.field(file,"Requirements file",owner.pathPicker(requirements,false));install=owner.button("Install requirements",()->{owner.workspace.dependencies.install(installation,Path.of(requirements.getText().trim()));refresh();});ScrollForm.field(file,"",install);form.section(file);
        JPanel named=ScrollForm.group("Find and install packages");JPanel find=new JPanel(new BorderLayout(6,0));find.add(packageName);search=owner.button("Find on PyPI",this::lookup);find.add(search,BorderLayout.EAST);ScrollForm.field(named,"Exact package name",find);
        details.setEditable(false);details.setOpaque(false);details.setLineWrap(true);details.setWrapStyleWord(true);details.setText("Find an exact package name on PyPI, or enter package/version specifications below.");ScrollForm.field(named,"Package details",details);
        addPackage=owner.button("Add to install list",()->{if(found!=null)packages.append((packages.getText().isBlank()?"":"\n")+found.name()+"=="+found.version());});addPackage.setEnabled(false);ScrollForm.field(named,"",addPackage);
        packages.setLineWrap(true);packages.setWrapStyleWord(true);ScrollForm.field(named,"Packages / versions",new HiddenScrollPane(packages));installPackages=owner.button("Install packages",()->{owner.workspace.dependencies.installPackages(installation,packages.getText());refresh();});ScrollForm.field(named,"",installPackages);form.section(named);
        JPanel activity=ScrollForm.group("Installation output");output.setEditable(false);output.setLineWrap(true);output.setWrapStyleWord(true);HiddenScrollPane logs=new HiddenScrollPane(output);logs.setPreferredSize(new Dimension(400,200));ScrollForm.field(activity,"",logs);form.section(activity);add(new HiddenScrollPane(form));
        cancel=owner.button("Cancel installation",()->{if(installation!=null)owner.workspace.dependencies.cancel(installation);refresh();});JPanel footer=new JPanel(new BorderLayout(8,0));footer.add(status);footer.add(cancel,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
    }
    private void lookup(){String name=packageName.getText().trim();int token=++lookupRevision;found=null;search.setEnabled(false);addPackage.setEnabled(false);details.setText("Looking up "+name+" on PyPI…");new SwingWorker<PyPiLookup.Result,Void>(){protected PyPiLookup.Result doInBackground()throws Exception{return PyPiLookup.find(name);}protected void done(){if(token!=lookupRevision)return;search.setEnabled(true);try{found=get();details.setText(found.name()+" · latest "+found.version()+"\n"+found.summary()+"\nPython: "+found.python());addPackage.setEnabled(true);}catch(Exception ex){details.setText("Lookup failed: "+(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()));}}}.execute();}
    void begin(Workspace.Installation i){installation=i;lookupRevision++;found=null;addPackage.setEnabled(false);search.setEnabled(true);revision=-1;target.setText(i.python);requirements.setText(Path.of(i.root,"requirements.txt").toString());packageName.setText("");packages.setText("");details.setText("Find an exact PyPI package name, such as requests. Add it, then install below.");refresh();}
    void refresh(){if(installation==null)return;boolean busy=owner.workspace.dependencies.busy(installation);install.setEnabled(!busy);installPackages.setEnabled(!busy);requirements.setEnabled(!busy);packages.setEditable(!busy);cancel.setEnabled(busy);String message=owner.workspace.dependencies.status(installation);if(!message.equals(status.getText()))status.setText(message);RunnerLogs log=owner.workspace.dependencies.logs(installation);if(log.revision()!=revision){var snapshot=log.snapshot();revision=snapshot.revision();output.setText(snapshot.output());output.setCaretPosition(output.getDocument().getLength());}}
}
