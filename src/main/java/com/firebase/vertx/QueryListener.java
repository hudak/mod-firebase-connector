package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.firebase.client.Firebase;
import com.nickhudak.vertx.promises.AsyncTask;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

import static com.nickhudak.vertx.json.json.ToJson.toJson;

/**
 * @author nhudak
 */
public class QueryListener extends AsyncTask<Void> {
  private final String address;
  private final Firebase ref;

  public QueryListener( Firebase ref, String address ) {
    this.ref = ref;
    this.address = address;
  }

  @Override protected void execute( final Promise<Void> future ) {
    getVertx().eventBus().registerHandler( address, new RequestHandler(), new AsyncResultHandler<Void>() {
      @Override public void handle( AsyncResult<Void> event ) {
        if ( event.failed() ) {
          future.reject( event.cause() );
        } else {
          getLogger().info( MessageFormat.format( "Listening on {0} for query requests", address ) );
          future.fulfill( null );
        }
      }
    } );
  }

  private JsonObject errorMessage( String cause ) {
    return new JsonObject().
      putBoolean( "success", false ).
      putString( "cause", cause );
  }

  private JsonObject queryResponse( String key, Object value ) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.putBoolean( "success", true );
    if ( key != null ) {
      jsonObject.putString( "id", key );
    }
    if ( value != null ) {
      jsonObject.putValue( "snapshot", toJson( value ) );
    }
    return jsonObject;
  }

  private QueryAction getQueryAction( Object request ) {
    QueryAction action = null;
    if ( request instanceof String ) {
      action = QueryAction.get( ref.child( (String) request ) );
    } else if ( request instanceof JsonObject ) {
      Map<String, Object> requestMap = ( (JsonObject) request ).toMap();

      Firebase target;
      if ( requestMap.get( "child" ) instanceof String ) {
        target = ref.child( (String) requestMap.get( "child" ) );
      } else {
        target = ref;
      }
      Object value = requestMap.get( "value" );

      String actionType = Objects.toString( requestMap.get( "action" ), "get" );
      switch ( actionType ) {
        case "get":
          action = QueryAction.get( target );
          break;
        case "put":
          if ( requestMap.containsKey( "value" ) ) {
            action = QueryAction.put( target, value );
          } else {
            getLogger().error( "Value to put not specified" );
          }
          break;
        case "updateChildren":
          if ( value instanceof Map ) {
            action = QueryAction.updateChildren( target, ( (JsonObject) request ).getObject( "value" ).toMap() );
          } else {
            getLogger().error( MessageFormat.format( "Unable to update children with value {0}", value ) );
          }
          break;
        case "push":
          target = target.push();
          if ( requestMap.containsKey( "value" ) ) {
            action = QueryAction.put( target, value );
          } else {
            action = QueryAction.get( target );
          }
          break;
        default:
          getLogger().error( MessageFormat.format( "Action not recognized: {0}", actionType ) );
      }
    } else {
      getLogger().error( MessageFormat.format( "Unsupported request type: {0}", request.getClass() ) );
    }
    return action;
  }

  class RequestHandler implements Handler<Message> {
    @Override public void handle( final Message event ) {
      QueryAction action = getQueryAction( event.body() );

      if ( action != null ) {
        action.runOnContext( QueryListener.this ).
          then( new PromiseAction<QueryAction.Response>() {
            @Override public void call( QueryAction.Response response ) {
              event.reply( queryResponse( response.id, response.value ) );
            }
          } ).
          fail( new PromiseAction<Exception>() {
            @Override public void call( Exception e ) {
              getLogger().error( "Query failed", e );
              event.reply( errorMessage( e.getMessage() ) );
            }
          } );
      } else {
        String message = MessageFormat.format( "Invalid Query: {0}", event.body() );
        getLogger().error( "Query failed" );
        event.reply( errorMessage( message ) );
      }
    }
  }
}
