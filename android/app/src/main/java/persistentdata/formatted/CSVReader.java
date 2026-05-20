package persistentdata.formatted;

import persistentdata.PersistentDataException;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

public class CSVReader implements FormattedReader<String[]> {
	private final CSVFormat format;
	private final Reader reader;

	public CSVReader(CSVFormat format, Reader reader) {
		this.format = format;
		this.reader = reader;
	}
	
	private boolean eof = false;
	public boolean hasNext() {
		return true;
	}

	// These format strings are provided to give you some ideas about what error cases might be encountered.
	private static final String LINE_TOO_SHORT_MESSAGE = "Line was too short: expected %s fields but found %s";
	private static final String LINE_TOO_LONG_MESSAGE = "Line was too long: expected %s fields";
	private static final String IMPROPER_ESCAPE_MESSAGE = "EOF reached unexpectedly while escaped";
	private static final String REACHED_EOF_MESSAGE = "Already reached end of file while reading";

	public String[] getNext() {
		if (format.COLUMN_COUNT == 0) {
			throw new CSVIOException(String.format(Locale.ROOT, LINE_TOO_LONG_MESSAGE, 0));
		}

		if (eof) {
			throw new CSVIOException(REACHED_EOF_MESSAGE);
		}

		StringBuilder result = new StringBuilder();
		boolean inSpecialField = false;
		boolean lastWasEscape = false;
		String[] fields = new String[format.COLUMN_COUNT];
		int fieldIndex = 0;
		boolean ignoredFirstQuote = false;
		while (true) {
			char c = format.LINE_SEPARATOR;
			try {
				int i = reader.read();
				if (i == -1) {
					if (inSpecialField) {
						throw new CSVIOException(IMPROPER_ESCAPE_MESSAGE);
					}
					eof = true;
					reader.close();
				} else {
					c = (char) i;
				}
			} catch (IOException e) {
				throw new CSVIOException(e.getMessage());
			}

			if (c == format.ESCAPE_MARKER) {
				inSpecialField = !inSpecialField;
				if (result.length() == 0 && !ignoredFirstQuote) {
					ignoredFirstQuote = true;
				} else if (lastWasEscape) {
					result.append(c);
					lastWasEscape = false;
				} else {
					lastWasEscape = true;
				}
			} else {
				lastWasEscape = false;
				if (c == format.FIELD_SEPARATOR && !inSpecialField) {
					fields[fieldIndex] = result.toString();
					fieldIndex++;
					if (fieldIndex >= format.COLUMN_COUNT) {
						throw new CSVIOException(String.format(Locale.ROOT, LINE_TOO_LONG_MESSAGE, format.COLUMN_COUNT));
					}
					ignoredFirstQuote = false;
					result = new StringBuilder();
				} else if (c == format.LINE_SEPARATOR && !inSpecialField) {
					fields[fieldIndex] = result.toString();
					fieldIndex++;
					if (fieldIndex < format.COLUMN_COUNT) {
						throw new CSVIOException(String.format(Locale.ROOT, LINE_TOO_SHORT_MESSAGE, format.COLUMN_COUNT, fieldIndex));
					}
					break;
				} else {
					result.append(c);
				}
			}
		}
		return fields;
	}

	public static class CSVIOException extends PersistentDataException {
		public CSVIOException(String message) {
			super(message);
		}
	}
}
