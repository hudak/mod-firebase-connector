package com.firebase.client;

import com.firebase.vertx.FirebaseVerticle;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;

import java.util.concurrent.ScheduledFuture;

/**
 * @author nhudak
 */
public class VertxConfig extends Config {
  private final FirebaseVerticle owner;
  private final VertxRunner runner;
  private final Logger logger;

  public VertxConfig( FirebaseVerticle firebaseVerticle ) {
    owner = firebaseVerticle;
    runner = new VertxRunner();
    logger = new VertxLogger();
    this.setLogger( logger );
    this.setEventTarget( runner );
    this.setRunLoop( runner );
  }

  private class VertxRunner implements RunLoop, EventTarget {
    private final Context context = owner.getVertx().currentContext();

    @Override public void scheduleNow( final Runnable runnable ) {
      context.runOnContext( new Handler<Void>() {
        @Override public void handle( Void event ) {
          runnable.run();
        }
      } );
    }

    @Override public ScheduledFuture schedule( final Runnable runnable, long timeout ) {
      VertxScheduledFuture future = new VertxScheduledFuture( owner.getVertx(), timeout, runnable );
      scheduleNow( future );
      return future;
    }

    @Override public void postEvent( Runnable runnable ) {
      scheduleNow( runnable );
    }

    @Override public void shutdown() {
      owner.getContainer().logger().warn( "Vertx Runner asked to shut down" );
    }

    @Override public void restart() {
      owner.getContainer().logger().warn( "Vertx Runner asked to restart" );
    }
  }

  private class VertxLogger implements Logger {
    private final org.vertx.java.core.logging.Logger logger = owner.getContainer().logger();

    @Override public void onLogMessage( Level level, String tag, String message, long timestamp ) {
      switch ( level ){
        case DEBUG:
          logger.debug( message );
          break;
        case INFO:
          logger.info( message );
          break;
        case WARN:
          logger.warn( message );
          break;
        case ERROR:
          logger.error( message );
          break;
      }
    }

    @Override public Level getLogLevel() {
      if( logger.isDebugEnabled() ) {
        return Level.DEBUG;
      } else if( logger.isInfoEnabled() ) {
        return Level.INFO;
      } else {
        return Level.WARN;
      }
    }
  }
}
