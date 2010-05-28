package tw.edu.ntu;

import java.io.BufferedWriter;
import java.io.IOException;
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
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;


@SuppressWarnings("deprecation")
public class BrightStar extends Activity implements SensorListener{
	
	private BrightStarRenderer brightStarRenderer;
	private GLSurfaceView mGLSurfaceView;
	private ZoomControls mZoom;
	private LinearLayout linearLayout;
	private static TextView julianDay, altitude, azimuth, pointAz, pointAlt;
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
	
	private double ra, dec;
	
	boolean ledon = true;
	boolean tcpip = true;
	boolean sensor = true;
	boolean location = true;
	boolean mTouchMove = false;
	
	private int mUserBrightness = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        //start up image
        Toast toast = new Toast(this);
        ImageView view = new ImageView(this);
        toast.setDuration(Toast.LENGTH_LONG);
        view.setImageResource(R.drawable.brightstars);
        toast.setView(view);
        toast.show();

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
        pointAz = (TextView) findViewById(R.id.pointAz);
        pointAlt = (TextView) findViewById(R.id.pointAlt);
        
        julianDay.setText((String) this.getResources().getText(R.string.julian_day) + Double.toString(brightStarRenderer.t1.getJD()));
        altitude.setText((String) this.getResources().getText(R.string.altitude) + Double.toString(round(brightStarRenderer.lookupdown)));
        azimuth.setText((String) this.getResources().getText(R.string.azimuth) + Double.toString(round(brightStarRenderer.yrot)));
        pointAz.setText((String) this.getResources().getText(R.string.point_az) + (String) this.getResources().getText(R.string.none));
        pointAlt.setText( (String) this.getResources().getText(R.string.point_alt) + (String) this.getResources().getText(R.string.none));
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
        	try {
        		mUserBrightness = Settings.System.getInt(getContentResolver(),
        		                  Settings.System.SCREEN_BRIGHTNESS);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        // Sensor service enabled
        if (sensor)
		{
			mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
		}
        //moveTeleScopeR(1.552107f, 0.25859f);
        //moveTeleScopeD(5,55,43,7,24,29);
        /*
        // TCP Connection with telescope enabled
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("MR08h00m00sMD00d00\'00\"")).start();
        	} catch (Exception e) {
                    // TODO
        	}
        }
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("GRD")).start();
        	} catch (Exception e) {
                    // TODO
        	}
        }
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("STOP")).start();
        	} catch (Exception e) {
                    // TODO
        	}
        }
        if (tcpip)
        {
        	try {
                new Thread(new TeleScopeConn("AST")).start();
        	} catch (Exception e) {
                    // TODO
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
            	
            	TimeCal.longi_d = mLocation.getLongitude();
            	CoordCal.lat_d = mLocation.getLatitude();
            	 
            	//Showing toast message if location is set.
            	Toast.makeText(this, "Set Location from network. Latitude:"+dLatitude+" Longigude:"+dLongitude, Toast.LENGTH_LONG).show();
            	 
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

    public void moveTeleScopeR(
    		double RA, double Dec) {
        if (tcpip)
        {
        	Log.e("TCP", "(RA="+Double.toString(RA)+",Dec="+Double.toString(Dec)+")");
        	while (Dec<0) Dec+=2*StrictMath.PI;
        	int RAh=(int)(12*RA/(StrictMath.PI));
        	int RAm=(int)((720*RA/(StrictMath.PI))-60*RAh);
        	int RAs=(int)((43200*RA/(StrictMath.PI))-3600*RAh-60*RAm);
        	float t=(float)(180*Dec/(StrictMath.PI)/2);
        	int Ded=(int)(t);
        	int Dem=(int)(60*(t-Ded));
        	int Des=(int)(3600*(t-Ded)-60*Dem);

        	moveTeleScopeD(RAh, RAm, RAs, Ded, Dem, Des);
        }
    }
    
    public void moveTeleScopeD(
    		int RAh, int RAm, int RAs,
    		int Ded, int Dem, int Des) {
        if (tcpip)
        {
	        String sRAh=(RAh<10?"0"+Integer.toString(RAh):Integer.toString(RAh));
	        String sRAm=(RAm<10?"0"+Integer.toString(RAm):Integer.toString(RAm));
	        String sRAs=(RAs<10?"0"+Integer.toString(RAs):Integer.toString(RAs));
	        String sDed=(Ded<10?"00"+Integer.toString(Ded):(Ded<100?"0"+Integer.toString(Ded):(Integer.toString(Ded))));
	        String sDem=(Dem<10?"0"+Integer.toString(Dem):Integer.toString(Dem));
	        String sDes=(Des<10?"0"+Integer.toString(Des):Integer.toString(Des));
	        String sCommand = "MR"+sRAh+"h"+sRAm+"m"+sRAs+"sMD"+sDed+"d"+sDem+"\'"+sDes+"\"";
	        Log.e("TCP", sCommand);
	        // "MR08h00m00sMD00d00\'00\""
	    	try {
	            new Thread(new TeleScopeConn(sCommand)).start();
	    	} catch (Exception e) {
	            // TODO
	    	}
        }
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
/*
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
        	                socket.close();
        	                Log.e("TCP", "socket is closed");
        	                break;
        	            }
        	        }
        	    } catch (IOException e) {
        	        e.printStackTrace();
        	    }
*/        	    
        	    socket.close();
                Log.e("TCP2h", "socket is closed");
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

        menu.add(0, 1, 0, R.string.menu_1);
        menu.add(0, 2, 0, R.string.menu_2);
        menu.add(0, 3, 0, R.string.menu_3);
        menu.add(0, 4, 0, R.string.menu_4);
        menu.add(0, 5, 0, R.string.menu_5);
        menu.add(0, 6, 0, R.string.menu_6);
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
        case 5:
        	if(brightStarRenderer.mConstellationVisible)
        		brightStarRenderer.mConstellationVisible = false;
        	else
        		brightStarRenderer.mConstellationVisible = true;
        case 6:
        	//sending ra, dec to telescope
        	moveTeleScopeR(ra, dec);
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
    			//is windows Y coordinate ?
    			brightStarRenderer.selectObject(upX, brightStarRenderer.width - upY);
    			//try to catch touch alt and azi
    			float winX = upX - brightStarRenderer.width/2f;
    			float winY = -upY + brightStarRenderer.height/2f;
    			double azi = CoordCal.cvWinXYtoAzi(-winX, winY, Math.toRadians(brightStarRenderer.lookupdown)
    											, Math.toRadians(brightStarRenderer.yrot), Math.toRadians(brightStarRenderer.fovy)
    											, brightStarRenderer.height, brightStarRenderer.width);
    			double alt = CoordCal.cvWinXYtoAlt(winX, winY, Math.toRadians(brightStarRenderer.lookupdown)
    											, Math.toRadians(brightStarRenderer.fovy)
    											, brightStarRenderer.height, brightStarRenderer.width);
    			
    			//azi = 2*Math.PI - azi;
    			double dec = CoordCal.cvAAtoDec(alt, azi);
    			double ra = CoordCal.cvAAtoRA(alt, azi, dec, brightStarRenderer.t1.getLSTr());
    			selectNearestStar(ra,dec);
    			//this.ra = ra;
    			//this.dec = dec;
    			float touchX = (float) CoordCal.cvRDtoX(this.ra, this.dec);
    			float touchY = (float) CoordCal.cvRDtoY(this.ra, this.dec);
    			float touchZ = (float) CoordCal.cvRDtoZ(this.dec);
    			//for display only
    			double dec1 = CoordCal.cvAAtoDec(alt, 2*Math.PI-azi);
    			double ra1 = CoordCal.cvAAtoRA(alt, 2*Math.PI-azi, dec1, brightStarRenderer.t1.getLSTr());
    			while(ra1 >= 2*Math.PI)
    				ra1 -= 2*Math.PI;
    			float zzz = (float) Math.toDegrees(2*Math.PI-azi);
    			while(zzz > 360f)
    				zzz -= 360f;
    			pointAlt.setText((String) this.getResources().getText(R.string.point_alt)+Double.toString(round(Math.toDegrees(alt))));
    			pointAz.setText((String) this.getResources().getText(R.string.point_az)+Double.toString(round(zzz)));
    			brightStarRenderer.createCrossLine(touchX, touchY, touchZ);
    			brightStarRenderer.mCross = true;
    			System.out.println("alt:"+Math.toDegrees(alt)+" azi:"+Math.toDegrees(azi));
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
					altitude.setText((String) this.getResources().getText(R.string.altitude)+Double.toString(round(brightStarRenderer.lookupdown)));
			        azimuth.setText((String) this.getResources().getText(R.string.azimuth)+Double.toString(round(brightStarRenderer.azimuth)));
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
	            	
	            	if(values[1] <= 0.0)
	            		orientaton[1] = values[1] * filterFactor + orientaton[1] * (1f - filterFactor);
	            	
	            	brightStarRenderer.yrot = 360f - orientaton[0];
	            	brightStarRenderer.azimuth = orientaton[0];
	            	
	            	brightStarRenderer.lookupdown = -orientaton[1]-90f;
	            	if(brightStarRenderer.lookupdown >= 90.0f)
						brightStarRenderer.lookupdown = 89.9f;
					else if(brightStarRenderer.lookupdown <= -90f)
						brightStarRenderer.lookupdown = -89f;
	            	
	            	brightStarRenderer.eyeCenterCal();
					brightStarRenderer.eyeUpCal();
					altitude.setText((String) this.getResources().getText(R.string.altitude)+Double.toString(round(brightStarRenderer.lookupdown)));
	            	azimuth.setText((String) this.getResources().getText(R.string.azimuth)+Double.toString(round(brightStarRenderer.azimuth)));
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

    public void selectNearestStar(double ra, double dec)
    {
    	double dRa;
    	double dDec;
    	double dSum = 20000.0;
    	int NearestStar = 0;
    	for(int i=0;i < brightStarRenderer.nrOfStarObjects;i++){
    		dRa = Math.pow(ra  - brightStarRenderer.reader.getSAO(i).getRa(), 2.0);
    		dDec = Math.pow(dec - brightStarRenderer.reader.getSAO(i).getDec(), 2.0);
    		if(dSum > (dRa+dDec)){
    			dSum = dRa+dDec;
    			NearestStar = i;
    		}
    	}
    	this.ra =  brightStarRenderer.reader.getSAO(NearestStar).getRa();
    	this.dec = brightStarRenderer.reader.getSAO(NearestStar).getDec();
    }
    
    public double round(double value){
    	return Math.round(value*100)/100.0;
    }

}