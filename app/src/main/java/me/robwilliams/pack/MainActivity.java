package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.List;

import me.robwilliams.pack.cloud.GoogleDriveService;
import me.robwilliams.pack.data.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private GoogleDriveService driveService;
    private ActivityResultLauncher<Intent> signInLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private AlertDialog currentBackupDialog; // Track the current backup selection dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driveService = new GoogleDriveService(this);
        setupActivityResultLaunchers();
    }

    private void setupActivityResultLaunchers() {
        // Set up sign-in result launcher
        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Sign-in result received with code: " + result.getResultCode());
                Log.d(TAG, "Intent data present: " + (result.getData() != null));
                if (result.getData() != null) {
                    Log.d(TAG, "Intent data: " + result.getData().toString());
                }

                // For Google Sign-in, we should always try to get the account from the intent data
                // The result code might not be RESULT_OK even for successful sign-ins
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    driveService.handleSignInResult(task, new GoogleDriveService.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Sign-in callback: success");
                            Toast.makeText(MainActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Sign-in callback: error - " + error);
                            Toast.makeText(MainActivity.this, "Sign in failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Log.d(TAG, "Sign-in cancelled - no intent data received");
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Set up file picker result launcher
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "File picker result received with code: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFileImport(result.getData());
                }
            }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void openListsActivity(View view) {
        Intent intent = new Intent(this, ListOverviewActivity.class);
        startActivity(intent);
    }

    public void openSetsActivity(View view) {
        Intent intent = new Intent(this, SetOverviewActivity.class);
        startActivity(intent);
    }

    public void openTripsActivity(View view) {
        Intent intent = new Intent(this, TripOverviewActivity.class);
        startActivity(intent);
    }

    public void openCloudBackup(View view) {
        showCloudBackupDialog();
    }

    private void showCloudBackupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cloud_backup, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.backupButton).setOnClickListener(v -> {
            dialog.dismiss();
            performBackup();
        });

        dialogView.findViewById(R.id.restoreButton).setOnClickListener(v -> {
            dialog.dismiss();
            performRestore();
        });

        dialogView.findViewById(R.id.importFileButton).setOnClickListener(v -> {
            dialog.dismiss();
            importDatabaseFile();
        });

        dialogView.findViewById(R.id.clearDatabaseButton).setOnClickListener(v -> {
            dialog.dismiss();
            showClearDatabaseConfirmation();
        });

        dialogView.findViewById(R.id.signOutButton).setOnClickListener(v -> {
            driveService.signOut();
            Toast.makeText(this, "Signed out from Google Drive", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performBackup() {
        Log.d(TAG, "performBackup called");
        Log.d(TAG, "OAuth configured: " + driveService.isOAuthConfigured());
        Log.d(TAG, "Currently signed in: " + driveService.isSignedIn());

        if (!driveService.isSignedIn()) {
            Log.d(TAG, "Not signed in, starting sign-in flow");
            driveService.signIn(this, new GoogleDriveService.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Auth callback success, starting backup");
                    startBackup();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Auth callback error: " + error);
                    Toast.makeText(MainActivity.this, "Sign in failed: " + error, Toast.LENGTH_LONG).show();
                }
            }, signInLauncher);
        } else {
            Log.d(TAG, "Already signed in, starting backup directly");
            startBackup();
        }
    }

    private void startBackup() {
        File databaseFile = getDatabasePath("pack");
        if (!databaseFile.exists()) {
            Toast.makeText(this, "No database found to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        driveService.backupDatabase(databaseFile, new GoogleDriveService.BackupCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Backup failed: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void performRestore() {
        Log.d(TAG, "performRestore called");
        Log.d(TAG, "OAuth configured: " + driveService.isOAuthConfigured());
        Log.d(TAG, "Currently signed in: " + driveService.isSignedIn());

        if (!driveService.isSignedIn()) {
            Log.d(TAG, "Not signed in, starting sign-in flow");
            driveService.signIn(this, new GoogleDriveService.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Auth callback success, showing backup selection");
                    showBackupSelectionDialog();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Auth callback error: " + error);
                    Toast.makeText(MainActivity.this, "Sign in failed: " + error, Toast.LENGTH_LONG).show();
                }
            }, signInLauncher);
        } else {
            Log.d(TAG, "Already signed in, showing backup selection directly");
            showBackupSelectionDialogWithAuthRetry();
        }
    }

    private void showBackupSelectionDialog() {
        showBackupSelectionDialogWithAuthRetry();
    }

    private void showBackupSelectionDialogWithAuthRetry() {
        // Dismiss any existing backup dialog first
        if (currentBackupDialog != null && currentBackupDialog.isShowing()) {
            currentBackupDialog.dismiss();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup_selection, null);
        currentBackupDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        AlertDialog dialog = currentBackupDialog;

        Spinner backupSpinner = dialogView.findViewById(R.id.backupSpinner);

        driveService.listBackups(new GoogleDriveService.RestoreCallback() {
            @Override
            public void onBackupListReady(List<GoogleDriveService.CloudBackup> backups) {
                runOnUiThread(() -> {
                    dialogView.findViewById(R.id.loadingText).setVisibility(View.GONE);
                    backupSpinner.setVisibility(View.VISIBLE);

                    if (backups.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No backups found", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    ArrayAdapter<GoogleDriveService.CloudBackup> adapter =
                            new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, backups);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    backupSpinner.setAdapter(adapter);

                    dialogView.findViewById(R.id.restoreButton).setEnabled(true);
                });
            }

            @Override
            public void onRestoreProgress(String message) {
                // Will be used in restore operation
            }

            @Override
            public void onRestoreSuccess(String message) {
                // Will be used in restore operation
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.w(TAG, "Failed to load backups, might be auth issue: " + error);

                    // Check if this might be an authentication issue
                    if (error.contains("auth") || error.contains("401") || error.contains("403") ||
                        error.contains("unauthorized") || error.contains("forbidden") ||
                        error.contains("credentials") || error.contains("token") ||
                        error.contains("Not authenticated")) {

                        // Dismiss the loading dialog first
                        dialog.dismiss();

                        // Use a short delay to ensure the dialog is properly dismissed
                        // before showing the re-authentication dialog
                        dialog.getWindow().getDecorView().postDelayed(() -> {
                            // Show re-authentication dialog
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Authentication Required")
                                    .setMessage("Your Google Drive session has expired. Would you like to sign in again?")
                                    .setPositiveButton("Sign In", (authDialog, which) -> {
                                        Log.d(TAG, "User chose to re-authenticate");
                                        driveService.signOut(); // Clear old session

                                        // Sign in and then show a fresh backup selection dialog
                                        driveService.signIn(MainActivity.this, new GoogleDriveService.AuthCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Re-authentication successful, showing fresh backup dialog");
                                                // This will dismiss any existing dialog and create a fresh one
                                                showBackupSelectionDialogWithAuthRetry();
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Re-authentication failed: " + error);
                                                Toast.makeText(MainActivity.this, "Sign in failed: " + error, Toast.LENGTH_LONG).show();
                                            }
                                        }, signInLauncher);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }, 100); // 100ms delay to ensure proper UI state

                    } else {
                        Toast.makeText(MainActivity.this, "Failed to load backups: " + error, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });
            }
        });

        dialogView.findViewById(R.id.restoreButton).setOnClickListener(v -> {
            GoogleDriveService.CloudBackup selected = (GoogleDriveService.CloudBackup) backupSpinner.getSelectedItem();
            if (selected != null) {
                dialog.dismiss();
                restoreFromBackup(selected);
            }
        });

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void restoreFromBackup(GoogleDriveService.CloudBackup backup) {
        File databaseFile = getDatabasePath("pack");

        driveService.restoreDatabase(backup.id, databaseFile, new GoogleDriveService.RestoreCallback() {
            @Override
            public void onBackupListReady(List<GoogleDriveService.CloudBackup> backups) {
                // Not used in restore
            }

            @Override
            public void onRestoreProgress(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onRestoreSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message + "\nPlease restart the app to see restored data.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Restore failed: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void importDatabaseFile() {
        Log.d(TAG, "importDatabaseFile called");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Database File"));
    }

    // onActivityResult removed - now using ActivityResultLauncher

    private void handleFileImport(Intent data) {
        try {
            android.net.Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Copy the selected file to a temporary location
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Cannot read selected file", Toast.LENGTH_SHORT).show();
                return;
            }

            File tempFile = new File(getCacheDir(), "temp_database.db");
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            // Validate that it's a SQLite database
            if (!isValidSQLiteDatabase(tempFile)) {
                Toast.makeText(this, "Selected file is not a valid SQLite database", Toast.LENGTH_LONG).show();
                tempFile.delete();
                return;
            }

            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Import Database")
                    .setMessage("This will replace your current data with the imported database. Continue?")
                    .setPositiveButton("Import", (dialog, which) -> {
                        importDatabase(tempFile);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        tempFile.delete();
                    })
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error importing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidSQLiteDatabase(File file) {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            // Check if it has the expected tables
            android.database.Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('list', 'item', 'trip')", null);

            boolean hasRequiredTables = cursor.getCount() >= 3; // Should have at least list, item, and trip tables
            cursor.close();
            db.close();

            return hasRequiredTables;
        } catch (Exception e) {
            return false;
        }
    }

    private void importDatabase(File sourceFile) {
        try {
            // Get the current database file
            File currentDb = getDatabasePath("pack");

            // Close any existing database connections
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.close();

            // Copy the source file to replace the current database
            java.io.FileInputStream inputStream = new java.io.FileInputStream(sourceFile);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(currentDb);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();
            sourceFile.delete(); // Clean up temp file

            Toast.makeText(this, "Database imported successfully!\nPlease restart the app to see your data.",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to import database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showClearDatabaseConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Database")
                .setMessage("⚠️ This will permanently delete ALL your data:\n\n• All packing lists\n• All trips\n• All items\n\nThis action cannot be undone!\n\nAre you sure you want to continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("DELETE ALL DATA", (dialog, which) -> {
                    clearDatabase();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearDatabase() {
        try {
            Log.d(TAG, "Clearing database");

            // Close any existing database connections
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.close();

            // Delete the database file entirely
            File databaseFile = getDatabasePath("pack");
            boolean deleted = deleteDatabase("pack");

            if (deleted) {
                Log.d(TAG, "Database file deleted successfully");
                Toast.makeText(this, "Database cleared successfully!\nAll your data has been deleted.",
                        Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Database file deletion returned false, but no exception");
                Toast.makeText(this, "Database cleared (file deletion returned false but no error)",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to clear database", e);
            Toast.makeText(this, "Failed to clear database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
