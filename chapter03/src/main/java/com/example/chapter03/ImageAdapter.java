package com.example.chapter03;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<String> imagePaths;
    private Context context;
    private boolean isPreview;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String imagePath);
    }

    public ImageAdapter(Context context, List<String> imagePaths, boolean isPreview, OnImageClickListener listener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.isPreview = isPreview;
        this.listener = listener;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String path = imagePaths.get(position);
        // 使用Glide加载图片
        Glide.with(context)
                .load(path)
                .override(isPreview ? 200 : 100) // 预览大图/缩略图
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(path);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}