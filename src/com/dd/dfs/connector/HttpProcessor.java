package com.dd.dfs.connector;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.request.SocketInputStream;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.StaticResourceProcessor;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;
import com.dd.dfs.utils.DateUtils;



public class HttpProcessor implements Runnable, Lifecycle{
	/**
	 * ��ʾ��processorû�����ڴ��������
	 */
	private static final int PROCESSOR_IDLE = 0;
	/**
	 * ��ʾ��processor���ڴ�����
	 */
	private static final int PROCESSOR_ACTIVE = 1;

	private HttpConnector connector = null;//�����뱾��������ص�������
	/**
	 * The identifier of this processor, unique per connector.
	 */
	private int id = 0;
	
	private boolean stopped = false;//��ʶ���߳��Ƿ���ֹͣ
	private Socket client = null;//��ǰ����Ŀͻ������ӣ�ע�ⲻ�ǽ���ʱ�õģ�����Ϊ��Ʒ���ݴ����
	private boolean isAvalible;//��־��ǰ�߳��Ƿ���Socket���Խ���
	
    private boolean started = false;//��¼�������Ƿ�����
    private LifecycleSupport lifecycle = new LifecycleSupport(this);//�������ڹ�����
    
    /**
     * Processor state
     */
    private int status = PROCESSOR_IDLE;
    /**
     * The thread synchronization object.
     */
    private Object threadSync = new Object();
    /**
     * The background thread.
     */
    private Thread thread = null;
    
    /**
     * True if the client has asked to recieve a request acknoledgement. If so
     * the server will send a preliminary 100 Continue response just after it
     * has successfully parsed the request headers, and before starting
     * reading the request entity body.
     */
    private boolean sendAck = false;

	private HttpRequest request = null;

	private HttpResponse response = null;

    /**
     * Ack string when pipelining HTTP requests.
     */
    private static final byte[] ack = (new String("HTTP/1.1 100 Continue\r\n\r\n")).getBytes();
    
	public HttpProcessor(HttpConnector connector, int id) {
		this.connector = connector;
		this.id = id;
		this.request = connector.createRequest();
        this.response = connector.createResponse();
	}
	
	private boolean keepAlive;//��־��ǰ�����Ƿ���http1.1�ĳ־�����
	private boolean ok;//��־��ǰProcess�������Ƿ�������
	
	/**
	 * �ӵ�һ���ͻ������Ӻ�Ĵ�����
	 * @param client �ͻ��˵�����Socket
	 */
	@SuppressWarnings("deprecation")
	public void process(Socket client) {
		keepAlive = true;//Ĭ��
		ok = true;//Ĭ��
		boolean finishResponse = true;
		
		SocketInputStream inputStream = null;
		OutputStream outputStream = null;
		
		try {
			inputStream =  new SocketInputStream(client.getInputStream());
		} catch (Exception e) {
			ok = false;
			e.printStackTrace();
		}
		
		while (!stopped && ok && keepAlive) {
			finishResponse = true;

			try {
				request.setInputStream(inputStream);
				request.setResponse(response);
				outputStream = client.getOutputStream();
				response.setOutputStream(outputStream);
				response.setRequest(request);
				response.setHeader("Server", "DD_DFS");//��ӷ�������Ϣ
			} catch (Exception e) {
				ok = false;
				System.out.println("process.create.err: " + e.getMessage());
			}

			// When the previous method returns, we're actually processing a request
	        status = PROCESSOR_ACTIVE;
	        
			if(ok){
				try{
					//���������һ�����ݣ������Request����
					parseRequest(inputStream, request);
					//��������ͷ����
					parseHeaders(inputStream, request);

					if(request.getProtocol() != null && request.getProtocol().startsWith("HTTP/1.1") || 
							request.getHeader("Connection") != null && request.getHeader("Connection").equals("keep-alive")){//�����HTTP1.1Э�飬֧�ֳ־�����
                        
						if(request.getHeader("Expect") !=null && request.getHeader("Expect").equals("100-continue")){
							//������ͷ����Expect:100-continue��ѯ�ʷ������Ƿ�֧�ֽϴ��������
							//������Ӧ
							sendAck = true;
						}
						
						// Sending a request acknowledge back to the client if requested.
						ackRequest(outputStream);
						
						//����Ƿ�����ֿ�

					}else {
						keepAlive = false;
					}
				} catch (EOFException e) {
	                // It's very likely to be a socket disconnect on either the client or the server
					//[�����˳�whileѭ���Ĺؼ�]�����һ����������֮�󣬿ͻ���û���ٷ��µ�������whileѭ���ٴ�parseRequest��ʱ�򣬴�InputStream��ȡ���ݾͻᱨ����������ʹok=false
	                ok = false;
	                finishResponse = false;
	            } catch (Exception e) {
	                ok = false;
	                try {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST");
					} catch (IOException f) {
						;
					}
	            }
			}

			response.setHeader("Date", DateUtils.getCurrentDate().toGMTString());
			
			if(ok){
				try{//��������	
					if (request.getRequestURI() != null && request.getRequestURI().startsWith("/fileops/")) {
						// �������ĵ�ַ��http://127.0.0.1:6363/fileops/upload_file?filename=dd.txt&path=dir1\dir2�������Servlet����
						connector.getContainer().invoke(request, response);
					} else {// ���þ�̬��Դ����
						StaticResourceProcessor staticResourceProcessor = new StaticResourceProcessor();
						staticResourceProcessor.process(request, response);
					}
					
				} catch (Exception e) {
					ok = false;
					try {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
					} catch (IOException f) {
						;
					}
				}
			}
			
			// Finish up the handling of the request
            if (finishResponse) {
				try{
					response.finishResponse();//����response
					request.finishRequest();//����request
					if (outputStream != null)
                        outputStream.flush();
				}catch (Exception e) {
					ok = false;
					e.printStackTrace();
				}
			}
			
            // We have to check if the connection closure has been requested
            // by the application or the response stream (in case of HTTP/1.0
            // and keep-alive).
			if ("close".equals(response.getHeader("Connection"))) {
				keepAlive = false;
			}

			// End of request processing
            status = PROCESSOR_IDLE;
            
            // Recycling the request and the response objects
            request.recycle();
            response.recycle();
		}
		
		//����whileѭ�����������
		try{
			System.out.println("=============���������============");
			shutdownInput(inputStream);
			client.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		client = null;
		
	}
	
	protected void shutdownInput(InputStream input) {
        try {
            int available = input.available();
            // skip any unread (bogus) bytes
            if (available > 0) {
                input.skip(available);
            }
        } catch (Throwable e) {
            ;
        }
    }

	/**
	 * ��������ͷ����
	 * @param inputStream
	 * @param request
	 * @throws IOException 
	 */
	private void parseHeaders(SocketInputStream inputStream, HttpRequest request) throws IOException {
		String header = null;
		while((header = inputStream.readHeader()) != null){
			int separater = header.indexOf(":");
			String name = header.substring(0, separater);
			String value = header.substring(separater+1);
			//*********����*********
			System.out.println(name+"��"+value);
			//һЩ���������ͷ��Ϣ��Ҫ����request��������س�Ա������ֵ
			if(name.equals("Cookie")){
				Cookie[] cookies = parseCookieHeader(value);
				for (int i = 0; i < cookies.length; i++) {
					if(cookies[i].getName().equals("jssessionid")){
						//���Cookie����jsessionid���򸲸�uri�еõ��ġ�
						//ֻ���ڽ���cookieʱ�Ż���uri��jsessionid��
						if(!request.isRequestedSessionIdFromURL()){
							request.setRequestedSessionIdFromUrl(false);
							request.setRequestedSessionIdFromCookie(true);
							request.setRequestedSessionId(cookies[i].getValue());
						}
					}
					request.addCookies(cookies[i]);
				}
			}else if(name.equals("Content-Length")){
				int length = -1;
				try{
					length = Integer.parseInt(value);
				}catch(Exception e){
					System.out.println("content-length invalued!"+value+length);
				}
				request.setContentLength(length);
			}else if(name.equals("Content-Type")){
				request.setContentType(value);
			} else if (name.equals("Connection")) {
				response.setHeader("Connection", value);
			}

			//��ӵ�request��headers��
			request.addHeader(name, value);
		}
		
	}

	/**
	 * ����name=cookie������ͷ��value����������Ϣ
	 * @param value
	 * @return
	 */
	private Cookie[] parseCookieHeader(String value) {
		//ʾ��	Cookie: username=dd;password=dd;;=
		if(value == null || value.length() == 0){
			return new Cookie[0];
		}
		
		ArrayList<Cookie> cookies = new ArrayList<Cookie>();
		while(value.length() > 0){
			int separater = value.indexOf(";");
			int indexOfEqual = value.indexOf("=");
			if(indexOfEqual != -1 && indexOfEqual < separater){
				String k = value.substring(0, indexOfEqual);
				String v = value.substring(indexOfEqual+1, separater);
				cookies.add(new Cookie(k, v));
				value = value.substring(separater+1);
			}else{//û����Ч��ֵ��
				break;
			}
		}
		
		return cookies.toArray(new Cookie[cookies.size()]);
	}
	
	/**
	 * �������������ݣ������Request����
	 * @param requestStream
	 * @param request
	 * @throws IOException 
	 */
	private void parseRequest(SocketInputStream requestStream, HttpRequest request) throws IOException {
		//��ȡ����������
		requestStream.readRequestLine();
		String method = requestStream.getMethod();
		String protocol = requestStream.getProtocol();
		String uri = requestStream.getUri();
		
		//��������ʽ��Э��汾
		request.setMethod(method);
		request.setProtocol(protocol);

		if(uri == null) return;
		//����uri�����uri�д���ѯ�����������õ�request��queryString��
		int indexOfQuestion = uri.indexOf("?");
		if(indexOfQuestion >= 0){//���uri�д���ѯ����
			request.setQueryString(uri.substring(indexOfQuestion+1));
			uri = uri.substring(0, indexOfQuestion);//���uri�а�����ѯ�ַ�������ȥ��
		}else{
			request.setQueryString(null);
		}
		
		//����uri�������http://hostname/a.html��ʽ����Ϊ/a.html��ʽ
		if(!uri.startsWith("/")){
			int pos = uri.indexOf("://");
			if(pos != -1){//��Э����
				pos = uri.indexOf('/', pos+3);
				if(pos == -1){//��ʾ����http://a.html�Ĵ��ַ
					uri = "";
				}else{//��������
					uri = uri.substring(pos);
				}
			}
		}
		
		//����uri�п��ܰ����������sessionId
		String match = ";jsessionid=";
		int indexOfSessionId = uri.indexOf(match);
		if(indexOfSessionId >= 0){//uri�а���sessionId
			String rest = uri.substring(indexOfSessionId+match.length());
			int endOfSessionId = rest.indexOf(";");
			if(endOfSessionId >= 0){
				request.setRequestedSessionId(rest.substring(0, endOfSessionId));
				rest = rest.substring(endOfSessionId);
			}else{
				request.setRequestedSessionId(rest);
				rest = "";
			}
			request.setRequestedSessionIdFromUrl(true);
			uri = uri.substring(0, indexOfSessionId)+rest;
		}else{
			request.setRequestedSessionId(null);
			request.setRequestedSessionIdFromUrl(false);
		}
		
		request.setRequestURI(uri);
		
	}

    /**
     * Send a confirmation that a request has been processed when pipelining.
     * HTTP/1.1 100 Continue is sent back to the client.
     *
     * @param output Socket output stream
     */   
    private void ackRequest(OutputStream output) throws IOException {
        if (sendAck)
            output.write(ack);
    }
	
	
	/**
	 * �����̣߳���ʼ�����ӵ��Ŀͻ���Socket
	 * @param client
	 */
	public synchronized void assign(Socket client) {
		while (isAvalible){//�ȱ�Processor����������Socket��������ҪһЩʱ��
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.client = client;
		this.isAvalible = true;
		notifyAll();//������await�����ȴ����߳�
	}

	
	/**
	 * �����̣߳��ȴ����õĿͻ���Socket
	 * @return
	 */
	public synchronized Socket await() {
		while (!isAvalible){//�ȴ����洫��Socket
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		Socket avalibleSocket = this.client;//���õ��ݴ���еĴ�������Ʒ
		this.isAvalible = false;//֪ͨassign���Է����µĲ�Ʒ��
		notifyAll();//���isAvalible�ı�֮ǰ���Ѿ����߳���assign��wait�ˣ���Ͻ��������ǣ�
					//����Ҫ�ȴ�����wait��ʱ�󣬲Ż�ȥ�ж�isAvalible����ʡ�ȴ�ʱ��
		return avalibleSocket;
	}
	
	@Override
	public void run() {
		while(!stopped){
			//�ȴ�Socket
			Socket clientSocket = await();
			if(clientSocket == null) continue;
		
			//����
			this.process(clientSocket);
			
			//���ձ��߳�
			connector.recycle(this);
		}
		
		// Tell threadStop() we have shut ourselves down successfully
        synchronized (threadSync) {
            threadSync.notifyAll();
        }
	}
	
	/**
     * Start the background processing thread.
     */
    private void threadStart() {

        System.out.println("httpProcessor.starting");

        String threadName = "HttpProcessor[HttpProcessor v1.0]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }


    /**
     * Stop the background processing thread.
     */
    private void threadStop() {

		System.out.println("httpProcessor.stopping");

		stopped = true;
		assign(null);

		if (status != PROCESSOR_IDLE) {
			// Only wait if the processor is actually processing a command
			synchronized (threadSync) {
				try {
					threadSync.wait(5000);//�ӳ�5s
				} catch (InterruptedException e) {
					;
				}
			}
		}
		thread = null;

    }


	// =============������Lifecycle�ӿ���غ���=============
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}


	@Override
	public void start() {
		if (started) {// �ѱ�����
			return;
		}
		
		//�����¼���START_EVENT
		lifecycle.invoke(START_EVENT, null);
		
		started = true;

		threadStart();
	}


	@Override
	public void stop() {
		if(!started){//δ����
			return;
		}
		
		//�����¼���STOP_EVENT
		this.lifecycle.invoke(STOP_EVENT, null);
		
        started = false;

        threadStop();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
