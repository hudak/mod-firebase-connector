package com.nickhudak.vertx.json;

import com.google.common.base.Function;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author nhudak
 */
public class FromJson implements Function<Object, Object> {
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
}
