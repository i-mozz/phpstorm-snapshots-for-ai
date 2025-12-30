package com.gbti.snapshotsforai.actions;

import com.gbti.snapshotsforai.ui.SnapshotHistoryDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * Action to view and manage snapshot history.
 */
public class ViewSnapshotHistoryAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        SnapshotHistoryDialog dialog = new SnapshotHistoryDialog(project);
        dialog.show();
    }
}
