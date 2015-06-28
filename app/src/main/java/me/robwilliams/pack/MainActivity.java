package me.robwilliams.pack;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void openListsActivity(View view) {
        Intent intent = new Intent(this, ListOverviewActivity.class);
        startActivity(intent);
    }

    public void openSetsActivity(View view) {
        Intent intent = new Intent(this, SetOverviewActivity.class);
        startActivity(intent);
    }
}
