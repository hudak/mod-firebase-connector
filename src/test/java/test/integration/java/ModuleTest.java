package test.integration.java;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.nickhudak.vertx.promises.Promises;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import java.text.MessageFormat;

import static org.vertx.testtools.VertxAssert.*;

/**
 * @author nhudak
 */
public class ModuleTest extends TestVerticle {

  public static final String ADDRESS = "test.integration";
  JsonObject testConfig;

  @Override public void start() {
    // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
    initialize();
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don't have to hardecode it in your tests
    String ref, secret;
    assertNotNull( "test.integration.ref not set", ref = System.getProperty( "test.integration.ref" ) );
    assertNotNull( "test.integration.secret not set", secret = System.getProperty( "test.integration.secret" ) );
    testConfig = new JsonObject().
      putString( "ref", ref ).
      putString( "auth", secret ).
      putString( "address", ADDRESS );
    container.logger().info( testConfig.copy().putString( "auth", "*****" ) );
    container.deployModule( System.getProperty( "vertx.modulename" ), testConfig, new AsyncResultHandler<String>() {
      @Override
      public void handle( AsyncResult<String> asyncResult ) {
        // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
        if ( asyncResult.failed() ) {
          fail( MessageFormat.format( "module failed to deploy: {0}", asyncResult.cause() ) );
        }
        assertTrue( asyncResult.succeeded() );
        assertNotNull( "deploymentID should not be null", asyncResult.result() );
        // If deployed correctly then start the tests!
        startTests();
      }
    } );
  }

  @Test public void insertAndQuery() {
    final JsonObject value = new JsonObject().putObject( "myContainer", new JsonObject().putNumber( "myValue", 42 ) );
    JsonObject query = new JsonObject().putString( "action", "put" ).putObject( "value", value );
    final EventBus eventBus = vertx.eventBus();
    Promise<Message<JsonObject>> promise = new Promise<>();
    container.logger().info( MessageFormat.format( "send {0}", query ) );
    eventBus.send( ADDRESS, query, promise );
    promise.
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new RepromiseFunction<JsonObject, Message<JsonObject>>() {
        @Override public Promise<Message<JsonObject>> call( JsonObject response ) {
          container.logger().info( MessageFormat.format( "response {0}", response ) );
          // Verify put
          assertTrue( response.getString( "cause" ), response.getBoolean( "success" ) );
          assertEquals( value, response.getObject( "snapshot" ) );

          // Send Query
          Promise<Message<JsonObject>> promise = new Promise<>();
          String query = "myContainer/myValue";
          container.logger().info( MessageFormat.format( "send {0}", query ) );
          eventBus.send( ADDRESS, query, promise );
          return promise;
        }
      } ).
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new PromiseAction<JsonObject>() {
        @Override public void call( JsonObject response ) {
          container.logger().info( MessageFormat.format( "response {0}", response ) );
          // Verify get
          assertTrue( response.getString( "cause" ), response.getBoolean( "success" ) );
          // Verify snapshot
          assertEquals( 42, response.getInteger( "snapshot" ).intValue() );
          testComplete();
        }
      } ).
      fail( new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          fail( e.getMessage() );
        }
      } );
  }
}
