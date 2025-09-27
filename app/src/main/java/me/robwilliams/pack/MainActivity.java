package me.robwilliams.pack;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

public class MainActivity extends AppCompatActivity {

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

    public void openTripsActivity(View view) {
        Intent intent = new Intent(this, TripOverviewActivity.class);
        startActivity(intent);
    }
}
