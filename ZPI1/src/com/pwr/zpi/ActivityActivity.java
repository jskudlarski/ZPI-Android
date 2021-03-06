package com.pwr.zpi;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.internal.av;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPosition.Builder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pwr.zpi.adapters.AdapterFactory;
import com.pwr.zpi.adapters.AdapterFactory.AdapterType;
import com.pwr.zpi.database.entity.Workout;
import com.pwr.zpi.database.entity.WorkoutAction;
import com.pwr.zpi.database.entity.WorkoutActionWarmUp;
import com.pwr.zpi.dialogs.MyDialog;
import com.pwr.zpi.listeners.ActityButtonStateChangeListener;
import com.pwr.zpi.listeners.MapTrackingListener;
import com.pwr.zpi.listeners.OnNextActionListener;
import com.pwr.zpi.services.LocationService;
import com.pwr.zpi.utils.ActualPaceCalculator;
import com.pwr.zpi.utils.MarkerWithTextBuilder;
import com.pwr.zpi.utils.TimeFormatter;
import com.pwr.zpi.views.GPSSignalDisplayer;

public class ActivityActivity extends AbstractActivityActivity implements OnClickListener {
	
	private static final String TAG = ActivityActivity.class.getSimpleName();
	
	
	public static final int MY_REQUEST_CODE = 1;
	public static final String SAVE_TAG = "save";
	public static final String DISTANCE_TAG = "distance";
	public static final String AVG_SPEED_TAG = "avg_speed";
	public static final String AVG_PACE_TAG = "avg_pace";
	public static final String DURATION_TAG = "duration";
	public static final String RUN_NUMBER_TAG = "run_number";
	public static final String NAME_TAG = "name_tag";
	

	
	//map options
	public static final float TRACE_THICKNESS = 5;
	public static final int TRACE_COLOR = Color.RED;
	private MapTrackingListener mapTrackingListener;
	
	//views
	private Button stopButton;
	private Button pauseButton;
	private Button resumeButton;
	private ImageButton musicPlayer;
	private ImageButton workoutDdrawerButton;
	private TextView DataTextView1;
	private TextView DataTextView2;
	private TextView LabelTextView1;
	private TextView LabelTextView2;
	private TextView unitTextView1;
	private TextView unitTextView2;
	//private TextView GPSAccuracy;
	private GPSSignalDisplayer gpsDisplayer;
	private TextView countDownTextView;
	private LinearLayout startStopLayout;
	private RelativeLayout dataRelativeLayout1;
	private RelativeLayout dataRelativeLayout2;
	private Location mLastLocation;
	private Location mPreLastLocation;
	private ImageButton zoomIn;
	private ImageButton zoomOut;
	private ImageButton mapCenter;
	private ImageButton imageButtonGoToScreen2;
	private FrameLayout frameLayoutViewOverMap;
	
	//map
	private PolylineOptions traceOnMap;
	private Polyline traceOnMapObject;
	private GoogleMap mMap;
	
	// measured values
	private double pace;
	private double avgPace;
	private double distance;
	private double lastDistance;
	private Long time = 0L;
	private int runNumber;
	private ActualPaceCalculator actualPaceCalculator;
	

	// service data
	boolean mIsBound;
	boolean isServiceConnected;
	private RunListenerApi api;
	private Handler handlerForService;
	
	// progress dialog lost gps
	private ProgressDialog lostGPSDialog;
	private boolean progressDialogDisplayed;
	
	// workout drawer fields
	private BaseAdapter drawerListAdapter;
	private ListView listView;
	private Workout workout;
	private Workout workoutCopy;
	private DrawerLayout drawerLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view);
		initFields();
		addListeners();
		initDisplayedData();
		
		prepareServiceAndStart();
		
		//	Debug.stopMethodTracing();
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		prepareServiceAndStart();
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
		stopButton = (Button) findViewById(R.id.stopButton);
		pauseButton = (Button) findViewById(R.id.pauseButton);
		resumeButton = (Button) findViewById(R.id.resumeButton);
		musicPlayer = (ImageButton) findViewById(R.id.buttonMusicDuringActivity);
		workoutDdrawerButton = (ImageButton) findViewById(R.id.imageButtonWorkoutDrawerButton);
		dataRelativeLayout1 = (RelativeLayout) findViewById(R.id.dataRelativeLayout1);
		dataRelativeLayout2 = (RelativeLayout) findViewById(R.id.dataRelativeLayout2);
		gpsDisplayer = (GPSSignalDisplayer) findViewById(R.id.gpsDisplayerActivity);
		countDownTextView = (TextView) findViewById(R.id.textViewCountDown);
		startStopLayout = (LinearLayout) findViewById(R.id.startStopLinearLayout);
		zoomIn = (ImageButton) findViewById(R.id.imageButtonMapZoomIn);
		zoomOut = (ImageButton) findViewById(R.id.imageButtonMapZoomOut);
		mapCenter = (ImageButton) findViewById(R.id.imageButtonMapCenter);
		imageButtonGoToScreen2 =(ImageButton) findViewById(R.id.imageButtonGoToActivityNoMap);
		frameLayoutViewOverMap = (FrameLayout) findViewById(R.id.frameLayoutViewOverMap);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMap = mapFragment.getMap();
		if (mMap != null)
		{
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setMyLocationButtonEnabled(false);
			mMap.getUiSettings().setCompassEnabled(false);
			mMap.getUiSettings().setZoomControlsEnabled(false);
			
			traceOnMap = new PolylineOptions();
			traceOnMap.width(TRACE_THICKNESS);
			traceOnMap.color(TRACE_COLOR);
			traceOnMapObject = mMap.addPolyline(traceOnMap);
		}
		
		mapTrackingListener = new MapTrackingListener(mapCenter);
		
		DataTextView1 = (TextView) findViewById(R.id.dataTextView1);
		DataTextView2 = (TextView) findViewById(R.id.dataTextView2);
		
		LabelTextView1 = (TextView) findViewById(R.id.dataTextView1Discription);
		LabelTextView2 = (TextView) findViewById(R.id.dataTextView2Discription);
		
		unitTextView1 = (TextView) findViewById(R.id.dataTextView1Unit);
		unitTextView2 = (TextView) findViewById(R.id.dataTextView2Unit);
		
		// to change displayed info, change dataTextViewContent and start
		// initLabelsMethod
		dataTextView1Content = distanceID;
		dataTextView2Content = timeID;
		
		isPaused = false;
		pausedManually = false;
		progressDialogDisplayed = false;
		
		Intent intent = getIntent();
		listView = (ListView) findViewById(R.id.left_drawer);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		runNumber = intent.getIntExtra(RUN_NUMBER_TAG, 0);
		if (intent.hasExtra(Workout.TAG)) {
			// drawer initialization
			listView.addHeaderView(getLayoutInflater().inflate(R.layout.workout_drawer_list_header, null));
			workout = getWorkoutData(getIntent());
			workoutCopy = new Workout();
			List<WorkoutAction> actions = new ArrayList<WorkoutAction>();
			if (workout.isWarmUp()) {
				workoutCopy.setWarmUp(true);
				int warmUpMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(
					this.getString(R.string.key_warm_up_time), "3"));
				actions.add(new WorkoutActionWarmUp(warmUpMinutes));
			}
			
			actions.addAll(workout.getActions());
			
			workoutCopy.setActions(actions);
			drawerListAdapter = AdapterFactory.getAdapter(AdapterType.DrawerWorkoutsAdapter, this,
				workoutCopy.getActions(), workoutCopy);
			listView.setAdapter(drawerListAdapter);
			listView.setVisibility(View.VISIBLE);
		}
		else {
			workoutDdrawerButton.setVisibility(View.GONE);
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			listView.setVisibility(View.GONE);
		}
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	private void addListeners() {
		stopButton.setOnClickListener(this);
		resumeButton.setOnClickListener(this);
		pauseButton.setOnClickListener(this);
		
		dataRelativeLayout1.setOnClickListener(this);
		dataRelativeLayout2.setOnClickListener(this);
		
		zoomIn.setOnClickListener(this);
		zoomOut.setOnClickListener(this);
		
		musicPlayer.setOnClickListener(this);
		
		mapCenter.setOnClickListener(this);
		
		imageButtonGoToScreen2.setOnClickListener(this); //TODO implement
		
		frameLayoutViewOverMap.setOnTouchListener(mapTrackingListener);
		if (workout != null) {
			workoutDdrawerButton.setOnClickListener(this);
			
			drawerLayout.setDrawerListener(new DrawerListener() {
				
				@Override
				public void onDrawerStateChanged(int arg0) {}
				
				@Override
				public void onDrawerSlide(View arg0, float arg1) {}
				
				@Override
				public void onDrawerOpened(View arg0) {
					
					if (workout == null) {
						drawerLayout.closeDrawer(Gravity.LEFT);
					}
					else {
						listView.smoothScrollToPosition(workoutCopy.getCurrentAction() + 4, workoutCopy.getActions()
							.size());
					}
				}
				
				@Override
				public void onDrawerClosed(View arg0) {
					Log.i(TAG, "drawer closed");
				}
			});
			
			workoutCopy.setOnNextActionListener(new OnNextActionListener());
		}
		addOnStateChangedListener();
		
	}
	
	private void addOnStateChangedListener()
	{
		ActityButtonStateChangeListener stateChangedListener = new ActityButtonStateChangeListener(this);
		pauseButton.setOnTouchListener(stateChangedListener);
		stopButton.setOnTouchListener(stateChangedListener);
		resumeButton.setOnTouchListener(stateChangedListener);
		workoutDdrawerButton.setOnTouchListener(stateChangedListener);
		musicPlayer.setOnTouchListener(stateChangedListener);
		dataRelativeLayout1.setOnTouchListener(stateChangedListener);
		dataRelativeLayout2.setOnTouchListener(stateChangedListener);
		zoomIn.setOnTouchListener(stateChangedListener);
		zoomOut.setOnTouchListener(stateChangedListener);
		mapCenter.setOnTouchListener(stateChangedListener);
		imageButtonGoToScreen2.setOnTouchListener(stateChangedListener);
		
	}
	
	private void initDisplayedData() {
		
		initLabels(DataTextView1, LabelTextView1, dataTextView1Content);
		initLabels(DataTextView2, LabelTextView2, dataTextView2Content);
	}
	
	private void prepareServiceAndStart() {
		doBindService();
		
		handlerForService = new Handler();
	}
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "activityactiviy destroyed");
		doUnbindService();
		super.onDestroy();
	}
	

	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		showAlertDialog(isServiceConnected,api,this,distance,time,runNumber);
	}
	

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == MY_REQUEST_CODE) {
			
			if (isServiceConnected && resultCode == RESULT_OK) {
				try {
					String name = data.getStringExtra(NAME_TAG);
					api.doSaveRun(data.getBooleanExtra(SAVE_TAG, false), name);
				}
				catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (isServiceConnected) {
				try {
					api.doSaveRun(false, "");
				}
				catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			setResult(RESULT_OK);	//dont know why its needed, but it is...
			finish();
			overridePendingTransition(R.anim.in_up_anim, R.anim.out_up_anim);
			
		}
	}
	
	private void showLostGpsSignalDialog() {
		handlerForService.post(new Runnable() {
			@Override
			public void run() {
				if (isServiceConnected) {
					
					progressDialogDisplayed = true;
					
					lostGPSDialog = ProgressDialog.show(ActivityActivity.this,
						getResources().getString(R.string.dialog_message_on_lost_gpsp), null); 
					lostGPSDialog.setCancelable(true);
					
				}
			}
		});
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.stopButton:
				showAlertDialog(isServiceConnected,api,this,distance,time,runNumber);
				break;
			case R.id.pauseButton:
				pausedManually = true;
				pauseRun();
				break;
			case R.id.resumeButton:
				resumeRun();
				break;
			case R.id.dataRelativeLayout1:
				clickedContentTextView = DataTextView1;
				clickedLabelTextView = LabelTextView1;
				clickedUnitTextView = unitTextView1;
				clickedField = 1;
				showMeassuredValuesMenu(distance,time,pace,avgPace);
				break;
			case R.id.dataRelativeLayout2:
				clickedContentTextView = DataTextView2;
				clickedLabelTextView = LabelTextView2;
				clickedUnitTextView = unitTextView2;
				clickedField = 2;
				showMeassuredValuesMenu(distance,time,pace,avgPace);
				break;
			case R.id.imageButtonWorkoutDrawerButton:
				boolean isOpen = drawerLayout.isDrawerOpen(Gravity.LEFT);
				if (!isOpen) {
					drawerLayout.openDrawer(Gravity.LEFT);
				}
				else {
					drawerLayout.closeDrawer(Gravity.LEFT);
				}
				break;
			case R.id.buttonMusicDuringActivity:
				startSystemMusicPlayer();
				break;
			case R.id.imageButtonMapZoomIn:
				mMap.animateCamera(CameraUpdateFactory.zoomIn());
				mapTrackingListener.setMapTracking(false);
				break;
			case R.id.imageButtonMapZoomOut:
				mMap.animateCamera(CameraUpdateFactory.zoomOut());
				mapTrackingListener.setMapTracking(false);
				break;
			case R.id.imageButtonMapCenter:
				if (mLastLocation != null)
				{
					Location preLast = (mPreLastLocation == null) ? mLastLocation : mPreLastLocation;
					CameraPosition cameraPosition = buildCameraPosition(new LatLng(
						mLastLocation.getLatitude(), mLastLocation.getLongitude()), mLastLocation, preLast);
					mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition
						));
					mapTrackingListener.setMapTracking(true);
				}
				break;
			case R.id.imageButtonGoToActivityNoMap:
				start2Screen();
		}
		
	}
	@Override
	protected void pauseRun() {
		handlerForService.post(new Runnable() {
			@Override
			public void run() {
				if (!isPaused
					&& (!(workout != null && workout.isWarmUp() && workout.getCurrentAction() == 0)))// || pausedManually))
				{
					if (actualPaceCalculator != null) {
						actualPaceCalculator.reset();
					}
					isPaused = true;
					startStopLayout.setVisibility(View.INVISIBLE);
					resumeButton.setVisibility(View.VISIBLE);
					try {
						if (isServiceConnected) {
							api.setPaused();
						}
					}
					catch (RemoteException e) {
						Log.e(TAG, "Failed to tell that activity is paused", e);
					}
				}
				else if (!isPaused && pausedManually)
				{
					Toast.makeText(ActivityActivity.this, getResources().getString(R.string.no_pause),
						Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	@Override
	protected void resumeRun() {
		handlerForService.post(new Runnable() {
			@Override
			public void run() {
				if (isPaused) {
					isPaused = false;
					pausedManually = false;
					startStopLayout.setVisibility(View.VISIBLE);
					resumeButton.setVisibility(View.GONE);
					try {
						if (isServiceConnected) {
							api.setResumed();
						}
					}
					catch (RemoteException e) {
						Log.e(TAG, "Failed to tell that activity is resumed", e);
					}
				}
			}
		});
	}
	
	// count everything with 2 last location points
	private void countData(final Location location, final Location lastLocation) {
		
		Log.i("ActivityActivity", "countData: " + location);
		handlerForService.post(new Runnable() {
			@Override
			public void run() {
				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
				
				traceOnMap.add(latLng);
				traceOnMapObject.setPoints(traceOnMap.getPoints());
				
				mapTrackingListener.checkUserInactivity();
				if (mapTrackingListener.isMapTracking()) {
					CameraPosition cameraPosition = buildCameraPosition(latLng, location, lastLocation);
					mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				}
				// mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
				
				gpsDisplayer.updateStrengthSignal(location.getAccuracy());
				
				if (actualPaceCalculator == null) {
					actualPaceCalculator = new ActualPaceCalculator();
				}
				pace = actualPaceCalculator.addPoint(location);//(double) 1 / (speed * 60 / 1000);
				
				double lastDistance = ActivityActivity.this.lastDistance / 1000;
				
				int distancetoShow = (int) (distance / 1000);
				// new km
				if (distancetoShow - (int) lastDistance > 0) {
					addMarker(location, distancetoShow);
				}
				
				synchronized (time) {
					avgPace = ((double) time / 60) / distance;
				}
			}
		});
	}
	
	private CameraPosition buildCameraPosition(LatLng latLng, Location location, Location lastLocation) {
		Builder builder = new CameraPosition.Builder().target(latLng).zoom(17);	// Sets the zoom
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_map_3d), true)) {
			builder.bearing(lastLocation.bearingTo(location)) // Sets the orientation of the
				// camera to east
				.tilt(60); // Creates a CameraPosition from the builder
		}
		return builder.build();
	}
	
	private void addMarker(Location location, int distance) {
		Marker marker = mMap.addMarker(new MarkerOptions()
			.position(new LatLng(location.getLatitude(), location.getLongitude())).title(distance + "km")
			.icon(BitmapDescriptorFactory
				.fromBitmap(MarkerWithTextBuilder.markerWithText(this, distance).getBitmap())));
	}
	
	// this runs on every update
	@Override
	protected void updateGpsInfo(final Location newLocation) {
		autoPauseIfEnabled(newLocation);
		handlerForService.post(new Runnable() {
			
			@Override
			public void run() {
				if (newLocation.getAccuracy() < LocationService.REQUIRED_ACCURACY) {
					// not first point after start or resume
					
					if (lostGPSDialog != null) {
						lostGPSDialog.dismiss();
						lostGPSDialog = null;
						progressDialogDisplayed = false;
					}
					if (!isPaused)
					{
						if (mLastLocation != null) {
							countData(newLocation, mLastLocation);
						}
						
						updateData(DataTextView1, dataTextView1Content,distance,pace,time,avgPace);
						updateData(DataTextView2, dataTextView2Content,distance,pace,time,avgPace);
					}
				}
				else if (newLocation.getAccuracy() >= LocationService.REQUIRED_ACCURACY) {
					//prevents displaying two progress Dialogs at once
					if (!progressDialogDisplayed) {
						showLostGpsSignalDialog();
					}
				}
				mPreLastLocation = mLastLocation;
				mLastLocation = newLocation;
			}
		});
	}
	
	private void autoPauseIfEnabled(Location newLocation) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_auto_pause), false)) {
			if (newLocation.hasSpeed()) {
				Log.e(TAG, "Speed:" + newLocation.getSpeed());
				if (newLocation.getSpeed() < MIN_SPEED_FOR_AUTO_PAUSE) {
					pauseRun();
				}
				else if (!pausedManually) {
					resumeRun();
				}
			}
			else {
				Log.e(TAG, "No speed.. pausing anyway");
				pauseRun();
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			showAlertDialog(isServiceConnected,api,this,distance,time,runNumber);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	// SERVICE METHODS
	
	private void doBindService() {
		
		Log.i("Service_info", "ActivityActivity Binding");
		Intent intent = new Intent(LocationService.class.getName());
		//intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		//intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		bindService(intent, serviceConnection, 0);
		mIsBound = true;
	}
	
	private void doUnbindService() {
		Log.i("Service_info", "ActivityActivity Unbinding");
		if (mIsBound) {
			try {
				api.removeListener(runListener);
				unbindService(serviceConnection);
			}
			catch (RemoteException e) {
				
				e.printStackTrace();
			}
			
			mIsBound = false;
			
		}
	}
	
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established");
			isServiceConnected = true;
			// that's how we get the client side of the IPC connection
			api = RunListenerApi.Stub.asInterface(service);
			try {
				api.addListener(runListener);
				List<Location> locationList = api.getWholeRun();
				if (locationList != null) {
					setTracefromServer(locationList);
				}
				
				int countDownTime = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
					ActivityActivity.this).getString(getString(R.string.key_countdown_before_start), "0"));
				
				api.setStarted(workoutCopy, countDownTime); // -,-' must be here because service has different preference context, so when user changes it in setting it doesn't work okay
			}
			catch (RemoteException e) {
				Log.e(TAG, "Failed to add listener", e);
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			isServiceConnected = false;
			Log.i(TAG, "Service connection closed");
		}
		
	};
	
	private final RunListener.Stub runListener = new RunListener.Stub() {
		
		@Override
		public void handleLocationUpdate() throws RemoteException {
			Location location = api.getLatestLocation();
			lastDistance = distance;
			distance = api.getDistance();
			updateGpsInfo(location);
		}
		
		@Override
		public void handleConnectionResult() throws RemoteException {}
		
		@Override
		public void handleTimeChange() throws RemoteException {
			handleTimeUpdates();
		}
		
		@Override
		public void handleWorkoutChange(Workout workout, boolean firtTime) throws RemoteException {
			Log.i(TAG, "workout data: " + workout.isWarmUp() + " " + workout.getCurrentAction() + " " + firtTime);
			if (workout.isWarmUp() && workout.getCurrentAction() == 0 && firtTime) {
				setWarmUpText();
			}
			handleWorkoutUpdate(workout);
		}
		
		@Override
		public void handleCountDownChange(int countDownNumber) throws RemoteException {
			handleCountDownUpdate(countDownNumber);
		}
		
		@Override
		public void handleLostGPS() throws RemoteException {
			if (!progressDialogDisplayed) {
				showLostGpsSignalDialog();
			}
			handlerForService.post(new Runnable() {
				
				@Override
				public void run() {
					gpsDisplayer.updateStrengthSignal(Double.MAX_VALUE);					
				}
			});
			
		}
		
	};
	
	private void reset() {
		distance = 0;
		traceOnMap = new PolylineOptions();
		traceOnMap.width(TRACE_THICKNESS);
		traceOnMap.color(TRACE_COLOR);
		//mMap.clear();
		time = 0L;
	}
	
	private void setWarmUpText() {
		handlerForService.post(new Runnable() {
			
			@Override
			public void run() {
				countDownTextView.setText(R.string.warmup);
				
			}
		});
	}
	
	private void setTracefromServer(final List<Location> locationList) {
		handlerForService.post(new Runnable() {
			
			@Override
			public void run() {
				if (locationList != null) {
					LatLng latLng = null;
					for (Location location : locationList) {
						latLng = new LatLng(location.getLatitude(), location.getLongitude());
						traceOnMap.add(latLng);
					}
					traceOnMapObject.setPoints(traceOnMap.getPoints());
					int size = locationList.size();
					if (size > 1) {
						CameraPosition cameraPosition = buildCameraPosition(latLng, locationList.get(size - 1),
							locationList.get(size - 2));
						mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
						
					}
				}
			}
		});
		
	}
	
	private void updateViewsAfterTimeChange() {
		handlerForService.post(new Runnable() {
			
			@Override
			public void run() {
				updateData(DataTextView1, dataTextView1Content, distance, pace, time, avgPace);
				updateData(DataTextView2, dataTextView2Content, distance, pace, time, avgPace);
			}
		});
	}
	@Override
	protected void handleTimeUpdates() {
		try {
			time = api.getTime();
			updateViewsAfterTimeChange();
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void handleWorkoutUpdate(final Workout newWorkout) {
		handlerForService.post(new Runnable() {
			
			@Override
			public void run() {
				ActivityActivity.this.workoutCopy.updateWorkoutData(newWorkout);
				drawerListAdapter.notifyDataSetChanged();
				listView.smoothScrollToPosition(workoutCopy.getCurrentAction() + 4, workoutCopy.getActions().size());
			}
		});
	}
	
	private void handleCountDownUpdate(final int countDownNumber) {
		countDownTextView.setVisibility(View.VISIBLE);
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (countDownNumber == -1) {
					countDownTextView.setVisibility(View.GONE);
					reset();
					//					startRecording();
				}
				else if (countDownNumber == 0) {
					countDownTextView.setText(ActivityActivity.this.getString(R.string.go));
				}
				else {
					countDownTextView.setText(countDownNumber + "");
				}
			}
		});
	}
	
	private void startSystemMusicPlayer() {
		Intent i;
		i = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
		startActivity(i);
	}
	
	private void start2Screen()
	{
		Intent i = new Intent(ActivityActivity.this, ActivityNoMapActivity.class);
		if (workout!=null)
			i.putExtra(Workout.TAG, workout);
		i.putExtra(RUN_NUMBER_TAG, runNumber);
		finish();
		startActivity(i);
		overridePendingTransition(R.anim.in_left_anim, R.anim.out_left_anim);
	}
}
