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
import android.widget.TextView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
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
import android.app.Dialog;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements TodoAdapter.TodoItemListener {
    private static final int PICK_IMAGES_REQUEST = 1;
    private List<Todo> todoList;
    private TodoAdapter adapter;
    private TodoDbHelper dbHelper;
    private EditText editTextTodo;
    private TextView textViewDueDate;
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
        todoList = dbHelper.getAllTodos();

        // 初始化视图
        editTextTodo = findViewById(R.id.edit_text_todo);
        Button buttonAdd = findViewById(R.id.button_add);

        Button buttonCompleted = findViewById(R.id.button_completed); // 这里报错是因为在布局文件activity_main.xml中没有定义id为button_completed的Button控件
        RecyclerView recyclerView = findViewById(R.id.recycler_view_todos);

        // 设置RecyclerView
        adapter = new TodoAdapter(todoList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 添加拖拽功能
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Collections.swap(todoList, fromPosition, toPosition);

                // 更新所有项目的位置
                for (int i = 0; i < todoList.size(); i++) {
                    Todo todo = todoList.get(i);
                    todo.setPosition(i);
                    dbHelper.updateTodo(todo);
                }

                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Todo todo = todoList.get(position);
                dbHelper.deleteTodo(todo.getId()); // 需要在TodoDbHelper中添加此方法
                todoList.remove(position);
                adapter.notifyItemRemoved(position);
            }

            // 添加滑动时的背景和图标
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);

                    // 绘制红色背景
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

                    // 绘制删除图标
                    Drawable icon = ContextCompat.getDrawable(MainActivity.this,
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

        // 添加新待办事项
        buttonAdd.setOnClickListener(v -> {
            String todoText = editTextTodo.getText().toString().trim();
            if (!todoText.isEmpty()) {
                Todo newTodo = new Todo(todoText, todoList.size());
                newTodo.setDueTime(selectedDueTime);
                // 添加选中的图片
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
                recyclerViewImages.setVisibility(View.GONE);
                textViewDueDate.setText("选择截止时间（可选）");
            }
        });

        buttonCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CompletedActivity.class);
            startActivity(intent);
        });

        textViewDueDate = findViewById(R.id.text_view_due_date);
        textViewDueDate.setOnClickListener(v -> showDateTimePicker());

        recyclerViewImages = findViewById(R.id.recycler_view_images);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, selectedImagePaths, true, this::showFullImage);
        recyclerViewImages.setAdapter(imageAdapter);

        Button buttonAddImages = findViewById(R.id.button_add_images);
        buttonAddImages.setOnClickListener(v -> openImagePicker());
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 5 - selectedImagePaths.size());
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    String path = getPathFromUri(imageUri);
                    if (path != null) {
                        selectedImagePaths.add(path);
                    }
                }
            } else if (data.getData() != null) {
                String path = getPathFromUri(data.getData());
                if (path != null) {
                    selectedImagePaths.add(path);
                }
            }
            recyclerViewImages.setVisibility(View.VISIBLE);
            imageAdapter.notifyDataSetChanged();
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
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
} 