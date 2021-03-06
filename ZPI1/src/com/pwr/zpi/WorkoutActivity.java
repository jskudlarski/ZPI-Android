package com.pwr.zpi;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.pwr.zpi.adapters.AdapterFactory;
import com.pwr.zpi.adapters.AdapterFactory.AdapterType;
import com.pwr.zpi.adapters.GenericBaseAdapter;
import com.pwr.zpi.adapters.WorkoutActionsRowBuilder;
import com.pwr.zpi.database.Database;
import com.pwr.zpi.database.entity.Workout;
import com.pwr.zpi.database.entity.WorkoutAction;
import com.pwr.zpi.dialogs.MyDialog;
import com.pwr.zpi.listeners.GestureListener;
import com.pwr.zpi.listeners.MyGestureDetector;

public class WorkoutActivity extends Activity implements GestureListener, OnItemClickListener {
	
	private GestureDetector gestureDetector;
	private MyGestureDetector myGestureDetector;
	private ListView actionsListView;
	private Workout workout;
	private BaseAdapter actionsAdapter;
	private View mCurrent;
	private View.OnTouchListener gestureListener;
	private Button addThisWorkoutButton;
	private Button editThisWorkoutButton;
	private TextView workoutNameTextView;
	private AdapterContextMenuInfo info;
	private TextView warmUpTextView;
	private ArrayList<WorkoutAction> actions;
	private static final int MY_REQUEST_CODE_EDIT = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.workout_activity);
		prepareGestureListener();
		initFields();
		addListeners();
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);  // Add this method.
	}
	
	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);  // Add this method.
	}
	
	private void initFields() {
		actionsListView = (ListView) findViewById(R.id.listViewWokoutActions);
		long ID = getIntent().getLongExtra(PlaningActivity.ID_TAG, -1);
		workout = readData(ID);
		actions = (ArrayList<WorkoutAction>) workout.getActions();
		actionsAdapter = AdapterFactory.getAdapter(AdapterType.WorkoutActionAdapter, this, actions, null);
		
		View header = getLayoutInflater().inflate(R.layout.workout_header, null);
		View footer = getLayoutInflater().inflate(R.layout.workout_footer, null);
		actionsListView.addHeaderView(header);
		actionsListView.addFooterView(footer);
		actionsListView.setAdapter(actionsAdapter);
		actionsListView.setAdapter(actionsAdapter);
		
		addThisWorkoutButton = (Button) header.findViewById(R.id.ButtonChooseWorkout);
		workoutNameTextView = (TextView) header.findViewById(R.id.textViewWorkoutName);
		warmUpTextView = (TextView) footer.findViewById(R.id.textViewWorkoutActivityWarmUp);
		editThisWorkoutButton = (Button) footer.findViewById(R.id.buttonWorkoutEdit);
		
		workoutNameTextView.setText(workout.getName());
		warmUpTextView.setText(workout.isWarmUp() ? R.string.yes : R.string.no);
		
		registerForContextMenu(actionsListView);
	}
	
	private void prepareGestureListener() {
		// Gesture detection
		myGestureDetector = new MyGestureDetector(this, false, false, false, true);
		gestureDetector = new GestureDetector(this, myGestureDetector);
		gestureListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mCurrent = v;
				return gestureDetector.onTouchEvent(event);
			}
		};
	}
	
	private void addListeners() {
		addThisWorkoutButton.setOnTouchListener(gestureListener);
		actionsListView.setOnTouchListener(gestureListener);
		editThisWorkoutButton.setOnTouchListener(gestureListener);
		actionsListView.setOnItemClickListener(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureListener.onTouch(null, event);
	}
	
	private Workout readData(long ID) {
		Workout workout;
		
		Database database = new Database(this);
		workout = database.getWholeSingleWorkout(ID);
		database.close();
		if (workout.getActions() == null) {
			ArrayList<WorkoutAction> emptyList = new ArrayList<WorkoutAction>();
			workout.setActions(emptyList);
		}
		return workout;
		
	}
	
	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		super.onBackPressed();
		//overridePendingTransition(R.anim.in_left_anim, R.anim.out_left_anim);
	}
	
	@Override
	public void onLeftToRightSwipe() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onRightToLeftSwipe() {
		finish();
		//overridePendingTransition(R.anim.in_left_anim, R.anim.out_left_anim);
	}
	
	@Override
	public void onUpToDownSwipe() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDownToUpSwipe() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSingleTapConfirmed(MotionEvent e) {
		int ID = mCurrent.getId();
		switch (ID) {
			case R.id.ButtonChooseWorkout:
				//TODO check GPS like in MainScreen
				Intent i = new Intent();
				i.putExtra(Workout.TAG, workout);
				setResult(RESULT_OK, i);
				finish();
				break;
			case R.id.buttonWorkoutEdit:
				Intent intent = new Intent(WorkoutActivity.this, NewWorkoutActivity.class);
				
				intent.putExtra(Workout.TAG, workout);
				startActivityForResult(intent, MY_REQUEST_CODE_EDIT);
				break;
			default:
				break;
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
			case MY_REQUEST_CODE_EDIT:
				if (resultCode == RESULT_OK) {
					workout = data.getParcelableExtra(Workout.TAG);
					
					workoutNameTextView.setText(workout.getName());
					actions.clear();
					actions.addAll(workout.getActions());
					actionsAdapter.notifyDataSetChanged();
					warmUpTextView.setText(workout.isWarmUp() ? R.string.yes : R.string.no);
					Database db = new Database(this);
					db.updateWorkout(workout);
					db.close();
					
				}
				break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		if (!myGestureDetector.isFlingDetected()) {
			//TODO edit action
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.delete_action_menuitem:
				info = (AdapterContextMenuInfo) item.getMenuInfo();
				
				if (actionsAdapter != null) {
					DialogInterface.OnClickListener positiveButtonHandler = new DialogInterface.OnClickListener() {
						
						// romove
						@SuppressWarnings("unchecked")
						@Override
						public void onClick(DialogInterface dialog, int id) {
							
							WorkoutAction toDelete = (WorkoutAction) actionsAdapter.getItem(info.position - 1);
							((GenericBaseAdapter<WorkoutAction, WorkoutActionsRowBuilder>) actionsAdapter)
								.remove(toDelete);
							Database db = new Database(WorkoutActivity.this);
							db.deleteWorkoutAction(workout.getID(), toDelete.getID());
							db.close();
						}
					};
					MyDialog.showAlertDialog(this, R.string.dialog_message_remove_action, R.string.empty_string,
						android.R.string.yes, android.R.string.no, positiveButtonHandler, null);
					
				}
				break;
			default:
				break;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		switch (v.getId()) {
			case R.id.listViewWokoutActions:
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.context_menu, menu);
				menu.setHeaderTitle(R.string.menu_ctx_actions);
				break;
			default:
				break;
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
}
