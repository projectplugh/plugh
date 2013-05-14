(ns plugh.http.server
  (:gen-class)
  (:import java.net.InetSocketAddress
           java.util.concurrent.Executors
           org.jboss.netty.bootstrap.ServerBootstrap
           org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory [org.jboss.netty.channel Channels ChannelPipelineFactory]
           [org.jboss.netty.handler.codec.http HttpRequestDecoder HttpRequestEncoder HttpContentCompressor]))


;;
;; Copyright 2012 The Netty Project
;;
;; The Netty Project licenses this file to you under the Apache License,
;; version 2.0 (the "License"); you may not use this file except in compliance
;; with the License. You may obtain a copy of the License at:
;;
;;   http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
;; WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
;; License for the specific language governing permissions and limitations
;; under the License.


(defn httpRequestHandler []
  (proxy [SimpleChannelUpstreamHandler] []
    (messageReceived [ctx evt]
      (

                                 )

                                             )))

;54  public class HttpRequestHandler extends SimpleChannelUpstreamHandler {
;                                                                           55
;                                                                           56      private HttpRequest request;
;                                                                           57      private boolean readingChunks;
;                                                                           58      /** Buffer that stores the response content */
;                                                                           59      private final StringBuilder buf = new StringBuilder();
;                                                                           60
;                                                                           61      @Override
;                                                                           62      public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
;                                                                                                                                                                             63          if (!readingChunks) {
;                                                                                                                                                                                                               64              HttpRequest request = this.request = (HttpRequest) e.getMessage();
;      65
;                                                                                                                                                                                                               66              if (is100ContinueExpected(request)) {
;                                                                                                                                                                                                                                                                     67                  send100Continue(e);
;        68              }
;                                                                                                                                                                                                               69
;                                                                                                                                                                                                               70              buf.setLength(0);
;                                                                                                                                                                                                               71              buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
;      72              buf.append("===================================\r\n");
;                                                                                                                                                                                                               73
;                                                                                                                                                                                                               74              buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
;                                                                                                                                                                                                               75              buf.append("HOSTNAME: " + getHost(request, "unknown") + "\r\n");
;      76              buf.append("REQUEST_URI: " + request.getUri() + "\r\n\r\n");
;                                                                                                                                                                                                               77
;                                                                                                                                                                                                               78              for (Map.Entry<String, String> h: request.getHeaders()) {
;                                                                                                                                                                                                                                                                                         79                  buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
;        80              }
;                                                                                                                                                                                                               81              buf.append("\r\n");
;                                                                                                                                                                                                               82
;                                                                                                                                                                                                               83              QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
;                                                                                                                                                                                                               84              Map<String, List<String>> params = queryStringDecoder.getParameters();
;      85              if (!params.isEmpty()) {
;                                               86                  for (Entry<String, List<String>> p: params.entrySet()) {
;                                                                                                                            87                      String key = p.getKey();
;                                                                                                                            88                      List<String> vals = p.getValue();
;                                                                                                                            89                      for (String val : vals) {
;                                                                                                                                                                              90                          buf.append("PARAM: " + key + " = " + val + "\r\n");
;            91                      }
;                                                                                                                            92                  }
;                                                                                                                            93                  buf.append("\r\n");
;          94              }
;                                               95
;                                               96              if (request.isChunked()) {
;                                                                                          97                  readingChunks = true;
;                                                                                          98              } else {
;                                                                                                                   99                  ChannelBuffer content = request.getContent();
;                                                                                                                   100                 if (content.readable()) {
;                                                                                                                                                                 101                     buf.append("CONTENT: " + content.toString(CharsetUtil.UTF_8) + "\r\n");
;              102                 }
;                                                                                                                   103                 writeResponse(e);
;            104             }
;                                                                                          105         } else {
;                                                                                                               106             HttpChunk chunk = (HttpChunk) e.getMessage();
;            107             if (chunk.isLast()) {
;                                                  108                 readingChunks = false;
;                                                  109                 buf.append("END OF CONTENT\r\n");
;              110
;                                                  111                 HttpChunkTrailer trailer = (HttpChunkTrailer) chunk;
;                                                  112                 if (!trailer.getHeaderNames().isEmpty()) {
;                                                                                                                 113                     buf.append("\r\n");
;                114                     for (String name: trailer.getHeaderNames()) {
;                                                                                      115                         for (String value: trailer.getHeaders(name)) {
;                                                                                                                                                                 116                             buf.append("TRAILING HEADER: " + name + " = " + value + "\r\n");
;                    117                         }
;                                                                                      118                     }
;                                                                                      119                     buf.append("\r\n");
;                  120                 }
;                                                                                                                 121
;                                                                                                                 122                 writeResponse(e);
;                123             } else {
;                                         124                 buf.append("CHUNK: " + chunk.getContent().toString(CharsetUtil.UTF_8) + "\r\n");
;                125             }
;                                                  126         }
;                                                  127     }
;                                                  128
;                                                  129     private void writeResponse(MessageEvent e) {
;                                                                                                       130         // Decide whether to close the connection or not.
;                131         boolean keepAlive = isKeepAlive(request);
;                                                                                                       132
;                                                                                                       133         // Build the response object.
;                134         HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
;                                                                                                       135         response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
;                136         response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
;                                                                                                       137
;                                                                                                       138         if (keepAlive) {
;                                                                                                                                    139             // Add 'Content-Length' header only for a keep-alive connection.
;                  140             response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
;                                                                                                                                    141         }
;                                                                                                                                    142
;                                                                                                                                    143         // Encode the cookie.
;                                                                                                                                    144         String cookieString = request.getHeader(COOKIE);
;                                                                                                                                    145         if (cookieString != null) {
;                                                                                                                                                                            146             CookieDecoder cookieDecoder = new CookieDecoder();
;                    147             Set<Cookie> cookies = cookieDecoder.decode(cookieString);
;                                                                                                                                                                            148             if(!cookies.isEmpty()) {
;                                                                                                                                                                                                                     149                 // Reset the cookies if necessary.
;                                                                                                                                                                                                                     150                 CookieEncoder cookieEncoder = new CookieEncoder(true);
;                      151                 for (Cookie cookie : cookies) {
;                                                                          152                     cookieEncoder.addCookie(cookie);
;                        153                 }
;                                                                                                                                                                                                                     154                 response.addHeader(SET_COOKIE, cookieEncoder.encode());
;                                                                                                                                                                                                                     155             }
;                                                                                                                                                                                                                     156         }
;                                                                                                                                                                                                                     157
;                                                                                                                                                                                                                     158         // Write the response.
;                                                                                                                                                                                                                     159         ChannelFuture future = e.getChannel().write(response);
;                                                                                                                                                                                                                     160
;                                                                                                                                                                                                                     161         // Close the non-keep-alive connection after the write operation is done.
;                      162         if (!keepAlive) {
;                                                    163             future.addListener(ChannelFutureListener.CLOSE);
;                        164         }
;                                                                                                                                                                                                                     165     }
;                                                                                                                                                                            166
;                                                                                                                                                                            167     private void send100Continue(MessageEvent e) {
;                                                                                                                                                                                                                                   168         HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
;                      169         e.getChannel().write(response);
;                                                                                                                                                                                                                                   170     }
;                                                                                                                                                                                                                                   171
;                                                                                                                                                                                                                                   172     @Override
;                                                                                                                                                                                                                                   173     public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
;                                                                                                                                                                                                                                   174             throws Exception {
;                                                                                                                                                                                                                                                                      175         e.getCause().printStackTrace();
;                        176         e.getChannel().close();
;                                                                                                                                                                                                                                                                      177     }
;                                                                                                                                                                                                                                                                      178 }

(defn httpServerPipelineFactory []

  (proxy [ChannelPipelineFactory] []
    (getPipeline []
      (let [pipeline (. Channels pipeline)]
        (. pipeline addLast "decoder" (new HttpRequestDecoder))
        (. pipeline addLast "encoder" (new HttpRequestEncoder))
        (. pipeline addLast "deflater" (new HttpContentCompressor))
        (. pipeline addLast "handler" (httpRequestHandler))
        pipeline
        ))))

(defn run-server [port]
  (let [bootstrap
        (new ServerBootstrap
          (new NioServerSocketChannelFactory
            (. Executors newCachedThreadPool)
            (. Executors newCachedThreadPool)))]
    (. bootstrap setPipelineFactory (httpServerPipelineFactory))
    (. bootstrap bind (new InetSocketAddress port))))

