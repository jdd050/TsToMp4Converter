import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Simulates console output
class ConsolePanel extends JPanel {
    private final JTextArea consoleArea;
    private final JScrollPane scrollPane;

    public ConsolePanel() {
        setLayout(new BorderLayout());

        consoleArea = new JTextArea(10, 40);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospace", Font.PLAIN, 12));
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.WHITE);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);

        scrollPane = new JScrollPane(consoleArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendLine(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public void clear() {
        consoleArea.setText("");
    }
}

// Custom PrintStream to redirect System.out to the console panel
class ConsolePrintStream extends java.io.PrintStream {
    private final ConsolePanel consolePanel;

    public ConsolePrintStream(ConsolePanel consolePanel) {
        super(System.out);
        this.consolePanel = consolePanel;
    }

    @Override
    public void println(String x) {
        super.println(x);
        consolePanel.appendLine(x);
    }
}

public class Main extends JFrame implements ActionListener {

    // App related fields, such as components
    private static final int APP_WIDTH = 800;
    private static final int APP_HEIGHT = 800;
    private static JLabel selectedFile;
    private static JLabel commandLabel = new JLabel();
    private static final ArrayList<File> files = new ArrayList<File>();
    private ConsolePanel consolePanel;
    // Limit threads
    private final ExecutorService executorService;

    public Main() {
        super();
        // Limit threads
        executorService = Executors.newSingleThreadExecutor();
        // Configure root window
        this.setSize(APP_WIDTH, APP_HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationByPlatform(true);
        this.setTitle("Transport Stream Converter");
        this.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(1,2, 10, 0));

        consolePanel = new ConsolePanel();

        System.setOut(new ConsolePrintStream(consolePanel));

        mainPanel.add(directoryPanel(this));
        mainPanel.add(ffmpegPanel(this));

        add(mainPanel, BorderLayout.CENTER);
        add(consolePanel, BorderLayout.SOUTH);
    }

    private JPanel directoryPanel(Main app) {
        JPanel directoryPanel = new JPanel();
        directoryPanel.setLayout(new BoxLayout(directoryPanel, BoxLayout.Y_AXIS));
        directoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton openSingleFile = new JButton("Open Single");
        JButton openMultiFile = new JButton("Open Multiple");
        openSingleFile.setPreferredSize(new Dimension(200, 50));
        openMultiFile.setPreferredSize(new Dimension(200, 50));

        openSingleFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        openSingleFile.setAlignmentY(Component.CENTER_ALIGNMENT);
        openMultiFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        openMultiFile.setAlignmentY(Component.CENTER_ALIGNMENT);

        openSingleFile.addActionListener(app);
        openMultiFile.addActionListener(app);

        directoryPanel.add(Box.createVerticalStrut(100)); // Spacer
        directoryPanel.add(openSingleFile);
        directoryPanel.add(Box.createVerticalStrut(100)); // Spacer
        directoryPanel.add(openMultiFile);
        directoryPanel.add(Box.createVerticalStrut(100)); // Spacer

        selectedFile = new JLabel("No file selected.");;
        selectedFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectedFile.setAlignmentY(Component.CENTER_ALIGNMENT);
        directoryPanel.add(selectedFile);

        return directoryPanel;
    }

    private JPanel ffmpegPanel(Main app) {
        JPanel ffmpegPanel = new JPanel();
        ffmpegPanel.setLayout(new BoxLayout(ffmpegPanel, BoxLayout.Y_AXIS));
        ffmpegPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel commandHeader = new JLabel("Sample command:");
        commandHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        commandHeader.setAlignmentY(Component.CENTER_ALIGNMENT);

        commandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        commandLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JButton convertButton = new JButton("Convert ts file(s)");
        convertButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        convertButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        convertButton.addActionListener(app);

        ffmpegPanel.add(Box.createVerticalStrut(150));
        ffmpegPanel.add(commandHeader);
        ffmpegPanel.add(commandLabel);
        ffmpegPanel.add(Box.createVerticalStrut(150));
        ffmpegPanel.add(convertButton);
        ffmpegPanel.add(Box.createVerticalStrut(150));
        return ffmpegPanel;
    }

    // Add window closing handler to properly shut down threads
    private void setupWindowClosing() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                shutdownExecutor();
            }
        });
    }

    private void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            // Thread management
            app.setupWindowClosing();
            FFmpeg ffmpegManager = new FFmpeg();

            if (!ffmpegManager.checkDependencies()) {
                System.out.println("FFmpeg not detected. Installing latest release.");
                if (!ffmpegManager.installFFmpeg()) {
                    System.out.println("Unable to install FFmpeg");
                }
            } else {
                System.out.println("FFmpeg detected.");
            }
            app.setVisible(true);
        });
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("Open Single")) {
            FileDialog fileDialog = new FileDialog(this, "Select a file", FileDialog.LOAD);
            fileDialog.setVisible(true);

            String filePath = fileDialog.getDirectory() + fileDialog.getFile();
            if (fileDialog.getFile() != null) {
                files.clear();
                File file = new File(filePath);
                if (!file.getName().contains(".ts")) {
                    selectedFile.setText("Invalid file selected. Only .ts files are supported.");
                } else {
                    selectedFile.setText(file.getAbsolutePath());
                    commandLabel.setText(commandLabel.getText() + "ffmpeg -i " + file.getName() + " output.mp4");
                    files.add(file);
                }
            } else {
                selectedFile.setText("User cancelled the operation");
            }
        } else if (command.equals("Open Multiple")) {
            FileDialog fileDialog = new FileDialog(this, "Select files", FileDialog.LOAD);
            fileDialog.setMultipleMode(true);
            fileDialog.setVisible(true);

            File[] selectedFiles = fileDialog.getFiles();
            if (selectedFiles.length > 0) {
                files.clear();
                int numOmitted = 0;
                for (File file : selectedFiles) {
                    if (file.getName().contains(".ts")) {
                        files.add(file);
                    } else {
                        System.out.println("Invalid file detected: " + file.getName());
                        numOmitted++;
                    }
                }
                selectedFile.setText(selectedFiles.length + " files selected. Non .ts files were omitted (" + Integer.toString(numOmitted) + "/" + Integer.toString(selectedFiles.length) + ")");
                commandLabel.setText(commandLabel.getText() + "ffmpeg -i " + files.getFirst() + " output.mp4");
            } else {
                selectedFile.setText("User cancelled the operation.");
            }
        } else if (command.equals("Convert ts file(s)")) {
            if (!files.isEmpty()) {
                for (File file : files) {
                    System.out.println("Queuing conversion for: " + file.getAbsolutePath());
                    executorService.submit(() -> {
                        try {
                            // Get number of CPU cores
                            int threads = Runtime.getRuntime().availableProcessors();

                            ProcessBuilder processBuilder = new ProcessBuilder(
                                    "ffmpeg",
                                    "-i", file.getAbsolutePath(),
                                    // Use multiple threads for encoding
                                    "-threads", String.valueOf(threads),
                                    // Use a faster preset since we're dedicating more resources
                                    "-preset", "faster",
                                    // Use all available cores for filtering
                                    "-filter_threads", String.valueOf(threads),
                                    // Use all available cores for video encoding
                                    "-filter_complex_threads", String.valueOf(threads),
                                    file.getAbsolutePath().replace(".ts", ".mp4")
                            );

                            processBuilder.redirectErrorStream(true);
                            Process ffmpeg = processBuilder.start();

                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.out.println(line);
                                }
                            }

                            int exitCode = ffmpeg.waitFor();
                            if (exitCode == 0) {
                                System.out.println("Conversion successful for: " + file.getName());
                            } else {
                                System.out.println("Conversion failed for: " + file.getName());
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

}