package com.trick2live.parser.rtf.parser.rtf;

import java.util.List;

/**
 * Implemented by classes that receive RTFParser messages.
*/

public interface RTFParserDelegate {

    /** constants representing RTF contexts in which text events may occur */
    public static final int IN_DOCUMENT = 0;
    public static final int IN_FONTTBL = 1;
    public static final int IN_FILETBL = 2;
    public static final int IN_COLORTBL = 3;
    public static final int IN_STYLESHEET = 4;
    public static final int IN_LISTTABLE = 5;
    public static final int IN_STYLE = 6;
    public static final int IN_REVTBL = 7;
    public static final int IN_INFO = 8;
    public static final int IN_PNTEXT = 9;
    public static final String NO_STYLE = new String();
    
    /**
     * Receive a block of text from the RTF document.  The text is
     * in the named style and occurs in <code>context</code.
     *
     * <p>Style is guaranteed to have object identity with one of the
     * styles in the list provided by the styleList message, if that
     * has been called.</p>
     *
     * @param text a <code>String</code> value
     * @param style a <code>String</code> value
     * @param context an <code>int</code> value
     */
    public void text(String text, String style, int context);

    /**
     * Receive a control symbol in a particular context.
     *
     * @param controlSymbol a <code>String</code> value
     * @param context an <code>int</code> value
     */
    public void controlSymbol(String controlSymbol, int context);

    /**
     * Receive a control word in a particular context.  The value, if
     * not provided, will be <code>0</code> as per the RTF spec.
     *
     * @param controlWord a <code>String</code> value
     * @param value an <code>int</code> value
     * @param context an <code>int</code> value
     */
    public void controlWord(String controlWord, int value, int context);

    /**
     * Receive notification about the opening of an RTF group with the
     * specified depth. The depth value is that of the group just opened.
     *
     * @param depth an <code>int</code> value
     */
    public void openGroup(int depth);

    /**
     * Receive notification about the closing of an RTF group with the
     * specified depth.  The depth value is that of the group just closed.
     *
     * @param depth an <code>int</code> value
     */
    public void closeGroup(int depth);

    /**
     * Receive notification about the list of style names defined for the
     * document
     *
     * @param styles a <code>List</code> of <code>String</code> objects.
     */
    public void styleList(List styles);
    
    /**
     * The document parsing has begun.
     */
    public void startDocument();

    /**
     * Parsing is complete.
     */
    public void endDocument();

}
