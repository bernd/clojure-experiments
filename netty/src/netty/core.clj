(ns netty.core
  (:gen-class)
  (:import
    [java.net InetSocketAddress]
    [java.util.concurrent Executors]
    [java.nio.charset Charset]
    [org.jboss.netty.bootstrap ServerBootstrap]
    [org.jboss.netty.channel Channels
                             ChannelPipelineFactory
                             SimpleChannelHandler]
    [org.jboss.netty.channel.socket.nio NioServerSocketChannelFactory]
    [org.jboss.netty.buffer ChannelBuffers]
    [org.jboss.netty.handler.codec.http HttpRequestDecoder
                                        HttpResponseEncoder
                                        HttpVersion
                                        HttpResponseStatus
                                        DefaultHttpResponse]))

(defn to-buf [s]
  (ChannelBuffers/copiedBuffer s (Charset/forName "UTF-8")))

(defn nsscf []
  (NioServerSocketChannelFactory.
    (Executors/newCachedThreadPool)
    (Executors/newCachedThreadPool)))

(defn pipeline-factory [handler]
  (proxy [ChannelPipelineFactory] []
    (getPipeline []
      (let [pipeline (Channels/pipeline)]
        (.addLast pipeline "decoder" (HttpRequestDecoder.))
        (.addLast pipeline "encoder" (HttpResponseEncoder.))
        (.addLast pipeline "handler" (handler))
        pipeline))))

(defn start [port handler]
  (let [channel-factory (nsscf)
        bootstrap (ServerBootstrap. channel-factory)
        pipeline (.setPipelineFactory bootstrap (pipeline-factory handler))]
    (doto bootstrap
      (.setOption "child.tcpNoDelay" true)
      (.setOption "child.keepAlive" true))
    (println "Listening on port:" port)
    (.bind bootstrap (InetSocketAddress. port))
    pipeline))

(defn http-handler []
  (proxy [SimpleChannelHandler] []
    (channelConnected [ctx e]
      (println "Connected"))
    (channelDisconnected [ctx e]
      (println "Disconnected"))
    (messageReceived [ctx e]
      (let [channel (.getChannel e)
            request (.getMessage e)
            response (DefaultHttpResponse. HttpVersion/HTTP_1_1 HttpResponseStatus/OK)]
        (doto response
          (.addHeader "Connection" "close")
          (.setContent (to-buf "Hello World!\n")))
        (doto channel
          (.write response)
          (.close))))
    (exceptionCaught [ctx e]
      (let [throwable (.getCause e)]
        (println (.getMessage throwable)))
      (-> e .getChannel .close))))

(defn -main []
  (start 3000 http-handler))
