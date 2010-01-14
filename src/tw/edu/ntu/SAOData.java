package tw.edu.ntu;

public class SAOData{
	private int starID;
	private double ra;
	private double dec;
	private byte[] spectral = new byte[2];
	private short magnitude;
	
	public void setData(int starID, double ra, double dec, byte[] spectral, short magnitude){
		this.starID = starID;
		this.ra = ra;
		this.dec = dec;
		this.spectral[0] = spectral[0];
		this.spectral[1] = spectral[1];
		this.magnitude = magnitude;
	}
	
	public int getStarID(){
		return starID;
	}
	
	public double getRa(){
		return ra;
	}
	
	public double getDec(){
		return dec;
	}
	
	public byte[] getSpectral(){
		return spectral;
	}
	
	public byte getSpec(){
		return spectral[0];
	}
	
	public short getMagnitude(){
		return magnitude;
	}
}