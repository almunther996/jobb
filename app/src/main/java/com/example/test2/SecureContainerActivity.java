package com.example.test2;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureContainerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<File> encryptedFiles = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_container);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Remove the default title (which shows the app name)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        recyclerView = findViewById(R.id.recyclerViewFiles);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new FileAdapter(encryptedFiles, this);
        recyclerView.setAdapter(adapter);

        loadEncryptedFiles();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        encryptAndSaveFile(fileUri);
                    }
                }
        );

        // ✅ FAB click listener
        FloatingActionButton fab = findViewById(R.id.fabAddFile);
        fab.setOnClickListener(v -> openFilePicker());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press here
            onBackPressed(); // or finish()
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Accept any file type, filter by MIME types below
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "image/*",         // All image types
                "application/pdf", // PDF files
                "video/*"          // All video types
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }


    private void encryptAndSaveFile(Uri fileUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Encrypting...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Step 1: Copy to temp file
                File originalFile = new File(getCacheDir(), "temp.jpg");
                try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                     FileOutputStream outputStream = new FileOutputStream(originalFile)) {

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                }

                // Step 2: Encrypt and save
                String extension = getFileExtensionFromUri(fileUri);

                if (extension.equals("pdf")){
                    extension = "12";
                } else if (extension.equals("mp4")) {
                    extension = "17";
                }
                else{
                    extension = "88";
                }
                File encryptedFile = new File(getFilesDir(), "enc_" + System.currentTimeMillis() + "." + extension + ".bin");
                EncryptionUtils.encryptFile(originalFile, encryptedFile);

                handler.post(() -> {
                    encryptedFiles.add(encryptedFile);
                    adapter.notifyItemInserted(encryptedFiles.size() - 1);
                    progressDialog.dismiss();
                    Toast.makeText(this, "File encrypted and saved", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

    private String getFileExtensionFromUri(Uri uri) {
        String extension = null;

        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));

        if (extension == null) {
            // fallback to file path-based guess
            String path = uri.getPath();
            if (path != null && path.contains(".")) {
                extension = path.substring(path.lastIndexOf(".") + 1);
            }
        }

        return extension != null ? extension : "bin"; // fallback
    }

    private String getOriginalExtension(String fileName) {
        // Example: enc_1716633091234.mp4.bin
        if (fileName.endsWith(".bin")) {
            int secondDot = fileName.lastIndexOf('.', fileName.lastIndexOf('.') - 1);
            int lastDot = fileName.lastIndexOf('.');
            if (secondDot != -1 && lastDot > secondDot) {
                return fileName.substring(secondDot + 1, lastDot); // e.g., "mp4"
            }
        }
        return "bin"; // fallback if structure doesn't match
    }





    private void loadEncryptedFiles() {
        File directory = getFilesDir();
        File[] files = directory.listFiles();
        if (files != null) {
            encryptedFiles.clear();
            for (File file : files) {
                encryptedFiles.add(file);
            }
            adapter.notifyDataSetChanged();
        }
    }
}