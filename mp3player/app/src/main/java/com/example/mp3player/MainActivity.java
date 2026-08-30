package com.example.mp3player;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ListView listView;
    private Button btnPlayPause;
    private Button btnStop;
    private TextView txtCurrentSong;
    
    private MediaPlayer mediaPlayer;
    private List<File> mp3Files;
    private ArrayAdapter<String> adapter;
    private int currentPosition = 0;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        listView = findViewById(R.id.listView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        txtCurrentSong = findViewById(R.id.txtCurrentSong);
        
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    pauseMusic();
                } else {
                    playMusic();
                }
            }
        });
        
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMusic();
            }
        });
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentPosition = position;
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                playMusic();
            }
        });
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMp3Files();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMp3Files();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMp3Files();
            } else {
                Toast.makeText(this, "Permission required to access music files", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void loadMp3Files() {
        mp3Files = new ArrayList<>();
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        
        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                        mp3Files.add(file);
                    }
                }
            }
        }
        
        // Also check Downloads folder
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                        if (!mp3Files.contains(file)) {
                            mp3Files.add(file);
                        }
                    }
                }
            }
        }
        
        List<String> fileNames = new ArrayList<>();
        for (File file : mp3Files) {
            fileNames.add(file.getName());
        }
        
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);
        
        if (mp3Files.isEmpty()) {
            Toast.makeText(this, "No MP3 files found. Add files to Music or Downloads folder.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void playMusic() {
        if (mp3Files.isEmpty()) {
            Toast.makeText(this, "No songs available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mp3Files.get(currentPosition).getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setText("Pause");
            txtCurrentSong.setText("Playing: " + mp3Files.get(currentPosition).getName());
            
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPosition = (currentPosition + 1) % mp3Files.size();
                    playMusic();
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing file", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void pauseMusic() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setText("Play");
        }
    }
    
    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying = false;
            btnPlayPause.setText("Play");
            txtCurrentSong.setText("Stopped");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
