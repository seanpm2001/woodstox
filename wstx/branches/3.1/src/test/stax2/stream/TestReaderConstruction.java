package stax2.stream;

import java.io.*;
import java.net.URL;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.io.*;

/**
 * Unit test suite that tests additional StAX2 stream reader construction
 * methods.
 */
public class TestReaderConstruction
    extends stax2.BaseStax2Test
{
    public void testCreateWithFile()
        throws IOException, XMLStreamException
    {
        File f = writeToTempFile("file");
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        verifyXML(ifact.createXMLStreamReader(f), "file");
    }

    public void testCreateWithURL()
        throws IOException, XMLStreamException
    {
        File f = writeToTempFile("URL");
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        URL url = f.toURL();
        verifyXML(ifact.createXMLStreamReader(url), "URL");
    }

    public void testCreateWithFileSource()
        throws IOException, XMLStreamException
    {
        File f = writeToTempFile("Filesource");
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        verifyXML(ifact.createXMLStreamReader(new Stax2FileSource(f)),
                  "Filesource");
    }

    public void testCreateWithURLSource()
        throws IOException, XMLStreamException
    {
        File f = writeToTempFile("URLSource");
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        verifyXML(ifact.createXMLStreamReader(new Stax2URLSource(f.toURL())),
                  "URLSource");
    }

    public void testCreateWithStringSource()
        throws XMLStreamException
    {
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        String xml = generateXML("Stringsource");
        verifyXML(ifact.createXMLStreamReader(new Stax2StringSource(xml)),
                  "Stringsource");
    }

    public void testCreateWithCharArraySource()
        throws XMLStreamException
    {
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        String xml = generateXML("CharArraySource");
        char[] ch = xml.toCharArray();
        verifyXML(ifact.createXMLStreamReader(new Stax2CharArraySource(ch, 0, ch.length)), "CharArraySource");
    }

    public void testCreateWithByteArraySource()
        throws XMLStreamException, IOException
    {
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        String xml = generateXML("ByteArraySource");
        byte[] b = xml.getBytes("UTF-8");
        verifyXML(ifact.createXMLStreamReader(new Stax2ByteArraySource(b, 0, b.length)), "ByteArraySource");
    }

    /*
    ////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////
     */

    public void verifyXML(XMLStreamReader sr, String textValue)
        throws XMLStreamException
    {
        /* No need to check thoroughly: mostly it's just checking that
         * the referenced doc can be found at all
         */
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals(textValue, getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    String generateXML(String text)
    {
        StringBuffer sb = new StringBuffer("<root>");
        sb.append(text);
        sb.append("</root>");
        return sb.toString();
    }

    File writeToTempFile(String text)
        throws IOException
    {
        File f = File.createTempFile("stax2test", null);
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        w.write(generateXML(text));
        w.flush();
        w.close();
        f.deleteOnExit();
        return f;
    }
}