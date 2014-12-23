package com.firebase.vertx;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.VertxFirebase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * @author nhudak
 */
public class FirebaseVerticle extends Verticle {
  Firebase ref;

  @Override
  public void start( final Future<Void> startedResult ) {
    final JsonObject config = container.config();

    if ( !config.containsField( "ref" ) ) {
      startedResult.setFailure( new VertxException( "ref is not configured" ) );
      return;
    }

    ref = new VertxFirebase( this, config.getString( "ref" ) );

    Authenticator authenticator = new Authenticator( ref, config.getValue( "auth" ) );
    vertx.runOnContext( authenticator );
    authenticator.getFuture().setHandler( new AsyncResultHandler<AuthData>() {
      @Override public void handle( AsyncResult<AuthData> authEvent ) {
        startedResult.setResult( null );
      }
    } );
  }
}
