package com.example.chapter03;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Context;
import android.app.Dialog;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import androidx.recyclerview.widget.LinearLayoutManager;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private List<Todo> todoList;
    private TodoItemListener listener;

    public interface TodoItemListener {
        void onItemChecked(int position, boolean isChecked);
    }

    public TodoAdapter(List<Todo> todoList, TodoItemListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.textViewTodo.setText(todo.getText());
        holder.checkBoxTodo.setChecked(todo.isCompleted());

        if (todo.getDueTime() > 0) {
            holder.textViewDueTime.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("截止时间：yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.textViewDueTime.setText(sdf.format(new Date(todo.getDueTime())));
        } else {
            holder.textViewDueTime.setVisibility(View.GONE);
        }

        holder.checkBoxTodo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onItemChecked(position, isChecked);
            }
        });

        if (!todo.getImagePaths().isEmpty()) {
            holder.recyclerViewImages.setVisibility(View.VISIBLE);
            holder.recyclerViewImages.setLayoutManager(
                    new LinearLayoutManager(holder.itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
            ImageAdapter imageAdapter = new ImageAdapter(
                    holder.itemView.getContext(),
                    todo.getImagePaths(),
                    false,
                    path -> showFullImage(holder.itemView.getContext(), path));
            holder.recyclerViewImages.setAdapter(imageAdapter);
        } else {
            holder.recyclerViewImages.setVisibility(View.GONE);
        }
    }

    private void showFullImage(Context context, String imagePath) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(context);
        Glide.with(context)
                .load(imagePath)
                .into(imageView);
        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxTodo;
        TextView textViewTodo;
        TextView textViewDueTime;
        RecyclerView recyclerViewImages;

        TodoViewHolder(View itemView) {
            super(itemView);
            checkBoxTodo = itemView.findViewById(R.id.checkbox_todo);
            textViewTodo = itemView.findViewById(R.id.text_view_todo);
            textViewDueTime = itemView.findViewById(R.id.text_view_due_time);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_todo_images);
        }
    }
}