package com.dd.dfs.connector.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;




import com.dd.dfs.Constants;
import com.dd.dfs.connector.request.HttpRequest;

public class HttpResponse {

	/*
	 * 相应报文格式，CRLF表示回车，*表示有多个，()表示可选的，SP表示空格 HTTP Response = Status-Line
	 * *((general-header | response-header | entity-header) CRLF) CRLF
	 * [message-body] 其中 Status-Line = HTTP-Version SP Status-Code SP
	 * Reason-Phrase CRLF
	 */

	private OutputStream outputStream = null;//原始outputStream
	private ResponseStream stream = null;//输出用Stream
	private PrintWriter writer = null;//输出用Writer
	private HttpRequest request = null;
	
	protected byte[] buffer = new byte[2048];
	protected int bufferCount = 0;
	/**
	 * The actual number of bytes written to this Response.
	 */
	protected int contentCount = 0;
	
	/**
	 * The content length associated with this Response.
	 */
	protected int contentLength = -1;
	/**
	 * The content type associated with this Response.
	 */
	protected String contentType = null;
	/**
	 * Has this response been committed yet?
	 */
	protected boolean committed = false;
	/**
	 * The set of Cookies associated with this Response.
	 */
	protected ArrayList<Cookie> cookies = new ArrayList<Cookie>();
	/**
	 * The HTTP headers explicitly added via addHeader(), but not including
	 * those to be added with setContentLength(), setContentType(), and so on.
	 */
	protected HashMap<String, String> headers = new HashMap<String, String>();
	/**
	 * The error message set by <code>sendError()</code>.
	 */
	protected String message = "OK";
	/**
	 * The HTTP status code associated with this Response.
	 */
	protected int status = 200;
	/**
	 * The character encoding associated with this Response.
	 */
	protected String encoding = null;
	/**
	 * Error flag. True if the response is an error report.
	 */
	protected boolean error = false;

	public HttpResponse() {
	}
	
	public HttpResponse(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * 结束Response，释放资源
	 * @throws IOException 
	 */
	public void finishResponse() throws IOException {
		if (status < 400) {
			if ((stream == null) && (getContentLength() == -1)
					&& (status >= 200) && (status != HttpServletResponse.SC_NOT_MODIFIED)
					&& (status != HttpServletResponse.SC_NO_CONTENT))
				setContentLength(0);
		} else {
			setHeader("Connection", "close");
		}
		 
		// If an HTTP error >= 400 has been created with no content,
        // attempt to create a simple error message
        if (!isCommitted() && (stream == null) && (writer == null) &&
            (status >= 400) && (contentType == null) && (contentCount == 0)) {
            try {
                setContentType("text/html");
                PrintWriter writer = getWriter();
                writer.println("<html>");
                writer.println("<head>");
                writer.println("<title>DFS Error Report</title>");
                writer.println("<br><br>");
                writer.println("<h1>HTTP Status ");
                writer.print(status);
                writer.print(" - ");
                if (message != null)
                    writer.print(message);
                writer.println("</h1>");
                writer.println("</body>");
                writer.println("</html>");
            } catch (IOException e) {
                throw e;
            } catch (Throwable e) {
                ;       // Just eat it
            }
        }
		
		// Flush the headers and finish this response
        sendHeaders();
		
        // If no stream has been requested yet, get one so we can
        // flush the necessary headers
        if (this.stream == null) {
            ServletOutputStream sos = getStream();
            sos.flush();
            sos.close();
            return;
        }

        // If our stream is closed, no action is necessary
        if ( stream.closed() )
            return;

        // Flush and close the appropriate output mechanism
        if (writer != null) {
            writer.flush();
            writer.close();
        } else {
            stream.flush();
            stream.close();
        }
	}

	/**
	 * Send the HTTP response headers, if this has not already occurred.
	 */
	public void sendHeaders() throws IOException {
		if (isCommitted())
			return;
		// Prepare a suitable output writer
		OutputStreamWriter osr = null;
		try {
			osr = new OutputStreamWriter(getOutputStream(),
					getCharacterEncoding());
		} catch (UnsupportedEncodingException e) {
			osr = new OutputStreamWriter(getOutputStream());
		}
		final PrintWriter outputWriter = new PrintWriter(osr);
		// Send the "Status:" header
		outputWriter.print(this.getProtocol());
		outputWriter.print(" ");
		outputWriter.print(status);
		if (message != null) {
			outputWriter.print(" ");
			outputWriter.print(message);
		}
		outputWriter.print("\r\n");
		// Send the content-length and content-type headers (if any)
		if (getContentType() != null) {
			outputWriter.print("Content-Type: " + getContentType() + "\r\n");
		}
		if (getContentLength() >= 0) {
			outputWriter
					.print("Content-Length: " + getContentLength() + "\r\n");
		}
		// Send all specified headers (if any)
		synchronized (headers) {
			Iterator<String> names = headers.keySet().iterator();
			while (names.hasNext()) {
				String name = (String) names.next();
				String value = (String) headers.get(name);
				outputWriter.print(name);
				outputWriter.print(": ");
				outputWriter.print(value);
				outputWriter.print("\r\n");
			}
		}
		// Add the session ID cookie if necessary
		/*
		 * HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
		 * HttpSession session = hreq.getSession(false); if ((session != null)
		 * && session.isNew() && (getContext() != null) &&
		 * getContext().getCookies()) { Cookie cookie = new Cookie("JSESSIONID",
		 * session.getId()); cookie.setMaxAge(-1); String contextPath = null; if
		 * (context != null) contextPath = context.getPath(); if ((contextPath
		 * != null) && (contextPath.length() > 0)) cookie.setPath(contextPath);
		 * else
		 * 
		 * cookie.setPath("/"); if (hreq.isSecure()) cookie.setSecure(true);
		 * addCookie(cookie); }
		 */
		// Send all specified cookies (if any)
		synchronized (cookies) {
			Iterator<?> items = cookies.iterator();
			while (items.hasNext()) {
				Cookie cookie = (Cookie) items.next();
				outputWriter.print(cookie.getName());
				outputWriter.print(": ");
				outputWriter.print(cookie.getValue());
				outputWriter.print("\r\n");
			}
		}

		// Send a terminating blank line to mark the end of the headers
		outputWriter.print("\r\n");
		outputWriter.flush();

		committed = true;
	}

    /**
     * Send an error response with the specified status and message.
     *
     * @param status HTTP status code to send
     * @param message Corresponding message to send
     *
     * @exception IllegalStateException if this response has
     *  already been committed
     * @exception IOException if an input/output error occurs
     */
    public void sendError(int status, String message) throws IOException {
        if (isCommitted())
            throw new IllegalStateException("httpResponseBase.sendError.IllegalStateException");
        
        setHeader("Connection", "close");
		error = true;

		// Record the status code and message.
		this.status = status;
		this.message = message;

		// Clear any data content that has been buffered
		bufferCount = 0;

    }
    
	public String getCharacterEncoding() {
		if (encoding == null)
			return ("ISO-8859-1");
		else
			return (encoding);
	}

	protected String getProtocol() {
		return request.getProtocol();
	}

	/**
	 * 处理静态请求的响应
	 * @deprecated 已不用，暂时未删
	 */
	public void sendStaticResource() {
		if (request.getRequestURI() == null)
			return;

		byte[] buffer = new byte[1024];
		FileInputStream fileInputStream = null;
		int readed = 0;
		File file = new File(Constants.FILE_ROOT, request.getRequestURI());
		System.out.println("请求资源位置：" + Constants.FILE_ROOT
				+ request.getRequestURI());
		if (file.exists()) {// 如果请求的静态资源存在
			this.setHeader("Content-Length", String.valueOf(file.length()));
			try {
				fileInputStream = new FileInputStream(file);
				while ((readed = fileInputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, readed);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {
					}
				}
			}
		} else {// 如果资源找不到，返回错误信息
			String errorMsg = "HTTP/1.1 404 File Not Found\r\n"
					+ "Content-Type: text/html\r\n" + "Content-Length: 23\r\n"
					+ "\r\n" + "<h1>File Not Found</h1>";
			try {
				outputStream.write(errorMsg.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 真正向输出流写入数据，其他的<code>write()<code>方法都是先缓存数据
	 * @throws IOException
	 */
	public void flushBuffer() throws IOException {
		if (!isCommitted())
			sendHeaders();

		committed = true;
		if (bufferCount > 0) {
			try {
				outputStream.write(buffer, 0, bufferCount);
			}
			finally {
				bufferCount = 0;
			}
		}
	}
	  
	public void write(int b) throws IOException {
		if (bufferCount >= buffer.length)
			flushBuffer();
		buffer[bufferCount++] = (byte) b;
		contentCount++;
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		// If the whole thing fits in the buffer, just put it there
		if (len == 0)
			return;
		if (len <= (buffer.length - bufferCount)) {
			System.arraycopy(b, off, buffer, bufferCount, len);
			bufferCount += len;
			contentCount += len;
			return;
		}

		// Flush the buffer and start writing full-buffer-size chunks
		flushBuffer();
		int iterations = len / buffer.length;
		int leftoverStart = iterations * buffer.length;
		int leftoverLen = len - leftoverStart;
		for (int i = 0; i < iterations; i++)
			write(b, off + (i * buffer.length), buffer.length);

		// Write the remainder (guaranteed to fit in the buffer)
		if (leftoverLen > 0)
			write(b, off + leftoverStart, leftoverLen);
	}

	/* some implementation of HttpServletResponse */

	public void setRequest(HttpRequest request) {
		this.request = request;
	}

    /**
     * Return the writer associated with this Response.
     *
     * @exception IllegalStateException if <code>getStream</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
	public PrintWriter getWriter() throws IOException {
		if (writer != null)
			return (writer);
		if (stream != null)
			throw new IllegalStateException("responseBase.getWriter.IllegalStateException");
		
		ResponseStream newStream = new ResponseStream(this);
		newStream.setCommit(false);
		OutputStreamWriter osr = new OutputStreamWriter(newStream, getCharacterEncoding());
		this.writer = new PrintWriter(osr, true);
		stream = newStream;

		return this.writer;
	}
	
	/**
     * Return the servlet output stream associated with this Response.
     *
     * @exception IllegalStateException if <code>getWriter</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
	public ResponseStream getStream() {
		if (writer != null)
            throw new IllegalStateException("responseBase.getOutputStream.IllegalStateException");

        if (stream == null)
            stream = new ResponseStream(this);
        stream.setCommit(true);
		return stream;
	}

	public void addCookie(Cookie cookie) {
		if (isCommitted())
			return;
		synchronized (cookies) {
			cookies.add(cookie);
		}
	}

	public void addHeader(String name, String value) {
		if (isCommitted())
			return;
		synchronized (headers) {
			headers.put(name, value);
		}
	}

	public void setHeader(String name, String value) {
		addHeader(name, value);
		String match = name.toLowerCase();
		if (match.equals("content-length")) {
			int contentLength = -1;
			try {
				contentLength = Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
			if (contentLength >= 0)
				setContentLength(contentLength);
		} else if (match.equals("content-type")) {
			setContentType(value);
		}
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public boolean isCommitted() {
		return committed;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getHeader(String key) {
		if(headers == null || headers.size() < 1)
			return null;
		return this.headers.get(key);
	}

	public void setStatus(int status) {
		this.status = status;
		if(status == 206)
			this.message = "Partial Content";
		if(status == 404)
			this.message = "File Not Found";
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
	public void recycle() {
		
		cookies.clear();
		headers.clear();
		message = "OK";
		status = HttpServletResponse.SC_OK;
		
		// buffer is NOT reset when recycling
        bufferCount = 0;
        contentCount = 0;
        committed = false;
        // connector is NOT reset when recycling
        contentLength = -1;
        contentType = null;
        encoding = null;
        request = null;
        stream = null;
        writer = null;
        error = false;
	}


}
