package dev.odoorunner;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Inline project setup. Discovery only reads paths; creation is an explicit user action. */
final class ProjectSetupPanel extends JPanel {
    private final RunnerWindow.Panel owner;
    private final Project project;
    private final Consumer<Workspace.Installation> onSave;
    private final Runnable onCancel;
    private final JTextField name = new RoundTextField(), source = new RoundTextField(), script = new RoundTextField("odoo-bin");
    private final JComboBox<String> interpreter = new RoundComboBox<>(), basePython = new RoundComboBox<>();
    private final JTextField destination = new RoundTextField();
    private final JTextArea notice = new JTextArea("Choose an Odoo source directory to find its virtual environments.");
    private final JTextArea output = new JTextArea(8, 60);
    private final JPanel projectFields = ScrollForm.group("Project source");
    private final JPanel createFields = ScrollForm.group("Create a virtual environment");
    private final JCheckBox createMode = new JCheckBox("Create a new virtual environment");
    private final JButton createButton, cancelCreation;
    private final javax.swing.Timer discoveryTimer;
    private Workspace.Installation original;
    private String initialSnapshot = "", lastSource = "";
    private boolean loading, busy, closed;
    private volatile boolean cancelRequested;
    private volatile CapturingProcessHandler creation;
    private SwingWorker<List<String>, Void> discovery;
    private int discoveryRevision;

    ProjectSetupPanel(RunnerWindow.Panel owner, Consumer<Workspace.Installation> onSave, Runnable onCancel) {
        super(new BorderLayout(10, 10));
        this.owner = owner; this.project = owner.project; this.onSave = onSave; this.onCancel = onCancel;
        interpreter.setEditable(true); basePython.setEditable(true);
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel form=projectFields;
        ScrollForm.field(form,"Project / version name",name);
        ScrollForm.field(form,"Odoo source directory",owner.pathPicker(source,true));
        ScrollForm.field(form,"Odoo entry script",owner.pathPicker(script,false));
        ScrollForm.field(form,"Python / virtualenv",comboPicker(interpreter));
        ScrollForm.field(form,"Discovery",owner.button("Detect environments",this::detect));ScrollForm.field(form,"Environment mode",createMode);
        ScrollForm.field(createFields,"Base Python executable",comboPicker(basePython));
        ScrollForm.field(createFields,"New environment path",owner.pathPicker(destination,true));
        createButton=owner.button("Create virtualenv",this::createEnvironment);cancelCreation=owner.button("Cancel creation",this::cancelCreation);cancelCreation.setEnabled(false);
        ScrollForm.field(createFields,"",owner.row(createButton,cancelCreation));
        ScrollForm contents=new ScrollForm();contents.section(form);contents.section(createFields);
        output.setEditable(false);output.setLineWrap(true);output.setWrapStyleWord(true);JPanel activity=ScrollForm.group("Environment activity");HiddenScrollPane log=new HiddenScrollPane(output);log.setPreferredSize(new Dimension(400,160));ScrollForm.field(activity,"",log);contents.section(activity);add(new HiddenScrollPane(contents));
        JPanel header=new JPanel(new BorderLayout(12,0));header.add(owner.button("← Runners",this::cancel),BorderLayout.WEST);header.add(Design.label("Project setup",18,true));header.add(owner.button("Save project",this::save),BorderLayout.EAST);add(header,BorderLayout.NORTH);
        notice.setEditable(false);notice.setOpaque(false);notice.setLineWrap(true);notice.setWrapStyleWord(true);notice.setFont(UIManager.getFont("Label.font"));notice.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));add(notice,BorderLayout.SOUTH);
        createMode.addActionListener(e -> updateControls());
        discoveryTimer = new javax.swing.Timer(350, e -> detect()); discoveryTimer.setRepeats(false);
        source.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { if (!loading && !busy) { discoveryRevision++; discoveryTimer.restart(); } }
        });
        updateControls();
    }

    private JPanel comboPicker(JComboBox<String> combo) {
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.add(combo);
        row.add(owner.button("Browse…", () -> {
            var chosen=PathBrowser.choose(project,selection(combo),false,false);if(!chosen.isEmpty())combo.setSelectedItem(chosen.get(0).toString());
        }), BorderLayout.EAST);
        return row;
    }

    void begin(Workspace.Installation installation) {
        discoveryRevision++; discoveryTimer.stop(); if (discovery != null) discovery.cancel(true);
        loading = true; original = installation; closed = false;
        name.setText(installation == null ? "" : installation.name);
        source.setText(installation == null ? "" : installation.root);
        script.setText(installation == null ? "odoo-bin" : installation.script);
        interpreter.removeAllItems(); interpreter.setSelectedItem(installation == null ? owner.workspace.getState().defaultPython : installation.python);
        basePython.removeAllItems(); for (String path : systemPythons()) basePython.addItem(path);
        if (!owner.workspace.getState().defaultPython.isBlank()) basePython.setSelectedItem(owner.workspace.getState().defaultPython);
        destination.setText(installation == null || installation.root.isBlank() ? "" : Path.of(installation.root).resolve(".venv").toString());
        lastSource = source.getText(); createMode.setSelected(false); output.setText(""); loading = false;
        initialSnapshot = snapshot(); updateControls(); if (!source.getText().isBlank()) detect();
    }

    private String selection(JComboBox<String> combo) { return Objects.toString(combo.getEditor().getItem(), "").trim(); }
    private String snapshot() { return String.join("\0", name.getText(), source.getText(), script.getText(), selection(interpreter), selection(basePython), destination.getText(), "" + createMode.isSelected()); }
    private void updateControls() {
        setChildrenEnabled(projectFields, !busy);
        createFields.setVisible(createMode.isSelected());
        for (Component c : createFields.getComponents()) c.setEnabled(!busy);
        setChildrenEnabled(createFields, !busy); cancelCreation.setEnabled(busy); createButton.setEnabled(!busy);
        name.setEnabled(!busy); source.setEnabled(!busy); script.setEnabled(!busy); interpreter.setEnabled(!busy); createMode.setEnabled(!busy);
        revalidate(); repaint();
    }
    private void setChildrenEnabled(Container container, boolean enabled) {
        for (Component c : container.getComponents()) { c.setEnabled(enabled); if (c instanceof Container child) setChildrenEnabled(child, enabled); }
    }

    private void detect() {
        if (busy || closed) return;
        String text = source.getText().trim(); final Path root;
        try { root = Path.of(text); if (text.isBlank() || !root.isAbsolute() || !Files.isDirectory(root)) { notice.setText("Choose an existing absolute Odoo source directory."); return; } }
        catch (InvalidPathException ex) { notice.setText("Invalid source directory."); return; }
        boolean wasClean = snapshot().equals(initialSnapshot);
        final String selectedBefore = selection(interpreter);
        if (!text.equals(lastSource)) { destination.setText(root.resolve(".venv").toString()); lastSource = text; }
        final int revision = ++discoveryRevision;
        if (discovery != null) discovery.cancel(true);
        final String beforeDiscovery = snapshot();
        notice.setText("Looking for virtual environments…");
        discovery = new SwingWorker<>() {
            protected List<String> doInBackground() { return findEnvironments(root); }
            protected void done() {
                if (closed || revision != discoveryRevision || isCancelled()) return;
                try {
                    boolean unchangedDuringDiscovery = snapshot().equals(beforeDiscovery);
                    List<String> found = get(); String current = selection(interpreter);
                    interpreter.removeAllItems(); for (String python : found) interpreter.addItem(python);
                    // Initial edits retain the saved interpreter. A new source selects its first detected environment.
                    boolean keepSaved = original != null && root.toString().equals(original.root) && current.equals(original.python);
                    if (keepSaved || !current.equals(selectedBefore) || (!current.isBlank() && found.isEmpty())) interpreter.setSelectedItem(current);
                    notice.setText(found.isEmpty() ? "No local virtualenv found. Browse to one or create a new environment below." : "Found " + found.size() + " virtual environment(s). Select the interpreter to use.");
                    if (wasClean && unchangedDuringDiscovery) initialSnapshot = snapshot();
                } catch (Exception ex) { notice.setText("Environment discovery failed: " + ex.getMessage()); }
            }
        }; discovery.execute();
    }

    static Path environmentPython(Path directory) {
        if (!Files.isRegularFile(directory.resolve("pyvenv.cfg"))) return null;
        for (String executable : List.of("bin/python", "bin/python3", "Scripts/python.exe")) {
            Path candidate = directory.resolve(executable);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) return candidate.toAbsolutePath().normalize();
        }
        return null;
    }

    static List<String> findEnvironments(Path root) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>(); candidates.add(root);
        for (String directory : List.of(".venv", "venv", "env", ".env")) candidates.add(root.resolve(directory));
        // One shallow source-directory scan also discovers custom names; never traverse addon trees.
        try (var children = Files.list(root)) { children.limit(200).filter(Files::isDirectory).sorted().forEach(candidates::add); }
        catch (Exception ignored) { }
        Path parent = root.getParent();
        if (parent != null) for (String directory : List.of(".venv", "venv", "env", ".env")) candidates.add(parent.resolve(directory));
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (Path directory : candidates) { Path python = environmentPython(directory); if (python != null) found.add(python.toString()); }
        return new ArrayList<>(found);
    }

    private List<String> systemPythons() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        String configured = owner.workspace.getState().defaultPython; if (!configured.isBlank()) paths.add(configured);
        LinkedHashSet<String> directories = new LinkedHashSet<>(List.of(System.getenv().getOrDefault("PATH", "").split(java.io.File.pathSeparator)));
        directories.addAll(List.of("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin"));
        for (String directory : directories) {
            if (directory.isBlank()) continue;
            for (String python : List.of("python3", "python3.14", "python3.13", "python3.12", "python3.11", "python3.10", "python3.9", "python.exe", "python")) {
                Path candidate = Path.of(directory, python); if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) paths.add(candidate.toAbsolutePath().normalize().toString());
            }
        }
        return new ArrayList<>(paths);
    }

    private void createEnvironment() {
        if (busy) return;
        Path root = absoluteDirectory(source.getText().trim());
        Path python = executable(selection(basePython));
        Path target = Path.of(destination.getText().trim());
        if (!target.isAbsolute() || target.getParent() == null || !Files.isDirectory(target.getParent())) throw new IllegalArgumentException("Choose an absolute new environment path inside an existing parent directory.");
        target = target.normalize();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("The environment directory already exists. Select its interpreter or choose a new directory; existing folders are never overwritten.");
        final Path environment = target;
        busy = true; cancelRequested = false; discoveryRevision++; discoveryTimer.stop(); if (discovery != null) discovery.cancel(true);
        output.setText("Creating virtualenv using " + python + "\nDestination: " + environment + "\n");
        notice.setText("Creating virtual environment…"); updateControls();
        new SwingWorker<Path, Void>() {
            protected Path doInBackground() throws Exception {
                if (cancelRequested) throw new IllegalStateException("Creation cancelled.");
                Files.createDirectory(environment); // Atomic refusal to reuse a target that appeared since validation.
                if (cancelRequested) throw new IllegalStateException("Creation cancelled. Empty target directory retained.");
                GeneralCommandLine command = new GeneralCommandLine(python.toString(), "-m", "venv", environment.toString()).withWorkDirectory(root.toString());
                creation = new CapturingProcessHandler(command);
                if (cancelRequested) { OSProcessUtil.killProcessTree(creation.getProcess()); creation.destroyProcess(); }
                ProcessOutput result = creation.runProcess(180000);
                String log = result.getStdout() + result.getStderr();
                SwingUtilities.invokeLater(() -> { if (!closed) output.append(log.length() > 24000 ? log.substring(0, 24000) + "\n[output truncated]" : log); });
                if (cancelRequested) throw new IllegalStateException("Creation cancelled; partial directory retained.");
                if (result.isTimeout()) throw new IllegalStateException("Creation timed out after 3 minutes; partial directory retained.");
                if (result.getExitCode() != 0) throw new IllegalStateException("Python venv exited with code " + result.getExitCode() + ". See output. Partial directory retained.");
                Path created = environmentPython(environment);
                if (created == null) throw new IllegalStateException("Python did not produce a usable virtual environment. See output.");
                return created;
            }
            protected void done() {
                creation = null; busy = false; if (closed) return; updateControls();
                try { interpreter.setSelectedItem(get().toString()); createMode.setSelected(false); updateControls(); notice.setText("Virtualenv created and selected. Save project to use it."); output.append("\nCreated successfully. Odoo requirements are not installed automatically.\n"); }
                catch (Exception ex) { Throwable cause = ex.getCause() == null ? ex : ex.getCause(); notice.setText(Objects.toString(cause.getMessage(), "Environment creation failed.")); output.append("\n" + notice.getText() + "\n"); }
            }
        }.execute();
    }

    private Path absoluteDirectory(String value) {
        Path path = Path.of(value); if (value.isBlank() || !path.isAbsolute() || !Files.isDirectory(path)) throw new IllegalArgumentException("Choose an existing absolute Odoo source directory."); return path.normalize();
    }
    private Path executable(String value) {
        Path path = Path.of(value); if (value.isBlank() || !path.isAbsolute() || !Files.isRegularFile(path) || !Files.isExecutable(path)) throw new IllegalArgumentException("Choose an absolute Python executable."); return path.normalize();
    }
    private void save() {
        if (busy) throw new IllegalArgumentException("Wait for virtualenv creation to finish, or cancel creation first.");
        Path root = absoluteDirectory(source.getText().trim()), python = executable(selection(interpreter));
        if (name.getText().isBlank()) throw new IllegalArgumentException("Project name is required.");
        String entry = script.getText().trim(); if (entry.isBlank() || !Files.isRegularFile(root.resolve(entry))) throw new IllegalArgumentException("Choose an existing Odoo entry script.");
        if (createMode.isSelected()) throw new IllegalArgumentException("Create the requested virtualenv first, or uncheck Create a new virtual environment to use the selected interpreter.");
        if (original != null && owner.workspace.dependencies.busy(original)) throw new IllegalArgumentException("Wait for dependency installation to finish before editing project setup.");
        if (original != null && original.runners.stream().anyMatch(owner::active)) throw new IllegalArgumentException("Stop this project's runners before saving setup.");
        Workspace.Installation saved = original == null ? new Workspace.Installation() : original;
        saved.name = name.getText().trim(); saved.root = root.toString(); saved.python = python.toString(); saved.script = entry;
        initialSnapshot = snapshot(); onSave.accept(saved);
    }
    private void cancelCreation() { cancelRequested = true; CapturingProcessHandler handler = creation; if (handler != null && !handler.isProcessTerminated()) { OSProcessUtil.killProcessTree(handler.getProcess()); handler.destroyProcess(); } notice.setText("Cancellation requested. Partial environment files will be retained."); }
    private void cancel() { if (canLeave()) onCancel.run(); }
    boolean canLeave() {
        if (busy) { notice.setText("Wait for environment creation or use Cancel creation before leaving setup."); return false; }
        return snapshot().equals(initialSnapshot) || Messages.showYesNoDialog(project, "Discard unsaved project setup? Any created virtualenv stays on disk.", "Project setup", Messages.getQuestionIcon()) == Messages.YES;
    }
    void dispose() { closed = true; discoveryTimer.stop(); discoveryRevision++; if (discovery != null) discovery.cancel(true); if (busy) cancelCreation(); }
}
