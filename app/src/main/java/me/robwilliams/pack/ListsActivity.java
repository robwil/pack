package me.robwilliams.pack;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ListsActivity extends ListActivity {

//    private PackDAO packDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);

//        packDAO = new PackDAO(this);
//        packDAO.open();

//        List<String> listNames = packDAO.getAllListNames();

        String[] listNamesRaw = new String[]{"test1", "test two"};
        List<String> listNames = new ArrayList<>(Arrays.asList(listNamesRaw));

        // use the SimpleCursorAdapter to show the
        // elements in a ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, listNames);
        setListAdapter(adapter);
    }

    public void launchActivity(View view) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        startActivity(intent);
    }

    public void addItem(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Item");

        // Set up the inputs
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        final EditText nameInput = new EditText(this);
        final EditText weightInput = new EditText(this);
        weightInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        weightInput.setText("1");
        linearLayout.addView(nameInput);
        linearLayout.addView(weightInput);
        builder.setView(linearLayout);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameInput.getText().toString();
                int weight = 1;
                try {
                    weight = Integer.parseInt(weightInput.getText().toString());
                } catch (NumberFormatException ex) {
                }
//                packDAO.createList(name, weight);
                ((ArrayAdapter<String>) getListAdapter()).add(name);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
