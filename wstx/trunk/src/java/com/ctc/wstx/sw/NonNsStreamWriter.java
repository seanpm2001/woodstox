/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.sw;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ctc.wstx.util.EmptyNamespaceContext;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Implementation of {@link XMLStreamWriter} used when namespace support
 * is not enabled. This means that only local names are used for elements
 * and attributes; and if rudimentary namespace declarations need to be
 * output, they are output using attribute writing methods.
 */
public class NonNsStreamWriter
    extends BaseStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    /**
     * Stack of currently open start elements; only local names
     * are included.
     */
    final StringVector mElements;

    /**
     * Container for attribute names for current element; used only
     * if uniqueness of attribute names is to be enforced.
     *<p>
     * TreeMap is used mostly because clearing it up is faster than
     * clearing up HashMap or HashSet, and the only access is done by
     * adding entries and see if an value was already set.
     */
    TreeMap mAttrNames;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public NonNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg);
        mElements = new StringVector(32);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    public NamespaceContext getNamespaceContext() {
        return EmptyNamespaceContext.getInstance();
    }

    public String getPrefix(String uri) {
        return null;
    }

    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set default namespace for non-namespace writer.");
    }

    public void setNamespaceContext(NamespaceContext context)
    {
        throw new IllegalArgumentException("Can not set NamespaceContext for non-namespace writer.");
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set namespace prefix for non-namespace writer.");
    }

    public void writeAttribute(String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new XMLStreamException("Trying to write an attribute when there is no open start element.");

        }

        // May need to check uniqueness?
        if (mCheckAttr) {
            if (mAttrNames == null) {
                mAttrNames = new TreeMap();
                mAttrNames.put(localName, value);
            } else {
                Object old = mAttrNames.put(localName, value);
                if (old != null) {
                    throw new IllegalArgumentException("Trying to write attribute '"+localName+"' twice (first value '"+old+"'; second '"+value+"').");
                }
            }
        }
        
        try {
            mWriter.write(' ');
            mWriter.write(localName);
            mWriter.write("=\"");
            XMLQuoter.outputDoubleQuotedAttr(mWriter, value);
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        writeAttribute(localName, value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        writeAttribute(localName, value);
    }

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set write namespaces with non-namespace writer.");
    }

    public void writeEmptyElement(String localName)
        throws XMLStreamException
    {
        doWriteStartElement(localName);
        mEmptyElement = true;
    }

    public void writeEmptyElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeEmptyElement(localName);
    }

    public void writeEmptyElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeEmptyElement(localName);
    }

    public void writeEndElement()
        throws XMLStreamException
    {
        /* Well, for one, we better have an open element in stack; otherwise
         * there's no way to figure out which element name to use.
         */
        if (mElements.isEmpty()) {
            throw new XMLStreamException("No open start element, when calling writeEndElement.");
        }

        doWriteEndElement(mElements.removeLast());
    }

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set write namespaces with non-namespace writer.");
    }

    public void writeStartElement(String localName)
        throws XMLStreamException
    {
        doWriteStartElement(localName);
        mEmptyElement = false;
    }

    public void writeStartElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeStartElement(localName);
    }

    public void writeStartElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartElement(localName);
    }
    
    /*
    ////////////////////////////////////////////////////
    // Package methods:
    ////////////////////////////////////////////////////
     */

    public void writeStartElement(StartElement elem)
        throws XMLStreamException
    {
        QName name = elem.getName();
        writeStartElement(name.getLocalPart());
        Iterator it = elem.getAttributes();
        while (it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            name = attr.getName();
            writeAttribute(name.getLocalPart(), attr.getValue());
        }
    }

    /**
     * Method called by {@link com.ctc.wstx.evt.WstxEventWriter} (instead of the version
     * that takes no argument), so that we can verify it does match the
     * start element, if necessary
     */
    public void writeEndElement(QName name)
        throws XMLStreamException
    {
        /* Well, for one, we better have an open element in stack; otherwise
         * there's no way to figure out which element name to use.
         */
        String local;
        if (mCheckStructure) {
            if (mElements.isEmpty()) {
                throw new XMLStreamException("No open start element, when calling writeEndElement.");
            }
            local = name.getLocalPart();
            String local2 = mElements.removeLast();
            if (!local.equals(local2)) {
                throw new IllegalArgumentException("Mismatching close element name, '"+local+"'; expected '"+local2+"'.");
            }
        } else {
            local = name.getLocalPart();
            if (!mElements.isEmpty()) {
                mElements.removeLast();
            }
        }
        doWriteEndElement(local);
    }

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or
     * attribute) is being output; except for end element which is
     * handled differently.
     */
    public void closeStartElement(boolean emptyElem)
        throws XMLStreamException
    {
        mStartElementOpen = false;
        if (mAttrNames != null) {
            mAttrNames.clear();
        }

        try {
            if (emptyElem) {
                // Extra space for readability (plus, browsers like it if using XHTML)
                mWriter.write(" />");
            } else {
                mWriter.write('>');
            }
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }

        // Need bit more special handling for empty elements...
        if (emptyElem) {
            mElements.removeLast();
            if (mElements.isEmpty()) {
                mState = STATE_EPILOG;
            }
        }
    }

    public String getTopElemName() {
        return mElements.getLastString();
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private void doWriteStartElement(String localName)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }
        if (mCheckContent) {
            checkNameValidity(localName, true);
        }

        if (mState == STATE_PROLOG) {
            mState = STATE_TREE;
        } else if (mCheckStructure && mState == STATE_EPILOG) {
            throw new IllegalStateException("Trying to output second root ('"
                                            +localName+"').");
        }

        mStartElementOpen = true;
        mElements.addString(localName);
        try {
            mWriter.write('<');
            mWriter.write(localName);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    /**
     *<p>
     * Note: Caller has to do actual removal of the element from element
     * stack, before calling this method.
     */
    private void doWriteEndElement(String localName)
        throws XMLStreamException
    {
        if (mStartElementOpen) {
            /* Can't/shouldn't call closeStartElement, but need to do same
             * processing. Thus, this is almost identical to closeStartElement:
             */
            mStartElementOpen = false;
            try {
                // We could write an empty element, implicitly?
                if (!mEmptyElement && mCfgOutputEmptyElems) {
                    // Extra space for readability
                    mWriter.write(" />");
                    if (mElements.isEmpty()) {
                        mState = STATE_EPILOG;
                    }
                    return;
                }
                // Nah, need to close open elem, and then output close elem
                mWriter.write('>');
            } catch (IOException ioe) {
                throw new XMLStreamException(ioe);
            }
        }

        try {
            mWriter.write("</");
            mWriter.write(localName);
            mWriter.write('>');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }

        if (mElements.isEmpty()) {
            mState = STATE_EPILOG;
        }
    }
}