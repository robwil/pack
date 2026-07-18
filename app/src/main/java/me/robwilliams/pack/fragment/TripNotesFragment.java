package me.robwilliams.pack.fragment;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.robwilliams.pack.R;
import me.robwilliams.pack.data.TripNoteContentProvider;
import me.robwilliams.pack.data.TripTodoContentProvider;

public class TripNotesFragment extends Fragment {

    private int tripId;
    private TextView notesDisplay;
    private EditText notesEdit;
    private android.widget.Button editSaveButton;
    private boolean editing = false;
    private long noteRowId = -1;

    private RecyclerView todoRecyclerView;
    private List<TodoItem> todoItems = new ArrayList<>();
    private TodoAdapter todoAdapter;
    private ItemTouchHelper itemTouchHelper;

    static class TodoItem {
        long id;
        String text;
        boolean done;
        int sortOrder;

        TodoItem(long id, String text, boolean done, int sortOrder) {
            this.id = id;
            this.text = text;
            this.done = done;
            this.sortOrder = sortOrder;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trip_notes, container, false);

        tripId = getArguments().getInt("tripId");

        notesDisplay = root.findViewById(R.id.notes_display);
        notesEdit = root.findViewById(R.id.notes_edit);
        editSaveButton = root.findViewById(R.id.notes_edit_save_button);
        todoRecyclerView = root.findViewById(R.id.todo_list);

        editSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editing) {
                    saveNote();
                } else {
                    enterEditMode();
                }
            }
        });

        root.findViewById(R.id.todo_add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTodoDialog();
            }
        });

        loadNote();
        loadTodos();

        return root;
    }

    private void loadNote() {
        Cursor cursor = getActivity().getContentResolver().query(
                TripNoteContentProvider.CONTENT_URI,
                new String[]{"_id", "content"},
                "trip_id=" + tripId, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            noteRowId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
            notesDisplay.setText(content);
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            noteRowId = -1;
            notesDisplay.setText("");
        }
        showReadMode();
    }

    private void enterEditMode() {
        editing = true;
        notesEdit.setText(notesDisplay.getText());
        notesDisplay.setVisibility(View.GONE);
        notesEdit.setVisibility(View.VISIBLE);
        editSaveButton.setText("Save");
        notesEdit.requestFocus();
    }

    private void saveNote() {
        editing = false;
        String content = notesEdit.getText().toString();
        ContentValues values = new ContentValues();
        values.put("content", content);
        values.put("trip_id", tripId);

        if (noteRowId >= 0) {
            getActivity().getContentResolver().update(
                    TripNoteContentProvider.CONTENT_URI, values,
                    "_id=" + noteRowId, null);
        } else {
            android.net.Uri uri = getActivity().getContentResolver().insert(
                    TripNoteContentProvider.CONTENT_URI, values);
            noteRowId = Long.parseLong(uri.getLastPathSegment());
        }
        notesDisplay.setText(content);
        showReadMode();
    }

    private void showReadMode() {
        notesEdit.setVisibility(View.GONE);
        notesDisplay.setVisibility(View.VISIBLE);
        editSaveButton.setText("Edit");
        editing = false;
    }

    private void loadTodos() {
        todoItems.clear();
        Cursor cursor = getActivity().getContentResolver().query(
                TripTodoContentProvider.CONTENT_URI,
                new String[]{"_id", "text", "done", "sort_order"},
                "trip_id=" + tripId, null, "done ASC, sort_order ASC, _id ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                todoItems.add(new TodoItem(
                        cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("text")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("done")) != 0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("sort_order"))));
            }
            cursor.close();
        }
        todoAdapter = new TodoAdapter();
        todoRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        todoRecyclerView.setAdapter(todoAdapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.LEFT;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder source,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = source.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                Collections.swap(todoItems, from, to);
                todoAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1f);
                viewHolder.itemView.setScaleX(1f);
                viewHolder.itemView.setScaleY(1f);
                viewHolder.itemView.setBackgroundColor(0);
                viewHolder.itemView.setElevation(0);
                persistSortOrder();
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        sortAndRefresh();
                    }
                });
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                todoRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        itemTouchHelper.attachToRecyclerView(null);
                        itemTouchHelper.attachToRecyclerView(todoRecyclerView);
                        todoAdapter.notifyItemChanged(position);
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Delete To-Do")
                                .setMessage("Delete \"" + todoItems.get(position).text + "\"?")
                                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteTodo(position))
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                    }
                });
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.85f);
                    viewHolder.itemView.setScaleX(1.03f);
                    viewHolder.itemView.setScaleY(1.03f);
                    viewHolder.itemView.setBackgroundColor(0x1A000000);
                    viewHolder.itemView.setElevation(8 * getResources().getDisplayMetrics().density);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    ColorDrawable bg = new ColorDrawable(0xFFD32F2F);
                    bg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    bg.draw(c);

                    Paint paint = new Paint();
                    paint.setColor(0xFFFFFFFF);
                    paint.setTextSize(14 * getResources().getDisplayMetrics().density);
                    paint.setAntiAlias(true);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    float textY = itemView.getTop() + (itemView.getHeight() + paint.getTextSize()) / 2f;
                    c.drawText("Delete", itemView.getRight() - 16 * getResources().getDisplayMetrics().density,
                            textY, paint);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(todoRecyclerView);
    }

    private void showAddTodoDialog() {
        final EditText input = new EditText(getActivity());
        input.setHint("What needs doing?");
        float density = getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        input.setPadding(dp16, dp16, dp16, dp16);

        new AlertDialog.Builder(getActivity())
                .setTitle("Add To-Do")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        int maxOrder = 0;
                        for (TodoItem item : todoItems) {
                            if (!item.done && item.sortOrder >= maxOrder) {
                                maxOrder = item.sortOrder + 1;
                            }
                        }
                        ContentValues values = new ContentValues();
                        values.put("trip_id", tripId);
                        values.put("text", text);
                        values.put("done", 0);
                        values.put("sort_order", maxOrder);
                        android.net.Uri uri = getActivity().getContentResolver().insert(
                                TripTodoContentProvider.CONTENT_URI, values);
                        long id = Long.parseLong(uri.getLastPathSegment());
                        todoItems.add(new TodoItem(id, text, false, maxOrder));
                        sortAndRefresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleTodo(int position) {
        TodoItem item = todoItems.get(position);
        item.done = !item.done;
        ContentValues values = new ContentValues();
        values.put("done", item.done ? 1 : 0);
        getActivity().getContentResolver().update(
                TripTodoContentProvider.CONTENT_URI, values,
                "_id=" + item.id, null);
        sortAndRefresh();
    }

    private void deleteTodo(int position) {
        TodoItem item = todoItems.get(position);
        getActivity().getContentResolver().delete(
                TripTodoContentProvider.CONTENT_URI,
                "_id=" + item.id, null);
        todoItems.remove(position);
        todoAdapter.notifyItemRemoved(position);
    }

    private void sortAndRefresh() {
        Collections.sort(todoItems, (a, b) -> {
            if (a.done != b.done) return a.done ? 1 : -1;
            return Integer.compare(a.sortOrder, b.sortOrder);
        });
        todoAdapter.notifyDataSetChanged();
    }

    private void persistSortOrder() {
        for (int i = 0; i < todoItems.size(); i++) {
            TodoItem item = todoItems.get(i);
            if (item.sortOrder != i) {
                item.sortOrder = i;
                ContentValues values = new ContentValues();
                values.put("sort_order", i);
                getActivity().getContentResolver().update(
                        TripTodoContentProvider.CONTENT_URI, values,
                        "_id=" + item.id, null);
            }
        }
    }

    private class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                LinearLayout layout = (LinearLayout) itemView;
                checkBox = (CheckBox) layout.getChildAt(0);
                textView = (TextView) layout.getChildAt(1);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            float density = getResources().getDisplayMetrics().density;
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding((int)(4 * density), (int)(10 * density),
                    (int)(4 * density), (int)(10 * density));
            layout.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            CheckBox checkBox = new CheckBox(getActivity());
            layout.addView(checkBox);

            TextView textView = new TextView(getActivity());
            textView.setTextSize(15);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textView.setLayoutParams(textParams);
            layout.addView(textView);

            return new ViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TodoItem item = todoItems.get(position);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(item.done);
            holder.textView.setText(item.text);

            if (item.done) {
                holder.textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
                holder.textView.setPaintFlags(holder.textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
                holder.textView.setPaintFlags(holder.textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    toggleTodo(pos);
                }
            });
            holder.textView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    toggleTodo(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return todoItems.size();
        }
    }
}
