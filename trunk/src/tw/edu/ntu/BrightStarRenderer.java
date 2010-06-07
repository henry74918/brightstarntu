package tw.edu.ntu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

public class BrightStarRenderer extends GLSurfaceView implements Renderer {

	private Context context;
	/** Is blending enabled */
	private boolean blend = true;
	/** Is twinkle enabled */
	//private boolean twinkle = false;
	
	int nrOfSolarObjects;
	int nrOfStarObjects;
	int nrOfObjects;
	int nrOfStrings;
	int width;
	int height;
	/*
	 * The following values are new values, used
	 * to navigate the world and heading
	 */
	protected final float piover180 = 0.0174532925f;
	protected final float TOUCH_SCALE = 0.3f;//0.2f;			//Proved to be good for normal rotation
	
	protected float heading;
	protected float xpos;
	protected float zpos;
	protected float yrot = 0;	 				//Y Rotation
	protected float azimuth;
	protected float walkbias = 0;
	protected float walkbiasangle = 0;
	protected float lookupdown = 0.0f; 
	protected float fovy = 90.0f;
	protected float oldX;
	protected float oldY;
	protected TimeCal t1;
	protected SAORead reader;
	protected boolean mGridVisible = true;
	protected boolean mGridRDVisible = false;
	protected boolean mMeridianVisible = true;
	protected boolean mCross = false;
	protected boolean mConstellationVisible = true;
	
	private final int CIRCLE_DEGREE = 5;
	private final int LINE_DEGREE = 5;
	private final int HORIZON_CIRCLE_INTERVAL = 10;
	private final int VERTICAL_LINE_INTERVAL = 15;
	private final int NUM_OF_HORIZON_CIRCLES = (int) ((180f/HORIZON_CIRCLE_INTERVAL) - 1);
	private final int NUM_OF_VERTICAL_LINES = (int) (360f / VERTICAL_LINE_INTERVAL);
	private final int NUM_OF_CIRCLE_VERTICES = (int) (360f/CIRCLE_DEGREE);
	private final int NUM_OF_LINE_VERTICES = (int) ((160f/LINE_DEGREE) +1);
	private final int NUM_OF_MERIDIAN_LINE_VERTICES = (int) ((180f/LINE_DEGREE) + 1);
	private final float coordinateScale = 100.0f;			//scale of the X,Y,Z axis
	private float textureScale = 2.0f;				//scale of the texture size
	private float centerX, centerY, centerZ;
	private float upX, upY, upZ;
	
	//private Stars stars;
	private MatrixGrabber mGrabber;
	//private TextView julianDay;

/*adding variables*/
	/** raw buffers to hold the index*/
	private ShortBuffer[] indexBuffer;
	
	/** raw buffers to hold the vertices*/
	private FloatBuffer[] vertexBuffer;
	private FloatBuffer[] vertexLineBuffer;

	/** raw buffers to hold the colors*/
	private FloatBuffer[] colorBuffer;
	
	/** The buffer holding the vertices */
	private FloatBuffer[] textureBuffer;
	
	/** The buffer holding the vertices */
	private float[][] textureColor;
	private FloatBuffer[] HorizonLineBuffer;
	private FloatBuffer[] HorizonRDLineBuffer;
	private FloatBuffer[] VerticalLineBuffer;
	private FloatBuffer[] VerticalRDLineBuffer;
	private FloatBuffer[] MeridianLineBuffer;
	private FloatBuffer CrossLineBuffer;
	private float[] magnitude;
	
	private int[] textures = new int[1];
	
    private Random rand = new Random((long)0.1);

	private int[] nrOfVertices;
	
	private ByteBuffer sb;
	private FloatBuffer fb;
    /*adding variables*/
	
	boolean solar_sim = true;

	public BrightStarRenderer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		/*
		this.setRenderer(this);
		Request focus
		this.requestFocus();
		this.setFocusableInTouchMode(true);
		this.setOnKeyListener(this);
		this.setOnTouchListener(this);
		 */
		this.context = context;
		
		//Read Data from Raw file to Memory
		reader = new SAORead(this.context);
		reader.read();

		nrOfSolarObjects = 0;
		nrOfStarObjects = reader.getNrOfStars();
		nrOfObjects = nrOfSolarObjects + nrOfStarObjects;
		nrOfStrings = 168;
		
		
		//New a time object and set time to now
		t1 = new TimeCal();
        t1.setTimeToNow();
        
        magnitude = new float[nrOfObjects];
        mGrabber = new MatrixGrabber();
        //Set julian Day
        //Calculate the initial 
        eyeCenterCal();
		eyeUpCal();

/*for test*/
		
		indexBuffer = new ShortBuffer[nrOfObjects];
		vertexBuffer = new FloatBuffer[nrOfObjects];
		colorBuffer = new FloatBuffer[nrOfObjects];
		textureBuffer = new FloatBuffer[nrOfObjects];
		textureColor = new float[nrOfObjects][];
		nrOfVertices = new int[nrOfSolarObjects];
		
		vertexLineBuffer = new FloatBuffer[nrOfStrings];
		
		//Load coordination and color for solar objects
	    //init3DSolar(0, 5f, 0f, 0f, 1f, "Sun" );
	    //init3DSolar(0, 0.8f, -0.7f, -0.3f, 0.1f, "Moon" );
	    //init3DSolar(0, -0.8f, -0.6f, 0.0f, 0.1f, "Earth" );

	    //Load coordination and color for star objects
		for (int i=nrOfSolarObjects;i<nrOfObjects;i++){
			double RA = reader.getSAO(i-nrOfSolarObjects).getRa();
			double Dec = reader.getSAO(i-nrOfSolarObjects).getDec();
			double Mag = reader.getSAO(i-nrOfSolarObjects).getMagnitude();
			byte Spec = reader.getSAO(i-nrOfSolarObjects).getSpec();
			
			init3DStar(i, (float)Math.cos(RA)*(float)Math.cos(Dec),
						(float)Math.sin(RA)*(float)Math.cos(Dec),
						(float)Math.sin(Dec), (float)Mag/100, Spec);
        }
        //load GRID into memory
		createHorizonLine();
        createVerticalLine();
        //load GRID RD into memory
        createHorizonRDLine();
        createVerticalRDLine();
        //
        createMeridianLine();
        initConstellations();
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
		//float xtrans = -xpos;						//Used For Player Translation On The X Axis
		//float ztrans = -zpos;						//Used For Player Translation On The Z Axis
		//float ytrans = -walkbias - 0.25f;			//Used For Bouncing Motion Up And Down
		//float sceneroty = 360.0f - yrot;			//360 Degree Angle For Player Direction
		
		//View
		//gl.glRotatef(-lookupdown, 1.0f, 0, 0);		//Rotate Up And Down To Look Up And Down
		//gl.glRotatef(sceneroty, 0, 1.0f, 0);		//Rotate Depending On Direction Player Is Facing
		//gl.glTranslatef(xtrans, ytrans, ztrans);	//Translate The Scene Based On Player Position
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, fovy, (float)width / (float)height, 0.1f, 100.0f);
		GLU.gluLookAt(gl, 0f, 0f, 0f, centerX, centerY, centerZ, upX, upY, upZ);
		mGrabber.getCurrentProjection(gl);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		mGrabber.getCurrentModelView(gl);
		//System.out.println("project:"+mGrabber.mProjection);
		//scale the hole university
		//gl.glLoadIdentity();
		//gl.glScalef(scale, scale, scale);
		//

/*star draw*/
		//stars.draw(gl, twinkle);
/*star draw*/		
		
		//draw Grid line
		if(mGridVisible){
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 0.0f, 0.0f, 0.3f);
			for(int i = 0; i < NUM_OF_HORIZON_CIRCLES;i++){
				if(i == (NUM_OF_HORIZON_CIRCLES-1)/2 ){
					gl.glColor4f(0.0f, 1.0f, 0.0f, 0.3f);
					gl.glVertexPointer(3, GL10.GL_FLOAT, 0, HorizonLineBuffer[i]);
					gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, NUM_OF_CIRCLE_VERTICES);
					gl.glColor4f(1.0f, 0.0f, 0.0f, 0.3f);
				}
				else{
					gl.glVertexPointer(3, GL10.GL_FLOAT, 0, HorizonLineBuffer[i]);
					gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, NUM_OF_CIRCLE_VERTICES);
				}
			}
			for(int i = 0; i < NUM_OF_VERTICAL_LINES;i++){
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, VerticalLineBuffer[i]);
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, NUM_OF_LINE_VERTICES);
			}
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}
		//draw Grid RD line
		if(mGridRDVisible){
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glColor4f(0.0f, 0.0f, 1.0f, 0.3f);
			for(int i = 0; i < NUM_OF_HORIZON_CIRCLES;i++){
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, HorizonRDLineBuffer[i]);
				gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, NUM_OF_CIRCLE_VERTICES);
			}
			for(int i = 0; i < NUM_OF_VERTICAL_LINES;i++){
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, VerticalRDLineBuffer[i]);
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, NUM_OF_LINE_VERTICES);
			}
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}
		
		//draw Meridian line
		if(mMeridianVisible){
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
			for(int i = 0; i < 2;i++){
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, MeridianLineBuffer[i]);
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, NUM_OF_MERIDIAN_LINE_VERTICES);
			}
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}
		if(mConstellationVisible){
			for (int i=0;i<nrOfStrings;i++)
			{
				gl.glDisable(GL10.GL_TEXTURE_2D);
				gl.glColor4f(1.0f, 1.0f, 0.0f, 0.7f);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexLineBuffer[i]);
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertexLineBuffer[i].capacity()/3);
				gl.glEnable(GL10.GL_TEXTURE_2D);
			}
		}
/*for test*/		
/*
        for (int i=0; i< nrOfSolarObjects; i++){
        	if (solar_sim){
                gl.glDisable(GL10.GL_TEXTURE_2D);
               	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer[i]);
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer[i]);
                gl.glDrawElements(GL10.GL_TRIANGLES, nrOfVertices[i], GL10.GL_UNSIGNED_SHORT, indexBuffer[i]); 
        	} else {
        		gl.glEnable(GL10.GL_TEXTURE_2D);
        		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer[i]);
                gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer[i]);
                gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, 922, 461, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, fb);
        	}
        }
*/
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		for (int i=nrOfSolarObjects; i < nrOfObjects; i++){
			gl.glColor4f(textureColor[i][0], textureColor[i][1], textureColor[i][2], 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer[i]);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer[i]);
			//gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer[i]);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		}
		
		//Draw Cross line
		
		if(mCross){
			gl.glDisable(GL10.GL_TEXTURE_2D);
			//gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);
			gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, CrossLineBuffer);
			//gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer[0]);
			gl.glDrawArrays(GL10.GL_LINES, 0, 8);
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}
		
		
		//twinkle
		//gl.glColor4f(textureColor[i][0], textureColor[i][1], textureColor[i][2], 0.0f);
		//gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		//gl.glDrawArrays(GL10.GL_POINTS, 0, 1);
/*for test*/
		
		//reset time to new
		//BrightStar.UpdateTextView(t1);
		//UpdateTextView();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		//tattoo is 240*320
		this.width = width;
		this.height = height;
		
		if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix
		gl.glLineWidth(2.0f);					//Set OpenGL linewidth

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, fovy, (float)width / (float)height, 0.1f, 100.0f);
		GLU.gluLookAt(gl, 0f, 0f, 0f, centerX, centerY, centerZ, upX, upY, upZ);
		mGrabber.getCurrentProjection(gl);
		//GLU.gluLookAt(gl, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f);
		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		//Settings
		gl.glDisable(GL10.GL_DITHER);						//Disable dithering
		//gl.glEnable(GL10.GL_TEXTURE_2D);					//Enable Texture Mapping
		gl.glShadeModel(GL10.GL_SMOOTH); 					//Enable Smooth Shading
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 			//Black Background
		gl.glClearDepthf(1.0f); 							//Depth Buffer Setup
		//gl.glDepthFunc(GL10.GL_LEQUAL); 					//The Type Of Depth Testing To Do
		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); //or change to fastest
		//line option
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);

		
		//Blend
		gl.glEnable(GL10.GL_BLEND);							//Enable blending
		gl.glDisable(GL10.GL_DEPTH_TEST);					//Disable depth test
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);		//Set The Blending Function For Translucency		

		//loading stars
		//create star data form SAO array
/*create stars*/
		//stars = new Stars(nrOfStarObjects, reader.getSAOAll());
		System.out.println("nofstars:" + nrOfStarObjects);
		//stars.loadGLTexture(gl, this.context);
		//gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
/*create stars*/
		
/*for test*/
		loadStarTexture(gl, this.context);
		if (!solar_sim) {
            loadSolarTexture(gl, this.context);
		}
        
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		//gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
/*for test*/
	}
	
    private void init3DSolar(int id, float x, float y, float z, float magnitude, String objectName) {
    	//assert(id <= _nrOfObjects);

    	int count=0;
    	// Can use following two spacing to adjust the granularity of the sphere.
    	int deltatheta = 10;
    	int deltaphi = 10;

    	int slicetheta = 360/deltatheta; 		//10-36, 20-18, 30-12
        int slicephi = 180/deltaphi; 			//10-18, 20-9 , 30-6
        int vertexphi = slicephi+1; 			//10-19, 20-10, 30-7
        int trianglephi = ((slicephi-2)*2)+2; 	//10-34, 20-16, 30-10

        int nCoord = slicetheta * vertexphi;	//10-36*19
        int nCoords = slicetheta*vertexphi*3; 	//10-36*19*(x,y,z)
        int nIndices = slicetheta*trianglephi*3;//10-36*34*(x,y,z)
        int nColors = slicetheta*trianglephi*4; //10-36*34*(r,g,b,a)

    	float[] coords = new float[nCoords]; 
        short[] indices = new short[nIndices]; 
        float[] colors = new float[nColors]; 

        //coords
        for( int theta = 0; theta < 360; theta+=deltatheta){ // from 0 to 2PI
            for( int phi = 90; phi >= -90; phi-=deltaphi){ // from PI/2 to -PI/2
            	float temp = 0;
            	temp = (float) (StrictMath.cos((float)(theta) / 180.0f * StrictMath.PI) * StrictMath.cos((float)(phi) / 180.0f * StrictMath.PI)) * magnitude;
            	if (StrictMath.abs(x)<0.0001f) {coords[count++] = 0;} else {coords[count++] = temp+x;}
            	temp = (float) (StrictMath.sin((float)(theta) / 180.0f * StrictMath.PI) * StrictMath.cos((float)(phi) / 180.0f * StrictMath.PI)) * magnitude;
            	if (StrictMath.abs(x)<0.0001f) {coords[count++] = 0;} else {coords[count++] = temp+y;}
            	temp = (float) (StrictMath.sin((float)(phi) / 180.0f * StrictMath.PI)) * magnitude;
            	if (StrictMath.abs(x)<0.0001f) {coords[count++] = 0;} else {coords[count++] = temp+z;}
            }
        }
        Log.e("COORDS",Integer.toString(count));

        //indices
        count=0;
        for (int i=0;i<slicetheta;i++){
        	indices[count++] = (short)(0);
        	indices[count++] = (short)( 1 + vertexphi*i);
        	indices[count++] = (short)(((vertexphi+1) + vertexphi*i)%nCoord);
        	for (int j=0;j<slicephi-2;j++) {
        		indices[count++] = (short)( 1+j + vertexphi*i);
        		indices[count++] = (short)( 2+j + vertexphi*i);
        		indices[count++] = (short)(((vertexphi+2)+j + vertexphi*i)%nCoord);

        		indices[count++] = (short)(((vertexphi+1)+j + vertexphi*i)%nCoord);
        		indices[count++] = (short)( 1+j + vertexphi*i);
        		indices[count++] = (short)(((vertexphi+2)+j + vertexphi*i)%nCoord);
        	}
        	indices[count++] = (short)((vertexphi-2) + vertexphi*i);
        	indices[count++] = (short)((vertexphi-1));
        	indices[count++] = (short)(((vertexphi*2-2) + vertexphi*i)%nCoord);
        }
        Log.e("INDICES",Integer.toString(count));
        
        //colors
        rand.setSeed((long)0.1);
        count=0;
        for( int i = 0; i < colors.length; i += 4 ) {
            colors[count++] = rand.nextFloat();
            colors[count++] = rand.nextFloat();
            colors[count++] = rand.nextFloat();
            colors[count++] = (float)0x10000;
        }
        Log.e("COLORS",Integer.toString(count));
        count=0;

        nrOfVertices[id] = indices.length;

        // float has 4 bytes
        ByteBuffer vbb = ByteBuffer.allocateDirect(coords.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer[id] = vbb.asFloatBuffer();
     
        // short has 2 bytes
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer[id] = ibb.asShortBuffer();

        // float has 4 bytes, 4 colors (RGBA) * number of vertices * 4 bytes
        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer[id] = cbb.asFloatBuffer();

        vertexBuffer[id].put(coords);
        indexBuffer[id].put(indices);
        colorBuffer[id].put(colors);
     
        vertexBuffer[id].position(0);
        indexBuffer[id].position(0);
        colorBuffer[id].position(0);
    }

    private void init3DStar(int id, float x, float y, float z, float magnitude, byte Spec) {

		short color = 0;
		float magScale;
		//decide the magnitude of a star
		magnitude += 2.6f;
		//magScale = 1f-((magnitude - 1f)/10f);
		magScale = (float) Math.pow(1.5, 5.6-magnitude);
		//set alpha to a star
		this.magnitude[id] = magScale;
		
		float uLen = (float) Math.sqrt(x*x+y*y+z*z);
		float[] u = {x/uLen, y/uLen, z/uLen};
		float vLen = (float) Math.sqrt(1+1+((-x-y)/z)*((-x-y)/z));
		float[] v = {1f/vLen, 1f/vLen, ((-x-y)/z)/vLen};
		//float a = (float) (Math.pow(u[2]*v[3]-u[3]*v[2], 2.0));
		float sLen =(float) Math.sqrt(Math.pow(u[1]*v[2]-u[2]*v[1], 2)+Math.pow(u[2]*v[0]-u[0]*v[2], 2)+Math.pow(u[0]*v[1]-u[1]*v[0], 2));
		float[] s ={ (u[1]*v[2]-u[2]*v[1])/sLen, (u[2]*v[0]-u[0]*v[2])/sLen, (u[0]*v[1]-u[1]*v[0])/sLen};
		
		for(int i =0;i<3;i++){
			v[i] = v[i] * textureScale * magScale;
			s[i] = s[i] * textureScale * magScale;
		}
		x = x * coordinateScale;
		y = y * coordinateScale;
		z = z * coordinateScale;
		
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

	public void initConstellations() {
		int RD[][][] = {
				{
				    {2, 3, 54,42,19,48,0},//£^1  Andromeda
				    {1, 41,47,42,36,50,0},//£o[9]Andromeda
				    {1, 9, 44,35,37,15,0},//£]   Andromeda
				    {0, 39,20,30,51,40,0},//£_   Andromeda
				    {0, 8, 23,29,5, 27,0},//£\[1]Andromeda
				},//1
				{
				    {1, 9, 44,35,37,15,0},//£]   Andromeda
				    {0, 56,45,38,29,57,0},//£g   Andromeda
				    {0, 49,49,41,4, 44,0},//£h   Andromeda
				    {1, 9, 30,47,14,31,0},//£p   Andromeda
				},//2
				{
				    {23,1, 55,42,19,34,0},//£j   Andromeda
				    {23,40,24,44,20,2, 0},//£e   Andromeda
				    {0, 17,6, 38,40,54,0},//£c   Andromeda
				    {0, 18,20,36,47,7, 0},//£m   Andromeda
				    {0, 36,53,33,43,10,0},//£k   Andromeda
				    {0, 39,20,30,51,40,0},//£_   Andromeda
				    {0, 38,34,29,18,44,0},//£`   Andromeda
				    {0, 47,20,24,16,3, 0},//£a   Andromeda
				},//3
				{
				    {10,56,43,37,8, 15,1},//£d   Antlia
				    {10,27,9, 31,4, 4, 1},//£\   Antlia
				    {9, 44,12,27,46,10,1},//£c   Antlia
				    {9, 31,32,31,52,19,1},//£a2  Antlia
				    {9, 29,15,35,57,5, 1},//£`   Antlia
				},//4
				{
				    {16,43,5, 77,30,60,1},//£]   Apus
				    {16,33,27,78,53,49,1},//£^   Apus
				    {16,20,27,78,40,3, 1},//£_2  Apus
				    {14,47,52,79,2, 41,1},//£\   Apus
				},//5
				{
				    {20,47,41,9, 29,44,1},//£`   Aquarius
				    {20,52,39,8, 58,60,1},//£g   Aquarius
				    {21,31,34,5, 34,16,1},//£]   Aquarius
				    {22,5, 47,0, 19,11,1},//£\   Aquarius
				    {22,21,39,1, 23,14,1},//£^   Aquarius
				    {22,28,50,0, 1, 12,1},//£a1  Aquarius
				    {22,35,21,0, 7, 2, 1},//£b   Aquarius
				    {22,52,37,7, 34,47,1},//£f   Aquarius
				    {22,49,36,13,35,33,1},//£n2  Aquarius
				    {22,54,39,15,49,15,1},//£_   Aquarius
				    {23,9, 27,21,10,21,1},//c2   Aquarius
				    {23,22,58,20,6, 1, 1},//b1   Aquarius
				    {23,41,46,17,48,60,1},//A2   Aquarius
				    {23,42,43,14,32,41,1},//£s2  Aquarius
				    {23,15,53,9, 5, 16,1},//£r1  Aquarius
				    {23,14,19,6, 2, 55,1},//£p   Aquarius
				    {22,35,21,0, 7, 2, 1},//£b   Aquarius
				},//6
				{
				    {22,28,50,0, 1, 12,1},//£a1  Aquarius
				    {22,25,17,1, 22,39,0},//£k   Aquarius
				},//7
				{
				    {22,5, 47,0, 19,11,1},//£\   Aquarius
				    {22,16,50,7, 46,60,1},//£c   Aquarius
				    {22,6, 26,13,52,10,1},//£d   Aquarius
				},//8
				{
				    {20,11,18,0, 49,17,1},//£c   Aquila
				    {19,52,28,1, 0, 20,0},//£b   Aquila
				    {19,55,19,6, 24,29,0},//£]   Aquila
				    {19,50,47,8, 52,3, 0},//£\   Aquila
				    {19,46,16,10,36,48,0},//£^   Aquila
				    {19,25,30,3, 6, 52,0},//£_   Aquila
				    {19,6, 15,4, 52,56,1},//£f   Aquila
				    {19,36,43,1, 17,12,1},//£d   Aquila
				    {19,52,28,1, 0, 20,0},//£b   Aquila
				},//9
				{
				    {19,6, 15,4, 52,56,1},//£f   Aquila
				    {19,1, 41,5, 44,20,1},//i    Aquila
				},//10
				{
				    {19,46,16,10,36,48,0},//£^   Aquila
				    {19,5, 25,13,51,49,0},//£a   Aquila
				    {18,59,37,15,4, 6, 0},//£`   Aquila
				},//11
				{
				    {18,6, 38,50,5, 29,1},//£c   Ara
				    {17,31,51,49,52,34,1},//£\   Ara
				    {16,59,35,53,9, 38,1},//£`1  Ara
				    {16,58,37,55,59,24,1},//£a   Ara
				    {17,25,24,56,22,40,1},//£^   Ara
				    {17,25,18,55,31,47,1},//£]   Ara
				    {17,31,51,49,52,34,1},//£\   Ara
				},//12
				{
				    {16,58,37,55,59,24,1},//£a   Ara
				    {17,31,6, 60,41,1, 1},//£_   Ara
				},//13
				{
				    {3, 11,38,19,43,36,0},//£_   Aries
				    {2, 59,13,21,20,26,0},//£`   Aries
				    {2, 49,59,27,15,39,0},//c    Aries
				    {2, 7, 10,23,27,46,0},//£\   Aries
				    {1, 54,38,20,48,30,0},//£]   Aries
				    {1, 53,32,19,17,39,0},//£^1  Aries
				},//14
				{
				    {5, 59,32,54,17,6, 0},//£_   Auriga
				    {5, 59,32,44,56,51,0},//£]   Auriga
				    {5, 16,41,45,59,56,0},//£\   Auriga
				    {5, 1, 58,43,49,24,0},//£`   Auriga
				    {5, 6, 31,41,14,5, 0},//£b   Auriga
				    {5, 2, 29,41,4, 33,0},//£a   Auriga
				    {4, 56,60,33,9, 58,0},//£d   Auriga
				},//15
				{
				    {5, 59,32,44,56,51,0},//£]   Auriga
				    {5, 59,43,37,12,46,0},//£c   Auriga
				},//16
				{
				    {15,24,30,37,22,37,0},//£g1  Bootes
				    {15,15,30,33,18,54,0},//£_   Bootes
				    {15,1, 57,40,23,26,0},//£]   Bootes
				    {14,32,5, 38,18,28,0},//£^   Bootes
				    {14,16,23,46,5, 16,0},//£f   Bootes
				    {14,13,29,51,47,24,0},//£e2  Bootes
				    {14,25,12,51,51,6, 0},//£c   Bootes
				},//17
				{
				    {15,15,30,33,18,54,0},//£_   Bootes
				    {14,44,59,27,4, 27,0},//£`   Bootes
				    {14,15,40,19,11,14,0},//£\   Bootes
				    {14,41,9, 13,43,42,0},//£a   Bootes
				    {14,40,44,16,25,6, 0},//£k1  Bootes
				    {14,45,14,16,57,52,0},//£j   Bootes
				    {14,51,23,19,6, 2, 0},//£i   Bootes
				},//18
				{
				    {14,32,5, 38,18,28,0},//£^   Bootes
				    {14,31,50,30,22,16,0},//£l   Bootes
				    {14,15,40,19,11,14,0},//£\   Bootes
				    {13,54,41,18,23,55,0},//£b   Bootes
				    {13,47,16,17,27,24,0},//£n   Bootes
				    {13,49,29,15,47,52,0},//£o   Bootes
				},//19
				{
				    {4, 42,3, 37,8, 41,1},//£]   Caelum
				    {4, 40,34,41,51,49,1},//£\   Caelum
				},//20
				{
				    {4, 57,17,53,45,8, 0},//    7Camelopardalis
				    {5, 3, 25,60,26,32,0},//£]   Camelopardalis
				    {4, 54,3, 66,20,34,0},//£\   Camelopardalis
				    {3, 50,21,71,19,56,0},//£^   Camelopardalis
				    {3, 29,4, 59,56,25,0},//B    Camelopardalis
				},//21
				{
				    {8, 46,42,28,45,36,0},//£d   Cancer
				    {8, 43,17,21,28,7, 0},//£^   Cancer
				    {8, 44,41,18,9, 18,0},//£_   Cancer
				    {8, 58,29,11,51,28,0},//£\   Cancer
				    {8, 16,31,9, 11,8, 0},//£]   Cancer
				    {8, 12,13,17,38,53,0},//£a1  Cancer
				    {8, 44,41,18,9, 18,0},//£_   Cancer
				},//22
				{
				    {12,56,2, 38,19,6, 0},//£\2  Canes_Venatici
				    {12,33,45,41,21,24,0},//£]   Canes_Venatici
				},//23
				{
				    {6, 54,11,12,2, 19,1},//£c   Canis_Major
				    {7, 3, 45,15,37,60,1},//£^   Canis_Major
				    {6, 56,8, 17,3, 15,1},//£d   Canis_Major
				    {6, 45,9, 16,42,47,1},//£\   Canis_Major
				    {6, 22,42,17,57,21,1},//£]   Canis_Major
				},//24
				{
				    {6, 45,9, 16,42,47,1},//£\   Canis_Major
				    {6, 55,37,20,8, 12,1},//£k   Canis_Major
				    {7, 3, 1, 23,49,60,1},//£j2  Canis_Major
				    {7, 8, 23,26,23,36,1},//£_   Canis_Major
				    {7, 1, 43,27,56,5, 1},//£m   Canis_Major
				    {6, 58,38,28,58,20,1},//£`   Canis_Major
				    {6, 20,19,30,3, 48,1},//£a   Canis_Major
				},//25
				{
				    {7, 8, 23,26,23,36,1},//£_   Canis_Major
				    {7, 14,49,26,46,22,1},//£s   Canis_Major
				    {7, 24,6, 29,18,11,1},//£b   Canis_Major
				},//26
				{
				    {7, 14,49,26,46,22,1},//£s   Canis_Major
				    {7, 18,42,24,57,16,1},//£n   Canis_Major
				},//27
				{
				    {7, 39,19,5, 13,39,0},//£\   Canis_Minor
				    {7, 27,9, 8, 17,22,0},//£]   Canis_Minor
				},//28
				{
				    {21,47,2, 16,7, 36,1},//£_   Capricornus
				    {21,40,5, 16,39,44,1},//£^   Capricornus
				    {21,26,40,22,24,41,1},//£a   Capricornus
				    {21,22,15,16,50,4, 1},//£d   Capricornus
				    {21,5, 57,17,13,58,1},//£c   Capricornus
				    {20,21,1, 14,46,53,1},//£]   Capricornus
				    {20,18,3, 12,32,42,1},//£\2  Capricornus
				},//29
				{
				    {20,21,1, 14,46,53,1},//£]   Capricornus
				    {20,28,52,17,48,49,1},//£l   Capricornus
				    {20,46,6, 25,16,14,1},//£r   Capricornus
				    {20,51,49,26,55,9, 1},//£s   Capricornus
				},//30
				{
				    {6, 23,57,52,41,45,1},//£\   Carina
				    {7, 56,47,52,58,57,1},//£q   Carina
				    {8, 22,31,59,30,34,1},//£`   Carina
				    {9, 17,5, 59,16,31,1},//£d   Carina
				    {10,17,5, 61,19,56,1},//q    Carina
				    {10,27,53,58,44,22,1},//s    Carina
				    {10,53,30,58,51,12,1},//u    Carina
				    {11,8, 35,58,58,30,1},//x    Carina
				    {10,42,57,64,23,40,1},//£c   Carina
				    {10,13,44,70,2, 16,1},//£s   Carina
				    {9, 13,12,69,43,3, 1},//£]   Carina
				    {9, 47,6, 65,4, 19,1},//£o   Carina
				},//31
				{
				    {10,17,5, 61,19,56,1},//q    Carina
				    {10,32,1, 61,41,7, 1},//p    Carina
				},//32
				{
				    {0, 9, 10,59,9, 1, 0},//£]   Cassiopeia
				    {0, 40,30,56,32,15,0},//£\   Cassiopeia
				    {0, 56,42,60,43,0, 0},//£^   Cassiopeia
				    {1, 25,49,60,14,8, 0},//£_   Cassiopeia
				    {1, 54,24,63,40,12,0},//£`   Cassiopeia
				},//33
				{
				    {13,45,42,33,2, 36,1},//i    Centaurus
				    {13,51,50,32,59,38,1},//k    Centaurus
				    {13,49,27,34,27,2, 1},//g    Centaurus
				    {14,6, 41,36,22,7, 1},//£c   Centaurus
				    {14,20,33,37,53,7, 1},//£r   Centaurus
				    {14,41,58,37,47,36,1},//b    Centaurus
				    {14,43,39,35,10,24,1},//c1   Centaurus
				},//34
				{
				    {13,20,36,36,42,44,1},//£d   Centaurus
				    {13,31,3, 39,24,26,1},//d    Centaurus
				    {13,49,27,34,27,2, 1},//g    Centaurus
				    {14,6, 41,36,22,7, 1},//£c   Centaurus
				    {14,20,33,37,53,7, 1},//£r   Centaurus
				    {14,23,2, 39,30,42,1},//a    Centaurus
				    {14,35,30,42,9, 28,1},//£b   Centaurus
				    {14,59,10,42,6, 15,1},//£e   Centaurus
				},//35
				{
				    {14,6, 41,36,22,7, 1},//£c   Centaurus
				    {13,49,30,41,41,16,1},//£h   Centaurus
				    {12,53,26,40,10,44,1},//n    Centaurus
				    {12,41,31,48,57,36,1},//£^   Centaurus
				    {12,28,2, 50,13,50,1},//£m   Centaurus
				    {12,8, 22,50,43,21,1},//£_   Centaurus
				    {12,11,39,52,22,6, 1},//£l   Centaurus
				    {11,21,0, 54,29,28,1},//£k   Centaurus
				    {11,31,49,59,30,56,1},//£j2  Centaurus
				    {11,35,47,63,1, 11,1},//£f   Centaurus
				},//36
				{
				    {13,49,30,41,41,16,1},//£h   Centaurus
				    {13,55,32,47,17,18,1},//£a   Centaurus
				    {13,39,53,53,27,59,1},//£`   Centaurus
				    {12,41,31,48,57,36,1},//£^   Centaurus
				},//37
				{
				    {14,39,41,60,50,6, 1},//£\1[cCentaurus
				    {13,39,53,53,27,59,1},//£`   Centaurus
				    {14,3, 49,60,22,23,1},//£]   Centaurus
				},//38
				{
				    {20,29,35,62,59,39,0},//£c   Cepheus
				    {20,45,17,61,50,12,0},//£b   Cepheus
				    {21,18,35,62,35,8, 0},//£\   Cepheus
				    {21,28,40,70,33,38,0},//£]   Cepheus
				    {23,39,21,77,37,55,0},//£^   Cepheus
				    {20,8, 53,77,42,41,0},//£e   Cepheus
				},//39
				{
				    {21,18,35,62,35,8, 0},//£\   Cepheus
				    {21,45,27,61,7, 15,0},//£h   Cepheus
				    {22,10,51,58,12,4, 0},//£a   Cepheus
				    {22,49,41,66,12,3, 0},//£d   Cepheus
				    {23,39,21,77,37,55,0},//£^   Cepheus
				},//40
				{
				    {22,10,51,58,12,4, 0},//£a   Cepheus
				    {22,15,2, 57,2, 36,0},//£`   Cepheus
				    {22,29,10,58,24,55,0},//£_   Cepheus
				},//41
				{
				    {0, 19,26,8, 49,26,1},//£d   Cetus
				    {0, 43,35,17,59,12,1},//£]   Cetus
				    {1, 8, 35,10,10,55,1},//£b   Cetus
				    {1, 24,1, 8, 10,58,1},//£c   Cetus
				    {1, 51,28,10,20,6, 1},//£a   Cetus
				    {2, 19,21,2, 58,37,1},//£j   Cetus
				    {2, 39,29,0, 19,43,0},//£_   Cetus
				    {2, 43,18,3, 14,10,0},//£^   Cetus
				    {2, 35,52,5, 35,36,0},//£h   Cetus
				    {2, 28,10,8, 27,36,0},//£i2  Cetus
				    {2, 44,56,10,6, 51,0},//£g   Cetus
				    {2, 59,43,8, 54,27,0},//£f   Cetus
				    {3, 2, 17,4, 5, 24,0},//£\   Cetus
				    {2, 43,18,3, 14,10,0},//£^   Cetus
				},//42
				{
				    {1, 8, 35,10,10,55,1},//£b   Cetus
				    {1, 44,5, 15,56,22,1},//£n   Cetus
				    {1, 51,28,10,20,6, 1},//£a   Cetus
				},//43
				{
				    {1, 44,5, 15,56,22,1},//£n   Cetus
				    {2, 0, 0, 21,4, 40,1},//£o   Cetus
				    {2, 44,7, 13,51,31,1},//£k   Cetus
				},//44
				{
				    {8, 18,31,76,55,12,1},//£\   Chamaeleon
				    {10,45,47,80,32,25,1},//£_2  Chamaeleon
				    {12,18,21,79,18,44,1},//£]   Chamaeleon
				    {10,35,28,78,36,28,1},//£^   Chamaeleon
				    {8, 18,31,76,55,12,1},//£\   Chamaeleon
				},//45
				{
				    {15,17,31,58,48,3, 1},//£]   Circinus
				    {14,42,31,64,58,28,1},//£\   Circinus
				    {15,23,23,59,19,14,1},//£^   Circinus
				},//46
				{
				    {6, 22,7, 33,26,11,1},//£_   Columba
				    {6, 16,33,35,8, 27,1},//£e   Columba
				    {5, 57,32,35,16,60,1},//£^   Columba
				    {5, 50,58,35,46,10,1},//£]   Columba
				    {5, 39,39,34,4, 27,1},//£\   Columba
				    {5, 31,13,35,28,14,1},//£`   Columba
				},//47
				{
				    {5, 57,32,35,16,60,1},//£^   Columba
				    {5, 59,9, 42,48,54,1},//£b   Columba
				},//48
				{
				    {13,9, 60,17,31,45,0},//£\   Coma_Berenices
				    {13,11,53,27,52,34,0},//£]   Coma_Berenices
				    {12,26,56,28,16,7, 0},//£^   Coma_Berenices
				},//49
				{
				    {18,58,43,37,6, 26,1},//£`   Corona_Australis
				    {19,6, 25,37,3, 46,1},//£^   Corona_Australis
				    {19,9, 28,37,54,15,1},//£\   Corona_Australis
				    {19,10,2, 39,20,26,1},//£]   Corona_Australis
				    {19,8, 21,40,29,48,1},//£_   Corona_Australis
				    {19,3, 7, 42,5, 42,1},//£a   Corona_Australis
				    {18,33,30,42,18,45,1},//£c   Corona_Australis
				},//50
				{
				    {16,1, 27,29,51,4, 0},//£d   Corona_Borealis
				    {15,57,35,26,52,41,0},//£`   Corona_Borealis
				    {15,49,36,26,4, 7, 0},//£_   Corona_Borealis
				    {15,42,45,26,17,44,0},//£^   Corona_Borealis
				    {15,34,41,26,42,54,0},//£\   Corona_Borealis
				    {15,27,50,29,6, 20,0},//£]   Corona_Borealis
				    {15,32,56,31,21,33,0},//£c   Corona_Borealis
				},//51
				{
				    {12,8, 25,24,43,44,1},//£\   Corvus
				    {12,10,8, 22,37,11,1},//£`   Corvus
				    {12,15,48,17,32,31,1},//£^   Corvus
				    {12,29,52,16,30,54,1},//£_   Corvus
				    {12,34,23,23,23,48,1},//£]   Corvus
				    {12,10,8, 22,37,11,1},//£`   Corvus
				},//52
				{
				    {11,36,41,9, 48,8, 1},//£c   Crater
				    {11,19,21,14,46,45,1},//£_   Crater
				    {10,59,47,18,17,57,1},//£\   Crater
				    {11,11,39,22,49,32,1},//£]   Crater
				    {11,24,53,17,41,2, 1},//£^   Crater
				    {11,56,1, 17,9, 3, 1},//£b   Crater
				},//53
				{
				    {11,19,21,14,46,45,1},//£_   Crater
				    {11,24,53,17,41,2, 1},//£^   Crater
				},//54
				{
				    {12,26,36,63,5, 57,1},//£\1  Crux
				    {12,31,10,57,6, 45,1},//£^   Crux
				},//55
				{
				    {12,47,43,59,41,19,1},//£]   Crux
				    {12,15,9, 58,44,56,1},//£_   Crux
				},//56
				{
				    {21,12,56,30,13,38,0},//£a   Cygnus
				    {20,46,12,33,58,10,0},//£`   Cygnus
				    {20,22,14,40,15,24,0},//£^   Cygnus
				    {19,44,58,45,7, 50,0},//£_   Cygnus
				    {19,29,42,51,43,46,0},//£d   Cygnus
				    {19,17,6, 53,22,5, 0},//£e   Cygnus
				},//57
				{
				    {19,30,43,27,57,35,0},//£]1  Cygnus
				    {19,39,23,30,9, 12,0},//£p   Cygnus
				    {19,56,18,35,5, 1, 0},//£b   Cygnus
				    {20,22,14,40,15,24,0},//£^   Cygnus
				    {20,41,26,45,16,49,0},//£\   Cygnus
				},//58
				{
				    {20,33,13,11,18,12,0},//£`   Delphinus
				    {20,37,33,14,35,43,0},//£]   Delphinus
				    {20,43,28,15,4, 29,0},//£_   Delphinus
				    {20,46,40,16,7, 29,0},//£^2  Delphinus
				    {20,39,38,15,54,43,0},//£\   Delphinus
				    {20,37,33,14,35,43,0},//£]   Delphinus
				},//59
				{
				    {4, 16,1, 51,29,14,1},//£^   Dorado
				    {4, 33,60,55,2, 42,1},//£\   Dorado
				    {5, 5, 31,57,28,23,1},//£a   Dorado
				    {5, 33,38,62,29,24,1},//£]   Dorado
				    {5, 44,46,65,44,8, 1},//£_   Dorado
				},//60
				{
				    {16,36,11,52,53,60,0},//   16Draco
				    {17,30,26,52,18,5, 0},//£]   Draco
				    {17,56,36,51,29,20,0},//£^   Draco
				    {17,53,32,56,52,21,0},//£i   Draco
				    {17,32,16,55,10,22,0},//£h2  Draco
				    {17,30,26,52,18,5, 0},//£]   Draco
				},//61
				{
				    {17,53,32,56,52,21,0},//£i   Draco
				    {19,12,33,67,39,41,0},//£_   Draco
				    {19,48,10,70,16,4, 0},//£`   Draco
				    {18,54,24,71,17,50,0},//£o   Draco
				    {18,20,45,71,20,16,0},//£p   Draco
				    {17,36,57,68,45,26,0},//£s   Draco
				    {17,8, 47,65,42,53,0},//£a   Draco
				    {16,23,60,61,30,51,0},//£b   Draco
				    {16,1, 54,58,33,52,0},//£c   Draco
				    {15,24,56,58,57,58,0},//£d   Draco
				    {14,4, 23,64,22,33,0},//£\   Draco
				    {12,33,29,69,47,18,0},//£e   Draco
				    {11,31,24,69,19,52,0},//£f   Draco
				},//62
				{
				    {21,15,49,5, 14,53,0},//£\   Equuleus
				    {21,22,54,6, 48,40,0},//£]   Equuleus
				    {21,14,29,10,0, 28,0},//£_   Equuleus
				    {21,10,20,10,7, 55,0},//£^   Equuleus
				    {21,15,49,5, 14,53,0},//£\   Equuleus
				},//63
				{
				    {1, 37,43,57,14,12,1},//£\   Eridanus
				    {1, 55,57,51,36,34,1},//£q   Eridanus
				    {2, 16,30,51,30,44,1},//£p   Eridanus
				    {2, 26,59,47,42,14,1},//£e   Eridanus
				    {2, 40,40,39,51,19,1},//£d   Eridanus
				    {2, 58,16,40,18,17,1},//£c1  Eridanus
				    {3, 19,53,43,4, 18,1},//e    Eridanus
				    {3, 37,6, 40,16,28,1},//y    Eridanus
				    {3, 48,36,37,37,12,1},//f    Eridanus
				    {3, 49,27,36,12,0, 1},//g    Eridanus
				    {4, 17,54,33,47,54,1},//£o4  Eridanus
				    {4, 24,2, 34,1, 1, 1},//d    Eridanus
				    {4, 35,33,30,33,44,1},//£o2  Eridanus
				    {4, 33,31,29,45,57,1},//£o1  Eridanus
				    {3, 59,55,24,0, 58,1},//£n9  Eridanus
				    {3, 46,51,23,14,54,1},//£n6  Eridanus
				    {3, 33,47,21,37,58,1},//£n5  Eridanus
				    {3, 19,31,21,45,29,1},//£n4  Eridanus
				    {3, 2, 24,23,37,28,1},//£n3  Eridanus
				    {2, 51,2, 21,0, 14,1},//£n2  Eridanus
				    {2, 45,6, 18,34,22,1},//£n1  Eridanus
				    {2, 56,26,8, 53,51,1},//£b   Eridanus
				    {3, 32,56,9, 27,30,1},//£`   Eridanus
				    {3, 43,15,9, 45,55,1},//£_   Eridanus
				    {3, 58,2, 13,30,30,1},//£^   Eridanus
				    {4, 11,52,6, 50,16,1},//£j1  Eridanus
				    {4, 36,19,3, 21,9, 1},//£h   Eridanus
				    {4, 45,30,3, 15,17,1},//£g   Eridanus
				    {4, 52,54,5, 27,10,1},//£s   Eridanus
				    {5, 7, 51,5, 5, 10,1},//£]   Eridanus
				    {5, 9, 9, 8, 45,15,1},//£f   Eridanus
				},//64
				{
				    {3, 12,4, 28,59,21,1},//£\   Fornax
				    {2, 49,5, 32,24,23,1},//£]   Fornax
				    {2, 12,54,30,43,26,1},//£g   Fornax
				},//65
				{
				    {6, 45,17,12,53,46,0},//£i   Gemini
				    {6, 37,43,16,23,58,0},//£^   Gemini
				    {6, 28,58,20,12,44,0},//£h   Gemini
				    {6, 22,58,22,30,50,0},//£g   Gemini
				    {6, 14,53,22,30,25,0},//£b   Gemini
				},//66
				{
				    {6, 22,58,22,30,50,0},//£g   Gemini
				    {6, 43,56,25,7, 52,0},//£`   Gemini
				    {7, 11,8, 30,14,43,0},//£n   Gemini
				    {7, 34,36,31,53,19,0},//£\   Gemini
				    {7, 43,19,28,53,3, 0},//£m   Gemini
				    {7, 45,19,28,1, 35,0},//£]   Gemini
				    {7, 44,27,24,23,53,0},//£e   Gemini
				    {7, 20,7, 21,58,56,0},//£_   Gemini
				    {7, 4, 7, 20,34,13,0},//£a   Gemini
				    {6, 37,43,16,23,58,0},//£^   Gemini
				},//67
				{
				    {7, 20,7, 21,58,56,0},//£_   Gemini
				    {7, 18,6, 16,32,26,0},//£f   Gemini
				},//68
				{
				    {23,6, 53,43,31,13,1},//£c   Grus
				    {23,10,21,45,14,48,1},//£d   Grus
				    {22,42,40,46,53,4, 1},//£]   Grus
				    {22,8, 14,46,57,38,1},//£\   Grus
				},//69
				{
				    {23,0, 53,52,45,15,1},//£a   Grus
				    {22,48,33,51,19,0, 1},//£`   Grus
				    {22,42,40,46,53,4, 1},//£]   Grus
				    {22,29,16,43,29,44,1},//£_1  Grus
				    {22,15,37,41,20,48,1},//£g1  Grus
				    {22,6, 7, 39,32,35,1},//£f   Grus
				    {21,53,56,37,21,53,1},//£^   Grus
				},//70
				{
				    {17,14,39,14,23,25,0},//£\1  Hercules
				    {16,30,13,21,29,23,0},//£]   Hercules
				    {16,21,55,19,9, 11,0},//£^   Hercules
				},//71
				{
				    {16,30,13,21,29,23,0},//£]   Hercules
				    {16,41,17,31,36,7, 0},//£a   Hercules
				    {16,42,54,38,55,21,0},//£b   Hercules
				    {16,34,6, 42,26,13,0},//£m   Hercules
				    {16,19,44,46,18,48,0},//£n   Hercules
				    {16,8, 46,44,56,5, 0},//£p   Hercules
				    {16,2, 48,46,2, 13,0},//£o   Hercules
				},//72
				{
				    {16,41,17,31,36,7, 0},//£a   Hercules
				    {17,0, 17,30,55,35,0},//£`   Hercules
				    {17,15,3, 36,48,33,0},//£k   Hercules
				    {16,42,54,38,55,21,0},//£b   Hercules
				},//73
				{
				    {17,15,3, 36,48,33,0},//£k   Hercules
				    {17,23,41,37,8, 45,0},//£l   Hercules
				    {17,56,15,37,15,2, 0},//£c   Hercules
				    {17,39,28,46,0, 23,0},//£d   Hercules
				},//74
				{
				    {17,0, 17,30,55,35,0},//£`   Hercules
				    {17,15,2, 24,50,22,0},//£_   Hercules
				    {17,30,44,26,6, 38,0},//£f   Hercules
				    {17,46,28,27,43,21,0},//£g   Hercules
				    {17,57,46,29,14,52,0},//£i   Hercules
				    {18,7, 33,28,45,45,0},//£j   Hercules
				    {18,23,42,21,46,13,0},//  109Hercules
				    {18,45,40,20,32,50,0},//  110Hercules
				    {18,52,16,21,25,31,0},//  112Hercules
				},//75
				{
				    {18,8, 45,20,48,52,0},//  102Hercules
				    {18,23,42,21,46,13,0},//  109Hercules
				    {18,45,40,20,32,50,0},//  110Hercules
				    {18,47,1, 18,10,52,0},//  111Hercules
				},//76
				{
				    {2, 58,48,64,4, 17,1},//£]   Horologium
				    {4, 14,0, 42,17,38,1},//£\   Horologium
				    {2, 24,54,60,18,42,1},//£f   Horologium
				},//77
				{
				    {2, 49,1, 62,48,24,1},//£h   Horologium
				    {4, 14,0, 42,17,38,1},//£\   Horologium
				},//78
				{
				    {8, 55,24,5, 56,44,0},//£a   Hydra
				    {8, 46,47,6, 25,8, 0},//£`   Hydra
				    {8, 37,39,5, 42,14,0},//£_   Hydra
				    {8, 38,45,3, 20,29,0},//£m   Hydra
				    {8, 43,13,3, 23,55,0},//£b   Hydra
				    {8, 55,24,5, 56,44,0},//£a   Hydra
				},//79
				{
				    {8, 55,24,5, 56,44,0},//£a   Hydra
				    {9, 14,22,2, 18,54,0},//£c   Hydra
				    {9, 39,51,1, 8, 34,1},//£d   Hydra
				    {9, 27,35,8, 39,31,1},//£\   Hydra
				    {9, 51,29,14,50,48,1},//£o1  Hydra
				    {10,5, 7, 13,3, 53,1},//£o2  Hydra
				    {10,10,35,12,21,14,1},//£f   Hydra
				    {10,26,6, 16,50,10,1},//£g   Hydra
				    {10,49,37,16,11,39,1},//£h   Hydra
				    {11,33,0, 31,51,27,1},//£i   Hydra
				    {11,52,55,33,54,29,1},//£]   Hydra
				    {13,18,55,23,10,17,1},//£^   Hydra
				    {14,6, 22,26,40,55,1},//£k   Hydra
				},//80
				{
				    {1, 58,46,61,34,12,1},//£\   Hydrus
				    {1, 54,56,67,38,51,1},//£b2  Hydrus
				    {2, 21,45,68,39,34,1},//£_   Hydrus
				    {2, 39,35,68,16,1, 1},//£`   Hydrus
				    {0, 25,39,77,15,18,1},//£]   Hydrus
				    {3, 47,14,74,14,21,1},//£^   Hydrus
				},//81
				{
				    {20,37,34,47,17,30,1},//£\   Indus
				    {21,19,52,53,26,57,1},//£c   Indus
				    {20,54,49,58,27,15,1},//£]   Indus
				},//82
				{
				    {22,29,32,47,42,25,0},//    5Lacerta
				    {22,31,17,50,16,57,0},//£\   Lacerta
				    {22,23,34,52,13,46,0},//£]   Lacerta
				    {22,24,31,49,28,35,0},//    4Lacerta
				    {22,29,32,47,42,25,0},//    5Lacerta
				    {22,30,29,43,7, 24,0},//    6Lacerta
				    {22,15,58,37,44,55,0},//    1Lacerta
				},//83
				{
				    {22,40,31,44,16,35,0},//   11Lacerta
				    {22,30,29,43,7, 24,0},//    6Lacerta
				    {22,21,2, 46,32,12,0},//    2Lacerta
				},//84
				{
				    {9, 24,39,26,10,57,0},//£e   Leo
				    {9, 31,43,22,58,5, 0},//£f   Leo
				    {9, 45,51,23,46,27,0},//£`   Leo
				    {9, 52,46,26,0, 26,0},//£g   Leo
				    {10,16,41,23,25,2, 0},//£a   Leo
				    {10,19,58,19,50,31,0},//£^1  Leo
				    {10,7, 20,16,45,46,0},//£b   Leo
				    {10,8, 22,11,58,2, 0},//£\   Leo
				    {9, 41,9, 9, 53,33,0},//£j   Leo
				},//85
				{
				    {10,19,58,19,50,31,0},//£^1  Leo
				    {11,2, 20,20,10,47,0},//b    Leo
				    {11,14,6, 20,31,26,0},//£_   Leo
				    {11,49,4, 14,34,20,0},//£]   Leo
				    {11,14,14,15,25,47,0},//£c   Leo
				    {10,8, 22,11,58,2, 0},//£\   Leo
				    {10,32,49,9, 18,24,0},//£l   Leo
				},//86
				{
				    {11,14,14,15,25,47,0},//£c   Leo
				    {11,23,55,10,31,47,0},//£d   Leo
				    {11,21,8, 6, 1, 46,0},//£m   Leo
				    {11,14,14,15,25,47,0},//£c   Leo
				},//87
				{
				    {10,53,19,34,12,56,0},//o    Leo_Minor
				    {10,27,53,36,42,27,0},//£]   Leo_Minor
				    {10,7, 26,35,14,41,0},//   21Leo_Minor
				},//88
				{
				    {5, 56,24,14,10,5, 1},//£b   Lepus
				    {5, 46,57,14,49,19,1},//£a   Lepus
				    {5, 32,44,17,49,20,1},//£\   Lepus
				    {5, 28,15,20,45,33,1},//£]   Lepus
				    {5, 44,28,22,26,51,1},//£^   Lepus
				    {5, 51,19,20,52,39,1},//£_   Lepus
				},//89
				{
				    {5, 12,56,16,12,20,1},//£g   Lepus
				    {5, 32,44,17,49,20,1},//£\   Lepus
				    {5, 28,15,20,45,33,1},//£]   Lepus
				    {5, 5, 28,22,22,15,1},//£`   Lepus
				},//90
				{
				    {15,58,11,14,16,46,1},//   48Libra
				    {15,53,49,16,43,47,1},//£c   Libra
				    {15,35,32,14,47,22,1},//£^   Libra
				    {15,17,0, 9, 22,58,1},//£]   Libra
				    {14,50,53,16,2, 30,1},//£\2  Libra
				    {15,4, 4, 25,16,55,1},//£m   Libra
				    {15,37,1, 28,8, 6, 1},//£o   Libra
				    {15,38,39,29,46,40,1},//£n   Libra
				},//91
				{
				    {15,17,0, 9, 22,58,1},//£]   Libra
				    {15,4, 4, 25,16,55,1},//£m   Libra
				},//92
				{
				    {15,12,17,52,5, 57,1},//£a   Lupus
				    {15,18,32,47,52,31,1},//£g   Lupus
				    {15,22,41,44,41,22,1},//£`   Lupus
				    {15,35,8, 41,10,0, 1},//£^   Lupus
				    {15,50,58,33,37,38,1},//£q   Lupus
				    {15,39,46,34,24,43,1},//£r1  Lupus
				    {15,21,22,40,38,51,1},//£_   Lupus
				    {14,58,32,43,8, 2, 1},//£]   Lupus
				    {14,51,38,43,34,31,1},//£j   Lupus
				    {14,26,11,45,22,45,1},//£n2  Lupus
				    {14,19,24,46,3, 29,1},//£d   Lupus
				    {14,37,53,49,25,33,1},//£l   Lupus
				    {14,41,56,47,23,17,1},//£\   Lupus
				    {15,5, 7, 47,3, 4, 1},//£k   Lupus
				    {15,18,32,47,52,31,1},//£g   Lupus
				},//93
				{
				    {14,58,32,43,8, 2, 1},//£]   Lupus
				    {15,5, 7, 47,3, 4, 1},//£k   Lupus
				},//94
				{
				    {15,35,8, 41,10,0, 1},//£^   Lupus
				    {16,0, 7, 38,23,48,1},//£b   Lupus
				},//95
				{
				    {9, 21,3, 34,23,33,0},//£\   Lynx
				    {9, 18,51,36,48,10,0},//   38Lynx
				    {8, 22,50,43,11,18,0},//£e   Lynx
				    {7, 26,43,49,12,42,0},//   21Lynx
				    {6, 57,17,58,25,23,0},//   15Lynx
				    {6, 19,37,59,0, 39,0},//    2Lynx
				},//96
				{
				    {19,13,45,39,8, 46,0},//£b   Lyra
				    {19,16,22,38,8, 1, 0},//£c   Lyra
				    {18,54,30,36,53,55,0},//£_2  Lyra
				    {18,44,46,37,36,18,0},//£a1  Lyra
				    {18,36,56,38,46,59,0},//£\   Lyra
				    {18,44,23,39,36,45,0},//£`2  Lyra
				    {18,55,20,43,56,45,0},//   13Lyra
				},//97
				{
				    {18,54,30,36,53,55,0},//£_2  Lyra
				    {18,58,57,32,41,22,0},//£^   Lyra
				    {18,50,5, 33,21,46,0},//£]   Lyra
				    {18,44,46,37,36,18,0},//£a1  Lyra
				},//98
				{
				    {18,36,56,38,46,59,0},//£\   Lyra
				    {18,19,52,36,3, 52,0},//£e   Lyra
				},//99
				{
				    {5, 2, 43,71,18,52,1},//£]   Mensa
				    {6, 10,14,74,45,9, 1},//£\   Mensa
				    {5, 31,53,76,20,30,1},//£^   Mensa
				    {4, 55,11,74,56,13,1},//£b   Mensa
				},//100
				{
				    {21,20,46,40,48,34,1},//£c1  Microscopium
				    {21,17,56,32,10,21,1},//£`   Microscopium
				    {21,1, 17,32,15,28,1},//£^   Microscopium
				    {20,49,58,33,46,47,1},//£\   Microscopium
				    {20,48,29,43,59,18,1},//£d   Microscopium
				},//101
				{
				    {8, 8, 36,2, 59,2, 1},//£a   Monoceros
				    {7, 41,15,9, 33,4, 1},//£\   Monoceros
				    {7, 11,52,0, 29,34,1},//£_   Monoceros
				    {6, 28,49,7, 1, 59,1},//£]   Monoceros
				    {6, 23,46,4, 35,34,0},//£`   Monoceros
				    {6, 32,54,7, 19,59,0},//   13Monoceros
				    {6, 40,59,9, 53,45,0},//   15Monoceros
				},//102
				{
				    {6, 28,49,7, 1, 59,1},//£]   Monoceros
				    {6, 14,51,6, 16,29,1},//£^   Monoceros
				    {6, 23,46,4, 35,34,0},//£`   Monoceros
				},//103
				{
				    {12,46,17,68,6, 29,1},//£]   Musca
				    {12,37,11,69,8, 8, 1},//£\   Musca
				    {12,17,35,67,57,38,1},//£`   Musca
				    {11,45,37,66,43,44,1},//£f   Musca
				},//104
				{
				    {13,2, 16,71,32,56,1},//£_   Musca
				    {12,37,11,69,8, 8, 1},//£\   Musca
				    {12,32,28,72,7, 59,1},//£^   Musca
				},//105
				{
				    {16,27,11,47,33,17,1},//£`   Norma
				    {16,19,51,50,9, 19,1},//£^2  Norma
				    {16,3, 13,49,13,47,1},//£b   Norma
				},//106
				{
				    {22,46,4, 81,22,54,1},//£]   Octans
				    {14,26,56,83,40,4, 1},//£_   Octans
				    {21,41,28,77,23,22,1},//£h   Octans
				},//107
				{
				    {17,27,21,29,52,0, 1},//d    Ophiuchus
				    {17,15,21,26,36,0, 1},//A    Ophiuchus
				    {17,22,1, 24,59,58,1},//£c   Ophiuchus
				    {17,26,22,24,10,30,1},//b    Ophiuchus
				    {17,21,0, 21,6, 45,1},//£i   Ophiuchus
				    {17,10,23,15,43,30,1},//£b   Ophiuchus
				    {16,37,10,10,34,2, 1},//£a   Ophiuchus
				    {16,27,48,8, 22,18,1},//£o   Ophiuchus
				    {16,18,19,4, 41,33,1},//£`   Ophiuchus
				},//108
				{
				    {16,14,21,3, 41,38,1},//£_   Ophiuchus
				    {16,57,40,9, 22,30,0},//£e   Ophiuchus
				    {17,34,56,12,33,38,0},//£\   Ophiuchus
				    {17,43,28,4, 34,1, 0},//£]   Ophiuchus
				    {17,10,23,15,43,30,1},//£b   Ophiuchus
				},//109
				{
				    {17,43,28,4, 34,1, 0},//£]   Ophiuchus
				    {17,47,54,2, 42,27,0},//£^   Ophiuchus
				    {18,0, 39,2, 55,54,0},//   67Ophiuchus
				    {18,7, 21,9, 33,49,0},//   72Ophiuchus
				},//110
				{
				    {17,59,2, 9, 46,24,1},//£h   Ophiuchus
				    {17,47,54,2, 42,27,0},//£^   Ophiuchus
				    {18,0, 39,2, 55,54,0},//   67Ophiuchus
				    {18,5, 27,2, 30,9, 0},//p    Ophiuchus
				},//111
				{
				    {6, 11,56,14,12,32,0},//£i   Orion
				    {6, 3, 55,20,8, 18,0},//£q2  Orion
				    {5, 54,23,20,16,35,0},//£q1  Orion
				    {6, 7, 34,14,46,7, 0},//£h   Orion
				    {6, 11,56,14,12,32,0},//£i   Orion
				    {6, 2, 23,9, 38,50,0},//£g   Orion
				    {5, 55,10,7, 24,25,0},//£\   Orion
				    {5, 40,46,1, 56,33,1},//£a   Orion
				    {5, 47,45,9, 40,11,1},//£e   Orion
				    {5, 14,32,8, 12,6, 1},//£]   Orion
				    {5, 32,0, 0, 17,57,1},//£_   Orion
				    {5, 25,8, 6, 20,59,0},//£^   Orion
				    {5, 35,8, 9, 56,3, 0},//£f   Orion
				    {5, 55,10,7, 24,25,0},//£\   Orion
				},//112
				{
				    {5, 40,46,1, 56,33,1},//£a   Orion
				    {5, 36,13,1, 12,7, 1},//£`   Orion
				    {5, 32,0, 0, 17,57,1},//£_   Orion
				},//113
				{
				    {5, 25,8, 6, 20,59,0},//£^   Orion
				    {4, 54,54,10,9, 4, 0},//£k1  Orion
				    {4, 50,37,8, 54,1, 0},//£k2  Orion
				    {4, 49,50,6, 57,40,0},//£k3  Orion
				    {4, 51,12,5, 36,18,0},//£k4  Orion
				    {4, 54,15,2, 26,26,0},//£k5  Orion
				    {4, 58,33,1, 42,50,0},//£k6  Orion
				},//114
				{
				    {20,25,39,56,44,6, 1},//£\   Pavo
				    {20,44,58,66,12,12,1},//£]   Pavo
				    {20,0, 35,72,54,37,1},//£`   Pavo
				    {18,43,2, 71,25,40,1},//£a   Pavo
				    {17,45,44,64,43,25,1},//£b   Pavo
				    {18,8, 35,63,40,5, 1},//£k   Pavo
				    {18,23,14,61,29,38,1},//£i   Pavo
				    {18,52,13,62,11,15,1},//£f   Pavo
				    {20,8, 42,66,10,46,1},//£_   Pavo
				    {20,44,58,66,12,12,1},//£]   Pavo
				    {21,26,26,65,22,5, 1},//£^   Pavo
				},//115
				{
				    {21,44,39,25,38,42,0},//£e   Pegasus
				    {22,7, 0, 25,20,42,0},//£d   Pegasus
				    {22,43,0, 30,13,17,0},//£b   Pegasus
				    {23,3, 46,28,4, 57,0},//£]   Pegasus
				    {22,50,0, 24,36,6, 0},//£g   Pegasus
				    {22,46,32,23,33,56,0},//£f   Pegasus
				    {21,44,31,17,21,0, 0},//    9Pegasus
				    {21,22,5, 19,48,16,0},//    1Pegasus
				},//116
				{
				    {21,44,11,9, 52,30,0},//£`   Pegasus
				    {22,10,12,6, 11,52,0},//£c   Pegasus
				    {22,41,28,10,49,53,0},//£a   Pegasus
				    {22,46,41,12,10,27,0},//£i   Pegasus
				    {23,4, 46,15,12,19,0},//£\   Pegasus
				    {0, 13,14,15,11,1, 0},//£^   Pegasus
				},//117
				{
				    {23,4, 46,15,12,19,0},//£\   Pegasus
				    {23,3, 46,28,4, 57,0},//£]   Pegasus
				},//118
				{
				    {4, 6, 35,50,21,5, 0},//£f   Perseus
				    {4, 14,54,48,24,34,0},//£g   Perseus
				    {4, 8, 40,47,42,45,0},//c    Perseus
				    {3, 42,55,47,47,16,0},//£_   Perseus
				    {3, 24,19,49,51,40,0},//£\   Perseus
				    {3, 4, 48,53,30,23,0},//£^   Perseus
				    {2, 50,42,55,53,44,0},//£b   Perseus
				    {1, 43,40,50,41,20,0},//£p   Perseus
				},//119
				{
				    {4, 8, 40,47,42,45,0},//c    Perseus
				    {4, 36,41,41,15,54,0},//e    Perseus
				},//120
				{
				    {3, 42,55,47,47,16,0},//£_   Perseus
				    {3, 57,51,40,0, 37,0},//£`   Perseus
				    {3, 58,58,35,47,28,0},//£i   Perseus
				    {3, 54,8, 31,53,1, 0},//£a   Perseus
				    {3, 44,19,32,17,18,0},//£j   Perseus
				},//121
				{
				    {3, 24,19,49,51,40,0},//£\   Perseus
				    {3, 9, 30,44,51,28,0},//£e   Perseus
				    {3, 8, 10,40,57,20,0},//£]   Perseus
				    {3, 5, 10,38,50,26,0},//£l   Perseus
				    {2, 50,35,38,19,8, 0},//   16Perseus
				},//122
				{
				    {1, 53,39,46,18,9, 1},//£r   Phoenix
				    {1, 28,22,43,19,4, 1},//£^   Phoenix
				    {1, 15,11,45,31,56,1},//£h   Phoenix
				    {1, 6, 5, 46,43,7, 1},//£]   Phoenix
				    {0, 26,17,42,18,18,1},//£\   Phoenix
				    {0, 9, 25,45,44,49,1},//£`   Phoenix
				    {23,35,5, 42,36,54,1},//£d   Phoenix
				    {23,58,56,52,44,45,1},//£k   Phoenix
				},//123
				{
				    {1, 28,22,43,19,4, 1},//£^   Phoenix
				    {1, 31,15,49,4, 23,1},//£_   Phoenix
				},//124
				{
				    {1, 6, 5, 46,43,7, 1},//£]   Phoenix
				    {1, 8, 23,55,14,45,1},//£a   Phoenix
				    {0, 43,21,57,27,47,1},//£b   Phoenix
				    {0, 9, 25,45,44,49,1},//£`   Phoenix
				},//125
				{
				    {6, 48,12,61,56,31,1},//£\   Pictor
				    {5, 49,50,56,9, 59,1},//£^   Pictor
				    {5, 47,17,51,4, 0, 1},//£]   Pictor
				},//126
				{
				    {1, 11,40,30,5, 23,0},//£n   Pisces
				    {1, 19,28,27,15,51,0},//£o   Pisces
				    {1, 13,45,24,35,2, 0},//£p   Pisces
				    {1, 5, 41,21,28,24,0},//£r1  Pisces
				    {1, 31,29,15,20,45,0},//£b   Pisces
				    {1, 45,24,9, 9, 28,0},//£j   Pisces
				    {2, 2, 3, 2, 45,50,0},//£\   Pisces
				    {1, 41,26,5, 29,15,0},//£h   Pisces
				    {1, 30,11,6, 8, 38,0},//£g   Pisces
				    {1, 13,44,7, 34,32,0},//£a   Pisces
				    {1, 2, 57,7, 53,24,0},//£`   Pisces
				    {0, 48,41,7, 35,7, 0},//£_   Pisces
				    {23,59,19,6, 51,49,0},//£s   Pisces
				    {23,39,57,5, 37,38,0},//£d   Pisces
				    {23,27,58,6, 22,45,0},//£c   Pisces
				    {23,17,9, 3, 16,56,0},//£^   Pisces
				},//127
				{
				    {22,57,39,29,37,19,1},//£\   Piscis_Austrinus
				    {22,55,57,32,32,23,1},//£_   Piscis_Austrinus
				    {22,52,32,32,52,32,1},//£^   Piscis_Austrinus
				    {22,31,30,32,20,46,1},//£]   Piscis_Austrinus
				    {22,10,8, 32,32,54,1},//£n   Piscis_Austrinus
				    {21,44,57,33,1, 32,1},//£d   Piscis_Austrinus
				    {22,14,19,27,46,1, 1},//£f   Piscis_Austrinus
				    {22,40,39,27,2, 37,1},//£`   Piscis_Austrinus
				    {22,57,39,29,37,19,1},//£\   Piscis_Austrinus
				},//128
				{
				    {8, 11,16,12,55,37,1},//   19Puppis
				    {8, 9, 2, 19,14,42,1},//   16Puppis
				    {8, 7, 33,24,18,16,1},//£l   Puppis
				    {8, 3, 35,40,0, 12,1},//£a   Puppis
				    {7, 49,14,46,22,24,1},//P    Puppis
				    {7, 29,14,43,18,7, 1},//£m   Puppis
				},//129
				{
				    {8, 7, 33,24,18,16,1},//£l   Puppis
				    {7, 56,52,22,52,48,1},//j    Puppis
				    {7, 49,18,24,51,35,1},//£i   Puppis
				    {7, 38,50,26,48,14,1},//k1   Puppis
				    {7, 35,23,28,22,9, 1},//p    Puppis
				    {7, 17,9, 37,5, 51,1},//£k   Puppis
				    {6, 37,46,43,11,45,1},//£h   Puppis
				    {6, 49,56,50,36,52,1},//£n   Puppis
				},//130
				{
				    {8, 40,6, 35,18,30,1},//£]   Pyxis
				    {8, 43,36,33,11,11,1},//£\   Pyxis
				    {8, 50,32,27,42,36,1},//£^   Pyxis
				},//131
				{
				    {4, 14,25,62,28,26,1},//£\   Reticulum
				    {3, 44,12,64,48,26,1},//£]   Reticulum
				    {3, 58,45,61,24,0, 1},//£_   Reticulum
				    {4, 1, 18,61,4, 45,1},//£d   Reticulum
				    {4, 16,29,59,18,6, 1},//£`   Reticulum
				    {4, 14,25,62,28,26,1},//£\   Reticulum
				},//132
				{
				    {19,40,6, 18,0, 50,0},//£\   Sagitta
				    {19,47,23,18,32,3, 0},//£_   Sagitta
				    {19,41,3, 17,28,34,0},//£]   Sagitta
				},//133
				{
				    {19,47,23,18,32,3, 0},//£_   Sagitta
				    {19,58,45,19,29,32,0},//£^   Sagitta
				},//134
				{
				    {19,23,53,40,36,56,1},//£\   Sagittarius
				    {19,55,16,41,52,6, 1},//£d   Sagittarius
				    {19,22,38,44,27,32,1},//£]1  Sagittarius
				},//135
				{
				    {19,55,16,41,52,6, 1},//£d   Sagittarius
				    {19,59,44,35,16,34,1},//£c1  Sagittarius
				    {20,2, 39,27,42,36,1},//c    Sagittarius
				    {19,55,50,26,17,59,1},//£s   Sagittarius
				    {19,36,42,24,53,1, 1},//h2   Sagittarius
				    {19,6, 56,27,40,11,1},//£n   Sagittarius
				    {19,2, 37,29,52,48,1},//£a   Sagittarius
				    {18,45,39,26,59,27,1},//£p   Sagittarius
				    {18,55,16,26,17,48,1},//£m   Sagittarius
				    {19,6, 56,27,40,11,1},//£n   Sagittarius
				},//136
				{
				    {19,21,44,15,57,18,1},//£o   Sagittarius
				    {19,21,40,17,50,50,1},//£l1  Sagittarius
				    {19,17,38,18,57,10,1},//d    Sagittarius

				    {19,4, 41,21,44,29,1},//£j   Sagittarius
				    {18,57,44,21,6, 24,1},//£i2  Sagittarius
				},//137
				{
				    {19,4, 41,21,44,29,1},//£j   Sagittarius
				    {18,55,16,26,17,48,1},//£m   Sagittarius
				    {18,27,58,25,25,16,1},//£f   Sagittarius
				    {18,13,46,21,3, 32,1},//£g   Sagittarius
				},//138
				{
				    {18,27,58,25,25,16,1},//£f   Sagittarius
				    {18,20,60,29,49,41,1},//£_   Sagittarius
				    {18,5, 49,30,25,25,1},//£^2  Sagittarius
				    {17,47,34,27,49,51,1},//    3Sagittarius
				},//139
				{
				    {18,20,60,29,49,41,1},//£_   Sagittarius
				    {18,24,10,34,23,4, 1},//£`   Sagittarius
				    {18,17,38,36,45,41,1},//£b   Sagittarius
				},//140
				{
				    {15,56,53,29,12,50,1},//£l   Scorpius
				    {15,58,51,26,6, 51,1},//£k   Scorpius
				    {16,0, 20,22,37,18,1},//£_   Scorpius
				    {16,5, 26,19,48,19,1},//£]1  Scorpius
				    {16,11,60,19,27,38,1},//£h   Scorpius
				},//141
				{
				    {15,58,51,26,6, 51,1},//£k   Scorpius
				    {16,21,11,25,35,34,1},//£m   Scorpius
				    {16,6, 48,20,40,9, 1},//£s1  Scorpius
				    {16,5, 26,19,48,19,1},//£]1  Scorpius
				},//142
				{
				    {16,21,11,25,35,34,1},//£m   Scorpius
				    {16,29,24,26,25,55,1},//£\   Scorpius
				    {16,35,53,28,12,58,1},//£n   Scorpius
				    {16,50,10,34,17,33,1},//£`   Scorpius
				    {16,51,52,38,2, 50,1},//£g1  Scorpius
				    {16,54,35,42,21,39,1},//£a2  Scorpius
				    {17,12,9, 43,14,19,1},//£b   Scorpius
				    {17,37,19,42,59,52,1},//£c   Scorpius
				    {17,47,35,40,7, 37,1},//£d1  Scorpius
				},//143
				{
				    {17,33,37,37,6, 14,1},//£f   Scorpius
				    {17,42,29,39,1, 48,1},//£e   Scorpius
				    {17,47,35,40,7, 37,1},//£d1  Scorpius
				    {17,49,51,37,2, 36,1},//G    Scorpius
				},//144
				{
				    {0, 58,36,29,21,27,1},//£\   Sculptor
				    {23,48,55,28,7, 48,1},//£_   Sculptor
				    {23,18,49,32,31,55,1},//£^   Sculptor
				    {23,32,58,37,49,6, 1},//£]   Sculptor
				},//145
				{
				    {18,23,40,8, 56,4, 1},//£a   Scutum
				    {18,35,12,8, 14,36,1},//£\   Scutum
				    {18,47,10,4, 44,52,1},//£]   Scutum
				    {18,43,31,8, 16,31,1},//£`   Scutum
				    {18,42,16,9, 3, 9, 1},//£_   Scutum
				    {18,29,12,14,33,57,1},//£^   Scutum
				    {18,35,12,8, 14,36,1},//£\   Scutum
				},//146
				{
				    {18,21,19,2, 53,50,1},//£b   Serpens
				    {17,41,25,12,52,31,1},//£j   Serpens
				    {17,37,35,15,23,54,1},//£i   Serpens
				},//147
				{
				    {15,49,37,3, 25,48,1},//£g   Serpens
				    {15,50,49,4, 28,39,0},//£`   Serpens
				    {15,44,16,6, 25,32,0},//£\   Serpens
				    {15,34,48,10,32,20,0},//£_   Serpens
				    {15,46,11,15,25,19,0},//£]   Serpens
				    {15,48,44,18,8, 30,0},//£e   Serpens
				    {15,56,27,15,39,53,0},//£^   Serpens
				    {15,46,11,15,25,19,0},//£]   Serpens
				},//148
				{
				    {10,30,18,0, 38,13,1},//£]   Sextans
				    {10,7, 56,0, 22,18,1},//£\   Sextans
				    {9, 52,30,8, 6, 18,1},//£^   Sextans
				},//149
				{
				    {5, 26,18,28,36,28,0},//£]   Taurus
				    {4, 42,15,22,57,25,0},//£n   Taurus
				    {4, 25,22,22,17,38,0},//£e1  Taurus
				    {4, 28,37,19,10,50,0},//£`   Taurus
				    {4, 22,56,17,32,33,0},//£_1  Taurus
				    {4, 19,48,15,37,40,0},//£^   Taurus
				    {4, 28,40,15,52,15,0},//£c2  Taurus
				    {4, 35,55,16,30,35,0},//£\   Taurus
				    {5, 37,39,21,8, 33,0},//£a   Taurus
				},//150
				{
				    {3, 47,29,24,6, 19,0},//£b   Taurus
				    {4, 4, 42,22,4, 55,0},//A1   Taurus
				    {4, 17,16,20,34,44,0},//£s   Taurus
				    {4, 28,37,19,10,50,0},//£`   Taurus
				    {4, 22,56,17,32,33,0},//£_1  Taurus
				    {4, 19,48,15,37,40,0},//£^   Taurus
				    {4, 0, 41,12,29,25,0},//£f   Taurus
				    {3, 30,52,12,56,12,0},//f    Taurus
				    {3, 27,10,9, 43,58,0},//£i   Taurus
				    {3, 24,49,9, 1, 45,0},//£j   Taurus
				},//151
				{
				    {4, 0, 41,12,29,25,0},//£f   Taurus
				    {4, 15,32,8, 53,33,0},//£g   Taurus
				    {4, 3, 9, 5, 59,22,0},//£h   Taurus
				},//152
				{
				    {18,11,14,45,57,16,1},//£`   Telescopium
				    {18,26,58,45,58,6, 1},//£\   Telescopium
				    {18,28,50,49,4, 12,1},//£a   Telescopium
				},//153
				{
				    {1, 53,5, 29,34,46,0},//£\   Triangulum
				    {2, 9, 33,34,59,15,0},//£]   Triangulum
				    {2, 17,19,33,50,50,0},//£^   Triangulum
				    {1, 53,5, 29,34,46,0},//£\   Triangulum
				},//154
				{
				    {16,48,40,69,1, 40,1},//£\   Triangulum_Australe
				    {15,55,9, 63,25,47,1},//£]   Triangulum_Australe
				    {15,18,55,68,40,46,1},//£^   Triangulum_Australe
				    {16,48,40,69,1, 40,1},//£\   Triangulum_Australe
				},//155
				{
				    {22,27,20,64,57,59,1},//£_   Tucana
				    {22,18,30,60,15,34,1},//£\   Tucana
				    {23,17,26,58,14,9, 1},//£^   Tucana
				    {0, 31,33,62,57,29,1},//£]1  Tucana
				    {0, 20,2, 64,52,39,1},//£a   Tucana
				    {23,57,35,64,17,53,1},//£b   Tucana
				    {23,17,26,58,14,9, 1},//£^   Tucana
				},//156
				{
				    {13,47,33,49,18,48,0},//£b   Ursa_Major
				    {13,23,55,54,55,32,0},//£a   Ursa_Major
				    {12,54,2, 55,57,35,0},//£`   Ursa_Major
				    {12,15,25,57,1, 57,0},//£_   Ursa_Major
				    {11,53,50,53,41,41,0},//£^   Ursa_Major
				    {11,1, 50,56,22,56,0},//£]   Ursa_Major
				    {11,3, 44,61,45,4, 0},//£\   Ursa_Major
				    {9, 31,32,63,3, 42,0},//h    Ursa_Major
				    {8, 30,16,60,43,6, 0},//£j   Ursa_Major
				},//157
				{
				    {11,1, 50,56,22,56,0},//£]   Ursa_Major
				    {9, 32,52,51,40,43,0},//£c   Ursa_Major
				    {8, 59,13,48,2, 32,0},//£d   Ursa_Major
				    {9, 3, 38,47,9, 24,0},//£e   Ursa_Major
				},//158
				{
				    {11,53,50,53,41,41,0},//£^   Ursa_Major
				    {11,46,3, 47,46,46,0},//£q   Ursa_Major
				    {11,9, 40,44,29,55,0},//£r   Ursa_Major
				    {10,22,20,41,29,58,0},//£g   Ursa_Major
				    {10,17,6, 42,54,52,0},//£f   Ursa_Major
				},//159
				{
				    {11,9, 40,44,29,55,0},//£r   Ursa_Major
				    {11,18,29,33,5, 39,0},//£h   Ursa_Major
				    {11,18,11,31,31,51,0},//£i   Ursa_Major
				},//160
				{
				    {2, 31,47,89,15,51,0},//£\   Ursa_Minor
				    {17,32,13,86,35,11,0},//£_   Ursa_Minor
				    {16,45,58,82,2, 14,0},//£`   Ursa_Minor
				    {15,44,3, 77,47,40,0},//£a   Ursa_Minor
				    {14,50,42,74,9, 20,0},//£]   Ursa_Minor
				    {15,20,44,71,50,2, 0},//£^   Ursa_Minor
				    {16,17,30,75,45,17,0},//£b   Ursa_Minor
				    {15,44,3, 77,47,40,0},//£a   Ursa_Minor
				},//161
				{
				    {14,50,42,74,9, 20,0},//£]   Ursa_Minor
				    {14,27,32,75,41,45,0},//    5Ursa_Minor
				},//162
				{
				    {8, 9, 32,47,20,12,1},//£^2  Vela
				    {8, 40,18,52,55,19,1},//£j   Vela
				    {8, 44,42,54,42,31,1},//£_   Vela
				    {9, 22,7, 55,0, 38,1},//£e   Vela
				    {9, 56,52,54,34,4, 1},//£p   Vela
				    {10,46,46,49,25,12,1},//£g   Vela
				    {10,37,18,48,13,32,1},//p    Vela
				    {10,14,44,42,7, 19,1},//q    Vela
				    {9, 30,42,40,28,1, 1},//£r   Vela
				    {9, 7, 60,43,25,57,1},//£f   Vela
				    {8, 44,24,42,38,58,1},//d    Vela
				    {8, 37,39,42,59,21,1},//e    Vela
				    {8, 9, 32,47,20,12,1},//£^2  Vela
				},//163
				{
				    {12,5, 13,8, 43,58,0},//£j   Virgo
				    {12,0, 52,6, 36,52,0},//£k   Virgo
				    {11,45,52,6, 31,47,0},//£h   Virgo
				    {11,50,41,1, 45,55,0},//£]   Virgo
				    {12,19,54,0, 40,0, 1},//£b   Virgo
				    {12,41,40,1, 26,58,1},//£^   Virgo
				    {12,55,36,3, 23,51,0},//£_   Virgo
				    {13,2, 11,10,57,33,0},//£`   Virgo
				},//164
				{
				    {12,55,36,3, 23,51,0},//£_   Virgo
				    {13,34,42,0, 35,45,1},//£a   Virgo
				    {13,25,12,11,9, 40,1},//£\   Virgo
				    {13,9, 57,5, 32,20,1},//£c   Virgo
				    {12,41,40,1, 26,58,1},//£^   Virgo
				},//165
				{
				    {14,43,4, 5, 39,27,1},//£g   Virgo
				    {14,16,1, 5, 59,58,1},//£d   Virgo
				    {14,12,54,10,16,27,1},//£e   Virgo
				    {13,25,12,11,9, 40,1},//£\   Virgo
				    {13,34,42,0, 35,45,1},//£a   Virgo
				    {14,1, 39,1, 32,40,0},//£n   Virgo
				    {14,46,15,1, 53,35,0},//  109Virgo
				},//166
				{
				    {9, 2, 27,66,23,45,1},//£\   Volans
				    {8, 25,44,66,8, 12,1},//£]   Volans
				    {8, 7, 56,68,37,2, 1},//£`   Volans
				    {7, 41,49,72,36,22,1},//£a   Volans
				    {7, 8, 45,70,29,57,1},//£^2  Volans
				    {7, 16,50,67,57,26,1},//£_   Volans
				    {8, 7, 56,68,37,2, 1},//£`   Volans
				},//167
				{
				    {19,16,13,21,23,26,0},//    1Vulpecula
				    {19,28,42,24,39,55,0},//£\   Vulpecula
				    {19,53,28,24,4, 46,0},//   13Vulpecula
				},//168
		};

		//int num_string = RD.length;
		for (int i=0;i<nrOfStrings;i++) {
			int len_string = RD[i].length;
			float line_coords[] = new float[len_string*3];
			int count = 0;
			for (int j=0;j<len_string;j++){
				double RA = ((RD[i][j][0]*3600.0+
					RD[i][j][1]*60.0+
					RD[i][j][2])/86400.0)*2*StrictMath.PI;
				double DE = ((RD[i][j][3]+
					RD[i][j][4]/60.0+
					RD[i][j][5]/3600.0)/180.0)*StrictMath.PI;

				if (RD[i][j][6]==1) {
					DE = -DE;
				}

				line_coords[count++] = (float)CoordCal.cvRDtoX(RA,DE);
				line_coords[count++] = (float)CoordCal.cvRDtoY(RA,DE);
				line_coords[count++] = (float)CoordCal.cvRDtoZ(DE);
			}
			// float has 4 bytes
			ByteBuffer vbb = ByteBuffer.allocateDirect(line_coords.length * 4);
			vbb.order(ByteOrder.nativeOrder());
			vertexLineBuffer[i] = vbb.asFloatBuffer();
			vertexLineBuffer[i].put(line_coords);
			vertexLineBuffer[i].position(0);
		}
    }
    
	/**
	 * Load the textures
	 * 
	 * @param gl - The GL Context
	 * @param context - The Activity context
	 */
	public void loadStarTexture(GL10 gl, Context context) {
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
	
    private void loadSolarTexture (GL10 gl, Context context) 
    {
		InputStream is = context.getResources().openRawResource(R.raw.earth);
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
  		int width = bitmap.getWidth();
    	int height = bitmap.getHeight();

    	Log.e("BMPonLoad", Integer.toString(width) + "x" + Integer.toString(height));
    	
  		sb = ByteBuffer.allocateDirect(width*height*4*4); //w*h*{RGBA}*4
  		sb.order(ByteOrder.nativeOrder());
  		fb = sb.asFloatBuffer();

  		int color, red, green, blue, alpha;
	
		for( int i = 0; i < width * height; i++ ) {
			color = bitmap.getPixel((i % width), (i / width));
			blue = color & 0xff;
			green = (color >> 8) & 0xff;
			red = (color >> 16) & 0xff;
			alpha = (color >> 24) & 0xff;

			fb.put((float)red/255);
			fb.put((float)green/255);
			fb.put((float)blue/255);
			fb.put((float)alpha/255);
		}
		
  		fb.position(0);
  		sb.position(0);
  		
  		//Generate one texture pointer...
		gl.glGenTextures(1, textures, 0);

		//...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		//Create Nearest Filtered Texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		//GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, fb);
	
		//Clean up
		bitmap.recycle();
    }
	
	/**
	 * calculate where eyes are looking for
	 * 
	 */
	public void eyeCenterCal(){
		
		double cenAltitude = Math.toRadians(lookupdown);
		double cenAzimuth = Math.toRadians(yrot);
		// lstr = TimeCal.cvHtoRadians(t1.getLSTh());
		
		double cenDec = CoordCal.cvAAtoDec(cenAltitude, cenAzimuth);
		double cenRa = CoordCal.cvAAtoRA(cenAltitude, cenAzimuth, cenDec, t1.getLSTr());
		//System.out.println("cenDec:"+Math.toDegrees(cenDec)+" cenRa:"+Math.toDegrees(cenRa));
		centerX = (float) CoordCal.cvRDtoX(cenRa, cenDec);
		centerY = (float) CoordCal.cvRDtoY(cenRa, cenDec);
		centerZ = (float) CoordCal.cvRDtoZ(cenDec);
	}
	
	public void eyeUpCal(){
		
		float upAlt;
		float upAzi;
		//System.out.println("yrot:"+yrot);
		if(lookupdown >= 0)
			upAlt = 90f - lookupdown;
		else
			upAlt = 90f + lookupdown;
		
		if(lookupdown >= 0)
			upAzi = yrot + 180;
		else
			upAzi = yrot;
			
		while (upAzi >= 360f)
			upAzi -= 360f;
		
		//System.out.println("upAlt:"+upAlt);
		double upAltitude = Math.toRadians(upAlt);
		double upAzimuth = Math.toRadians(upAzi);
		//double lstr = TimeCal.cvHtoRadians(t1.getLSTh());
		
		//if(upAzimuth > Math.toRadians(2*Math.PI))
		//	upAzimuth -= 2*Math.PI;
		
		double upDec = CoordCal.cvAAtoDec(upAltitude, upAzimuth);
		//System.out.println("upAltitide:"+upAltitude+" upAzimuth:"+upAzimuth+" lstr:"+t1.getLSTr());
		double upRa = CoordCal.cvAAtoRA(upAltitude, upAzimuth, upDec, t1.getLSTr());
		//System.out.println("upDec:"+Math.toDegrees(upDec)+" upRa:"+Math.toDegrees(upRa));
		upX = (float) CoordCal.cvRDtoX(upRa, upDec);
		upY = (float) CoordCal.cvRDtoY(upRa, upDec);
		upZ = (float) CoordCal.cvRDtoZ(upDec);
	}
	
	public void selectObject(float winX, float winY){
		int[] view = {0,0, width, height}; 
		float[] obj = new float[3];
		int result;
		result = Glu.gluUnProject(winX, winY, 0f, mGrabber.mModelView, 0, mGrabber.mProjection, 0, view, 0, obj,0);
		System.out.println("objX:"+obj[0]+" objY:"+obj[1]+" obyZ:"+obj[2]);
	}

	public void createHorizonLine(){
		
		double azi,alt = 90 - HORIZON_CIRCLE_INTERVAL;
		double dec, ra;
		
		//create memory space for Altitude lines
		HorizonLineBuffer = new FloatBuffer[NUM_OF_HORIZON_CIRCLES];
		float[] coords = new float[NUM_OF_CIRCLE_VERTICES*3];
		
		for(int j=0; j < NUM_OF_HORIZON_CIRCLES; j++){
			azi = 0;
			for(int i = 0; i < NUM_OF_CIRCLE_VERTICES; i++){
				
				dec = CoordCal.cvAAtoDec(Math.toRadians(alt), Math.toRadians(azi));
				ra = CoordCal.cvAAtoRA(Math.toRadians(alt), Math.toRadians(azi), dec, t1.getLSTr());
				coords[i*3] = (float) CoordCal.cvRDtoX(ra, dec);
				coords[i*3+1] = (float) CoordCal.cvRDtoY(ra, dec);
				coords[i*3+2] = (float) CoordCal.cvRDtoZ(dec);
				/*
				coords[i*3] = (float) CoordCal.cvRDtoX(Math.toRadians(angle), Math.toRadians(circle));
				coords[i*3+1] = (float) CoordCal.cvRDtoY(Math.toRadians(angle), Math.toRadians(circle));
				coords[i*3+2] = (float) CoordCal.cvRDtoZ(Math.toRadians(circle));
				*/
				azi += CIRCLE_DEGREE;
			}
			alt -= HORIZON_CIRCLE_INTERVAL;
			
			ByteBuffer lb = ByteBuffer.allocateDirect(coords.length * 4);
			lb.order(ByteOrder.nativeOrder());
			HorizonLineBuffer[j] = lb.asFloatBuffer();
			HorizonLineBuffer[j].put(coords);
			HorizonLineBuffer[j].position(0);
		}
	}
	
	public void createVerticalLine(){

		double azi=0, alt=0;
		double ra,dec;
		 
		VerticalLineBuffer = new FloatBuffer[NUM_OF_VERTICAL_LINES];
		float[] coords = new float[NUM_OF_LINE_VERTICES*3];
		
		for(int i=0; i < NUM_OF_VERTICAL_LINES; i++){
			alt = 90 - HORIZON_CIRCLE_INTERVAL;
			for(int j=0;j < NUM_OF_LINE_VERTICES;j++){				
				dec = CoordCal.cvAAtoDec(Math.toRadians(alt), Math.toRadians(azi));
				ra = CoordCal.cvAAtoRA(Math.toRadians(alt), Math.toRadians(azi), dec, t1.getLSTr());
				coords[j*3] = (float) CoordCal.cvRDtoX(ra, dec);
				coords[j*3+1] = (float) CoordCal.cvRDtoY(ra, dec);
				coords[j*3+2] = (float) CoordCal.cvRDtoZ(dec);
				/*
				coords[j*3] = (float) CoordCal.cvRDtoX(Math.toRadians(azi), Math.toRadians(alt));
				coords[j*3+1] = (float) CoordCal.cvRDtoY(Math.toRadians(azi), Math.toRadians(alt));
				coords[j*3+2] = (float) CoordCal.cvRDtoZ(Math.toRadians(alt));
				*/
				alt -= LINE_DEGREE;
			}
			azi += VERTICAL_LINE_INTERVAL;
			
			ByteBuffer vlb = ByteBuffer.allocateDirect(coords.length * 4);
			vlb.order(ByteOrder.nativeOrder());
			VerticalLineBuffer[i] = vlb.asFloatBuffer();
			VerticalLineBuffer[i].put(coords);
			VerticalLineBuffer[i].position(0);
		}
	}
	
	public void createHorizonRDLine(){
		
		double azi,alt = 90 - HORIZON_CIRCLE_INTERVAL;
		//double dec, ra;
		
		//create memory space for Altitude lines
		HorizonRDLineBuffer = new FloatBuffer[NUM_OF_HORIZON_CIRCLES];
		float[] coords = new float[NUM_OF_CIRCLE_VERTICES*3];
		
		for(int j=0; j < NUM_OF_HORIZON_CIRCLES; j++){
			azi = 0;
			for(int i = 0; i < NUM_OF_CIRCLE_VERTICES; i++){
				/*
				dec = CoordCal.cvAAtoDec(Math.toRadians(alt), Math.toRadians(azi));
				ra = CoordCal.cvAAtoRA(Math.toRadians(alt), Math.toRadians(azi), dec, t1.getLSTr());
				coords[i*3] = (float) CoordCal.cvRDtoX(ra, dec);
				coords[i*3+1] = (float) CoordCal.cvRDtoY(ra, dec);
				coords[i*3+2] = (float) CoordCal.cvRDtoZ(dec);
				*/
				coords[i*3] = (float) CoordCal.cvRDtoX(Math.toRadians(azi), Math.toRadians(alt));
				coords[i*3+1] = (float) CoordCal.cvRDtoY(Math.toRadians(azi), Math.toRadians(alt));
				coords[i*3+2] = (float) CoordCal.cvRDtoZ(Math.toRadians(alt));
				
				azi += CIRCLE_DEGREE;
			}
			alt -= HORIZON_CIRCLE_INTERVAL;
			
			ByteBuffer lb = ByteBuffer.allocateDirect(coords.length * 4);
			lb.order(ByteOrder.nativeOrder());
			HorizonRDLineBuffer[j] = lb.asFloatBuffer();
			HorizonRDLineBuffer[j].put(coords);
			HorizonRDLineBuffer[j].position(0);
		}
	}
	
	public void createVerticalRDLine(){

		double azi=0, alt=0;
		 
		VerticalRDLineBuffer = new FloatBuffer[NUM_OF_VERTICAL_LINES];
		float[] coords = new float[NUM_OF_LINE_VERTICES*3];
		
		for(int i=0; i < NUM_OF_VERTICAL_LINES; i++){
			alt = 90 - HORIZON_CIRCLE_INTERVAL;
			for(int j=0;j < NUM_OF_LINE_VERTICES;j++){				
				coords[j*3] = (float) CoordCal.cvRDtoX(Math.toRadians(azi), Math.toRadians(alt));
				coords[j*3+1] = (float) CoordCal.cvRDtoY(Math.toRadians(azi), Math.toRadians(alt));
				coords[j*3+2] = (float) CoordCal.cvRDtoZ(Math.toRadians(alt));		
				alt -= LINE_DEGREE;
			}
			azi += VERTICAL_LINE_INTERVAL;
			
			ByteBuffer vlb = ByteBuffer.allocateDirect(coords.length * 4);
			vlb.order(ByteOrder.nativeOrder());
			VerticalRDLineBuffer[i] = vlb.asFloatBuffer();
			VerticalRDLineBuffer[i].put(coords);
			VerticalRDLineBuffer[i].position(0);
		}
	}
	
	public void createMeridianLine(){
		
		double azi=0, alt=0;
		double ra,dec;
		
		MeridianLineBuffer = new FloatBuffer[2];
		float[] coords = new float[NUM_OF_MERIDIAN_LINE_VERTICES*3];
		
		for(int i=0; i < 2; i++){
			alt = 90;
			for(int j=0;j < NUM_OF_MERIDIAN_LINE_VERTICES;j++){				
				dec = CoordCal.cvAAtoDec(Math.toRadians(alt), Math.toRadians(azi));
				ra = CoordCal.cvAAtoRA(Math.toRadians(alt), Math.toRadians(azi), dec, t1.getLSTr());
				coords[j*3] = (float) CoordCal.cvRDtoX(ra, dec);
				coords[j*3+1] = (float) CoordCal.cvRDtoY(ra, dec);
				coords[j*3+2] = (float) CoordCal.cvRDtoZ(dec);
				alt -= LINE_DEGREE;
			}
			azi += 180.0;
			
			ByteBuffer vlb = ByteBuffer.allocateDirect(coords.length * 4);
			vlb.order(ByteOrder.nativeOrder());
			MeridianLineBuffer[i] = vlb.asFloatBuffer();
			MeridianLineBuffer[i].put(coords);
			MeridianLineBuffer[i].position(0);
		}
	}
	
	public void createCrossLine(float x, float y, float z){
		//calculate the point's x,y,z
		float lineScale = 3f;
		
		float uLen = (float) Math.sqrt(x*x+y*y+z*z);
		float[] u = {x/uLen, y/uLen, z/uLen};
		float vLen = (float) Math.sqrt(1+1+((-x-y)/z)*((-x-y)/z));
		float[] v = {1f/vLen, 1f/vLen, ((-x-y)/z)/vLen};
		//float a = (float) (Math.pow(u[2]*v[3]-u[3]*v[2], 2.0));
		float sLen =(float) Math.sqrt(Math.pow(u[1]*v[2]-u[2]*v[1], 2)+Math.pow(u[2]*v[0]-u[0]*v[2], 2)+Math.pow(u[0]*v[1]-u[1]*v[0], 2));
		float[] s ={ (u[1]*v[2]-u[2]*v[1])/sLen, (u[2]*v[0]-u[0]*v[2])/sLen, (u[0]*v[1]-u[1]*v[0])/sLen};
		
		//float fovScale = (float) Math.tan(fovy /2.0);
		for(int i =0;i<3;i++){
			v[i] = v[i] * lineScale;
			s[i] = s[i] * lineScale;
		}
		
		
		x = x * coordinateScale;
		y = y * coordinateScale;
		z = z * coordinateScale;
		
		float[] coords = {
				//x*1f, y*1f, z*1f,
				//x-v[0]-s[0], y-v[1]-s[1], z-v[2]-s[2],
				//x+v[0]-s[0], y+v[1]-s[1], z+v[2]-s[2],
				//x-v[0]+s[0], y-v[1]+s[1], z-v[2]+s[2],
				//x+v[0]+s[0], y+v[1]+s[1], z+v[2]+s[2],
				x-2*v[0],y-2*v[1],z-2*v[2],
				x-1*v[0],y-1*v[1],z-1*v[2],
				x+2*v[0],y+2*v[1],z+2*v[2],
				x+1*v[0],y+1*v[1],z+1*v[2],
				x-2*s[0],y-2*s[1],z-2*s[2],
				x-1*s[0],y-1*s[1],z-1*s[2],
				x+2*s[0],y+2*s[1],z+2*s[2],
				x+1*s[0],y+1*s[1],z+1*s[2],
		};
		ByteBuffer vlb = ByteBuffer.allocateDirect(coords.length * 4);
		vlb.order(ByteOrder.nativeOrder());
		CrossLineBuffer = vlb.asFloatBuffer();
		CrossLineBuffer.put(coords);
		CrossLineBuffer.position(0);
	}

	
}