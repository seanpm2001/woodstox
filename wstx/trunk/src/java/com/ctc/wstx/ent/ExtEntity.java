package com.ctc.wstx.ent;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.io.InputSourceFactory;
import com.ctc.wstx.io.WstxInputResolver;
import com.ctc.wstx.io.WstxInputSource;

public abstract class ExtEntity
    extends EntityDecl
{
    final String mPublicId;
    final String mSystemId;

    public ExtEntity(Location loc, String name, URL ctxt,
                     String pubId, String sysId)
    {
        super(loc, name, ctxt);
        mPublicId = pubId;
        mSystemId = sysId;
    }
    
    public abstract String getNotationName();

    public String getPublicId() {
        return mPublicId;
    }

    public String getReplacementText() {
        return null;
    }

    public String getSystemId() {
        return mSystemId;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public abstract void writeEnc(Writer w) throws IOException;

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Access to data

    public char[] getReplacementChars() {
        return null;
    }

    // // // Type information
    
    public boolean isExternal() { return true; }
    
    public abstract boolean isParsed();
    
    public abstract WstxInputSource createInputSource(WstxInputSource parent,
                                                      WstxInputResolver res)
        throws IOException, XMLStreamException;
}