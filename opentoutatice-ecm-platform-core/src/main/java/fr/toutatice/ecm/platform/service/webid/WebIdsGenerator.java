package fr.toutatice.ecm.platform.service.webid;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.uidgen.AbstractUIDGenerator;


/**
 * Generates a random 6 char id for each document
 * 
 * @author Dorian Licois
 */
public class WebIdsGenerator extends AbstractUIDGenerator {

    /**
     * The number of bytes used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @see java.lang.Long since 1.8
     */
    public static final int BYTES = Long.SIZE / Byte.SIZE;

    @Override
    public String getSequenceKey(DocumentModel document) throws DocumentException {
        // no sequence used in this generator
        return null;
    }

    @Override
    public String createUID(DocumentModel document) throws DocumentException {

        int size = 6;

        long random = 0;
        for (int i = 0; i < size; i++) {
            int value = RandomUtils.nextInt(61);
            random += value * Math.round(Math.pow(64, i));
        }

        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putLong(0, random);
        byte[] bytes = buffer.array();

        byte[] truncatedArray = new byte[size];
        for (int i = 0; i < size; i++) {
            truncatedArray[i] = bytes[BYTES - size + i];
        }
        
        String encodedString = Base64.encodeBase64String(truncatedArray);
        return StringUtils.removeStart(encodedString, "AA");
    }

}
