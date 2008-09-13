package stax2.typed;

import java.lang.reflect.Array;
import java.util.Random;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

import stax2.BaseStax2Test;

/**
 * Base class that contains set of simple unit tests to verify implementation
 * of {@link TypedXMLStreamReader}. Concrete sub-classes are used to
 * test both native and wrapped Stax2 implementations.
 *
 * @author Tatu Saloranta
 */
public abstract class ReaderArrayTestBase
    extends BaseStax2Test
{
    // Let's test variable length arrays
    final static int[] COUNTS = new int[] {
        7, 39, 116, 900, 5003
    };

    /*
    ////////////////////////////////////////
    // Abstract methods
    ////////////////////////////////////////
     */

    protected abstract XMLStreamReader2 getReader(String contents)
        throws XMLStreamException;

    /*
    ////////////////////////////////////////
    // Test methods, elem, valid
    ////////////////////////////////////////
     */

    public void testSimpleIntArrayElem()
        throws XMLStreamException
    {
        for (int i = 0; i < COUNTS.length; ++i) {
            int len = COUNTS[i];
            int[] data = intArray(len);
            String XML = buildDoc(data);

            // First, full read
            verifyInts(XML, data, len);
            // Then one by one
            verifyInts(XML, data, 1);
            // And finally, random
            verifyInts(XML, data, -1);
        }
    }

    public void testSimpleLongArrayElem()
        throws XMLStreamException
    {
        for (int i = 0; i < COUNTS.length; ++i) {
            int len = COUNTS[i];
            long[] data = longArray(len);
            String XML = buildDoc(data);

            // First, full read
            verifyLongs(XML, data, len);
            // Then one by one
            verifyLongs(XML, data, 1);
            // And finally, random
            verifyLongs(XML, data, -1);
        }
    }

    public void testSimpleFloatArrayElem()
        throws XMLStreamException
    {
        for (int i = 0; i < COUNTS.length; ++i) {
            int len = COUNTS[i];
            float[] data = floatArray(len);
            String XML = buildDoc(data);

            // First, full read
            verifyFloats(XML, data, len);
            // Then one by one
            verifyFloats(XML, data, 1);
            // And finally, random
            verifyFloats(XML, data, -1);
        }
    }

    public void testSimpleDoubleArrayElem()
        throws XMLStreamException
    {
        for (int i = 0; i < COUNTS.length; ++i) {
            int len = COUNTS[i];
            double[] data = doubleArray(len);
            String XML = buildDoc(data);

            // First, full read
            verifyDoubles(XML, data, len);
            // Then one by one
            verifyDoubles(XML, data, 1);
            // And finally, random
            verifyDoubles(XML, data, -1);
        }
    }

    public void testEmptyElems()
        throws XMLStreamException
    {
        // And then some edge cases too
        for (int i = 0; i < 4; ++i) {
            XMLStreamReader2 sr = getReader("<root />");
            assertTokenType(START_ELEMENT, sr.next());
            int count;

            switch (i) {
            case 0:
                count = sr.readElementAsIntArray(new int[1], 0, 1);
                break;
            case 1:
                count = sr.readElementAsLongArray(new long[1], 0, 1);
                break;
            case 2:
                count = sr.readElementAsFloatArray(new float[1], 0, 1);
                break;
            default:
                count = sr.readElementAsDoubleArray(new double[1], 0, 1);
                break;
            }
            sr.close();
            assertEquals(-1, count);
        }
    }

    /*
    ////////////////////////////////////////
    // Test methods, elem, invalid
    ////////////////////////////////////////
     */

    public void testInvalidIntArrayElem()
        throws XMLStreamException
    {
        XMLStreamReader2 sr;

        for (int i = 0; i < 4; ++i) {
            sr = getReader("<root>1 2</root>");
            // Can't call on START_DOCUMENT
            try {
                switch (i) {
                case 0:
                    sr.readElementAsIntArray(new int[3], 0, 1);
                    fail("Expected an exception when trying to read at START_DOCUMENT");
                case 1:
                    sr.readElementAsLongArray(new long[3], 0, 1);
                    fail("Expected an exception when trying to read at START_DOCUMENT");
                case 2:
                    sr.readElementAsFloatArray(new float[3], 0, 1);
                    fail("Expected an exception when trying to read at START_DOCUMENT");
                default:
                    sr.readElementAsDoubleArray(new double[3], 0, 1);
                    fail("Expected an exception when trying to read at START_DOCUMENT");
                }
            } catch (XMLStreamException xse) { }
            
            sr = getReader("<root><!-- comment --></root>");
            sr.next();
            assertTokenType(COMMENT, sr.next());
            // or comment
            try {
                switch (i) {
                case 0:
                    sr.readElementAsIntArray(new int[3], 0, 1);
                    fail("Expected an exception when trying to read at COMMENT");
                case 1:
                    sr.readElementAsLongArray(new long[3], 0, 1);
                    fail("Expected an exception when trying to read at COMMENT");
                case 2:
                    sr.readElementAsFloatArray(new float[3], 0, 1);
                    fail("Expected an exception when trying to read at COMMENT");
                default:
                    sr.readElementAsDoubleArray(new double[3], 0, 1);
                    fail("Expected an exception when trying to read at COMMENT");
                }
            } catch (XMLStreamException xse) { }
        }
    }

    /*
    ////////////////////////////////////////
    // Test methods, attr, valid
    ////////////////////////////////////////
     */

    public void testSimpleIntArrayAttr()
        throws XMLStreamException
    {
    }

    public void testSimpleLongArrayAttr()
        throws XMLStreamException
    {
    }

    public void testSimpleFloatArrayAttr()
        throws XMLStreamException
    {
    }

    public void testSimpleDoubleArrayAttr()
        throws XMLStreamException
    {
    }

    /*
    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////
     */

    private int[] intArray(int count)
    {
        Random r = new Random(count);
        int[] result = new int[count];
        for (int i = 0; i < count; ++i) {
            int base = r.nextInt();
            int shift = (r.nextInt() % 24);
            result[i] = (base >> shift);
        }
        return result;
    }

    private long[] longArray(int count)
    {
        Random r = new Random(count);
        long[] result = new long[count];
        for (int i = 0; i < count; ++i) {
            long base = r.nextLong();
            int shift = (r.nextInt() % 56);
            result[i] = (base >> shift);
        }
        return result;
    }

    private float[] floatArray(int count)
    {
        Random r = new Random(count);
        float[] result = new float[count];
        for (int i = 0; i < count; ++i) {
            float f = r.nextFloat();
            result[i] = r.nextBoolean() ? -f : f;
        }
        return result;
    }

    private double[] doubleArray(int count)
    {
        Random r = new Random(count);
        double[] result = new double[count];
        for (int i = 0; i < count; ++i) {
            double d = r.nextDouble();
            result[i] = r.nextBoolean() ? -d : d;
        }
        return result;
    }


    private String buildDoc(Object dataArray)
    {
        int len = Array.getLength(dataArray);
        StringBuilder sb = new StringBuilder(len * 8);
        sb.append("<root>");
        for (int i = 0; i < len; ++i) {
            sb.append(Array.get(dataArray, i).toString());
            sb.append(' ');
        }
        sb.append("</root>");
        return sb.toString();
    }
    
    private void assertArraysEqual(Object expArray, Object actArray)
    {
        int expLen = Array.getLength(expArray);
        int actLen = Array.getLength(actArray);

        if (expLen != actLen) {
            fail("Expected number of entries "+expLen+", got "+actLen);
        }
        for (int i = 0; i < expLen; ++i) {
            assertEquals(Array.get(expArray, i), Array.get(actArray, i));
        }
    }

    private void verifyInts(String doc, int[] data, int blockLen)
        throws XMLStreamException
    {
        Random r = (blockLen < 0) ? new Random(blockLen) : null;
        int[] buffer = new int[Math.max(blockLen, 256)];
        int[] result = new int[data.length];
        int entries = 0;

        XMLStreamReader2 sr = getReader(doc);
        sr.next();
        assertTokenType(START_ELEMENT, sr.getEventType());

        while (true) {
            int readLen = (r == null) ? blockLen : (1 + r.nextInt() & 0xFF);
            int got = sr.readElementAsIntArray(buffer, 0, readLen);
            if (got < 0) { 
                break;
            }
            if ((entries + got) > result.length) {
                fail("Expected only "+result.length+" entries, already got "+(entries+got));
            }
            System.arraycopy(buffer, 0, result, entries, got);
            entries += got;
        }
        assertArraysEqual(data, result);
        sr.close();
    }

    private void verifyLongs(String doc, long[] data, int blockLen)
        throws XMLStreamException
    {
        Random r = (blockLen < 0) ? new Random(blockLen) : null;
        long[] buffer = new long[Math.max(blockLen, 256)];
        long[] result = new long[data.length];
        int entries = 0;

        XMLStreamReader2 sr = getReader(doc);
        sr.next();
        assertTokenType(START_ELEMENT, sr.getEventType());

        while (true) {
            int readLen = (r == null) ? blockLen : (1 + r.nextInt() & 0xFF);
            int got = sr.readElementAsLongArray(buffer, 0, readLen);
            if (got < 0) { 
                break;
            }
            if ((entries + got) > result.length) {
                fail("Expected only "+result.length+" entries, already got "+(entries+got));
            }
            System.arraycopy(buffer, 0, result, entries, got);
            entries += got;
        }
        assertArraysEqual(data, result);
        sr.close();
    }

    private void verifyFloats(String doc, float[] data, int blockLen)
        throws XMLStreamException
    {
        Random r = (blockLen < 0) ? new Random(blockLen) : null;
        float[] buffer = new float[Math.max(blockLen, 256)];
        float[] result = new float[data.length];
        int entries = 0;

        XMLStreamReader2 sr = getReader(doc);
        sr.next();
        assertTokenType(START_ELEMENT, sr.getEventType());

        while (true) {
            int readLen = (r == null) ? blockLen : (1 + r.nextInt() & 0xFF);
            int got = sr.readElementAsFloatArray(buffer, 0, readLen);
            if (got < 0) { 
                break;
            }
            if ((entries + got) > result.length) {
                fail("Expected only "+result.length+" entries, already got "+(entries+got));
            }
            System.arraycopy(buffer, 0, result, entries, got);
            entries += got;
        }
        assertArraysEqual(data, result);
        sr.close();
    }

    private void verifyDoubles(String doc, double[] data, int blockLen)
        throws XMLStreamException
    {
        Random r = (blockLen < 0) ? new Random(blockLen) : null;
        double[] buffer = new double[Math.max(blockLen, 256)];
        double[] result = new double[data.length];
        int entries = 0;

        XMLStreamReader2 sr = getReader(doc);
        sr.next();
        assertTokenType(START_ELEMENT, sr.getEventType());

        while (true) {
            int readLen = (r == null) ? blockLen : (1 + r.nextInt() & 0xFF);
            int got = sr.readElementAsDoubleArray(buffer, 0, readLen);
            if (got < 0) { 
                break;
            }
            if ((entries + got) > result.length) {
                fail("Expected only "+result.length+" entries, already got "+(entries+got));
            }
            System.arraycopy(buffer, 0, result, entries, got);
            entries += got;
        }
        assertArraysEqual(data, result);
        sr.close();
    }
}

