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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
    private final Map<String, File> decryptedFilesCache = new HashMap<>();


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
        holder.imageView.setImageResource(getIconForFile(file));

        holder.imageView.setOnLongClickListener(v -> {
            showContextMenu(holder, file);
            return true;
        });


        holder.imageView.setOnClickListener(null);
    }

    private int getIconForFile(File file) {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".bin")) {
            // Extract extension before .bin, e.g. file.12.bin or file.17.bin
            int secondDot = name.lastIndexOf('.', name.lastIndexOf('.') - 1);
            int lastDot = name.lastIndexOf('.');
            if (secondDot != -1 && lastDot > secondDot) {
                String ext = name.substring(secondDot + 1, lastDot);
                switch (ext) {
                    case "12": // PDF
                        return R.drawable.pdf_enc;  // Your PDF icon
                    case "17": // Video
                        return R.drawable.vid_enc; // Your video icon
                    case "88":
                        return R.drawable.img_enc;
                    default:
                        return R.drawable.ic_lock_try; // Default locked icon
                }
            }
        }

        // If file doesn't match pattern, show locked icon by default
        return R.drawable.ic_lock_try;
    }


    private void showContextMenu(FileViewHolder holder, File file) {
        PopupMenu popup = new PopupMenu(context, holder.imageView);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.file_context_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_open) {
                openFile(file, holder);
                return true;

            } else if (itemId == R.id.menu_share) {
                shareFile(file);
                return true;
//            } else if (itemId == R.id.menu_save) {
//                saveFileToExternal(file);
//                return true;
            } else if (itemId == R.id.menu_delete) {
                deleteFile(file, holder);
                return true;
            }
            else if (itemId == R.id.menu_info) {
                showFileInfoDialog(file);
                return true;
            }


            return false;
        });


        popup.show();
    }

    private void showFileInfoDialog(File file) {
        String fileName = file.getName();
        long fileSize = file.length(); // in bytes
        long lastModified = file.lastModified();

        String type = "Unknown";
        if (fileName.contains(".12.")) type = "PDF";
        else if (fileName.contains(".17.")) type = "Video";
        else if (fileName.contains(".88.")) type = "Image";

        String status = decryptedFilesCache.containsKey(file.getAbsolutePath()) ? "Decrypted" : "Encrypted";

        String message = "Name: " + fileName +
                "\nType: " + type +
                "\nSize: " + readableFileSize(fileSize) +
                "\nModified: " + android.text.format.DateFormat.format("yyyy-MM-dd hh:mm a", lastModified) +
                "\nStatus: " + status;

        new AlertDialog.Builder(context)
                .setTitle("File Info")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }



    private void openFile(File file, FileViewHolder holder) {
        // Check if decrypted file is cached
        if (decryptedFilesCache.containsKey(file.getAbsolutePath())) {
            File decryptedFile = decryptedFilesCache.get(file.getAbsolutePath());
            openDecryptedFile(decryptedFile, file, holder);
            return;
        }

        // Else proceed with decrypting
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

                decryptedFilesCache.put(file.getAbsolutePath(), tempDecryptedFile);

                File finalTempDecryptedFile = tempDecryptedFile;
                String finalExtension = extension;

                handler.post(() -> {
                    progressDialog.dismiss();
                    openDecryptedFile(finalTempDecryptedFile, file, holder);
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
    }

    private void openDecryptedFile(File decryptedFile, File originalFile, FileViewHolder holder) {
        String fileName = originalFile.getName().toLowerCase();
        String extension = "bin";
        if (fileName.endsWith(".bin")) {
            int secondDot = fileName.lastIndexOf('.', fileName.lastIndexOf('.') - 1);
            int lastDot = fileName.lastIndexOf('.');
            if (secondDot != -1 && lastDot > secondDot) {
                extension = fileName.substring(secondDot + 1, lastDot);
            }
        }

        if (extension.equals("17")) {

            holder.imageView.setImageResource(R.drawable.vid_dec);
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("video_path", decryptedFile.getAbsolutePath());
            context.startActivity(intent);
        } else if (extension.equals("12")) {
            holder.imageView.setImageResource(R.drawable.pdf_dec);
            try {
                Uri pdfUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        decryptedFile
                );
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }
        } else {
            holder.imageView.setImageResource(R.drawable.img_dec);
            Intent intent = new Intent(context, ImageViewerActivity.class);
            intent.putExtra("image_path", decryptedFile.getAbsolutePath());
            context.startActivity(intent);
        }
    }



    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share File"));
        } catch (Exception e) {
            Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show();
        }
    }

//    private void saveFileToExternal(File file) {
//        Toast.makeText(context, "Save not implemented yet", Toast.LENGTH_SHORT).show();
//        // TODO: Implement saving to external storage
//    }

    private void deleteFile(File file, FileViewHolder holder) {
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
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewEncrypted);
        }
    }
}
