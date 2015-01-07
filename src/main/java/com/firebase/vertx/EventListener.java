package com.firebase.vertx;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Map;
import java.util.Set;

import static com.nickhudak.vertx.json.ToJson.toJson;

/**
 * @author nhudak
 */
public class EventListener implements ValueEventListener, ChildEventListener {
  private final EventBus eventBus;
  public final Map<String, String> addressMap;

  public static final Set<String> VALUE_EVENTS = ImmutableSet.of(
    "dataChange"
  );
  public static final Set<String> CHILD_EVENTS = ImmutableSet.of(
    "childAdded",
    "childChanged",
    "childRemoved",
    "childMoved"
  );
  public static final Set<String> EVENTS = Sets.union( VALUE_EVENTS, CHILD_EVENTS );

  public EventListener( EventBus eventBus ) {
    this.eventBus = eventBus;
    this.addressMap = Maps.newHashMap();
  }

  public static EventListener create( Verticle verticle ) {
    JsonObject config = verticle.getContainer().config();
    EventListener eventListener = new EventListener( verticle.getVertx().eventBus() );

    for ( String listeningEvent : Sets.intersection( EVENTS, config.getFieldNames() ) ) {
      eventListener.addressMap.put( listeningEvent, config.getString( listeningEvent ) );
    }

    return eventListener;
  }

  public EventListener register( Firebase ref ) {
    ref.addValueEventListener( this );
    ref.addChildEventListener( this );
    return this;
  }

  public EventListener unregister( Firebase ref ) {
    ref.removeEventListener( (ChildEventListener) this );
    ref.removeEventListener( (ValueEventListener) this );
    return this;
  }

  @Override public void onDataChange( DataSnapshot dataSnapshot ) {
    handleEvent( "dataChange", dataSnapshot, null );
  }

  @Override public void onChildAdded( DataSnapshot dataSnapshot, String previous ) {
    handleEvent( "childAdded", dataSnapshot, previous );
  }

  @Override public void onChildChanged( DataSnapshot dataSnapshot, String previous ) {
    handleEvent( "childChanged", dataSnapshot, previous );
  }

  @Override public void onChildRemoved( DataSnapshot dataSnapshot ) {
    handleEvent( "childRemoved", dataSnapshot, null );
  }

  @Override public void onChildMoved( DataSnapshot dataSnapshot, String s ) {
    handleEvent( "childMoved", dataSnapshot, null );
  }

  @Override public void onCancelled( FirebaseError error ) {
    JsonObject data = new JsonObject().
      putString( "event", "cancelled" ).
      putString( "cause", error.getMessage() );
    for ( String address : Sets.newHashSet( addressMap.values() ) ) {
      eventBus.send( address, data );
    }
  }

  private void handleEvent( String event, DataSnapshot dataSnapshot, String previous ) {
    if ( addressMap.containsKey( event ) ) {
      String address = addressMap.get( event );
      JsonObject data = new JsonObject().
        putString( "event", event ).
        putString( "id", dataSnapshot.getKey() ).
        putValue( "snapshot", toJson( dataSnapshot.getValue() ) );
      if ( previous != null ) {
        data.putString( "previous", previous );
      }
      eventBus.send( address, data );
    }
  }
}
