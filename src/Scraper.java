
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Scraper {
	private File file;
	private PrintWriter printwrite;
	private int scrapCounter;





	public void createFile(String dirName, String fileName) throws IOException{
		file = new File(dirName + "/" +fileName);
		file.getParentFile().mkdirs();
		file.createNewFile();			
	}

	public void openFile(String fileName) throws IOException {
		printwrite = new PrintWriter(new FileOutputStream (new File(fileName),true));

	}


	public void closeFile() throws IOException {
		printwrite.close();
	}


	public void startScrapeLvl1_Amz(String url, DBConn dbConn) throws Exception {

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

				startScrapeLvl2_Amz(itemURL,dbConn);

				//System.out.println(itemName+";"+itemRating+";"+itemPrice+";"+itemReviewTimes+";"+itemPrime+";"+itemID+";"+itemImgURL);

				//Set all scraped into database
				//dbConn.ModDB("INSERT INTO [dbo].[Amz_Product_Details]([asin],[manufacturer_id],[category_id],[name],[product_description],[product_url],[number_of_reviews],[star_rating],[current_regular_price],[current_sale_price],[percent_off],[historic_low_price],[historic_high_price],[in_stock],[stock_status],[free_1d],[free_2d],[free_2d_date],[sold_by],[is_active],[created_by],[created_tms],[updated_by],[updated_tms])VALUES('"+itemID+"',null,0,'"+itemName+"','','"+itemURL+"',0,0.0,0.0,"+itemPrice+",null,0,0,0,0,null,null,null,'',1,'','','','')");


			}
		}
	} // end startScrapeLvl1

	public void startScrapeLvl2_Amz(String url, DBConn dbConn) throws Exception{

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


	public void startScrapeLvl1_FstTech(String url, DBConn dbConn) throws Exception{
		int itemsScraped = 0;

		final Document document = Jsoup.connect(url).get();		
		Elements elements = document.select("td.PageContentPanel");		
		for(Element element : elements.select("div#Products_Grid")) {	
			for(Element elementInner1 : element.select("div.ProductGridItem")) {
				itemsScraped++;					

				//item Name
				String itemName = elementInner1.select("div.GridItemName").text();// Item Name	

				if(itemName.equals("next")) {
					// if the end of page "next" is detected
				}else {
					//----item URL
					String[] itemURL_toSplit = (elementInner1.select("div.GridItemName").html()).split("\""); // HTML containing item Price (href inside GridItemName)
					String itemURL = "https://www.fasttech.com"+itemURL_toSplit[1];
					checkExistingURLs(itemURL,dbConn);

					//					//----item SKU
					//					String[] itemSKU_toSplit = itemURL_toSplit[1].split("/");
					//					String itemSKU = itemSKU_toSplit[4].split("-")[0];
					//					//----item Price
					//					String itemPrice = elementInner1.select("div.GridItemPrice").text();// Item Price
					//
					//					System.out.println("#" +itemsScraped+ " : " +itemName+ " | " +itemPrice+ " | "+ itemURL+" | "+ itemSKU+" | ");				

					Thread.sleep(500); // check for page load complete then continue to Level 2


				}



			}//end innerElement

		}//end element

	}// end startScrapeLvl1_FstTech

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


		public void startScrapeLvl2_FstTech_insert(String url, DBConn dbConn) throws Exception{

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

				String query = "";
						dbConn.upsertDB(query);
			}










		}// end start Scrape Lvl2





	}// end class
