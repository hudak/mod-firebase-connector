package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.VertxFirebase;
import com.google.common.collect.Lists;
import com.nickhudak.vertx.promises.Promises;
import org.vertx.java.core.Future;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.text.MessageFormat;
import java.util.List;

/**
 * @author nhudak
 */
public class FirebaseVerticle extends Verticle {
  @Override
  public void start( final Future<Void> startedResult ) {
    final JsonObject config = container.config();

    if ( !config.containsField( "ref" ) ) {
      startedResult.setFailure( new VertxException( "ref is not configured" ) );
      return;
    }

    final Firebase ref = new VertxFirebase( this, config.getString( "ref" ) );

    Promise<AuthData> authPromise;
    if ( config.containsField( "auth" ) ) {
      authPromise = new Authenticator( ref, config.getValue( "auth" ) ).runOnContext( this );
    } else {
      authPromise = Promises.fulfilled( null );
    }

    authPromise.
      then( new RepromiseFunction<AuthData, List<Void>>() {
        @Override public Promise<List<Void>> call( AuthData authData ) {
          container.logger().debug( MessageFormat.format( "authData: {0}", authData ) );
          List<Promise<Void>> startup = Lists.newArrayList();
          if ( config.containsField( "address" ) ) {
            String address = config.getValue( "address" ).toString();
            startup.add( new QueryListener( ref, address ).runOnContext( FirebaseVerticle.this ) );
          }

          return Promises.all( startup );
        }
      } ).
      then( new PromiseAction<List<Void>>() {
        @Override public void call( List<Void> voids ) {
          startedResult.setResult( null );
        }
      } ).
      fail( new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          container.logger().error( "Firebase Verticle failed to start", e );
          startedResult.setFailure( e );
        }
      } );
  }
}
