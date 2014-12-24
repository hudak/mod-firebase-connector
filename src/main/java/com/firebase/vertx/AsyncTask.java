package com.firebase.vertx;

import org.vertx.java.core.Context;
import org.vertx.java.core.Future;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.impl.DefaultFutureResult;

/**
 * @author nhudak
 */
public abstract class AsyncTask<T> {

  private final Future<T> future = new DefaultFutureResult<>();

  public final void runOnContext( final Context context ) {
    context.runOnContext( new VoidHandler() {
      @Override protected void handle() {
        execute( context, future );
      }
    } );
  }

  abstract void execute( Context context, Future<T> future );

  public Future<T> getFuture() {
    return future;
  }
}
