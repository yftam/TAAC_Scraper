import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import scrapeInstance.Amazon;
import scrapeInstance.ScrapeUtil;
import settings.LocalOutputDest;

public class DatabaseConnection {

	public Connection con;
	public ScrapeUtil util = new ScrapeUtil();

	public DatabaseConnection() {
		try {
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			String path = LocalOutputDest.DB_DEST;
			String url = "jdbc:ucanaccess://" + path;

			con = DriverManager.getConnection(url);
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public ResultSet querySelect(String query) throws SQLException {
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);

		return rs;
	}
	
	public void upsertAmazonProduct(Amazon amz) throws SQLException {
		// if product does not exist
		if(!querySelect("SELECT ASIN FROM AmazonProduct WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'").isBeforeFirst()) {
			insertAmazonProduct(amz);
		} else {
			updateAmazonProduct(amz);
		}
	}
	
	public void insertAmazonProduct(Amazon amz) throws SQLException {
		System.out.println("PRODUCT NOT EXIST... INSERT "+amz.getAsin());
		String query =  "INSERT INTO AmazonProduct ("
				+ 			"[Active],[Category],[Link],[Marketplace],[ASIN],[DateAdded],[DateUpdated],[Product],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]) "
				+ 		"VALUES(true,?,?,?,?,Now(),Now(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
		ps.setString(parameterIndex, amz.getCategory()); parameterIndex++;
		ps.setString(parameterIndex, amz.getUrl()); parameterIndex++;
		ps.setString(parameterIndex, amz.getMarketplace()); parameterIndex++;
		ps.setString(parameterIndex, amz.getAsin()); parameterIndex++;
		ps.setString(parameterIndex, amz.getProductTitle()); parameterIndex++;
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
		ps.setString(parameterIndex, util.printRank(amz.getRankList()).replace("'", "''")); parameterIndex++;
		ps.executeUpdate();
	}
	
	public void insertAmazonProductHistory(Amazon amz) throws SQLException {
		System.out.println("INSERT "+amz.getAsin()+" INTO HISTORY FOR FUTURE TRACKING...");
		String query =  "INSERT INTO AmazonProductHistory ("
				+ 			"[Active],[Category],[Link],[Marketplace],[ASIN],[DateOverridden],[Product],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]) "
				+ 		"SELECT [Active],[Category],[Link],[Marketplace],[ASIN],Now(),[Product],[Prime],[AmazonSt],[3rdPtySt],[Rating],[Reviews],"
				+ 			"[AnsweredQ],[PriceNow],[Save],[Save%],[Coupon],[Promo],[LowestPrice],[IsLowest],[$Within],[AveragePrice],"
				+ 			"[%BelowAverageStatus],[$Below],[Stock],[Merchant],[PrimeExclusive],[BestSeller],[AmzChoice],[IsAddOn],[Rank]"
				+ 		"FROM AmazonProduct WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'";
		PreparedStatement ps = con.prepareStatement(query);
		ps.executeUpdate();
	}

	public void updateAmazonProduct(Amazon amz) throws SQLException {
		System.out.println("PRODUCT EXIST... UPDATE "+amz.getAsin());
		insertAmazonProductHistory(amz);
		String query =  "UPDATE AmazonProduct SET "
				+ 			"[DateUpdated]=Now(),[Rating]=?,[Reviews]=?,[AnsweredQ]=?,[PriceNow]=?,[Save]=?,[Save%]=?,[Coupon]=?,[Promo]=?,"
				+ 			"[Stock]=?,[Merchant]=?,[PrimeExclusive]=?,[BestSeller]=?,[AmzChoice]=?,[IsAddOn]=?,[Rank]=? "
				+ 		"WHERE Marketplace = '"+ amz.getMarketplace() + "' AND ASIN = '"+ amz.getAsin() + "'";
		PreparedStatement ps = con.prepareStatement(query);
		int parameterIndex = 1;
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
		ps.setString(parameterIndex, util.printRank(amz.getRankList()).replace("'", "''")); parameterIndex++;
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
