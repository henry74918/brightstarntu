package tw.edu.ntu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;

public class BrightStarRenderer extends GLSurfaceView implements Renderer, OnKeyListener, OnTouchListener{

	private Context context;
	/** Is blending enabled */
	private boolean blend = true;
	/** Is twinkle enabled */
	private boolean twinkle = false;
	
	int nrOfStarObjects;
	int width;
	int height;
	/*
	 * The following values are new values, used
	 * to navigate the world and heading
	 */
	private final float piover180 = 0.0174532925f;
	private float heading;
	private float xpos;
	private float zpos;
	private float yrot = 0;	 				//Y Rotation
	private float walkbias = 0;
	private float walkbiasangle = 0;
	private float lookupdown = 0.0f; 
	private float scale = 3.0f;			//scale the university
	private float centerX, centerY, centerZ;
	private float upX, upY, upZ;
	private double cenAltitude, cenAzimuth;
	
	/* Variables and factor for the input handler */
	private float oldX;
    private float oldY;
	private final float TOUCH_SCALE = 0.3f;//0.2f;			//Proved to be good for normal rotation
	
	private SAORead reader;
	private TimeCal t1;
	private Stars stars;

/*adding variables*/
	/** raw buffers to hold the index*/
	private ShortBuffer[] indexBuffer;
	
	/** raw buffers to hold the vertices*/
	private FloatBuffer[] vertexBuffer;

	/** raw buffers to hold the colors*/
	private FloatBuffer[] colorBuffer;
	
	/** The buffer holding the vertices */
	private FloatBuffer[] textureBuffer;
	
	/** The buffer holding the vertices */
	private float[][] textureColor;
	
	private int[] textures = new int[1];
/*adding variables*/
	
	public BrightStarRenderer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		this.setRenderer(this);
		//Request focus
		this.requestFocus();
		this.setFocusableInTouchMode(true);
		
		//
		this.context = context;
		
		//Read Data from Raw file to Memory
		reader = new SAORead(this.context);
		reader.read();
		nrOfStarObjects = reader.getNrOfStars();
		
		//New a time object and set time to now
		t1 = new TimeCal();
        t1.setTimeToNow();
        
        //Calculate the initial 
        eyeCenterCal();
		eyeUpCal();
		//Set the world as listener to this view
		//this.setOnKeyListener(this);
		this.setOnTouchListener(this);
		
/*for test*/
		
		indexBuffer = new ShortBuffer[nrOfStarObjects];
		vertexBuffer = new FloatBuffer[nrOfStarObjects];
		colorBuffer = new FloatBuffer[nrOfStarObjects];
		textureBuffer = new FloatBuffer[nrOfStarObjects];
		textureColor = new float[nrOfStarObjects][];
		//Load coordination and color
		for (int i=0;i<nrOfStarObjects;i++){
			double RA = reader.getSAO(i).getRa();
			double Dec = reader.getSAO(i).getDec();
			double Mag = reader.getSAO(i).getMagnitude();
			byte Spec = reader.getSAO(i).getSpec();
			
			init3DStar(i, (float)Math.cos(RA)*(float)Math.cos(Dec),
						(float)Math.sin(RA)*(float)Math.cos(Dec),
						(float)Math.sin(Dec), (float)Mag/100, Spec);
        }
        
/*for test*/
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub
		//Clear Screen And Depth Buffer
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);	
		
		gl.glLoadIdentity();					//Reset The Current Modelview Matrix
		
		//Check if the blend flag has been set to enable/disable blending
		
		if(blend) {
			gl.glEnable(GL10.GL_BLEND);			//Turn Blending On
			gl.glDisable(GL10.GL_DEPTH_TEST);	//Turn Depth Testing Off
			
		} else {
			gl.glDisable(GL10.GL_BLEND);		//Turn Blending On
			gl.glEnable(GL10.GL_DEPTH_TEST);	//Turn Depth Testing Off
		}
		
		//
		float xtrans = -xpos;						//Used For Player Translation On The X Axis
		float ztrans = -zpos;						//Used For Player Translation On The Z Axis
		float ytrans = -walkbias - 0.25f;			//Used For Bouncing Motion Up And Down
		float sceneroty = 360.0f - yrot;			//360 Degree Angle For Player Direction
		
		//View
		//gl.glRotatef(-lookupdown, 1.0f, 0, 0);		//Rotate Up And Down To Look Up And Down
		//gl.glRotatef(sceneroty, 0, 1.0f, 0);		//Rotate Depending On Direction Player Is Facing
		//gl.glTranslatef(xtrans, ytrans, ztrans);	//Translate The Scene Based On Player Position
		
		//use glulookat to rotate our view
		
		//double upDec = Math.toRadians(90) + dec;
		//double upRa = sceneroty + Math.toRadians(180);
		//float upX = (float) CoordCal.cvRD2X(upRa, upDec);
		//float upY = (float) CoordCal.cvRD2Y(upRa, upDec);
		//float upZ = (float) CoordCal.cvRD2Z(upDec);
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
		GLU.gluLookAt(gl, 0f, 0f, 0f, centerX, centerY, centerZ, upX, upY, upZ);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		//System.out.println("centerX:"+centerX+" centerY:"+centerY+" centerZ:"+centerZ);
		//System.out.println("upX:"+upX+" upY:"+upY+" upZ:"+upZ);
		//scale the hole university
		gl.glScalef(scale, scale, scale);
		//
/*star draw*/
		//stars.draw(gl, twinkle);
/*star draw*/		
		
/*for test*/		
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		for (int i=0; i < nrOfStarObjects; i++){
			gl.glColor4f(textureColor[i][0], textureColor[i][1], textureColor[i][2], 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer[i]);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer[i]);
			//gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer[i]);
			//gl.glMatrixMode(GL10.GL_PROJECTION);
			//gl.glLoadIdentity();
			//GLU.gluLookAt(gl, 0f, 0f, 0f, 0f, (float)Math.sin(Math.toRadians((double)i*0.1)), (float)-Math.cos(Math.toRadians((double)i*0.1)), 0f, (float)Math.cos(Math.toRadians((double)i*0.1)), (float)Math.sin(Math.toRadians((double)i*0.1)));
			//gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
			//twinkle
			//gl.glColor4f(textureColor[i][0], textureColor[i][1], textureColor[i][2], 0.0f);
			//gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
			//gl.glDrawArrays(GL10.GL_POINTS, 0, 1);
		}
/*for test*/
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		this.width = width;
		this.height = height;
		
		if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
		GLU.gluLookAt(gl, 0f, 0f, 0f, centerX, centerY, centerZ, upX, upY, upZ);
		//GLU.gluLookAt(gl, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f);
		System.out.println("OOOOOOOOOOOOOOOOOOOOO");
		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		//Settings
		gl.glDisable(GL10.GL_DITHER);						//Disable dithering
		gl.glEnable(GL10.GL_TEXTURE_2D);					//Enable Texture Mapping
		gl.glShadeModel(GL10.GL_SMOOTH); 					//Enable Smooth Shading
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 			//Black Background
		gl.glClearDepthf(1.0f); 							//Depth Buffer Setup
		//gl.glDepthFunc(GL10.GL_LEQUAL); 					//The Type Of Depth Testing To Do
		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); //or change to fastest
		
		//Blend
		gl.glEnable(GL10.GL_BLEND);							//Enable blending
		gl.glDisable(GL10.GL_DEPTH_TEST);					//Disable depth test
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);		//Set The Blending Function For Translucency		

		//loading stars
		//create star data form SAO array
/*create stars*/
		stars = new Stars(nrOfStarObjects, reader.getSAOAll());
		System.out.println("nofstars:" + nrOfStarObjects);
		stars.loadGLTexture(gl, this.context);
		//gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
/*create stars*/
		
/*for test*/
		loadGLTexture(gl, this.context);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		//gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
/*for test*/
	}
	

/* ***** Listener Events ***** */	
	/**
	 * Override the key listener to receive onKey events.
	 *  
	 */
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		//Handle key down events
		if(event.getAction() == KeyEvent.ACTION_DOWN) {
			return onKeyDown(keyCode, event);
		}
		
		return false;
	}

	/**
	 * Check for the DPad presses left, right, up and down.
	 * Walk in the according direction or rotate the "head".
	 * 
	 * @param keyCode - The key code
	 * @param event - The key event
	 * @return If the event has been handled
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//
		if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			heading += 1.0f;	
			yrot = heading;					//Rotate The Scene To The Left
			System.out.println("LEFT!");
			
		} else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			heading -= 1.0f;
			yrot = heading;					//Rotate The Scene To The Right
			
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			xpos -= (float)Math.sin(heading * piover180) * 0.05f;	//Move On The X-Plane Based On Player Direction
			zpos -= (float)Math.cos(heading * piover180) * 0.05f;	//Move On The Z-Plane Based On Player Direction
			
			if(walkbiasangle >= 359.0f) {							//Is walkbiasangle>=359?
				walkbiasangle = 0.0f;								//Make walkbiasangle Equal 0
			} else {
				walkbiasangle += 10;								//If walkbiasangle < 359 Increase It By 10
			}
			walkbias = (float)Math.sin(walkbiasangle * piover180) / 20.0f;	//Causes The Player To Bounce
	
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			xpos += (float)Math.sin(heading * piover180) * 0.05f;	//Move On The X-Plane Based On Player Direction
			zpos += (float)Math.cos(heading * piover180) * 0.05f;	//Move On The Z-Plane Based On Player Direction
			
			if(walkbiasangle <= 1.0f) {								//Is walkbiasangle<=1?
				walkbiasangle = 359.0f;								//Make walkbiasangle Equal 359
			} else {
				walkbiasangle -= 10;								//If walkbiasangle > 1 Decrease It By 10
			}
			walkbias = (float)Math.sin(walkbiasangle * piover180) / 20.0f;	//Causes The Player To Bounce
		}else
			return false;
	
		//We handled the event
		return true;
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		boolean handled = false;
		//System.out.println("OnTouch");
		//
		float x = event.getX();
		float y = event.getY();
		
		//If a touch is moved on the screen
		if(event.getAction() == MotionEvent.ACTION_MOVE) {
			//Calculate the change
			float dx = x - oldX;
			float dy = y - oldY;
			        		
			//Up and down looking through touch
			lookupdown += dy * TOUCH_SCALE;
			if(lookupdown > 90.0f)
				lookupdown = 90.0f;
			else if(lookupdown < -90.0f)
				lookupdown = -90.0f;
			//Look left and right through moving on screen
			heading += dx * TOUCH_SCALE;
			yrot = heading;
			
			if(yrot > 360.0f)
				yrot -= 360.0f;
			else if(yrot < 0f)
				yrot += 360.0f;
			
			
			//calculate glulookat argument
			eyeCenterCal();
			eyeUpCal();
			//We handled the event
			handled = true;
		}
        
        //Remember the values
        oldX = x;
        oldY = y;

		return handled;
	}
	
	/**
	 * Override the touch screen listener.
	 * 
	 * React to moves and presses on the touchscreen.
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		//System.out.println("OnTouchEvent");
		return true;
	}
    


	private void init3DStar(int id, float x, float y, float z, float Mag, byte Spec) {
		float magnitude = 0.0f;
		short color = 0;
		
		if (Mag>=3 && Mag<4) {
			magnitude = 0.004f;
		} else if (Mag>=2 && Mag<3){
			magnitude = 0.006f;			
		} else if (Mag>=1 && Mag<2){
			magnitude = 0.008f;
		} else if (Mag>=0 && Mag<1){
			magnitude = 0.01f;
		} else {
			magnitude = 0.015f;
		}
		float uLen = (float) Math.sqrt(x*x+y*y+z*z);
		float[] u = {x/uLen, y/uLen, z/uLen};
		float vLen = (float) Math.sqrt(1+1+((-x-y)/z)*((-x-y)/z));
		float[] v = {1f/vLen, 1f/vLen, ((-x-y)/z)/vLen};
		//float a = (float) (Math.pow(u[2]*v[3]-u[3]*v[2], 2.0));
		float sLen =(float) Math.sqrt(Math.pow(u[1]*v[2]-u[2]*v[1], 2)+Math.pow(u[2]*v[0]-u[0]*v[2], 2)+Math.pow(u[0]*v[1]-u[1]*v[0], 2));
		float[] s ={ (u[1]*v[2]-u[2]*v[1])/sLen, (u[2]*v[0]-u[0]*v[2])/sLen, (u[0]*v[1]-u[1]*v[0])/sLen};
		
		for(int i =0;i<3;i++){
			v[i] = v[i]*0.1f;
			s[i] = s[i]*0.1f;
		}
		x = x*8f;
		y = y*8f;
		z = z*8f;
		
		float[] coords = {
				//x*1f, y*1f, z*1f,
				x-v[0]-s[0], y-v[1]-s[1], z-v[2]-s[2],
				x+v[0]-s[0], y+v[1]-s[1], z+v[2]-s[2],
				x-v[0]+s[0], y-v[1]+s[1], z-v[2]+s[2],
				x+v[0]+s[0], y+v[1]+s[1], z+v[2]+s[2],
				//x-0.1f, y-0.1f, z, 		//Bottom Left
				//x+0.1f, y-0.1f, z, 		//Bottom Right
				//x-0.1f, y+0.1f, z,	 	//Top Left
				//x+0.1f, y+0.1f, z 		//Top Right
		};
		//System.out.println("x:"+x+" y:"+y+" z:"+z) ;
	
		
		switch (Spec){
		case 'O':
			color = 1;
			break;
		case 'B':
			color = 2;
			break;
		case 'A':
			color = 3;
			break;
		case 'F':
			color = 4;
			break;
		case 'G':
			color = 5;
			break;
		case 'K':
			color = 6;
			break;
		case 'M':
			color = 7;
			break;
		default:
			color = 0;
			break;
		}
		
		float[][] colors = {{
				1f, 1f, 1f, 1f, // default color is white
			},{
				162f/255f, 190f/255f, 255f/255f, 1f,//O
				//0.633f, 0.742f, 1.0f, 1.0f,
			},{                
				220f/255f, 231f/255f, 255f/255f, 1f,//B
				//0.859f, 0.902f, 1.0f, 1.0f,
			},{
				255f/255f, 255f/255f, 255f/255f, 1f,//A
				//1.0f, 1.0f, 1.0f, 1.0f,
			},{
				252f/255f, 255f/255f, 229f/255f, 1f,//F
				//0.984f, 1.0f, 0.895f, 1.0f,
			},{
				255f/255f, 253f/255f, 202f/255f, 1f,//G
				//1.0f, 0.988f, 0.789f, 1.0f,
			},{
				255f/255f, 206f/255f,  74f/255f, 1f,//K
				//1.0f, 0.805f, 0.289f, 1.0f,
			},{
				253f/255f, 127f/255f,  58f/255f, 1f,//M
				//0.988f, 0.496f, 0.227f, 1.0f,
			}
		};
	
		short[] indices = new short[] {
			0, 1, 3,
			0, 2, 1,
			0, 3, 2,
			1, 2, 3,
		};
		
		/** The initial texture coordinates (u, v) */	
		float texture[] = {
									0.0f, 0.0f, 
									1.0f, 0.0f, 
									0.0f, 1.0f, 
									1.0f, 1.0f,
												};
	
		// float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(coords.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer[id] = vbb.asFloatBuffer();
		vertexBuffer[id].put(coords);
		vertexBuffer[id].position(0);
		 
		// short has 2 bytes
		ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		ibb.order(ByteOrder.nativeOrder());
		indexBuffer[id] = ibb.asShortBuffer();
		indexBuffer[id].put(indices);
		indexBuffer[id].position(0);
		
		// float has 4 bytes, 4 colors (RGBA) * number of vertices * 4 bytes
		ByteBuffer cbb = ByteBuffer.allocateDirect(colors[0].length * 4);
		cbb.order(ByteOrder.nativeOrder());
		colorBuffer[id] = cbb.asFloatBuffer();
		colorBuffer[id].put(colors[color]);	
		colorBuffer[id].position(0);
		
		//texture buffer
		ByteBuffer tb = ByteBuffer.allocateDirect(texture.length * 4);
		tb.order(ByteOrder.nativeOrder());
		textureBuffer[id] = tb.asFloatBuffer();
		textureBuffer[id].put(texture);
		textureBuffer[id].position(0);
		
		//set color array
		textureColor[id] = new float[4];
		textureColor[id] = colors[color];
	}

	/**
	 * Load the textures
	 * 
	 * @param gl - The GL Context
	 * @param context - The Activity context
	 */
	public void loadGLTexture(GL10 gl, Context context) {
		//Get the texture from the Android resource directory
		InputStream is = context.getResources().openRawResource(R.drawable.star);
		Bitmap bitmap = null;
		try {
			//BitmapFactory is an Android graphics utility for images
			bitmap = BitmapFactory.decodeStream(is);

		} finally {
			//Always clear and close
			try {
				is.close();
				is = null;
			} catch (IOException e) {
			}
		}

		//Generate there texture pointer
		gl.glGenTextures(1, textures, 0);

		//Create Linear Filtered Texture and bind it to texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		//require low processing power
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		
		//Clean up
		bitmap.recycle();
	}
	
	private void eyeCenterCal(){
		if(lookupdown == 90.0f)
			lookupdown = 89.99f;
		else if(lookupdown == -90.0f)
			lookupdown = -89.99f;
		
		System.out.println("Alt:"+lookupdown+" Azi:"+yrot);
		double cenAltitude = Math.toRadians(lookupdown);
		double cenAzimuth = Math.toRadians(yrot);
		double lstr = TimeCal.cvH2Radians(t1.getLSTh());
		
		double cenDec = CoordCal.cvAAtoDec(cenAltitude, cenAzimuth);
		double cenRa = CoordCal.cvAAtoRA(cenAltitude, cenAzimuth, cenDec, lstr);
		centerX = (float) CoordCal.cvRDtoX(cenRa, cenDec);
		centerY = (float) CoordCal.cvRDtoY(cenRa, cenDec);
		centerZ = (float) CoordCal.cvRDtoZ(cenDec);
	}
	
	private void eyeUpCal(){
		if(lookupdown == 90.0f)
			lookupdown = 89.99f;
		else if(lookupdown == -90.0f)
			lookupdown = -89.99f;
		
		double upAltitude = Math.toRadians(90 + lookupdown);
		double upAzimuth = Math.toRadians(yrot + 180);
		double lstr = TimeCal.cvH2Radians(t1.getLSTh());
		
		if(upAzimuth > Math.toRadians(2*Math.PI))
			upAzimuth -= 2*Math.PI;
		
		double upDec = CoordCal.cvAAtoDec(upAltitude, upAzimuth);
		//System.out.println("upAltitide:"+upAltitude+" upAzimuth:"+upAzimuth+" lstr:"+lstr);
		double upRa = CoordCal.cvAAtoRA(upAltitude, upAzimuth, upDec, lstr);
		//System.out.println("upRa:"+upRa+" upDec:"+upDec);
		upX = (float) CoordCal.cvRDtoX(upRa, upDec);
		upY = (float) CoordCal.cvRDtoY(upRa, upDec);
		upZ = (float) CoordCal.cvRDtoZ(upDec);
	}
}