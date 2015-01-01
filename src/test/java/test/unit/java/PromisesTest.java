package test.unit.java;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.PromiseFunction;
import com.nickhudak.vertx.promises.Promises;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.impl.DefaultFutureResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PromisesTest {

  private boolean[] returnChecked = new boolean[1];

  @Before
  public void setUp() throws Exception {
    returnChecked[0] = false;
  }

  @Test
  public void testAll() throws Exception {
    Promise<Integer> intPromise = new Promise<>();
    Promise<Long> longPromise = new Promise<>();
    Promise<Double> doublePromise = new Promise<>();
    Collection<Promise<Number>> list = Arrays.asList(
      intPromise.then( Promises.<Integer, Number>castTo( Number.class ) ),
      longPromise.then( Promises.<Long, Number>castTo( Number.class ) ),
      doublePromise.then( Promises.<Double, Number>castTo( Number.class ) )
    );
    Promise<List<Number>> all = Promises.all( list );

    all.then( new PromiseAction<List<Number>>() {
      @Override public void call( List<Number> numbers ) {
        assertEquals( Arrays.asList( 42, 4.2, 36l ), numbers );
        returnChecked[0] = true;
      }
    }, new PromiseAction<Exception>() {
      @Override public void call( Exception e ) {
        throw new AssertionError( e );
      }
    } );

    assertTrue( all.isPending() );
    intPromise.fulfill( 42 );
    assertTrue( all.isPending() );
    doublePromise.fulfill( 4.2 );
    assertTrue( all.isPending() );
    longPromise.fulfill( 36l );
    assertTrue( all.isFulfilled() );

    assertTrue( returnChecked[0] );
  }

  @Test
  public void testAllWithFailure() throws Exception {
    final Exception reason = new Exception( "failed" );
    List<Promise<String>> list = Arrays.asList(
      new Promise<String>(),
      Promises.fulfilled( "pass" ),
      Promises.<String>rejected( reason )
    );

    Promises.all( list ).then( new PromiseAction<List<String>>() {
      @Override public void call( List<String> strings ) {
        fail();
      }
    }, new PromiseAction<Exception>() {
      @Override public void call( Exception e ) {
        assertSame( reason, e );
        returnChecked[0] = true;
      }
    } );

    assertTrue( returnChecked[0] );
  }

  @Test
  public void testUnwrapAsyncResult() throws Exception {
    final DefaultFutureResult<String> future = new DefaultFutureResult<>( "value" );
    final Promise<AsyncResult<String>> promise = Promises.<AsyncResult<String>>fulfilled( future );
    final Exception cause = new Exception( "cause" );

    promise.
      then( Promises.<String>unwrapAsyncResult() ).
      then( new PromiseFunction<String, AsyncResult<Number>>() {
        @Override public AsyncResult<Number> call( String s ) {
          assertEquals( "value", s );
          return new DefaultFutureResult<>( cause );
        }
      } ).
      then( Promises.<Number>unwrapAsyncResult() ).
      then( new PromiseAction<Number>() {
        @Override public void call( Number number ) {
          fail( "Unreachable" );
        }
      } ).
      fail( new PromiseAction<Exception>() {
        @Override public void call( Exception e ) {
          assertSame( cause, e );
          returnChecked[0] = true;
        }
      } );
    assertTrue( returnChecked[0] );
  }
}
