package fr.toutatice.ecm.platform.core.convert;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;


public class LibreOfficeCommandLineConverter extends CommandLineBasedConverter {

    /** specific Logger */ 
    private static final Log sofficelog = LogFactory.getLog("soffice");
	
    private static final String POOL_SIZE_PARAMETER = "EnvironmentPoolSize";

    private static final String TIMEOUT_DURATION_PARAMETER = "EnvironmentTimeoutDuration";

    /** poolSize */
    private static int poolSize;

    /**
     * timeoutDuration
     *
     * the maximum time to wait, in seconds
     *
     *
     */
    private static String timeoutDuration;

    private static AtomicInteger instanceCounter;

    protected class TTCCmdReturn {

        protected final CmdParameters params;

        protected final List<String> output;

        protected TTCCmdReturn(CmdParameters params, List<String> output) {
            this.params = params;
            this.output = output;
        }
    }

    protected TTCCmdReturn execOnBlobTTC(String commandName, Map<String, Blob> blobParameters, Map<String, String> parameters) throws ConversionException {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        List<String> filesToDelete = new ArrayList<>();
        try {
            if (blobParameters != null) {
                for (String blobParamName : blobParameters.keySet()) {
                    Blob blob = blobParameters.get(blobParamName);
                    File file = File.createTempFile("cmdLineBasedConverter", "." + FilenameUtils.getExtension(blob.getFilename()));
                    blob.transferTo(file);
                    params.addNamedParameter(blobParamName, file);
                    filesToDelete.add(file.getAbsolutePath());
                }
            }

            if (parameters != null) {
                for (String paramName : parameters.keySet()) {
                    params.addNamedParameter(paramName, parameters.get(paramName));
                }
            }
            params.addNamedParameter("timeoutDuration", timeoutDuration);

            ExecResult result = cles.execCommand(commandName, params);
            if (!result.isSuccessful()) {
                throw result.getError();
            }
            return new TTCCmdReturn(params, result.getOutput());
        } catch (CommandNotAvailable e) {
            // XXX bubble installation instructions
            throw new ConversionException("Unable to find targetCommand", e);
        } catch (IOException | CommandException e) {
            throw new ConversionException("Error while converting via CommandLineService", e);
        } finally {
            for (String fileToDelete : filesToDelete) {
                new File(fileToDelete).delete();
            }
        }
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String commandName = getCommandName(blobHolder, parameters);
        if (commandName == null) {
            throw new ConversionException("Unable to determine target CommandLine name");
        }

        TTCCmdReturn result;
        BlobHolder blobResult;
        instanceCounter.incrementAndGet();
        
        if(sofficelog.isDebugEnabled()) {
        	sofficelog.debug("instanceCounter = "+instanceCounter);
        }
        
        try {
            Map<String, Blob> blobParams = getCmdBlobParameters(blobHolder, parameters);
            Map<String, String> strParams = getCmdStringParameters(blobHolder, parameters);
            result = execOnBlobTTC(commandName, blobParams, strParams);
            blobResult = buildResult(result.output, result.params);
        } finally {
            instanceCounter.decrementAndGet();
        }

        return blobResult;
    }


    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<>();
        try {
            cmdBlobParams.put("inFilePath", blobHolder.getBlob());
        } catch (Exception e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<>();

        // tmp working directory
        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append("soffice_" + System.currentTimeMillis());
        File outDir = new File(tmpPath.toString());
        if (!outDir.mkdir()) {
            throw new ConversionException("Unable to create tmp dir for transformer output");
        }
        cmdStringParams.put("outDirPath", outDir.getAbsolutePath());

        // tmp soffice user directory to manage multiple instances
        int envIndex = instanceCounter.get();
        if (envIndex < poolSize) {
            Path envPath = new Path(baseDir).append("sofficeUserEnv_" + String.valueOf(envIndex));
            File envDir = new File(envPath.toString());
            if (!envDir.isDirectory() && !envDir.mkdir()) {
                throw new ConversionException("Unable to create tmp soffice user directory for transformer output");
            }
            cmdStringParams.put("envDirPath", envDir.getAbsolutePath());
        }else{
            throw new ConversionException("LibreOffice user environment pool is full");
        }
        
        cmdStringParams.put("logPath", Framework.getProperty("nuxeo.log.dir"));

        return cmdStringParams;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        CmdParameters.ParameterValue outDirPath = cmdParams.getParameters().get("outDirPath");
        File outputDir = new File(outDirPath.getValue());
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<>();

        for (File file : files) {
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());

            if (file.getName().equalsIgnoreCase("index.html")) {
                blobs.add(0, blob);
            } else {
                blobs.add(blob);
            }
        }
        return new SimpleCachableBlobHolder(blobs);
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        initParameters = descriptor.getParameters();
        if (initParameters == null) {
            initParameters = new HashMap<>();
        }
        poolSize = Integer.valueOf(initParameters.get(POOL_SIZE_PARAMETER));
        timeoutDuration = initParameters.get(TIMEOUT_DURATION_PARAMETER);
        instanceCounter = new AtomicInteger();
        getCommandLineService();
    }

}
