package com.lulan.shincolle.utility;

public class FormatHelper {
	
	public FormatHelper() {
	}
	
	//�Nsecond�ন�榡 00:00:00
	public static String getTimeFormated(int sec) {
		int timeSec = 0;
		int timeMin = 0;
		int timeHr = 0;
		
		timeSec = sec % 60;
		timeMin = (sec % 3600) / 60;
		timeHr = sec / 3600;
		
		return get2Digit(timeHr) + ":" + get2Digit(timeMin) + ":" + get2Digit(timeSec);
	}

	//�N�Ʀr�ର2���, �`�Nnum > 0
	public static String get2Digit(int num) {
	    if(num == 0) {
	        return "00";
	    }
	    
	    if(num < 10) {
	        return "0" + num;
	    }

	    return String.valueOf(num);
	}

}
