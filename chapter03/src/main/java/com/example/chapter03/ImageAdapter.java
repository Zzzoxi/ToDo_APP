package com.example.chapter03;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private Context context;
    private List<String> imagePaths;
    private boolean isEditable;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String imagePath); // 点击查看大图
        void onDeleteClick(int position); // 删除图片
    }

    public ImageAdapter(Context context, List<String> imagePaths, boolean isEditable, OnImageClickListener listener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.isEditable = isEditable;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);

        // 加载缩略图
        Glide.with(context)
                .load(imagePath)
                .centerCrop()
                .into(holder.imageView);

        // 设置点击事件
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imagePath);
            }
        });

        // 显示或隐藏删除按钮
        if (isEditable) {
            holder.buttonDelete.setVisibility(View.VISIBLE);
            holder.buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(position);
                }
            });
        } else {
            holder.buttonDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton buttonDelete;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}