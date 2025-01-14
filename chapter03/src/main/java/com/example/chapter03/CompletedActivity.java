package com.example.chapter03;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.core.content.ContextCompat;
import java.util.List;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CompletedActivity extends AppCompatActivity {
    private TodoDbHelper dbHelper;   // 数据库帮助类
    private CompletedAdapter adapter;   // 已完成事项适配器
    private RecyclerView recyclerView; // 显示已完成事项的 RecyclerView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);  // 设置已完成事项页面的布局

        dbHelper = new TodoDbHelper(this);  // 创建数据库帮助类实例
        List<Todo> completedList = dbHelper.getCompletedTodos();  // 从数据库获取已完成的待办事项

        recyclerView = findViewById(R.id.recycler_view_completed);

        adapter = new CompletedAdapter(completedList, new CompletedAdapter.TodoItemListener() {
            @Override
            public void onRestoreItem(int position) {
                // 恢复已完成的待办事项
                Todo todo = completedList.get(position);
                todo.setCompleted(false); // 设置为未完成
                dbHelper.updateTodo(todo); // 更新数据库
                completedList.remove(position); // 从列表中移除
                adapter.notifyItemRemoved(position); // 通知适配器更新
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);  // 设置适配器

        // 添加滑动删除功能
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition(); // 获取被滑动的项的位置
                Todo todo = completedList.get(position); // 获取待办事项

                // 弹出确认删除对话框
                new AlertDialog.Builder(CompletedActivity.this)
                        .setTitle("删除已完成待办")
                        .setMessage("确定要删除这个已完成待办事项吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            dbHelper.deleteTodo(todo.getId()); // 从数据库中删除
                            completedList.remove(position); // 从列表中移除
                            adapter.notifyItemRemoved(position); // 通知适配器更新
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // 取消删除，刷新该项
                        })
                        .show(); // 显示确认对话框
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);

                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

                    Drawable icon = ContextCompat.getDrawable(CompletedActivity.this,
                            android.R.drawable.ic_menu_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);

        // 设置底部导航
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_completed); // 设置当前选中项
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_todo) {
                finish();
                overridePendingTransition(0, 0); // 添加无动画切换
                return true;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保返回时选中正确的标签
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_completed);
    }
}