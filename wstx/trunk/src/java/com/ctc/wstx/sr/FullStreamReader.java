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

package com.ctc.wstx.sr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.util.URLUtil;

/**
 * Implementation of {@link XMLStreamReader} that builds on
 * {@link WstxStreamReader}, but adds full DTD-handling, including
 * DTD validation
 */
public class FullStreamReader
    extends WstxStreamReader
{
    /*
    ////////////////////////////////////////////////
    // Constants for standard StAX properties:
    ////////////////////////////////////////////////
    */

    final static String STAX_PROP_ENTITIES = "javax.xml.stream.entities";

    final static String STAX_PROP_NOTATIONS = "javax.xml.stream.notations";

    /*
    ////////////////////////////////////////////////////
    // DTD information (entities, ...)
    ////////////////////////////////////////////////////
     */

    // // // Note: some members that logically belong here, are actually
    // // // part of superclass

    /**
     * Combined DTD set, constructed from parsed internal and external
     * entities (which may have been set via override DTD functionality).
     */
    DTDSubset mDTD = null;

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    private FullStreamReader(BranchingReaderSource input, ReaderCreator owner,
                             ReaderConfig cfg, InputElementStack elemStack)
        throws IOException, XMLStreamException
    {
        super(input, owner, cfg, elemStack);
    }

    /**
     * Factory method for constructing readers.
     *
     * @param owner "Owner" of this reader, factory that created the reader;
     *   needed for returning updated symbol table information after parsing.
     * @param input Input source used to read the XML document.
     * @param cfg Object that contains reader configuration info.
     */
    public static FullStreamReader createFullStreamReader
        (BranchingReaderSource input, ReaderCreator owner,
         ReaderConfig cfg, InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
        InputElementStack elemStack;
        if (!cfg.willValidateWithDTD()) {
            elemStack = WstxStreamReader.createElementStack(cfg);
        } else {
            boolean normAttrs = cfg.willNormalizeAttrValues();
            if (cfg.willSupportNamespaces()) {
                elemStack = new VNsInputElementStack(16, sPrefixXml, sPrefixXmlns, normAttrs);
            } else {
                elemStack = new VNonNsInputElementStack(16, normAttrs);
            }
        }

        FullStreamReader sr = new FullStreamReader(input, owner, cfg, elemStack);
        sr.initProlog(bs);
        return sr;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        // Need to have full info...
        if (mStTokenUnfinished) {
            try { finishToken(); } catch (Exception ie) {
                throwLazyError(ie);
            }
        }

        // DTD-specific properties...
        if (mCurrToken == DTD && mDTD != null) {
            if (name.equals(STAX_PROP_ENTITIES)) {
                return mDTD.getGeneralEntityList();
            }
            if (name.equals(STAX_PROP_NOTATIONS)) {
                return mDTD.getNotationList();
            }
        }

        return super.getProperty(name);
    }

    /*
    ////////////////////////////////////////////////////
    // Extended (non-StAX) public API:
    ////////////////////////////////////////////////////
     */

    public DTDSubset getDTD() {
        return mDTD;
    }

    public void setDTDOverride(String pubId, String sysId)
        throws IOException, XMLStreamException
    {
        mDTD = findDtdExtSubset(pubId, sysId, null);
    }

    public void setDTDOverride(String pubId, URL source)
        throws IOException, XMLStreamException
    {
        setDTDOverride(pubId, source.toExternalForm());
    }

    /*
    ////////////////////////////////////////////////////
    // Private methods, DOCTYPE handling
    ////////////////////////////////////////////////////
     */

    /**
     * This method gets called to handle remainder of DOCTYPE declaration,
     * essentially the optional internal subset. Internal subset, if such
     * exists, is always read, but whether its contents are added to the
     * read buffer depend on passed-in argument.
     *<p>
     * NOTE: Since this method overrides the default implementation, make
     * sure you do NOT change the method signature.
     *
     * @param copyContents If true, will copy contents of DOCTYPE declaration
     *   in the text buffer (in addition to parsing it for actual use); if
     *   false, will only do parsing.
     */
    protected void finishDTD(boolean copyContents)
        throws IOException, XMLStreamException
    {
        if (!hasConfigFlags(CFG_SUPPORT_DTD)) {
            super.finishDTD(copyContents);
            return;
        }

        // Does anyone care about stuff in DTD?
        if (!copyContents) {
            ((BranchingReaderSource) mInput).endBranch(mInputPtr);
        }

        char c = getNextChar(SUFFIX_IN_DTD);
        DTDSubset intSubset = null;

        /* Do we have an internal subset? Note that we have earlier checked
         * that it has to be either '[' or closing '>'.
         */
        if (c == '[') {
            intSubset = mConfig.getDtdReader().readInternalSubset(this, mInput, mConfig);
            // And then we need closing '>'
            c = getNextCharAfterWS(SUFFIX_IN_DTD_INTERNAL);
        }

        if (c != '>') {
            throwUnexpectedChar(c, "; expected '>' to finish DOCTYPE declaration.");
        }

        // Enough about DOCTYPE decl itself:
        if (copyContents) {
            ((BranchingReaderSource) mInput).endBranch(mInputPtr);
        }

        /* But, then, we also may need to read the external subset, if
         * one was defined:
         */
        DTDSubset combo;

        /* 19-Sep-2004, TSa: That does not need to be done, however, if
         *    there's a DTD override set.
         */
        if (mDTD != null) {
            // We have earlier override that's already parsed
            combo = mDTD;
        } else {
            // Nope, no override
            DTDSubset extSubset = (mDtdPublicId != null || mDtdSystemId != null) ?
                findDtdExtSubset(mDtdPublicId, mDtdSystemId, intSubset) : null;
            
            if (intSubset == null) {
                combo = extSubset;
            } else if (extSubset == null) {
                combo = intSubset;
            } else {
                combo = intSubset.combineWithExternalSubset(this, extSubset);
            }
            
            mDTD = combo;
        }

        mGeneralEntities = (combo == null) ? null : combo.getGeneralEntityMap();
        if (hasConfigFlags(CFG_VALIDATE_AGAINST_DTD)) {
            mElementStack.setElementSpecs(combo.getElementMap(), mSymbols,
                                          mCfgNormalizeAttrs, mGeneralEntities);
        }
    }

    /**
     * Method called by <code>finishDTD</code>, to locate the specified
     * external DTD subset. Subset may be obtained from a cache, if cached
     * copy exists and is compatible; if not, it will be read from the
     * source identified by the public and/or system identifier passed.
     */
    private DTDSubset findDtdExtSubset(String pubId, String sysId,
                                       DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        boolean cache = hasConfigFlags(CFG_CACHE_DTDS);
        URL sysRef = null;
        DTDId dtdId = null;
        DTDSubset extSubset = null;

        if (cache) {
            dtdId = constructDtdId(pubId, sysId);
            sysRef = dtdId.getSystemId();
            extSubset = mOwner.findCachedDTD(dtdId);
            /* Ok, now; can use the cached copy iff it does not refer to
             * any parameter entities internal subset (if one exists)
             * defines:
             */
            if (extSubset != null) {
                if (intSubset == null || extSubset.isReusableWith(intSubset)) {
                    return extSubset;
                }
            }
        }

        // No useful cached copy? Need to read it then:
            
        /* For now, we do require system identifier; otherwise we don't
         * know how to resolve DTDs by public id. In future should
         * probably also have some simple catalog resolving facility?
         */
        if (sysId == null) {
            throwParseError("Can not resolve DTD with public id '"
                            +mDtdPublicId+"'; missing system identifier.");
        }
        
        if (sysRef == null) {
            sysRef = resolveExtSubsetPath(sysId);
        }

        WstxInputSource src = null;

        try {
            WstxInputResolver res = mConfig.getDtdResolver();
            if (res != null) {
                // null, since it's not an entity expansion
                src = res.resolveReference
                    (mInput, null, mDtdPublicId, mDtdSystemId, sysRef);
            }
            
            if (src == null) {
                // null, since it's not an entity expansion
                src = DefaultInputResolver.getInstance().resolveReference
                    (mInput, null, mDtdPublicId, mDtdSystemId, sysRef);
            }
        } catch (FileNotFoundException fex) {
            /* Let's catch and rethrow this just so we get more meaningful
             * description (with input source position etc)
             */
            throwParseError("(was "+fex.getClass().getName()+") "+fex.getMessage());
        }

        extSubset = mConfig.getDtdReader().readExternalSubset(this, src, mConfig, intSubset);
        
        if (cache) {
            /* Ok; can be cached, but only if it does NOT refer to
             * parameter entities defined in the internal subset (if
             * it does, there's no easy/efficient to check if it could
             * be used later on, plus it's unlikely it could be)
             */
            if (extSubset.isCachable()) {
                mOwner.addCachedDTD(dtdId, extSubset);
            }
        }

        return extSubset;
    }

    /**
     * Method called to resolve path to external DTD subset, given
     * system identifier.
     */
    private URL resolveExtSubsetPath(String systemId)
        throws IOException
    {
        // Do we have a context to use for resolving?
        URL ctxt = (mInput == null) ? null : mInput.getSource();

        /* Ok, either got a context or not; let's create the URL based on
         * the id, and optional context:
         */
        if (ctxt == null) {
            /* Call will try to figure out if system id has the protocol
             * in it; if not, create a relative file, if it does, try to
             * resolve it.
             */
            return URLUtil.urlFromSystemId(systemId);
        }
        return URLUtil.urlFromSystemId(systemId, ctxt);
    }

    protected DTDId constructDtdId(String pubId, String sysId)
        throws IOException
    {
        /* Following settings will change what gets stored as DTD, so
         * they need to separate cached instances too:
         */
        int significantFlags = mConfigFlags &
            (CFG_NAMESPACE_AWARE
             | CFG_NORMALIZE_LFS | CFG_NORMALIZE_ATTR_VALUES
             /* Let's optimize non-validating case; DTD info we need
              * is less if so (no need to store content specs for one)
              */
             | CFG_VALIDATE_AGAINST_DTD
             );
        
        if (pubId != null) {
            return DTDId.constructFromPublicId(pubId, significantFlags);
        }
        URL sysRef = resolveExtSubsetPath(sysId);
        return DTDId.constructFromSystemId(sysRef, significantFlags);
    }

    /*
    ////////////////////////////////////////////////////
    // Private methods, DTD validation support
    ////////////////////////////////////////////////////
     */

    /**
     * Method called by lower-level parsing code when invalid content
     * (anything inside element with 'empty' content spec; text inside
     * non-mixed element etc) is found during basic scanning. Note
     * that actual DTD element structure problems are not reported
     * through this method.
     */
    protected void reportInvalidContent(int evtType)
        throws WstxException
    {
        switch (mVldContent) {
        case CONTENT_ALLOW_NONE:
            throwParseError(ErrorConsts.ERR_VLD_EMPTY,
                            mElementStack.getTopElementDesc(),
                            ErrorConsts.tokenTypeDesc(evtType));
            break;
        case CONTENT_ALLOW_NON_MIXED:
            throwParseError(ErrorConsts.ERR_VLD_NON_MIXED,
                            mElementStack.getTopElementDesc());
            break;
        case CONTENT_ALLOW_DTD_ANY:
            /* Not 100% sure if this should ever happen... depends on
             * interpretation of 'any' content model?
             */
            throwParseError(ErrorConsts.ERR_VLD_ANY,
                            mElementStack.getTopElementDesc(),
                            ErrorConsts.tokenTypeDesc(evtType));
            break;
        default: // should never occur:
            throwParseError("Internal error: trying to report invalid content for "+evtType);
        }
    }
}