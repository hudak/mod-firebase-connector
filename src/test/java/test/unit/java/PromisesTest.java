package test.unit.java;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.PromiseFunction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.nickhudak.vertx.promises.Promises;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

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
    Collection<Promise<Number>>  list = Arrays.asList(
      intPromise.then( new PromiseFunction<Integer, Number>() {
        @Override public Number call( Integer number ) {
          return number;
        }
      } ),
      longPromise.then( new PromiseFunction<Long, Number>() {
        @Override public Number call( Long number ) {
          return number;
        }
      } ),
      doublePromise.then( new PromiseFunction<Double, Number>() {
        @Override public Number call( Double number ) {
          return number;
        }
      } )
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
}
