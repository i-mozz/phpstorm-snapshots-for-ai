package com.gbti.snapshotsforai.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dialog for viewing and managing snapshot history.
 */
public class SnapshotHistoryDialog extends DialogWrapper {

    private final Project project;
    private final JTable snapshotTable;
    private final DefaultTableModel tableModel;
    private final List<SnapshotInfo> snapshots = new ArrayList<>();

    public SnapshotHistoryDialog(Project project) {
        super(project);
        this.project = project;

        setTitle("Snapshot History");
        setOKButtonText("Close");
        setCancelButtonText("Delete Selected");

        String[] columns = {"Date", "Time", "Size", "File Name"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        snapshotTable = new JTable(tableModel);
        snapshotTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        snapshotTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        snapshotTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        snapshotTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        snapshotTable.getColumnModel().getColumn(3).setPreferredWidth(200);

        loadSnapshots();
        init();
    }

    private void loadSnapshots() {
        snapshots.clear();
        tableModel.setRowCount(0);

        String basePath = project.getBasePath();
        if (basePath == null) return;

        Path snapshotsDir = Paths.get(basePath, ".snapshots");
        if (!Files.exists(snapshotsDir)) return;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        try (Stream<Path> files = Files.list(snapshotsDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                 .filter(p -> p.getFileName().toString().startsWith("snapshot-"))
                 .sorted(Comparator.comparing(this::getFileCreationTime).reversed())
                 .forEach(path -> {
                     try {
                         BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                         Instant creationTime = attrs.creationTime().toInstant();
                         LocalDateTime dateTime = LocalDateTime.ofInstant(creationTime, ZoneId.systemDefault());
                         long size = Files.size(path);

                         SnapshotInfo info = new SnapshotInfo(
                             path,
                             dateTime,
                             size
                         );
                         snapshots.add(info);

                         tableModel.addRow(new Object[]{
                             dateTime.format(dateFormatter),
                             dateTime.format(timeFormatter),
                             formatSize(size),
                             path.getFileName().toString()
                         });
                     } catch (IOException e) {
                         // Skip files that can't be read
                     }
                 });
        } catch (IOException e) {
            // Directory listing failed
        }
    }

    private Instant getFileCreationTime(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return attrs.creationTime().toInstant();
        } catch (IOException e) {
            return Instant.MIN;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(500, 300));

        JScrollPane scrollPane = new JScrollPane(snapshotTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("Open");
        openButton.addActionListener(e -> openSelectedSnapshot());
        buttonsPanel.add(openButton);

        JButton compareButton = new JButton("Compare (select 2)");
        compareButton.addActionListener(e -> compareSelectedSnapshots());
        buttonsPanel.add(compareButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadSnapshots());
        buttonsPanel.add(refreshButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        if (snapshots.isEmpty()) {
            JLabel emptyLabel = new JLabel("No snapshots found", SwingConstants.CENTER);
            emptyLabel.setForeground(Color.GRAY);
            panel.add(emptyLabel, BorderLayout.NORTH);
        }

        return panel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        deleteSelectedSnapshot();
    }

    private void openSelectedSnapshot() {
        int row = snapshotTable.getSelectedRow();
        if (row < 0 || row >= snapshots.size()) {
            Messages.showWarningDialog("Please select a snapshot to open.", "Snapshots for AI");
            return;
        }

        SnapshotInfo info = snapshots.get(row);
        VirtualFile file = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(info.path);
        if (file != null) {
            file.refresh(false, false);
            FileEditorManager.getInstance(project).openFile(file, true);
            close(OK_EXIT_CODE);
        } else {
            Messages.showErrorDialog("Could not open snapshot file.", "Snapshots for AI");
        }
    }

    private void deleteSelectedSnapshot() {
        int[] rows = snapshotTable.getSelectedRows();
        if (rows.length == 0) {
            Messages.showWarningDialog("Please select a snapshot to delete.", "Snapshots for AI");
            return;
        }

        StringBuilder message = new StringBuilder("Are you sure you want to delete:\n");
        for (int row : rows) {
            if (row >= 0 && row < snapshots.size()) {
                message.append("- ").append(snapshots.get(row).path.getFileName()).append("\n");
            }
        }

        int result = Messages.showYesNoDialog(
            message.toString(),
            "Delete Snapshot(s)",
            Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            for (int i = rows.length - 1; i >= 0; i--) {
                int row = rows[i];
                if (row >= 0 && row < snapshots.size()) {
                    try {
                        Files.delete(snapshots.get(row).path);
                    } catch (IOException e) {
                        // Continue with other deletions
                    }
                }
            }
            loadSnapshots();
        }
    }

    private void compareSelectedSnapshots() {
        int[] rows = snapshotTable.getSelectedRows();
        if (rows.length != 2) {
            Messages.showWarningDialog(
                "Please select exactly 2 snapshots to compare.",
                "Snapshots for AI"
            );
            return;
        }

        SnapshotInfo info1 = snapshots.get(rows[0]);
        SnapshotInfo info2 = snapshots.get(rows[1]);

        VirtualFile file1 = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(info1.path);
        VirtualFile file2 = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(info2.path);

        if (file1 == null || file2 == null) {
            Messages.showErrorDialog("Could not open snapshot files for comparison.", "Snapshots for AI");
            return;
        }

        DiffContent content1 = DiffContentFactory.getInstance().create(project, file1);
        DiffContent content2 = DiffContentFactory.getInstance().create(project, file2);

        SimpleDiffRequest request = new SimpleDiffRequest(
            "Snapshot Comparison",
            content1, content2,
            info1.path.getFileName().toString(),
            info2.path.getFileName().toString()
        );

        DiffManager.getInstance().showDiff(project, request);
        close(OK_EXIT_CODE);
    }

    /**
     * Information about a snapshot file.
     */
    private static class SnapshotInfo {
        final Path path;
        final LocalDateTime dateTime;
        final long size;

        SnapshotInfo(Path path, LocalDateTime dateTime, long size) {
            this.path = path;
            this.dateTime = dateTime;
            this.size = size;
        }
    }
}
