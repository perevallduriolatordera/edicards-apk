package net.ifeu.library.Ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FTPUploader {

    private FTPClient ftpClient;

    public FTPUploader() {
        ftpClient = new FTPClient();
    }

    public void uploadFile(String server, int port, String username, String password, String remoteDirectory, String localFilePath) {
        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();

            createDirectories(remoteDirectory);

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            File localFile = new File(localFilePath);
            try (FileInputStream inputStream = new FileInputStream(localFile)) {
                String remoteFilePath = remoteDirectory + "/" + localFile.getName();
                boolean uploaded = ftpClient.storeFile(remoteFilePath, inputStream);
                if (uploaded) {
                    System.out.println("File uploaded successfully!");
                } else {
                    System.out.println("Failed to upload file.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDirectories(String remoteDirectory) throws IOException {
        String[] directories = remoteDirectory.split("/");
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                if (!ftpClient.changeWorkingDirectory(dir)) {
                    if (!ftpClient.makeDirectory(dir)) {
                        throw new IOException("Unable to create remote directory: " + dir);
                    }
                    ftpClient.changeWorkingDirectory(dir);
                }
            }
        }
    }

}
