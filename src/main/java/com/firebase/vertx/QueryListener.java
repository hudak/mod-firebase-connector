package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
import rx.functions.Func1;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nhudak
 */
public class QueryListener extends AsyncTask<Void> {
  private final Vertx vertx;
  private final String address;
  private final Firebase ref;
  private final Logger logger;

  public QueryListener( Verticle owner, Firebase ref, String address ) {
    this.vertx = owner.getVertx();
    this.logger = owner.getContainer().logger();
    this.ref = ref;
    this.address = address;
  }

  @Override void execute( Context context, final Promise<Void> future ) {
    vertx.eventBus().registerHandler( address, queryRequest, new AsyncResultHandler<Void>() {
      @Override public void handle( AsyncResult<Void> event ) {
        if ( event.failed() ) {
          future.reject( event.cause() );
        } else {
          future.fulfill( null );
        }
      }
    } );
  }

  private Handler<Message> queryRequest = new Handler<Message>() {
    @Override public void handle( final Message event ) {
      QueryAction action = null;
      Firebase target = ref;
      if ( event.body() instanceof String ) {
        target = ref.child( event.body().toString() );
        action = new Get();
      } else if ( event.body() instanceof JsonObject ) {
        JsonObject request = (JsonObject) event.body();
        if ( request.containsField( "child" ) ) {
          target = ref.child( request.getString( "child" ) );
        }
        switch ( request.getString( "action", "get" ) ) {
          case "get":
            action = new Get();
            break;
          case "put":
            if ( request.containsField( "value" ) ) {
              action = new Put( fromJson( request.getValue( "value" ) ) );
            }
            break;
          default:
            event.reply( errorMessage( "Action not recognized: " + request.getString( "action" ) ) );
        }
      }

      if ( action != null ) {
        action.call( target ).then( new PromiseAction<JsonObject>() {
          @Override public void call( JsonObject response ) {
            event.reply( response );
          }
        }, new PromiseAction<Exception>() {
          @Override public void call( Exception e ) {
            logger.warn( "Query failed", e );
            event.reply( errorMessage( e.getMessage() ) );
          }
        } );
      } else {
        event.reply( errorMessage( "Invalid Query: " + event.body().toString() ) );
      }
    }
  };

  private static Object fromJson( Object value ) {
    if( value instanceof JsonObject ) {
      return ( (JsonObject) value ).toMap();
    } else if ( value instanceof JsonArray ) {
      return ( (JsonArray) value ).toArray();
    } else {
      return value;
    }
  }

  private static Object toJson( Object value ) {
    if ( value instanceof Map ) {
      LinkedHashMap<String, Object> map = new LinkedHashMap<>();
      for ( Object key : ( (Map) value ).keySet()) {
        map.put( key.toString(), ( (Map) value ).get( key ) );
      }
      value = new JsonObject( map );
    } else if ( value instanceof List ) {
      value = new JsonArray( (List) value );
    } else if ( value instanceof Object[] ) {
      value = new JsonArray( (Object[]) value );
    }
    return value;
  }

  private interface QueryAction extends Func1<Firebase, Promise<JsonObject>> {
    @Override Promise<JsonObject> call( Firebase ref );
  }

  private class Get implements QueryAction {
    @Override public Promise<JsonObject> call( Firebase ref ) {
      final Promise<JsonObject> promise = new Promise<>();
      ref.addListenerForSingleValueEvent( new ValueEventListener() {
        @Override public void onDataChange( DataSnapshot dataSnapshot ) {
          promise.fulfill( queryResponse( dataSnapshot ) );
        }
        @Override public void onCancelled( FirebaseError firebaseError ) {
          promise.reject( firebaseError.toException() );
        }
      } );
      return promise;
    }
  }

  private class Put implements QueryAction {
    private final Object value;

    public Put( Object value ) {
      this.value = value;
    }

    @Override public Promise<JsonObject> call( Firebase ref ) {
      final Promise<JsonObject> promise = new Promise<>();
      ref.setValue( value, new Firebase.CompletionListener() {
        @Override public void onComplete( FirebaseError firebaseError, Firebase firebase ) {
          if ( firebaseError == null ) {
            promise.fulfill( queryResponse( firebase.getKey(), toJson( value ) ) );
          } else {
            promise.reject( firebaseError.toException() );
          }
        }
      } );
      return promise;
    }
  }

  private JsonObject errorMessage( String cause ) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.putBoolean( "success", false );
    jsonObject.putString( "cause", cause );
    return jsonObject;
  }

  private JsonObject queryResponse( DataSnapshot dataSnapshot ) {
    return queryResponse( dataSnapshot.getKey(), toJson( dataSnapshot.getValue() ) );
  }

  private JsonObject queryResponse( String key, Object value ) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.putBoolean( "success", true );
    jsonObject.putString( "id", key );
    jsonObject.putValue( "snapshot", value );
    return jsonObject;
  }
}
