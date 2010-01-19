package tw.edu.ntu;

import javax.microedition.khronos.opengles.GL;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomControls;

public class BrightStar extends Activity {
	
	private BrightStarRenderer brightStarRenderer;
	private GLSurfaceView mGLSurfaceView;
	private ZoomControls mZoom;
	private LinearLayout linearLayout;
	private static TextView julianDay, altitude, azimuth;
	private float downX;
	private float downY;
	boolean mZoomVisible = false;
	
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

    }
    
	/**
	 * resume 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		//brightStarRenderer.onResume();
		mGLSurfaceView.onResume();
	}

	/**
	 * pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		//brightStarRenderer.onPause();
		mGLSurfaceView.onPause();
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

        menu.add(0, 1, 0,"menu_start");
        menu.add(0, 2, 0, "menu_stop");

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
        	//linearLayout.addView(mZoom);
        	return true;
        case 2:
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
    		if (diffX <= 3f && diffY <= 3f){
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
    		}
    	}
    
		
		//If a touch is moved on the screen
		if(event.getAction() == MotionEvent.ACTION_MOVE) {
			//Calculate the change
			float dx = x - brightStarRenderer.oldX;
			float dy = y - brightStarRenderer.oldY;
			
			if(Math.abs(dx) >= 2f || Math.abs(dy) >= 2f){
				//Up and down looking through touch
				brightStarRenderer.lookupdown += dy * brightStarRenderer.TOUCH_SCALE;
				if(brightStarRenderer.lookupdown >= 90.0f)
					brightStarRenderer.lookupdown = 89.9f;
				else if(brightStarRenderer.lookupdown <= -90.0f)
					brightStarRenderer.lookupdown = -89.9f;
				
				//Look left and right through moving on screen
				brightStarRenderer.heading += dx * brightStarRenderer.TOUCH_SCALE;
				brightStarRenderer.yrot = brightStarRenderer.heading;
				while(brightStarRenderer.yrot >= 360.0f){
					brightStarRenderer.yrot -= 360.0f;
				}
				while(brightStarRenderer.yrot < 0f){
					brightStarRenderer.yrot += 360.0f;	
				}
				
				//System.out.println("updown:"+brightStarRenderer.lookupdown+" yrot:"+brightStarRenderer.yrot);
				//calculate glulookat argument
				brightStarRenderer.eyeCenterCal();
				brightStarRenderer.eyeUpCal();
				
				//set new altitude and azimuth text
				altitude.setText("altitude:"+Float.toString(brightStarRenderer.lookupdown));
		        azimuth.setText("azimuth:"+Float.toString(brightStarRenderer.yrot));
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
			//System.out.println("ZoomIn");
		}
	};
    
	OnClickListener mZoomOutListener = new OnClickListener() {
		public void onClick(View v) {
			if(brightStarRenderer.fovy < 120)
					brightStarRenderer.fovy += 5;
			//System.out.println("ZoomOut");
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