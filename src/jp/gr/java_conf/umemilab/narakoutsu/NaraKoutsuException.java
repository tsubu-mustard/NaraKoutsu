package jp.gr.java_conf.umemilab.narakoutsu;

public class NaraKoutsuException extends Exception {
	private int errorId;

	/**
	 * シリアル値
	 */
	private static final long serialVersionUID = 1044580966547972563L;
	
	public static final int ID_BUS_STOP_NOT_FOUND = 1;
	public static final int ID_BUS_STOP_NOT_SINGLE = 2;
	public static final int ID_ROUTE_NOT_FOUND = 3;
	public static final int ID_HTML_ERROR = 4;
	
	public NaraKoutsuException(int errorId) {
		this.errorId = errorId;
	}
	
	public int getId() {
		return errorId;
	}

}
