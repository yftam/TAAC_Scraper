
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


//import java.util.logging.Logger;

public class Scraper {
	private File file;
	private PrintWriter printwrite, errorpw;
	private Map<String, String> loginCookies;
	int totalItemsScraped = 0, totalItemsScrapedWithInfo = 0;
	
	private int maxItemToScrape = 10000;

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
	
	public void startCamelProductPage(String url) throws Exception{
		Thread.sleep(1500);	//setDelay
		totalItemsScraped++;
		final Document document = Jsoup.connect(url).cookies(loginCookies).get();		// feed URL to start scrape
		String productString = document.select("h2#tracks").text();
		String productTitle = productString.substring(0,productString.length()-13).replace("Create Amazon price watches for: ", "").replace(",", "");
		String asin = productString.substring(productString.length()-11,productString.length()-1);
		Elements priceInfoRow = document.select("div#header_tracker").select("tbody");
		String priceInfoAmazonRow = priceInfoRow.select("tr:nth-child(1)").select(":nth-child(9)").text();
		String priceInfo3rdPtyRow = priceInfoRow.select("tr:nth-child(3)").select(":nth-child(9)").text().replace("+", "plus ");
		
		String prime = "";
		String lowestPriceAmazonStr = document.select("div#section_amazon").select("tr.lowest_price").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		Double lowestPriceAmazon = lowestPriceAmazonStr.equals("") ? 0 : Double.parseDouble(lowestPriceAmazonStr);
		String averagePriceAmazonStr = document.select("div#section_amazon").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		Double averagePriceAmazon = averagePriceAmazonStr.equals("") ? 0 : Double.parseDouble(averagePriceAmazonStr);
		String lowestPrice3rdPtyStr = document.select("div#section_new").select("tr.lowest_price").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		Double lowestPrice3rdPty = lowestPrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(lowestPrice3rdPtyStr);
		String averagePrice3rdPtyStr = document.select("div#section_new").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		Double averagePrice3rdPty = averagePrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(averagePrice3rdPtyStr);
		
		if(priceInfoAmazonRow.contains("Prime") && priceInfo3rdPtyRow.contains("Prime")) {
			prime = "Both";
		} else if(priceInfoAmazonRow.contains("Prime")) {
			prime = "Amazon";
		} else if(priceInfo3rdPtyRow.contains("Prime")) {
			prime = "3rdPty";
		}
		priceInfoAmazonRow = priceInfoAmazonRow.replace("Prime ","");
		priceInfo3rdPtyRow = priceInfo3rdPtyRow.replaceAll("Prime\\s{0,1}","");
		System.out.println("#"+totalItemsScraped+" CAMEL: "+asin+" | "+productTitle+" | Prime-"+prime+" | Amazon-"+priceInfoAmazonRow+"+lowest $"+lowestPriceAmazon+"+avg $"+averagePriceAmazon+" | 3rdPartyNew-"+priceInfo3rdPtyRow+"+lowest $"+lowestPrice3rdPty+"+avg $"+averagePrice3rdPty);
		if(prime == "") {
			System.out.println("-- No Prime, skipping. --");
			return;
		}
		
		long startTime, endTime;
		startTime = System.nanoTime();
		
		try {
			Document amazonPage = Jsoup.connect("https://www.amazon.com/gp/product/"+asin).get();
	
			//constant fields
			String rating = amazonPage.select("div#averageCustomerReviews").select("span#acrPopover").attr("title").replace(" out of 5 stars", "");
			String reviews = "";
			try { reviews = amazonPage.select("span#acrCustomerReviewText, span[data-hook=\"total-review-count\"]").first().text().replaceAll("[^\\d]", "");
			} catch (NullPointerException ne) { reviews = "0"; }
			String answeredQ = amazonPage.select("a#askATFLink > span").text().replaceAll("[^\\d]", "");
			String priceStr = amazonPage.select("span#priceblock_ourprice, span#priceblock_dealprice").text().replaceAll("\\$|\\,", "");
			if(priceStr.equals("")) {	//most likely in the case of scraping Books
				priceStr = amazonPage.select("span[class=\"a-size-medium a-color-price offer-price a-text-normal\"], span[class=\"a-size-medium a-color-price header-price\"]").text().replaceAll("\\$|\\,", "");
			}
			Double price = priceStr.equals("") ? 0 : Double.parseDouble(priceStr);
			String savings = amazonPage.select("tr#regularprice_savings > td.a-span12, tr#dealprice_savings > td.a-span12").text();
			if(savings.equals("")) {	//most likely in the case of scraping Books
//				savings = amazonPage.select("div#buyBoxInner span[class=\"a-size-base a-color-secondary\"]").text().replaceAll("^\\D{1,}\\$", "");
				savings = amazonPage.select("span[class=\"a-size-base a-color-secondary\"]:contains($)").text().replaceAll("^\\D{1,}\\$", "");
			}
			Double savingsDollar = 0.0;
			int savingsPercentage = 0;
			if (savings.contains(" ")) {
				String[] savingsSplit = savings.split(" ");
				savingsDollar = Double.parseDouble(savingsSplit[0].replaceAll("\\$|\\,", ""));
				savingsPercentage = Integer.parseInt(savingsSplit[1].replaceAll("\\(|\\)|\\%", ""));
			}
			String availability = amazonPage.select("div#availability, span#availability, span#pantry-availability").text().replace(",", "");
			String merchantInfo = amazonPage.select("div#merchant-info, span#merchant-info, div#pantry-availability-brief").text();
			String merchant = merchantInfo.contains("Amazon Pantry") || amazonPage.select("div#pantryStoreMessage_feature_div").text().contains("Prime Pantry") ? "PrimePantry" : merchantInfo.contains("sold by Amazon") ? "Amazon" : merchantInfo.contains("Fulfilled by") ? "FBA" : "No Prime Buybox";
			String primeExclusive = merchantInfo.contains("exclusively") || amazonPage.select("div#pantryPrimeExclusiveMessage_feature_div").text().toLowerCase().contains("exclusively") ? "Yes" : "No";
			
			String rank = amazonPage.select("li:contains(Amazon Best Sellers Rank)").text().replace(",", "");
			if(rank.equals("")) {
				rank = amazonPage.select("tr:contains(Best Sellers Rank)").text().replace(",", "");
			}
			String newRank = rank.replaceAll("(Amazon ){0,}Best Sellers Rank\\:{0,} \\#|\\s(\\(.{1,}\\))", "");
			String[] rankArr = newRank.split(" #");
			Collections.sort(Arrays.asList(rankArr), new Comparator<String>() {
			    public int compare(String o1, String o2) {
			        return extractInt(o1) - extractInt(o2);
			    }
			    int extractInt(String s) {
			    	String num = s.replaceAll("\\D", "");	// return 0 if no digits found
			        return num.isEmpty() ? 0 : Integer.parseInt(num);
			    }
			});
			
			//special fields
			String bestSellerCategory = amazonPage.select("div#centerCol").select("span#cat-name").text();
			String amazonChoiceCategory = amazonPage.select("span.ac-keyword-link").text();
			String promo = "";
			try { promo = amazonPage.select("div#unclippedCoupon, div#clippedCoupon, div#applicablePromotionList_feature_div, div#applicable_promotion_list_sec").first().text().replace(",", "");
			} catch (NullPointerException ne) { }
//			String coupon = promo.replaceAll("([=-z]|\\s|[ -#]|[&--]|\\/|\\.{2,}){1,}\\.{0,1}", "");
			String coupon = "";
			if(!promo.equals("")) {
				Pattern p = Pattern.compile("(\\$\\d+(\\.\\d+)?|\\d+\\%)");
				Matcher matcher = p.matcher(promo);
				if (matcher.find()) {
					coupon = matcher.group(1);
				}
			}
			String addon = amazonPage.select("div.a-box-group, div#addOnItemHeader").text().contains("Add-on") ? "Yes" : "No";
			
			//calculate % within lowest/average price tracked on camelcamelcamel
			String lowestStatus = "", averageStatus = "";	//if no buybox, no info shown
			Double lowestPrice = 0.0, averagePrice = 0.0, dollarWithinLowest = 0.0, dollarBelowAverage = 0.0;
			int withinLowestPercentage = 0, belowAveragePercentage = 0;
			if(merchant == "Amazon" || merchant == "PrimePantry") {
				lowestPrice = lowestPriceAmazon;
				averagePrice = averagePriceAmazon;
			} else if(merchant == "FBA") {
				lowestPrice = lowestPrice3rdPty;
				averagePrice = averagePrice3rdPty;
			}
			if((merchant == "Amazon" || merchant == "FBA") && price > 0) {
				if(price <= lowestPrice) {
					lowestStatus = "Yes";
				} else if(lowestPrice == 0.0) {
					lowestStatus = "N/A";
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
					averageStatus = "N/A";
				}
			}
			
			endTime = System.nanoTime();

//			"ASIN,Product,Prime,AmazonSt,3rdPtySt,Rating,Reviews,AnsweredQ,PriceNow,Save,Save%,Coupon,LowestPrice,$Within,$Within%,AveragePrice,$Below,$Below%,Stock,Seller,BestSeller,AmzChoice,IsAddOn,Rank"
			printwrite.println("https://www.amazon.com/gp/product/"+asin+","+asin+","+productTitle+","+prime+","+priceInfoAmazonRow+","+priceInfo3rdPtyRow+","+rating+","+reviews+","+answeredQ+
					",$"+price+","+skipZero(savingsDollar)+","+skipZero(savingsPercentage)+","+coupon+","+promo+","+skipZero(lowestPrice)+","+lowestStatus+","+skipZero(dollarWithinLowest)+","+skipZero(averagePrice)+","+averageStatus+","+skipZero(dollarBelowAverage)+
					","+availability+","+merchant+","+primeExclusive+","+bestSellerCategory+","+amazonChoiceCategory+","+addon+","+printRank(rankArr));
			System.out.println(" -> AMZ: "+rating+" Rating | "+reviews+" Reviews | "+answeredQ+" answered Q | $"+price+" | $"+savingsDollar+" | "+savingsPercentage+"% | Coupon-"+coupon+promo+" | LowestPrice-"+lowestPrice+"-%within"+lowestStatus+"-$within$"+dollarWithinLowest+" | BelowAverage-"+averagePrice+"-%below"+averageStatus+"-$below$"+dollarBelowAverage+" | "+availability+" | "+merchant);
			System.out.println(" -> AMZ: bestSellerCategory-"+bestSellerCategory+ " | AmzChoiceCategory-"+amazonChoiceCategory+" | "+addon+ " | "+printRank(rankArr));
			System.out.println(" -> Time used to scrape AMZ page: "+(endTime-startTime)*0.000000001);
			totalItemsScrapedWithInfo++;
		} catch (Exception ex) {
            System.out.println(ex.getMessage() + "\nAn exception occurred.");
            ex.printStackTrace(System.err);
            errorpw.println(new Date());
            errorpw.println("https://www.amazon.com/gp/product/"+asin);
        	ex.printStackTrace(errorpw);
        	errorpw.close();
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
