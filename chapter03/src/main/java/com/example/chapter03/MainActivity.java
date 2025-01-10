package com.example.chapter03;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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
import java.util.Collections;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
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
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity implements TodoAdapter.TodoItemListener {
    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int EDIT_TODO_REQUEST = 3;
    private List<Todo> todoList;
    private TodoAdapter adapter;
    private TodoDbHelper dbHelper;
    private EditText editTextTodo;
    private TextView textViewDueDate;
    private LinearLayout dropdownPanel;
    private long selectedDueTime = 0;
    private List<String> selectedImagePaths = new ArrayList<>();
    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化存储
        dbHelper = new TodoDbHelper(this);
        todoList = new ArrayList<>(); // 确保初始化列表
        todoList.addAll(dbHelper.getAllTodos());

        // 初始化视图
        editTextTodo = findViewById(R.id.edit_text_todo);
        Button buttonAdd = findViewById(R.id.button_add);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_todos);
        dropdownPanel = findViewById(R.id.dropdown_panel);
        textViewDueDate = findViewById(R.id.text_view_due_date);
        TextView textViewAddImages = findViewById(R.id.text_view_add_images);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        // 设置待办事项列表
        adapter = new TodoAdapter(todoList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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
            String todoText = editTextTodo.getText().toString().trim();
            if (!todoText.isEmpty()) {
                Todo newTodo = new Todo(todoText, todoList.size());
                newTodo.setDueTime(selectedDueTime);
                for (String path : selectedImagePaths) {
                    newTodo.addImagePath(path);
                }
                todoList.add(newTodo);
                adapter.notifyItemInserted(todoList.size() - 1);
                editTextTodo.setText("");
                dbHelper.saveTodo(newTodo);

                // 重置所有状态
                selectedDueTime = 0;
                selectedImagePaths.clear();
                imageAdapter.notifyDataSetChanged();
                dropdownPanel.setVisibility(View.GONE);
                textViewDueDate.setText("输入截止时间（可选）");
                editTextTodo.clearFocus();
            }
        });

        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> {
            if (dropdownPanel.getVisibility() == View.VISIBLE) {
                clearAndCloseDropdown();
            }
        });

        // 防止点击下拉面板内的内容时关闭面板
        dropdownPanel.setOnClickListener(v -> {
            // 消费点击事件，防止传递到根视图
        });

        // 设置返回按钮点击事件
        ImageButton buttonCollapse = findViewById(R.id.button_collapse);
        buttonCollapse.setOnClickListener(v -> clearAndCloseDropdown());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保返回时选中正确的标签
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_todo);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    showTimePicker(year, month, dayOfMonth);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(year, month, day, hourOfDay, minute);
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
                        selectedImagePaths.add(path);
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
            recyclerViewImages.setVisibility(View.VISIBLE);
            imageAdapter.notifyDataSetChanged();
        }

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
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showFullImage(String imagePath) {
        // 创建一个对话框显示大图
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        Glide.with(this)
                .load(imagePath)
                .into(imageView);
        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onItemChecked(int position, boolean isChecked) {
        if (isChecked) {
            Todo todo = todoList.get(position);
            todo.setCompleted(true);
            todo.setCompletedTime(System.currentTimeMillis());
            dbHelper.updateTodo(todo);
            todoList.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }

    @Override
    public void onDeleteItem(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除待办")
                .setMessage("确定要删除这个待办事项吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    Todo todo = todoList.get(position);
                    dbHelper.deleteTodo(todo.getId());
                    todoList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton("取消", null)
                .show();
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
}