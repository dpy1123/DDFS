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
	 * 表示此processor没有正在处理的请求
	 */
	private static final int PROCESSOR_IDLE = 0;
	/**
	 * 表示此processor正在处理中
	 */
	private static final int PROCESSOR_ACTIVE = 1;

	private HttpConnector connector = null;//保存与本处理类相关的连接器
	/**
	 * The identifier of this processor, unique per connector.
	 */
	private int id = 0;
	
	private boolean stopped = false;//标识该线程是否已停止
	private Socket client = null;//当前传入的客户端连接，注意不是解析时用的，是作为产品的暂存队列
	private boolean isAvalible;//标志当前线程是否有Socket可以解析
	
    private boolean started = false;//记录本容器是否启动
    private LifecycleSupport lifecycle = new LifecycleSupport(this);//生命周期管理工具
    
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
	
	private boolean keepAlive;//标志当前连接是否是http1.1的持久连接
	private boolean ok;//标志当前Process过程中是否发生错误
	
	/**
	 * 接到一个客户端连接后的处理方法
	 * @param client 客户端的连接Socket
	 */
	@SuppressWarnings("deprecation")
	public void process(Socket client) {
		keepAlive = true;//默认
		ok = true;//默认
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
				response.setHeader("Server", "DD_DFS");//添加服务器信息
			} catch (Exception e) {
				ok = false;
				System.out.println("process.create.err: " + e.getMessage());
			}

			// When the previous method returns, we're actually processing a request
	        status = PROCESSOR_ACTIVE;
	        
			if(ok){
				try{
					//解析请求第一行内容，并填充Request对象
					parseRequest(inputStream, request);
					//解析请求头内容
					parseHeaders(inputStream, request);

					if(request.getProtocol() != null && request.getProtocol().startsWith("HTTP/1.1") || 
							request.getHeader("Connection") != null && request.getHeader("Connection").equals("keep-alive")){//如果是HTTP1.1协议，支持持久连接
                        
						if(request.getHeader("Expect") !=null && request.getHeader("Expect").equals("100-continue")){
							//且请求头带了Expect:100-continue，询问服务器是否支持较大的请求体
							//发送响应
							sendAck = true;
						}
						
						// Sending a request acknowledge back to the client if requested.
						ackRequest(outputStream);
						
						//检查是否允许分块

					}else {
						keepAlive = false;
					}
				} catch (EOFException e) {
	                // It's very likely to be a socket disconnect on either the client or the server
					//[这是退出while循环的关键]如果在一次请求处理完之后，客户端没有再发新的请求，在while循环再次parseRequest的时候，从InputStream读取内容就会报错，进入这里使ok=false
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
				try{//处理请求	
					if (request.getRequestURI() != null && request.getRequestURI().startsWith("/fileops/")) {
						// 如果请求的地址是http://127.0.0.1:6363/fileops/upload_file?filename=dd.txt&path=dir1\dir2，则调用Servlet处理
						connector.getContainer().invoke(request, response);
					} else {// 调用静态资源处理
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
					response.finishResponse();//结束response
					request.finishRequest();//结束request
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
		
		//结束while循环，处理结束
		try{
			System.out.println("=============请求处理结束============");
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
	 * 解析请求头内容
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
			//*********测试*********
			System.out.println(name+"："+value);
			//一些特殊的请求头信息，要设置request对象中相关成员变量的值
			if(name.equals("Cookie")){
				Cookie[] cookies = parseCookieHeader(value);
				for (int i = 0; i < cookies.length; i++) {
					if(cookies[i].getName().equals("jssessionid")){
						//如果Cookie中有jsessionid，则覆盖uri中得到的。
						//只有在禁用cookie时才会用uri传jsessionid。
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

			//添加到request的headers中
			request.addHeader(name, value);
		}
		
	}

	/**
	 * 解析name=cookie的请求头中value所包含的信息
	 * @param value
	 * @return
	 */
	private Cookie[] parseCookieHeader(String value) {
		//示例	Cookie: username=dd;password=dd;;=
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
			}else{//没有有效键值对
				break;
			}
		}
		
		return cookies.toArray(new Cookie[cookies.size()]);
	}
	
	/**
	 * 解析请求行内容，并填充Request对象
	 * @param requestStream
	 * @param request
	 * @throws IOException 
	 */
	private void parseRequest(SocketInputStream requestStream, HttpRequest request) throws IOException {
		//读取请求行内容
		requestStream.readRequestLine();
		String method = requestStream.getMethod();
		String protocol = requestStream.getProtocol();
		String uri = requestStream.getUri();
		
		//设置请求方式和协议版本
		request.setMethod(method);
		request.setProtocol(protocol);

		if(uri == null) return;
		//处理uri，如果uri有带查询参数，则设置到request的queryString中
		int indexOfQuestion = uri.indexOf("?");
		if(indexOfQuestion >= 0){//如果uri有带查询参数
			request.setQueryString(uri.substring(indexOfQuestion+1));
			uri = uri.substring(0, indexOfQuestion);//如果uri中包含查询字符串，则去掉
		}else{
			request.setQueryString(null);
		}
		
		//处理uri，如果是http://hostname/a.html形式，改为/a.html形式
		if(!uri.startsWith("/")){
			int pos = uri.indexOf("://");
			if(pos != -1){//有协议名
				pos = uri.indexOf('/', pos+3);
				if(pos == -1){//表示形如http://a.html的错地址
					uri = "";
				}else{//有主机名
					uri = uri.substring(pos);
				}
			}
		}
		
		//处理uri中可能包含的请求的sessionId
		String match = ";jsessionid=";
		int indexOfSessionId = uri.indexOf(match);
		if(indexOfSessionId >= 0){//uri中包含sessionId
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
	 * 唤醒线程，开始解析接到的客户端Socket
	 * @param client
	 */
	public synchronized void assign(Socket client) {
		while (isAvalible){//等本Processor获得了所需的Socket对象，这需要一些时间
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.client = client;
		this.isAvalible = true;
		notifyAll();//唤醒因await方法等待的线程
	}

	
	/**
	 * 阻塞线程，等待可用的客户端Socket
	 * @return
	 */
	public synchronized Socket await() {
		while (!isAvalible){//等待外面传入Socket
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		Socket avalibleSocket = this.client;//先拿到暂存队列的待解析产品
		this.isAvalible = false;//通知assign可以放入新的产品了
		notifyAll();//如果isAvalible改变之前，已经有线程在assign中wait了，则赶紧唤醒他们，
					//否则要等待他们wait超时后，才会去判断isAvalible，节省等待时间
		return avalibleSocket;
	}
	
	@Override
	public void run() {
		while(!stopped){
			//等待Socket
			Socket clientSocket = await();
			if(clientSocket == null) continue;
		
			//解析
			this.process(clientSocket);
			
			//回收本线程
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
					threadSync.wait(5000);//延迟5s
				} catch (InterruptedException e) {
					;
				}
			}
		}
		thread = null;

    }


	// =============以下是Lifecycle接口相关函数=============
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
		if (started) {// 已被启动
			return;
		}
		
		//触发事件：START_EVENT
		lifecycle.invoke(START_EVENT, null);
		
		started = true;

		threadStart();
	}


	@Override
	public void stop() {
		if(!started){//未启动
			return;
		}
		
		//触发事件：STOP_EVENT
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
