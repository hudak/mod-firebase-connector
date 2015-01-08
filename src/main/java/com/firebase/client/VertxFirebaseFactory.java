package com.firebase.client;

import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.Verticle;

import java.util.concurrent.ScheduledFuture;

/**
 * @author nhudak
 */
public class VertxFirebaseFactory {
  private final Verticle owner;
  private final Context context;
  private final org.vertx.java.core.logging.Logger logger;

  public VertxFirebaseFactory( Verticle owner ) {
    this.owner = owner;
    this.context = owner.getVertx().currentContext();
    this.logger = owner.getContainer().logger();
  }

  public Firebase createRef( String ref ) {
    Config config = new Config();
    config.setLogger( new VertxLogger() );
    config.setEventTarget( new VertxRunner() );
    config.setRunLoop( new VertxRunner() );
    return new Firebase( ref, config );
  }

  private class VertxRunner implements RunLoop, EventTarget {
    @Override public void scheduleNow( final Runnable runnable ) {
      context.runOnContext( new Handler<Void>() {
        @Override public void handle( Void nothing ) {
          try {
            runnable.run();
          } catch ( Exception e ) {
            logger.error( "Uncaught exception from Firebase", e );
          }
        }
      } );
    }

    @Override public ScheduledFuture schedule( final Runnable runnable, long delay ) {
      VertxScheduledRunnable future = new VertxScheduledRunnable( owner.getVertx(), delay, runnable );
      scheduleNow( future );
      return future;
    }

    @Override public void postEvent( Runnable runnable ) {
      scheduleNow( runnable );
    }

    @Override public void shutdown() {
      logger.warn( "Vertx Runner asked to shut down" );
    }

    @Override public void restart() {
      logger.warn( "Vertx Runner asked to restart" );
    }
  }

  private class VertxLogger implements Logger {
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
