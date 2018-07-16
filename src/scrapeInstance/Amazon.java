package scrapeInstance;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Amazon extends ScrapeUtil {

	private String marketplace, url, asin;
	private String scrapedDateTime;
	private String category = "";
	private Document amazonPage;
	private String productTitle = "";
	private Double rating = 0.0;
	private int reviews = 0, answeredQ = 0;
	private Double price = 0.0, savingsDollar = 0.0;
	private int savingsPercentage = 0;
	private String coupon = "", promo = "";
	private String availability = "", merchant = "";
	private String primeExclusive = "", addon = "";
	private String bestSellerCategory = "", amazonChoiceCategory = "";
	private String rank = "";
	private String[] rankList;
	
	private boolean camelScraped = false;
	private Camel camel = new Camel();
	String lowestStatus = "", averageStatus = "";	//if no buybox, no info shown
	Double lowestPrice = 0.0, averagePrice = 0.0, dollarWithinLowest = 0.0, dollarBelowAverage = 0.0;
	
	private static int totalItemsScraped;
	private static int totalItemsScrapedWithInfo;
	
	public Amazon(String marketplace, Camel camel) {
		this(marketplace, camel.getAsin());
		this.camel = camel;
		camelScraped = true;
	}

	public Amazon(String marketplace, String urlOrAsin) {
		this.marketplace = marketplace;
		if(urlOrAsin.contains("amazon") || urlOrAsin.contains("/")) {	//passing in url
			url = urlOrAsin;
			asin = findMatch(urlOrAsin, "(\\/([0-9]{10}|B([A-Z]|[0-9]){9}|[0-9]{9}[A-Z]{1})\\/)|Asin\\=B([A-Z]|[0-9]){9}").replace("Asin=", "").replace("/", "");
		} else {	//passing in ASIN
			asin = urlOrAsin;
			if(marketplace == "US") {
				url = "https://www.amazon.com/gp/product/"+asin;
			} else if(marketplace == "CA") {
				url = "https://www.amazon.ca/gp/product/"+asin;
			}
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		scrapedDateTime = df.format(new Date());
	}

	public void scrapeAmazonPage() throws Exception {
		totalItemsScraped++;

		Response response = Jsoup.connect(url)
				.ignoreContentType(true)
				.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36")  
				.referrer("http://www.google.com")
				.timeout(12000)
				.followRedirects(true)
				.execute();
		amazonPage = response.parse();
//		amazonPage = Jsoup.connect(url).get();

		//constant fields
		productTitle = amazonPage.select("span#productTitle").text().replace(",", "");;
		try { rating = Double.parseDouble(amazonPage.select("div#averageCustomerReviews").select("span#acrPopover").attr("title").replace(" out of 5 stars", ""));
		} catch (Exception e) { rating = 0.0; }
		try { reviews = Integer.parseInt(amazonPage.select("span#acrCustomerReviewText, span[data-hook=\"total-review-count\"]").first().text().replaceAll("[^\\d]", ""));
		} catch (Exception e) { reviews = 0; }
		try { answeredQ = Integer.parseInt(amazonPage.select("a#askATFLink > span").text().replaceAll("[^\\d]", ""));
		} catch (Exception e) { answeredQ = 0; }
		String priceStr = amazonPage.select("span#priceblock_ourprice, span#priceblock_dealprice, span#priceblock_saleprice").text().replaceAll("\\D*\\$|\\,", "");
		if(priceStr.equals("")) {	//most likely in the case of scraping Books
			priceStr = amazonPage.select("span[class=\"a-size-medium a-color-price offer-price a-text-normal\"], span[class=\"a-size-medium a-color-price header-price\"]").text().replaceAll("\\D*\\$|\\,", "");
		}
		if(url.contains("_videogames_")) {
			priceStr = priceStr.trim().replace(" ", ".");
		}
		price = priceStr.equals("") ? 0 : Double.parseDouble(priceStr);
		String savings = amazonPage.select("tr#regularprice_savings > td.a-span12, tr#dealprice_savings > td.a-span12").text();
		if(savings.equals("")) {	//most likely in the case of scraping Books
			savings = amazonPage.select("span[class=\"a-size-base a-color-secondary\"]:contains($)").text().replaceAll("^\\D*\\$", "");
		}
		if (savings.contains("%")) {
			savings = savings.replaceAll("^\\D*\\$", "").trim();
			String[] savingsSplit = savings.split(" ");
			savingsDollar = Double.parseDouble(savingsSplit[0].replaceAll("\\D*\\$|\\,", ""));
			savingsPercentage = Integer.parseInt(savingsSplit[1].replaceAll("\\(|\\)|\\%", ""));
		}
		availability = amazonPage.select("div#availability, span#availability, span#pantry-availability").text().replace(",", "");
		String merchantInfo = amazonPage.select("div#merchant-info, span#merchant-info, div#pantry-availability-brief, div#digital-toggle-buybox").text().toLowerCase();
		merchant = merchantInfo.contains("Amazon Pantry") || amazonPage.select("div#pantryStoreMessage_feature_div").text().contains("prime pantry") ? "PrimePantry" : merchantInfo.contains("sold by amazon") ? "Amazon" : merchantInfo.contains("fulfilled by") ? "FBA" : "No Prime Buybox";
		primeExclusive = merchantInfo.contains("exclusively") || amazonPage.select("div#pantryPrimeExclusiveMessage_feature_div").text().toLowerCase().contains("exclusively") ? "Yes" : "No";
		
		rank = amazonPage.select("li:contains(Amazon Best Sellers Rank)").text().replace(",", "");
		if(rank.equals("")) {
			rank = amazonPage.select("tr:contains(Best Sellers Rank)").text().replace(",", "");
		}
		rankList = rank.replaceAll("(Amazon ){0,}Best Sellers Rank\\:{0,} \\#|\\s(\\(.{1,}\\))", "").split(" #");
		Collections.sort(Arrays.asList(rankList), new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return extractInt(o1) - extractInt(o2);
		    }
		    int extractInt(String s) {
		    	String num = s.replaceAll("\\D", "");	// return 0 if no digits found
		        return num.isEmpty() ? 0 : Integer.parseInt(num);
		    }
		});
		
		//special fields
		bestSellerCategory = amazonPage.select("div.badge-wrapper").select("span.cat-link").text().replaceAll("^in\\s|,", "");
		if(bestSellerCategory.equals("")) {
			bestSellerCategory = amazonPage.select("div.badge-wrapper").select("span.cat-name").text().replaceAll("^in\\s|,", "");
		}
		amazonChoiceCategory = amazonPage.select("span.ac-keyword-link").text();
		try { promo = amazonPage.select("div#unclippedCoupon, div#clippedCoupon, div#applicablePromotionList_feature_div, div#applicable_promotion_list_sec").first().text().replace(",", "");
		} catch (NullPointerException ne) { }
//		coupon = promo.replaceAll("([=-z]|\\s|[ -#]|[&--]|\\/|\\.{2,}){1,}\\.{0,1}", "");
		if(!promo.equals("")) {
			coupon = findMatch(promo, "(\\$\\s*\\d+(\\.\\d+)?|\\d+\\%)").replace(" ", "");
		}
		addon = amazonPage.select("div.a-box-group, div#addOnItemHeader").text().contains("Add-on") ? "Yes" : "No";
		
		//calculate % within lowest/average price tracked on camelcamelcamel
		if(camelScraped) {
			int withinLowestPercentage = 0, belowAveragePercentage = 0;
			if(merchant == "Amazon" || merchant == "PrimePantry") {
				lowestPrice = camel.getLowestPriceAmazon();
				averagePrice = camel.getAveragePriceAmazon();
			} else if(merchant == "FBA") {
				lowestPrice = camel.getLowestPrice3rdPty();
				averagePrice = camel.getAveragePrice3rdPty();
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
						lowestStatus = "Within "+withinLowestPercentage+"%";
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
		}
		totalItemsScrapedWithInfo++;
		
		if(productTitle.equals("")) {
			if(amazonPage.select("p").text().contains("robot")) {
				System.err.println("========= BLOCKED BY AMAZON, EXITING =========");
//    			closeFile();
                System.exit(1);
			}
		}
	}
	
	public String getMarketplace() {
		return marketplace;
	}

	public void setMarketplace(String marketplace) {
		this.marketplace = marketplace;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAsin() {
		return asin;
	}

	public void setAsin(String asin) {
		this.asin = asin;
	}

	public String getScrapedDateTime() {
		return scrapedDateTime;
	}

	public void setScrapedDateTime(String scrapedDateTime) {
		this.scrapedDateTime = scrapedDateTime;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Document getAmazonPage() {
		return amazonPage;
	}

	public void setAmazonPage(Document amazonPage) {
		this.amazonPage = amazonPage;
	}

	public String getProductTitle() {
		return productTitle;
	}

	public void setProductTitle(String productTitle) {
		this.productTitle = productTitle;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public int getReviews() {
		return reviews;
	}

	public void setReviews(int reviews) {
		this.reviews = reviews;
	}

	public int getAnsweredQ() {
		return answeredQ;
	}

	public void setAnsweredQ(int answeredQ) {
		this.answeredQ = answeredQ;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getSavingsDollar() {
		return savingsDollar;
	}

	public void setSavingsDollar(Double savingsDollar) {
		this.savingsDollar = savingsDollar;
	}

	public int getSavingsPercentage() {
		return savingsPercentage;
	}

	public void setSavingsPercentage(int savingsPercentage) {
		this.savingsPercentage = savingsPercentage;
	}

	public String getCoupon() {
		return coupon;
	}

	public void setCoupon(String coupon) {
		this.coupon = coupon;
	}

	public String getPromo() {
		return promo;
	}

	public void setPromo(String promo) {
		this.promo = promo;
	}

	public String getAvailability() {
		return availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}

	public String getMerchant() {
		return merchant;
	}

	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	public String getPrimeExclusive() {
		return primeExclusive;
	}

	public void setPrimeExclusive(String primeExclusive) {
		this.primeExclusive = primeExclusive;
	}

	public String getAddon() {
		return addon;
	}

	public void setAddon(String addon) {
		this.addon = addon;
	}

	public String getBestSellerCategory() {
		return bestSellerCategory;
	}

	public void setBestSellerCategory(String bestSellerCategory) {
		this.bestSellerCategory = bestSellerCategory;
	}

	public String getAmazonChoiceCategory() {
		return amazonChoiceCategory;
	}

	public void setAmazonChoiceCategory(String amazonChoiceCategory) {
		this.amazonChoiceCategory = amazonChoiceCategory;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String[] getRankList() {
		return rankList;
	}

	public void setRankList(String[] rankList) {
		this.rankList = rankList;
	}

	public boolean isCamelScraped() {
		return camelScraped;
	}

	public void setCamelScraped(boolean camelScraped) {
		this.camelScraped = camelScraped;
	}

	public Camel getCamel() {
		return camel;
	}

	public void setCamel(Camel camel) {
		this.camel = camel;
	}

	public String getLowestStatus() {
		return lowestStatus;
	}

	public void setLowestStatus(String lowestStatus) {
		this.lowestStatus = lowestStatus;
	}

	public String getAverageStatus() {
		return averageStatus;
	}

	public void setAverageStatus(String averageStatus) {
		this.averageStatus = averageStatus;
	}

	public Double getLowestPrice() {
		return lowestPrice;
	}

	public void setLowestPrice(Double lowestPrice) {
		this.lowestPrice = lowestPrice;
	}

	public Double getAveragePrice() {
		return averagePrice;
	}

	public void setAveragePrice(Double averagePrice) {
		this.averagePrice = averagePrice;
	}

	public Double getDollarWithinLowest() {
		return dollarWithinLowest;
	}

	public void setDollarWithinLowest(Double dollarWithinLowest) {
		this.dollarWithinLowest = dollarWithinLowest;
	}

	public Double getDollarBelowAverage() {
		return dollarBelowAverage;
	}

	public void setDollarBelowAverage(Double dollarBelowAverage) {
		this.dollarBelowAverage = dollarBelowAverage;
	}

	public static int getTotalItemsScraped() {
		return totalItemsScraped;
	}

	public static void setTotalItemsScraped(int totalItemsScraped) {
		Amazon.totalItemsScraped = totalItemsScraped;
	}

	public static int getTotalItemsScrapedWithInfo() {
		return totalItemsScrapedWithInfo;
	}

	public static void setTotalItemsScrapedWithInfo(int totalItemsScrapedWithInfo) {
		Amazon.totalItemsScrapedWithInfo = totalItemsScrapedWithInfo;
	}

}
