/*
 *  Copyright 2005 Blandware (http://www.blandware.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.trick2live.parser.rtf.parser.rtf;



import com.trick2live.parser.rtf.exception.PlainTextExtractorException;

import java.io.InputStream;
import java.io.Writer;

/**
 * An interface for an extractor which will extract a plain text from the
 * documents of a specific format.
*/
public interface SpecificPlainTextExtractor {
    /**
     * Extracts a plain text from a document.
     *
     * @param input the input stream that supplies a document for extraction
     * @param output the writer that will accept the extracted text
     * @param encoding the encoding of the document in <code>input</code>.
     * Extractor may ignore the <code>encoding</code> if it doesn't make sence
     * in the corresponding format. Otherwise, if it is <code>null</code>, then
     * the extractor will choose the encoding itself. If the
     * <code>encoding</code> make sence and is not <code>null</code>, then the
     * extractor should use it. If the extractor finds out the encoding from
     * document by itself, it uses it and ignores given (or default) encoding.
     * @throws PlainTextExtractorException throwed on exception raised during
     * extracting
     */
    public void extract(InputStream input, Writer output, String encoding)
            throws PlainTextExtractorException;

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
    public String getUsedEncoding();
}
