package test.integration.java;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.nickhudak.vertx.promises.Promises;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
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
  private EventBus eventBus;

  @Override public void start() {
    // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
    initialize();
    eventBus = vertx.eventBus();
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don't have to hardecode it in your tests
    String ref, secret;
    assertNotNull( "test.integration.ref not set", ref = System.getProperty( "test.integration.ref" ) );
    assertNotNull( "test.integration.secret not set", secret = System.getProperty( "test.integration.secret" ) );
    JsonObject testConfig = new JsonObject().
      putString( "ref", ref ).
      putString( "auth", secret ).
      putString( "address", ADDRESS );
    container.logger().info( testConfig.copy().putString( "auth", "*****" ) );

    // Start Module
    Promise<AsyncResult<String>> start = new Promise<>();
    container.deployModule( System.getProperty( "vertx.modulename" ), testConfig, start );
    start.
      then( Promises.<String>unwrapAsyncResult() ).
      then( new RepromiseFunction<String, Message<JsonObject>>() {
        @Override public Promise<Message<JsonObject>> call( String deploymentId ) {
          assertNotNull( "deploymentID should not be null", deploymentId );

          // Reset container
          JsonObject myContainer = new JsonObject().putObject( "myContainer", new JsonObject() );
          JsonObject query = new JsonObject().
            putString( "action", "put" ).
            putObject( "value", myContainer );

          Promise<Message<JsonObject>> promise = new Promise<>();
          container.logger().info( MessageFormat.format( "send {0}", query ) );
          eventBus.send( ADDRESS, query, promise );
          return promise;
        }
      } ).
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new RepromiseFunction<JsonObject, Void>() {
        @Override public Promise<Void> call( JsonObject response ) {
          // If deployed correctly then start the tests!
          if ( response.getBoolean( "success" ) ) {
            startTests();
            return Promises.fulfilled( null );
          } else {
            return Promises.rejected( new Exception( response.getString( "cause" ) ) );
          }
        }
      } ).
      fail( new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          fail( e.getMessage() );
        }
      } );
  }

  @Test public void testSetAndUpdateValue() {
    final JsonObject value = new JsonObject().putNumber( "myValue", 42 );
    final JsonObject update = new JsonObject().putString( "myId", "xkcd" );
    final JsonObject finalValue = new JsonObject().mergeIn( value ).mergeIn( update );
    JsonObject query = new JsonObject().
      putString( "action", "put" ).
      putString( "child", "myContainer" ).
      putObject( "value", value );

    Promise<Message<JsonObject>> promise = new Promise<>();
    container.logger().info( MessageFormat.format( "send {0}", query ) );
    eventBus.send( ADDRESS, query, promise );
    promise.
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new RepromiseFunction<JsonObject, Message<JsonObject>>() {
        @Override public Promise<Message<JsonObject>> call( JsonObject response ) {
          container.logger().info( MessageFormat.format( "response {0}", response ) );
          // Verify put
          if ( !response.getBoolean( "success" ) ) {
            return Promises.rejected( new Exception( response.getString( "cause" ) ) );
          }
          assertEquals( value, response.getObject( "snapshot" ) );

          // Update value
          JsonObject query = new JsonObject().
            putString( "action", "updateChildren" ).
            putString( "child", "myContainer" ).
            putObject( "value", update );
          Promise<Message<JsonObject>> promise = new Promise<>();
          container.logger().info( MessageFormat.format( "send {0}", query ) );
          eventBus.send( ADDRESS, query, promise );
          return promise;
        }
      } ).
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new RepromiseFunction<JsonObject, Message<JsonObject>>() {
        @Override public Promise<Message<JsonObject>> call( JsonObject response ) {
          container.logger().info( MessageFormat.format( "response {0}", response ) );
          if ( !response.getBoolean( "success" ) ) {
            return Promises.rejected( new Exception( response.getString( "cause" ) ) );
          }
          // Verify update
          assertEquals( finalValue, response.getObject( "snapshot" ) );

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

  @Test public void testPushValue() {
    final JsonObject value = new JsonObject().putString( "myValue", "some such" ).putNumber( "myNumber", 42 );
    // Send Push
    Promise<Message<JsonObject>> promise = new Promise<>();
    JsonObject query = new JsonObject().
      putString( "action", "push" ).
      putString( "child", "myContainer" ).
      putObject( "value", value );
    container.logger().info( MessageFormat.format( "send {0}", query ) );
    eventBus.send( ADDRESS, query, promise );
    promise.
      then( Promises.<JsonObject>unwrapMessage() ).
      then( new PromiseAction<JsonObject>() {
        @Override public void call( JsonObject response ) {
          container.logger().info( MessageFormat.format( "response {0}", response ) );
          assertTrue( response.getString( "cause" ), response.getBoolean( "success" ) );
          // Verify push
          assertNotNull( response.getString( "id" ) );
          assertEquals( value, response.getObject( "snapshot" ) );

          testComplete();
        }
      } ).
      fail( new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          fail( e.toString() );
        }
      } );
  }
}
