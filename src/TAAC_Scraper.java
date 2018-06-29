import settings.*;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.*;
import org.openqa.selenium.remote.DesiredCapabilities;

public class TAAC_Scraper {
	
	private static Map<String, String> connectCamel() {
		Map<String, String> camelLoginCookies = null;
        try {
            Connection.Response res = Jsoup.connect("https://camelcamelcamel.com/sessions/create")
                    .data("login",       "fantaspick@gmail.com")
                    .data("password",       "happybirthday2018")
                    .method(Method.POST)
                    .execute();

            camelLoginCookies = res.cookies();
	        System.out.println(res.cookies());
        } catch (MalformedURLException ex) {
            System.out.println("The URL specified was unable to be parsed or uses an invalid protocol. Please try again.");
            ex.printStackTrace();
            System.exit(1);
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + "\nAn exception occurred.");
			ex.printStackTrace();
            System.exit(1);
        }
		return camelLoginCookies;
	}

	public static void main(String[] args) throws Exception {
		Scraper scraper = new Scraper();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String ts = dateFormat.format(new Date()).replace(" ", "-").replaceAll("\\/|\\:", "");

		if(Settings.SCRAPE_MODE == 1) {
		    Map<String, String> camelLoginCookies = connectCamel();
			new File("scrape_results/"+ts).mkdirs();
			String camelPopularProductsURL = "https://camelcamelcamel.com/popular";	//Show Deals Only
//			String[] camelAmzCategory = {"baby-products"};
			String[] camelAmzCategory = {
//					"appliances","apps-for-android","arts-crafts-sewing","automotive","baby-products","beauty",
//					"books",
//					"cell-phones-accessories",
//					"collectibles-fine-art",
					"electronics",
//					"everything-else","grocery-gourmet-food","health-personal-care","home-kitchen",
//					"industrial-scientific","movies-tv","mp3-downloads","music","musical-instruments","office-products","other","patio-lawn-garden",
//					"pet-supplies","software","spine","sports-outdoors","tools-home-improvement","toys-games","video-games"
					};
//			String[] camelAmzCategory = {"toys-games","video-games"};
			
			long startTime, endTime;
			startTime = System.nanoTime();
	
			try {
				for (int i = 0; i < camelAmzCategory.length; i++) {
					scraper.createFile("scrape_results/"+ts+"/"+ts+"-"+camelAmzCategory[i]+".csv", "scrape_results/"+ts+"/"+"error.txt");
					scraper.startCamelPopularProductsURL(camelPopularProductsURL, "?deal=1&bn="+camelAmzCategory[i], camelLoginCookies);
					scraper.closeFile();
				}
				endTime = System.nanoTime();
				System.out.println("========= "+new Date().toString()+" "+(endTime-startTime)*0.000000001+" =========");
				System.out.println("========= FINISHED =========");
//				Thread.sleep(3000);		//1000 = 1 second
			}catch (Exception e) {
				System.err.println("FATAL ERROR");
				e.printStackTrace();
			}
		} else if (Settings.SCRAPE_MODE == 2) {	//manual scrape
		    Map<String, String> camelLoginCookies = connectCamel();
			new File("scrape_results/"+ts).mkdirs();
			String[] asin = {"B00005O6B7","B01MZGTG96","B000TFHN56","B003TXSAHU","B0055B2RGO","B00IJ0ALYS","B076S9YBMH","B06XDW4ZXL","B00JE5FGH4","B00DF6YAIE","B01HM4Z8AI","B01MU6ZAPG","B007KNTGPU","B01HV8ZA62"};
//			String[] asin = {"0840774842","0470344016","0984504176","0802414354","B000BNG4VU","B00NRGID5S","B00O56ZOP6","B00I8YK4AQ","B01NA67U7U","B075L9KHW8","B01KLKCRTA","B07BHBTX6F"};
			scraper.setLoginCookies(camelLoginCookies);
			scraper.createFile("scrape_results/"+ts+"/"+ts+"-manualTest.csv", "scrape_results/"+ts+"/"+"error.txt");
			for (int i = 0; i < asin.length; i++) {
				scraper.startCamelProductPage("https://camelcamelcamel.com/product/"+asin[i]);
			}
			scraper.closeFile();
		} else if (Settings.SCRAPE_MODE == 3) {	//scrape Amazon Best Seller Categories
			String url = Urls.AMAZON_US_BEST_SELLER_ROOT;	//by default use Amazon US url
			if (Settings.AMAZON_MARKETPLACE == "CA") {
				url = Urls.AMAZON_CA_BEST_SELLER_ROOT;
			}
			new File("scrape_results/amazon_best_sellers").mkdirs();
			scraper.createFile("scrape_results/amazon_best_sellers/"+ts+"-categories_list.csv", "scrape_results/amazon_best_sellers/"+"error.txt");
			scraper.startAmazonBestSellersList(url, 1);
//			scraper.startAmazonBestSellersList("https://www.amazon.com/Best-Sellers/zgbs/amazon-devices/ref=zg_bs_nav_0", 2);
			scraper.closeFile();
		} else if (Settings.SCRAPE_MODE == 4) {	//scrape Amazon Today's Deals -> Deal of the Day filter
			String url = Urls.AMAZON_US_TODAYS_DEALS_DEAL_OF_THE_DAY;	//by default use Amazon US url
			if (Settings.AMAZON_MARKETPLACE == "CA") {
				url = Urls.AMAZON_CA_TODAYS_DEALS_DEAL_OF_THE_DAY;
			}
			scraper.startAmazonTodaysDeals(url);
		} else if (Settings.SCRAPE_MODE == 90) {	//keepa url decoder
			String url = "https://keepa.com/#!deals/%7B%22page%22%3A0%2C%22perPage%22%3A80%2C%22domainId%22%3A%221%22%2C%22excludeCategories%22%3A%5B%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%5D%2C%22includeCategories%22%3A%5B%5B%5D%2C%5B165796011%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%2C%5B%5D%5D%2C%22priceTypes%22%3A%5B0%5D%2C%22deltaRange%22%3A%5B0%2C2147483647%5D%2C%22deltaPercentRange%22%3A%5B20%2C2147483647%5D%2C%22salesRankRange%22%3A%5B-1%2C10000%5D%2C%22currentRange%22%3A%5B500%2C2147483647%5D%2C%22isLowest%22%3Atrue%2C%22isLowestOffer%22%3Afalse%2C%22isBackInStock%22%3Afalse%2C%22isOutOfStock%22%3Afalse%2C%22titleSearch%22%3A%22%22%2C%22isRangeEnabled%22%3Atrue%2C%22isFilterEnabled%22%3Atrue%2C%22filterErotic%22%3Afalse%2C%22hasReviews%22%3Afalse%2C%22isPrimeExclusive%22%3Afalse%2C%22sortType%22%3A4%2C%22dateRange%22%3A1%2C%22settings%22%3A%7B%22viewTyp%22%3A0%7D%2C%22isRisers%22%3Afalse%2C%22isHighest%22%3Afalse%7D";
			url = url.replace("%22","\"").replace("%7B", "{").replace("7D", "}").replace("%3A", ":").replace("%2C", ",").replace("%5B", "[").replace("%5D", "]");
			System.out.println(url);
//			Document keepa = Jsoup.parse(new File("keepaHTML/test.txt"), null);
//			Elements e = keepa.select("div#filterCats").select("span[style]");
//			System.out.println(e.html());
			DesiredCapabilities caps = new DesiredCapabilities();
			caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "C:\\Users\\Alex New\\Google Drive (taac.fellows@gmail.com)\\Code\\TAAC_Scraper\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");
			caps.setJavascriptEnabled(true);
			
			WebDriver driver = new PhantomJSDriver(caps);
			driver.get("https://keepa.com/#!deals/");
			System.out.println(driver.getPageSource());
		}
	}//end main
}//end class
