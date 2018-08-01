import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import scrapeInstance.Amazon;
import scrapeInstance.ScrapeUtil;
import settings.LocalOutputDest;
import settings.Settings;

public class DatabaseConnection {

	public Connection con;
	public Connection backupCon;
	public ScrapeUtil util = new ScrapeUtil();

	public DatabaseConnection() {
		try {
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			String path = LocalOutputDest.DB_DEST;
			String url = "jdbc:ucanaccess://" + path;

			System.out.println("CONNECTION TO MAIN DATABASE STARTING ... ");
			con = DriverManager.getConnection(url);
	        System.out.println("CONNECTION TO MAIN DATABASE SUCESSFUL ... ");
//			System.out.println("CONNECTION TO BACKUP DATABASE STARTING ... ");
//			backupCon = DriverManager.getConnection("jdbc:ucanaccess://"+LocalOutputDest.LOCAL_GOOGLE_DRIVE_ROOT_FOLDER+"/Code/TAAC_Scraper_Database/DataCSVs/TACC_DB_backup.accdb");
//	        System.out.println("CONNECTION TO BACKUP DATABASE SUCESSFUL ... ");
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public ResultSet querySelect(String query) throws SQLException {
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);

		return rs;
	}
	
	public void executeUpdate(String query) throws SQLException {
		PreparedStatement st = con.prepareStatement(query);
		st.executeUpdate();
	}
	
	public ResultSet selectAmazonBestSellersCategories() throws SQLException {
		String query = "SELECT [CategoryName],[CategoryUrl] FROM AmazonBestSellersCategories "
				+ "WHERE Marketplace = '"+Settings.AMAZON_MARKETPLACE+"' AND CategoryLevel = "+Settings.AMAZON_BEST_SELLERS_CATEGORY_LEVEL+" AND Enabled = true";
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);

		return rs;
	}
	
	public ResultSet selectAmazonProducts(String listOfProducts) throws SQLException {
		String query = "SELECT * FROM AmazonProduct "
				+ "WHERE Marketplace = '"+Settings.AMAZON_MARKETPLACE+"' AND ASIN IN ("+listOfProducts+")";
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);

		return rs;
	}
	
	public void setAllProductInactive(String marketplace) throws SQLException {
		String query =  "UPDATE AmazonProduct SET [Active]=? WHERE Marketplace = '"+ marketplace+"'";
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
		ps.setBoolean(parameterIndex, false);
		ps.executeUpdate();
	}
	
	public void upsertAmazonProductList(List<String[]> toInsert, List<String[]> toUpdate) throws SQLException {
		insertAmazonProductList(toInsert, "AmazonProduct");
		updateAmazonProductList(toUpdate);
	}
	
	public void insertAmazonProductList(List<String[]> toInsert, String table) throws SQLException {
		int linesToInsertAtOnce = 5;
		int linesInserted = 0;
		while(linesInserted < toInsert.size()) {
		String insert =  "INSERT INTO "+table+" ("
				+ 			"[Active],[Category],[Link],[Marketplace],[ASIN],[DateAdded],[DateUpdated],[Product],[PostCriteria],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]) "
				+ 		"VALUES";
		String values = "";
//		String deleteValues = "";
		for(int i = 0; i < linesToInsertAtOnce; i++) {
			values = util.buildStringForQuery(values, "(true,?,?,?,?,Now(),Now(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		}
		String query = insert+values;
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
		for(int i = 0; i < linesToInsertAtOnce; i++) {
			ps = setPreparedStatementValInsert(ps, parameterIndex, toInsert.get(i+linesToInsertAtOnce));
			parameterIndex = parameterIndex + 30;
//			deleteValues = util.buildStringForQuery(deleteValues, "'"+toInsert.get(i)[3]+"'");
		}
//		String delete =	"DELETE FROM "+table+" WHERE Marketplace = '"+Settings.AMAZON_MARKETPLACE+"' AND ASIN IN ("+deleteValues+")";
//		PreparedStatement d = con.prepareStatement(delete);
//		d.ex
		ps.executeUpdate();
		linesInserted = linesInserted + linesToInsertAtOnce;
		}
	}
	
	private PreparedStatement setPreparedStatementValInsert(PreparedStatement ps, int parameterIndex, String[] result) throws SQLException {
		int splitCount = 0;
		System.out.println("'"+result[3]+"',");
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setCategory
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setUrl
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setMarketplace
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setAsin
		splitCount++;	//skipping setScrapedDateTime
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setProductTitle
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setPostCriteria
		ps.setString(parameterIndex, ""); parameterIndex++;	//
		ps.setString(parameterIndex, ""); parameterIndex++;	//
		ps.setString(parameterIndex, ""); parameterIndex++;	//
		ps.setDouble(parameterIndex, Double.parseDouble(result[splitCount].replace("$",""))); parameterIndex++; splitCount++;	//setRating
		ps.setInt(parameterIndex, Integer.parseInt(result[splitCount])); parameterIndex++; splitCount++;	//setReviews
		ps.setInt(parameterIndex, Integer.parseInt(result[splitCount])); parameterIndex++; splitCount++;	//setAnsweredQ
		ps.setDouble(parameterIndex, result[splitCount].replace(" ", "").equals("") ? 0.0 : Double.parseDouble(result[splitCount].replace("$",""))); parameterIndex++; splitCount++;	//setPrice
		ps.setDouble(parameterIndex, result[splitCount].replace(" ", "").equals("") ? 0.0 : Double.parseDouble(result[splitCount].replace("$",""))); parameterIndex++; splitCount++;	//setSavingsDollar
		ps.setInt(parameterIndex, result[splitCount].replace(" ", "").equals("") ? 0 : Integer.parseInt(result[splitCount].replace("%",""))); parameterIndex++; splitCount++;	//setSavingsPercentage
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setCoupon
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setPromo
		ps.setDouble(parameterIndex, 0.0); parameterIndex++;	//
		ps.setString(parameterIndex, ""); parameterIndex++;	//
		ps.setDouble(parameterIndex, 0.0); parameterIndex++;	//
		ps.setDouble(parameterIndex, 0.0); parameterIndex++;	//
		ps.setString(parameterIndex, ""); parameterIndex++;	//
		ps.setDouble(parameterIndex, 0.0); parameterIndex++;	//
		ps.setString(parameterIndex, result[splitCount].replace("'", "''")); parameterIndex++; splitCount++;	//setAvailability
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setMerchant
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setPrimeExclusive
		ps.setString(parameterIndex, result[splitCount].replace("'", "''")); parameterIndex++; splitCount++;	//setBestSellerCategory
		ps.setString(parameterIndex, result[splitCount].replace("'", "''")); parameterIndex++; splitCount++;	//setAmazonChoiceCategory
		ps.setString(parameterIndex, result[splitCount]); parameterIndex++; splitCount++;	//setAddon
		ps.setString(parameterIndex, result[splitCount].replace("'", "''")); parameterIndex++; splitCount++;	//setRank
		return ps;
	}

	public void updateAmazonProductList(List<String[]> toUpdate) throws SQLException {
		insertAmazonProductList(toUpdate, "AmazonProductTemp");
		String query =  "UPDATE AmazonProduct AS p INNER JOIN AmazonProductTemp AS t ON (p.Marketplace = t.Marketplace) AND (p.ASIN = t.ASIN) "
				+ 		"SET p.Active=true,p.Category=t.Category,p.DateUpdated=Now(),p.PostCriteria=t.PostCriteria,p.Rating=t.Rating,p.Reviews=t.Reviews,"
				+ 		"p.AnsweredQ=t.AnsweredQ,p.PriceNow=t.PriceNow,p.Save=t.Save,p.[Save%]=t.[Save%],p.Coupon=t.Coupon,p.Promo=t.Promo,p.Stock=t.Stock,"
				+ 		"p.Merchant=t.Merchant,p.PrimeExclusive=t.PrimeExclusive,p.BestSeller=t.BestSeller,p.AmzChoice=t.AmzChoice,p.IsAddOn=t.IsAddOn,p.Rank=t.Rank";
		PreparedStatement ps = con.prepareStatement(query);
		ps.executeUpdate();
	}

	public void upsertAmazonProduct(Amazon amz) throws SQLException {
		// if product does not exist
//		if(!querySelect("SELECT ASIN FROM AmazonProduct WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'").isBeforeFirst()) {
		if(!amz.isExisting()) {
			insertAmazonProduct(amz);
		} else {
			updateAmazonProduct(amz);
		}
	}
	
	public void insertAmazonProduct(Amazon amz) throws SQLException {
		System.out.println("PRODUCT NOT EXIST... INSERT "+amz.getAsin());
		String query =  "INSERT INTO AmazonProduct ("
				+ 			"[Active],[Category],[Link],[Marketplace],[ASIN],[DateAdded],[DateUpdated],[Product],[PostCriteria],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]) "
				+ 		"VALUES(true,?,?,?,?,Now(),Now(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
		ps.setString(parameterIndex, amz.getCategory()); parameterIndex++;
		ps.setString(parameterIndex, amz.getUrl()); parameterIndex++;
		ps.setString(parameterIndex, amz.getMarketplace()); parameterIndex++;
		ps.setString(parameterIndex, amz.getAsin()); parameterIndex++;
		ps.setString(parameterIndex, amz.getProductTitle()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPostCriteria()); parameterIndex++;
		ps.setString(parameterIndex, amz.getCamel().getPrime()); parameterIndex++;
		ps.setString(parameterIndex, amz.getCamel().getPriceInfoAmazonRow()); parameterIndex++;
		ps.setString(parameterIndex, amz.getCamel().getPriceInfo3rdPtyRow()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getRating()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getReviews()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getAnsweredQ()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getPrice()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getSavingsDollar()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getSavingsPercentage()); parameterIndex++;
		ps.setString(parameterIndex, amz.getCoupon()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPromo() ); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getLowestPrice()); parameterIndex++;
		ps.setString(parameterIndex, amz.getLowestStatus()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getDollarWithinLowest()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getAveragePrice()); parameterIndex++;
		ps.setString(parameterIndex, amz.getAverageStatus()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getDollarBelowAverage()); parameterIndex++;
		ps.setString(parameterIndex, amz.getAvailability().replace("'", "''")); parameterIndex++;
		ps.setString(parameterIndex, amz.getMerchant()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPrimeExclusive()); parameterIndex++;
		ps.setString(parameterIndex, amz.getBestSellerCategory().replace("'", "''")); parameterIndex++;
		ps.setString(parameterIndex, amz.getAmazonChoiceCategory().replace("'", "''")); parameterIndex++;
		ps.setString(parameterIndex, amz.getAddon()); parameterIndex++;
		ps.setString(parameterIndex, amz.getRank().replace("'", "''")); parameterIndex++;
		ps.executeUpdate();
	}
	
	public void insertAmazonProductHistory(Amazon amz) throws SQLException {
		System.out.println("INSERT "+amz.getAsin()+" INTO HISTORY FOR FUTURE TRACKING...");
		String query =  "INSERT INTO AmazonProductHistory ("
				+ 			"[Active],[Category],[Link],[Marketplace],[ASIN],[DateOverridden],[Product],[PostCriteria],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]) "
				+ 		"SELECT [Active],[Category],[Link],[Marketplace],[ASIN],Now(),[Product],[PostCriteria],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]"
				+ 		"FROM AmazonProduct WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'";
		PreparedStatement ps = backupCon.prepareStatement(query);
		ps.executeUpdate();
	}

	public void updateAmazonProduct(Amazon amz) throws SQLException {
		System.out.println("PRODUCT EXIST... UPDATE "+amz.getAsin());
//		insertAmazonProductHistory(amz);
		String query =  "UPDATE AmazonProduct SET "
				+ 			"[Active]=true,[Category]=?,[DateUpdated]=Now(),[PostCriteria]=?,[Rating]=?,[Reviews]=?,[AnsweredQ]=?,[PriceNow]=?,[Save]=?,[Save%]=?,[Coupon]=?,[Promo]=?,"
				+ 			"[Stock]=?,[Merchant]=?,[PrimeExclusive]=?,[BestSeller]=?,[AmzChoice]=?,[IsAddOn]=?,[Rank]=? "
				+ 		"WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'";
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
		ps.setString(parameterIndex, amz.getCategory()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPostCriteria()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getRating()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getReviews()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getAnsweredQ()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getPrice()); parameterIndex++;
		ps.setDouble(parameterIndex, amz.getSavingsDollar()); parameterIndex++;
		ps.setInt(parameterIndex, amz.getSavingsPercentage()); parameterIndex++;
		ps.setString(parameterIndex, amz.getCoupon()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPromo()); parameterIndex++;
		ps.setString(parameterIndex, amz.getAvailability().replace("'", "''") ); parameterIndex++;
		ps.setString(parameterIndex, amz.getMerchant()); parameterIndex++;
		ps.setString(parameterIndex, amz.getPrimeExclusive()); parameterIndex++;
		ps.setString(parameterIndex, amz.getBestSellerCategory().replace("'", "''")); parameterIndex++;
		ps.setString(parameterIndex, amz.getAmazonChoiceCategory().replace("'", "''")); parameterIndex++;
		ps.setString(parameterIndex, amz.getAddon()); parameterIndex++;
		ps.setString(parameterIndex, amz.getRank().replace("'", "''")); parameterIndex++;
		ps.executeUpdate();
	}
	
	public void queryInsert(String query) {
		try {

			Statement st = con.createStatement();
			st.executeUpdate(query);

		} catch (Exception e) {
			System.err.println(e);
		}

	}


}//end class
