package wstxtest.stream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.WstxInputProperties;

import wstxtest.cfg.*;

/**
 * Set of unit tests that check how Woodstox handles white space in
 * prolog and/or epilog.
 */
import com.ctc.wstx.api.ReaderConfig;

public class TestConfig
    extends BaseStreamTest
{
    public void testSettingResolvers()
        throws XMLStreamException
    {
        XMLInputFactory ifact = getNewInputFactory();
        // Default should be "no custom resolvers"
        assertNull(ifact.getProperty(WstxInputProperties.P_DTD_RESOLVER));
        assertNull(ifact.getProperty(WstxInputProperties.P_ENTITY_RESOLVER));

        // But if and when they are set, they should stick for both factory:
        XMLResolver dtdR = new DTDResolver();
        XMLResolver entityR = new EntityResolver();

        ifact.setProperty(WstxInputProperties.P_DTD_RESOLVER, dtdR);
        ifact.setProperty(WstxInputProperties.P_ENTITY_RESOLVER, entityR);

        Object gotDtdR = ifact.getProperty(WstxInputProperties.P_DTD_RESOLVER);
        Object gotEntityR = ifact.getProperty(WstxInputProperties.P_ENTITY_RESOLVER);
        assertTrue("DTD resolver set for factory should stick: didn't except value ["+gotDtdR+"]",
                   dtdR == gotDtdR);
        assertTrue("Entity resolver set for factory should stick: didn't except value ["+gotEntityR+"]",
                   entityR == gotEntityR);

        // and for the instances as well:
        XMLStreamReader sr = constructStreamReader(ifact, "<root />");
        gotDtdR = sr.getProperty(WstxInputProperties.P_DTD_RESOLVER);
        gotEntityR = sr.getProperty(WstxInputProperties.P_ENTITY_RESOLVER);

        assertTrue("DTD resolver set should be passed to instance by factory: didn't except value ["+gotDtdR+"]",
                   dtdR == gotDtdR);
        assertTrue("Entity resolver set should be passed to instance by factory: didn't except value ["+gotEntityR+"]",
                   entityR == gotEntityR);
    }

    /**
     * Unit test that ensures that DTD resolver gets properly called
     * when configured
     */
    public void testUsingDTDResolver()
        throws XMLStreamException
    {
        // !!! TBI
    }

    /**
     * Unit test that ensures that entity resolver gets properly called
     * when configured
     */
    public void testUsingEntityResolver()
        throws XMLStreamException
    {
        // !!! TBI
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
     */

    /*
    //////////////////////////////////////////////////////
    // Helper classes:
    //////////////////////////////////////////////////////
     */

    private final static class DTDResolver
        implements XMLResolver
    {
        public Object resolveEntity(String publicID, String systemID,
                                   String baseURI, String namespace)
        {
            return null;
        }
    }

    private final static class EntityResolver
        implements XMLResolver
    {
        public Object resolveEntity(String publicID, String systemID,
                                   String baseURI, String namespace)
        {
            return null;
        }
    }
}
