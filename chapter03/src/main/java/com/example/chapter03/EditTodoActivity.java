
package com.example.chapter03;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class EditTodoActivity extends AppCompatActivity {
    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private EditText editTextTodo;
    private TextView textViewDueDate;
    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private TodoDbHelper dbHelper;
    private long selectedDueTime;
    private List<String> selectedImagePaths;
    private Todo currentTodo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_todo);

        dbHelper = new TodoDbHelper(this);

        // 获取传递过来的Todo对象
        long todoId = getIntent().getLongExtra("todo_id", -1);
        if (todoId == -1) {
            finish();
            return;
        }
        currentTodo = dbHelper.getTodoById(todoId);
        if (currentTodo == null) {
            finish();
            return;
        }
        selectedImagePaths = new ArrayList<>(currentTodo.getImagePaths());
        selectedDueTime = currentTodo.getDueTime();

        // 初始化视图
        editTextTodo = findViewById(R.id.edit_text_todo);
        textViewDueDate = findViewById(R.id.text_view_due_date);
        recyclerViewImages = findViewById(R.id.recycler_view_images);
        ImageButton buttonBack = findViewById(R.id.button_back);
        Button buttonSave = findViewById(R.id.button_save);
        TextView textViewAddImages = findViewById(R.id.text_view_add_images);

        // 设置当前数据
        editTextTodo.setText(currentTodo.getText());
        if (selectedDueTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            textViewDueDate.setText("截止时间：" + sdf.format(new Date(selectedDueTime)));
        }

        // 设置图片列表
        recyclerViewImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, selectedImagePaths, true, null);
        recyclerViewImages.setAdapter(imageAdapter);

        // 设置点击事件
        buttonBack.setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveTodo());
        textViewDueDate.setOnClickListener(v -> showDateTimePicker());
        textViewAddImages.setOnClickListener(v -> openImagePicker());
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDueTime > 0) {
            calendar.setTimeInMillis(selectedDueTime);
        }

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

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
                int count = Math.min(data.getClipData().getItemCount(), 5 - selectedImagePaths.size());
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    String path = getRealPathFromUri(imageUri);
                    if (path != null) {
                        selectedImagePaths.add(path);
                    }
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                String path = getRealPathFromUri(imageUri);
                if (path != null) {
                    selectedImagePaths.add(path);
                }
            }
            imageAdapter.notifyDataSetChanged();
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

    private void saveTodo() {
        String todoText = editTextTodo.getText().toString().trim();
        if (todoText.isEmpty()) {
            Toast.makeText(this, "待办事项不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTodo.setText(todoText);
        currentTodo.setDueTime(selectedDueTime);
        currentTodo.getImagePaths().clear();
        currentTodo.getImagePaths().addAll(selectedImagePaths);

        dbHelper.updateTodo(currentTodo);
        Log.d("EditTodoActivity", "Saving todo: " + currentTodo.getText() + ", id: " + currentTodo.getId());

        setResult(RESULT_OK);
        finish();
    }
}