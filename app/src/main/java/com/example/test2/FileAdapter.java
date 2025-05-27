package com.example.test2;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<File> fileList;
    private final Context context;
    private final Map<String, File> decryptedPdfFiles = new HashMap<>();




    public FileAdapter(List<File> fileList, Context context) {
        this.fileList = fileList;
        this.context = context;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.imageView.setImageResource(R.drawable.ic_lock_try);
        holder.buttonDecrypt.setVisibility(View.VISIBLE);

        holder.imageView.setOnClickListener(null); // reset click listener

        holder.buttonDecrypt.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Decrypting...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                try {
                    byte[] decryptedBytes = EncryptionUtils.decryptFile(file);

                    String fileName = file.getName().toLowerCase();
                    String extension = "bin";
                    if (fileName.endsWith(".bin")) {
                        int secondDot = fileName.lastIndexOf('.', fileName.lastIndexOf('.') - 1);
                        int lastDot = fileName.lastIndexOf('.');
                        if (secondDot != -1 && lastDot > secondDot) {
                            extension = fileName.substring(secondDot + 1, lastDot);
                        }
                    }

                    File tempDecryptedFile = new File(context.getCacheDir(), "dec_" + System.currentTimeMillis());

                    try (FileOutputStream fos = new FileOutputStream(tempDecryptedFile)) {
                        fos.write(decryptedBytes);
                    }

                    File finalTempDecryptedFile = tempDecryptedFile;
                    String finalExtension = extension;

                    handler.post(() -> {
                        progressDialog.dismiss();

                        if (finalExtension.equals("17")) {
                            Intent intent = new Intent(context, VideoPlayerActivity.class);
                            intent.putExtra("video_path", finalTempDecryptedFile.getAbsolutePath());
                            context.startActivity(intent);
                        } else if (finalExtension.equals("12")) {
                            holder.imageView.setImageResource(R.drawable.ic_pdf_placeholder);
                            holder.buttonDecrypt.setVisibility(View.GONE);
                            decryptedPdfFiles.put(file.getAbsolutePath(), finalTempDecryptedFile);

                            holder.imageView.setOnClickListener(view -> {
                                try {
                                    Uri pdfUri = FileProvider.getUriForFile(
                                            context,
                                            context.getPackageName() + ".provider",
                                            finalTempDecryptedFile
                                    );

                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(pdfUri, "application/pdf");
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.length);
                            holder.imageView.setImageBitmap(bitmap);
                            holder.buttonDecrypt.setVisibility(View.GONE);
                        }

                        Toast.makeText(context, "File decrypted", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to decrypt", Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            });
        });

        holder.buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete File")
                    .setMessage("Are you sure you want to delete this file?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        boolean deleted = file.delete();
                        if (deleted) {
                            int index = holder.getAdapterPosition();
                            fileList.remove(index);
                            notifyItemRemoved(index);
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton buttonDecrypt;
        ImageButton buttonDelete;
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewEncrypted);
            buttonDecrypt = itemView.findViewById(R.id.buttonDecrypt);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);

        }
    }
}
