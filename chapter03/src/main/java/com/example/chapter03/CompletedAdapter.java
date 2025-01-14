package com.example.chapter03;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.bumptech.glide.Glide;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.CompletedViewHolder> {
    private List<Todo> completedTodoList;
    private SimpleDateFormat dateFormat;
    private TodoItemListener listener;

    public interface TodoItemListener {
        void onRestoreItem(int position);
    }

    public CompletedAdapter(List<Todo> completedTodoList, TodoItemListener listener) {
        this.completedTodoList = completedTodoList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public CompletedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed, parent, false);
        return new CompletedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedViewHolder holder, int position) {
        Todo todo = completedTodoList.get(position);
        holder.textViewTodo.setText(todo.getText());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.textViewCompletedTime.setText("完成时间：" + sdf.format(new Date(todo.getCompletedTime())));

        // 设置截止时间
        if (todo.getDueTime() > 0) {
            holder.textViewDueTime.setVisibility(View.VISIBLE);
            String dueTimeText = "截止时间：" + sdf.format(new Date(todo.getDueTime()));
            System.out.println(dueTimeText);
            holder.textViewDueTime.setText(dueTimeText);
        } else {
            holder.textViewDueTime.setVisibility(View.GONE);
        }

        // 设置图片适配器
        if (!todo.getImagePaths().isEmpty()) {
            holder.recyclerViewImages.setVisibility(View.VISIBLE);
            if (holder.recyclerViewImages.getLayoutManager() == null) {
                holder.recyclerViewImages.setLayoutManager(
                        new LinearLayoutManager(holder.itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL, false));
            }

            // 使用延迟加载
            holder.recyclerViewImages.post(() -> {
                ImageAdapter imageAdapter = new ImageAdapter(
                        holder.itemView.getContext(),
                        todo.getImagePaths(),
                        false,
                        new ImageAdapter.OnImageClickListener() {
                            @Override
                            public void onImageClick(String imagePath) {
                                showFullImage(holder.itemView.getContext(), imagePath);
                            }

                            @Override
                            public void onDeleteClick(int position) {
                                // 待办列表中的图片不可删除
                            }
                        });
                holder.recyclerViewImages.setAdapter(imageAdapter);
            });
        } else {
            holder.recyclerViewImages.setVisibility(View.GONE);
        }

        // 设置恢复按钮的点击事件
        holder.buttonRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreItem(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return completedTodoList.size();
    }

    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTodo;
        TextView textViewCompletedTime;
        TextView textViewDueTime;
        RecyclerView recyclerViewImages; // 图片 RecyclerView
        ImageButton buttonRestore;

        CompletedViewHolder(View itemView) {
            super(itemView);
            textViewTodo = itemView.findViewById(R.id.text_view_completed_todo);
            textViewCompletedTime = itemView.findViewById(R.id.text_view_completed_time);
            textViewDueTime = itemView.findViewById(R.id.text_view_due_time);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_todo_images); // 获取图片 RecyclerView
            buttonRestore = itemView.findViewById(R.id.button_restore);
        }
    }

    private void showFullImage(Context context, String imagePath) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_viewer, null);
        ImageView imageView = view.findViewById(R.id.image_view_full);
        ImageButton buttonClose = view.findViewById(R.id.button_close);

        Glide.with(context)
                .load(imagePath)
                .into(imageView);

        buttonClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }
}
