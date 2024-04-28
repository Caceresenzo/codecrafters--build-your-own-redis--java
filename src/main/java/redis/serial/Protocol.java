package redis.serial;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Protocol {

	public static final char ARRAY = '*';
	public static final char NULL = '_';
	public static final char SIMPLE_STRING = '+';
	public static final char SIMPLE_ERROR = '-';
	public static final char INTEGER = ':';
	public static final char BULK_STRING = '$';

	public static final String CRLF = "\r\n";

}