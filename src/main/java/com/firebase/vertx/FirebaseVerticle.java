package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.VertxFirebase;
import org.vertx.java.core.Context;
import org.vertx.java.core.Future;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author nhudak
 */
public class FirebaseVerticle extends Verticle {
  @Override
  public void start( final Future<Void> startedResult ) {
    final JsonObject config = container.config();
    final Context currentContext = vertx.currentContext();

    if ( !config.containsField( "ref" ) ) {
      startedResult.setFailure( new VertxException( "ref is not configured" ) );
      return;
    }

    final Firebase ref = new VertxFirebase( this, config.getString( "ref" ) );

    Promise<AuthData> auth = new Authenticator( ref, config.getValue( "auth" ) ).runOnContext( currentContext );
    auth.then( new RepromiseFunction<AuthData, Boolean>() {
      @Override public Promise<Boolean> call( AuthData authData ) {
        container.logger().debug( authData );
        Observable<Void> startup = Observable.empty();
        if ( config.containsField( "address" ) ) {
          String address = config.getValue( "address" ).toString();
          startup.mergeWith( new QueryListener( FirebaseVerticle.this, ref, address )
              .runOnContext( currentContext )
              .toObservable()
          );
        }
        return new Promise<>( startup.all( new Func1<Void, Boolean>() {
          @Override public Boolean call( Void aVoid ) {
            return true;
          }
        } ) );
      }
    } ).fail( new PromiseAction<Exception>() {
      @Override public void call( Exception e ) {
        startedResult.setFailure( e );
      }
    } ).then( new PromiseAction<Boolean>() {
      @Override public void call( Boolean aBoolean ) {
        startedResult.setResult( null );
      }
    } );
  }
}
