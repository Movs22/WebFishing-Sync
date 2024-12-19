package me.movies22.webfishing.sync;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;

public class Main {
	private static JFrame window;
	private static Logger logger;
	private static Drive driveService;
	private static String folderId;
	private static Boolean hasBackup = false;
	private static Path WEBFISHING_BACKUP;
	private static Path WEBFISHING;
	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {	
		
		try {
			
			// Initializes google drive service (used to save/load WebFish's data)
            driveService = GoogleDriveService.getDriveService();
            
            // Finds a folder named "WEBFISHING" under this user's account. If there isn't a folder with that name then creates one. NOTE: If there's a folder named "WEBFISHING" that wasn't created by this app, it won't pick up.
            folderId = GoogleDriveService.getOrCreateFolderId(driveService, "WEBFISHING");
            
            // Defines the WEBFISHING data folder's location (as well as a BACKUP folder's location)
            String dataFolder = System.getenv("APPDATA");
            WEBFISHING_BACKUP = Path.of(dataFolder + "\\Godot\\app_userdata\\webfishing_2_newver_BACKUP");
            WEBFISHING = Path.of(dataFolder + "\\Godot\\app_userdata\\webfishing_2_newver");
            
            // Checks if the backups folder exists.
            hasBackup = Files.exists(WEBFISHING_BACKUP);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
		
		logger = Logger.getGlobal();
		
		// Creates the app's GUI.
		window = new JFrame();
		window.setLayout(new BorderLayout());
		
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		window.setSize(300, 250);
		window.setResizable(false);
		window.setLocationRelativeTo(null);
		
		// Loads app's icon (as PNG)
		InputStream r = Main.class.getResourceAsStream("resources/app.png");
		try {
			window.setIconImage(ImageIO.read(r));
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e.getCause());
		}
		window.setTitle("WebFishing | Sync");

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(300, 200));
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.setLayout(new GridLayout(4, 1, 10, 10));
		
		JButton save = new JButton();
		save.setText("Save data");
		save.setEnabled(false);
		panel.add(save);
		
		JButton load = new JButton();
		load.setText("Load data");
		load.setEnabled(false);
		panel.add(load);
		
		JButton restore = new JButton();
		restore.setText("Restore");
		if(!hasBackup) restore.setEnabled(false);
		panel.add(restore);
		
		JPanel panel3 = new JPanel();
		
		JButton open = new JButton();
		open.setText("View save");
		open.setEnabled(false);
		JButton open2 = new JButton();
		open2.setText("View backup");
		open2.setEnabled(false);
		
		panel3.add(open);
		panel3.add(open2);	
		panel.add(panel3);
		
		JPanel panel2 = new JPanel();
		panel2.setPreferredSize(new Dimension(300, 20));
		panel2.setLayout(new FlowLayout(FlowLayout.CENTER));
		JLabel info = new JLabel();
		
		// Enables the save data/open data folder buttons if the user has successfully logged on.
		if(driveService == null) {
			info.setText("NOT logged in!");
		} else {
			About about = driveService.about().get().setFields("user").execute();
			save.setEnabled(true);
			open.setEnabled(true);
			if(hasBackup) open2.setEnabled(true);
			if(!GoogleDriveService.download(driveService, folderId).isEmpty()) load.setEnabled(true);
			
			info.setText("Logged in as " + about.getUser().getDisplayName() + " (" + about.getUser().getEmailAddress() + ")");
		}
		
		// Triggered when the user clicks on "Save Data"
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					save.setEnabled(false);
					window.pack();
					
					// Uploading... UI
					JFrame progressFrame = new JFrame("Uploading...");
					progressFrame.setResizable(false);
			        progressFrame.setSize(300, 100);
			        progressFrame.setLocationRelativeTo(null);
			        progressFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			        JLabel label = new JLabel();
			        label.setText("Uploading...");
			        JPanel panel = new JPanel();
			        panel.setLayout(new FlowLayout());
			        panel.add(label);
			        progressFrame.add(panel);
			        progressFrame.pack();
			        progressFrame.setVisible(true);
			        
			        // Triggers the upload process.
			        SaveManager.saveFiles(driveService, folderId, WEBFISHING, save, progressFrame, window, load);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(window, "Failed to save data.");
					e1.printStackTrace();
				}
			}
		});
		
		// Triggered when the user clicks on "Load Data"
		load.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// Backups existing data in case the download process goes horribly wrong.
					Boolean a = SaveManager.backupFiles(WEBFISHING, WEBFISHING_BACKUP);
					if(a) {
						hasBackup = true;
						open2.setEnabled(true);
						restore.setEnabled(true);
						load.setEnabled(false);
						JFrame progressFrame = new JFrame("Downloading...");
						progressFrame.setResizable(false);
				        progressFrame.setSize(300, 100);
				        progressFrame.setLocationRelativeTo(null);
				        progressFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

				        JLabel label = new JLabel();
				        label.setText("Downloading...");
				        JPanel panel = new JPanel();
				        panel.setLayout(new FlowLayout());
				        panel.add(label);
				        progressFrame.add(panel);
				        progressFrame.pack();
				        progressFrame.setVisible(true);
				        
				        // Downloads files
						SaveManager.downloadFiles(driveService, folderId, WEBFISHING, load, progressFrame, window, restore);
					}
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(window, "Failed to save data.");
					e1.printStackTrace();
				}
			}
		});
		
		// Restores data from backup folder
		restore.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SaveManager.restoreFiles(WEBFISHING, WEBFISHING_BACKUP, window);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(window, "FAILED to restore files.");
					e1.printStackTrace();
				}
			}
		});
		
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!Files.exists(WEBFISHING)) {
					JOptionPane.showMessageDialog(window, "Couldn't find the WEBFISHING Backups directory.");
					return;
				}
				try {
					Desktop.getDesktop().open(new File(WEBFISHING.toString()));
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(window, "Couldn't find the WEBFISHING directory.");
					e1.printStackTrace();
				}
			}
		});
		
		open2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!Files.exists(WEBFISHING_BACKUP)) {
					JOptionPane.showMessageDialog(window, "Couldn't find the WEBFISHING Backups directory.");
					return;
				}
				try {
					Desktop.getDesktop().open(new File(WEBFISHING_BACKUP.toString()));
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(window, "Couldn't find the WEBFISHING Backups directory.");
					e1.printStackTrace();
				}
			}
		});
		
		panel2.add(info);
		
		window.add(panel, BorderLayout.NORTH);

		window.add(panel2, BorderLayout.SOUTH);
		
		window.pack();
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		window.setVisible(true);
	}
}
