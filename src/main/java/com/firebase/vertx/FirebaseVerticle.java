package com.firebase.vertx;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.VertxFirebase;
import org.vertx.java.core.Future;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;

import static com.firebase.client.Firebase.AuthResultHandler;

/**
 * @author nhudak
 */
public class FirebaseVerticle extends Verticle implements AuthResultHandler{
    Firebase ref;

    private Future<Void> started;

    @Override
    public void start(Future<Void> startedResult) {
        this.started = startedResult;
        JsonObject config = container.config();

        if (!config.containsField( "ref" )) {
            startedResult.setFailure(new VertxException("ref is not configured"));
            return;
        }

        ref = new VertxFirebase(this, config.getString( "ref" ));

        authenticate( ref, config.getValue( "auth" ) );
    }

    void authenticate( Firebase ref, Object auth ) {
        if ( auth == null ) {
            onAuthenticated( null );
        } else if ( auth instanceof JsonObject ) {
            JsonObject authMap = (JsonObject) auth;
            if (authMap.containsField( "email" ) && authMap.containsField( "password" )) {
                ref.authWithPassword( authMap.getString( "email" ), authMap.getString( "password" ), this );
            } else {
                String provider = authMap.getString( "oauth", "google" );
                Map<String, String> options = normalizeOptions( authMap.getObject( "options", new JsonObject() ) );
                options.put( "access_token", authMap.getString( "access_token", options.get( "access_token" ) ) );
                ref.authWithOAuthToken( provider, options, this );
            }
        } else if (auth instanceof String) {
            String authString = (String) auth;
            if (authString.equalsIgnoreCase("anonymous")) {
                ref.authAnonymously( this );
            } else {
                ref.authWithCustomToken( authString, this );
            }
        } else {
            started.setFailure( new VertxException("Unable to authenticate given $auth") );
        }
    }

    private Map<String, String> normalizeOptions( JsonObject options ) {
        Map<String, String> normalized = new HashMap<>();
        for ( String field : options.getFieldNames() ) {
            normalized.put( field, options.getString( field ) );
        }
        return normalized;
    }

    @Override public void onAuthenticated( AuthData authData ) {
        started.setResult( null );
    }

    @Override public void onAuthenticationError( FirebaseError firebaseError ) {
        started.setFailure( firebaseError.toException() );
    }
}
