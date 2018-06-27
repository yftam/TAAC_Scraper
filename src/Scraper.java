
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import scrapeInstance.*;


public class Scraper {
	private File file;
	private PrintWriter printwrite, errorpw;
	private Map<String, String> loginCookies;
	int totalItemsScraped = 0, totalItemsScrapedWithInfo = 0;
	
	private int maxItemToScrape = 60;

	public void createFile(String fileName, String errorFileName) throws IOException{
		file = new File(fileName);
		file.createNewFile();
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));
		printwrite.println("Link,ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,Promo,LowestPrice,IsLowest,$Within,AveragePrice,%Below,$Below,Stock,Merchant,PrimeExclusive,BestSeller,AmzChoice,IsAddOn,Rank");
        errorpw = new PrintWriter(new FileOutputStream (new File(errorFileName),true));
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
	
	public int randomBetween(int low, int high) {
		Random r = new Random();
		int result = r.nextInt(high-low) + low;
		return result;
	}
	
	public void startCamelProductPage(String url) throws Exception{
		int delay = randomBetween(500, 1000);	//setDelay
		System.out.println("Delaying "+delay+ "ms.");
		Thread.sleep(delay);
//		totalItemsScraped++;
//		String productTitle = "", asin = "", priceInfoAmazonRow = "", priceInfo3rdPtyRow = "", prime = "";
//		Double lowestPriceAmazon = 0.0, averagePriceAmazon = 0.0, lowestPrice3rdPty = 0.0, averagePrice3rdPty = 0.0;
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

//			"ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
			printwrite.println(amz.getUrl()+","+amz.getAsin()+","+amz.getProductTitle()+","+camel.getPrime()+","+camel.getPriceInfoAmazonRow()+","+camel.getPriceInfo3rdPtyRow()+","+amz.getRating()+","+amz.getReviews()+","+amz.getAnsweredQ()+
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
	
	private String skipZero(int toCheck) {
		if(toCheck == 0) {
			return " ";
		} else {
			return toCheck+"%";
		}
	}
	
	private String skipZero(double toCheck) {
		if(toCheck == 0.0) {
			return " ";
		} else {
			return "$"+toCheck;
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

	public void startScrapeLvl1_Amz(String url) throws Exception {

		final Document document = Jsoup.connect(url).get();		
		Elements elements = document.select("div#zg_centerListWrapper");		
		for(Element element : elements.select("div.zg_itemImmersion")) {				
			for(Element subelement1 : element.select("div.zg_itemWrapper")) {

				//=== Item Picture URL ===
				String itemImgURL = subelement1.select("div.a-section img").attr("src");// imageURL

				//=== Item Name ===
				String itemNameRaw = subelement1.select("a.a-link-normal div").text();// item name
				String itemName = itemNameRaw.replace("'", "''"); // remove ' so wont be confused with query

				//=== Item ID ===
				String[] itemIdNameParse = (subelement1.select("div.a-section a.a-link-normal").attr("href")).split("/");// itemID Aand Name to be prased by '/'
				String itemID = itemIdNameParse[3]; // item ID

				//=== Item URL for more description ===
				String itemURL = "https://amazon.ca"+ subelement1.select("div.a-section a.a-link-normal").attr("href");

				//=== Item Rating ===
				String itemRating = subelement1.select("div.a-icon-row a").attr("title");// rating		

				//=== Price ===
				String[] itemPriceParse = (subelement1.select("span.p13n-sc-price").text()).split(" ");// price
				Double itemPrice;
				if (itemPriceParse.length <= 1) { // check if the Price is blank. i.e. item not unavailable				
					itemPrice= 999999999999.00;	// if item unavailable set set price
				}else {
					itemPrice = Double.parseDouble(itemPriceParse[1].toString());		
				}

				//=== Reviews ===
				String itemReviewTimes = subelement1.select("div.a-icon-row a.a-size-small").text();// Number of Reviews

				//=== Prime ===
				String itemPrime = subelement1.select("div.a-row i").attr("aria-label");// Prime?

				startScrapeLvl2_Amz(itemURL);

				//System.out.println(itemName+";"+itemRating+";"+itemPrice+";"+itemReviewTimes+";"+itemPrime+";"+itemID+";"+itemImgURL);

				//Set all scraped into database
				//dbConn.ModDB("INSERT INTO [dbo].[Amz_Product_Details]([asin],[manufacturer_id],[category_id],[name],[product_description],[product_url],[number_of_reviews],[star_rating],[current_regular_price],[current_sale_price],[percent_off],[historic_low_price],[historic_high_price],[in_stock],[stock_status],[free_1d],[free_2d],[free_2d_date],[sold_by],[is_active],[created_by],[created_tms],[updated_by],[updated_tms])VALUES('"+itemID+"',null,0,'"+itemName+"','','"+itemURL+"',0,0.0,0.0,"+itemPrice+",null,0,0,0,0,null,null,null,'',1,'','','','')");
			}
		}
	} // end startScrapeLvl1

	public void startScrapeLvl2_Amz(String url) throws Exception{

		final Document document = Jsoup.connect(url).get();		
		Elements elements = document.select("div#dp.home_improvement.en_CA");		
		for(Element element : elements.select("div#dp-container.a-container")) {	

			//customer review
			String itemName = element.select("div#title_feature_div").text();// imageURL
			System.out.println(itemName);
			//customer review
			String itemImgURL = element.select("div#averageCustomerReviews").text();// imageURL
			//System.out.println(itemImgURL);

			// product features
			String feature = element.select("div#featurebullets_feature_div").text();// imageURL
			//System.out.println(feature);

			//List Price / Current Price / Shipping / Saved %
			String price = element.select("div#price").text();// imageURL
			//System.out.println(price);


		}

	}// end start Scrape Lvl2

	public void startScrapeLvl2_FstTech_insert(String url) throws Exception{

		final Document document = Jsoup.connect(url).get();		// feed URL to start scrape

		// item Descriptions?
		String itemDescriptions = null;		
		for(Element elements : document.select("div#ProductDetails")) {
			itemDescriptions = elements.select("div.ProductDescriptions").text();
		}

		Elements elements = document.select("div.ProductBackdrop");		
		for(Element element : elements.select("div.ProductOptionsBox")) {	

			// item Price ?
			String itemPrice = element.select("span#content_Price").text();

			// item available ?
			String itemAvaliable = element.select("input#AddToCart").attr("value").toString();
			if(itemAvaliable.equals("")) {
				itemAvaliable = "NOT AVALIABLE";
			}else if(itemAvaliable.equals("ADD TO CART")) {
				itemAvaliable = "AVALIABLE";
			}

			//Shipping free?
			Boolean shippingFee_Bool = (element.select("div").text()).contains("ships free (worldwide)");
			String itemFreeShipping = null;
			if (shippingFee_Bool == true) {
				itemFreeShipping = "Free Shipping";				
			}else if(shippingFee_Bool == false) {
				itemFreeShipping = "Shipping Fee";
			}

			//item descriptions
			String itemDescrptions = element.select("div.ProductDescriptions").text();	


			System.out.println(itemPrice+" | "+itemAvaliable+" | "+ itemFreeShipping +" | "+itemDescriptions);

		}
	}// end start Scrape Lvl2
}// end class
