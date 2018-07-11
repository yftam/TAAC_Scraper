import java.sql.*;

import settings.LocalOutputDest;

public class Connect2DB {

	public Connection con;

	public Connect2DB() {
		newConnection();
	}
	
	public void newConnection() {
		try {

			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			String path = LocalOutputDest.DB_DEST;

			String url = "jdbc:ucanaccess://" + path;

			con = DriverManager.getConnection(url);
			int in = 1;

		} catch (Exception e) {
			System.err.println(e);
		}

	} // newConnection()

	public ResultSet querySelect(String query) throws SQLException {
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(query);

		return rs;
	}

//	public boolean querySelect(String query) throws SQLException {   // returns true if rs is Empty (add-able)
//		Statement st = con.createStatement();
//		ResultSet rs = st.executeQuery(query);
//		int coloumnsNumber = rs.getMetaData().getColumnCount();
//
//		if (!rs.isBeforeFirst()) { // rs is empty
//			System.out.println("IS EMPTY, NO DUPLICATE, CAN INSERT...");
//			return true;
//		} else { // rs has somehting
//			// print rs
//			while (rs.next()) {
//				for (int i = 1; i <= coloumnsNumber; i++) {
//					if (i > 1) {
//						System.out.print(", ");
//					}
//					String coloumnValue = rs.getString(i);
//					System.out.print(coloumnValue + " " + rs.getMetaData().getColumnName(i));
//				}
//				System.out.println();
//			}
//			return false;
//		}
//	}

	public void queryInsert(String query) {
		try {

			Statement st = con.createStatement();
			st.executeUpdate(query);

		} catch (Exception e) {
			System.err.println(e);
		}

	}


}//end class
