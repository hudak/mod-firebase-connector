package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import org.vertx.java.core.Context;
import org.vertx.java.core.VoidHandler;

/**
 * @author nhudak
 */
public abstract class AsyncTask<T> {

  public final Promise<T> runOnContext( final Context context ) {
    final Promise<T> promise = new Promise<>();
    context.runOnContext( new VoidHandler() {
      @Override protected void handle() {
        execute( context, promise );
      }
    } );
    return promise;
  }

  abstract void execute( Context context, Promise<T> future );
}
