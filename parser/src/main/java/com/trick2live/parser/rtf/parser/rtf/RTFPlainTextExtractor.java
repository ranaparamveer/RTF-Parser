package com.trick2live.parser.rtf.parser.rtf;



import com.trick2live.parser.rtf.common.Constants;
import com.trick2live.parser.rtf.exception.PlainTextExtractorException;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * An extractor that extracts a plain text from RTF documents.
 */
public class RTFPlainTextExtractor
  implements SpecificPlainTextExtractor,
             RTFParserDelegate {
    private StringWriter buffer = null;
    private boolean inIgnorableDestination;
    private int ignorableDestBraceLevel;
    private int braceLevel;

    public RTFPlainTextExtractor() {
    }

    /**
     * Extracts a plain text from an RTF document.
     *
     * @param input the input stream that supplies an MS Excel document for
     * extraction
     * @param output the writer that will accept the extracted text
     * @param encoding ignored
     * @throws PlainTextExtractorException throwed on exception raised during
     * extracting
     */
    public void extract(InputStream input, Writer output, String encoding)
            throws PlainTextExtractorException {
        // TODO: 'Special' symbols like '(c)', '--' and so on
        braceLevel = 0;
        inIgnorableDestination = false;
        buffer = new StringWriter();
        RTFParser parser = new RTFParser(input);

        parser.setNewLine(Constants.EOL);
        parser.setDelegate(this);
        try {
            parser.parse();
            output.write(buffer.toString());
        } catch (Exception e) {
            throw new PlainTextExtractorException(e);
        }
    }

    private void tryToWriteOutput(String str, int context) {
        if (context == IN_DOCUMENT) {
            if (!inIgnorableDestination) {
                if (buffer != null) {
                    buffer.write(str);
                }
            }
        }
    }

    public void text(String text, String style, int context) {
        tryToWriteOutput(text, context);
    }

    public void controlSymbol(String controlSymbol, int context) {
        if (controlSymbol.equals("\\*")) {
            // Handle ignorable destination: ignore it
            if (inIgnorableDestination) {
                // Do nothing: just continue to ignore
            } else {
                inIgnorableDestination = true;
                ignorableDestBraceLevel = braceLevel;
            }
        }
    }

    public void controlWord(String controlWord, int value, int context) {
        if (controlWord.equals("\\cell")) {
            tryToWriteOutput(" ", context);
        } else if (controlWord.equals("\\row")) {
            tryToWriteOutput(Constants.EOL, context);
        } else if (controlWord.equals("\\object") || controlWord.equals("\\pict")) {
            // Handle object and picture destinations: ignore them
            if (inIgnorableDestination) {
                // Do nothing: just continue to ignore
            } else {
                inIgnorableDestination = true;
                ignorableDestBraceLevel = braceLevel;
            }
        }
    }

    public void openGroup(int depth) {
        braceLevel++;
    }

    public void closeGroup(int depth) {
        braceLevel--;
        if (inIgnorableDestination && braceLevel < ignorableDestBraceLevel) {
            inIgnorableDestination = false;
        }
    }

    public void styleList(List styles) {
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    /**
     * @see com.innoorz.rtf.parser.rtf.SpecificPlainTextExtractor#getUsedEncoding()
     */
    public String getUsedEncoding() {
        return null;
    }
}
