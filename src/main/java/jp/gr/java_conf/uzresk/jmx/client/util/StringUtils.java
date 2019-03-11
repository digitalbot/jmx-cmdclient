package jp.gr.java_conf.uzresk.jmx.client.util;

public class StringUtils {
	private StringUtils() {}

	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	public static boolean isBlank(String str) {
		if (isEmpty(str)) {
			return true;
		}
		for (char c : str.toCharArray()) {
			if (!Character.isWhitespace(c)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
}
