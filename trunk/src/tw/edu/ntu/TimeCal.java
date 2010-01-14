package tw.edu.ntu;

import android.text.format.Time;

public class TimeCal{
	private Time timeNow = new Time();
	//private long now;
	private double JD;
	private int gmtoffh;
	private double gmtoffd;
	private double h;
	private double UTh;
	private double GSTh;
	//private double HAh;
	private double LSTh;
	private static final double longi_d = 120.0;
	private static final double lat_d = 25.0;
	//private static final double pi = 3.1416;

	public double getJD(){
		return JD;
	}
	
	public double geth(){
		return h;
	}
	
	public double getUTh(){
		return UTh;
	}
	
	public double getGSTh(){
		return GSTh;
	}
	
	public double getLSTh(){
		return LSTh;
	}
	
	//public double cvRadHA2RadRA(double radHA){
	//	return 
	//}
	
	public static double cvH2Radians(double h){
		return (h / 24 * 2 * Math.PI);
	}
	
	public static double cvH2Degree(double h){
		return h * 15;
	}
	
	public double cvRAh2HAh(double RAh){
		return LSTh - RAh;
	}
	
	public double cvHAh2RAh(double HAh){
		return LSTh - HAh;
	}
	
	private void calJD(){
		int y = timeNow.year;
		int m = timeNow.month + 1;
		double d = (double) ((double)timeNow.monthDay + ((double)(timeNow.hour*3600 + timeNow.minute*60 + timeNow.second)/86400));
		int B = 0;
		if(y >= 1582){
			int A = (int) (y/100);
			B = (int) (2 - A + A/4);
		}
		if(m == 1 || m == 2){
			y--;
			m = m + 12;
		}
		int C = (int) (365.25 * y);
		int D = (int) (30.6001 *(m+1));
		JD = (double) (B + C + D + d + 1720994.5);
		JD = JD - gmtoffd;
	}
	
	private void calGSTh(){
		double S = JD - 2451545.0;
		//double S = 2444351.5 - 2451545.0;
		double T = S / 36525.0;
		double T0 = 6.697374558 + (2400.051336*T) + (0.000025862 * T * T);
		GSTh = fixTo24(UTh*1.002737909 + fixTo24(T0));
	}
	
	private void calLST(){
		LSTh = fixTo24(GSTh + (longi_d / 15.0));
	}
	
	public void setTimeToNow(){
		//now = System.currentTimeMillis();
		//timeNow.set(now);
		timeNow.setToNow();
		gmtoffd = (double)timeNow.gmtoff / 86400;
		gmtoffh = (int) (timeNow.gmtoff / 3600);
		//double aaa = (double)timeNow.gmtoff / 86400;
		h = timeNow.hour+((double)timeNow.minute+((double)timeNow.second/60))/60;
		UTh = fixTo24(h - gmtoffh);
		calJD();
		calGSTh();
		calLST();
		//HA = (h / 24.0) * pi;
	}
	
	private double fixTo24(double i){
		while(i > 24){
			i -= 24;
		}
		while(i < 0){
			i += 24;
		}
		return i;
	}
}