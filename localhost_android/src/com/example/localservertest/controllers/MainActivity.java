package com.example.localservertest.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import com.example.localservertest.R;
import com.example.localservertest.controllers.service.HttpServerService;

public class MainActivity extends ActionBarActivity
{
	ToggleButton btn_toggle;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btn_toggle = (ToggleButton) findViewById(R.id.toggleBtn);
		btn_toggle.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked)
			{
				if(isChecked)
				{
					startService(new Intent(MainActivity.this, HttpServerService.class));
				}
				else
				{
					stopService(new Intent(MainActivity.this, HttpServerService.class));
				}
			}
		});
	}
}
