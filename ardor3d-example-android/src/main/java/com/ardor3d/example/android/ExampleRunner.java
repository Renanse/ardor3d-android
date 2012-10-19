
package com.ardor3d.example.android;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ExampleRunner extends Activity {

    protected int selectedIndex = -1;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_runner);

        final Button runButton = (Button) findViewById(R.id.play_button);
        runButton.setVisibility(View.GONE);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                Intent intent = null;

                switch (selectedIndex) {
                    case 0:
                        intent = new Intent(ExampleRunner.this, AndroidBoxExample.class);
                        break;
                    case 1:
                        intent = new Intent(ExampleRunner.this, AndroidShapesExample.class);
                        break;
                    case 2:
                        intent = new Intent(ExampleRunner.this, AndroidMultiPassTextureExample.class);
                        break;
                    case 3:
                        intent = new Intent(ExampleRunner.this, AndroidNewDynamicSmokerExample.class);
                        break;

                }
                if (intent != null) {
                    ExampleRunner.this.startActivity(intent);
                }
            }
        });

        final TextView titleLabel = (TextView) findViewById(R.id.title_label);
        final TextView descrLabel = (TextView) findViewById(R.id.description_label);

        final Resources res = getResources();
        final String[] exampleNames = res.getStringArray(R.array.example_names);
        final String[] descriptions = res.getStringArray(R.array.descriptions);

        // grab the list and add a listener
        final ListView exampleList = (ListView) findViewById(R.id.examples_list);
        exampleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                runButton.setVisibility(View.VISIBLE);
                titleLabel.setText(exampleNames[position]);
                descrLabel.setText(descriptions[position]);
                selectedIndex = position;
            }
        });

        //
    }
}
