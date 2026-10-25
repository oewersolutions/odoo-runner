package dev.odoorunner;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public final class OpenRunnerAction extends DumbAwareAction {
    private static final Key<RunnerDialog> DIALOG=Key.create("dev.odoorunner.popup");
    @Override public void actionPerformed(@NotNull AnActionEvent event){
        Project project=event.getProject();if(project==null||project.isDisposed())return;
        RunnerDialog dialog=project.getUserData(DIALOG);
        if(dialog!=null&&!dialog.isDisposed()){dialog.toFront();return;}
        dialog=new RunnerDialog(project);project.putUserData(DIALOG,dialog);dialog.show();
    }
    @Override public void update(@NotNull AnActionEvent event){event.getPresentation().setEnabledAndVisible(event.getProject()!=null);}
    @Override public @NotNull ActionUpdateThread getActionUpdateThread(){return ActionUpdateThread.BGT;}
}
