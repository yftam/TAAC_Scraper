
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.sql.ResultSet;
//import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Scraper {
	private File file;
	private PrintWriter printwrite;
	private int scrapCounter;
	private Map<String, String> loginCookies;
	int catagoryItemsScraped = 0;

	public void createFile(String dirName, String fileName) throws IOException{
		file = new File(dirName + "/" +fileName);
		file.getParentFile().mkdirs();
		file.createNewFile();			
	}

	public void createFile(String fileName) throws IOException{
		file = new File(fileName);
		file.createNewFile();		
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));	
	}

	public void openFile(String fileName) throws IOException {
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));

	}

	public void closeFile() throws IOException {
		printwrite.close();
	}

	public void startCamelPopularProductsURL(String url, String category, Map<String, String> cookies) throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		Date date = new Date();
		String ts = dateFormat.format(date).replace(" ", "-").replaceAll("\\/|\\:", "");
		createFile(ts+"-"+category+".csv");
		
		loginCookies = cookies;

		final Document document = Jsoup.connect(url+category).cookies(loginCookies).get();
		String categoryName = document.select("option[selected=\"selected\"]").text().replaceFirst("\\s(-|\\+)*\\d{2}:\\d{2}.{1,}", "");	//remove useless timezone string
		System.out.println("***** CATEGORY "+categoryName+" *****");
		printwrite.println("ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,IsLowest,$Within,AveragePrice,%Below,$Below,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank");
		
		Elements elements = document.select("table.product_grid");

		for(Element tr : elements.select("tr")) {	//looping for each row
			if(tr.attr("id").contains("upper")) {
				for(Element td : tr.select("td")) {	//looping for each product(colomn) in a row
					String productPageUrl = td.select("a").attr("href");// camel product page url
					catagoryItemsScraped++;
					startCamelProductPage(productPageUrl);
					
//					Thread.sleep(500); // check for page load complete then continue to Level 2
				}
			}
		}
		System.out.println("Scraped "+catagoryItemsScraped+" items for "+categoryName+" category.");
		printwrite.close();
		if(document.select("div.pagination").select("a").text().contains("Next")) {
			System.out.println("HAS NEXT PAGE");
		}
	}// end startCamelPopularProductsURL
	
	public void startCamelProductPage(String url) throws Exception{
		final Document document = Jsoup.connect(url).cookies(loginCookies).get();		// feed URL to start scrape
		String productString = document.select("h2#tracks").text();
		String productTitle = productString.substring(0,productString.length()-13).replace("Create Amazon price watches for: ", "").replace(",", "");
		String asin = productString.substring(productString.length()-11,productString.length()-1);
		Elements priceInfoRow = document.select("div#header_tracker").select("tbody");
		String priceInfoAmazonRow = priceInfoRow.select("tr:nth-child(1)").select(":nth-child(9)").text();
		String priceInfo3rdPtyRow = priceInfoRow.select("tr:nth-child(3)").select(":nth-child(9)").text().replace("+", "plus ");
		
		String prime = "";
		String lowestPriceAmazonStr = document.select("div#section_amazon").select("tr.lowest_price").select("td:contains($)").text().replace("$", "");
		Double lowestPriceAmazon = lowestPriceAmazonStr.equals("") ? 0 : Double.parseDouble(lowestPriceAmazonStr);
		String averagePriceAmazonStr = document.select("div#section_amazon").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replace("$", "");
		Double averagePriceAmazon = averagePriceAmazonStr.equals("") ? 0 : Double.parseDouble(averagePriceAmazonStr);
		String lowestPrice3rdPtyStr = document.select("div#section_new").select("tr.lowest_price").select("td:contains($)").text().replace("$", "");
		Double lowestPrice3rdPty = lowestPrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(lowestPrice3rdPtyStr);
		String averagePrice3rdPtyStr = document.select("div#section_new").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replace("$", "");
		Double averagePrice3rdPty = averagePrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(averagePrice3rdPtyStr);
		
		if(priceInfoAmazonRow.contains("Prime") && priceInfo3rdPtyRow.contains("Prime")) {
			prime = "Both";
		} else if(priceInfoAmazonRow.contains("Prime")) {
			prime = "Amazon";
		} else if(priceInfo3rdPtyRow.contains("Prime")) {
			prime = "3rdPty";
		}
		priceInfoAmazonRow.replace("rime","");
		priceInfo3rdPtyRow.replace("rime","");
		System.out.println("#"+catagoryItemsScraped+" CAMEL: "+asin+" | "+productTitle+" | Prime-"+prime+" | Amazon-"+priceInfoAmazonRow+"+lowest $"+lowestPriceAmazon+"+avg $"+averagePriceAmazon+" | 3rdPartyNew-"+priceInfo3rdPtyRow+"+lowest $"+lowestPrice3rdPty+"+avg $"+averagePrice3rdPty);
		if(prime == "") {
			System.out.println("-- No Prime, skipping. --");
			return;
		}
		
		long startTime, endTime;
		startTime = System.nanoTime();
		
		try {
			Document amazonPage = Jsoup.connect("https://www.amazon.com/gp/product/"+asin).get();
	
			//constant fields
			String rating = amazonPage.select("div#averageCustomerReviews").select("span#acrPopover").attr("title").replace(" out of 5 stars", "").replace(",", "");
			String reviews = amazonPage.select("span[data-hook=\"total-review-count\"]").text().replace(",", "");
			String answeredQ = amazonPage.select("a#askATFLink > span").text().replace(" answered questions", "").replace(",", "");
			String priceStr = amazonPage.select("span#priceblock_ourprice, span#priceblock_dealprice").text().replace("$", "");
			Double price = priceStr.equals("") ? 0 : Double.parseDouble(priceStr);
			String savings = amazonPage.select("tr#regularprice_savings > td.a-span12, tr#dealprice_savings > td.a-span12").text();
			Double savingsDollar = 0.0, savingsPercentage = 0.0;
			if (savings.contains(" ")) {
				String[] savingsSplit = savings.split(" ");
				savingsDollar = Double.parseDouble(savingsSplit[0].replace("$", ""));
				savingsPercentage = Double.parseDouble(savingsSplit[1].replaceAll("\\(|\\)|\\%", ""));
			}
			String availability = amazonPage.select("div#availability, span#availability").text().replace(",", "");
			String merchantInfo = amazonPage.select("div#merchant-info, span#merchant-info").text();
			String merchant = merchantInfo.contains("sold by Amazon") ? "Amazon" : merchantInfo.contains("Fulfilled by") ? "FBA" : "No Buybox";
			String rank = amazonPage.select("tr:contains(Best Sellers Rank)").text().replace(",", "");
			
			//special fields
			String bestSellerCategory = amazonPage.select("div#centerCol").select("span#cat-name").text();
			String amazonChoiceCategory = amazonPage.select("span.ac-keyword-link").text();
			String coupon = amazonPage.select("div#unclippedCoupon, div#clippedCoupon").text().replaceAll("([=-z]|\\s|[ -#]|[&--]|\\/|\\.{2,}){1,}\\.{0,1}", "");
			String addon = amazonPage.select("div.a-box-group").select("span.a-text-bold").text().contains("Add-on") ? "Yes" : "No";
			
			//calculate % within lowest/average price tracked on camelcamelcamel
			String lowestStatus = "N/A", averageStatus = "N/A";
			Double lowestPrice = 0.0, averagePrice = 0.0, dollarWithinLowest = 0.0, dollarBelowAverage = 0.0;
			int withinLowestPercentage = 0, belowAveragePercentage = 0;
			if(merchant == "Amazon") {
				lowestPrice = lowestPriceAmazon;
				averagePrice = averagePriceAmazon;
			} else if(merchant == "FBA") {
				lowestPrice = lowestPrice3rdPty;
				averagePrice = averagePrice3rdPty;
			}
			if(price > 0) {
				if(price <= lowestPrice) {
					lowestStatus = "Yes";
				} else {
					dollarWithinLowest = round(price-lowestPrice, 2);
					withinLowestPercentage = (int) (dollarWithinLowest/lowestPrice*100);
					if(withinLowestPercentage <= 10) {	//within 10% of lowestPrice
						lowestStatus = "W/in "+withinLowestPercentage+"%";
					} else {
						lowestStatus = "No - "+withinLowestPercentage+"%";
						dollarWithinLowest = 0.0;
					}
				}
				if(price > 0 && price <= averagePrice) {
					dollarBelowAverage = round(averagePrice-price, 2);
					belowAveragePercentage = (int) (dollarBelowAverage/averagePrice*100);
					averageStatus = belowAveragePercentage+"% Below";
				} else {
					averageStatus = "No";
				}
			}
			
			endTime = System.nanoTime();

//			"ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
			printwrite.println(asin+","+productTitle+","+prime+","+priceInfoAmazonRow+","+priceInfo3rdPtyRow+","+rating+","+reviews+","+answeredQ+",$"+price+",$"+savingsDollar+","+savingsPercentage+"%,"+coupon+
					","+lowestPrice+","+lowestStatus+",$"+dollarWithinLowest+","+averagePrice+","+averageStatus+",$"+dollarBelowAverage+","+availability+","+merchant+","+bestSellerCategory+","+amazonChoiceCategory+","+addon+","+rank);
			System.out.println(" -> AMZ: "+rating+" Rating | "+reviews+" Reviews | "+answeredQ+" answered Q | $"+price+" | $"+savingsDollar+" | "+savingsPercentage+"% | Coupon-"+coupon+" | LowestPrice-"+lowestPrice+"-%within"+lowestStatus+"-$within$"+dollarWithinLowest+" | BelowAverage-"+averagePrice+"-%below"+averageStatus+"-$below$"+dollarBelowAverage+" | "+availability+" | "+merchant);
			System.out.println(" -> AMZ: bestSellerCategory-"+bestSellerCategory+ " | AmzChoiceCategory-"+amazonChoiceCategory+" | "+addon+ " | "+rank);
			System.out.println(" -> Time used to scrape AMZ page: "+(endTime-startTime)*0.000000001);
		} catch (Exception ex) {
            System.out.println(ex.getMessage() + "\nAn exception occurred. Most likely 404");
        }
	}
	
	public static double round(double value, int places) {
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
/*
	public void checkExistingURLs (String itemURL, DBConn dbConn) throws Exception {
		String query = "SELECT * from dbo.FT_Product where item_url = "+itemURL;  // query input and return ArrayList of String results
		try {
			ResultSet rs = dbConn.selectFromDB(query);
			if (rs == null) {
				// item not in DB -> new item
				startScrapeLvl2_FstTech_insert(itemURL,dbConn);
			}else {
				// item in DB

			}
		}catch(EOFException e) {
			
		}
	}
*/

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
