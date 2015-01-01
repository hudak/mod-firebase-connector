package com.nickhudak.vertx.promises;

import com.darylteo.vertx.promises.java.Promise;
import org.vertx.java.core.Context;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

/**
 * @author nhudak
 */
public abstract class AsyncTask<T> {

  private Vertx vertx;
  private Container container;
  private Context context;

  public Promise<T> runOnContext( Verticle owner ) {
    vertx = owner.getVertx();
    container = owner.getContainer();
    return runOnContext( owner.getVertx().currentContext() );
  }

  public Promise<T> runOnContext( AsyncTask<?> owner ) {
    vertx = owner.vertx;
    container = owner.container;
    return runOnContext( owner.context );
  }

  private Promise<T> runOnContext( final Context context ) {
    final Promise<T> promise = new Promise<>();
    this.context = context;
    context.runOnContext( new VoidHandler() {
      @Override protected void handle() {
        try {
          execute( promise );
        } catch ( Exception e ) {
          promise.reject( e );
        }
      }
    } );
    return promise;
  }

  abstract protected void execute( Promise<T> future );

  protected Vertx getVertx() {
    return vertx;
  }

  protected Container getContainer() {
    return container;
  }

  protected Logger getLogger() {
    return container.logger();
  }
}
