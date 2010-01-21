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
	private boolean twinkle = false;
	
	int nrOfSolarObjects;
	int nrOfStarObjects;
	int nrOfObjects;
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
	protected boolean mGridVisible = false;
	protected boolean mGridRDVisible = false;
	protected boolean mMeridianVisible = false;
	
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
	private float textureScale = 2f;				//scale of the texture size
	private float centerX, centerY, centerZ;
	private float upX, upY, upZ;
	
	private SAORead reader;
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
	private float[] magnitude;
	
	private int[] textures = new int[2];
	
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
		
		vertexLineBuffer = new FloatBuffer[3];
		
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
        initConstellation1("Cas");
        initConstellation2("UMa");
        initConstellation3("Ori");
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

		//draw Cas line
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexLineBuffer[0]);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 5);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		//draw UMa line
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexLineBuffer[1]);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 7);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		
		//draw UMa line
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexLineBuffer[2]);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 26);
		gl.glEnable(GL10.GL_TEXTURE_2D);
/*for test*/		

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

		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		for (int i=nrOfSolarObjects; i < nrOfObjects; i++){
			gl.glColor4f(textureColor[i][0], textureColor[i][1], textureColor[i][2], 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer[i]);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer[i]);
			//gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer[i]);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
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
		magScale = 1f-((magnitude - 1f)/10f);
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

    public void initConstellation1(String sName) {
    	int RD[][] = {           // Cas
    			{0,9,42,59,12,28},  // beta
    			{0,41,55,56,35,32}, // alpha
    			{0,57,19,60,46,15}, // gamma
    			{1,26,28,60,17,17}, // delta
    			{1,55,8,63,43,9}    // epsilon
    	};

    	float line_coords[] = new float[5*3];
        int count = 0;
    	for (int i=0;i<5;i++){
    		double RA = ((RD[i][0]*3600.0+
    				     RD[i][1]*60.0+
    				     RD[i][2])/86400.0)*2*StrictMath.PI;
    		double DE = ((RD[i][3]+
	                     RD[i][4]/60.0+
	                     RD[i][5]/3600.0)/180.0)*StrictMath.PI;
    		
    		line_coords[count++] = (float)CoordCal.cvRDtoX(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoY(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoZ(DE);
    	};
    	// float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(line_coords.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexLineBuffer[0] = vbb.asFloatBuffer();
		vertexLineBuffer[0].put(line_coords);
		vertexLineBuffer[0].position(0);
    }
 
    public void initConstellation2(String sName) {
    	int RD[][] = {           // UMa
    			{13,47,56,49,15,48},  // eta
    			{13,24,20,54,52,24},  // zeta
    			{12,54,28,55,54,20},  // epsilon
    			{12,15,55,56,58,37},  // delta
    			{11,54,21,53,38,20},  // gamma
    			{11,2,26,56,19,42},   // beta
    			{11,4,21,61,41,50}    //alpha
    	};

    	float line_coords[] = new float[7*3];
        int count = 0;
    	for (int i=0;i<7;i++){
    		double RA = ((RD[i][0]*3600.0+
    				     RD[i][1]*60.0+
    				     RD[i][2])/86400.0)*2*StrictMath.PI;
    		double DE = ((RD[i][3]+
	                     RD[i][4]/60.0+
	                     RD[i][5]/3600.0)/180.0)*StrictMath.PI;
    		
    		line_coords[count++] = (float)CoordCal.cvRDtoX(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoY(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoZ(DE);
    	};
    	// float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(line_coords.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexLineBuffer[1] = vbb.asFloatBuffer();
		vertexLineBuffer[1].put(line_coords);
		vertexLineBuffer[1].position(0);
    }

    public void initConstellation3(String sName) {
    	int RD[][] = {           // Ori
    			{6,12,31,14,12,21,0},
    			{6,8,9,14,45,59,0},
    			{5,54,59,20,16,38,0},
    			{6,4,31,20,8,15,0},
    			{6,12,31,14,12,21,0},
    			{6,2,56,9,38,48,0},
    			{5,55,43,7,24,29,0},
    			
    			{5,41,16,1,56,17,1},
    			{5,48,14,9,40,0,1},
    			{5,15,1,8,11,26,1},
    			{5,32,31,0,17,32,1},
    			{5,36,43,1,11,46,1},
    			{5,41,16,1,56,17,1},
    			{5,36,43,1,11,46,1},
    			{5,32,31,0,17,32,1},
    			{5,25,40,6,21,29,0},
    			{5,35,42,9,56,24,0},
    			{5,55,43,7,24,29,0},
    			{5,35,42,9,56,24,0},
    			{5,25,40,6,21,29,0},
    			
    			{4,55,27,10,9,58,0},
    			{4,51,10,8,55,0,0},
    			{4,50,23,6,58,41,0},
    			{4,51,45,5,37,18,0},
    			{4,54,47,2,27,23,0},
    			{4,59,4,1,43,43,0},
    	};

    	float line_coords[] = new float[26*3];
        int count = 0;
    	for (int i=0;i<26;i++){
    		double RA = ((RD[i][0]*3600.0+
    				     RD[i][1]*60.0+
    				     RD[i][2])/86400.0)*2*StrictMath.PI;
    		double DE = ((RD[i][3]+
                         RD[i][4]/60.0+
                         RD[i][5]/3600.0)/180.0)*StrictMath.PI;

    		if (RD[i][6]==1) {
    			DE = -DE;
    		}
    		
    		line_coords[count++] = (float)CoordCal.cvRDtoX(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoY(RA,DE);
    		line_coords[count++] = (float)CoordCal.cvRDtoZ(DE);
    	};
    	// float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(line_coords.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexLineBuffer[2] = vbb.asFloatBuffer();
		vertexLineBuffer[2].put(line_coords);
		vertexLineBuffer[2].position(0);
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
		//System.out.println("cX:"+centerX+" cY:"+centerY+" cZ:"+centerZ);
	}
	
	public void eyeUpCal(){
		
		float upAlt;
		if(lookupdown >= 0)
			upAlt = 90f - lookupdown;
		else
			upAlt = 90f + lookupdown;
		
		float upAzi = yrot + 180;
			if (upAzi >= 360f)
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
		//System.out.println("uX:"+upX+" uY:"+upY+" uZ:"+upZ);
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

	
}