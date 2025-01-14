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
    // 创建一个新的视图缓存组件
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    // 将待办事项的数据绑定到视图上，负责更新视图的状态和内容
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todoList.get(position);  //根据传入的位置获取当前的待办事项对象
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

        // 优化图片加载
        if (!todo.getImagePaths().isEmpty()) {    // 有图片
            holder.recyclerViewImages.setVisibility(View.VISIBLE);
            if (holder.recyclerViewImages.getLayoutManager() == null) {
                holder.recyclerViewImages.setLayoutManager(
                        new LinearLayoutManager(holder.itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL, false));
            }

            // 使用延迟加载
            // 使用 post 方法延迟加载图片，以确保视图已经完全绘制
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

        // 为复选框设置状态变化监听器，待办事项是否完成
        holder.checkBoxTodo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onItemChecked(position, isChecked);
            }
        });

        // 设置更多选项按钮
        holder.buttonMore.setOnClickListener(v -> showPopupMenu(v, position));
    }

    private void showPopupMenu(View view, int position) {
        // 创建一个 PopupMenu 对象，指定上下文和锚点视图
        PopupMenu popup = new PopupMenu(view.getContext(), view);

        // 向菜单中添加“编辑”和“删除”选项
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "编辑");
        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "删除");

        // 设置菜单项点击事件的监听器
        popup.setOnMenuItemClickListener(item -> {
            // 检查点击的菜单项 ID
            if (item.getItemId() == 1) {
                // 如果点击的是“编辑”，调用监听器的 onEditItem 方法
                if (listener != null) {
                    listener.onEditItem(position);
                }
                return true;
            } else if (item.getItemId() == 2) {
                // 如果点击的是“删除”，调用监听器的 onDeleteItem 方法
                if (listener != null) {
                    listener.onDeleteItem(position);
                }
                return true;
            }
            return false;
        });

        // 显示弹出菜单
        popup.show();
    }

    // 展示全屏图片
    private void showFullImage(Context context, String imagePath) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);   // 创建全屏对话框
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_viewer, null);  // 加载布局
        ImageView imageView = view.findViewById(R.id.image_view_full);
        ImageButton buttonClose = view.findViewById(R.id.button_close);

        Glide.with(context)  // 加载图片
                .load(imagePath)
                .into(imageView);

        buttonClose.setOnClickListener(v -> dialog.dismiss());  // 为关闭按钮设置点击事件，点击时关闭对话框
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public int getItemCount() {   // 返回待办事项列表中项目的数量
        return todoList.size();
    }

    @Override
    public void onViewRecycled(@NonNull TodoViewHolder holder) {   // 在视图被回收时清理资源，以避免内存泄漏
        super.onViewRecycled(holder);
        if (holder.recyclerViewImages != null) {
            holder.recyclerViewImages.setAdapter(null);
        }
    }

    // 定义一个视图持有者类，用于缓存待办事项的视图组件
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