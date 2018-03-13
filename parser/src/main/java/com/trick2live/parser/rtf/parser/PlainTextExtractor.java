package com.trick2live.parser.rtf.parser;


import com.trick2live.parser.rtf.common.Constants;
import com.trick2live.parser.rtf.exception.PlainTextExtractorException;
import com.trick2live.parser.rtf.exception.UnsupportedMimeTypeException;
import com.trick2live.parser.rtf.parser.rtf.SpecificPlainTextExtractor;
import com.trick2live.parser.rtf.parser.rtf.RTFPlainTextExtractor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * <p>
 * Used to extract a plain text from formatted documents.
 * </p>
 * <p>
 * To use, first instantiate with <code>new PlainTextExtractor()</code>, then
 * call the <code>extract</code> method.
 * </p>
 * <p>
 * One common ruleset is applied to figure out encoding to use during extracting
 * (first matched rule is used to obtain encoding):
 * <ol>
 * <li>
 * If encoding doesn't make sense for this format, it's totally ignored.
 * </li>
 * <li>
 * If extractor finds out the encoding from document by itself (for example,
 * HTML files often contain such information), it uses it.
 * </li>
 * <li>
 * If encoding is given to extractor as a parameter to <code>extract</code>
 * method, it's used.
 * </li>
 * <li>
 * Otherwise default encoding is used (it's currently
 * <code>Constants.DEFAULT_ENCODING</code>)
 * </li>
 * </ol>
 * </p>
 */
public class PlainTextExtractor {

    /*
     * Internal field used to store encoding that was used by extractor during
     * extracting process
     */
    protected String usedEncoding = null;

    /**
     * Constructs new PlainTextExtractor instance
     */
    public PlainTextExtractor() {}

    /**
     * Extracts a plain text from a formatted document to a given writer.
     *
     * @param input the stream that supplies the document
     * @param mimeType the mime type of the document
     * @param output the writer which will accept the extracted text
     * @param encoding the encoding of the document in the stream. If the
     * <code>encoding</code> is <code>null</code>, then the extractor uses
     * its default encoding (currently all extractors use
     * <code>Constants.DEFAULT_ENCODING</code>).
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public void extract(InputStream input,
                        String mimeType,
                        Writer output,
                        String encoding)
    throws UnsupportedMimeTypeException,
            PlainTextExtractorException
    {
        SpecificPlainTextExtractor extractor;

        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType parameter is null");
        }
        if (mimeType.equals("application/rtf")) {
            extractor = new RTFPlainTextExtractor();
        } else {
            throw new UnsupportedMimeTypeException("This mimeType is not supported: " + mimeType);
        }
        extractor.extract(input, output, encoding);
        usedEncoding = extractor.getUsedEncoding();
    }

    /**
     * Extracts a plain text from a formatted document and returns it as a
     * string.
     *
     * @param input the stream that supplies the document
     * @param mimeType the mime type of the document
     * @param encoding the encoding of the document in the stream. If the
     * <code>encoding</code> is <code>null</code>, then the extractor uses
     * its default encoding (currently all extractors use
     * <code>Constants.DEFAULT_ENCODING</code>).
     * @return the extracted text as a string
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public String extract(InputStream input,
                          String mimeType,
                          String encoding)
    throws UnsupportedMimeTypeException,
           PlainTextExtractorException
    {
        StringWriter writer = new StringWriter();
        extract(input, mimeType, writer, encoding);
        return writer.toString();
    }

    /**
     * Extracts a plain text from a formatted document to a given writer. The
     * document is assumed to have the default encoding.
     *
     * @param input the stream that supplies the document
     * @param mimeType the mime type of the document
     * @param output the writer which will accept the extracted text
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public void extract(InputStream input,
                        String mimeType,
                        Writer output)
    throws UnsupportedMimeTypeException,
           PlainTextExtractorException
    {
        extract(input, mimeType, output, null);
    }

    /**
     * Extracts a plain text from a formatted document and returns it as a string.
     * The document is assumed to have the default encoding.
     *
     * @param input the stream that supplies the document
     * @param mimeType the mime type of the document
     * @return the extracted text as a string
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public String extract(InputStream input,
                          String mimeType)
    throws UnsupportedMimeTypeException,
           PlainTextExtractorException
    {
        return extract(input, mimeType, (String)null);
    }

    /**
     * Extracts a plain text from a formatted document to a given writer. The
     * document is given as a <code>String</code>.
     *
     * @param input the string that supplies the document
     * @param mimeType the mime type of the document
     * @param output the writer which will accept the extracted text
     * @param encoding the encoding of the document in the stream. If the
     * <code>encoding</code> is <code>null</code>, then the extractor uses
     * its default encoding (currently all extractors use
     * <code>Constants.DEFAULT_ENCODING</code>). Also, that encoding is used to
     * convert an input string to a byte stream (if not given, it is again
     * assumed to be <code>Constants.DEFAULT_ENCODING</code>).
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public void extract(String input, String mimeType, Writer output,
                        String encoding) throws UnsupportedMimeTypeException,
            PlainTextExtractorException {
        try {
            extract(stringToInputStream(input, encoding), mimeType,
                    output, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new PlainTextExtractorException(e);
        }
    }

    /**
     * Extracts a plain text from a formatted document and returns it as a
     * string. The document is given as a <code>String</code>.
     *
     * @param input the string that supplies the document
     * @param mimeType the mime type of the document
     * @param encoding the encoding of the document in the stream. If the
     * <code>encoding</code> is <code>null</code>, then the extractor uses
     * its default encoding (currently all extractors use
     * <code>Constants.DEFAULT_ENCODING</code>). Also, that encoding is used to
     * convert an input string to a byte stream (if not given, it is again
     * assumed to be <code>Constants.DEFAULT_ENCODING</code>).
     * @return the extracted text as a string
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public String extract(String input, String mimeType, String encoding)
            throws UnsupportedMimeTypeException, PlainTextExtractorException {
        try {
            return extract(stringToInputStream(input, encoding), mimeType,
                           encoding);
        } catch (UnsupportedEncodingException e) {
            throw new PlainTextExtractorException(e);
        }
    }

    /**
     * Extracts a plain text from a formatted document to a given writer. The
     * document is assumed to have the default encoding. The document is given
     * as a <code>String</code>, which is decoded to bytes using default
     * encoding too.
     *
     * @param input the string that supplies the document
     * @param mimeType the mime type of the document
     * @param output the writer which will accept the extracted text
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public void extract(String input, String mimeType, Writer output)
            throws UnsupportedMimeTypeException, PlainTextExtractorException {
        extract(input, mimeType, output, null);
    }

    /**
     * Extracts a plain text from a formatted document and returns it as a string.
     * The document is assumed to have the default encoding. The document
     * is given as a <code>String</code>, which is decoded to bytes using
     * default encoding too.
     *
     * @param input the string that supplies the document
     * @param mimeType the mime type of the document
     * @return the extracted text as a string
     * @throws UnsupportedMimeTypeException throwed when a given mime type is
     * not supported
     * @throws PlainTextExtractorException any other exception raised during
     * extracting
     */
    public String extract(String input, String mimeType)
            throws UnsupportedMimeTypeException, PlainTextExtractorException {
        return extract(input, mimeType, (String) null);
    }

    /**
     * <p>
     * Returns encoding that was used for extracting. If encoding has no sense
     * for particular document format or it's unknown for extractor, returns
     * <code>null</code>.
     * </p>
     * <p>
     * This method should be called after calling <code>extract</code>; before
     * it this method may return anything.
     * </p>
     *
     * @return encoding used or <code>null</code>
     */
    public String getUsedEncoding() {
        return usedEncoding;
    }

    /**
     * Converts a <code>String</code> to an <code>InputStream</code> using given
     * <code>encoding</code>. If the <code>encoding</code> is <code>null</code>,
     * it's assumed to be a default encoding
     * (<code>Constants.DEFAULT_ENCODING</code>).
     * @param input the string to be converted
     * @param encoding the encoding
     * @return an InputStream that supplies bytes from the <code>string</code>
     * @throws UnsupportedEncodingException if encoding is not supported
     */
    protected InputStream stringToInputStream(String input, String encoding)
            throws UnsupportedEncodingException {
        if (encoding == null || encoding.trim().length() == 0) {
            encoding = Constants.DEFAULT_ENCODING;
        }
        return new ByteArrayInputStream(input.getBytes(encoding));
    }
}
