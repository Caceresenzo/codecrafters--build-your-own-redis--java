package redis.util;

import java.text.DecimalFormat;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NumberUtils {

	private static final DecimalFormat NO_SCIENTIFIC = new DecimalFormat("0");

	static {
		NO_SCIENTIFIC.setMaximumFractionDigits(340);
	}

	public static String formatDoubleNoScientific(double value) {
		return NO_SCIENTIFIC.format(value);
	}

}