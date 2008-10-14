package com.ctc.wstx.io;

import java.io.*;

import com.ctc.wstx.api.ReaderConfig;

/**
 * Simple basic class for optimized Readers Woodstox has; implements
 * "cookie-cutter" methods that are used by all actual implementations.
 */
abstract class BaseReader
    extends Reader
{
    protected final static char NULL_CHAR = (char) 0;
    protected final static char NULL_BYTE = (byte) 0;

    /**
     * In xml 1.1, NEL (0x85) behaves much the way \n does (can
     * be follow \r as part of the linefeed
     */
    protected final static char CONVERT_NEL_TO = '\n';

    /**
     * In xml 1.1, LSEP bit like \n, or \r. Need to choose one as the
     * result. Let's use \n, for simplicity
     */
    protected final static char CONVERT_LSEP_TO = '\n';

    /**
     * DEL character is both the last ascii char, and illegal in xml 1.1.
     */
    final static char CHAR_DEL = (char) 127;

    protected final ReaderConfig mConfig;

    private InputStream mIn;

    protected byte[] mBuffer;

    protected int mPtr;
    protected int mLength;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    protected BaseReader(ReaderConfig cfg, InputStream in, byte[] buf, int ptr, int len)
    {
        mConfig = cfg;
        mIn = in;
        mBuffer = buf;
        mPtr = ptr;
        mLength = len;
    }

    /*
    ////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////
    */

    /**
     * Method that can be called to indicate the xml conformance used
     * when reading content using this reader. Some of the character
     * validity checks need to be done at reader level, and sometimes
     * they depend on xml level (for example, xml 1.1 has new linefeeds
     * and both more and less restricted characters).
     */
    public abstract void setXmlCompliancy(int xmlVersion);

    /*
    ////////////////////////////////////////
    // Reader API
    ////////////////////////////////////////
    */

    public void close()
        throws IOException
    {
        InputStream in = mIn;

        if (in != null) {
            mIn = null;
            freeBuffers();
            in.close();
        }
    }

    char[] mTmpBuf = null;

    /**
     * Although this method is implemented by the base class, AND it should
     * never be called by Woodstox code, let's still implement it bit more
     * efficiently just in case
     */
    public int read()
        throws IOException
    {
        if (mTmpBuf == null) {
            mTmpBuf = new char[1];
        }
        if (read(mTmpBuf, 0, 1) < 1) {
            return -1;
        }
        return mTmpBuf[0];
    }

    /*
    ////////////////////////////////////////
    // Internal/package methods:
    ////////////////////////////////////////
    */

    protected final InputStream getStream() { return mIn; }

    /**
     * Method for reading as many bytes from the underlying stream as possible
     * (that fit in the buffer), to the beginning of the buffer.
     */
    protected final int readBytes()
        throws IOException
    {
        mPtr = 0;
        mLength = 0;
        if (mIn != null) {
            int count = mIn.read(mBuffer, 0, mBuffer.length);
            if (count > 0) {
                mLength = count;
            }
            return count;
        }
        return -1;
    }

    /**
     * Method for reading as many bytes from the underlying stream as possible
     * (that fit in the buffer considering offset), to the specified offset.
     *
     * @return Number of bytes read, if any; -1 to indicate none available
     *  (that is, end of input)
     */
    protected final int readBytesAt(int offset)
        throws IOException
    {
        // shouldn't modify mPtr, assumed to be 'offset'
        if (mIn != null) {
            int count = mIn.read(mBuffer, offset, mBuffer.length - offset);
            if (count > 0) {
                mLength += count;
            }
            return count;
        }
        return -1;
    }

    /**
     * This method should be called along with (or instead of) normal
     * close. After calling this method, no further reads should be tried.
     * Method will try to recycle read buffers (if any).
     */
    public final void freeBuffers()
    {
        /* 11-Apr-2005, TSa: Ok, we can release the buffer now, to be
         *   recycled by the next stream reader instantiated by this
         *   thread (if any).
         */
        byte[] buf = mBuffer;
        if (buf != null) {
            mBuffer = null;
            if (mConfig != null) {
                mConfig.freeFullBBuffer(buf);
            }
        }
    }

    protected void reportBounds(char[] cbuf, int start, int len)
        throws IOException
    {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    protected void reportStrangeStream()
        throws IOException
    {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }

    protected void reportInvalidXml11(int value, int bytePos, int charPos)
        throws IOException
    {
        throw new CharConversionException("Invalid character 0x"
                                          +Integer.toHexString(value)
                                          +", can only be included in xml 1.1 using character entities (at char #"+charPos+", byte #"+bytePos+")");
    }
}

