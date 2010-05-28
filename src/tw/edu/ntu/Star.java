package tw.edu.ntu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * This class is an object representation of 
 * a Star
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class Star {
	
	public float x, y, z;
	public float r, g, b;				//Stars Color
	public float magnitude;
	public byte spectral; 
	
	/** The buffer holding the vertices */
	private FloatBuffer vertexBuffer;
	/** The buffer holding the texture coordinates */
	private FloatBuffer textureBuffer;
	/** The buffer holding the vertices color */
	private FloatBuffer colorBuffer;

	/** The initial vertex definition */
	private float vertices[] = {
								x, y, z,
								//x-0.1f, y-0.1f, z, 		//Bottom Left
								//x+0.1f, y-0.1f, z, 		//Bottom Right
								//x-0.1f, y+0.1f, z,	 	//Top Left
								//x+0.1f, y+0.1f, z 		//Top Right
													};
	
	/** The initial texture coordinates (u, v) */	
	private float texture[] = {
								0.0f, 0.0f, 
								1.0f, 0.0f, 
								0.0f, 1.0f, 
								1.0f, 1.0f,
											};

	/** The initial vertex color*/
	private float color[] = {
								r, g, b, 1.0f
											};
	/**
	 * The Star constructor.
	 * 
	 * Initiate the buffers.
	 */
	public Star() {

		//
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		//
		byteBuf = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuf.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
		
		//
		byteBuf = ByteBuffer.allocateDirect(color.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		colorBuffer = byteBuf.asFloatBuffer();
		colorBuffer.put(color);
		colorBuffer.position(0);
	}

	/**
	 * The object own drawing function.
	 * Called from the renderer to redraw this instance
	 * with possible changes in values.
	 * 
	 * @param gl - The GL Context
	 */
	public void draw(GL10 gl) {
		//Enable the vertex, texture and normal state
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		//gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
		//Point to our buffers
		//gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		//gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		
		//Draw the vertices as triangle strip
		//gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		//Draw point
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
		gl.glDrawArrays(GL10.GL_POINTS, 0, 1);
		
		//Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		//gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		
		System.out.println("x:"+x+" y:"+y+" z:"+z+" r:"+r+" g:"+g+" b:"+b);
	}
	public void setSpectral(byte spectral){
		this.spectral = spectral;
		setStarColor(spectral);
	}
	
	/**
	 * Use spectral to decide star color
	 * 
	 * @param spectral
	 */
	private void setStarColor(byte spectral){
		switch(spectral){
		case 'O':
			r = 162f/255f;
			g = 190f/255f;
			b = 255f/255f;		
			break;
		case 'B':
			r = 220f/255f;
			g = 231f/255f;
			b = 255f/255f;
			break;
		case 'A':
			r = 255f/255f;
			g = 255f/255f;
			b = 255f/255f;
			break;
		case 'F':
			r = 252f/255f;
			g = 255f/255f;
			b = 229f/255f;
			break;
		case 'G':
			r = 255f/255f;
			g = 253f/255f;
			b = 202f/255f;
			break;
		case 'K':
			r = 255f/255f;
			g = 206f/255f; 
			b =  74f/255f;
			break;
		case 'M':
			r = 253f/255f;
			g = 127f/255f;
			b =  58f/255f;
			break;
		default:
			r = 1.0f;
			g = 1.0f;
			b = 1.0f;
			break;
		}
	}
}
