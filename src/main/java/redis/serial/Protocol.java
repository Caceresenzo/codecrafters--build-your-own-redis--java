package redis.serial;

public interface Protocol {

	public static final char ARRAY = '*';
	public static final char SIMPLE_STRING = '+';
	public static final char SIMPLE_ERROR = '-';
	public static final char BULK_STRING = '$';

	public static final String CRLF = "\r\n";

}