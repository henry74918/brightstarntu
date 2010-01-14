package tw.edu.ntu;

import javax.microedition.khronos.opengles.GL;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public class BrightStar extends Activity {
	
	private BrightStarRenderer brightStarRenderer;
	private GLSurfaceView mGLSurfaceView;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        brightStarRenderer = new BrightStarRenderer(this);
        setContentView(brightStarRenderer);
        //mGLSurfaceView = new GLSurfaceView(this);
        //mGLSurfaceView.setGLWrapper(new GLSurfaceView.GLWrapper() {
        //    public GL wrap(GL gl) {
        //        return new MatrixTrackingGL(gl);
        //    }});
        //mGLSurfaceView.setRenderer(new BrightStarRenderer(this));
        //setContentView(mGLSurfaceView);
    }
    
	/**
	 * resume 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		brightStarRenderer.onResume();
		//mGLSurfaceView.onResume();
	}

	/**
	 * pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		brightStarRenderer.onPause();
		//mGLSurfaceView.onPause();
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
        	return true;
        case 2:
        	return true;
        }
		return false;
    }
    
    public boolean onTouchEvent(final MotionEvent event){
    	System.out.println("Touch under BS");
		return true;
    	
    }
}