package com.nickhudak.vertx.promises;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.darylteo.vertx.promises.java.functions.PromiseFunction;
import com.darylteo.vertx.promises.java.functions.RepromiseFunction;
import com.google.common.collect.Lists;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.eventbus.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nhudak
 */
public class Promises {
  public static <T> Promise<List<T>> all( final Collection<Promise<T>> promises ) {
    if ( promises.isEmpty() ) {
      return fulfilled( Collections.<T>emptyList() );
    }
    final Promise<List<T>> promiseAll = new Promise<>();
    final List<T> completionList = Lists.newArrayListWithExpectedSize( promises.size() );
    PromiseAction<T> somethingFinished = new PromiseAction<T>() {
      @Override public void call( T t ) {
        if ( promiseAll.isPending() ) {
          completionList.add( t );
          if ( completionList.size() == promises.size() ) {
            promiseAll.fulfill( completionList );
          }
        }
      }
    };
    PromiseAction<Exception> somethingFailed = new PromiseAction<Exception>() {
      @Override public void call( Exception e ) {
        promiseAll.reject( e );
      }
    };
    for ( Promise<T> promise : promises ) {
      promise.then( somethingFinished ).fail( somethingFailed );
    }
    return promiseAll;
  }

  public static <T> Promise<T> fulfilled( T value ) {
    Promise<T> promise = new Promise<>();
    promise.fulfill( value );
    return promise;
  }

  public static <T> Promise<T> rejected( Throwable reason ) {
    Promise<T> promise = new Promise<>();
    //Fail callbacks may not be reached if reason is an Error, therefore wrap in an Exception if necessary
    promise.reject( reason instanceof Exception ? reason : new Exception( reason ) );
    return promise;
  }

  public static <T> PromiseFunction<Message<T>, T> unwrapMessage() {
    return new PromiseFunction<Message<T>, T>() {
      @Override public T call( Message<T> message ) {
        return message.body();
      }
    };
  }

  public static <T> RepromiseFunction<AsyncResult<T>, T> unwrapAsyncResult() {
    return new RepromiseFunction<AsyncResult<T>, T>() {
      @Override public Promise<T> call( AsyncResult<T> asyncResult ) {
        if ( asyncResult.succeeded() ) {
          return fulfilled( asyncResult.result() );
        } else {
          return rejected( asyncResult.cause() );
        }
      }
    };
  }

  public static <I,O> RepromiseFunction<I, O> castTo( final Class<O> type ) {
    return new RepromiseFunction<I, O>() {
      @Override public Promise<O> call( I o ) {
        try {
          return fulfilled( type.cast( o ) );
        } catch ( ClassCastException e ) {
          return rejected( e );
        }
      }
    };
  }
}
