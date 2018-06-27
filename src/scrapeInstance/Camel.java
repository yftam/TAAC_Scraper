package scrapeInstance;

import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Camel {

	private String url = "";
	private Document camelPage;
	private Map<String, String> loginCookies;
	private String productTitle = "", asin = "";
	private String priceInfoAmazonRow = "", priceInfo3rdPtyRow = "", prime = "";
	private Double lowestPriceAmazon = 0.0, averagePriceAmazon = 0.0, lowestPrice3rdPty = 0.0, averagePrice3rdPty = 0.0;
	
	private static int totalItemsScraped;
	
	public Camel(String url, Map<String, String> cookies) {
		this.url = url;
		loginCookies = cookies;
	}
	
	public void scrapeCamelPage() throws Exception {
		totalItemsScraped++;
		camelPage = Jsoup.connect(url).cookies(loginCookies).get();		// feed URL to start scrape
		String productString = camelPage.select("h2#tracks").text();
		productTitle = productString.substring(0,productString.length()-13).replace("Create Amazon price watches for: ", "").replace(",", "");
		asin = productString.substring(productString.length()-11,productString.length()-1);
		Elements priceInfoRow = camelPage.select("div#header_tracker").select("tbody");
		priceInfoAmazonRow = priceInfoRow.select("tr:nth-child(1)").select(":nth-child(9)").text();
		priceInfo3rdPtyRow = priceInfoRow.select("tr:nth-child(3)").select(":nth-child(9)").text().replace("+", "plus ");
		
		String lowestPriceAmazonStr = camelPage.select("div#section_amazon").select("tr.lowest_price").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		lowestPriceAmazon = lowestPriceAmazonStr.equals("") ? 0 : Double.parseDouble(lowestPriceAmazonStr);
		String averagePriceAmazonStr = camelPage.select("div#section_amazon").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		averagePriceAmazon = averagePriceAmazonStr.equals("") ? 0 : Double.parseDouble(averagePriceAmazonStr);
		String lowestPrice3rdPtyStr = camelPage.select("div#section_new").select("tr.lowest_price").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		lowestPrice3rdPty = lowestPrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(lowestPrice3rdPtyStr);
		String averagePrice3rdPtyStr = camelPage.select("div#section_new").select("tbody").select("tr:contains(Average)").select("td:contains($)").text().replaceAll("\\$|\\,", "");
		averagePrice3rdPty = averagePrice3rdPtyStr.equals("") ? 0 : Double.parseDouble(averagePrice3rdPtyStr);
		
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
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Document getCamelPage() {
		return camelPage;
	}

	public void setCamelPage(Document camelPage) {
		this.camelPage = camelPage;
	}

	public Map<String, String> getLoginCookies() {
		return loginCookies;
	}

	public void setLoginCookies(Map<String, String> loginCookies) {
		this.loginCookies = loginCookies;
	}

	public String getProductTitle() {
		return productTitle;
	}

	public void setProductTitle(String productTitle) {
		this.productTitle = productTitle;
	}

	public String getAsin() {
		return asin;
	}

	public void setAsin(String asin) {
		this.asin = asin;
	}

	public String getPriceInfoAmazonRow() {
		return priceInfoAmazonRow;
	}

	public void setPriceInfoAmazonRow(String priceInfoAmazonRow) {
		this.priceInfoAmazonRow = priceInfoAmazonRow;
	}

	public String getPriceInfo3rdPtyRow() {
		return priceInfo3rdPtyRow;
	}

	public void setPriceInfo3rdPtyRow(String priceInfo3rdPtyRow) {
		this.priceInfo3rdPtyRow = priceInfo3rdPtyRow;
	}

	public String getPrime() {
		return prime;
	}

	public void setPrime(String prime) {
		this.prime = prime;
	}

	public Double getLowestPriceAmazon() {
		return lowestPriceAmazon;
	}

	public void setLowestPriceAmazon(Double lowestPriceAmazon) {
		this.lowestPriceAmazon = lowestPriceAmazon;
	}

	public Double getAveragePriceAmazon() {
		return averagePriceAmazon;
	}

	public void setAveragePriceAmazon(Double averagePriceAmazon) {
		this.averagePriceAmazon = averagePriceAmazon;
	}

	public Double getLowestPrice3rdPty() {
		return lowestPrice3rdPty;
	}

	public void setLowestPrice3rdPty(Double lowestPrice3rdPty) {
		this.lowestPrice3rdPty = lowestPrice3rdPty;
	}

	public Double getAveragePrice3rdPty() {
		return averagePrice3rdPty;
	}

	public void setAveragePrice3rdPty(Double averagePrice3rdPty) {
		this.averagePrice3rdPty = averagePrice3rdPty;
	}

	public static int getTotalItemsScraped() {
		return totalItemsScraped;
	}

	public static void setTotalItemsScraped(int totalItemsScraped) {
		Camel.totalItemsScraped = totalItemsScraped;
	}
}
