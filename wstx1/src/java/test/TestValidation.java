package test;

import java.io.*;
import java.util.List;

import javax.xml.stream.*;

import com.ctc.wstx.stax.WstxInputProperties;

/**
 * Simple non-automated unit test for running validating stream reader on
 * given document.
 */
public class TestValidation
    implements XMLStreamConstants
{
    private TestValidation() {
    }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        return XMLInputFactory.newInstance();
    }

    protected int test(File file)
        throws Exception
    {
        XMLInputFactory f = getFactory();
        System.out.println("Factory instance: "+f.getClass());

        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                      Boolean.FALSE
                      //Boolean.TRUE
                      );


        f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        f.setProperty(XMLInputFactory.RESOLVER, new TestResolver1());

        if (f.isPropertySupported(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE)) {
            f.setProperty(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE,
                          Boolean.FALSE
                          //Boolean.TRUE
            );
        }

        if (f.isPropertySupported(WstxInputProperties.P_MIN_TEXT_SEGMENT)) {
            f.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT,
                          new Integer(6));
        }

        /*
        if (f.isPropertySupported(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES)) {
            java.util.Map m = new java.util.HashMap();
            m.put("myent", "foobar");
            m.put("myent2", "<tag>R&amp;B + &myent;</tag>");
            f.setProperty(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES, m);
        }
        */

        /*
        if (f.isPropertySupported(WstxInputProperties.P_DTD_RESOLVER)) {
            f.setProperty(WstxInputProperties.P_DTD_RESOLVER,
                          new TestResolver2());
        }
        if (f.isPropertySupported(WstxInputProperties.P_ENTITY_RESOLVER)) {
            f.setProperty(WstxInputProperties.P_ENTITY_RESOLVER,
                          new TestResolver2());
        }
        */

        // Uncomment for boundary-condition stress tests:
        if (f.isPropertySupported(WstxInputProperties.P_INPUT_BUFFER_LENGTH)) {
            f.setProperty(WstxInputProperties.P_INPUT_BUFFER_LENGTH,
                          new Integer(11));
        }
        if (f.isPropertySupported(WstxInputProperties.P_TEXT_BUFFER_LENGTH)) {
            f.setProperty(WstxInputProperties.P_TEXT_BUFFER_LENGTH,
                          new Integer(20));
        }

        if (f.isPropertySupported(WstxInputProperties.P_BASE_URL)) {
            f.setProperty(WstxInputProperties.P_BASE_URL, file.toURL());
        }

        // To test windows linefeeds:
        /*
        if (f.isPropertySupported(WstxInputProperties.P_NORMALIZE_LFS)) {
            f.setProperty(WstxInputProperties.P_NORMALIZE_LFS, Boolean.TRUE);
        } else {
            System.out.println("No property "+WstxInputProperties.P_NORMALIZE_LFS+", skipping.");
        }
        */

        System.out.println("Coalesce: "+f.getProperty(XMLInputFactory.IS_COALESCING));
        System.out.println("Namespace-aware: "+f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        System.out.println("Entity-expanding: "+f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));

        int total = 0;
        InputStream in;
        XMLStreamReader streamReader;

        in = new FileInputStream(file);

        streamReader = f.createXMLStreamReader(in);

        while (streamReader.hasNext()) {
            int type = streamReader.next();
            total += type; // so it won't be optimized out...

            boolean hasName = streamReader.hasName();

            System.out.print("["+type+"]");

            if (streamReader.hasText()) {
                String text = streamReader.getText();
                int textLen = streamReader.getTextLength();
                //total += textLen;
                // Sanity check (note: RI tends to return nulls?)
                if (text != null) {
                    char[] textBuf = streamReader.getTextCharacters();
                    int start = streamReader.getTextStart();
                    String text2 = new String(textBuf, start, textLen);
                    if (!text.equals(text2)) {
                        throw new Error("Text access via 'getText()' different from accessing via buffer: text='"+text+"', array='"+text2+"'");
                    }
                }

                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length(); // to prevent dead code elimination
                }
                if (type == CHARACTERS || type == CDATA) {
                    System.out.println(" Text = '"+text+"'.");
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    List entities = (List) streamReader.getProperty("javax.xml.stream.entities");
                    List notations = (List) streamReader.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.println(" DTD ("+entCount+" entities, "+notCount
                                       +" notations), declaration = <<\n");
                    System.out.println(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+streamReader.getLocalName()+" -> '"+streamReader.getText()+"'.");
                    hasName = false; // to suppress further output
                } else if (type == COMMENT) {
                    System.out.println(" Comment <!--"+text+"-->");
                } else { // comment, PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+streamReader.getPITarget()+"'.");
                System.out.println(" PI data = '"+streamReader.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                System.out.print(" ["+streamReader.getAttributeCount()+" attrs]");
            }
            if (hasName) {
                System.out.print(" Name: '"+streamReader.getName()+"' (prefix <"
                                   +streamReader.getPrefix()+">)");
            }

            System.out.println();
        }
        return total;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestValidation.class+" [file]");
            System.exit(1);
        }
        try {
          int total = new TestValidation().test(new File(args[0]));
          System.out.println("Total: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }
}
