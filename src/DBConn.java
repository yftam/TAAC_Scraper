import java.sql.*;
import java.util.ArrayList;

public class DBConn {
	String connectionURL;
	Connection con = null;



	public void ConnectDB () {
		//DBConnections
		try {
			connectionURL = "jdbc:sqlserver://localhost:1433;"+"databaseName=FantaspickDB;integratedSecurity=true;";
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			con = DriverManager.getConnection(connectionURL);
			System.out.println("Connected");
		} catch (Exception e) {
			System.err.println(e);
		}
	}


	public ResultSet selectFromDB(String query) { // select from database 
		ResultSet rs = null;
		try {			
			rs = con.createStatement().executeQuery(query); //send and execute SQL manipulation statement
		}catch (Exception e) {
			e.printStackTrace();
		}
		return rs;
	}

	public void upsertDB(String query) { // add to & Modify database 
		try {			
			con.createStatement().executeQuery(query); //send and execute SQL manipulation statement
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	public ArrayList <String> QueryDB(String inputQuery) {  //use to Query database
		ResultSet result = null;		
		String SQL = inputQuery;
		ArrayList <String> databaseQueryResults = new ArrayList ();

		try {
			result = con.createStatement().executeQuery(SQL); //send and execute query on SQL
			int columnsNumber = result.getMetaData().getColumnCount(); // Get the length (# of columns) in result set			
			while (result.next()) { //Concatenate DB results & print
				for(int i = 2; i < columnsNumber; i++) {
					System.out.print(result.getString(i) + " ");
					databaseQueryResults.add(result.getString(i));
				}
			}
		}catch (Exception e)		{
			e.printStackTrace();
		}

		return databaseQueryResults;
	}




}//end class
