package com.example.chapter03;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.text.SimpleDateFormat;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import android.app.Dialog;
import android.widget.ImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ImageButton;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity implements TodoAdapter.TodoItemListener {
    private static final int PICK_IMAGES_REQUEST = 1;   // 图片选择请求码
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int EDIT_TODO_REQUEST = 3;
    private List<Todo> todoList;   // 待办事项列表
    private TodoAdapter adapter;
    private TodoDbHelper dbHelper;
    private EditText editTextTodo;         // 输入待办事项的 EditText
    private TextView textViewDueDate;     // 显示截止时间的 TextView
    private LinearLayout dropdownPanel;   // 下拉面板
    private long selectedDueTime = 0;       // 选中的截止时间
    private List<String> selectedImagePaths = new ArrayList<>();   // 选中的图片路径
    private RecyclerView recyclerViewImages;  // 图片预览的 RecyclerView
    private ImageAdapter imageAdapter;  // 图片适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化存储
        dbHelper = new TodoDbHelper(this);
        todoList = new ArrayList<>(); // 确保初始化列表
        todoList.addAll(dbHelper.getAllTodos());   // 从数据库加载所有待办事项

        // 初始化视图
        editTextTodo = findViewById(R.id.edit_text_todo);  // 获取输入框
        Button buttonAdd = findViewById(R.id.button_add);  // 获取添加按钮
        RecyclerView recyclerView = findViewById(R.id.recycler_view_todos);  // 获取待办事项列表中的recyclerView
        dropdownPanel = findViewById(R.id.dropdown_panel);   // 获取下拉面板
        textViewDueDate = findViewById(R.id.text_view_due_date);  // 获取截止时间的textview
        TextView textViewAddImages = findViewById(R.id.text_view_add_images);  // 获取选择时间的textview
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);  // 获取底部导航

        // 设置待办事项列表
        adapter = new TodoAdapter(todoList, this);   // 创建适配器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // 使用自定义的LayoutManager来优化性能
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        setupItemTouchHelper(recyclerView);

        // 设置图片预览列表
        recyclerViewImages = findViewById(R.id.recycler_view_images);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, selectedImagePaths, true,
                new ImageAdapter.OnImageClickListener() {
                    @Override
                    public void onImageClick(String imagePath) {
                        showFullImage(imagePath);
                    }

                    @Override
                    public void onDeleteClick(int position) {
                        selectedImagePaths.remove(position);
                        imageAdapter.notifyItemRemoved(position);
                    }
                });
        recyclerViewImages.setAdapter(imageAdapter);

        // 设置输入框点击事件，显示下拉面板
        editTextTodo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dropdownPanel.setVisibility(View.VISIBLE);
            }
        });

        // 设置底部导航
        bottomNavigation.setSelectedItemId(R.id.navigation_todo);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_completed) {
                Intent intent = new Intent(MainActivity.this, CompletedActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        // 设置日期选择
        textViewDueDate.setOnClickListener(v -> showDateTimePicker());

        // 设置图片选择
        textViewAddImages.setOnClickListener(v -> openImagePicker());

        // 添加新待办事项
        buttonAdd.setOnClickListener(v -> {
            String todoText = editTextTodo.getText().toString().trim();   // 获取输入的待办
            if (!todoText.isEmpty()) {
                Todo newTodo = new Todo(todoText, todoList.size());  // 创建新待办todo
                newTodo.setDueTime(selectedDueTime);  // 设置待办截止时间
                for (String path : selectedImagePaths) {   // 设置待办相关图片
                    newTodo.addImagePath(path);
                }
                todoList.add(newTodo);   // 在总待办中添加新待办事项
                adapter.notifyItemInserted(todoList.size() - 1);  // 更新视图
                editTextTodo.setText("");  // 重置输入文本框
                dbHelper.saveTodo(newTodo);  // 将新待办事项写进数据库

                // 重置所有状态，准备下一条新待办
                selectedDueTime = 0;
                selectedImagePaths.clear();
                imageAdapter.notifyDataSetChanged();
                dropdownPanel.setVisibility(View.GONE);
                textViewDueDate.setText("输入截止时间（可选）");
                editTextTodo.clearFocus();
            }
        });

        // 用户点击面板外部内容，将自清空并收缩下拉面板
        View rootView = findViewById(android.R.id.content);  // 获取根视图
        rootView.setOnClickListener(v -> {
            if (dropdownPanel.getVisibility() == View.VISIBLE) {
                clearAndCloseDropdown();  // 如果下拉面板可见，清空并关闭面板
            }
        });

        // 防止点击下拉面板内的内容时关闭面板
        dropdownPanel.setOnClickListener(v -> {
            // 消费点击事件，防止传递到根视图
        });

        // 设置返回按钮点击事件
        ImageButton buttonCollapse = findViewById(R.id.button_collapse);
        buttonCollapse.setOnClickListener(v -> clearAndCloseDropdown());  // 点击返回按钮时清空并关闭面板
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保返回时选中正确的标签
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_todo);

        // 重新加载未完成事项
        todoList.clear(); // 清空当前列表
        todoList.addAll(dbHelper.getAllTodos()); // 从数据库重新加载未完成事项
        adapter.notifyDataSetChanged(); // 通知适配器更新
    }

    // 显示日期选择器（年月日）
    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();  // 获取当前时间
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    showTimePicker(year, month, dayOfMonth);   // 选择日期后显示时间选择器
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();  // 显示日期选择器
    }

    // 时间选择器（时分）
    private void showTimePicker(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();  // 获取当前时间
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(year, month, day, hourOfDay, minute);  // 设置选择的时间
                    selectedDueTime = calendar.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    textViewDueDate.setText("截止时间：" + sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void openImagePicker() {
        if (selectedImagePaths.size() >= 5) {
            Toast.makeText(this, "最多只能添加5张图片", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上使用新的权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        } else {
            // Android 13以下使用旧的权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // 启动图片选择器
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGES_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                // 多选图片的情况
                int count = Math.min(data.getClipData().getItemCount(), 5 - selectedImagePaths.size());
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    String path = getRealPathFromUri(imageUri);
                    if (path != null) {
                        selectedImagePaths.add(path); // 添加选中图片的路径
                    }
                }
            } else if (data.getData() != null) {
                // 单选图片的情况
                Uri imageUri = data.getData();
                String path = getRealPathFromUri(imageUri);
                if (path != null) {
                    selectedImagePaths.add(path);
                }
            }
            recyclerViewImages.setVisibility(View.VISIBLE);   // 显示图片预览
            imageAdapter.notifyDataSetChanged();  // 通知适配器更新
        }

        // 如果选择编辑待办
        if (requestCode == EDIT_TODO_REQUEST && resultCode == RESULT_OK) {
            // 刷新列表
            todoList.clear(); // 清空当前列表
            todoList.addAll(dbHelper.getAllTodos()); // 重新加载所有待办
            adapter.notifyDataSetChanged(); // 通知适配器数据已更新
        }

    }

    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);  // 返回图片的真实路径
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;  // 返回 null 如果未找到路径
    }

    private void showFullImage(String imagePath) {
        // 创建一个对话框显示大图
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        Glide.with(this)
                .load(imagePath)
                .into(imageView);  // 使用 Glide 加载图片
        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());  // 点击图片关闭对话框
        dialog.show();
    }

    // 待办事项完成
    @Override
    public void onItemChecked(int position, boolean isChecked) {
        if (isChecked) { // 如果待办事项被勾选
            Todo todo = todoList.get(position);
            todo.setCompleted(true); // 设置为已完成
            todo.setCompletedTime(System.currentTimeMillis()); // 记录完成时间
            dbHelper.updateTodo(todo); // 更新数据库
            todoList.remove(position); // 从列表中移除
            adapter.notifyItemRemoved(position); // 通知适配器更新
        }
    }

    @Override
    public void onDeleteItem(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除待办")
                .setMessage("确定要删除这个待办事项吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    Todo todo = todoList.get(position);
                    dbHelper.deleteTodo(todo.getId()); // 从数据库中删除
                    todoList.remove(position); // 从列表中移除
                    adapter.notifyItemRemoved(position); // 通知适配器更新
                })
                .setNegativeButton("取消", null)
                .show(); // 显示确认对话框
    }

    @Override
    public void onEditItem(int position) {
        Todo todo = todoList.get(position);
        Intent intent = new Intent(this, EditTodoActivity.class);
        intent.putExtra("todo_id", todo.getId());
        startActivityForResult(intent, EDIT_TODO_REQUEST);
    }

    private void clearAndCloseDropdown() {
        // 清空输入
        editTextTodo.setText("");
        editTextTodo.clearFocus();

        // 清空选择的时间
        selectedDueTime = 0;
        textViewDueDate.setText("输入截止时间（可选）");

        // 清空选择的图片
        selectedImagePaths.clear();
        imageAdapter.notifyDataSetChanged();

        // 隐藏面板
        dropdownPanel.setVisibility(View.GONE);

        // 隐藏软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTextTodo.getWindowToken(), 0);
    }

    // 实现拖动排序
    // 允许用户通过简单的上下拖动来重新排列待办事项列表
    private void setupItemTouchHelper(RecyclerView recyclerView) {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) { // 只允许上下拖动，禁用左右滑动

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // 更新列表顺序
                Todo movedItem = todoList.get(fromPosition);
                todoList.remove(fromPosition);
                todoList.add(toPosition, movedItem);

                // 更新所有受影响项目的position
                for (int i = Math.min(fromPosition, toPosition);
                     i <= Math.max(fromPosition, toPosition); i++) {
                    Todo todo = todoList.get(i);
                    todo.setPosition(i);
                    dbHelper.updateTodo(todo);
                }

                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 不需要实现，因为禁用了滑动
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
}