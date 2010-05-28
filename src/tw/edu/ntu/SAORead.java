package tw.edu.ntu;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class SAORead {
	private Context context;
	private SAOData[] SAO;
	private int nrOfStars;
	
	public SAORead(Context context){
		this.context = context;
	}
	
	public SAOData getSAO(int i){
		return SAO[i];
	}
	
	public SAOData[] getSAOAll(){
		return SAO;
	}
	
	public int getNrOfStars(){
		return nrOfStars;
	}
	
	public void read(){
		DataInputStream dis = new DataInputStream(this.context.getResources().openRawResource(R.raw.saora_429_less4));
		try { 
			//System.out.println( "file: SAOra_100"); 
			int i, starID;
			short magnitude; 
			double ra,dec;
			byte[] spectral= new byte[2];
			
			Log.d("filename", "ready to read");
			nrOfStars = dis.readInt(); 
			Log.d("filename", Integer.toString(nrOfStars));
			
			SAO = new SAOData[nrOfStars];
			for(i = 0; i < nrOfStars;i++){
				starID = dis.readInt();
				ra = dis.readDouble();
				dec = dis.readDouble();
				spectral[0] = dis.readByte();
				spectral[1] = dis.readByte();
				magnitude = dis.readShort();
				SAO[i] = new SAOData();
				SAO[i].setData(starID, ra, dec, spectral, magnitude);
				//SAO Data information
				//System.out.println("Index:" + i);
				//System.out.println("ID:" + SAO[i].getStarID());
				//System.out.println("RA:" + SAO[i].getRa());
				//System.out.println("DEC:" + SAO[i].getDec());
				//String value = new String(SAO[i].getSpectral());
				//System.out.println("Spe:" + value);
				//System.out.println("Mag:" + SAO[i].getMagnitude());
				//System.out.println();
			} 

		} catch (EOFException eof) 
			{System.out.println( "EOF reached " ); } 
		catch (IOException ioe) 
			{System.out.println( "IO error: " + ioe );}
		finally {
			//Always clear and close
			try {
				dis.close();
				dis = null;
			} catch (IOException e) {
			}
		//end read file
		}
	}
}