package com.example.chapter03;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.CompletedViewHolder> {
    private List<Todo> completedList;
    private SimpleDateFormat dateFormat;

    public CompletedAdapter(List<Todo> completedList) {
        this.completedList = completedList;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public CompletedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed, parent, false);
        return new CompletedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedViewHolder holder, int position) {
        Todo todo = completedList.get(position);
        holder.textViewTodo.setText(todo.getText());
        String completedTime = dateFormat.format(new Date(todo.getCompletedTime()));
        holder.textViewTime.setText("完成时间：" + completedTime);
    }

    @Override
    public int getItemCount() {
        return completedList.size();
    }

    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTodo;
        TextView textViewTime;

        CompletedViewHolder(View itemView) {
            super(itemView);
            textViewTodo = itemView.findViewById(R.id.text_view_completed_todo);
            textViewTime = itemView.findViewById(R.id.text_view_completed_time);
        }
    }
}
