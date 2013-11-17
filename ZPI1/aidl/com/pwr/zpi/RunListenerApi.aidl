package com.pwr.zpi;

import com.pwr.zpi.RunListener;
import com.pwr.zpi.database.entity.Workout;

interface RunListenerApi {
 
	List<Location> getWholeRun();
	double getDistance();
	long getTime();
	Location getLatestLocation();
	Intent getConnectionResult();
	int getGPSStatus();
	void setStarted(in Workout workout);
	void setPaused();
	void setResumed();
	void setStoped();
	void doSaveRun(in boolean save);
	void prepareTextToSpeech();
   
  	void addListener(RunListener listener);
 
 	void removeListener(RunListener listener);
}