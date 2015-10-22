/*******************************************************************************
 * Copyright (c) 2010, 2012 Institute for Dutch Lexicology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package nl.ru.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.RuleBasedCollator;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Een collectie reguliere expressies om verschillende patronen uit Strings te filteren.
 */
public class StringUtil {
  public static final int INDEX_NOT_FOUND = -1;
	/** String containing nbsp character (decimal 160 = hex A0) */
	public static final String STR_NON_BREAKING_SPACE = "\u00A0";

	/** nbsp character (decimal 160 = hex A0) */
	public static final char CHAR_NON_BREAKING_SPACE = '\u00A0';

	/** Matches whitespace. */
	private static final Pattern PATT_WHITESPACE = Pattern.compile("\\s+");

	/** Match een XML tag */
	private final static Pattern PATT_XML_TAG = Pattern.compile("<[^>]+>");

	/** Match een dubbele quote */
	private final static Pattern PATT_DOUBLE_QUOTE = Pattern.compile("\"");

	/** Match een enkele quote */
	private final static Pattern PATT_SINGLE_QUOTE = Pattern.compile("'");

	/** Matcht een niet-lege string die alleen whitespace bevat */
	private final static Pattern PATT_ONLY_WHITESPACE = Pattern.compile("^\\s+$");

	private static final Pattern PATT_APOSTROPHE = Pattern.compile("'");

	/** Pattern matching nbsp character (decimal 160 = hex A0) */
	private static final Pattern PATT_NON_BREAKING_SPACE = Pattern.compile(STR_NON_BREAKING_SPACE);
  
  /** Hex array */
  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Matches Unicode diacritics composition characters, which are separated out by the Normalizer
	 * and then discarded using this regex.
	 */
	private static final Pattern PATT_DIACRITICS = Pattern
			.compile("\\p{InCombiningDiacriticalMarks}+");

	/** Whitespace and/or punctuation at start */
	final static Pattern PATT_WS_PUNCT_AT_END = Pattern.compile("[\\p{P}\\s]+$");

	/** Whitespace and/or punctuation at end */
	final static Pattern PATT_WS_PUNCT_AT_START = Pattern.compile("^[\\p{P}\\s]+");

	/** Punctuation. */
	private static final Pattern PATT_PUNCTUATION = Pattern.compile("\\p{P}");

	/** The default collator: Dutch, case-insensitive */
	protected static Collator dutchInsensitiveCollator = null;

	private static Collator englishInsensitiveCollator;

	/**
	 * Replaces space with non-breaking space so the browser doesn't word-wrap
	 *
	 * @param input
	 *            the string with spaces
	 * @return the string with non-breaking spaces
	 */
	public static String makeNonBreaking(String input) {
		return PATT_WHITESPACE.matcher(input).replaceAll(STR_NON_BREAKING_SPACE);
	}

	/**
	 * Replaces non-breaking spaces with normal spaces.
	 *
	 * @param string
	 *            the input
	 * @return the result
	 */
	public static String convertNbspToSpace(String string) {
		return PATT_NON_BREAKING_SPACE.matcher(string).replaceAll(" ");
	}
  
  // Count matches
  //-----------------------------------------------------------------------
  /**
   * <p>Counts how many times the substring appears in the larger string.</p>
   *
    * <p>A {@code null} or empty ("") String input returns {@code 0}.</p>
    *
    * <pre>
    * StringUtils.countMatches(null, *)       = 0
    * StringUtils.countMatches("", *)         = 0
    * StringUtils.countMatches("abba", null)  = 0
    * StringUtils.countMatches("abba", "")    = 0
    * StringUtils.countMatches("abba", "a")   = 2
    * StringUtils.countMatches("abba", "ab")  = 1
    * StringUtils.countMatches("abba", "xxx") = 0
    * </pre>
    *
    * @param str  the CharSequence to check, may be null
    * @param sub  the substring to count, may be null
    * @return the number of occurrences, 0 if either CharSequence is {@code null}
    * @since 3.0 Changed signature from countMatches(String, String) to countMatches(CharSequence, CharSequence)
    */
   public static int countMatches(final CharSequence str, final CharSequence sub) {
       if (isEmpty(str) || isEmpty(sub)) {
           return 0;
       }
       int count = 0;
       int idx = 0;
       while ((idx = CharSequenceUtils.indexOf(str, sub, idx)) != INDEX_NOT_FOUND) {
           count++;
           idx += sub.length();
       }
       return count;
   }

   /**
    * <p>Counts how many times the char appears in the given string.</p>
    *
    * <p>A {@code null} or empty ("") String input returns {@code 0}.</p>
    *
    * <pre>
    * StringUtils.countMatches(null, *)       = 0
    * StringUtils.countMatches("", *)         = 0
    * StringUtils.countMatches("abba", 0)  = 0
    * StringUtils.countMatches("abba", 'a')   = 2
    * StringUtils.countMatches("abba", 'b')  = 2
    * StringUtils.countMatches("abba", 'x') = 0
    * </pre>
    *
    * @param str  the CharSequence to check, may be null
    * @param ch  the char to count
    * @return the number of occurrences, 0 if the CharSequence is {@code null}
    */
  public static int countMatches(final CharSequence str, final char ch) {
      if (isEmpty(str)) {
          return 0;
      }
      int count = 0;
      // We could also call str.toCharArray() for faster look ups but that would generate more garbage.
      for (int i = 0; i < str.length(); i++) {
          if (ch == str.charAt(i)) {
              count++;
          }
      }
      return count;
  }
  
  /**
  * <p>Checks if a CharSequence is empty ("") or null.</p>
  *
  * <pre>
  * StringUtils.isEmpty(null)      = true
  * StringUtils.isEmpty("")        = true
  * StringUtils.isEmpty(" ")       = false
  * StringUtils.isEmpty("bob")     = false
  * StringUtils.isEmpty("  bob  ") = false
  * </pre>
  *
  *
  * @param cs  the CharSequence to check, may be null
  * @return {@code true} if the CharSequence is empty or null
  */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }
   
  public static boolean isNumeric(String str)
  {
    NumberFormat formatter = NumberFormat.getInstance();
    ParsePosition pos = new ParsePosition(0);
    formatter.parse(str, pos);
    return str.length() == pos.getIndex();
  }
	/**
	 * Abbreviates a string for display if necessary.
	 *
	 * Also replaces line breaks with spaces. Uses overshootAllowed of 0, and adds ellipsis if
	 * abbreviated.
	 *
	 * @param str
	 *            the string to abbreviate
	 * @param preferredLength
	 *            the maximum length we would like to see
	 * @return the (possibly) abbreviated string
	 */
	public static String abbreviate(String str, int preferredLength) {
		return abbreviate(str, preferredLength, 0, true);
	}

	/**
	 * Abbreviates a string for display if necessary.
	 *
	 * Also normalizes whitspace (replacing a line break with a space).
	 *
	 * @param str
	 *            the string to abbreviate
	 * @param preferredLength
	 *            the maximum length we would like to see
	 * @param overshootAllowed
	 *            how many more characters than the previous value is allowable
	 * @param addEllipsis
	 *            whether or not we should add "..." at the end if we abbreviated
	 * @return the (possibly) abbreviated string
	 */
	public static String abbreviate(String str, int preferredLength, int overshootAllowed,
			boolean addEllipsis) {
		String result = str.replaceAll("\\s+", " "); // normalize whitespace
		if (result.length() > preferredLength + overshootAllowed) {
			int i = result.substring(0, preferredLength + 1).lastIndexOf(" ");
			if (i >= 1)
				result = result.substring(0, i);
			else
				result = result.substring(0, preferredLength);
			if (addEllipsis)
				result += "...";
		}
		return result.trim();
	}

	/**
	 * Escape capital letters with the specified escape string.
	 *
	 * NOTE: this only works properly on ASCII strings!
	 *
	 * This can be used to compensate for case-insensitive filesystems.
	 *
	 * @param str
	 *            the string in which to escape capitals
	 * @param escapeString
	 *            the string to add in front of each capital
	 * @return the escaped string
	 */
	public static String escapeCapitals(String str, String escapeString) {
		// TODO: eliminate Latin char assumption; define static Pattern
		return str.replaceAll("[A-Z]", escapeString + "$0");
	}

	/**
	 * Escape csv special character (quote)
	 *
	 * @param termStr
	 *            the string to escape characters in
	 * @return the escaped string
	 */
	public static String escapeCsvCharacters(String termStr) {
		return PATT_DOUBLE_QUOTE.matcher(termStr).replaceAll("\"\"");
	}

	static final Pattern regexCharacters = Pattern
			.compile("([\\|\\\\\\?\\*\\+\\(\\)\\[\\]\\-\\^\\$\\{\\}\\.])");

	/**
	 * Escape regex special characters
	 *
	 * @param termStr
	 *            the string to escape characters in
	 * @return the escaped string
	 */
	public static String escapeRegexCharacters(String termStr) {
		Matcher m = regexCharacters.matcher(termStr);
		termStr = m.replaceAll("\\\\$1");
		return termStr;
	}

	/**
	 * Escape regex special characters
	 *
	 * @param termStr
	 *            the string to escape characters in
	 * @return the escaped string
	 */
	public static boolean containsRegexCharacters(String termStr) {
		Matcher m = regexCharacters.matcher(termStr);
		return m.matches();
	}

	/**
	 * Escape the wildcard characters * and ? in a string with a \
	 *
	 * @param result
	 *            the original string
	 * @return the string with the wildcard characters escaped with a \.
	 */
	public static String escapeWildcardCharacters(String result) {
		// Escape regex-characters
		Pattern p = Pattern.compile("([\\?\\*])");
		Matcher m = p.matcher(result);
		result = m.replaceAll("\\\\$1");
		return result;
	}

	/**
	 * Escape the special XML chars (<, >, &, ") with their named entity equivalents.
	 *
	 * @param source
	 *            the source string
	 * @return the escaped string
	 */
	public static String escapeXmlChars(String source) {
		int estResultLength = source.length() * 5 / 4; // reasonable estimate of max. space needed
		StringBuilder sb = new StringBuilder(estResultLength);
		int start = 0;
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
				sb.append(source.substring(start, i));
				switch (c) {
				case '<':
					sb.append("&lt;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				case '"':
					sb.append("&quot;");
					break;
				case '\'':
					sb.append("''");
					break;
				}
				start = i + 1;
			}
		}
		sb.append(source.substring(start));
		return sb.toString();
	}

	/**
	 * Escape a string for inclusion in JSON output.
	 *
	 * Conforms to the JSON.org spec for strings.
	 *
	 * @param input the input string
	 * @return the JSON-escaped output string
	 */
	public static String escapeJson(String input) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			// One of the characters with its own escape?
			int x = "\"\\\b\f\n\r\t".indexOf(c);
			if (x < 0) {
				// No; other control character?
				if (c < 32) {
					// Yes. Use numeric escape
					result.append(String.format("\\u%04x", (int)c));
				} else {
					// No, normal char; no problem
					result.append(c);
				}
			} else {
				// Double quote, backslash, bell, form feed,
				// newline, return or tab. Add special escape
				// depending on the character.
				switch (x) {
				case 0: result.append("\\\""); break;
				case 1: result.append("\\\\"); break;
				case 2: result.append("\\b"); break;
				case 3: result.append("\\f"); break;
				case 4: result.append("\\n"); break;
				case 5: result.append("\\r"); break;
				case 6: result.append("\\t"); break;
				}
			}
		}
		return result.toString();
	}
  
  /**
   * escapeHexCoding
	 *    Escape a string for inclusion in JSON output:
   *        (1) compress it
   *        (2) convert it to a hex-dump
	 *
	 * @param input the input string
	 * @return the HEX coded output string
	 */
	public static String escapeHexCoding(String input) {
		StringBuilder result = new StringBuilder();
    // Turn string into bytes using UTF8
    byte[] arByte = input.getBytes(Charset.forName("UTF-8"));
    // Compress the byte array
    Deflater compresser = new Deflater();
    compresser.setInput(arByte);
    compresser.finish();
    // Make room for the output
    byte[] arCompr = new byte[arByte.length];
    // Compress the input into the output
    int compressedDataLength = compresser.deflate(arCompr);
    compresser.end();
    // COpy to smaller array
    byte[] arSmall = new byte[compressedDataLength];
    for (int i=0;i<arSmall.length;i++) {
      arSmall[i] = arCompr[i];
    }
    /* 
    // Convert the compressed output to HEX 
		for (int i = 0; i < compressedDataLength; i++) {
      char[] hexChars = new char[2];
      // Get a byt-to-hex equivalent efficiently
      int v = arCompr[i] & 0xFF;
      hexChars[0] = hexArray[v >>> 4];
      hexChars[1] = hexArray[v & 0x0F];
      // Add the byte-to-hex equivalent to the result
      result.append(new String(hexChars));
		}
		return result.toString();
    /* */
    /* Alternative: base64 encoding */
    String sEnc = Base64.encode(arSmall);
    return sEnc;
    /* */
	}
  /**
   * unescapeHexCoding
	 *    Unescape a string that was turned into a hex dump:
   *      (1) hex to byte array
   *      (2) byte array unzip
   *      (3) result to string
	 *
	 * @param input the input string
   * 
	 * @return the HEX coded output string
   * 
   * @throws java.io.IOException
   * @throws java.util.zip.DataFormatException
   */
  public static String unescapeHexCoding(String input) throws IOException, DataFormatException {
    int resultLength = 0;
		// StringBuilder result = new StringBuilder();
    int compressedDataLength = input.length();
    // Convert Base64 into byte array
    byte[] arByte = Base64.decode(input);
    
    /*
    // Turn hex input into byte array
    byte[] arByte = new byte[compressedDataLength];
    for (int i = 0; i < input.length(); i+=2) {
      char[] hexChars = new char[2];
      hexChars[0] = input.charAt(i);
      hexChars[1] = input.charAt(i+1);
      int iHC0 = hexChars[0] << 4;
      int iHC1 = hexChars[1] & 0x0F;
      int v = 16 *  iHC0 + iHC1;
      arByte[i/2] = (byte) v;
    }
    */
    // Decompress byte-array
    byte[] arDecr = new byte[compressedDataLength * 10];
    Inflater decompresser = new Inflater();
    // decompresser.setInput(arByte, 0, compressedDataLength);
    decompresser.setInput(arByte);
    resultLength = decompresser.inflate(arDecr);
    decompresser.end();    
    
    // Convert byte array into string again
    String sResult = new String(arDecr, 0, resultLength, "UTF-8");

		return sResult;
  }

	/**
	 * Get the default collator.
	 *
	 * @return the default collator.
	 */
	public static Collator getDefaultCollator() {
		return getDutchInsensitiveCollator();
	}

	/**
	 * Get a Dutch, case-insensitive collator.
	 *
	 * @return the Dutch, case-insensitive collator.
	 */
	public static Collator getDutchInsensitiveCollator() {
		if (dutchInsensitiveCollator == null) {
			dutchInsensitiveCollator = Collator.getInstance(LocaleUtil.getDutchLocale());
			dutchInsensitiveCollator.setStrength(Collator.SECONDARY);
		}
		return dutchInsensitiveCollator;
	}

	/**
	 * Get a Dutch, case-insensitive collator.
	 *
	 * @return the Dutch, case-insensitive collator.
	 */
	public static Collator getEnglishInsensitiveCollator() {
		if (englishInsensitiveCollator == null) {
			englishInsensitiveCollator = Collator.getInstance(LocaleUtil.getEnglishLocale());
			englishInsensitiveCollator.setStrength(Collator.SECONDARY);
		}
		return englishInsensitiveCollator;
	}

	/**
	 * Checks if the specified string is made up of whitespace only.
	 *
	 * @param string
	 *            the string to check
	 * @return true if the specified string is only whitespace, false otherwise
	 */
	public static boolean isWhitespace(String string) {
		return PATT_ONLY_WHITESPACE.matcher(string).matches();
	}

	/**
	 * Join a number of (string representations of) items to a single string using a delimiter
	 *
	 * @param <T>
	 *            the type of items to join
	 * @param parts
	 *            the parts to join
	 * @param delimiter
	 *            the delimiter to use
	 * @return the joined string
	 */
	public static <T> String join(Iterable<T> parts, String delimiter) {
          StringBuilder builder = new StringBuilder();
          Iterator<T> iter = parts.iterator();
          while (iter.hasNext()) {
            builder.append(iter.next().toString());
            if (!iter.hasNext()) {
              break;
            }
            builder.append(delimiter);
          }
          return builder.toString();
	}

	/**
	 * Join a number of (string representations of) items to a single string using a delimiter
	 *
	 * @param <T>
	 *            the type of items to join
	 * @param parts
	 *            the array of parts to join
	 * @param delimiter
	 *            the delimiter to use
	 * @return the joined string
	 */
	public static <T> String join(T[] parts, String delimiter) {
          StringBuilder builder = new StringBuilder();
          for (T t: parts) {
            if (builder.length() > 0)
              builder.append(delimiter);
            builder.append(t.toString());
          }
          return builder.toString();
	}

	/**
	 * Join keys and values from a map to produce a string.
	 *
	 * @param map the map to join
	 * @param delimiter how to delimit map entries
	 * @param keyValueDelimiter what to put between key and value
	 * @return the resulting string
	 */
	public static <T, U> String join(Map<T, U> map, String delimiter, String keyValueDelimiter) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<T, U> e: map.entrySet()) {
			if (builder.length() > 0)
				builder.append(delimiter);
			builder.append(e.getKey().toString()).append(keyValueDelimiter).append(e.getValue().toString());
		}
		return builder.toString();
	}

	/**
	 * Join keys and values from a map to produce a string.
	 *
	 * Uses an equals sign between key and value.
	 *
	 * @param map the map to join
	 * @param delimiter how to delimit map entries
	 * @return the resulting string
	 */
	public static <T, U> String join(Map<T, U> map, String delimiter) {
		return join(map, delimiter, "=");
	}

	/**
	 * Join keys and values from a map to produce a string.
	 *
	 * Uses an equals sign between key and value and a semicolon and
	 * space between entries.
	 *
	 * @param map the map to join
	 * @return the resulting string
	 */
	public static <T, U> String join(Map<T, U> map) {
		return join(map, "; ");
	}

	/**
	 * Limit a string to a certain length, adding an ellipsis if desired.
	 *
	 * Tries to cut the string at a word boundary, but will cut through words if necessary.
	 *
	 * @param str
	 *            the string
	 * @param preferredLength
	 *            the preferred maximum length for the result string
	 * @param overshootAllowed
	 *            the number of characters the string may run beyond the preferred length without
	 *            resorting to cutting
	 * @param addEllipsis
	 *            whether or not to add three periods (...) to a string after cutting
	 * @return the resulting string. This is at most
	 *         <code>preferredLength + max(overshootAllowed, 3)</code> long (if addEllipsis is
	 *         true).
	 */
	public static String limitStringToLength(String str, int preferredLength, int overshootAllowed,
			boolean addEllipsis) {
		String result = str;
		if (result.length() > preferredLength + overshootAllowed) {
			int i = result.substring(0, preferredLength + 1).lastIndexOf(" ");
			if (i >= 1)
				result = result.substring(0, i);
			else
				result = result.substring(0, preferredLength);
			if (addEllipsis)
				result += "...";
		}
		return result.trim();
	}

	/**
	 * Replace adjacent whitespace characters with a single space
	 *
	 * @param s
	 *            source string
	 * @return the result
	 */
	public static String normalizeWhitespace(String s) {
		Matcher m = PATT_WHITESPACE.matcher(s);
		return m.replaceAll(" ");
	}

	/**
	 * Removes apostrophes from a string
	 *
	 * @param str
	 *            the input string
	 * @return the string with apostrophes removed.
     * @deprecated was used for sorting; should be done with Collator instead.
	 */
	@Deprecated
	public static String removeApostrophes(String str) {
		Matcher m = PATT_APOSTROPHE.matcher(str);
		str = m.replaceAll("");
		return str;
	}

	/**
	 * Remove XML tags from a string
	 *
	 * @param s
	 *            the source string
	 * @return the string with tags removed
	 */
	public static String removeTags(String s) {
		return PATT_XML_TAG.matcher(s).replaceAll(""); // tags verwijderen
	}

	/**
	 * Unescape XML special characters (<, >, & and ")
	 *
	 * @param source
	 *            the string with XML characters escaped as XML entities
	 * @return the unescaped string
	 */
	public static String unescapeXmlChars(String source) {
		// TODO: define static Patterns for these
		return source.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&")
				.replaceAll("&quot;", "\"");
	}

	/**
	 * Wrap text at the specified number of characters
	 *
	 * @param message
	 *            the text to wrap
	 * @param wrapAt
	 *            the maximum number of characters per line
	 * @return the wrapped text
	 */
	public static String wrapText(String message, int wrapAt) {
		StringBuilder wrapped = new StringBuilder();
		String lines[] = message.split("\n");
		for (String line : lines) {
			if (line.length() > 0) {
				while (line.length() > 0) {
					int i = wrapAt + 1;
					if (i < line.length()) {
						while (i > 0) {
							char c = line.charAt(i);
							if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
								break;
							i--;
						}
						if (i == 0)
							i = wrapAt + 1;
					} else
						i = line.length();
					wrapped.append(line.substring(0, i).trim()).append("\n");
					line = line.substring(i).trim();
				}
			} else {
				wrapped.append("\n");
			}
		}
		return wrapped.toString().trim();
	}

	/**
	 * When called with a null reference, returns the empty string. Otherwise, returns the string
	 * unchanged
	 *
	 * @param str
	 *            the input string (or a null reference)
	 * @return the original string, or the empty string
	 */
	public static String nullToEmpty(String str) {
		return str == null ? "" : str;
	}

	/**
	 * Convert accented letters to their unaccented counterparts.
	 *
	 * @param input
	 *            the string possibly containing accented letters.
	 * @return the unaccented version
	 */
	public static String removeAccents(String input) {
		// Separate characters into base character and diacritics characters
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

		// Remove diacritics
		return PATT_DIACRITICS.matcher(normalized).replaceAll("");
	}

	/**
	 * Replace any punctuation characters with a space.
	 *
	 * @param input
	 *            the input string
	 * @return the string without punctuation
	 */
	public static String removePunctuation(String input) {
		return PATT_PUNCTUATION.matcher(input).replaceAll(" ");
	}

	/**
	 * Remove any punctuation and whitespace at the start and end of input.
	 *
	 * @param input
	 *            the input string
	 * @return the string without punctuation or whitespace at the edges.
	 */
	public static String trimWhitespaceAndPunctuation(String input) {
		input = PATT_WS_PUNCT_AT_END.matcher(input).replaceAll("");
		input = PATT_WS_PUNCT_AT_START.matcher(input).replaceAll("");
		return input;
	}

	/**
	 * Return the singular or the plural form of a noun depending on a number.
	 *
	 * This version of the method simply appends an "s" to form the plural.
	 * For irregular plural forms, use the version that takes 3 parameters.
	 *
	 * @param singular the singular to 'pluralize'
	 * @param number if this equals 1, no s is added
	 * @return the possibly pluralized form
	 */
	public static String pluralize(String singular, long number) {
		return pluralize(singular, singular + "s", number);
	}

	/**
	 * Return the singular or the plural form of a noun depending on a number.
	 *
	 * @param singular the singular form of the word
	 * @param plural the plural form of the word
	 * @param number if this equals 1, the sinular is returned, otherwise the plural
	 * @return the possibly pluralized form
	 */
	public static String pluralize(String singular, String plural, long number) {
		return number == 1 ? singular : plural;
	}

	/**
	 * Convert the first character in a string to uppercase.
	 * @param str the string to be capitalized
	 * @return the capitalized string
	 */
	public static String capitalizeFirst(String str) {
		if (str.length() == 0)
			return str;
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Returns a new collator that takes spaces into account (unlike the default Java collators,
	 * which ignore spaces), so we can sort "per word".
	 *
	 * Example: with the default collator, "cat dog" would be sorted after "catapult" (a after d).
	 * With the per-word collator, "cat dog" would be sorted before "catapult" (cat before
	 * catapult).
	 *
	 * NOTE: the base collator must be a RuleBasedCollator, but the argument has type Collator for
	 * convenience (not having to explicitly cast when calling)
	 *
	 * @param base
	 *            the collator to base the per-word collator on.
	 * @return the per-word collator
	 */
	public static RuleBasedCollator getPerWordCollator(Collator base) {
		if (!(base instanceof RuleBasedCollator))
			throw new RuntimeException("Base collator must be rule-based!");

		try {
			// Insert a collation rule to sort the space character before the underscore
			RuleBasedCollator ruleBasedCollator = (RuleBasedCollator) base;
			String rules = ruleBasedCollator.getRules();
			return new RuleBasedCollator(rules.replaceAll("<'_'", "<' '<'_'"));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a new collator that sort digits at the end of the alphabet instead of the beginning.
	 *
	 * NOTE: the base collator must be a RuleBasedCollator, but the argument has type Collator for
	 * convenience (not having to explicitly cast when calling)
	 *
	 * @param base
	 *            the collator to base the new collator on.
	 * @return the new collator
	 */
	public static RuleBasedCollator getSortDigitsAtEndCollator(Collator base) {
		if (!(base instanceof RuleBasedCollator))
			throw new RuntimeException("Base collator must be rule-based!");

		try {
			// Insert a collation rule to sort the space character before the underscore
			RuleBasedCollator ruleBasedCollator = (RuleBasedCollator) base;
			String rules = ruleBasedCollator.getRules();
			rules = rules.replaceAll("<0<1<2<3<4<5<6<7<8<9", "");
			rules += "<0<1<2<3<4<5<6<7<8<9";
			return new RuleBasedCollator(rules);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Escape any apostrophes in the string, for when we want to single-quote it.
	 * @param str the string to escape
	 * @return the escaped string
	 */
	public static String escapeApostrophe(String str) {
		return PATT_SINGLE_QUOTE.matcher(str).replaceAll("\\\\'");
	}

	/**
	 * Escape double quote and backslash with a backslash character.
	 *
	 * Useful for putting strings between double quotes.
	 *
	 * @param str the string to escape
	 * @return the escaped string
	 */
	public static String escapeDoubleQuotedString(String str) {
		str = str.replaceAll("[\"\\\\]", "\\\\$0");
		str = str.replaceAll("\r?\n", "\\\\n");
		return str;
	}

	/**
	 * A lowercase letter followed by an uppercase one,
	 * both matched in groups.
	 */
	static Pattern lcaseUcase = Pattern.compile("(\\p{Ll})(\\p{Lu})");

	/**
	 * Convert a string from a camel-case "identifier" style to
	 * a human-readable version, by putting spaces between words,
	 * uppercasing the first letter and lowercasing the rest.
	 *
	 * E.g. myCamelCaseString becomes "My camel case string".
	 *
	 * @param camelCaseString a string in camel case, i.e. multiple capitalized
	 *   words glued together.
	 * @return a human-readable version of the input string
	 */
	public static String camelCaseToDisplayable(String camelCaseString) {
		return camelCaseToDisplayable(camelCaseString, false);
	}

	/**
	 * Convert a string from a camel-case "identifier" style to
	 * a human-readable version, by putting spaces between words,
	 * uppercasing the first letter and lowercasing the rest.
	 *
	 * E.g. myCamelCaseString becomes "My camel case string".
	 *
	 * @param camelCaseString a string in camel case, i.e. multiple capitalized
	 *   words glued together.
	 * @param dashesToSpaces if true, also converts dashes to spaces
	 * @return a human-readable version of the input string
	 */
	public static String camelCaseToDisplayable(String camelCaseString, boolean dashesToSpaces) {
		String spaceified = lcaseUcase.matcher(camelCaseString).replaceAll("$1 $2");
		if (dashesToSpaces)
			spaceified = spaceified.replace('-', ' ');
		return capitalizeFirst(spaceified.toLowerCase());
	}

  /**
   * repeatChar - Create a string that consists of [iNumber] times [chThis]
   * 
   * @param iNumber
   * @param chThis
   * @return 
   */
  public static String repeatChar(int iNumber, char chThis) {
    return new String(new char[iNumber]).replace('\0', chThis);
  }
}
