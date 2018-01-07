package org.ekstep.dialcode.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.common.Platform;
import org.ekstep.common.exception.ServerException;
import org.ekstep.dialcode.common.DialCodeErrorCodes;
import org.ekstep.dialcode.common.DialCodeErrorMessage;
import org.ekstep.graph.cache.util.RedisStoreUtil;

public class SeqRandomGenerator {

	/**
	 * Get Max Index from Cassandra and Set it to Cache.
	 */
	static {
		double maxIndex;
		try {
			maxIndex = DialCodeStoreUtil.getDialCodeIndex();
			setMaxIndexToCache(maxIndex);
		} catch (Exception e) {
			throw new ServerException(DialCodeErrorCodes.ERR_SERVER_ERROR, DialCodeErrorMessage.ERR_SERVER_ERROR);
		}
	}

	private static final String[] alphabet = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C",
			"D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
			"Z" };

	private static final String stripChars = Platform.config.getString("dialcode.strip.chars");
	private static final Double length = Platform.config.getDouble("dialcode.length");
	private static final BigDecimal largePrimeNumber = new BigDecimal(
			Platform.config.getInt("dialcode.large.prime_number"));

	public static Map<Double, String> generate(double count) throws Exception {
		Map<Double, String> codes = new HashMap<Double, String>();
		double startIndex = getMaxIndex();
		int totalChars = alphabet.length;
		BigDecimal exponent = BigDecimal.valueOf(totalChars);
		exponent = exponent.pow(length.intValue());
		double codesCount = 0;
		double lastIndex = startIndex;
		while (codesCount < count) {
			BigDecimal number = new BigDecimal(lastIndex);
			BigDecimal num = number.multiply(largePrimeNumber).remainder(exponent);
			String code = baseN(num, totalChars);
			if (code.length() == length) {
				setMaxIndex();
				codesCount += 1;
				codes.put(lastIndex, code);
			}
			lastIndex = getMaxIndex();
		}
		return codes;
	}

	private static String baseN(BigDecimal num, int base) {
		if (num.doubleValue() == 0) {
			return "0";
		}
		double div = Math.floor(num.doubleValue() / base);
		String val = baseN(new BigDecimal(div), base);
		return StringUtils.stripStart(val, stripChars) + alphabet[num.remainder(new BigDecimal(base)).intValue()];
	}

	private static void setMaxIndex() throws Exception {
		DialCodeStoreUtil.setDialCodeIndex();
	}

	/**
	 * @param maxIndex
	 */
	public static void setMaxIndexToCache(Double maxIndex) {
		RedisStoreUtil.saveNodeProperty("domain", "dialcode", "max_index", maxIndex.toString());
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private static Double getMaxIndex() throws Exception {
		String indexStr = RedisStoreUtil.getNodeProperty("domain", "dialcode", "max_index");
		if (StringUtils.isNotBlank(indexStr)) {
			double index = Double.parseDouble(indexStr);
			++index;
			setMaxIndexToCache(index);
			return index;
		} else {
			double maxIndex = DialCodeStoreUtil.getDialCodeIndex();
			return maxIndex;
		}
	}

}