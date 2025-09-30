package me.robwilliams.pack.cloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GoogleDriveService {
    private static final String TAG = "GoogleDriveService";
    private static final int REQUEST_CODE_SIGN_IN = 1001;
    private static final String APP_FOLDER = "Pack_Backup";

    private Context context;
    private Drive driveService;
    private GoogleSignInClient signInClient;

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface BackupCallback {
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RestoreCallback {
        void onBackupListReady(List<CloudBackup> backups);
        void onRestoreProgress(String message);
        void onRestoreSuccess(String message);
        void onError(String error);
    }

    public static class CloudBackup {
        public String id;
        public String name;
        public Date date;
        public long size;

        public CloudBackup(String id, String name, Date date, long size) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.size = size;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(date) + " (" + (size / 1024) + " KB)";
        }
    }

    public GoogleDriveService(Context context) {
        this.context = context;
        initializeSignInClient();
    }

    private void initializeSignInClient() {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA));

        // Get build-variant specific OAuth configuration
        String serverClientId = getOAuthClientId();
        String buildType = context.getString(me.robwilliams.pack.R.string.build_type);

        Log.d(TAG, "Initializing Google Sign-In for build type: " + buildType);
        Log.d(TAG, "OAuth Client ID: " + (serverClientId != null ? serverClientId.substring(0, Math.min(20, serverClientId.length())) + "..." : "null"));

        if (serverClientId != null && !isPlaceholderClientId(serverClientId)) {
            Log.d(TAG, "Using configured OAuth client ID for " + buildType + " build");
            // Note: We don't use requestIdToken() as it requires a web client ID
            // The Android client ID alone is sufficient for Google Drive API access
        } else {
            Log.w(TAG, "No valid OAuth client ID configured for " + buildType + " build. " +
                    "Please update the client ID in src/" + buildType + "/res/values/google_oauth_config.xml");
        }

        GoogleSignInOptions signInOptions = builder.build();
        signInClient = GoogleSignIn.getClient(context, signInOptions);
    }

    private String getOAuthClientId() {
        try {
            return context.getString(me.robwilliams.pack.R.string.server_client_id);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get OAuth client ID from resources", e);
            return null;
        }
    }

    private boolean isPlaceholderClientId(String clientId) {
        return clientId == null ||
               clientId.equals("YOUR_DEBUG_OAUTH_CLIENT_ID_HERE") ||
               clientId.equals("YOUR_RELEASE_OAUTH_CLIENT_ID_HERE") ||
               clientId.trim().isEmpty();
    }

    public boolean isSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null && !account.isExpired();
    }

    /**
     * Sign out from Google Drive and clear cached authentication
     */
    public void signOut() {
        Log.d(TAG, "Signing out from Google Drive");
        if (signInClient != null) {
            signInClient.signOut().addOnCompleteListener(task -> {
                Log.d(TAG, "Sign out completed");
            });
        }
        driveService = null;
    }

    /**
     * Check if OAuth is properly configured for the current build variant
     * @return true if a valid OAuth client ID is configured
     */
    public boolean isOAuthConfigured() {
        String clientId = getOAuthClientId();
        boolean configured = !isPlaceholderClientId(clientId);

        String buildType = context.getString(me.robwilliams.pack.R.string.build_type);
        Log.d(TAG, "OAuth configuration check for " + buildType + " build: " + (configured ? "CONFIGURED" : "NOT CONFIGURED"));

        return configured;
    }

    /**
     * Get debugging information about the current OAuth configuration
     * @return String with configuration details for logging
     */
    public String getOAuthConfigDebugInfo() {
        String buildType = context.getString(me.robwilliams.pack.R.string.build_type);
        String clientId = getOAuthClientId();
        boolean configured = !isPlaceholderClientId(clientId);

        return "Build Type: " + buildType +
               ", OAuth Configured: " + (configured ? "YES" : "NO") +
               ", Client ID: " + (configured ? "[CONFIGURED]" : "[PLACEHOLDER]");
    }

    public void signIn(Activity activity, AuthCallback callback, androidx.activity.result.ActivityResultLauncher<Intent> launcher) {
        if (isSignedIn()) {
            initializeDriveService();
            callback.onSuccess();
            return;
        }

        // Store callback for later use
        this.authCallback = callback;

        Intent signInIntent = signInClient.getSignInIntent();
        launcher.launch(signInIntent);
    }

    private AuthCallback authCallback;

    public void handleSignInResult(Task<GoogleSignInAccount> completedTask, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Sign in successful for: " + account.getEmail());
            Log.d(TAG, "Account granted scopes: " + account.getGrantedScopes());

            initializeDriveService();
            Log.d(TAG, "Drive service initialized");

            // Call the provided callback
            callback.onSuccess();

            // Also call the stored callback if it exists (for backup/restore operations)
            if (authCallback != null) {
                Log.d(TAG, "Calling stored callback for backup/restore");
                authCallback.onSuccess();
                authCallback = null; // Clear it after use
            }
        } catch (ApiException e) {
            Log.e(TAG, "Sign in failed with code: " + e.getStatusCode(), e);
            String errorMessage = getHumanReadableError(e);

            callback.onError(errorMessage);

            // Also call the stored callback if it exists
            if (authCallback != null) {
                authCallback.onError(errorMessage);
                authCallback = null; // Clear it after use
            }
        } catch (Exception e) {
            Log.e(TAG, "Sign in failed", e);
            String errorMessage = "Unexpected sign in error: " + e.getMessage();
            callback.onError(errorMessage);

            // Also call the stored callback if it exists
            if (authCallback != null) {
                authCallback.onError(errorMessage);
                authCallback = null; // Clear it after use
            }
        }
    }

    private String getHumanReadableError(ApiException e) {
        switch (e.getStatusCode()) {
            case 7: // NETWORK_ERROR
                return "Network error. Please check your internet connection.";
            case 8: // INTERNAL_ERROR
                return "Internal error. Please try again.";
            case 10: // DEVELOPER_ERROR
                return "App configuration error. Please check OAuth setup.";
            case 12501: // SIGN_IN_CANCELLED
                return "Sign in was cancelled.";
            case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                return "Sign in already in progress.";
            case 12500: // SIGN_IN_FAILED
                return "Sign in failed. Please try again.";
            default:
                return "Sign in failed: " + e.getMessage() + " (Code: " + e.getStatusCode() + ")";
        }
    }

    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
            credential.setSelectedAccount(account.getAccount());

            driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Pack")
                    .build();
        }
    }

    public void backupDatabase(java.io.File databaseFile, BackupCallback callback) {
        if (driveService == null) {
            callback.onError("Not authenticated with Google Drive");
            return;
        }

        new BackupTask(databaseFile, callback).execute();
    }

    public void listBackups(RestoreCallback callback) {
        if (driveService == null) {
            callback.onError("Not authenticated with Google Drive");
            return;
        }

        new ListBackupsTask(callback).execute();
    }

    public void restoreDatabase(String fileId, java.io.File targetFile, RestoreCallback callback) {
        if (driveService == null) {
            callback.onError("Not authenticated with Google Drive");
            return;
        }

        new RestoreTask(fileId, targetFile, callback).execute();
    }

    private class BackupTask extends AsyncTask<Void, String, String> {
        private java.io.File databaseFile;
        private BackupCallback callback;

        public BackupTask(java.io.File databaseFile, BackupCallback callback) {
            this.databaseFile = databaseFile;
            this.callback = callback;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            callback.onProgress(progress[0]);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                publishProgress("Preparing backup...");

                if (driveService == null) {
                    return "ERROR: Drive service not initialized";
                }

                Log.d(TAG, "Starting backup for file: " + databaseFile.getAbsolutePath());
                Log.d(TAG, "File exists: " + databaseFile.exists() + ", size: " + databaseFile.length());

                // Create filename with timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String fileName = "pack_backup_" + timestamp + ".db";

                publishProgress("Uploading to Google Drive...");

                // Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList("appDataFolder"));

                Log.d(TAG, "Uploading file: " + fileName);

                // Upload file
                FileContent mediaContent = new FileContent("application/x-sqlite3", databaseFile);
                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();

                Log.d(TAG, "Upload successful, file ID: " + uploadedFile.getId());
                return "Backup completed successfully: " + fileName;

            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                e.printStackTrace(); // This will show in logcat
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.startsWith("ERROR:")) {
                callback.onError(result.substring(7));
            } else {
                callback.onSuccess(result);
            }
        }
    }

    private class ListBackupsTask extends AsyncTask<Void, Void, List<CloudBackup>> {
        private RestoreCallback callback;
        private String error;

        public ListBackupsTask(RestoreCallback callback) {
            this.callback = callback;
        }

        @Override
        protected List<CloudBackup> doInBackground(Void... voids) {
            try {
                FileList result = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name contains 'pack_backup' and name contains '.db'")
                        .setFields("files(id, name, createdTime, size)")
                        .setOrderBy("createdTime desc")
                        .execute();

                List<CloudBackup> backups = new ArrayList<>();
                for (File file : result.getFiles()) {
                    Date date = new Date(file.getCreatedTime().getValue());
                    long size = file.getSize() != null ? file.getSize() : 0;
                    backups.add(new CloudBackup(file.getId(), file.getName(), date, size));
                }

                return backups;

            } catch (Exception e) {
                Log.e(TAG, "Failed to list backups", e);
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CloudBackup> backups) {
            if (backups != null) {
                callback.onBackupListReady(backups);
            } else {
                callback.onError("Failed to load backups: " + error);
            }
        }
    }

    private class RestoreTask extends AsyncTask<Void, String, String> {
        private String fileId;
        private java.io.File targetFile;
        private RestoreCallback callback;

        public RestoreTask(String fileId, java.io.File targetFile, RestoreCallback callback) {
            this.fileId = fileId;
            this.targetFile = targetFile;
            this.callback = callback;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            callback.onRestoreProgress(progress[0]);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                publishProgress("Downloading backup from Google Drive...");

                // Download file
                FileOutputStream outputStream = new FileOutputStream(targetFile);
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                outputStream.close();

                return "Database restored successfully";

            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.startsWith("ERROR:")) {
                callback.onError(result.substring(7));
            } else {
                callback.onRestoreSuccess(result);
            }
        }
    }
}