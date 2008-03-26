/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imapserver.codec.decode.base;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.imapserver.codec.decode.DecoderUtils;
import org.apache.james.imapserver.codec.decode.ImapCommandParser;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.apache.james.imapserver.codec.decode.MessagingImapCommandParser;

/**
 * <p>
 * <strong>Note:</strong> 
 * </p>
 * @version $Revision: 109034 $
 */
public abstract class AbstractImapCommandParser extends AbstractLogEnabled implements ImapCommandParser, MessagingImapCommandParser
{
    private static final int QUOTED_BUFFER_INITIAL_CAPACITY = 64;

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    
    private ImapCommand command;
    private Imap4Rev1MessageFactory messageFactory;
    private StatusResponseFactory statusResponseFactory;
    
    public AbstractImapCommandParser() {
        super();
    }
    
    public ImapCommand getCommand() {
        return command;
    }
    
    protected void setCommand(ImapCommand command) {
        this.command = command;
    }

    /**
     * @see org.apache.james.imapserver.codec.decode.MessagingImapCommandParser#getMessageFactory()
     */
    public Imap4Rev1MessageFactory getMessageFactory() {
        return messageFactory;
    }

    /**
     * @see org.apache.james.imapserver.codec.decode.MessagingImapCommandParser#setMessageFactory(org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory)
     */
    public void setMessageFactory(Imap4Rev1MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    public final StatusResponseFactory getStatusResponseFactory() {
        return statusResponseFactory;
    }

    public final void setStatusResponseFactory(
            StatusResponseFactory statusResponseFactory) {
        this.statusResponseFactory = statusResponseFactory;
    }

    /**
     * Parses a request into a command message
     * for later processing.
     * @param request <code>ImapRequestLineReader</code>, not null
     * @return <code>ImapCommandMessage</code>, not null
     */
    public final ImapMessage parse( ImapRequestLineReader request, String tag ) {
        ImapMessage result;
        try {
            
            ImapMessage message = decode(command, request, tag);
            setupLogger(message);
            result = message;
            
        } catch ( ProtocolException e ) {
            getLogger().debug("error processing command ", e);
            String msg = e.getMessage() + " Command should be '" +
                    command.getExpectedMessage() + "'";
            result = messageFactory.createErrorMessage( msg, tag );
        }
        return result;
    }
    
    /**
     * Parses a request into a command message
     * for later processing.
     * @param request <code>ImapRequestLineReader</code>, not null
     * @param tag TODO
     * @param command <code>ImapCommand</code> to be parsed, not null
     * @return <code>ImapCommandMessage</code>, not null
     * @throws ProtocolException if the request cannot be parsed
     */
    protected abstract ImapMessage decode( ImapCommand command, ImapRequestLineReader request, String tag ) 
        throws ProtocolException;
    
    /**
     * Reads an argument of type "atom" from the request.
     */
    public static String atom( ImapRequestLineReader request ) throws ProtocolException
    {
        return consumeWord( request, new ATOM_CHARValidator() );
    }

    /**
     * Reads a command "tag" from the request.
     */
    public static String tag(ImapRequestLineReader request) throws ProtocolException
    {
        CharacterValidator validator = new TagCharValidator();
        return consumeWord( request, validator );
    }

    /**
     * Reads an argument of type "astring" from the request.
     */
    public String astring(ImapRequestLineReader request) throws ProtocolException
    {
        return astring(request, null);
    }
    
    /**
     * Reads an argument of type "astring" from the request.
     */
    public String astring(ImapRequestLineReader request, Charset charset) throws ProtocolException
    {
        char next = request.nextWordChar();
        switch ( next ) {
            case '"':
                return consumeQuoted( request, charset );
            case '{':
                return consumeLiteral( request, charset );
            default:
                return atom( request );
        }
    }

    /**
     * Reads an argument of type "nstring" from the request.
     */
    public String nstring( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        switch ( next ) {
            case '"':
                return consumeQuoted( request );
            case '{':
                return consumeLiteral( request, null );
            default:
                String value = atom( request );
                if ( "NIL".equals( value ) ) {
                    return null;
                }
                else {
                    throw new ProtocolException( "Invalid nstring value: valid values are '\"...\"', '{12} CRLF *CHAR8', and 'NIL'." );
                }
        }
    }

    /**
     * Reads a "mailbox" argument from the request. Not implemented *exactly* as per spec,
     * since a quoted or literal "inbox" still yeilds "INBOX"
     * (ie still case-insensitive if quoted or literal). I think this makes sense.
     *
     * mailbox         ::= "INBOX" / astring
     *              ;; INBOX is case-insensitive.  All case variants of
     *              ;; INBOX (e.g. "iNbOx") MUST be interpreted as INBOX
     *              ;; not as an astring.
     */
    public String mailbox( ImapRequestLineReader request ) throws ProtocolException
    {
        String mailbox = astring( request );
        if ( mailbox.equalsIgnoreCase( ImapConstants.INBOX_NAME ) ) {
            return ImapConstants.INBOX_NAME;
        }
        else {
            return mailbox;
        }
    }

    /**
     * Reads one <code>date</code> argument from the request.
     * @param request <code>ImapRequestLineReader</code>, not null
     * @return <code>DayMonthYear</code>, not null
     * @throws ProtocolException
     */
    public DayMonthYear date(ImapRequestLineReader request) throws ProtocolException {
        
        final char one = request.consume();
        final char two = request.consume();
        final int day;
        if (two == '-') {
            day = DecoderUtils.decodeFixedDay(' ', one);
        } else {
            day = DecoderUtils.decodeFixedDay(one, two);
            nextIsDash(request);
        }
        
        final char monthFirstChar = request.consume();
        final char monthSecondChar = request.consume();
        final char monthThirdChar = request.consume();
        final int month = DecoderUtils.decodeMonth(monthFirstChar, monthSecondChar, monthThirdChar) + 1;
        nextIsDash(request);
        final char milleniumChar = request.consume();
        final char centuryChar = request.consume();
        final char decadeChar = request.consume();
        final char yearChar = request.consume();
        final int year = DecoderUtils.decodeYear(milleniumChar, centuryChar, decadeChar, yearChar);
        final DayMonthYear result = new DayMonthYear(day, month, year);
        return result;
    }

    private void nextIsDash(ImapRequestLineReader request) throws ProtocolException {
        final char next = request.consume();
        if (next != '-') {
            throw new ProtocolException("Expected dash but was " + next);
        }
    }
    
    /**
     * Reads a "date-time" argument from the request.
     */
    public Date dateTime( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        String dateString;
        if ( next == '"' ) {
            dateString = consumeQuoted( request );
        }
        else {
            throw new ProtocolException( "DateTime values must be quoted." );
        }

        return DecoderUtils.decodeDateTime(dateString);
    }

    /**
     * Reads the next "word from the request, comprising all characters up to the next SPACE.
     * Characters are tested by the supplied CharacterValidator, and an exception is thrown
     * if invalid characters are encountered.
     */
    protected static String consumeWord( ImapRequestLineReader request,
                                  CharacterValidator validator )
            throws ProtocolException
    {
        StringBuffer atom = new StringBuffer();

        char next = request.nextWordChar();
        while( ! isWhitespace( next ) ) {
            if ( validator.isValid( next ) )
            {
                atom.append( next );
                request.consume();
            }
            else {
                throw new ProtocolException( "Invalid character: '" + next + "'" );
            }
            next = request.nextChar();
        }
        return atom.toString();
    }

    private static boolean isWhitespace( char next )
    {
        return ( next == ' ' || next == '\n' || next == '\r' || next == '\t' );
    }

    /**
     * Reads an argument of type "literal" from the request, in the format:
     *      "{" charCount "}" CRLF *CHAR8
     * Note before calling, the request should be positioned so that nextChar
     * is '{'. Leading whitespace is not skipped in this method.
     * @param charset ,
     * or null for <code>US-ASCII</code>
     */
    protected String consumeLiteral( final ImapRequestLineReader request, final Charset charset )
            throws ProtocolException
    {
        if (charset == null) {
            return consumeLiteral(request, US_ASCII);
        } else {
            // The 1st character must be '{'
            consumeChar( request, '{' );
    
            StringBuffer digits = new StringBuffer();
            char next = request.nextChar();
            while ( next != '}' && next != '+' )
            {
                digits.append( next );
                request.consume();
                next = request.nextChar();
            }
    
            // If the number is *not* suffixed with a '+', we *are* using a synchronized literal,
            // and we need to send command continuation request before reading data.
            boolean synchronizedLiteral = true;
            // '+' indicates a non-synchronized literal (no command continuation request)
            if ( next == '+' ) {
                synchronizedLiteral = false;
                consumeChar(request, '+' );
            }
    
            // Consume the '}' and the newline
            consumeChar( request, '}' );
            consumeCRLF( request );
    
            if ( synchronizedLiteral ) {
                request.commandContinuationRequest();
            }
    
            final int size = Integer.parseInt( digits.toString() );
            final byte[] bytes = new byte[size];
            request.read( bytes );
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return decode(charset, buffer);
        }
    }

    private String decode(final Charset charset, final ByteBuffer buffer) throws ProtocolException {
        try {
            
            final String result = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buffer).toString();
            return result;
            
        } catch (IllegalStateException e) {
            throw new ProtocolException ("Bad character encoding", e);
        } catch (MalformedInputException e) {
            throw new ProtocolException ("Bad character encoding", e);
        } catch (UnmappableCharacterException e) {
            throw new ProtocolException ("Bad character encoding", e);
        } catch (CharacterCodingException e) {
            throw new ProtocolException ("Bad character encoding", e);
        }
    }

    /**
     * Consumes a CRLF from the request.
     * TODO we're being liberal, the spec insists on \r\n for new lines.
     * @param request
     * @throws ProtocolException
     */
    private void consumeCRLF( ImapRequestLineReader request )
            throws ProtocolException
    {
        char next = request.nextChar();
        if ( next != '\n' ) {
            consumeChar( request, '\r' );
        }
        consumeChar( request, '\n' );
    }

    /**
     * Consumes the next character in the request, checking that it matches the
     * expected one. This method should be used when the
     */
    protected void consumeChar( ImapRequestLineReader request, char expected )
            throws ProtocolException
    {
        char consumed = request.consume();
        if ( consumed != expected ) {
            throw new ProtocolException( "Expected:'" + expected + "' found:'" + consumed + "'" );
        }
    }

    /**
     * Reads a quoted string value from the request.
     */
    protected String consumeQuoted( ImapRequestLineReader request)
            throws ProtocolException
    { 
        return consumeQuoted(request, null);
    }
    
    /**
     * Reads a quoted string value from the request.
     */
    protected String consumeQuoted( ImapRequestLineReader request, Charset charset )
            throws ProtocolException
    {
        if (charset == null) {
            return consumeQuoted(request, US_ASCII);
        } else {
            // The 1st character must be '"'
            consumeChar(request, '"' );
            final QuotedStringDecoder decoder = new QuotedStringDecoder(charset);
            final String result = decoder.decode(request);
            consumeChar( request, '"' );
            return result;
        }
    }

    /**
     * Reads a base64 argument from the request.
     */
    public byte[] base64( ImapRequestLineReader request ) throws ProtocolException
    {
        // TODO: throw unsupported exception?
        // TODO: log
        return null;
    }

    /**
     * Reads a "flags" argument from the request.
     */
    public Flags flagList( ImapRequestLineReader request ) throws ProtocolException
    {
        Flags flags = new Flags();
        request.nextWordChar();
        consumeChar( request, '(' );
        CharacterValidator validator = new NoopCharValidator();
        String nextWord = consumeWord( request, validator );
        while ( ! nextWord.endsWith(")" ) ) {
            DecoderUtils.setFlag( nextWord, flags );
            nextWord = consumeWord( request, validator );
        }
        // Got the closing ")", may be attached to a word.
        if ( nextWord.length() > 1 ) {
            int parenIndex = nextWord.indexOf(')');
            if (parenIndex > 0) {
                final String nextFlag = nextWord.substring(0, parenIndex );
                DecoderUtils.setFlag( nextFlag, flags );
            }
        }

        return flags;
    }

    /**
     * Reads an argument of type "number" from the request.
     */
    public long number( ImapRequestLineReader request ) throws ProtocolException
    {
        return readDigits(request, 0, 0, true);
    }
    
    private long readDigits( final ImapRequestLineReader request, int add, final long total, final boolean first ) throws ProtocolException
    {
        final char next;
        if (first) {
            next = request.nextWordChar();
        } else {
            request.consume();
            next = request.nextChar();
        }
        final long currentTotal = (10 * total) + add;
        switch (next) {
            case '0': return readDigits(request, 0, currentTotal, false);
            case '1': return readDigits(request, 1, currentTotal, false);
            case '2': return readDigits(request, 2, currentTotal, false);
            case '3': return readDigits(request, 3, currentTotal, false);
            case '4': return readDigits(request, 4, currentTotal, false);
            case '5': return readDigits(request, 5, currentTotal, false);
            case '6': return readDigits(request, 6, currentTotal, false);
            case '7': return readDigits(request, 7, currentTotal, false);
            case '8': return readDigits(request, 8, currentTotal, false);
            case '9': return readDigits(request, 9, currentTotal, false);
            case '.':
            case ' ':
            case '>':
            case '\r':
            case '\n':
            case '\t':
                return currentTotal;
            default:
                throw new ProtocolException("Expected a digit but was " + next);
        }
    }
    
    /**
     * Reads an argument of type "nznumber" (a non-zero number)
     * (NOTE this isn't strictly as per the spec, since the spec disallows
     * numbers such as "0123" as nzNumbers (although it's ok as a "number".
     * I think the spec is a bit shonky.)
     */
    public long nzNumber( ImapRequestLineReader request ) throws ProtocolException
    {
        long number = number( request );
        if ( number == 0 ) {
            throw new ProtocolException( "Zero value not permitted." );
        }
        return number;
    }

    private static boolean isCHAR( char chr )
    {
        return ( chr >= 0x01 && chr <= 0x7f );
    }

    private boolean isCHAR8( char chr )
    {
        return ( chr >= 0x01 && chr <= 0xff );
    }

    protected static boolean isListWildcard( char chr )
    {
        return ( chr == '*' || chr == '%' );
    }

    private static boolean isQuotedSpecial( char chr )
    {
        return ( chr == '"' || chr == '\\' );
    }

    /**
     * Consumes the request up to and including the eno-of-line.
     * @param request The request
     * @throws ProtocolException If characters are encountered before the endLine.
     */
    public void endLine( ImapRequestLineReader request ) throws ProtocolException
    {
        request.eol();
    }

    /**
     * Reads a "message set" argument, and parses into an IdSet.
     * Currently only supports a single range of values.
     */
    public IdRange[] parseIdRange( ImapRequestLineReader request )
            throws ProtocolException
    {
        CharacterValidator validator = new MessageSetCharValidator();
        String nextWord = consumeWord( request, validator );

        int commaPos = nextWord.indexOf( ',' );
        if ( commaPos == -1 ) {
            return new IdRange[]{ parseRange( nextWord ) };
        }

        ArrayList rangeList = new ArrayList();
        int pos = 0;
        while ( commaPos != -1 ) {
            String range = nextWord.substring( pos, commaPos );
            IdRange set = parseRange( range );
            rangeList.add( set );

            pos = commaPos + 1;
            commaPos = nextWord.indexOf( ',', pos );
        }
        String range = nextWord.substring( pos );
        rangeList.add( parseRange( range ) );
        return (IdRange[]) rangeList.toArray(new IdRange[rangeList.size()]);
    }

    private IdRange parseRange( String range ) throws ProtocolException
    {
        int pos = range.indexOf( ':' );
        try {
            if ( pos == -1 ) {
                long value = parseLong( range );
                return new IdRange( value );
            }
            else {
                long lowVal = parseLong( range.substring(0, pos ) );
                long highVal = parseLong( range.substring( pos + 1 ) );
                return new IdRange( lowVal, highVal );
            }
        }
        catch ( NumberFormatException e ) {
            throw new ProtocolException( "Invalid message set.", e);
        }
    }

    private long parseLong( String value ) {
        if ( value.length() == 1 && value.charAt(0) == '*' ) {
            return Long.MAX_VALUE;
        }
        return Long.parseLong( value );
    }
    /**
     * Provides the ability to ensure characters are part of a permitted set.
     */
    public interface CharacterValidator
    {
        /**
         * Validates the supplied character.
         * @param chr The character to validate.
         * @return <code>true</code> if chr is valid, <code>false</code> if not.
         */
        boolean isValid( char chr );
    }

    public static class NoopCharValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return true;
        }
    }

    public static class ATOM_CHARValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return ( isCHAR( chr ) && !isAtomSpecial( chr ) &&
                     !isListWildcard( chr ) && !isQuotedSpecial( chr ) );
        }

        private boolean isAtomSpecial( char chr )
        {
            return ( chr == '(' ||
                    chr == ')' ||
                    chr == '{' ||
                    chr == ' ' ||
                    chr == Character.CONTROL );
        }
    }

    public static class TagCharValidator extends ATOM_CHARValidator
    {
        public boolean isValid( char chr )
        {
            if ( chr == '+' ) return false;
            return super.isValid( chr );
        }
    }

    public static class MessageSetCharValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return ( isDigit( chr ) ||
                    chr == ':' ||
                    chr == '*' ||
                    chr == ',' );
        }

        private boolean isDigit( char chr )
        {
            return '0' <= chr && chr <= '9';
        }
    }

    /**
     * Decodes contents of a quoted string.
     * Charset aware.
     * One shot, not thread safe.
     */
    private static class QuotedStringDecoder {
        /** Decoder suitable for charset */
        private final CharsetDecoder decoder;
        
        /** byte buffer will be filled then flushed to character buffer */
        private final ByteBuffer buffer;
        /** character buffer may be dynamically resized */
        CharBuffer charBuffer;
        
        public QuotedStringDecoder(Charset charset) {
            decoder = charset.newDecoder();
            buffer = ByteBuffer.allocate(QUOTED_BUFFER_INITIAL_CAPACITY);
            charBuffer = CharBuffer.allocate(QUOTED_BUFFER_INITIAL_CAPACITY);
        }
        
        public String decode(ImapRequestLineReader request) throws ProtocolException {
            try {
                decoder.reset();
                char next = request.nextChar();
                while( next != '"' ) {
                    // fill up byte buffer before decoding
                    if (!buffer.hasRemaining()) {
                        decodeByteBufferToCharacterBuffer(false);
                    }
                    if ( next == '\\' ) {
                        request.consume();
                        next = request.nextChar();
                        if ( ! isQuotedSpecial( next ) ) {
                            throw new ProtocolException( "Invalid escaped character in quote: '" +
                                    next + "'" );
                        }
                    }
                    // TODO: nextChar does not report accurate chars so safe to cast to byte
                    buffer.put( (byte) next );
                    request.consume();
                    next = request.nextChar();
                }
                completeDecoding();
                final String result = charBuffer.toString();
                return result;

            } catch (IllegalStateException e) {
                throw new ProtocolException ("Bad character encoding", e);
            }
        }

        private void completeDecoding() throws ProtocolException {
            decodeByteBufferToCharacterBuffer(true);
            flush();
            charBuffer.flip();
        }

        private void flush() throws ProtocolException {
            final CoderResult coderResult = decoder.flush(charBuffer);
            if (coderResult.isOverflow()) {
                upsizeCharBuffer();
                flush();
            } else if (coderResult.isError()) {
                throw new ProtocolException("Bad character encoding");
            }
        }

        /**
         * Decodes contents of the byte buffer to the character buffer.
         * The character buffer will be replaced by a larger one if required.
         * @param endOfInput is the input ended
         */
        private CoderResult decodeByteBufferToCharacterBuffer(final boolean endOfInput) throws ProtocolException {
            buffer.flip();
            return decodeMoreBytesToCharacterBuffer(endOfInput);
        }

        private CoderResult decodeMoreBytesToCharacterBuffer(final boolean endOfInput) throws ProtocolException {
            final CoderResult coderResult = decoder.decode(buffer, charBuffer, endOfInput);
            if (coderResult.isOverflow()) {
                upsizeCharBuffer();
                return decodeMoreBytesToCharacterBuffer(endOfInput);
            } else if (coderResult.isError()) {
                throw new ProtocolException("Bad character encoding");
            } else if (coderResult.isUnderflow()) {
                buffer.clear();
            }
            return coderResult;
        }

        /**
         * Increases the size of the character buffer.
         */
        private void upsizeCharBuffer() {
            final int oldCapacity = charBuffer.capacity();
            CharBuffer oldBuffer = charBuffer;
            charBuffer = CharBuffer.allocate(oldCapacity + QUOTED_BUFFER_INITIAL_CAPACITY);
            oldBuffer.flip();
            charBuffer.put(oldBuffer);
        }
    }
}
