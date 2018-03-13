package com.trick2live.parser.rtf.parser.rtf;

/**
 * Token literal values and constants.
 */
public interface RTFParserConstants {

  /** End of File. */
  int EOF = 0;

  int BACKSLASH = 1;
  int HEX_ESCAPE = 2;
  int LBRACE = 6;
  int RBRACE = 7;
  int NON_BREAKING_SPACE = 8;
  int OPTIONAL_HYPHEN = 9;
  int NON_BREAKING_HYPHEN = 10;
  int ESCAPED_NEWLINE = 11;
  int ESCAPED_CARRIAGE_RETURN = 12;
  int IGNORABLE_DESTINATION = 13;
  int FORMULA_CHARACTER = 14;
  int INDEX_SUBENTRY = 15;
  int ESCAPED_LBRACE = 16;
  int ESCAPED_RBRACE = 17;
  int ESCAPED_BACKSLASH = 18;
  int CONTROL_SYM = 19;
  int TEXT = 20;
  int HEX_DIGIT = 21;
  int HEX_CHAR = 22;
  int U = 27;
  int UC = 28;
  int F = 29;
  int CS = 30;
  int FCHARSET = 31;
  int PLAIN = 32;
  int PC = 33;
  int PCA = 34;
  int MAC = 35;
  int RTF = 36;
  int ANSI = 37;
  int ANSICPG = 38;
  int DEFF = 39;
  int INFO = 40;
  int REVTBL = 41;
  int PNTEXT = 42;
  int FONTTBL = 43;
  int COLORTBL = 44;
  int PNSECLVL = 45;
  int LISTTABLE = 46;
  int STYLESHEET = 47;
  int TAB = 48;
  int ZWJ = 49;
  int ZWNJ = 50;
  int PAR = 51;
  int LINE = 52;
  int EMDASH = 53;
  int ENDASH = 54;
  int EMSPACE = 55;
  int ENSPACE = 56;
  int BULLET = 57;
  int LQUOTE = 58;
  int RQUOTE = 59;
  int LTRMARK = 60;
  int RTLMARK = 61;
  int LDBLQUOTE = 62;
  int RDBLQUOTE = 63;
  int CLFITTEXT = 64;
  int CLFTSWIDTH = 65;
  int CLNOWRAP = 66;
  int CLWWIDTH = 67;
  int TDFRMTXTBOTTOM = 68;
  int TDFRMTXTLEFT = 69;
  int TDFRMTXTRIGHT = 70;
  int TDFRMTXTTOP = 71;
  int TRFTSWIDTHA = 72;
  int TRFTSWIDTHB = 73;
  int TRFTSWIDTH = 74;
  int TRWWIDTHA = 75;
  int TRWWIDTHB = 76;
  int TRWWIDTH = 77;
  int SECTSPECIFYGENN = 78;
  int LC_LETTER = 79;
  int CONTROL_WORD = 80;
  int DIGIT = 81;
  int CW_VAL = 82;

  int CONTROL = 0;
  int HEX = 1;
  int DEFAULT = 2;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\"\\\\\"",
    "\"\\\\\\\'\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\t\"",
    "\"{\"",
    "\"}\"",
    "\"\\\\~\"",
    "\"\\\\-\"",
    "\"\\\\_\"",
    "\"\\\\\\n\"",
    "\"\\\\\\r\"",
    "\"\\\\*\"",
    "\"\\\\|\"",
    "\"\\\\:\"",
    "\"\\\\{\"",
    "\"\\\\}\"",
    "\"\\\\\\\\\"",
    "<CONTROL_SYM>",
    "<TEXT>",
    "<HEX_DIGIT>",
    "<HEX_CHAR>",
    "\" \"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\t\"",
    "\"u\"",
    "\"uc\"",
    "\"f\"",
    "\"cs\"",
    "\"fcharset\"",
    "\"plain\"",
    "\"pc\"",
    "\"pca\"",
    "\"mac\"",
    "\"rtf\"",
    "\"ansi\"",
    "\"ansicpg\"",
    "\"deff\"",
    "\"info\"",
    "\"revtbl\"",
    "\"pntext\"",
    "\"fonttbl\"",
    "\"colortbl\"",
    "\"pnseclvl\"",
    "\"listtable\"",
    "\"stylesheet\"",
    "\"tab\"",
    "\"zwj\"",
    "\"zwnj\"",
    "\"par\"",
    "\"line\"",
    "\"emdash\"",
    "\"endash\"",
    "\"emspace\"",
    "\"enspace\"",
    "\"bullet\"",
    "\"lquote\"",
    "\"rquote\"",
    "\"ltrmark\"",
    "\"rtlmark\"",
    "\"ldblquote\"",
    "\"rdblquote\"",
    "\"clFitText\"",
    "\"clftsWidth\"",
    "\"clNoWrap\"",
    "\"clwWidth\"",
    "\"tdfrmtxtBottom\"",
    "\"tdfrmtxtLeft\"",
    "\"tdfrmtxtRight\"",
    "\"tdfrmtxtTop\"",
    "\"trftsWidthA\"",
    "\"trftsWidthB\"",
    "\"trftsWidth\"",
    "\"trwWidthA\"",
    "\"trwWidthB\"",
    "\"trwWidth\"",
    "\"sectspecifygenN\"",
    "<LC_LETTER>",
    "<CONTROL_WORD>",
    "<DIGIT>",
    "<CW_VAL>",
    "<token of kind 83>",
  };

}
