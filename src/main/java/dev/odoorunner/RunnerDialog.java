package dev.odoorunner;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.*;

final class RunnerDialog extends DialogWrapper {
    private final RunnerWindow.Panel panel;
    RunnerDialog(Project project){
        super(project,false);panel=new RunnerWindow.Panel(project);
        setTitle("Odoo Runner");setModal(false);init();setResizable(true);
    }
    @Override protected JComponent createCenterPanel(){return panel;}
    @Override protected Action[] createActions(){return new Action[0];}
    @Override protected JComponent createSouthPanel(){return null;}
    @Override protected String getDimensionServiceKey(){return "dev.odoorunner.popup.large.dimensions";}
    @Override public void doOKAction(){if(panel.canClose())super.doOKAction();}
    @Override public void doCancelAction(){if(panel.canClose())super.doCancelAction();}
    @Override protected void dispose(){panel.dispose();super.dispose();}
}
