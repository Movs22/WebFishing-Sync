package me.movies22.webfishing.sync;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public class GoogleDriveService {

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";


    public static Credential authorize() throws Exception {
        // Load client secrets from resources/
        try (InputStreamReader reader = new InputStreamReader(
                GoogleDriveService.class.getResourceAsStream("resources/client_secret.json"))) {

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);

            // Prompts user with login screen
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    clientSecrets,
                    Collections.singletonList(DriveScopes.DRIVE_FILE)).setDataStoreFactory(new FileDataStoreFactory(Paths.get(TOKENS_DIRECTORY_PATH).toFile()))
                    .setAccessType("offline") 
                    .build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver.Builder().setPort(8080).setCallbackPath("/callback").build())
                    .authorize("user");
        }
    }
    
    public static Drive getDriveService() throws Exception {
    	Credential credential = authorize();

        return new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential
        ).setApplicationName("Webfishing Sync").build();
    }
    
    public static String getOrCreateFolderId(Drive driveService, String folderName) throws IOException {
        // Search for the folder by name. Ignores any folders in the trash
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed = false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();

        if (files.isEmpty()) {
            // Folder doesn't exist, create it
            File folderMetadata = new File();
            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            // Create the folder
            File folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            return folder.getId();
        } else {
            // Folder exists, return its ID
            return files.get(0).getId();
        }
    }
    
    public static File getFileByName(Drive driveService, String folderId, String fileName) throws IOException {
    	// Finds a file with the given name, under the specified folderId. Discards any files in the trash
        String query = "mimeType != 'application/vnd.google-apps.folder' and name = '" + fileName + "' and '" + folderId + "' in parents and trashed = false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, parents)")
                .execute();

        List<File> files = result.getFiles();

        if (files.isEmpty()) {
            return null; // File does not exist
        } else {
            return files.get(0); // Return the first matching file
        }
    }
    
    
    // Returns all files under the specified folderId;
    public static List<File> download(Drive driveService, String folderId) throws IOException {
    	String query = "mimeType != 'application/vnd.google-apps.folder' and '" + folderId + "' in parents and trashed = false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, parents)")
                .execute();

        List<File> files = result.getFiles();

        return files;
    }
    
    public static boolean uploadFile(Drive driveService, String folderId, java.io.File uploading, File previous) throws IOException {
    	if(previous != null) {
    		driveService.files().delete(previous.getId()).execute();
    	}
    	
    	File fileMetadata = new File();
        fileMetadata.setName(uploading.getName());
        fileMetadata.setParents(Collections.singletonList(folderId));
        
        FileContent mediaContent = new FileContent("application/octet-stream", uploading);  // Adjust MIME type as needed

        // Upload the file
        driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, parents")
                .execute();
        return true;
    }
}
