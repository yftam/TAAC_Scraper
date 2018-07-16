package scrapeInstance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrapeUtil {
	
	public String findMatch(String toFind, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = p.matcher(toFind);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return toFind;
		}
	}
	
	public double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public String skipZero(int toCheck) {
		if(toCheck == 0) {
			return " ";
		} else {
			return toCheck+"%";
		}
	}
	
	public String skipZero(double toCheck) {
		if(toCheck == 0.0) {
			return " ";
		} else {
			return "$"+toCheck;
		}
	}
	
	public String printRank(String[] rankArr) {
		String returnStr = "";
		for(int i = 0; i < rankArr.length; i++) {
			returnStr = returnStr + "#" + rankArr[i] + ",";
		}
		return returnStr;
	}
}
