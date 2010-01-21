package tw.edu.ntu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.microedition.khronos.opengles.GL;

import android.app.Activity;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomControls;


public class BrightStar extends Activity implements SensorListener{
	
	private BrightStarRenderer brightStarRenderer;
	private GLSurfaceView mGLSurfaceView;
	private ZoomControls mZoom;
	private LinearLayout linearLayout;
	private static TextView julianDay, altitude, azimuth;
	private float downX;
	private float downY;
	private float filterFactor = 0.3f;
	private float[] orientaton = new float[3];
	boolean mZoomVisible = false;
	private SensorManager mSm;
	private LocationManager mLm;
	private Location mLocation = null;
	private String strLocationProvider = "";
	private PowerManager mPm;
	private PowerManager.WakeLock mWakeLock;
	private boolean ifLocked = false;
	
	private double dAltitude;
	private double dLatitude;
	private double dLongitude;
	
	boolean ledon = false;
	boolean tcpip = true;
	boolean sensor = true;
	boolean location = true;
	boolean mTouchMove = true;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mGLSurfaceView.setGLWrapper(new GLSurfaceView.GLWrapper() {
                public GL wrap(GL gl) {
                    return new MatrixTrackingGL(gl);
                }});
        brightStarRenderer = new BrightStarRenderer(this);
        mGLSurfaceView.setRenderer(brightStarRenderer);
        
        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mZoom = new ZoomControls(this);
        mZoom.setVisibility(View.INVISIBLE);
        mZoom.setOnZoomInClickListener(mZoomInListener);
        mZoom.setOnZoomOutClickListener(mZoomOutListener);
        linearLayout.addView(mZoom);
        
        julianDay = (TextView) findViewById(R.id.julianDay);
        altitude = (TextView) findViewById(R.id.altitude);
        azimuth = (TextView) findViewById(R.id.azimuth);
        julianDay.setText("Julian Day:"+Double.toString(brightStarRenderer.t1.getJD()));
        altitude.setText("altitude:"+Float.toString(brightStarRenderer.lookupdown));
        azimuth.setText("azimuth:"+Float.toString(brightStarRenderer.yrot));
        //brightStarRenderer = new BrightStarRenderer(this);
        //setContentView(brightStarRenderer);

        // LED on enabled
        if (ledon)
        {
        	mPm = (PowerManager) getSystemService(POWER_SERVICE);
        	
        	mWakeLock = mPm.newWakeLock
        	(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "BackLight"
        	);
        }
        // Sensor service enabled
        if (sensor)
		{
			mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
		}

        // TCP Connection with telescope enabled
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("MR08h00m00sMD00d00\'00\"")).start();
        	} catch (Exception e) {
        		/* */
        	}
        }
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("GRD")).start();
        	} catch (Exception e) {

        	}
        }
        /*
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("STOP")).start();
        	} catch (Exception e) {

        	}
        }
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("AST")).start();
        	} catch (Exception e) {

        	}
        }*/
        
        // Location service enabled
        if (location)
        {
            mLm = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocation = getLocationProvider(mLm);
            if (mLocation != null){
            	dAltitude = mLocation.getAltitude();
            	dLatitude = mLocation.getLatitude();
            	dLongitude = mLocation.getLongitude();
            	
            	 TimeCal.longi_d = mLocation.getLatitude();
            	 CoordCal.lat_d = mLocation.getLongitude();
            	
            	Log.e("GPSinit", Double.toString(mLocation.getLatitude()));
            	Log.e("GPSinit", mLocation.toString());
            }
            // 2000ms, 10m
            mLm.requestLocationUpdates(strLocationProvider, 2000, 10, mLocationListener);
        }
    }

    // 海拔
    public double getAltitude() {
    	return dAltitude;
    }
    // 緯度
    public double getLatitude() { 
    	return dLatitude;
    }
    // 經度
    public double getLongitude() {
    	return dLongitude;
    }

        // TCPIP feature tested and enabled
	class TeleScopeConn implements Runnable {
		String message;
		public TeleScopeConn(String command) throws Exception
                {
			this.message = command;
                }

		public void run () {
        	try {
        	    Socket socket = new Socket("192.168.2.101", 10102);
        	    Log.e("TCP", "C: Connecting...");
        	    PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
        	    out.println(message);
        	    Log.e("TCP", "C: Sending: '" + message + "'");

        	    char[] line = new char[25];
        	    try {
        	    	Log.e("TCP", "1");
        	    	BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        	        while (true) {
        	            line = new char[25];
        	            in.read(line, 0, 25);
        	            if (line.length > 0) {
        	                String msg = new String(line);
        	                Log.e("TCP", "C: Receiving: '" + msg + "'");
        	                break;
        	            }
        	        }
        	    } catch (IOException e) {
        	        e.printStackTrace();
        	    }
        	    
        	    socket.close();
                Log.e("TCP", "socket is closed");
        	} catch (IOException ioe) {
                ioe.printStackTrace();
        	} finally {
                /* */
        	}
        }
    }

	/**
	 * resume 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		//brightStarRenderer.onResume();
		mGLSurfaceView.onResume();
		if (ledon)
		{
			/* onResume()時呼叫wakeLock() */
			wakeLock();
		}
    	if (sensor)
    	{
            mSm.registerListener(this, 
                SensorManager.SENSOR_ACCELEROMETER | 
                SensorManager.SENSOR_MAGNETIC_FIELD | 
                SensorManager.SENSOR_ORIENTATION
                ,
                SensorManager.SENSOR_DELAY_FASTEST);
    	}
	}

	/**
	 * pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
        if (ledon)
        {
        	/* onPause()時呼叫wakeUnlock() */
        	wakeUnlock();
        }
		mGLSurfaceView.onPause();
	}

    @Override
    protected void onStop() {
    	if (sensor)
    	{
            mSm.unregisterListener(this);
    	}
        super.onStop();
    }
	
    /* 喚起WakeLock 的方法 */
    private void wakeLock()
    {
        if (!ifLocked)
        {
           //setBrightness(255);
           ifLocked = true;
           mWakeLock.acquire();
        }
        //setBrightness(255);
    }
    
    /* 釋放WakeLock 的方法 */
    private void wakeUnlock()
    {
        if (ifLocked)
        {
            mWakeLock.release();
            ifLocked = false;
            //setBrightness(mUserBrightness);
        }
    }

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, 1, 0, "AltAz Grid SW");
        menu.add(0, 2, 0, "RaDec Grid SW");
        menu.add(0, 3, 0, "Meridian SW");
        menu.add(0, 4, 0, "View Mode");
        return true;
    }
    
    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 1:
			if(brightStarRenderer.mGridVisible)
        		brightStarRenderer.mGridVisible = false;
        	else
        		brightStarRenderer.mGridVisible = true; 	
        	return true;
        case 2:
			if(brightStarRenderer.mGridRDVisible)
        		brightStarRenderer.mGridRDVisible = false;
        	else
        		brightStarRenderer.mGridRDVisible = true;
        	return true;
        case 3:
        	if(brightStarRenderer.mMeridianVisible)
        		brightStarRenderer.mMeridianVisible = false;
        	else
        		brightStarRenderer.mMeridianVisible = true;
        	return true;
        case 4:
        	if(mTouchMove)
        		mTouchMove = false;
        	else
        		mTouchMove = true;
        	return true;
        }
		return false;
    }
    
    /**
     * Handling TouchEvent, including move and click
     */
    public boolean onTouchEvent(final MotionEvent event){
		float x = event.getX();
		float y = event.getY();
		
    	//System.out.println("Touch under BS");
    	
    	//chick if it is a click on screen, show or don't show the zoom button.
    	if(event.getAction() == MotionEvent.ACTION_DOWN){
    		downX = event.getX();
    		downY = event.getY();
    		//System.out.println("Up");
    	}
    	if(event.getAction() == MotionEvent.ACTION_UP){
    		float upX = event.getX();
    		float upY = event.getY();
    		//System.out.println("Down");
    		float diffX = Math.abs(downX - upX);
    		float diffY = Math.abs(downY - upY);
    		if (diffX <= 6f && diffY <= 6f){
    			if(mZoomVisible){
    				mZoomVisible = false;
    				mZoom.setVisibility(View.INVISIBLE);
    			}
    			else{
    				mZoomVisible = true;
    				mZoom.setVisibility(View.VISIBLE);
    			}
    			System.out.println("upX:"+upX+" upY:"+upY);
    			//is windows Y coordinate ?
    			brightStarRenderer.selectObject(upX, brightStarRenderer.width - upY);
    			//try to catch touch alt and azi
    			float winX = upX - brightStarRenderer.width/2f;
    			float winY = -upY + brightStarRenderer.height/2f;
    			//System.out.println("winX:"+winX+" winY"+winY);
    			//System.out.println("fovy:"+brightStarRenderer.fovy);
    			double azi = CoordCal.cvWinXYtoAzi(-winX, winY, Math.toRadians(brightStarRenderer.lookupdown)
    											, Math.toRadians(brightStarRenderer.yrot), Math.toRadians(brightStarRenderer.fovy)
    											, brightStarRenderer.height, brightStarRenderer.width);
    			double alt = CoordCal.cvWinXYtoAlt(winX, winY, Math.toRadians(brightStarRenderer.lookupdown)
    											, Math.toRadians(brightStarRenderer.fovy)
    											, brightStarRenderer.height, brightStarRenderer.width);
    			
    			double dec = CoordCal.cvAAtoDec(alt, azi);
    			double ra = CoordCal.cvAAtoRA(alt, azi, dec, brightStarRenderer.t1.getLSTr());
    			float touchX = (float) CoordCal.cvRDtoX(ra, dec);
    			float touchY = (float) CoordCal.cvRDtoY(ra, dec);
    			float touchZ = (float) CoordCal.cvRDtoZ(dec);
    			brightStarRenderer.createCrossLine(touchX, touchY, touchZ);
    			brightStarRenderer.mCross = true;
    			System.out.println("azi:"+Math.toDegrees(azi)+" alt:"+Math.toDegrees(alt));
    		}
    	}
    
		if(mTouchMove){
			//If a touch is moved on the screen
			if(event.getAction() == MotionEvent.ACTION_MOVE) {
				//Calculate the change
				float dx = x - brightStarRenderer.oldX;
				float dy = y - brightStarRenderer.oldY;
				if(Math.abs(dx) >= 6f || Math.abs(dy) >= 6f){
					//Up and down looking through touch
					brightStarRenderer.lookupdown += dy * brightStarRenderer.TOUCH_SCALE;
					if(brightStarRenderer.lookupdown >= 90.0f)
						brightStarRenderer.lookupdown = 89.9f;
					else if(brightStarRenderer.lookupdown < 0.0f)
						brightStarRenderer.lookupdown = 0f;
					
					//Look left and right through moving on screen
					//brightStarRenderer.heading += dx * brightStarRenderer.TOUCH_SCALE;
					//brightStarRenderer.yrot = brightStarRenderer.heading;
					brightStarRenderer.yrot += dx * brightStarRenderer.TOUCH_SCALE;
					while(brightStarRenderer.yrot >= 360.0f){
						brightStarRenderer.yrot -= 360.0f;
					}
					while(brightStarRenderer.yrot < 0f){
						brightStarRenderer.yrot += 360.0f;	
					}
					brightStarRenderer.azimuth = 360f - brightStarRenderer.yrot;
					//System.out.println("updown:"+brightStarRenderer.lookupdown+" yrot:"+brightStarRenderer.yrot);
					//calculate glulookat argument
					brightStarRenderer.eyeCenterCal();
					brightStarRenderer.eyeUpCal();
					
					//set new altitude and azimuth text
					altitude.setText("altitude:"+Float.toString(brightStarRenderer.lookupdown));
			        azimuth.setText("azimuth:"+Float.toString(brightStarRenderer.azimuth));
				}
			}
		}
        
        //Remember the values
		brightStarRenderer.oldX = x;
		brightStarRenderer.oldY = y;

		return true;
    	
	}
    
	OnClickListener mZoomInListener = new OnClickListener() {
		public void onClick(View v) {
			if(brightStarRenderer.fovy > 5)
					brightStarRenderer.fovy -= 5;
		}
	};
    
	OnClickListener mZoomOutListener = new OnClickListener() {
		public void onClick(View v) {
			if(brightStarRenderer.fovy < 120)
					brightStarRenderer.fovy += 5;
		}
	};
	
    public void onSensorChanged(int sensor, float[] values) {
        //Log.d(Integer.toString(sensor), "sensor: " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
        synchronized (this) {

        	if(!mTouchMove){
	            if (sensor == SensorManager.SENSOR_ORIENTATION) {
	            	//Log.d(Integer.toString(sensor), "sensor: " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
	            	//brightStarRenderer.yrot = values[0];
	            	if(Math.abs(orientaton[0] - values[0]) > 300.0)
	            		orientaton[0] = values[0];
	            	else
	            		orientaton[0] = values[0] * filterFactor + orientaton[0] * (1f - filterFactor);
	            	
	            	orientaton[1] = values[1] * filterFactor + orientaton[1] * (1f - filterFactor);
	            	
	            	brightStarRenderer.yrot = 360f - orientaton[0];
	            	brightStarRenderer.azimuth = orientaton[0];
	            	
	            	brightStarRenderer.lookupdown = -orientaton[1]-90f;
	            	if(brightStarRenderer.lookupdown >= 90.0f)
						brightStarRenderer.lookupdown = 89.9f;
					else if(brightStarRenderer.lookupdown < 0.0f)
						brightStarRenderer.lookupdown = 0f;
	            	
	            	brightStarRenderer.eyeCenterCal();
					brightStarRenderer.eyeUpCal();
	            	azimuth.setText("azimuth:"+Float.toString(brightStarRenderer.azimuth));
	            	altitude.setText("altitude:"+Float.toString(brightStarRenderer.lookupdown));
	            }
        	}
        	
        	mGLSurfaceView.invalidate();
        }
    }

    public void onAccuracyChanged(int sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }
    
    public Location getLocationProvider(LocationManager mLm) {
    	Location retLocation = null;
    	try {
    		Criteria mCriteria = new Criteria();
    		mCriteria.setAccuracy(Criteria.ACCURACY_FINE);
    		mCriteria.setAltitudeRequired(false);
    		mCriteria.setBearingRequired(false);
    		mCriteria.setCostAllowed(true);
    		mCriteria.setPowerRequirement(Criteria.POWER_LOW);
    		//List<String> providers = mLm.getAllProviders();
    		strLocationProvider = mLm.getBestProvider(mCriteria, true);
    		Log.e("GPS", "LocationProvider: " + strLocationProvider);
    		retLocation = mLm.getLastKnownLocation(strLocationProvider);
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	return retLocation;
    }

    public final LocationListener mLocationListener = new LocationListener()
    {
    	@Override
    	public void onLocationChanged(Location location) {
    		Log.e("GPS", location.toString());
    	}
    	@Override
    	public void onProviderDisabled(String provider) {
    		// TODO
    	}
    	@Override
    	public void onProviderEnabled(String provider) {
    		// TODO
    	}
    	@Override
    	public void onStatusChanged(String provider, int status, Bundle extras) {
    		// TODO
    	}
    };

////////////////////////////////////////////////////////////////////////
	/**
	 * Check for the DPad presses left, right, up and down.
	 * Walk in the according direction or rotate the "head".
	 * 
	 * @param keyCode - The key code
	 * @param event - The key event
	 * @return If the event has been handled
	 */
    /*
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//
		System.out.println("KeyPress");
		
		if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			brightStarRenderer.heading += 1.0f;	
			brightStarRenderer.yrot = brightStarRenderer.heading;					//Rotate The Scene To The Left
			System.out.println("LEFT!");
			
		} else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			brightStarRenderer.heading -= 1.0f;
			brightStarRenderer.yrot = brightStarRenderer.heading;					//Rotate The Scene To The Right
			
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			brightStarRenderer.xpos -= (float)Math.sin(brightStarRenderer.heading * brightStarRenderer.piover180) * 0.05f;	//Move On The X-Plane Based On Player Direction
			brightStarRenderer.zpos -= (float)Math.cos(brightStarRenderer.heading * brightStarRenderer.piover180) * 0.05f;	//Move On The Z-Plane Based On Player Direction
			
			if(brightStarRenderer.walkbiasangle >= 359.0f) {							//Is walkbiasangle>=359?
				brightStarRenderer.walkbiasangle = 0.0f;								//Make walkbiasangle Equal 0
			} else {
				brightStarRenderer.walkbiasangle += 10;								//If walkbiasangle < 359 Increase It By 10
			}
			brightStarRenderer.walkbias = (float)Math.sin(brightStarRenderer.walkbiasangle * brightStarRenderer.piover180) / 20.0f;	//Causes The Player To Bounce
	
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			brightStarRenderer.xpos += (float)Math.sin(brightStarRenderer.heading * brightStarRenderer.piover180) * 0.05f;	//Move On The X-Plane Based On Player Direction
			brightStarRenderer.zpos += (float)Math.cos(brightStarRenderer.heading * brightStarRenderer.piover180) * 0.05f;	//Move On The Z-Plane Based On Player Direction
			
			if(brightStarRenderer.walkbiasangle <= 1.0f) {								//Is walkbiasangle<=1?
				brightStarRenderer.walkbiasangle = 359.0f;								//Make walkbiasangle Equal 359
			} else {
				brightStarRenderer.walkbiasangle -= 10;								//If walkbiasangle > 1 Decrease It By 10
			}
			brightStarRenderer.walkbias = (float)Math.sin(brightStarRenderer.walkbiasangle * brightStarRenderer.piover180) / 20.0f;	//Causes The Player To Bounce
		}else
			return false;
	
		//We handled the event
		return true;
	}
	*/

}