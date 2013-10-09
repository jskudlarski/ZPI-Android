package com.pwr.zpi;

import com.pwr.zpi.listeners.GestureListener;
import com.pwr.zpi.listeners.MyGestureDetector;

import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class PlaningActivity extends Activity implements GestureListener {

	GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.planing_activity);
		prepareGestureListener();
	}


	@Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.in_left_anim, R.anim.out_left_anim);
	}
	
	private void prepareGestureListener() {
		// Gesture detection
		gestureDetector = new GestureDetector(this, new MyGestureDetector(this, false, false, false, true));
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureListener.onTouch(null, event);
	}
	
	@Override
	public void onLeftToRightSwipe() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onRightToLeftSwipe() {
		finish();
		overridePendingTransition(R.anim.in_left_anim,R.anim.out_left_anim);
	}


	@Override
	public void onUpToDownSwipe() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onDownToUpSwipe() {
		// TODO Auto-generated method stub
		
	}
}