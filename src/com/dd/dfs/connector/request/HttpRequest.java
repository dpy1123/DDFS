package com.dd.dfs.connector.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;

import com.dd.dfs.connector.response.HttpResponse;


public class HttpRequest {

	private InputStream is = null;//����ԭʼInputStream
	private RequestStream stream = null;//���ڶ�ȡ����������
	private ArrayList<Cookie> cookies = null;
	private HashMap<String, String> headers = null;
	private ParameterMap parameters = null;
	
	private String method;
	private String protocol;
	private String queryString;//uri�еĲ�ѯ����
	private String requestURI;//����uri
	private String requestedSessionId;//����Ự��ʶ���Ƿ��ڲ�ѯ�ַ����У�jsessionid��ֵ
	private boolean isRequestedSessionIdFromUrl;//jsessionid�����Ự��ʶ���Ƿ��ڲ�ѯ�ַ����У�������Cookie��
	private boolean isRequestedSessionIdFromCookie;
	private int contentLength;
	private String contentType;
	private String characterEncoding;
	private boolean isParametersParsed = false;//��¼�����Ƿ������
	
    /**
     * Remote address.
     */
	private String remoteAddr = null;
	private HttpResponse response = null;

	public HttpRequest() {
	}
	
	public HttpRequest(InputStream inputStream) {
		this.is = inputStream;
	}
	
	/**
	 * ����request���ͷ���Դ
	 */
	public void finishRequest(){
		try {
			if(stream != null){
				stream.close();
			}
		} catch (IOException e) {
		}
	}
	
	public void addHeader(String name, String value){
		if(this.headers == null){
			this.headers = new HashMap<String, String>();
		}
		this.headers.put(name, value);
	}


	/**
	 * ������������
	 */
	private void parseParameters(){
		if(isParametersParsed) return;
		
		ParameterMap results = this.parameters;
		if(results == null){
			results = new ParameterMap();
			results.setLocked(false);//��д��
			//���ñ��뷽ʽ
			String encoding = getCharacterEncoding();
			if(encoding == null){
				encoding = "ISO-8859-1";
			}
			//��������
			//1.��uri�д��Ĳ�������
			parseParamsStr(results, this.queryString);
			//2.��������������������"content-length"��ֵ�����0��"content-type"��ֵΪ"application/x-www-form-urlencoded"
			if(this.contentType == null){
				this.contentType = "";
			}
			int separater = contentType.indexOf(";");
			if(separater >= 0){
				this.contentType = contentType.substring(0, separater).trim();
			}
			if("POST".equals(this.method) && this.contentLength > 0 &&
					"application/x-www-form-urlencoded".equals(this.contentType)){
				//�Ƚ����������
				byte[] buffer = new byte[contentLength];
				int readLength = 0;
				try {
					ServletInputStream is = getInputStream();
					while (readLength < contentLength) {
						int next = is.read(buffer, readLength, contentLength - readLength);
						if (next == -1) break;
						readLength += next;
					}
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				String messageBody = new String(buffer, Charset.forName(encoding));
				parseParamsStr(results, messageBody);
			}
			//�������
			results.setLocked(true);
			isParametersParsed = true;
			this.parameters = results;
		}
	}
	
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	/**
	 * �����ַ����еĲ���
	 * @param results ��Ž��
	 * @param paramStr ���������ַ������磺username=dd&password=1234
	 */
	private void parseParamsStr(ParameterMap results, String paramStr){
		if(paramStr == null) return;
		while(paramStr.length() > 0){
			int separater = paramStr.indexOf("&");
			int equal = paramStr.indexOf("=");
			if(equal != -1){
				String k = paramStr.substring(0, equal);
				String v = "";
				if(equal < separater){
					v = paramStr.substring(equal+1, separater);
					paramStr = paramStr.substring(separater+1);
				}else{
					v = paramStr.substring(equal+1);
					paramStr = "";
				}
				results.put(k, v);
			}else{
				break;
			}
		}
	}
	
	//===================Ϊ��������س�Ա��������ӵ�set����===================
	public void setRequestedSessionIdFromCookie(
			boolean isRequestedSessionIdFromCookie) {
		this.isRequestedSessionIdFromCookie = isRequestedSessionIdFromCookie;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setRequestedSessionIdFromUrl(boolean isRequestedSessionIdFromUrl) {
		this.isRequestedSessionIdFromUrl = isRequestedSessionIdFromUrl;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	
	//===================�ӿڵ�Ĭ��ʵ�ַ���====================

	public ServletInputStream getInputStream() throws IOException {
		if (stream == null)
			stream = new RequestStream(this);
		return (stream);
	}



	public String getParameter(String arg0) {
		parseParameters();
		return (String) this.parameters.get(arg0);
	}

	public Map<String, String[]> getParameterMap() {
		parseParameters();
		Map<String, String[]> map = new HashMap<String, String[]>();
		Iterator<Object> i = parameters.keySet().iterator();
		while (i.hasNext()) {
			String name = (String) i.next();
			String[] value =new String[]{ (String) parameters.get(name)};
			map.put(name, value);
		}
		return map;
	}

	public String[] getParameterValues(String arg0) {
		parseParameters();
		return new String[]{(String) this.parameters.get(arg0)};
	}

	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * ��������ı��뷽ʽ 
	 * @param encoding ֧������֮һ��US-ASCII/ISO-8859-1/UTF-8/UTF-16BE/UTF-16LE/UTF-16
	 * @throws UnsupportedEncodingException
	 */
	public void setCharacterEncoding(String encoding)
			throws UnsupportedEncodingException {
		if ("US-ASCII".equalsIgnoreCase(encoding)
				|| "ISO-8859-1".equalsIgnoreCase(encoding)
				|| "UTF-8".equalsIgnoreCase(encoding)
				|| "UTF-16BE".equalsIgnoreCase(encoding)
				|| "UTF-16LE".equalsIgnoreCase(encoding)
				|| "UTF-16".equalsIgnoreCase(encoding))
			this.characterEncoding = encoding;
		else 
			throw new UnsupportedEncodingException("��֧�ֵ�Encoding��ʽ��"+encoding);
	}


	public String getHeader(String key) {
		if(headers == null || headers.size() < 1)
			return null;
		return this.headers.get(key);
	}

	public String getMethod() {
		return this.method;
	}



	public String getQueryString() {
		return this.queryString;
	}


	public String getRequestURI() {
		return this.requestURI;
	}


	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}



	public boolean isRequestedSessionIdFromCookie() {
		return this.isRequestedSessionIdFromCookie;
	}

	public boolean isRequestedSessionIdFromURL() {
		return this.isRequestedSessionIdFromUrl;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return this.isRequestedSessionIdFromURL();
	}

	public void addCookies(Cookie cookie) {
		if(this.cookies == null){
			this.cookies = new ArrayList<Cookie>();
		}
		this.cookies.add(cookie);
	}

	public Cookie[] getCookies() {
		if(this.cookies == null){
			return new Cookie[0];
		}
		return this.cookies.toArray(new Cookie[cookies.size()]);
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public InputStream getStream() {
		return is;
	}

	/**
     * Return the remote IP address making this Request.<br>
     * Return the IP address of the client or last proxy that sent the request. 
     */
	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setStream(RequestStream stream) {
		this.stream = stream;
	}
	
	/**
     * Set the IP address of the remote client associated with this Request.
     *
     * @param remoteAddr The remote IP address
     */
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public void setInputStream(InputStream is) {
		this.is = is;
	}

	public void setResponse(HttpResponse response) {
		this.response  = response;
	}

	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * Release all object references, and initialize instance variables, in
	 * preparation for reuse of this object.
	 */
	public void recycle() {
		if(cookies != null){
			cookies.clear();
		}
		if(headers != null){
			headers.clear();
		}
        method = null;
        if (parameters != null) {
            parameters.setLocked(false);
            parameters.clear();
        }
        queryString = null;
        requestedSessionId = null;
        requestURI = null;
        
        characterEncoding = null;
        // connector is NOT reset when recycling
        contentLength = -1;
        contentType = null;
        protocol = null;
        remoteAddr = null;
        response = null;
        stream = null;
        isParametersParsed = false;
    	
	}
}
