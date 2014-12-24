package com.firebase.vertx;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import org.vertx.java.core.Context;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nhudak
 */
public class Authenticator extends AsyncTask<AuthData> {
  private final Firebase ref;
  private final Object config;
  private Firebase.AuthResultHandler authResultHandler;

  public Authenticator( Firebase ref, Object config ) {
    this.ref = ref;
    this.config = config;
  }

  @Override void execute( Context context, final Future<AuthData> future ) {
    authResultHandler = new Firebase.AuthResultHandler(){
      @Override public void onAuthenticated( AuthData authData ) {
        future.setResult( authData );
      }
      @Override public void onAuthenticationError( FirebaseError firebaseError ) {
        future.setFailure( firebaseError.toException() );
      }
    };

    if ( config == null ) {
      future.setResult( null );
    } else if ( config instanceof JsonObject ) {
      JsonObject authMap = (JsonObject) config;
      if (authMap.containsField( "email" ) && authMap.containsField( "password" )) {
        ref.authWithPassword( authMap.getString( "email" ), authMap.getString( "password" ), authResultHandler );
      } else if ( authMap.containsField( "oauth" ) ) {
        String provider = authMap.getString( "oauth" );
        Map<String, String> options = new HashMap<>();
        JsonObject jsonObject = authMap.getObject( "options", new JsonObject() );
        for( String field : jsonObject.getFieldNames() ) {
          options.put( field, jsonObject.getObject( field ).toString() );
        }
        options.put( "access_token", authMap.getString( "access_token", options.get( "access_token" ) ) );
        ref.authWithOAuthToken( provider, options, authResultHandler );
      } else {
        future.setFailure( new RuntimeException( "Unable to authenticate with " + authMap ) );
      }
    } else if ( config instanceof String) {
      String authString = (String) config;
      if (authString.equalsIgnoreCase("anonymous")) {
        ref.authAnonymously( authResultHandler );
      } else {
        ref.authWithCustomToken( authString, authResultHandler );
      }
    } else {
      future.setFailure( new RuntimeException( "Unable to authenticate with " + config ) );
    }
  }
}
