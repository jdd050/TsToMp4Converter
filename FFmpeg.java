import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class FFmpeg {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_RELEASE_PATH = "/etc/os-release";
    private static final String FFMPEG_VERSION_CMD = "ffmpeg -version";

    /**
     * Checks if FFmpeg is installed and accessible in the system path.
     * @return true if FFmpeg is detected, false otherwise
     */
    public boolean checkDependencies() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ffmpeg", "-version"});

            try (Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A")) {
                String result = scanner.hasNext() ? scanner.next() : "";
                return result.toLowerCase().contains("ffmpeg version");
            }
        } catch (IOException e) {
            System.err.println("Error checking FFmpeg: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to install FFmpeg using the appropriate package manager for the current OS.
     * @return true if installation was successful, false otherwise
     */
    public boolean installFFmpeg() {
        if (OS_NAME.contains("win")) {
            return installWindowsFFmpeg();
        } else if (OS_NAME.contains("mac")) {
            return installMacFFmpeg();
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux")) {
            return installLinuxFFmpeg();
        }

        System.err.println("Unsupported operating system: " + OS_NAME);
        return false;
    }

    private boolean installWindowsFFmpeg() {
        try {
            if (!isWingetAvailable()) {
                System.err.println("Winget package manager not found");
                return false;
            }

            Process ffmpegInstall = Runtime.getRuntime().exec("winget install FFmpeg");
            return ffmpegInstall.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error installing FFmpeg on Windows: " + e.getMessage());
            return false;
        }
    }

    private boolean isWingetAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("powershell.exe winget; echo $?");
            process.getOutputStream().close();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("True")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error checking winget availability: " + e.getMessage());
        }
        return false;
    }

    private boolean installMacFFmpeg() {
        try {
            Process process = Runtime.getRuntime().exec("brew install ffmpeg");
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error installing FFmpeg on Mac: " + e.getMessage());
            return false;
        }
    }

    private boolean installLinuxFFmpeg() {
        String distro = getLinuxDistro();
        if (distro == null) {
            System.err.println("Failed to determine Linux distribution");
            return false;
        }

        try {
            String command = getLinuxInstallCommand(distro);
            if (command == null) {
                System.err.println("Unsupported Linux distribution: " + distro);
                return false;
            }

            Process process = Runtime.getRuntime().exec(command);
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error installing FFmpeg on Linux: " + e.getMessage());
            return false;
        }
    }

    private String getLinuxDistro() {
        try (BufferedReader reader = new BufferedReader(new FileReader(OS_RELEASE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ID=")) {
                    return line.split("=")[1].replace("\"", "").toLowerCase();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading OS release info: " + e.getMessage());
        }
        return null;
    }

    private String getLinuxInstallCommand(String distro) {
        return switch (distro) {
            case "ubuntu", "debian" -> "sudo apt update && sudo apt install -y ffmpeg";
            case "fedora" -> "sudo dnf install -y ffmpeg";
            case "arch" -> "sudo pacman -S --noconfirm ffmpeg";
            default -> null;
        };
    }
}