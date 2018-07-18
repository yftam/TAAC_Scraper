
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
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
import org.jsoup.Connection.Response;
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
	private DatabaseConnection dbConn;
	private ScrapeUtil util = new ScrapeUtil();

	private int maxItemToScrape = 9999;

	public void createFile(String fileName, String errorFileName) throws IOException{
		file = new File(fileName);
		file.delete();
		file.createNewFile();
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));
		errorpw = new PrintWriter(new FileOutputStream (new File(errorFileName),true));
	}
	
	public void setDbConn(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
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
	
	public void startCamelProductPage(String url) throws Exception {
		Camel camel = new Camel(url, loginCookies);
		try {
			camel.scrapeCamelPage();
			totalItemsScraped = Camel.getTotalItemsScraped();
			if(camel.getPrime() == "") {
				System.out.println("-- No Prime, skipping. --");
				return;
			}
		} catch (StringIndexOutOfBoundsException oob) {
			System.err.println("Out of bound");
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("========= Camelcamelcamel HTTP Page Error, "+new Date().toString()+" =========");
			System.err.println("========= BLOCKED BY CAMELCAMELCAMEL, EXITING =========");
			closeFile();
            System.exit(1);
		}
		
		Amazon amz = new Amazon(Settings.AMAZON_MARKETPLACE, camel);
		startAmazonProductPage(amz);
		util.delayBetween(500, 1000);	//setDelay
	}
	
	public void startAmazonProductPage(Amazon amz) throws Exception {
		long startTime, endTime;
		try {
			startTime = System.nanoTime();
			amz.scrapeAmazonPage();
			endTime = System.nanoTime();

//			"Category,Link,ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
			if(amz.isCamelScraped()) {
				printwrite.println(amz.getUrl()+","+amz.getMarketplace()+","+amz.getAsin()+","+amz.getScrapedDateTime()+","+amz.getProductTitle()+","+amz.getCamel().getPrime()+","+amz.getCamel().getPriceInfoAmazonRow()+","+amz.getCamel().getPriceInfo3rdPtyRow()+","+amz.getRating()+","+amz.getReviews()+","+amz.getAnsweredQ()+
						",$"+util.skipZero(amz.getPrice())+","+util.skipZero(amz.getSavingsDollar())+","+util.skipZero(amz.getSavingsPercentage())+","+amz.getCoupon()+","+amz.getPromo()+","+util.skipZero(amz.getLowestPrice())+","+amz.getLowestStatus()+","+util.skipZero(amz.getDollarWithinLowest())+","+util.skipZero(amz.getAveragePrice())+","+amz.getAverageStatus()+","+util.skipZero(amz.getDollarBelowAverage())+
						","+amz.getAvailability()+","+amz.getMerchant()+","+amz.getPrimeExclusive()+","+amz.getBestSellerCategory()+","+amz.getAmazonChoiceCategory()+","+amz.getAddon()+","+util.printRank(amz.getRankList()));
			} else {
				printwrite.println(amz.getUrl()+","+amz.getMarketplace()+","+amz.getAsin()+","+amz.getScrapedDateTime()+","+amz.getProductTitle()+","+amz.getRating()+","+amz.getReviews()+","+amz.getAnsweredQ()+
						","+util.skipZero(amz.getPrice())+","+util.skipZero(amz.getSavingsDollar())+","+util.skipZero(amz.getSavingsPercentage())+","+amz.getCoupon()+","+amz.getPromo()+
						","+amz.getAvailability()+","+amz.getMerchant()+","+amz.getPrimeExclusive()+","+amz.getBestSellerCategory()+","+amz.getAmazonChoiceCategory()+","+amz.getAddon()+","+util.printRank(amz.getRankList()));
			}
			System.out.println(" -> AMZ: "+amz.getAsin()+" | "+amz.getProductTitle());
			System.out.println(" -> AMZ: "+amz.getRating()+" Rating | "+amz.getReviews()+" Reviews | "+amz.getAnsweredQ()+" answered Q | $"+amz.getPrice()+" | $"+amz.getSavingsDollar()+" | "+amz.getSavingsPercentage()+"% | Coupon-"+amz.getCoupon()+amz.getPromo()+" | "+amz.getAvailability()+" | "+amz.getMerchant());			
			System.out.println(" -> AMZ: bestSellerCategory-"+amz.getBestSellerCategory()+ " | AmzChoiceCategory-"+amz.getAmazonChoiceCategory()+" | "+amz.getAddon()+ " | "+util.printRank(amz.getRankList()));
			if(amz.isCamelScraped()) System.out.println(" -> CAMEL: LowestPrice-"+amz.getLowestPrice()+"-%within"+amz.getLowestStatus()+"-$within$"+amz.getDollarWithinLowest()+" | BelowAverage-"+amz.getAveragePrice()+"-%below"+amz.getAverageStatus()+"-$below$"+amz.getDollarBelowAverage());
			
			if(Settings.DB_INSTANT_UPSERT || Settings.SCRAPE_MODE == Settings.SCRAPE_MODE_CAMEL_POPULAR_ITEMS || Settings.SCRAPE_MODE == Settings.SCRAPE_MODE_CAMEL_MANUAL_LIST) {
				dbConn.upsertAmazonProduct(amz);
			} else {
				System.out.println("DB_INSTANT_UPSERT OFF, UPSERT AT END");
			}
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
			Response response = Jsoup.connect(url)
					.ignoreContentType(true)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36")  
					.referrer("http://www.google.com")
					.timeout(12000)
					.followRedirects(true)
					.execute();
			Document amazonBestSellersPage = response.parse();
//			Document amazonBestSellersPage = Jsoup.connect(url).get();
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
					util.delayBetween(1000,2000);
				if(level < Settings.LEVELS_TO_GET_FOR_LIST_OF_BEST_SELLERS_CATEGORIES)
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
	
	public void startAmazonBestSellersTopProducts(String url, String category) throws Exception {
		System.out.println(category+" - "+url);
		Response response = Jsoup.connect(url)
				.ignoreContentType(true)
				.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36")  
				.referrer("http://www.google.com")
				.timeout(12000)
				.followRedirects(true)
				.execute();
		Document amazonBestSellersPage = response.parse();
//		Document amazonBestSellersPage = Jsoup.connect(url).get();
		String urlPrefix = "https://www.amazon.com";
		String itemURL;
		if(Settings.AMAZON_MARKETPLACE.equals("CA")) {
			urlPrefix = "https://www.amazon.ca";
		}
		int count = 1;
		Elements elements = amazonBestSellersPage.select("div#zg_centerListWrapper");
		for(Element element : elements.select("div.zg_itemImmersion")) {
			itemURL = urlPrefix + element.select("a.a-link-normal").attr("href");

			Amazon amz = new Amazon(Settings.AMAZON_MARKETPLACE, itemURL);
			amz.setCategory(category);
			System.out.println("#"+count+": "+amz.getScrapedDateTime()+" | "+Settings.AMAZON_MARKETPLACE+" | "+category+" | "+itemURL);
			startAmazonProductPage(amz);
			util.delayBetween(1000,2000);
			count++;
			if(count > Settings.TEST_MAX_ITEM_TO_SCRAPE) break;
		}
	}
	
	public void startCombiningAmazonBestSellersTopProductResults(String outputDir, String outputFolderName) throws Exception {
		File resultsFolder = new File(outputDir);
		FilenameFilter filter = new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	            return name.toLowerCase().endsWith(".csv");
	        }
	    };
	    File combinedResultFile = new File(resultsFolder+"/"+outputFolderName+"_COMBINED_RESULTS.csv");
	    combinedResultFile.delete();
	    combinedResultFile.createNewFile();
		printwrite = new PrintWriter(new FileOutputStream (combinedResultFile,true));
		printwrite.println("Category,Link,Marketplace,ASIN,Time,Product,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,"
				+ "Coupon,Promo,Stock,Merchant,PrimeExclusive,BestSeller,AmzChoice,IsAddOn,Rank");
		
		for(File file : resultsFolder.listFiles(filter)) {
			String category = file.getName().replaceAll("^[A-Z]{2}\\-|\\d+\\-|.csv", "");
			System.out.println(file.getName());
			Scanner sc = new Scanner(file);
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				if(line.startsWith("https")) {
					printwrite.println(category+","+line);
				}
			}
			sc.close();
		}
		printwrite.close();
	}
	
	public void upsertCombinedResults(String outputDir, String outputFolderName) throws FileNotFoundException, SQLException {
		File combinedResultFile = new File(outputDir+"/"+outputFolderName+"_COMBINED_RESULTS.csv");
		dbConn.setAllProductInactive(Settings.AMAZON_MARKETPLACE);
		Scanner sc = new Scanner(combinedResultFile);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(!line.startsWith("Category")) {
				Amazon amz = loadAmazonProductResult(line);
				dbConn.upsertAmazonProduct(amz);
			}
		}
		sc.close();
	}
	
	public Amazon loadAmazonProductResult(String amazonProductResult) {
		Amazon amz = new Amazon();
		String[] result = amazonProductResult.split(",");
		int splitCount = 0;
		amz.setCategory(result[splitCount]); splitCount++;
		amz.setUrl(result[splitCount]); splitCount++;
		amz.setMarketplace(result[splitCount]); splitCount++;
		amz.setAsin(result[splitCount]); splitCount++;
		amz.setScrapedDateTime(result[splitCount]); splitCount++;
		amz.setProductTitle(result[splitCount]); splitCount++;
		amz.setRating(Double.parseDouble(result[splitCount].replace("$",""))); splitCount++;
		amz.setReviews(Integer.parseInt(result[splitCount])); splitCount++;
		amz.setAnsweredQ(Integer.parseInt(result[splitCount])); splitCount++;
		amz.setPrice(result[splitCount].replace(" ", "").equals("") ? 0.0 : Double.parseDouble(result[splitCount].replace("$",""))); splitCount++;
		amz.setSavingsDollar(result[splitCount].replace(" ", "").equals("") ? 0.0 : Double.parseDouble(result[splitCount].replace("$",""))); splitCount++;
		amz.setSavingsPercentage(result[splitCount].replace(" ", "").equals("") ? 0 : Integer.parseInt(result[splitCount].replace("%",""))); splitCount++;
		amz.setCoupon(result[splitCount]); splitCount++;
		amz.setPromo(result[splitCount]); splitCount++;
		amz.setAvailability(result[splitCount]); splitCount++;
		amz.setMerchant(result[splitCount]); splitCount++;
		amz.setPrimeExclusive(result[splitCount]); splitCount++;
		amz.setBestSellerCategory(result[splitCount]); splitCount++;
		amz.setAmazonChoiceCategory(result[splitCount]); splitCount++;
		amz.setAddon(result[splitCount]); splitCount++;
		amz.setRank(result[splitCount]); splitCount++;
		return amz;
	}
	
	public void startAmazonTodaysDeals(String url) {
		
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

}// end class
