package com.example.chapter03;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private List<Todo> todoList;
    private TodoItemListener listener;

    public interface TodoItemListener {
        void onItemChecked(int position, boolean isChecked);
        void onDeleteItem(int position);
        void onEditItem(int position);
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

        // 设置截止时间
        if (todo.getDueTime() > 0) {
            holder.textViewDueTime.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String dueTimeText = "截止时间：" + sdf.format(new Date(todo.getDueTime()));
            holder.textViewDueTime.setText(dueTimeText);
        } else {
            holder.textViewDueTime.setVisibility(View.GONE);
        }

        // 设置图片
        if (!todo.getImagePaths().isEmpty()) {
            holder.recyclerViewImages.setVisibility(View.VISIBLE);
            holder.recyclerViewImages.setLayoutManager(
                    new LinearLayoutManager(holder.itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
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
        } else {
            holder.recyclerViewImages.setVisibility(View.GONE);
        }

        holder.checkBoxTodo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onItemChecked(position, isChecked);
            }
        });

        // 设置更多选项按钮
        holder.buttonMore.setOnClickListener(v -> showPopupMenu(v, position));
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "编辑");
        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "删除");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (listener != null) {
                    listener.onEditItem(position);
                }
                return true;
            } else if (item.getItemId() == 2) {
                if (listener != null) {
                    listener.onDeleteItem(position);
                }
                return true;
            }
            return false;
        });

        popup.show();
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

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxTodo;
        TextView textViewTodo;
        TextView textViewDueTime;
        RecyclerView recyclerViewImages;
        ImageButton buttonMore;

        TodoViewHolder(View itemView) {
            super(itemView);
            checkBoxTodo = itemView.findViewById(R.id.checkbox_todo);
            textViewTodo = itemView.findViewById(R.id.text_view_todo);
            textViewDueTime = itemView.findViewById(R.id.text_view_due_time);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_todo_images);
            buttonMore = itemView.findViewById(R.id.button_more);
        }
    }
}