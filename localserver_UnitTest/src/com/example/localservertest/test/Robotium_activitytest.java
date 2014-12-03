package com.example.localservertest.test;

import android.test.ActivityInstrumentationTestCase2;
import com.example.localservertest.controllers.MainActivity;
import com.robotium.solo.Solo;

public class Robotium_activitytest extends ActivityInstrumentationTestCase2<MainActivity>
{
	private Solo solo;
	
	public Robotium_activitytest()
	{
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception
	{
		solo = new Solo(getInstrumentation(), getActivity());
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		solo.finishOpenedActivities();
	}
	
	public void testAddNote() throws Exception {
		//Unlock the lock screen
		solo.unlockScreen(); 
		//In text field 0, enter Note 1
		solo.clickOnToggleButton("OFF");
		


	}
	
	
}
