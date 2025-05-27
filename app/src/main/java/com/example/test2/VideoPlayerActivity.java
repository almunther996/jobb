package com.example.test2;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import androidx.appcompat.widget.Toolbar;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Toolbar toolbar = findViewById(R.id.videoToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        VideoView videoView = findViewById(R.id.videoView);
        String videoPath = getIntent().getStringExtra("video_path");

        if (videoPath != null) {
            Uri videoUri = Uri.parse(videoPath);
            videoView.setVideoURI(videoUri);
            videoView.setMediaController(new MediaController(this));
            videoView.start();
        }
    }

}
