package wstxtest.stream;

import java.io.StringReader;

import javax.xml.stream.*;

import com.ctc.wstx.api.WstxInputProperties;

/**
 * Tests that verify that it is possible to limit aspects of general parsed
 * entity handling: specifically, total number of expansions per document,
 * and maximum nesting depth of entity expansion.
 * 
 * @since 4.3
 */
public class TestEntityLimits
    extends BaseStreamTest
{
    public void testMaxEntityNesting() throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
                +" <!ENTITY top '&middle;'>\n"
                +" <!ENTITY middle '&bottom;'>\n"
                +" <!ENTITY bottom 'yay!'>\n"
                +"]><root>&top;</root>"
               ;

        // First: with default limits (high), should be fine
        XMLInputFactory f = getNewInputFactory();
        XMLStreamReader sr = f.createXMLStreamReader(new StringReader(XML));
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();

        // and with max depth of 3 as well
        f.setProperty(WstxInputProperties.P_MAX_ENTITY_DEPTH, Integer.valueOf(3));
        sr = f.createXMLStreamReader(new StringReader(XML));
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();

        // but not with 2
        f.setProperty(WstxInputProperties.P_MAX_ENTITY_DEPTH, Integer.valueOf(2));
        sr = f.createXMLStreamReader(new StringReader(XML));
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        try {
            sr.next();
            fail("Should have failed with entity depth limit extension");
        } catch (XMLStreamException e) {
            verifyException(e, "Maximum entity expansion depth");
        }
        sr.close();
    }
}
