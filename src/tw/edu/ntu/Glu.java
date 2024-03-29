package tw.edu.ntu;

import javax.microedition.khronos.opengles.GL10;

public class Glu{

	private static final float[] _tempGluUnProjectData = new float[40];
	private static final int     _temp_m   = 0;
	private static final int     _temp_A   = 16;
	private static final int     _temp_in  = 32;
	private static final int     _temp_out = 36;
	
	public static int gluUnProject(float winx, float winy, float winz, float model[], int offsetM, float proj[], int offsetP, int viewport[], int offsetV, float[] xyz, int offset) 
	{ 
		/* Normalize between -1 and 1 */ 
		_tempGluUnProjectData[_temp_in]   = (winx - viewport[offsetV]) * 2f / viewport[offsetV+2] - 1.0f;
		_tempGluUnProjectData[_temp_in+1] = (winy - viewport[offsetV+1]) * 2f / viewport[offsetV+3] - 1.0f;
		_tempGluUnProjectData[_temp_in+2] = 2f * winz - 1.0f;
		_tempGluUnProjectData[_temp_in+3] = 1.0f;
		/* Get the inverse */ 
		android.opengl.Matrix.multiplyMM(_tempGluUnProjectData, _temp_A, proj, offsetP, model, offsetM);
		android.opengl.Matrix.invertM(_tempGluUnProjectData, _temp_m, _tempGluUnProjectData, _temp_A);
		android.opengl.Matrix.multiplyMV(_tempGluUnProjectData, _temp_out, _tempGluUnProjectData, _temp_m, _tempGluUnProjectData, _temp_in);
		if (_tempGluUnProjectData[_temp_out+3] == 0.0)
			return GL10.GL_FALSE;
		xyz[offset]  =  _tempGluUnProjectData[_temp_out  ] / _tempGluUnProjectData[_temp_out+3];
		xyz[offset+1] = _tempGluUnProjectData[_temp_out+1] / _tempGluUnProjectData[_temp_out+3];
		xyz[offset+2] = _tempGluUnProjectData[_temp_out+2] / _tempGluUnProjectData[_temp_out+3];
		return GL10.GL_TRUE;
	} 
}
