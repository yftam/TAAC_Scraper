package settings;

public class Settings {
	public static int THREAD_NUM = 4;
	
	public static int SCRAPE_MODE = 4;
	public static String AMAZON_MARKETPLACE = "US";
	
	public static int SCRAPE_MODE_CAMEL_POPULAR_ITEMS = 1;
	public static int SCRAPE_MODE_CAMEL_MANUAL_LIST = 2;
	public static int SCRAPE_MODE_GET_LIST_OF_BEST_SELLERS_CATEGORIES = 3;
	public static int SCRAPE_MODE_SCRAPE_TOP_PRODUCTS_IN_BEST_SELLERS_CATEGORIES = 4;
		public static int SCRAPE_TOP_PRODUCTS_IN_BEST_SELLERS_CATEGORIES_START_LINE = 1;
		public static String AMAZON_BEST_SELLERS_CATEGORY_LEVEL = "1";
	public static int SCRAPE_MODE_SCRAPE_TOP_PRODUCTS_IN_SPECIFIED_BEST_SELLERS_CATEGORY = 5;
	public static int SCRAPE_MODE_COMBINE_TOP_PRODUCTS_RESULTS = 6;
	public static int SCRAPE_MODE_SCRAPE_TODAYS_DEALS_DEAL_OF_THE_DAY = 7;
}
