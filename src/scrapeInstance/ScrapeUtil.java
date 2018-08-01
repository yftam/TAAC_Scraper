package scrapeInstance;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrapeUtil {
	
	public void delayBetween(int low, int high) throws InterruptedException {
		Random r = new Random();
		int delay = r.nextInt(high-low) + low;
		System.out.println("   => Delaying "+delay+ "ms.");
		Thread.sleep(delay);
	}
	
	public String findMatch(String toFind, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = p.matcher(toFind);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return toFind;
		}
	}
	
	public String buildStringForQuery(String originalString, String stringToAdd) {
		if(originalString.equals("")) {
			originalString = stringToAdd;
		} else {
			originalString = originalString+","+stringToAdd;
		}
		return originalString;
	}
	
	public String buildUnionStringForQuery(String originalString, String stringToAdd) {
		if(originalString.equals("")) {
			originalString = stringToAdd;
		} else {
			originalString = originalString+" UNION ALL "+stringToAdd;
		}
		return originalString;
	}
	
	public String convertStringArrayToListString(String[] stringArray) {
		String toReturn = "";
		for(int i = 0; i < stringArray.length; i++) {
			toReturn = toReturn + "'"+stringArray[i]+"'";
			if(i < stringArray.length-1) toReturn = toReturn + ",";
		}
		return toReturn;
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
			returnStr = returnStr + "#" + rankArr[i] + ";";
		}
		return returnStr;
	}
	
	public void errorEndCheck(Throwable e, PrintWriter errorpw, String toPrint) {
        errorpw.println(new Date());
        if(!toPrint.equals("")) errorpw.println(toPrint);
    	e.printStackTrace(errorpw);
    	errorpw.close();
		Scanner input = new Scanner(System.in);
		System.out.println("Enter anything to end instance.");
		String end = input.next();
		System.out.println("END instance");
	}
}
