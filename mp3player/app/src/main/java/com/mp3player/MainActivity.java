package com.mp3player;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    static {
        // Load native C++ library
        System.loadLibrary("mp3_scanner");
    }
    
    // Native method declarations
    private native long nativeInit();
    private native void nativeDestroy(long handle);
    private native String[] nativeScanFolder(long handle, String path);
    private native String[] nativeSearch(long handle, String query);
    private native int nativeGetFileCount(long handle);
    private native void nativeBuildIndex(long handle);
    
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private Button btnPlay, btnPause, btnStop, btnScan, btnSearch;
    private TextView txtStatus, txtCurrentSong;
    private MediaPlayer mediaPlayer;
    private List<String> mp3Files;
    private long nativeHandle;
    private int currentPosition = 0;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
        
        // Initialize native scanner
        nativeHandle = nativeInit();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnScan = findViewById(R.id.btnScan);
        btnSearch = findViewById(R.id.btnSearch);
        txtStatus = findViewById(R.id.txtStatus);
        txtCurrentSong = findViewById(R.id.txtCurrentSong);
        
        mp3Files = new ArrayList<>();
        adapter = new SongAdapter(mp3Files, this::playSong);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        btnPlay.setOnClickListener(v -> playCurrentSong());
        btnPause.setOnClickListener(v -> pauseSong());
        btnStop.setOnClickListener(v -> stopSong());
        btnScan.setOnClickListener(v -> scanForMusic());
        btnSearch.setOnClickListener(v -> performSearch());
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 
                    PERMISSION_REQUEST_CODE);
            } else {
                scanForMusic();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            } else {
                scanForMusic();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                scanForMusic();
            } else {
                Toast.makeText(this, "Permission required to access music files", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void scanForMusic() {
        txtStatus.setText("Scanning for MP3 files...");
        
        new Thread(() -> {
            String musicPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String[] scannedFiles = nativeScanFolder(nativeHandle, musicPath);
            
            runOnUiThread(() -> {
                if (scannedFiles != null && scannedFiles.length > 0) {
                    mp3Files.clear();
                    for (String file : scannedFiles) {
                        mp3Files.add(file);
                    }
                    adapter.notifyDataSetChanged();
                    
                    // Build search index for faster queries
                    nativeBuildIndex(nativeHandle);
                    
                    txtStatus.setText(String.format("Found %d MP3 files", mp3Files.size()));
                } else {
                    txtStatus.setText("No MP3 files found. Check permissions.");
                }
            });
        }).start();
    }
    
    private void performSearch() {
        // Simple search implementation - could be enhanced with search dialog
        if (mp3Files.isEmpty()) {
            Toast.makeText(this, "No files to search", Toast.LENGTH_SHORT).show();
            return;
        }
        
        txtStatus.setText("Searching...");
        
        new Thread(() -> {
            String[] results = nativeSearch(nativeHandle, "");
            
            runOnUiThread(() -> {
                if (results != null) {
                    mp3Files.clear();
                    for (String file : results) {
                        mp3Files.add(file);
                    }
                    adapter.notifyDataSetChanged();
                    txtStatus.setText(String.format("Search complete: %d results", results.length));
                }
            });
        }).start();
    }
    
    private void playSong(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            txtCurrentSong.setText(new File(filePath).getName());
            txtStatus.setText("Playing...");
            
            mediaPlayer.setOnCompletionListener(mp -> {
                currentPosition++;
                if (currentPosition < mp3Files.size()) {
                    playSong(mp3Files.get(currentPosition));
                }
            });
            
        } catch (IOException e) {
            Toast.makeText(this, "Error playing file: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void playCurrentSong() {
        if (!mp3Files.isEmpty() && currentPosition < mp3Files.size()) {
            playSong(mp3Files.get(currentPosition));
        }
    }
    
    private void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            txtStatus.setText("Paused");
        }
    }
    
    private void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            txtStatus.setText("Stopped");
            txtCurrentSong.setText("");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSong();
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
        }
    }
}
