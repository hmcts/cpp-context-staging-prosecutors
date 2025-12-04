package uk.gov.moj.cpp.staging.prosecutors.converter;

import static java.lang.String.format;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class MediaTypeResolver {

    private static final Map<String, String> mediaTypeMap;
    static {
        mediaTypeMap = new HashMap<>();
        mediaTypeMap.put("pdf", "application/pdf");
        mediaTypeMap.put("txt", "text/plain");
        mediaTypeMap.put("doc", "application/msword");
        mediaTypeMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mediaTypeMap.put("xls", "application/vnd.ms-excel");
        mediaTypeMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mediaTypeMap.put("xml", "application/xml");
        mediaTypeMap.put("html", "text/html");
        mediaTypeMap.put("htm", "text/html");
        mediaTypeMap.put("docm", "application/vnd.ms-word.document.macroEnabled.12");
        mediaTypeMap.put("mht", "message/rfc822");
        mediaTypeMap.put("jpeg", "image/jpeg");
        mediaTypeMap.put("jpg", "image/jpeg");
        mediaTypeMap.put("tif", "image/tiff");
        mediaTypeMap.put("tiff", "image/tiff");
        mediaTypeMap.put("msg", "application/vnd.ms-outlook");
        mediaTypeMap.put("rtf", "application/rtf");
    }

    public String resolveMediaType(final String fileName) {

        if(fileName == null){
            throw new BadRequestException("Can not resolve mediaType! File name is empty.");
        }

        final String fileExtension = FilenameUtils.getExtension(fileName);
        if(fileExtension.isEmpty()){
            throw new BadRequestException(format("Can not resolve mediaType! File format can not be found in the FileName : %s",fileName));
        }

        final String mediaType = mediaTypeMap.get(fileExtension.toLowerCase());

        if( mediaType == null){
            throw new BadRequestException(format("File format is not supported! File name : %s",fileName));
        }

        return mediaType;
    }
}
