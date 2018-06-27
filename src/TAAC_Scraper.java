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
public class TAAC_Scraper {
	
    private static int scrapeMode = 1;

	public static void main(String[] args) throws Exception {
		Scraper scraper = new Scraper();
	    Map<String, String> camelLoginCookies = null;
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String ts = dateFormat.format(new Date()).replace(" ", "-").replaceAll("\\/|\\:", "");
		new File("scrape_results/"+ts).mkdirs();
	    if(scrapeMode == 1 || scrapeMode == 2) {
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
	    }
		if(scrapeMode == 1) {
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
		} else if (scrapeMode == 2) {	//manual scrape
			String[] asin = {"B00005O6B7","B01MZGTG96","B000TFHN56","B003TXSAHU","B0055B2RGO","B00IJ0ALYS","B076S9YBMH","B06XDW4ZXL","B00JE5FGH4","B00DF6YAIE","B01HM4Z8AI","B01MU6ZAPG","B007KNTGPU","B01HV8ZA62"};
//			String[] asin = {"0840774842","0470344016","0984504176","0802414354","B000BNG4VU","B00NRGID5S","B00O56ZOP6","B00I8YK4AQ","B01NA67U7U","B075L9KHW8","B01KLKCRTA","B07BHBTX6F"};
			scraper.setLoginCookies(camelLoginCookies);
			scraper.createFile("scrape_results/"+ts+"/"+ts+"-manualTest.csv", "scrape_results/"+ts+"/"+"error.txt");
			for (int i = 0; i < asin.length; i++) {
				scraper.startCamelProductPage("https://camelcamelcamel.com/product/"+asin[i]);
			}
			scraper.closeFile();
		}
	}//end main
}//end class
