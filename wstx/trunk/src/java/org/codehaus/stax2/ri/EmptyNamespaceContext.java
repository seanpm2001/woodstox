package org.codehaus.stax2.ri;

import java.io.Writer;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamWriter;

/**
 * Dummy {@link NamespaceContext} implementation that contains no
 * namespace information, except bindings that are specified by
 * the namespace specification itself (for prefixes "xml" and "xmlns")
 */
public class EmptyNamespaceContext
    implements NamespaceContext
{
    final static EmptyNamespaceContext sInstance = new EmptyNamespaceContext();
    
    private EmptyNamespaceContext() { }

    public static EmptyNamespaceContext getInstance() { return sInstance; }

    /*
    /////////////////////////////////////////////
    // NamespaceContext API
    /////////////////////////////////////////////
     */

    public final String getNamespaceURI(String prefix)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (prefix == null) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (prefix.length() > 0) {
            if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                return XMLConstants.XML_NS_URI;
            }
            if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            }
        }
        return null;
    }

    public final String getPrefix(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty URI as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        return null;
    }

    public final Iterator getPrefixes(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return new SingletonIterator(XMLConstants.XML_NS_PREFIX);
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return new SingletonIterator(XMLConstants.XMLNS_ATTRIBUTE);
        }

        return EmptyIterator.getInstance();
    }

    /*
    /////////////////////////////////////////////////////
    // Singleton iterator helper class
    /////////////////////////////////////////////////////
     */

/**
 * Simple read-only iterator that iterators over one specific item, passed
 * in as constructor argument.
 */
    final static class SingletonIterator
        implements Iterator
    {
        private final Object mValue;
        
        private boolean mDone = false;
        
        public SingletonIterator(Object value) {
            mValue = value;
        }
        
        public boolean hasNext() {
            return !mDone;
        }
        
        public Object next() {
            if (mDone) {
                throw new java.util.NoSuchElementException();
            }
            mDone = true;
            return mValue;
        }
        
        public void remove() {
            throw new UnsupportedOperationException("Can not remove item from SingletonIterator.");
        }
    }
}
