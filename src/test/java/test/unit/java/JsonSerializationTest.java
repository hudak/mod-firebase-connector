package test.unit.java;

import com.nickhudak.vertx.json.FromJson;
import com.nickhudak.vertx.json.ToJson;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class JsonSerializationTest {

  private JsonObject jsonObject;
  private ImmutableMap<String, Object> javaObject;

  @Before
  public void prepareObjects() throws Exception {
    jsonObject = new JsonObject();
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    jsonObject.putNumber( "number", 42 );
    builder.put( "number", 42 );

    jsonObject.putString( "string", "value1" );
    builder.put( "string", "value1" );

    JsonObject nested = new JsonObject();
    nested.putString( "string", "value2" );
    jsonObject.putObject( "nested", nested );
    builder.put( "nested", ImmutableMap.<String, Object>of( "string", "value2" ) );

    List<Integer> list = Arrays.asList( 2, 4, 8, 16 );
    jsonObject.putArray( "array", new JsonArray( list ) );
    builder.put( "array", list );

    javaObject = builder.build();
  }

  @Test
  public void testFromJson() throws Exception {
    Assert.assertEquals( FromJson.fromJson( jsonObject ), javaObject );
  }

  @Test
  public void testToJson() throws Exception {
    Assert.assertEquals( ToJson.toJson( javaObject ), jsonObject );
  }

  @Test
  public void testIdentity(){
    assertEquals( "toJson", ToJson.toJson( jsonObject ), jsonObject );
    assertEquals( "fromJson", FromJson.fromJson( javaObject ), javaObject );
  }

  @Test
  public void testPrimitives(){
    assertEquals( "toJson", ToJson.toJson( 42 ), 42 );
    assertEquals( "fromJson", FromJson.fromJson( 42 ), 42 );

    assertEquals( "toJson", ToJson.toJson( "aString" ), "aString" );
    assertEquals( "fromJson", FromJson.fromJson( "aString" ), "aString" );
  }
}
