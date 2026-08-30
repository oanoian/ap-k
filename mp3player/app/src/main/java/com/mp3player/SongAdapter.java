package com.mp3player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    
    private List<String> songs;
    private OnSongClickListener listener;
    
    public interface OnSongClickListener {
        void onSongClick(String filePath);
    }
    
    public SongAdapter(List<String> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        String filePath = songs.get(position);
        File file = new File(filePath);
        
        holder.txtTitle.setText(file.getName());
        holder.txtPath.setText(file.getParent());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(filePath);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return songs.size();
    }
    
    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtPath;
        
        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtSongTitle);
            txtPath = itemView.findViewById(R.id.txtSongPath);
        }
    }
}
