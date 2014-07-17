package com.dd.dfs.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;

import com.dd.dfs.Constants;
import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.filesystem.operator.PhysicalFileOperator;
import com.dd.dfs.utils.DateUtils;


/**
 * 静态资源请求的处理类
 * @author DD
 *
 */
public class StaticResourceProcessor {
	/**
     * MIME multipart separation string
     */
    protected static final String mimeSeparation = "DFS_MIME_BOUNDARY";
    /**
     * The MIME mappings for this web application, keyed by extension.
     */
    private static final HashMap<String, String> mimeMappings = new HashMap<String, String>();
    
    static{
    	mimeMappings.put("mp4", "video/mp4");
    	mimeMappings.put("exe", "application/octet-stream");
    }
    

	public void process(HttpRequest request, HttpResponse response) throws IOException {
		if (request.getRequestURI() == null)
			return;
		
		File file = new File(Constants.FILE_ROOT, request.getRequestURI());
		System.out.println("请求资源位置：" + Constants.FILE_ROOT + request.getRequestURI());
		
		if (!file.exists()) {// 如果请求的静态资源不存在
			// 如果资源找不到，返回错误信息
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

	    // Checking If headers
        if (!checkIfHeaders(request, response, file)) {
            return;
        }
        
		OutputStream out = response.getStream();
		PhysicalFileOperator physicOps =  new PhysicalFileOperator();
		
		//Find content type.
		String contentType = getMimeType(file.getName());
		response.setContentType(contentType);
		response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
		
		// Parse range specifier
		Vector<Range> ranges = null;
		ranges = parseRange(request, response, file);
		
		// ETag header
		response.setHeader("ETag", getETag(file));

		response.setHeader("Last-Modified", getLastModifidDate(file));
		
		
		
		if ( ( ranges == null || ranges.isEmpty() ) && request.getHeader("Range") == null ) {
			//如果没有分段请求，直接返回整个内容
			response.setContentLength((int) file.length());
			//response.sendHeaders();//header由ResponseStream在首次flushbuff的时候发送
			if("GET".equals(request.getMethod()))
				physicOps.downloadData(file.getAbsolutePath(), out);
		}
		else {
			if ((ranges == null) || (ranges.isEmpty()))
				return;
			// Partial content response.
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			
			if (ranges.size() == 1) {
				Range range = (Range) ranges.elementAt(0);
				response.setHeader("Content-Range", "bytes " + range.start
						+ "-" + range.end + "/" + range.length);
				response.setContentLength((int) (range.end - range.start + 1));
				//response.sendHeaders();
				physicOps.downloadData(file.getAbsolutePath(), out, range.start, (range.end - range.start + 1));
				
			}else{
				response.setContentType("multipart/byteranges; boundary=" + mimeSeparation);
				PrintWriter writer = response.getWriter();
				while (((Enumeration<?>) ranges).hasMoreElements() ) {
		            Range currentRange = (Range) ((Enumeration<?>) ranges).nextElement();
		            
		            // Writing MIME header.
					writer.println("--" + mimeSeparation);
					if (contentType != null)
						writer.println("Content-Type: " + contentType);
					writer.println("Content-Range: bytes " + currentRange.start
							+ "-" + currentRange.end + "/"
							+ currentRange.length);
					writer.println();

		            // Printing content
		            physicOps.downloadData(file.getAbsolutePath(), out, currentRange.start, (currentRange.end - currentRange.start + 1));
		        }

				writer.print("--" + mimeSeparation + "--");
			}
		}
		
	}

	/**
     * Check if the conditions specified in the optional If headers are
     * satisfied.
	 * @throws IOException 
     */
	private boolean checkIfHeaders(HttpRequest request, HttpResponse response,
			File file) throws IOException {
		return checkIfMatch(request, response, file) 
	            && checkIfModifiedSince(request, response, file) 
	            && checkIfNoneMatch(request, response, file) 
	            && checkIfUnmodifiedSince(request, response, file);
	}


	private boolean checkIfUnmodifiedSince(HttpRequest request,
			HttpResponse response, File file) {
		try {
			long lastModified = file.lastModified();
			long headerValue = -1;
			String header = request.getHeader("If-Unmodified-Since");
			if (header != null)
				headerValue = DateUtils.getDateFromString(header).getTime();
			if (headerValue != -1) {
				if (lastModified > headerValue) {
					// The entity has not been modified since the date
					// specified by the client. This is not an error case.
					response.sendError(
							HttpServletResponse.SC_PRECONDITION_FAILED,
							"PRECONDITION_FAILED");
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean checkIfNoneMatch(HttpRequest request,
			HttpResponse response, File file) throws IOException {
		String eTag = getETag(file);
		String headerValue = request.getHeader("If-None-Match");
		if (headerValue != null) {

			boolean conditionSatisfied = false;

			if (!headerValue.equals("*")) {

				StringTokenizer commaTokenizer = new StringTokenizer(
						headerValue, ",");

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag))
						conditionSatisfied = true;
				}

			} else {
				conditionSatisfied = true;
			}

			if (conditionSatisfied) {

				// For GET and HEAD, we should respond with
				// 304 Not Modified.
				// For every other method, 412 Precondition Failed is sent
				// back.
				if (("GET".equals(request.getMethod()))
						|| ("HEAD".equals(request.getMethod()))) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return false;
				} else {
					response.sendError(
							HttpServletResponse.SC_PRECONDITION_FAILED,
							"PRECONDITION_FAILED");
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkIfModifiedSince(HttpRequest request,
			HttpResponse response, File file) {
		try {
            long headerValue = -1;
            String header = request.getHeader("If-Modified-Since");
            if(header != null)
            	headerValue = DateUtils.getDateFromString(header).getTime();
            long lastModified = file.lastModified();
            if (headerValue != -1) {
    
                // If an If-None-Match header has been specified, if modified since
                // is ignored.
                if ((request.getHeader("If-None-Match") == null) 
                    && (lastModified <= headerValue + 1000)) {
                    // The entity has not been modified since the date
                    // specified by the client. This is not an error case.
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return false;
                }
            }
        } catch(Exception e) {
            return false;
        }
        return true;
	}

	private boolean checkIfMatch(HttpRequest request, HttpResponse response,
			File file) throws IOException {
		String eTag = getETag(file);
		String headerValue = request.getHeader("If-Match");
		if (headerValue != null) {
			if (headerValue.indexOf('*') == -1) {

				StringTokenizer commaTokenizer = new StringTokenizer(
						headerValue, ",");
				boolean conditionSatisfied = false;

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag))
						conditionSatisfied = true;
				}

				// If none of the given ETags match, 412 Precodition failed is
				// sent back
				if (!conditionSatisfied) {
					response.sendError(
							HttpServletResponse.SC_PRECONDITION_FAILED,
							"PRECONDITION_FAILED");
					return false;
				}

			}
		}
		return true;
	}

	/**
	 * Parse the range header.
	 * @param request
	 * @param response
	 * @param file 请求文件
	 * @throws IOException
	 */
	private Vector<Range> parseRange(HttpRequest request, HttpResponse response, File file) {
		// Checking If-Range
		//IF-Range头部需配合Range，如果没有Range参数，则If-Range会被视为无效。
		//如果If-Range匹配上，那么客户端已经存在的部分是有效的，服务器将返回缺失部分，也就是Range里指定的，然后返回206（Partial content)
		//否则证明客户端的部分已无效（可能已经更改），那么服务器将整个实体内容全部返回给客户端，同时返回200OK
        String headerValue = request.getHeader("If-Range");
        if (headerValue != null) {

            String eTag = getETag(file);
            long lastModified = file.lastModified();

            Date date = null;

            // Parsing the HTTP Date
			try {
				date = DateUtils.getDateFromString(headerValue);
			} catch (Exception e) {
				;
			}

            if (date == null) {

                // If the ETag the client gave does not match the entity
                // etag, then the entire entity is returned.
                if (!eTag.equals(headerValue.trim())){
                	return null;
                }

            } else {

                // If the timestamp of the entity the client got is older than
                // the last modification date of the entity, the entire entity
                // is returned.
                if (lastModified > (date.getTime() + 1000))
                    return null;

            }

        }
        
		long fileLength = file.length();
        if (fileLength == 0)
            return null;
        
        // Retrieving the range header 
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null)
            return null;
        
        //如果range字段不符合“Range：bytes=”的形式
        if (!rangeHeader.startsWith("bytes")) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }
        
        rangeHeader = rangeHeader.substring(6);
        
        Vector<Range> result = new Vector<Range>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");
        // Parsing the range list.
        // Range可能有很多段请求，如Range：bytes=0-500,1000-1500
        while (commaTokenizer.hasMoreTokens()) {
        	String range = commaTokenizer.nextToken();
			Range currentRange = new Range();
			currentRange.length = fileLength;
			
			int dashPos = range.indexOf('-');
			if (dashPos == -1) { //如果range字段不包含“Range：bytes=x-x”
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }
			try {
				if (dashPos == 0) {//表示最后500个字节：bytes=-500
                    long offset = Long.parseLong(range);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
	            }
				currentRange.start = Long.parseLong(range.substring(0, dashPos));
                if (dashPos < range.length() - 1)//表示第二个500字节：bytes=500-999
                    currentRange.end = Long.parseLong(range.substring(dashPos + 1, range.length()));
                else//表示500字节以后的范围：bytes=500-
                    currentRange.end = fileLength - 1;
			} catch (NumberFormatException e) {
				response.addHeader("Content-Range", "bytes */" + fileLength);
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return null;
			}
			
			if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }
			
			result.addElement(currentRange);
        }
        
		return result;
	}

    /**
     * Get the ETag associated with a file.
     * @param File object
     */
	private String getETag(File file) {
		return "W/\"" + file.length() + "-" + file.lastModified() + "\"";
	}
    
	@SuppressWarnings("deprecation")
	private String getLastModifidDate(File file) {
		String date = null;
		long times = file.lastModified();
		if(times == 0L){
			date = DateUtils.getCurrentDate().toGMTString();
		}else{
			date = new Date(times).toGMTString();
		}
		return date;
	}
	
    /**
     * Return the MIME type of the specified file, or <code>null</code> if
     * the MIME type cannot be determined.
     *
     * @param file Filename for which to identify a MIME type
     */
    public String getMimeType(String file) {
        if (file == null)
            return (null);
        int period = file.lastIndexOf(".");
        if (period < 0)
            return (null);
        String extension = file.substring(period + 1);
        if (extension.length() < 1)
            return (null);
        return (mimeMappings.get(extension));
    }
	
	// ------------------------------------------------------ Range Inner Class
	private class Range {

		public long start;
		public long end;
		public long length;

		/**
		 * Validate range.
		 */
		public boolean validate() {
			if (end >= length)
				end = length - 1;
			return ((start >= 0) && (end >= 0) && (start <= end) && (length > 0));
		}

	}
}
