package com.nickhudak.vertx.json.json;

import com.google.common.base.Function;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import rx.functions.Func1;

/**
 * @author nhudak
 */
public class FromJson implements Function<Object, Object>, Func1<Object, Object> {
  public static Object fromJson( Object o ) {
    return new FromJson().apply( o );
  }

  @Override public Object apply( Object o ) {
    if ( o instanceof JsonObject ) {
      return ( (JsonObject) o ).toMap();
    } else if ( o instanceof JsonArray ) {
      return ( (JsonArray) o ).toList();
    } else {
      return o;
    }
  }

  @Override public Object call( Object o ) {
    return apply( o );
  }
}
