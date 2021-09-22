package asia.leadsgen.psp.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.server.vertical.PSPVertical;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Created by HungDX on 21-April-16.
 */
public class PSPServer implements Runnable {

    private int workerPoolSize;
    private long workerMaxExecuteTime;
    private int eventLoopPoolSize;
    private long eventLoopMaxExecuteTime;
    private long threadCheckInterval;
    private int serverPort;
    private PSPVertical vertxVertical;
    

        public void setServerPort(int serverPort) {
                this.serverPort = serverPort;
        }

    public void setWorkerPoolSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
    }

    public void setEventLoopPoolSize(int eventLoopPoolSize) {
        this.eventLoopPoolSize = eventLoopPoolSize;
    }

    public void setThreadCheckInterval(long threadCheckInterval) {
        this.threadCheckInterval = threadCheckInterval;
    }

    public void setWorkerMaxExecuteTime(long workerMaxExecuteTime) {
        this.workerMaxExecuteTime = workerMaxExecuteTime;
    }

    public void setEventLoopMaxExecuteTime(long eventLoopMaxExecuteTime) {
        this.eventLoopMaxExecuteTime = eventLoopMaxExecuteTime;
    }

    public void setVertxVertical(PSPVertical vertxVertical) {
        this.vertxVertical = vertxVertical;
    }

    public void init() throws InterruptedException {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {

            VertxOptions vertxOptions = new VertxOptions();

            vertxOptions.setWorkerPoolSize(workerPoolSize);
            vertxOptions.setMaxWorkerExecuteTime(workerMaxExecuteTime);
            vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
            vertxOptions.setMaxEventLoopExecuteTime(eventLoopMaxExecuteTime);
            vertxOptions.setBlockedThreadCheckInterval(threadCheckInterval);
            vertxVertical.setServerPort(this.serverPort);
            Vertx.vertx(vertxOptions).deployVerticle(vertxVertical);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PSPServer.class.getName());
}
