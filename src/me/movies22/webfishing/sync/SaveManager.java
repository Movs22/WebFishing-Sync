package me.movies22.webfishing.sync;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class SaveManager {

	public static boolean backupFiles(Path source, Path dest) throws IOException {
		if (!Files.exists(dest)) {
			Files.createDirectories(dest);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
			for (Path entry : stream) {
				if (entry.getFileName().toString().equals("logs")) {
					continue;
				}

				Path targetFile = dest.resolve(entry.getFileName());

				if (Files.isRegularFile(entry)) {
					Files.copy(entry, targetFile, StandardCopyOption.REPLACE_EXISTING);
				}

			}
		}
		return true;
	}
	
	public static boolean restoreFiles(Path source, Path dest, JFrame window) throws IOException {
		if (!Files.exists(source)) {
			JOptionPane.showMessageDialog(window, "Successfully saved your data on the cloud!");
			return false;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dest)) {
			for (Path entry : stream) {
				if (entry.getFileName().toString().equals("logs")) {
					continue;
				}

				Path targetFile = source.resolve(entry.getFileName());

				if (Files.isRegularFile(entry)) {
					Files.copy(entry, targetFile, StandardCopyOption.REPLACE_EXISTING);
				}

			}
		}
		JOptionPane.showMessageDialog(window, "Successfully restored your data to the previous save file!");
		return true;
	}

	public static boolean saveFiles(Drive ds, String folderId, Path source, JButton save, JFrame progress, JFrame window, JButton load)
			throws IOException {
		new Thread(() -> {
			List<File> previous;
			
			try {
				previous = GoogleDriveService.download(ds, folderId);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			int i = 0;

			try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
				for (Path entry : stream) {
					if (entry.getFileName().toString().equals("logs")) {
						continue;
					}

					java.io.File file = entry.toFile();
					File prevFile = null;
					for (File p : previous) {
						System.out.println(p.getName() + " | " + entry.getFileName() + " | "
								+ p.getName().equals(entry.getFileName().toString()));
						if (p.getName().equals(entry.getFileName().toString())) {
							prevFile = p;
							break;
						}
						;
					}

					if (Files.isRegularFile(entry)) {
						GoogleDriveService.uploadFile(ds, folderId, file, prevFile);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			JOptionPane.showMessageDialog(window, "Successfully saved your data on the cloud!");
			load.setEnabled(true);
			save.setEnabled(true);
			progress.dispose();
			System.out.println("FINISHED");
		}).start();
		return true;
	}
	
	public static boolean downloadFiles(Drive ds, String folderId, Path target, JButton load, JFrame progress, JFrame window, JButton restore)
			throws IOException {
		new Thread(() -> {
			List<File> files;
			
			try {
				files = GoogleDriveService.download(ds, folderId);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			for(File file : files) {
				try {
					InputStream inputStream = ds.files().get(file.getId()).executeMediaAsInputStream();
					try (FileOutputStream outputStream = new FileOutputStream(Paths.get(target.toString(), file.getName()).toString())) {
			            byte[] buffer = new byte[1024];  // Buffer to read data in chunks
			            int bytesRead;
			            
			            // Read the content from the InputStream and write it to the OutputStream
			            while ((bytesRead = inputStream.read(buffer)) != -1) {
			                outputStream.write(buffer, 0, bytesRead);
			            }
			        } catch (IOException e) {
			            System.err.println("Error downloading file: " + e.getMessage());
			        }
				} catch (IOException e) {
					e.printStackTrace();
				}
		        

				System.out.println(file.getName());
			}
			JOptionPane.showMessageDialog(window, "Successfully downloaded your data on the cloud! Press RESTORE to restore your previous (local) save.");
			load.setEnabled(true);
			restore.setEnabled(true);
			progress.dispose();
			System.out.println("FINISHED");
		}).start();
		return true;
	}
}
