package com.example.chapter03;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class TodoDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "todo.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_TODO = "todos";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_POSITION = "position";
    private static final String COLUMN_COMPLETED_TIME = "completed_time";
    private static final String COLUMN_DUE_TIME = "due_time";
    private static final String TABLE_IMAGES = "todo_images";
    private static final String COLUMN_TODO_ID = "todo_id";
    private static final String COLUMN_IMAGE_PATH = "image_path";

    public TodoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTodoTable = "CREATE TABLE " + TABLE_TODO + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TEXT + " TEXT, " +
                COLUMN_COMPLETED + " INTEGER, " +
                COLUMN_POSITION + " INTEGER, " +
                COLUMN_COMPLETED_TIME + " INTEGER, " +
                COLUMN_DUE_TIME + " INTEGER)";

        String createImageTable = "CREATE TABLE " + TABLE_IMAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TODO_ID + " INTEGER, " +
                COLUMN_IMAGE_PATH + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_TODO_ID + ") REFERENCES " +
                TABLE_TODO + "(" + COLUMN_ID + ") ON DELETE CASCADE)";

        db.execSQL(createTodoTable);
        db.execSQL(createImageTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO);
        onCreate(db);
    }

    public void saveTodo(Todo todo) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues todoValues = new ContentValues();
            todoValues.put(COLUMN_TEXT, todo.getText());
            todoValues.put(COLUMN_COMPLETED, todo.isCompleted() ? 1 : 0);
            todoValues.put(COLUMN_POSITION, todo.getPosition());
            todoValues.put(COLUMN_COMPLETED_TIME, todo.getCompletedTime());
            todoValues.put(COLUMN_DUE_TIME, todo.getDueTime());
            long todoId = db.insert(TABLE_TODO, null, todoValues);
            todo.setId(todoId);

            for (String path : todo.getImagePaths()) {
                ContentValues imageValues = new ContentValues();
                imageValues.put(COLUMN_TODO_ID, todoId);
                imageValues.put(COLUMN_IMAGE_PATH, path);
                db.insert(TABLE_IMAGES, null, imageValues);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private void loadTodoImages(SQLiteDatabase db, Todo todo) {
        String query = "SELECT " + COLUMN_IMAGE_PATH + " FROM " + TABLE_IMAGES +
                " WHERE " + COLUMN_TODO_ID + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(todo.getId())})) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH));
                todo.addImagePath(path);
            }
        }
    }

    public List<Todo> getAllTodos() {
        List<Todo> todoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TODO +
                " WHERE " + COLUMN_COMPLETED + " = 0" +
                " ORDER BY " + COLUMN_POSITION;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT);
            int positionIndex = cursor.getColumnIndexOrThrow(COLUMN_POSITION);
            int dueTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_DUE_TIME);

            while (cursor.moveToNext()) {
                String text = cursor.getString(textIndex);
                int position = cursor.getInt(positionIndex);
                Todo todo = new Todo(text, position);
                todo.setId(cursor.getLong(idIndex));
                todo.setDueTime(cursor.getLong(dueTimeIndex));
                todoList.add(todo);
            }
        }
        for (Todo todo : todoList) {
            loadTodoImages(db, todo);
        }
        db.close();
        return todoList;
    }

    public void updateTodo(Todo todo) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TEXT, todo.getText());
            values.put(COLUMN_COMPLETED, todo.isCompleted() ? 1 : 0);
            values.put(COLUMN_POSITION, todo.getPosition());
            values.put(COLUMN_COMPLETED_TIME, todo.getCompletedTime());
            values.put(COLUMN_DUE_TIME, todo.getDueTime());

            // 更新todo基本信息
            int updatedRows = db.update(TABLE_TODO, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(todo.getId())});
            Log.d("TodoDbHelper", "Updated rows: " + updatedRows);

            // 删除原有的图片记录
            int deletedRows = db.delete(TABLE_IMAGES, COLUMN_TODO_ID + " = ?",
                    new String[]{String.valueOf(todo.getId())});
            Log.d("TodoDbHelper", "Deleted image rows: " + deletedRows);

            // 插入新的图片记录
            for (String imagePath : todo.getImagePaths()) {
                ContentValues imageValues = new ContentValues();
                imageValues.put(COLUMN_TODO_ID, todo.getId());
                imageValues.put(COLUMN_IMAGE_PATH, imagePath);
                long newRowId = db.insert(TABLE_IMAGES, null, imageValues);
                Log.d("TodoDbHelper", "Inserted image row: " + newRowId);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void deleteAllTodos() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, null, null);
        db.close();
    }

    public List<Todo> getUncompletedTodos() {
        List<Todo> todoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TODO +
                " WHERE " + COLUMN_COMPLETED + " = 0" +
                " ORDER BY " + COLUMN_POSITION;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            int textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT);
            int positionIndex = cursor.getColumnIndexOrThrow(COLUMN_POSITION);

            while (cursor.moveToNext()) {
                String text = cursor.getString(textIndex);
                int position = cursor.getInt(positionIndex);
                Todo todo = new Todo(text, position);
                todoList.add(todo);
            }
        }
        db.close();
        return todoList;
    }

    public List<Todo> getCompletedTodos() {
        List<Todo> todoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TODO +
                " WHERE " + COLUMN_COMPLETED + " = 1" +
                " ORDER BY " + COLUMN_COMPLETED_TIME + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT);
            int positionIndex = cursor.getColumnIndexOrThrow(COLUMN_POSITION);
            int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_TIME);

            while (cursor.moveToNext()) {
                String text = cursor.getString(textIndex);
                int position = cursor.getInt(positionIndex);
                Todo todo = new Todo(text, position);
                todo.setId(cursor.getLong(idIndex));
                todo.setCompleted(true);
                todo.setCompletedTime(cursor.getLong(timeIndex));
                todoList.add(todo);
            }
        }
        db.close();
        return todoList;
    }

    public void deleteTodo(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Todo getTodoById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Todo todo = null;

        try {
            // 首先查询todo的基本信息
            String selectQuery = "SELECT * FROM " + TABLE_TODO +
                    " WHERE " + COLUMN_ID + " = ?";

            try (Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)})) {
                if (cursor.moveToFirst()) {
                    String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                    int position = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POSITION));
                    boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1;
                    long completedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_TIME));
                    long dueTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DUE_TIME));

                    todo = new Todo(text, position);
                    todo.setId(id);
                    todo.setCompleted(completed);
                    todo.setCompletedTime(completedTime);
                    todo.setDueTime(dueTime);

                    // 查询关联的图片
                    String imageQuery = "SELECT " + COLUMN_IMAGE_PATH +
                            " FROM " + TABLE_IMAGES +
                            " WHERE " + COLUMN_TODO_ID + " = ?";

                    try (Cursor imageCursor = db.rawQuery(imageQuery, new String[]{String.valueOf(id)})) {
                        while (imageCursor.moveToNext()) {
                            String imagePath = imageCursor.getString(
                                    imageCursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH));
                            todo.addImagePath(imagePath);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return todo;
    }
}