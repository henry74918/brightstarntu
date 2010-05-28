package tw.edu.ntu;



public class CoordCal{
	public static double lat_d = 25.0;
	
	/**
	 * Convert Hour-angel and Declination to Altitude.
	 * Will difference by local latitude.
	 * 
	 * @param ha_r 	- Hour-angel in radians.
	 * @param dec_r	- Declination in radians.
	 * @return 		- Altitude in radians.
	 */
	public static double cvHDtoAlt(double ha_r, double dec_r){
		double lat_r = Math.toRadians(lat_d);
		//System.out.println("long_r:" + lat_r);
		return Math.asin(Math.sin(dec_r)*Math.sin(lat_r) + Math.cos(dec_r)*Math.cos(lat_r)*Math.cos(ha_r));
	}
	
	/**
	 * Convert Hour-angle and Declination to Azimuth.
	 * Will difference by altitude and local latitude.
	 * 
	 * @param ha_r	- Hour-angle in radians.
	 * @param dec_r	- Declination in radians.
	 * @param a_r	- Altitude in radians.
	 * @return		- Azimuth in radians.
	 */
	public static double cvHDtoAzi(double ha_r, double dec_r, double a_r){
		double lat_r = Math.toRadians(lat_d);
		double cosAzimuth = (Math.sin(dec_r) - Math.sin(lat_r)*Math.sin(a_r)) / (Math.cos(lat_r)*Math.cos(a_r));
		if(cosAzimuth > 1.0f)
			cosAzimuth = 1.0f;
		else if(cosAzimuth < -1.0f)
			cosAzimuth = -1.0f;
		double azimuth = Math.acos(cosAzimuth);
		if(Math.sin(ha_r) >= 0)
			azimuth = 2 * Math.PI - azimuth;
		return azimuth;
	}
	
	/**
	 * Convert Altitude and Azimuth to Declination.
	 * Result will difference by local latitude.
	 * 
	 * @param alt_r	- Altitude in radians.
	 * @param azi_r	- Azimuth in radians.
	 * @return		- Declination in radians.
	 */
	public static double cvAAtoDec(double alt_r, double azi_r){
		double lat_r = Math.toRadians(lat_d);
		return Math.asin(Math.sin(alt_r)*Math.sin(lat_r) + Math.cos(alt_r)*Math.cos(lat_r)*Math.cos(azi_r));
	}
	
	/**
	 * Convert Altitude and Azimuth to Hour-angle.
	 * Result will difference by Azimuth and local latitude.
	 * 
	 * @param alt_r	- Altitude in radians.
	 * @param azi_r	- Azimuth in radians.
	 * @param dec_r	- Declination in radians.
	 * @return		- Hour-angle in radians.
	 */
	public static double cvAAtoHA(double alt_r, double azi_r, double dec_r){
		double lat_r = Math.toRadians(lat_d);
		double cosHa = (Math.sin(alt_r) - Math.sin(lat_r)*Math.sin(dec_r)) / (Math.cos(lat_r)*Math.cos(dec_r));
		//System.out.println("cosHa:"+cosHa);
		if(cosHa > 1.0f)
			cosHa = 1.0f;
		else if(cosHa < -1.0f)
			cosHa = -1.0f;
		double ha = Math.acos(cosHa);
		if(Math.sin(azi_r) >= 0)
			ha = 2*Math.PI - ha;
		return ha;
	}
	
	/**
	 * Convert Right Ascension and Declination to Altitude.
	 * Result will difference by local latitude and Local Sidereal Time.
	 * 
	 * @param RA_r	- Right Ascension in radians.
	 * @param Dec_r	- Declination in radians.
	 * @param LST_r	- Local Sidereal Time in radians.
	 * @return		- Altitude in radians.
	 */
	public static double cvRDtoAlt(double RA_r, double Dec_r, double LST_r){
		double HA_r = LST_r - RA_r;
		return cvHDtoAlt(HA_r, Dec_r);
	}
	
	/**
	 * Convert Right Ascension and Declination to Azimuth.
	 * Result will difference by local latitude, local sidereal time and altitude.
	 * 
	 * @param RA_r	- Right Ascension in radians.
	 * @param Dec_r	- Declination in radians.
	 * @param alt_r	- Altitude in radians.
	 * @param lst_r	- Local Sidereal Time in radians.
	 * @return		- Azimuth in radians.
	 */
	public static double cvRDtoAzi(double ra_r, double dec_r, double alt_r, double lst_r){
		double HA_r = lst_r - ra_r;
		return cvHDtoAzi(HA_r, dec_r, alt_r);
	}
	
	/**
	 * Convert Altitude and Azimuth to Right Ascension.
	 * Result will difference by local latitude, local sidereal time and declination.
	 * 
	 * @param alt_r	- Altitude in radians.
	 * @param azi_r	- Azimuth in radians.
	 * @param dec_r	- Declination in radians.
	 * @param lst_r	- Local Sidereal Time in radians.
	 * @return		- Right Ascension in radians.
	 */
	public static double cvAAtoRA(double alt_r, double azi_r, double dec_r, double lst_r){
		double ha_r = cvAAtoHA(alt_r, azi_r, dec_r);
		double ra_r = ha_r + lst_r;
		while(ra_r > 2*Math.PI)
			ra_r -= 2*Math.PI;
		return ra_r;
	}
	
	/**
	 * Convert Right Ascension and Declination to X coordinate in openGL
	 * 
	 * @param ra_r	- Right Ascension in radians.
	 * @param dec_r	- Declination in radians.
	 * @return		- X coordinate between (-1, 1)
	 */
	public static double cvRDtoX(double ra_r, double dec_r){
		return Math.cos(ra_r)*Math.cos(dec_r);
	}
	
	/**
	 * Convert Right Ascension and Declination to Y coordinate in openGL
	 * 
	 * @param ra_r	- Right Ascension in radians.
	 * @param dec_r	- Declination in radians.
	 * @return		- Y coordinate between (-1, 1)
	 */
	public static double cvRDtoY(double ra_r, double dec_r){
		return Math.sin(ra_r)*(float)Math.cos(dec_r);
	}
	
	/**
	 * Convert Right Ascension and Declination to Z coordinate in openGL
	 * 
	 * @param ra_r	- Right Ascension in radians.
	 * @param dec_r	- Declination in radians.
	 * @return		- Z coordinate between (-1, 1)
	 */
	public static double cvRDtoZ(double dec_r){
		return Math.sin(dec_r);
	}

	public static double cvWinXYtoAlt(double winX, double winY, double cenAlt,double fovy,double h, double w){
		double fovx = fovy*w/h;
		double x = winX*(2*Math.tan(fovx/2.0)/w);
		double y = winY*(2*Math.tan(fovy/2.0)/h);
		//x = x / 100.0;
		//y = y / 100.0;
		double p = Math.sqrt(Math.pow(x, 2.0)+Math.pow(y, 2.0));
		double c = Math.atan(p);
		double alt = Math.asin(Math.cos(c)*Math.sin(cenAlt) + (y*Math.sin(c)*Math.cos(cenAlt))/p);
		return alt;
	}
	
	public static double cvWinXYtoAzi(double winX, double winY, double cenAlt, double cenAzi, double fovy,double h, double w){
		double fovx = fovy*w/h;
		double x = winX*(2*Math.tan(fovx/2.0)/w);
		double y = winY*(2*Math.tan(fovy/2.0)/h);
		//x = x / 100.0;
		//y = y / 100.0;
		double p = Math.sqrt(Math.pow(x, 2.0)+Math.pow(y, 2.0));
		double c = Math.atan(p);
		double azi = cenAzi + Math.atan(x*Math.sin(c)/(p*Math.cos(cenAlt)*Math.cos(c) - y*Math.sin(cenAlt)*Math.sin(c)));
		return azi;
	}
	
	public static double cvAAtoWinX(double alt, double azi, double cenAlt, double cenAzi){
		double cosC = Math.sin(cenAlt)*Math.sin(alt) + Math.cos(cenAlt)*Math.cos(alt)*Math.cos(azi - cenAzi);
		double winX = Math.cos(alt)*Math.sin(azi-cenAzi) / cosC;
		return winX;
	}
	
	public static double cvAAtoWinY(double alt, double azi, double cenAlt, double cenAzi){
		double cosC = Math.sin(cenAlt)*Math.sin(alt) + Math.cos(cenAlt)*Math.cos(alt)*Math.cos(azi - cenAzi);
		double winY = (Math.cos(cenAlt)*Math.sin(alt) - Math.sin(cenAlt)*Math.cos(alt)*Math.cos(azi-cenAzi)) / cosC;
		return winY;
	}
}