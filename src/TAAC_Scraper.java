import java.sql.*;

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
public class TAAC_Scraper {
	
    private static Map<String, String> loginCookies;

    private void login() {
    }

	public static void main(String[] args) throws Exception {
		String camelPopularProductsURL = "https://camelcamelcamel.com/popular?deal=1&bn=";	//Show Deals Only
		String[] camelAmzCategory = {"home-kitchen"};
//		String[] camelAmzCategory = {"appliances","apps-for-android","arts-crafts-sewing","automotive","baby-products","beauty","books","cell-phones-accessories",
//				"collectibles-fine-art","electronics","everything-else","grocery-gourmet-food","health-personal-care","home-kitchen",
//				"industrial-scientific","movies-tv","mp3-downloads","music","musical-instruments","office-products","other","patio-lawn-garden",
//				"pet-supplies","software","spine","sports-outdoors","tools-home-improvement","toys-games","video-games"};
		String fileName;
		Scraper scraper = new Scraper();
		long startTime, endTime;
		int scrapeCount;
//		DBConn dbConn;
//		dbConn = new DBConn();		
//		dbConn.ConnectDB(); // connect to SQL DB
		//========================================= Scraping================================================
		try {			
			//	fileName = "Scrape.txt";
			//	scrapper.createFile("E:\\_GitHub\\Fantaspick", fileName);
			scrapeCount =0;

			//	while (true) {
			startTime = System.nanoTime();
			//scrapper.openFile(fileName);
			
	        try {
	            Connection.Response res = Jsoup.connect("https://camelcamelcamel.com/sessions/create")
	                    .data("login",       "fantaspick@gmail.com")
	                    .data("password",       "happybirthday2018")
	                    .method(Method.POST)
	                    .execute();

	            loginCookies = res.cookies();
		        System.out.println(res.cookies());
//		        Document document = Jsoup.connect("https://camelcamelcamel.com").cookies(loginCookies).get();
//		        Elements dev = document.select("ul#top_right_menu_2014");
//		        System.out.println(dev);
	        } catch (MalformedURLException ex) {
	            System.out.println("The URL specified was unable to be parsed or uses an invalid protocol. Please try again.");
	            System.exit(1);
	        } catch (Exception ex) {
	            System.out.println(ex.getMessage() + "\nAn exception occurred.");
	            System.exit(1);
	        }
			for (int i = 0; i < camelAmzCategory.length; i++) {
				scraper.startCamelPopularProductsURL(camelPopularProductsURL, camelAmzCategory[i], loginCookies);
			}

			//scrapper.closeFile();
			endTime = System.nanoTime();
			System.out.println("========= "+new Date().toString()+" "+(endTime-startTime)*0.000000001+" "+ scrapeCount+++" =========");
//			Thread.sleep(3000);		//1000 = 1 second
		}catch (Exception e) {
			e.printStackTrace();
		}
	}//end main

}//end class





/*		===Example===
 * final Document document = Jsoup.connect("http://www.imdb.com/chart/top").get(); // URL
//System.out.println(document.outerHtml()); // html code of whole site	

for(Element row : document.select("table.chart.full-width tr")) {			
	final String title = row.select(".titleColumn").text();
	final String rating = row.select(".imdbRating").text();			
	System.out.println(title + " -> "+ rating);
}
 */		

//final Document document = Jsoup.connect("https://www.amazon.ca/gp/bestsellers/hi/ref=sv_hi_0").get();		
//Elements elements = document.select("div#zg_centerListWrapper");
//for(Element element : elements.select("div.zg_itemImmersion")) {	
//	String itemInfo = element.select("div.zg_itemWrapper").text();
//	System.out.println(itemInfo);
//
//
//}
