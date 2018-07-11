
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import scrapeInstance.*;
import settings.Settings;


public class Scraper {
	private File file;
	private PrintWriter printwrite, errorpw;
	private Map<String, String> loginCookies;
	int totalItemsScraped = 0, totalItemsScrapedWithInfo = 0;
	private Connect2DB dbConn;

	private int maxItemToScrape = 9999;

	public void createFile(String fileName, String errorFileName) throws IOException{
		file = new File(fileName);
		file.createNewFile();
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));
		errorpw = new PrintWriter(new FileOutputStream (new File(errorFileName),true));
	}
	
	public void connectDB() {
	    System.out.println("CONNECTION TO DATABASE STARTED ... ");
        dbConn = new Connect2DB();
        dbConn.newConnection();
        System.out.println("CONNECTION TO DATABASE SUCESSFUL ... ");
	}
	
	public void filePrintln(String print) {
		printwrite.println(print);
	}

	public void closeFile() throws IOException {
		printwrite.close();
	}
	
	public void setLoginCookies(Map<String, String> cookies) {
		loginCookies = cookies;
	}

	public void startCamelPopularProductsURL(String url, String categoryUrl, Map<String, String> cookies) throws Exception {
		loginCookies = cookies;
        
		final Document document = Jsoup.connect(url+categoryUrl).cookies(loginCookies).get();
		String categoryName = document.select("option[selected=\"selected\"]").text().replaceFirst("\\s(-|\\+)*\\d{2}:\\d{2}.{1,}", "");	//remove useless timezone string
		System.out.println("***** CATEGORY "+categoryName+" *****");
		
		Elements elements = document.select("table.product_grid");

		for(Element tr : elements.select("tr")) {	//looping for each row
			if(tr.attr("id").contains("upper")) {
				for(Element td : tr.select("td")) {	//looping for each product(colomn) in a row
					String productPageUrl = td.select("a").attr("href");// camel product page url
					startCamelProductPage(productPageUrl);
//					break;
				}
			}
			if(totalItemsScraped >= maxItemToScrape) {
				System.out.println("Reached max items to scrape, breaking.");
				break;
			}
		}
		
//		System.out.println("Scraped "+totalItemsScraped+" items for "+categoryName+" category.");
		System.out.println("Scraped "+totalItemsScraped+" items.");
		System.out.println("Scraped successfully with info: "+totalItemsScrapedWithInfo+" items.");
		Elements checkNextPage = document.select("div.pagination").select("a:contains(Next)");
		if(checkNextPage.text().contains("Next") && totalItemsScraped < maxItemToScrape) {
			String categoryUrlNextPage = checkNextPage.attr("href");
			System.out.println("HAS NEXT PAGE, link: "+categoryUrlNextPage);
			startCamelPopularProductsURL(url, categoryUrlNextPage, cookies);
		}
	}// end startCamelPopularProductsURL
	
	public void delayBetween(int low, int high) throws InterruptedException {
		Random r = new Random();
		int delay = r.nextInt(high-low) + low;
		System.out.println("Delaying "+delay+ "ms.");
		Thread.sleep(delay);
	}
	
	public void startCamelProductPage(String url) throws Exception {
		delayBetween(500, 1000);	//setDelay

		Camel camel = new Camel(url, loginCookies);
		try {
			camel.scrapeCamelPage();
			totalItemsScraped = Camel.getTotalItemsScraped();
			if(camel.getPrime() == "") {
				System.out.println("-- No Prime, skipping. --");
				return;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("========= Camelcamelcamel HTTP Page Error, "+new Date().toString()+" =========");
			System.err.println("========= BLOCKED BY CAMELCAMELCAMEL, EXITING =========");
			closeFile();
            System.exit(1);
		}
		
		long startTime, endTime;
		Amazon amz = new Amazon("US", camel);
		
		try {
			startTime = System.nanoTime();
			amz.scrapeAmazonPage();
			endTime = System.nanoTime();

			addProductToDB(amz);	// add2DB

//			"ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
			printwrite.println(amz.getUrl()+","+amz.getAsin()+","+new Date().toString()+","+amz.getProductTitle()+","+camel.getPrime()+","+camel.getPriceInfoAmazonRow()+","+camel.getPriceInfo3rdPtyRow()+","+amz.getRating()+","+amz.getReviews()+","+amz.getAnsweredQ()+
					",$"+amz.getPrice()+","+skipZero(amz.getSavingsDollar())+","+skipZero(amz.getSavingsPercentage())+","+amz.getCoupon()+","+amz.getPromo()+","+skipZero(amz.getLowestPrice())+","+amz.getLowestStatus()+","+skipZero(amz.getDollarWithinLowest())+","+skipZero(amz.getAveragePrice())+","+amz.getAverageStatus()+","+skipZero(amz.getDollarBelowAverage())+
					","+amz.getAvailability()+","+amz.getMerchant()+","+amz.getPrimeExclusive()+","+amz.getBestSellerCategory()+","+amz.getAmazonChoiceCategory()+","+amz.getAddon()+","+printRank(amz.getRankList()));
		
			System.out.println(" -> AMZ: "+amz.getRating()+" Rating | "+amz.getReviews()+" Reviews | "+amz.getAnsweredQ()+" answered Q | $"+amz.getPrice()+" | $"+amz.getSavingsDollar()+" | "+amz.getSavingsPercentage()+"% | Coupon-"+amz.getCoupon()+amz.getPromo()+" | LowestPrice-"+amz.getLowestPrice()+"-%within"+amz.getLowestStatus()+"-$within$"+amz.getDollarWithinLowest()+" | BelowAverage-"+amz.getAveragePrice()+"-%below"+amz.getAverageStatus()+"-$below$"+amz.getDollarBelowAverage()+" | "+amz.getAvailability()+" | "+amz.getMerchant());
			System.out.println(" -> AMZ: bestSellerCategory-"+amz.getBestSellerCategory()+ " | AmzChoiceCategory-"+amz.getAmazonChoiceCategory()+" | "+amz.getAddon()+ " | "+printRank(amz.getRankList()));
			System.out.println(" -> Time used to scrape AMZ page: "+(endTime-startTime)*0.000000001);
			totalItemsScrapedWithInfo = Amazon.getTotalItemsScrapedWithInfo();
		} catch (Exception e) {
            System.out.println(e.getMessage() + "\nAn exception occurred.");
            e.printStackTrace(System.err);
            errorpw.println(new Date());
            errorpw.println(amz.getUrl());
        	e.printStackTrace(errorpw);
        	errorpw.close();
        	
        	String eToString = e.toString();
        	if(eToString.contains("Status=503")) {
    			System.err.println("========= Amazon HTTP Error statis 503, "+new Date().toString()+" =========");
				System.err.println("========= BLOCKED BY AMAZON, EXITING =========");
    			closeFile();
                System.exit(1);
        	}
        }
	}
	
	public void startAmazonBestSellersList(String url, int level) throws Exception {
		try {
			Document amazonBestSellersPage = Jsoup.connect(url).get();
			Element list;
			if(level == 1) {
				list = amazonBestSellersPage.select("ul#zg_browseRoot > ul").first();
			} else {
				list = amazonBestSellersPage.select("ul:has(span.zg_selected)").last().select("ul:has(a)").last();
			}

			String check = list.select("span.zg_selected").text();
			String spaces = "", commas = "";
			for(int i = 1; i < level; i++) {
				spaces = spaces + " ";
				commas = commas + ",";
			}
			if(check.equals("")) {
				for(Element li : list.select("li")) {	//looping for each row
					String category = li.text().replace(",", "");
					String categoryUrl = li.select("a").attr("href");
					System.out.println(spaces+"Level: "+level+" Category: "+category+" - URL: "+categoryUrl);
					printwrite.println(level+commas+","+category+","+categoryUrl);
//				closeFile();
					delayBetween(1000,2000);
				if(level < 3)
						startAmazonBestSellersList(categoryUrl, level+1);
				}
			} else {
				System.out.println(spaces+"    NO MORE SUB CATEGORY UNDER "+check);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            errorpw.println(new Date());
            errorpw.println(url);
        	e.printStackTrace(errorpw);
        	errorpw.close();
		}
	}
	
	public void startAmazonBestSellersTopProducts(String url) throws Exception {
		System.out.println(url);
		Document amazonBestSellersPage = Jsoup.connect(url).get();
		String urlPrefix = "https://www.amazon.com";
		String itemURL;
		if(Settings.AMAZON_MARKETPLACE.equals("CA")) {
			urlPrefix = "https://www.amazon.ca";
		}
		int count = 1;
		Elements elements = amazonBestSellersPage.select("div#zg_centerListWrapper");		
		for(Element element : elements.select("div.zg_itemImmersion")) {
			itemURL = urlPrefix + element.select("a.a-link-normal").attr("href");
			System.out.println("#"+count+": "+itemURL);
			
			long startTime, endTime;
			Amazon amz = new Amazon("US", itemURL);
			
			try {
				startTime = System.nanoTime();
				amz.scrapeAmazonPage();
				addProductToDB(amz);
				endTime = System.nanoTime();

//					"ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
				printwrite.println(amz.getUrl()+","+amz.getAsin()+","+new Date().toString()+","+amz.getProductTitle()+","+amz.getRating()+","+amz.getReviews()+","+amz.getAnsweredQ()+
						","+amz.getPrice()+","+skipZero(amz.getSavingsDollar())+","+skipZero(amz.getSavingsPercentage())+","+amz.getCoupon()+","+amz.getPromo()+
//							","+skipZero(amz.getLowestPrice())+","+amz.getLowestStatus()+","+skipZero(amz.getDollarWithinLowest())+","+skipZero(amz.getAveragePrice())+","+amz.getAverageStatus()+","+skipZero(amz.getDollarBelowAverage())+
						","+amz.getAvailability()+","+amz.getMerchant()+","+amz.getPrimeExclusive()+","+amz.getBestSellerCategory()+","+amz.getAmazonChoiceCategory()+","+amz.getAddon()+","+printRank(amz.getRankList()));
				System.out.println(" -> AMZ: "+amz.getAsin()+" | "+amz.getProductTitle());
				System.out.println(" -> AMZ: "+amz.getRating()+" Rating | "+amz.getReviews()+" Reviews | "+amz.getAnsweredQ()+" answered Q | $"+amz.getPrice()+" | $"+amz.getSavingsDollar()+" | "+amz.getSavingsPercentage()+"% | Coupon-"+amz.getCoupon()+amz.getPromo()
//					+" | LowestPrice-"+amz.getLowestPrice()+"-%within"+amz.getLowestStatus()+"-$within$"+amz.getDollarWithinLowest()+" | BelowAverage-"+amz.getAveragePrice()+"-%below"+amz.getAverageStatus()+"-$below$"+amz.getDollarBelowAverage()
				+" | "+amz.getAvailability()+" | "+amz.getMerchant());
				System.out.println(" -> AMZ: bestSellerCategory-"+amz.getBestSellerCategory()+ " | AmzChoiceCategory-"+amz.getAmazonChoiceCategory()+" | "+amz.getAddon()+ " | "+printRank(amz.getRankList()));
				System.out.println(" -> Time used to scrape AMZ page: "+(endTime-startTime)*0.000000001);
				totalItemsScrapedWithInfo = Amazon.getTotalItemsScrapedWithInfo();
			} catch (Exception e) {
	            System.out.println(e.getMessage() + "\nAn exception occurred.");
	            e.printStackTrace(System.err);
	            errorpw.println(new Date());
	            errorpw.println(amz.getUrl());
	        	e.printStackTrace(errorpw);
	        	errorpw.close();
	        	
	        	String eToString = e.toString();
	        	if(eToString.contains("Status=503")) {
	    			System.err.println("========= Amazon HTTP Error statis 503, "+new Date().toString()+" =========");
					System.err.println("========= BLOCKED BY AMAZON, EXITING =========");
	    			closeFile();
	                System.exit(1);
	        	}
	        }
			delayBetween(1000,2000);
			count++;
		}
	}
	
	public void startCombiningAmazonBestSellersTopProductResults(String resultsDir) throws Exception {
		File resultsFolder = new File(resultsDir);
		FilenameFilter filter = new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	            return name.toLowerCase().endsWith(".csv");
	        }
	    };
	    File combinedResultFile = new File(resultsFolder+"/COMBINED_RESULTS.csv");
	    combinedResultFile.delete();
	    combinedResultFile.createNewFile();
		printwrite = new PrintWriter(new FileOutputStream (combinedResultFile,true));
		printwrite.println("Category,Link,ASIN,Time,Product,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,Promo,"
//				+ "LowestPrice,IsLowest,$Within,AveragePrice,%Below,$Below,"
				+ "Stock,Merchant,PrimeExclusive,BestSeller,AmzChoice,IsAddOn,Rank");
		
		for(File file : resultsFolder.listFiles(filter)) {
			String category = file.getName().replaceAll("\\d+\\-|.csv", "");
			System.out.println(file.getName());
			Scanner sc = new Scanner(file);
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				if(line.startsWith("https")) {
					printwrite.println(category+","+line);
				}
			}
		}
		printwrite.close();
	}
	
	public void startAmazonTodaysDeals(String url) {
		
	}
	
	private String skipZero(int toCheck) {
		if(toCheck == 0) {
			return " ";
		} else {
			return toCheck+"";
		}
	}
	
	private String skipZero(double toCheck) {
		if(toCheck == 0.0) {
			return " ";
		} else {
			return ""+toCheck;
		}
	}
	
	private String printRank(String[] rankArr) {
		String returnStr = "";
		for(int i = 0; i < rankArr.length; i++) {
			returnStr = returnStr + "#" + rankArr[i] + ",";
		}
		return returnStr;
	}
	
	public double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public void categoriesListToDB(File file) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			int level = Integer.parseInt(line.substring(0,1));
			String url = line.replaceAll(".*https", "https").replaceAll("\\,*$", "");
			String category;
			if(level == 1) {
				category = line.replaceAll("1,|,https.+", "").replaceAll("(\\s\\&\\s)|\\s", "-");
			} else {
				category = url.replaceAll(".*Best-Sellers-|\\/zgbs.*", "");
			}
			System.out.println(line);
			dbConn.queryInsert("INSERT INTO AmazonBestSellersCategories ([Marketplace],[CategoryLevel],[CategoryName],[CategoryUrl],[Enabled])"
					+ "VALUES ('"+Settings.AMAZON_MARKETPLACE+"',"+level+",'"+category+"','"+url+"',true)");
		}
		sc.close();
	}

	public void addProductToDB(Amazon amz) throws SQLException {
		
        if ( 
		!dbConn.querySelect("SELECT  "
                + "ASIN "
                + "FROM CamelResults "
                + "WHERE "
                + "ASIN = " + "'"+ amz.getAsin() + "'" ).isBeforeFirst()) {

		dbConn.queryInsert("INSERT INTO CamelResults ("
                + "[Link], "
                + "[ASIN], "
                + "[Date_java], "
                + "[Product], "
                + "[Prime], "
                + "[AmazonSt], "
                + "[3rdPtySt], "
                + "[Rating], "
                + "[Reviews], "
                + "[AnsweredQ], "
                + "[PriceNow], "
                + "[Save], "
                + "[Save%], "
                + "[Coupon], "
                + "[Promo], "
                + "[LowestPrice], "
                + "[IsLowest], "
                + "[$Within], "
                + "[AveragePrice], "
                + "[%BelowAverageStatus], "
                + "[$Below], "
                + "[Stock], "
                + "[Merchant], "
                + "[PrimeExclusive], "
                + "[BestSeller], "
                + "[AmzChoice], "
                + "[IsAddOn], "
                + "[Rank] )"
                + "VALUES ("
                + "'" +amz.getUrl() + "',"
                + "'" +amz.getAsin() + "',"
                + "'" +new Date().toString() + "',"
                + "'" +amz.getProductTitle().replace("'", "''") +"',"
                + "'" +amz.getCamel().getPrime() + "',"
                + "'" +amz.getCamel().getPriceInfoAmazonRow() + "',"
                + "'" +amz.getCamel().getPriceInfo3rdPtyRow()+ "',"
                + "'" +amz.getRating() + "',"
                + "'" +amz.getReviews() + "',"
                + "'" +amz.getAnsweredQ()+ "',"
                + amz.getPrice()+ ","
                + amz.getSavingsDollar()+ ","
                + "'" +amz.getSavingsPercentage()+ "',"
                + "'" +amz.getCoupon()+ "',"
                + "'" +amz.getPromo() + "',"
                + amz.getLowestPrice() + ","
                + "'" +amz.getLowestStatus()+ "',"
                + amz.getDollarWithinLowest()+ ","
                + amz.getAveragePrice() + ","
                + "'" + amz.getAverageStatus()+ "',"
                + amz.getDollarBelowAverage() + ","
                + "'" +amz.getAvailability().replace("'", "''") + "',"
                + "'" +amz.getMerchant() + "',"
                + "'" +amz.getPrimeExclusive() + "',"
                + "'" +amz.getBestSellerCategory().replace("'", "''")+ "',"
                + "'" +amz.getAmazonChoiceCategory().replace("'", "''")+ "',"         
                + "'" +amz.getAddon()+ "',"
                + "'" +printRank(amz.getRankList()).replace("'", "''") + "')");
		
		
        } else {
        	
        	System.out.println ("ITEM EXISTS ... SKIP INSERT...");
//        	
//			dbConn.queryInsert("UPDATE CamelResults ("
//					+ "SET "
//                    + "[Link] = " + amz.getUrl() + ", "
//                    + "[Date_java] = " + new Date().toString() + ", "
//                    + "[Product] =  " + amz.getProductTitle() + ", "
//                    + "[Prime] =  "  + camel.getPrime() + ", "
//                    + "[AmazonSt] =  " + camel.getPriceInfoAmazonRow() + ", "
//                    + "[3rdPtySt]=  "  + camel.getPriceInfo3rdPtyRow() + ", "
//                    + "[Rating]=   "  + amz.getRating() + ", "
//                    + "[Reviews]=   " + amz.getReviews() + ", "
//                    + "[AnsweredQ]=   " + amz.getAnsweredQ() + ", "
//                    + "[PriceNow]=   "  + amz.getPrice() + ", "
//                    + "[Save]=   " + amz.getSavingsDollar() + ", "
//                    + "[Save%]=   " + amz.getSavingsPercentage() + ", "
//                    + "[Coupon]=   "  + amz.getCoupon() + ", "
//                    + "[Promo]=   "  + amz.getPromo() + ", "
//                    + "[LowestPrice]=   " + amz.getLowestPrice() + ", "
//                    + "[IsLowest]=   "  + amz.getLowestStatus() + ", "
//                    + "[$Within]=   " + amz.getDollarWithinLowest() + ", "
//                    + "[AveragePrice%]=   " + amz.getAveragePrice() + ", "
//                    + "[%BelowAverageStatus]=   "  + amz.getAverageStatus() + ", "
//                    + "[$Below]=   " + amz.getDollarBelowAverage() + ", "
//                    + "[Stock]=   " + amz.getAvailability() + ", "
//                    + "[Merchant]=   " + amz.getMerchant() + ", "
//                    + "[PrimeExclusive]=   " + amz.getPrimeExclusive() + ", "
//                    + "[BestSeller]=   " + amz.getBestSellerCategory() + ", "
//                    + "[AmzChoice]=   " + amz.getAmazonChoiceCategory() + ", "
//                    + "[IsAddOn]=   " + amz.getAddon() + ", "
//                    + "[Rank]=   )" + printRank(amz.getRankList()) + " "
//                    + "WHERE  "
//                    + "[ASIN] = " + amz.getAsin() );
//                    
//
        }
	}


}// end class
