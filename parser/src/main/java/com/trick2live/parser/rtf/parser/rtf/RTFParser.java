package com.trick2live.parser.rtf.parser.rtf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.trick2live.parser.rtf.parser.rtf.RTFParserConstants.*;


/**
 * <p>RTFParser</p>
 */
public class RTFParser implements RTFParserDelegate {

    /* maps windows character sets to java encoding names */
    /* note: sparse array */
    private static final String[] CHARSET_ENCODING_TABLE = new String[255];

    static {
        CHARSET_ENCODING_TABLE[0]   = "Cp1252";   // ANSI
        CHARSET_ENCODING_TABLE[1]   = "Cp1252";   // Default
        CHARSET_ENCODING_TABLE[2]   = "Cp1252";   // Symbol
        CHARSET_ENCODING_TABLE[3]   = null;       // Invalid
        CHARSET_ENCODING_TABLE[77]  = "MacRoman"; // Mac
        CHARSET_ENCODING_TABLE[128] = "MS932";    // Shift JIS
        CHARSET_ENCODING_TABLE[129] = "MS949";    // Hangul
        CHARSET_ENCODING_TABLE[130] = "Johab";    // Johab
        CHARSET_ENCODING_TABLE[134] = "MS936";    // GB2312
        CHARSET_ENCODING_TABLE[136] = "Big5";    // Big5
        CHARSET_ENCODING_TABLE[161] = "Cp1253";   // Greek
        CHARSET_ENCODING_TABLE[162] = "Cp1254";   // Turkish
        CHARSET_ENCODING_TABLE[163] = "Cp1258";   // Vietnamese
        CHARSET_ENCODING_TABLE[177] = "Cp1255";   // Hebrew
        CHARSET_ENCODING_TABLE[178] = "Cp1256";   // Arabic
        CHARSET_ENCODING_TABLE[179] = "Cp1256";   // Arabic Traditional
        CHARSET_ENCODING_TABLE[180] = "Cp1256";   // Arabic User
        CHARSET_ENCODING_TABLE[181] = "Cp1255";   // Hebrew User
        CHARSET_ENCODING_TABLE[186] = "Cp1257";   // Baltic
        CHARSET_ENCODING_TABLE[204] = "Cp866";    // Russian
        CHARSET_ENCODING_TABLE[222] = "MS874";    // Thai
        CHARSET_ENCODING_TABLE[238] = "Cp1250";   // East European
        CHARSET_ENCODING_TABLE[254] = "Cp437";    // PC 437
    }

    /*
    * These next two tables map windows codepages to java encoding names.
    * The codepage ints are too large to do a sparse array, so we have
    * two parallel arrays and do a binary search to find the common offset.
    */
    private static final int[] RTF_CODEPAGE = {
            437,  // United States IBM

            /*  Not supported by JDK 1.3.1
            708,  // Arabic (ASMO 708)
            709,  // Arabic (ASMO 449+, BCON V4)
            710,  // Arabic (transparent Arabic)
            711,  // Arabic (Nafitha Enhanced)
            720,  // Arabic (transparent ASMO)
            */

            819,  // Windows 3.1 (United States and Western Europe)
            850,  // IBM multilingual
            852,  // Eastern European
            860,  // Portuguese
            862,  // Hebrew
            863,  // French Canadian
            864,  // Arabic
            865,  // Norwegian
            866,  // Soviet Union
            874,  // Thai
            932,  // Japanese
            936,  // Simplified Chinese
            949,  // Korean
            950,  // Traditional Chinese
            1250, // Windows 3.1 (Eastern European)
            1251, // Windows 3.1 (Cyrillic)
            1252, // Western European
            1253, // Greek
            1254, // Turkish
            1255, // Hebrew
            1256, // Arabic
            1257, // Baltic
            1258, // Vietnamese
            1361  // Johab
    };

    private static final String[] JAVA_ENCODINGS = {
            "Cp437",  // United States IBM
            /*  Not supported by JDK 1.3.1
            "Cp708",  // Arabic (ASMO 708)
            "Cp709",  // Arabic (ASMO 449+, BCON V4)
            "Cp710",  // Arabic (transparent Arabic)
            "Cp711",  // Arabic (Nafitha Enhanced)
            "Cp720",  // Arabic (transparent ASMO)
            */
            "Cp819",  // Windows 3.1 (United States and Western Europe)
            "Cp850",  // IBM multilingual
            "Cp852",  // Eastern European
            "Cp860",  // Portuguese
            "Cp862",  // Hebrew
            "Cp863",  // French Canadian
            "Cp864",  // Arabic
            "Cp865",  // Norwegian
            "Cp866",  // Soviet Union
            "MS874",  // Thai
            "MS932",  // Japanese
            "MS936",  // Simplified Chinese
            "MS949",  // Korean
            "Big5",  // Traditional Chinese
            "Cp1250", // Windows 3.1 (Eastern European)
            "Cp1251", // Windows 3.1 (Cyrillic)
            "Cp1252", // Western European
            "Cp1253", // Greek
            "Cp1254", // Turkish
            "Cp1255", // Hebrew
            "Cp1256", // Arabic
            "Cp1257", // Baltic
            "Cp1258", // Vietnamese
            "Johab"   // Johab
    };

    /**
     * Searches RTF_CODEPAGE table for the offset of rtfCodepage and returns
     * the corresponding encoding name from the JAVA_ENCODINGS table, or
     * null if none is present.
     * @param rtfCodepage codepage
     * @return String
     */
    private static String getJavaEncoding(int rtfCodepage) {
        int offset = Arrays.binarySearch(RTF_CODEPAGE, rtfCodepage);
        return offset < 0 ? null : JAVA_ENCODINGS[offset];
    }

    /* support for skipping bytes after a unicode character.
    * TODO: handle \bin
    */
    // the default number of bytes to skip after a unicode character
    private static final Integer DEFAULT_SKIP_STATE = 1;
    // the current number of bytes to skip after a unicode character
    private Integer _currentSkipState = DEFAULT_SKIP_STATE;
    // a stack of skip states for bytes following a unicode character
    private final Stack<Integer> _ucSkipStates = new Stack<Integer>();

    // the default encoding for all RTF documents
    private static final String DEFAULT_ENCODING = "Cp1252";
    // the document encoding for this RTF document
    private String _documentEncoding = DEFAULT_ENCODING;

    /* support for parsing the \fonttbl to discover font codes and
    * their assigned encodings
    */
    // this holds the font table key (\fN) while we're waiting for the
    // charset (\fcharsetN) declaration in the font table.
    private int _currentFontValue = 0;
    // this maps font codes (\fN) to the encodings assigned (\fcharsetN)
    // in the fonttbl
    private final Map<Integer, String> _fontEncodingMap = new HashMap<Integer, String>();

    /**
     * support for encoding changes via references to the font table
     */
    // the current text encoding
    private String _currentEncoding = DEFAULT_ENCODING;
    // a stack of text encodings across groups
    private final Stack<String> _fontEncodingStack = new Stack<String>();

    private int _currentStyleValue = 0;
    private final Map<Integer, String> _styleMap = new HashMap<Integer, String>();
    private final Stack<String> _styleStack = new Stack<String>();
    private String _currentStyle = NO_STYLE;

    private int _where = IN_DOCUMENT;

    private int _braceDepth = 0;
    private String _newline;

    // The delegate to which the parser forwards productions.
    // Unless setDelegate is called, this will be the parser
    // itself, which supplies a no-op implementation (see below).
    // this enables us to avoid doing null checks in the delegate
    // calls.

    private RTFParserDelegate _delegate = this;

    public static void main(String args[]) throws ParseException {
        //RTFParser parser = RTFParser.createParser(new InputStreamReader(System.in));
        RTFParser parser = null;
        try {
            parser = RTFParser.createParser(new InputStreamReader(new FileInputStream(new File(args[0]))));
            parser.parse();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void reinitialize(Reader reader) {
        ReInit(reader);
    }

    public static RTFParser createParser(Reader reader) {
        return new RTFParser(reader);
    }

    public void parse() throws ParseException {
        try {
            document();
        } catch (UnsupportedEncodingException uee) {
            throw new ParseException("Could not decode bytes in encoding: " +
                    uee.getMessage());
        }
    }

    public void setDelegate(RTFParserDelegate delegate) {
        _delegate = delegate;
    }

    public String getNewLine() {
        return _newline;
    }

    public void setNewLine(String newline) {
        _newline = newline;
    }

    /**
     * Returns a numbered font which supports the encoding.
     * This data is gleaned from the RTF fonttbl, and so
     * is not available until after the fonttbl has been
     * parsed.  No guarantees are made about which font
     * will be returned if multiple fonts support the
     * encoding.
     *
     * @param encoding input encoding
     * @return a font control word value.
     */
    public int getFontForEncoding(String encoding) {
        for (Map.Entry<Integer, String> entry : _fontEncodingMap.entrySet()) {
            if (entry.getValue().equals(encoding)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    // no-op implementation of RTFParserDelegate interface, for cases
    // when delegate is not set.
    public void text(String text, String style, int context) {
        System.out.println(text);
    }

    public void controlSymbol(String controlSymbol, int context) {
    }

    public void controlWord(String controlWord, int value, int context) {
    }

    public void openGroup(int depth) {
    }

    public void closeGroup(int depth) {
    }

    public void styleList(List styles) {
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    private void setCurrentEncoding(String encoding) {
        if (null == encoding) {
            throw new IllegalArgumentException("current encoding cannot be null");
        }
        _currentEncoding = encoding;
    }

    private String getCurrentEncoding() {
        if (_where == IN_DOCUMENT) {
            return _currentEncoding;
        } else {
            return _documentEncoding;
        }
    }

    private String getCurrentStyle() {
        return _currentStyle;
    }

    private void setCurrentStyle(String style) {
        _currentStyle = style;
    }

    private Integer getCurrentSkipState() {
        return _currentSkipState;
    }

    private void setCurrentSkipState(Integer skipState) {
        _currentSkipState = skipState;
    }

    private void setDocumentEncoding(String encoding) {
        if (null == encoding) {
            throw new IllegalArgumentException("document encoding cannot be null");
        }
        _documentEncoding = encoding;
    }

    /**
     * convenience method which downcasts the chars in str to a byte
     * array without attempting to decode them.
     * @param str input String
     * @return byte array
     */
    private byte[] stringToBytes(String str) {
        char[] cbuf = str.toCharArray();
        byte[] buf = new byte[cbuf.length];
        for (int i = 0; i < cbuf.length; i++) {
            buf[i] = (byte) cbuf[i];
        }
        return buf;
    }

    /**
     * Sends the parser delegate a block of unicode text along with
     * the name of the style in which it was found and the location
     * in the document where it occurred.
     * All text encoding is resolved here so the delegate doesn't need
     * to concern itself with the various ways in which RTF encodes
     * non-ASCII strings.
     * @throws java.io.UnsupportedEncodingException exception
     * @throws ParseException exception
     */
    final public void text() throws ParseException, UnsupportedEncodingException {
        StringBuffer buf = new StringBuffer();
        StringBuffer cbuf = new StringBuffer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte b;
        byte[] raw;
        label_1:
        while (true) {
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case U:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                    switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                        case U:
                            u(cbuf);
                            raw = skip_after_unicode();
                            if (raw != null) {
                                cbuf.append(new String(raw, getCurrentEncoding()));
                            }
                            break;
                        case ESCAPED_LBRACE:
                        case ESCAPED_RBRACE:
                        case ESCAPED_BACKSLASH:
                            escaped(cbuf);
                            break;
                        case ESCAPED_NEWLINE:
                        case ESCAPED_CARRIAGE_RETURN:
                        case TAB:
                        case ZWJ:
                        case ZWNJ:
                        case PAR:
                        case LINE:
                        case EMDASH:
                        case ENDASH:
                        case EMSPACE:
                        case ENSPACE:
                        case BULLET:
                        case LQUOTE:
                        case RQUOTE:
                        case LTRMARK:
                        case RTLMARK:
                        case LDBLQUOTE:
                        case RDBLQUOTE:
                            special_character(cbuf);
                            break;
                        case NON_BREAKING_SPACE:
                        case OPTIONAL_HYPHEN:
                        case NON_BREAKING_HYPHEN:
                            textual_control_symbol(cbuf);
                            break;
                        default:
                            jj_la1[0] = jj_gen;
                            consumeToken(-1);
                            throw new ParseException();
                    }
                    if (baos.size() > 0) {
                        buf.append(baos.toString(getCurrentEncoding()));
                        baos.reset();
                    }
                    buf.append(cbuf.toString());
                    cbuf.setLength(0);
                    break;
                case HEX_CHAR:
                    b = hex();
                    baos.write(b);
                    break;
                case TEXT:
                    raw = raw_text();
                    baos.write(raw, 0, raw.length);
                    break;
                default:
                    jj_la1[1] = jj_gen;
                    consumeToken(-1);
                    throw new ParseException();
            }
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case TEXT:
                case HEX_CHAR:
                case U:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                    break;
                default:
                    jj_la1[2] = jj_gen;
                    break label_1;
            }
        }
        if (baos.size() > 0) {
            buf.append(baos.toString(getCurrentEncoding()));
            baos.reset();
        }
        if (_where == IN_STYLESHEET) {
            _styleMap.put(_currentStyleValue, buf.toString());
        }
        _delegate.text(buf.toString(), getCurrentStyle(), _where);
    }

    final public byte[] raw_text() throws ParseException, UnsupportedEncodingException {
        Token tok = consumeToken(TEXT);
        return stringToBytes(tok.image);
    }

    final public void escaped(StringBuffer buf) throws ParseException {
        Token tok;
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case ESCAPED_BACKSLASH:
                tok = consumeToken(ESCAPED_BACKSLASH);
                break;
            case ESCAPED_LBRACE:
                tok = consumeToken(ESCAPED_LBRACE);
                break;
            case ESCAPED_RBRACE:
                tok = consumeToken(ESCAPED_RBRACE);
                break;
            default:
                jj_la1[3] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
        buf.append(tok.image.charAt(0));
    }

    final public void textual_control_symbol(StringBuffer buf) throws ParseException {
        Token tok;
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case NON_BREAKING_SPACE:
                tok = consumeToken(NON_BREAKING_SPACE);
                break;
            case OPTIONAL_HYPHEN:
                tok = consumeToken(OPTIONAL_HYPHEN);
                break;
            case NON_BREAKING_HYPHEN:
                tok = consumeToken(NON_BREAKING_HYPHEN);
                break;
            default:
                jj_la1[4] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
        buf.append(tok.image);
    }

    final public byte hex() throws ParseException {
        Token hex = consumeToken(HEX_CHAR);
        return (byte) Integer.parseInt(hex.image.substring(2), 16);
    }

    final public void special_character(StringBuffer buf) throws ParseException {
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case LINE:
                consumeToken(LINE);
                buf.append("\\r");
                break;
            case TAB:
                consumeToken(TAB);
                buf.append("\\t");
                break;
            case EMDASH:
                consumeToken(EMDASH);
                buf.append('\u2014');
                break;
            case ENDASH:
                consumeToken(ENDASH);
                buf.append('\u2013');
                break;
            case EMSPACE:
                consumeToken(EMSPACE);
                buf.append('\u2003');
                break;
            case ENSPACE:
                consumeToken(ENSPACE);
                buf.append(' ');
                break;
            case BULLET:
                consumeToken(BULLET);
                buf.append('\u2022');
                break;
            case LQUOTE:
                consumeToken(LQUOTE);
                buf.append('\u2018');
                break;
            case RQUOTE:
                consumeToken(RQUOTE);
                buf.append('\u2019');
                break;
            case LDBLQUOTE:
                consumeToken(LDBLQUOTE);
                buf.append('\u201c');
                break;
            case RDBLQUOTE:
                consumeToken(RDBLQUOTE);
                buf.append('\u201d');
                break;
            case LTRMARK:
                consumeToken(LTRMARK);
                buf.append('\u200e');
                break;
            case RTLMARK:
                consumeToken(RTLMARK);
                buf.append('\u200f');
                break;
            case ZWJ:
                consumeToken(ZWJ);
                buf.append('\u200d');
                break;
            case ZWNJ:
                consumeToken(ZWNJ);
                buf.append('\u200c');
                break;
            case ESCAPED_NEWLINE:
            case ESCAPED_CARRIAGE_RETURN:
            case PAR:
                switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                    case PAR:
                        consumeToken(PAR);
                        break;
                    case ESCAPED_NEWLINE:
                        consumeToken(ESCAPED_NEWLINE);
                        break;
                    case ESCAPED_CARRIAGE_RETURN:
                        consumeToken(ESCAPED_CARRIAGE_RETURN);
                        break;
                    default:
                        jj_la1[5] = jj_gen;
                        consumeToken(-1);
                        throw new ParseException();
                }
                buf.append(getNewLine());
                break;
            default:
                jj_la1[6] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
    }

    final public void lbrace() throws ParseException {
        consumeToken(LBRACE);
        _fontEncodingStack.push(getCurrentEncoding());
        _ucSkipStates.push(getCurrentSkipState());
        _styleStack.push(getCurrentStyle());
        _delegate.openGroup(++_braceDepth);
    }

    final public void rbrace() throws ParseException {
        consumeToken(RBRACE);
        setCurrentSkipState(_ucSkipStates.pop());
        setCurrentEncoding(_fontEncodingStack.pop());
        setCurrentStyle(_styleStack.pop());
        _delegate.closeGroup(_braceDepth);
        if (1 == --_braceDepth) { // leaving a table
            if (_where == IN_STYLESHEET) {
                _delegate.styleList(new ArrayList(_styleMap.values()));
            }
            _where = IN_DOCUMENT;
        }
    }

    final public void table_declaration() throws ParseException {
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case INFO:
                consumeToken(INFO);
                _where = IN_INFO;
                break;
            case FONTTBL:
                consumeToken(FONTTBL);
                _where = IN_FONTTBL;
                break;
            case COLORTBL:
                consumeToken(COLORTBL);
                _where = IN_COLORTBL;
                break;
            case STYLESHEET:
                consumeToken(STYLESHEET);
                _where = IN_STYLESHEET;
                break;
            case LISTTABLE:
                consumeToken(LISTTABLE);
                _where = IN_LISTTABLE;
                break;
            case REVTBL:
                consumeToken(REVTBL);
                _where = IN_REVTBL;
                break;
            case PNTEXT:
                consumeToken(PNTEXT);
                switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                    case CW_VAL:
                        consumeToken(CW_VAL);
                        break;
                    default:
                        jj_la1[7] = jj_gen;
                }
                _where = IN_PNTEXT;
                break;
            case PNSECLVL:
                consumeToken(PNSECLVL);
                switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                    case CW_VAL:
                        consumeToken(CW_VAL);
                        break;
                    default:
                        jj_la1[8] = jj_gen;
                }
                _where = IN_PNTEXT;
                break;
            default:
                jj_la1[9] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
    }

    final public void control_symbol() throws ParseException {
        Token sym = null;
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case CONTROL_SYM:
                sym = consumeToken(CONTROL_SYM);
                break;
            case IGNORABLE_DESTINATION:
                sym = consumeToken(IGNORABLE_DESTINATION);
                break;
            case FORMULA_CHARACTER:
                sym = consumeToken(FORMULA_CHARACTER);
                break;
            case INDEX_SUBENTRY:
                sym = consumeToken(INDEX_SUBENTRY);
                break;
            default:
                jj_la1[10] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
        _delegate.controlSymbol(sym.image, _where);
    }

    final public Token mixed_case_control_word() throws ParseException {
        Token word = null;
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case CLFITTEXT:
                word = consumeToken(CLFITTEXT);
                break;
            case CLFTSWIDTH:
                word = consumeToken(CLFTSWIDTH);
                break;
            case CLNOWRAP:
                word = consumeToken(CLNOWRAP);
                break;
            case CLWWIDTH:
                word = consumeToken(CLWWIDTH);
                break;
            case TDFRMTXTBOTTOM:
                word = consumeToken(TDFRMTXTBOTTOM);
                break;
            case TDFRMTXTLEFT:
                word = consumeToken(TDFRMTXTLEFT);
                break;
            case TDFRMTXTRIGHT:
                word = consumeToken(TDFRMTXTRIGHT);
                break;
            case TDFRMTXTTOP:
                word = consumeToken(TDFRMTXTTOP);
                break;
            case TRFTSWIDTHA:
                word = consumeToken(TRFTSWIDTHA);
                break;
            case TRFTSWIDTHB:
                word = consumeToken(TRFTSWIDTHB);
                break;
            case TRFTSWIDTH:
                word = consumeToken(TRFTSWIDTH);
                break;
            case TRWWIDTHA:
                word = consumeToken(TRWWIDTHA);
                break;
            case TRWWIDTHB:
                word = consumeToken(TRWWIDTHB);
                break;
            case TRWWIDTH:
                word = consumeToken(TRWWIDTH);
                break;
            case SECTSPECIFYGENN:
                word = consumeToken(SECTSPECIFYGENN);
                break;
            default:
                jj_la1[11] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
        return word;
    }

    final public void control_word() throws ParseException {
        Token word = null, val = null;
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case CONTROL_WORD:
                word = consumeToken(CONTROL_WORD);
                break;
            case CLFITTEXT:
            case CLFTSWIDTH:
            case CLNOWRAP:
            case CLWWIDTH:
            case TDFRMTXTBOTTOM:
            case TDFRMTXTLEFT:
            case TDFRMTXTRIGHT:
            case TDFRMTXTTOP:
            case TRFTSWIDTHA:
            case TRFTSWIDTHB:
            case TRFTSWIDTH:
            case TRWWIDTHA:
            case TRWWIDTHB:
            case TRWWIDTH:
            case SECTSPECIFYGENN:
                word = mixed_case_control_word();
                break;
            default:
                jj_la1[12] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case CW_VAL: val = consumeToken(CW_VAL); break;
            default: jj_la1[13] = jj_gen;
        }
        int v = null == val ? 0 : Integer.parseInt(val.image);
        _delegate.controlWord(word.image, v, _where);
    }

    final public void u(StringBuffer buf) throws ParseException {
        consumeToken(U);
        Token val = consumeToken(CW_VAL);
        int ucValue = Integer.parseInt(val.image);
        // correct RTF negative unicode char value
        if (ucValue < 0) {
            ucValue += 65536;
        }
        buf.append((char) ucValue);
    }

    byte[] skip_after_unicode() throws ParseException, UnsupportedEncodingException {
        Token tok;
        byte[] raw = null;

        for (int skip = getCurrentSkipState().intValue(); skip != 0; skip--) {
            tok = getNextToken();
            switch (tok.kind) {
                case HEX_CHAR:
                    break; // buh bye!
                case TEXT:
                    if (tok.image.length() > skip) {
                        byte[] tmp = stringToBytes(tok.image);
                        raw = new byte[tmp.length - skip];
                        System.arraycopy(tmp, skip, raw, 0, raw.length);
                        return raw;
                    }
                    break; // the text was exactly what we needed: buh bye!
                default:
                    throw new IllegalStateException("unexpected token while skipping");
            }
        }
        return raw;
    }

    final public void uc() throws ParseException {
        Token word = consumeToken(UC), val = consumeToken(CW_VAL);
        int bytesToSkip = null == val ? 0 : Integer.parseInt(val.image);
        setCurrentSkipState(bytesToSkip);
    }

    final public void fcharset() throws ParseException {
        Token word = consumeToken(FCHARSET), val = consumeToken(CW_VAL);
        int charset = null == val ? 0 : Integer.parseInt(val.image);
        if (IN_FONTTBL == _where) {
            // Modified: always use _documentEncoding
            _fontEncodingMap.put(_currentFontValue,
                                 /*CHARSET_ENCODING_TABLE[charset]*/_documentEncoding);
        } else {
            // this shouldn't happen -- forward onto delegate?
        }
    }

    final public void deff() throws ParseException {
        consumeToken(DEFF);
        consumeToken(CW_VAL);
    }

    final public void f() throws ParseException {
        consumeToken(F);
        Token val = consumeToken(CW_VAL);
        int font = null == val ? 0 : Integer.parseInt(val.image);
        if (IN_FONTTBL == _where) {
            _currentFontValue = font;
        } else if (IN_DOCUMENT == _where) {
            String encoding = (String) _fontEncodingMap.get(font);
            setCurrentEncoding(null == encoding ? DEFAULT_ENCODING : encoding);
        } else {
            // consume this font event
        }
    }

    final public void cs() throws ParseException {
        consumeToken(CS);
        Token val = consumeToken(CW_VAL);
        int style = null == val ? 0 : Integer.parseInt(val.image);
        if (IN_STYLESHEET == _where) {
            _currentStyleValue = style;
        } else if (IN_DOCUMENT == _where) {
            setCurrentStyle((String) _styleMap.get(style));
        } else {
            // consume this style event
        }
    }

    final public void plain() throws ParseException {
        consumeToken(PLAIN);
        setCurrentStyle(NO_STYLE);
    }

    /** these productions identify the document encoding;
     * note that they are almost always clobbered by an \ansicpg
     * or by unicode characters
     * @throws ParseException when parsing ended unexpectedly
     */
    final public void document_charset() throws ParseException {
        switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
            case PC:
                consumeToken(PC);
                setDocumentEncoding(getJavaEncoding(437));
                break;
            case PCA:
                consumeToken(PCA);
                setDocumentEncoding(getJavaEncoding(850));
                break;
            case MAC:
                consumeToken(MAC);
                setDocumentEncoding("MacRoman");
                break;
            case ANSI:
                consumeToken(ANSI);
                setDocumentEncoding(getJavaEncoding(1252));
                break;
            default:
                jj_la1[14] = jj_gen;
                consumeToken(-1);
                throw new ParseException();
        }
    }

    /** specifies the ANSI codepage to use as the document's encoding.
     * Subject to local overrides.
     * @throws ParseException when parsing ended unexpectedly
     */
    final public void ansicpg() throws ParseException {
        consumeToken(ANSICPG);
        Token val = consumeToken(CW_VAL);
        // must be a value in the map - we should throw if it isn't there.
        int cp = null == val ? 0 : Integer.parseInt(val.image);
        setDocumentEncoding(getJavaEncoding(cp));
        setCurrentEncoding(getJavaEncoding(cp)); /* Modified: added this line */

    }

    // TODO: consider collecting special characters in a buffer
    final public void group() throws ParseException, UnsupportedEncodingException {
        lbrace();
        label_2:
        while (true) {
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case INFO:
                case REVTBL:
                case PNTEXT:
                case FONTTBL:
                case COLORTBL:
                case PNSECLVL:
                case LISTTABLE:
                case STYLESHEET:
                    table_declaration();
                    break;
                case UC:
                    uc();
                    break;
                case F:
                    f();
                    break;
                case FCHARSET:
                    fcharset();
                    break;
                case CS:
                    cs();
                    break;
                case PLAIN:
                    plain();
                    break;
                case CLFITTEXT:
                case CLFTSWIDTH:
                case CLNOWRAP:
                case CLWWIDTH:
                case TDFRMTXTBOTTOM:
                case TDFRMTXTLEFT:
                case TDFRMTXTRIGHT:
                case TDFRMTXTTOP:
                case TRFTSWIDTHA:
                case TRFTSWIDTHB:
                case TRFTSWIDTH:
                case TRWWIDTHA:
                case TRWWIDTHB:
                case TRWWIDTH:
                case SECTSPECIFYGENN:
                case CONTROL_WORD:
                    control_word();
                    break;
                case IGNORABLE_DESTINATION:
                case FORMULA_CHARACTER:
                case INDEX_SUBENTRY:
                case CONTROL_SYM:
                    control_symbol();
                    break;
                case LBRACE:
                    group();
                    break;
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case TEXT:
                case HEX_CHAR:
                case U:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                    text();
                    break;
                default:
                    jj_la1[15] = jj_gen;
                    consumeToken(-1);
                    throw new ParseException();
            }
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case LBRACE:
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case IGNORABLE_DESTINATION:
                case FORMULA_CHARACTER:
                case INDEX_SUBENTRY:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case CONTROL_SYM:
                case TEXT:
                case HEX_CHAR:
                case U:
                case UC:
                case F:
                case CS:
                case FCHARSET:
                case PLAIN:
                case INFO:
                case REVTBL:
                case PNTEXT:
                case FONTTBL:
                case COLORTBL:
                case PNSECLVL:
                case LISTTABLE:
                case STYLESHEET:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                case CLFITTEXT:
                case CLFTSWIDTH:
                case CLNOWRAP:
                case CLWWIDTH:
                case TDFRMTXTBOTTOM:
                case TDFRMTXTLEFT:
                case TDFRMTXTRIGHT:
                case TDFRMTXTTOP:
                case TRFTSWIDTHA:
                case TRFTSWIDTHB:
                case TRFTSWIDTH:
                case TRWWIDTHA:
                case TRWWIDTHB:
                case TRWWIDTH:
                case SECTSPECIFYGENN:
                case CONTROL_WORD:
                    break;
                default:
                    jj_la1[16] = jj_gen;
                    break label_2;
            }
        }
        rbrace();
    }

    final public void document() throws ParseException, UnsupportedEncodingException {
        _delegate.startDocument();
        lbrace();
        consumeToken(RTF);
        consumeToken(CW_VAL);
        document_charset();
        label_3:
        while (true) {
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case UC:
                case ANSICPG:
                case DEFF:
                    break;
                default:
                    jj_la1[17] = jj_gen;
                    break label_3;
            }
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case UC:
                    uc();
                    break;
                case ANSICPG:
                    ansicpg();
                    break;
                case DEFF:
                    deff();
                    break;
                default:
                    jj_la1[18] = jj_gen;
                    consumeToken(-1);
                    throw new ParseException();
            }
        }
        label_4:
        while (true) {
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case UC:
                    uc();
                    break;
                case F:
                    f();
                    break;
                case CS:
                    cs();
                    break;
                case PLAIN:
                    plain();
                    break;
                case CLFITTEXT:
                case CLFTSWIDTH:
                case CLNOWRAP:
                case CLWWIDTH:
                case TDFRMTXTBOTTOM:
                case TDFRMTXTLEFT:
                case TDFRMTXTRIGHT:
                case TDFRMTXTTOP:
                case TRFTSWIDTHA:
                case TRFTSWIDTHB:
                case TRFTSWIDTH:
                case TRWWIDTHA:
                case TRWWIDTHB:
                case TRWWIDTH:
                case SECTSPECIFYGENN:
                case CONTROL_WORD:
                    control_word();
                    break;
                case IGNORABLE_DESTINATION:
                case FORMULA_CHARACTER:
                case INDEX_SUBENTRY:
                case CONTROL_SYM:
                    control_symbol();
                    break;
                case LBRACE:
                    group();
                    break;
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case TEXT:
                case HEX_CHAR:
                case U:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                    text();
                    break;
                default:
                    jj_la1[19] = jj_gen;
                    consumeToken(-1);
                    throw new ParseException();
            }
            switch ((jj_ntk == -1) ? nextToken() : jj_ntk) {
                case LBRACE:
                case NON_BREAKING_SPACE:
                case OPTIONAL_HYPHEN:
                case NON_BREAKING_HYPHEN:
                case ESCAPED_NEWLINE:
                case ESCAPED_CARRIAGE_RETURN:
                case IGNORABLE_DESTINATION:
                case FORMULA_CHARACTER:
                case INDEX_SUBENTRY:
                case ESCAPED_LBRACE:
                case ESCAPED_RBRACE:
                case ESCAPED_BACKSLASH:
                case CONTROL_SYM:
                case TEXT:
                case HEX_CHAR:
                case U:
                case UC:
                case F:
                case CS:
                case PLAIN:
                case TAB:
                case ZWJ:
                case ZWNJ:
                case PAR:
                case LINE:
                case EMDASH:
                case ENDASH:
                case EMSPACE:
                case ENSPACE:
                case BULLET:
                case LQUOTE:
                case RQUOTE:
                case LTRMARK:
                case RTLMARK:
                case LDBLQUOTE:
                case RDBLQUOTE:
                case CLFITTEXT:
                case CLFTSWIDTH:
                case CLNOWRAP:
                case CLWWIDTH:
                case TDFRMTXTBOTTOM:
                case TDFRMTXTLEFT:
                case TDFRMTXTRIGHT:
                case TDFRMTXTTOP:
                case TRFTSWIDTHA:
                case TRFTSWIDTHB:
                case TRFTSWIDTH:
                case TRWWIDTHA:
                case TRWWIDTHB:
                case TRWWIDTH:
                case SECTSPECIFYGENN:
                case CONTROL_WORD:
                    break;
                default:
                    jj_la1[20] = jj_gen;
                    break label_4;
            }
        }
        rbrace();
        _delegate.endDocument();
    }

    /**
     * Generated Token Manager.
     */
    public RTFParserTokenManager token_source;
    SimpleCharStream inputStream;
    /**
     * Current token.
     */
    public Token token;

    /**
     * Next token.
     */
    public Token jj_nt;
    private int jj_ntk;
    private int jj_gen;
    final private int[] jj_la1 = new int[21];
    static private int[] jj_la1_0;
    static private int[] jj_la1_1;
    static private int[] jj_la1_2;

    static {
        jj_la1_init_0();
        jj_la1_init_1();
        jj_la1_init_2();
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[]{0x8071f00, 0x8571f00, 0x8571f00, 0x70000, 0x700, 0x1800, 0x1800, 0x0, 0x0, 0x0, 0x8e000, 0x0, 0x0, 0x0, 0x0, 0xf85fff40, 0xf85fff40, 0x10000000, 0x10000000, 0x785fff40, 0x785fff40,};
    }

    private static void jj_la1_init_1() {
        jj_la1_1 = new int[]{0xffff0000, 0xffff0000, 0xffff0000, 0x0, 0x0, 0x80000, 0xffff0000, 0x0, 0x0, 0xff00, 0x0, 0x0, 0x0, 0x0, 0x2e, 0xffffff01, 0xffffff01, 0xc0, 0xc0, 0xffff0001, 0xffff0001,};
    }

    private static void jj_la1_init_2() {
        jj_la1_2 = new int[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x40000, 0x40000, 0x0, 0x0, 0x7fff, 0x17fff, 0x40000, 0x0, 0x17fff, 0x17fff, 0x0, 0x0, 0x17fff, 0x17fff,};
    }

    /**
     * Constructor with InputStream.
     * @param stream input stream
     */
    public RTFParser(InputStream stream) {
        this(stream, null);
    }

    /**
     * Constructor with InputStream and supplied encoding
     * @param stream input stream
     * @param encoding input stream encoding
     */
    public RTFParser(InputStream stream, String encoding) {
        try {
            inputStream = new SimpleCharStream(stream, encoding, 1, 1);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source = new RTFParserTokenManager(inputStream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    /**
     * Reinitialise.
     * @param stream input stream
     */
    public void ReInit(InputStream stream) {
        ReInit(stream, null);
    }

    /**
     * Reinitialise.
     * @param stream input stream
     * @param encoding input stream encoding
     */
    public void ReInit(InputStream stream, String encoding) {
        try {
            inputStream.ReInit(stream, encoding, 1, 1);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source.ReInit(inputStream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    /**
     * Constructor.
     * @param stream Reader
     */
    public RTFParser(Reader stream) {
        inputStream = new SimpleCharStream(stream, 1, 1);
        token_source = new RTFParserTokenManager(inputStream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    /**
     * Reinitialise.
     * @param stream Reader
     */
    public void ReInit(Reader stream) {
        inputStream.ReInit(stream, 1, 1);
        token_source.ReInit(inputStream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    /**
     * Constructor with generated Token Manager.
     * @param tm RTFParserTokenManager
     */
    public RTFParser(RTFParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    /**
     * Reinitialise.
     * @param tm RTFParserTokenManager
     */
    public void ReInit(RTFParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 21; i++) jj_la1[i] = -1;
    }

    private Token consumeToken(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) token = token.next;
        else token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }


    /**
     * Get the next Token.
     * @return Token
     */
    final public Token getNextToken() {
        if (token.next != null) token = token.next;
        else token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /**
     * Get the specific Token.
     * @param index idx
     * @return Token
     */
    final public Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) t = t.next;
            else t = t.next = token_source.getNextToken();
        }
        return t;
    }

    private int nextToken() {
        if ((jj_nt = token.next) == null)
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        else
            return (jj_ntk = jj_nt.kind);
    }

    private java.util.List<int[]> expeсtedEntries = new java.util.ArrayList<int[]>();
    private int[] expectedEntry;
    private int jj_kind = -1;

    /**
     * Generate ParseException.
     * @return ParseException
     */
    public ParseException generateParseException() {
        expeсtedEntries.clear();
        boolean[] la1tokens = new boolean[84];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 21; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                    if ((jj_la1_2[i] & (1 << j)) != 0) {
                        la1tokens[64 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 84; i++) {
            if (la1tokens[i]) {
                expectedEntry = new int[1];
                expectedEntry[0] = i;
                expeсtedEntries.add(expectedEntry);
            }
        }
        int[][] exptokseq = new int[expeсtedEntries.size()][];
        for (int i = 0; i < expeсtedEntries.size(); i++) {
            exptokseq[i] = expeсtedEntries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

}
