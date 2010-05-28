package tw.edu.ntu;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * This class contains, loads, initiates textures
 * and draws our stars
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class Stars {
	
	private int num;					//Basic number of stars	
	private Star[] stars;					//Hold all our star instances in this array
	//private float zoom = -15.0f;
	
	/** Our texture pointer */
	private int[] textures = new int[1];
	
	/**
	 * Constructor for our stars holder
	 * with the number of maximum stars.
	 * Initiate all stars with random 
	 * numbers.
	 * 
	 * @param num - Number of stars
	 */
	public Stars(int num, SAOData[] data) {
		
		this.num = num;
		
		//Initiate the stars array
		stars = new Star[num];
		
		//Initiate our stars
		for(int i = 0; i < num; i++) {
			
			//Load coordination and color
			double ra = data[i].getRa();
			double dec = data[i].getDec();
			double mag = data[i].getMagnitude() / 100.0;
			byte spec = data[i].getSpec();
			float x = (float)Math.cos(ra)*(float)Math.cos(dec);
			float y =(float)Math.sin(ra)*(float)Math.cos(dec);
			float z =(float)Math.sin(dec);
			
			stars[i] = new Star();
			stars[i].x = x*1f;
			stars[i].y = y*1f;
			stars[i].z = z*1f;
			stars[i].magnitude = (float) mag;
			stars[i].setSpectral(spec); 
		}
		//for(int i = 0; i < num; i++)
			//System.out.println("x:"+stars[i].x+" y:"+stars[i].y+" z:"+stars[i].z+" r:"+stars[i].r+" g:"+stars[i].g+" b:"+stars[i].b);
	}
	
	/**
	 * The Stars drawing function.
	 * 
	 * @param gl - The GL Context
	 * @param twinkle - Twinkle on or off
	 */
	public void draw(GL10 gl, boolean twinkle) {
		//Bind the star texture for all stars
		//gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
				
		//System.out.println("num:"+num);
		//Iterate through all stars
		for(int i = 0; i < 1; i++) {
			//Recover the current star into an object
			Star star = stars[i];
			//gl.glTranslatef(0.0f, 0.0f, zoom); 				//Zoom Into The Screen (Using The Value In 'zoom')
			
			//Twinkle, twinkle little star
			
			if(twinkle) {
				//Twinkle with an over drawn second star
				gl.glColor4f(	stars[(num - i) - 1].r, 
								stars[(num - i) - 1].g, 
								stars[(num - i) - 1].b, 
								1.0f);			
				//Draw
				star.draw(gl);
			}
			
			//set color
			//gl.glColor4f(star.r, star.g, star.b, 1.0f);
			//Draw
			star.draw(gl);		}
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
}
