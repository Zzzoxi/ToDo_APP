package com.example.chapter03;

import java.util.ArrayList;
import java.util.List;

public class Todo {
    // 唯一标识符，用于区分不同的待办事项
    private long id;

    // 待办事项的文本描述
    private String text;

    // 表示待办事项是否已完成
    private boolean completed;

    // 待办事项在列表中的位置
    private int position;

    // 记录待办事项完成的时间
    private long completedTime;

    // 记录待办事项的截止时间
    private long dueTime;

    // 存储与待办事项相关的图片路径列表
    private List<String> imagePaths;

    public Todo(String text, int position) {
        this.text = text;
        this.completed = false;
        this.position = position;
        this.completedTime = 0;
        this.dueTime = 0;
        this.imagePaths = new ArrayList<>();
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDueTime() {
        return dueTime;
    }

    public void setDueTime(long dueTime) {
        this.dueTime = dueTime;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public void addImagePath(String path) {
        if (imagePaths.size() < 5) {
            imagePaths.add(path);
        }
    }
}