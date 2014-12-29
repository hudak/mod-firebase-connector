package test.integration.java;

import com.firebase.vertx.FirebaseVerticle;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

/**
 * @author nhudak
 */
public class FirebaseTest extends TestVerticle {

  JsonObject testConfig;

  @Override public void start() {
    testConfig = new JsonObject( vertx.fileSystem().readFileSync( "testConfig.json" ).toString() );
    super.start();
  }

  @Test public void secretAuthentication() {
    JsonObject config = new JsonObject();
    config.putString( "ref", testConfig.getString( "ref" ) );
    config.putString( "auth", testConfig.getString( "secret" ) );

    container.deployVerticle( FirebaseVerticle.class.getName(), config, new AsyncResultHandler<String>() {
      @Override public void handle( AsyncResult<String> event ) {
        if ( event.failed() ) {
          throw new AssertionError( event.cause() );
        } else {
          testComplete();
        }
      }
    } );
  }

  @Test public void insertAndQuery() {
    final String address = "firebase.root";
    final EventBus eventBus = vertx.eventBus();
    JsonObject config = new JsonObject( String.format( "{\"ref\":\"%s\", \"auth\":\"%s\", \"address\":\"%s\"}",
      testConfig.getString( "ref" ),
      testConfig.getString( "secret" ),
      address
    ) );

    container.deployVerticle( FirebaseVerticle.class.getName(), config, new AsyncResultHandler<String>() {
      @Override public void handle( AsyncResult<String> event ) {
        if ( event.failed() ) {
          throw new AssertionError( event.cause() );
        }
        JsonObject query = new JsonObject( "{\"action\":\"put\", \"value\":{\"myContainer\":{\"myValue\":42}}}" );
        eventBus.send( address, query, new Handler<Message>() {
          @Override public void handle( Message message ) {
            assertTrue( message.body() instanceof JsonObject );
            JsonObject response = (JsonObject) message.body();
            assertTrue( response.getString( "cause" ), response.getBoolean( "success" ) );
            eventBus.send( address, "myContainer/myValue", new Handler<Message>() {
              @Override public void handle( Message message ) {
                JsonObject response = (JsonObject) message.body();
                assertTrue( response.getString( "cause" ), response.getBoolean( "success" ) );
                assertEquals( 42, response.getInteger( "snapshot" ).intValue() );
                testComplete();
              }
            } );
          }
        } );
      }
    } );
  }
}
