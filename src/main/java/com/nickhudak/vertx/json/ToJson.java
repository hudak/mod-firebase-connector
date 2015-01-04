package com.nickhudak.vertx.json;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * @author nhudak
 */
public class ToJson implements Function<Object, Object> {
  public static Object toJson( Object input ) {
    return new ToJson().apply( input );
  }

  @Override public Object apply( Object o ) {
    if ( o instanceof Map ) {
      // Copy to new map to enforce string keys
      Map<String, Object> map = Maps.newLinkedHashMap();
      for ( Map.Entry entry : ( (Map<?, ?>) o ).entrySet() ) {
        map.put( entry.getKey().toString(), entry.getValue() );
      }
      return new JsonObject( map );
    } else if ( o instanceof List ) {
      return new JsonArray( (List) o );
    } else if ( o instanceof Object[] ) {
      return new JsonArray( (Object[]) o );
    } else {
      return o;
    }
  }
}
