package com.firebase.vertx;

import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.VertxFirebase;
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

    new Authenticator( ref, config.getValue( "auth" ) ).
      runOnContext( vertx.currentContext() ).
      then( new PromiseAction<AuthData>() {
        @Override public void call( AuthData authData ) {
          startedResult.setResult( null );
        }
      }, new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          startedResult.setFailure( e );
        }
      } );
  }
}
