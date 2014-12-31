package com.nickhudak.vertx.promises;

import com.darylteo.vertx.promises.java.Promise;
import com.darylteo.vertx.promises.java.functions.PromiseAction;
import com.google.common.collect.Lists;

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
    for( Promise<T> promise : promises ) {
      promise.then( somethingFinished ).fail( somethingFailed );
    }
    return promiseAll;
  }

  public static <T> Promise<T> fulfilled( T value ) {
    Promise<T> promise = new Promise<>();
    promise.fulfill( value );
    return promise;
  }

  public static <T> Promise<T> rejected( Throwable reason  ) {
    Promise<T> promise = new Promise<>();
    promise.reject( reason );
    return promise;
  }
}
