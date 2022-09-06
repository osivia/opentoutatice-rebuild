/**
 * 
 */
package fr.toutatice.ecm.platform.core.services.infos.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;


/**
 * @author david
 *
 */
public class FileInfosProvider implements DocumentInformationsProvider {

    /** Is pdf convertible property. */
    public static final String IS_PDF_CONVERTIBLE = "isPdfConvertible";
    /** Error on pdf conversion. */
    public static final String ERROR_ON_PDF_CONVERSION = "errorOnPdfConversion";
    /** Converter name. */
    public static final String TTC_ANY_2_PDF_CONVERTER = "toutaticeAny2pdf";

    /** Conversion service. */
    private static ConversionService conversionService;

    /**
     * Getter for Libre Office converter.
     */
    public static ConversionService getConversionService() {
        if (conversionService == null) {
            conversionService = (ConversionService) Framework.getService(ConversionService.class);
        }
        return conversionService;
    }

    /**
     * Checks if File is convertible as pdf.
     */
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument)  {
        // Infos
        Map<String, Object> infos = new HashMap<String, Object>(1);

        // Convertible to pdf
        boolean convertible = false;

        // Must be File
        if (currentDocument != null && "File".equals(currentDocument.getType())) {
            // Can be convert to pdf
            BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
            Blob blob = bh.getBlob();

            if (blob != null) {
                String mimeType = blob.getMimeType();
                if ("application/pdf".equals(mimeType)) {
                    convertible = true;
                } else {
                    // Check if mimeType is supported by converter
                    convertible = getConversionService().isSourceMimeTypeSupported(TTC_ANY_2_PDF_CONVERTER, mimeType);

                    // Instead of declare all possible text mimeTypes on toutaticeAny2pdf converter,
                    // we say that all based text mimeTypes can be convert
                    if (!convertible && StringUtils.startsWith(mimeType, "text/")) {
                        convertible = true;
                    }
                }

            }
        }

        // Result
        infos.put(IS_PDF_CONVERTIBLE, convertible);

        return infos;
    }

}
