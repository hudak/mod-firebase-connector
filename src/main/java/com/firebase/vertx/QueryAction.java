package com.firebase.vertx;

import com.darylteo.vertx.promises.java.Promise;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.nickhudak.vertx.promises.AsyncTask;

import java.util.Map;

/**
* @author nhudak
*/
abstract class QueryAction extends AsyncTask<QueryAction.Response> {

  public static QueryAction put( final Firebase ref, final Object value ) {
    return new QueryAction() {
      @Override protected void execute( final Promise<Response> future ) {
        ref.setValue( value, new Firebase.CompletionListener() {
          @Override public void onComplete( FirebaseError firebaseError, Firebase firebase ) {
            if ( firebaseError == null ) {
              future.fulfill( new Response( firebase.getKey(), value ) );
            } else {
              future.reject( firebaseError.toException() );
            }
          }
        } );
      }
    };
  }

  public static QueryAction get( final Firebase ref ) {
    return new QueryAction() {
      @Override protected void execute( final Promise<Response> future ) {
        ref.addListenerForSingleValueEvent( new ValueEventListener() {
          @Override public void onDataChange( DataSnapshot dataSnapshot ) {
            future.fulfill( new Response( dataSnapshot ) );
          }
          @Override public void onCancelled( FirebaseError firebaseError ) {
            future.reject( firebaseError.toException() );
          }
        } );
      }
    };
  }

  public static QueryAction updateChildren( final Firebase ref, final Map<String, Object> value ) {
    return new QueryAction() {
      QueryAction self = this;
      @Override protected void execute( final Promise<Response> future ) {
        ref.updateChildren( value, new Firebase.CompletionListener() {
          @Override public void onComplete( FirebaseError firebaseError, Firebase firebase ) {
            if ( firebaseError == null ) {
              // Get snapshot at reference
              future.become( get( firebase ).runOnContext( self ) );
            } else {
              future.reject( firebaseError.toException() );
            }
          }
        } );
      }
    };
  }

  public static QueryAction remove( final Firebase ref ) {
    return new QueryAction() {
      @Override protected void execute( final Promise<Response> future ) {
        ref.removeValue( new Firebase.CompletionListener() {
          @Override public void onComplete( FirebaseError firebaseError, Firebase firebase ) {
            if ( firebaseError == null ) {
              future.fulfill( new Response( firebase.getKey(), null ) );
            } else {
              future.reject( firebaseError.toException() );
            }
          }
        } );
      }
    };
  }

  public static class Response {
    private Response( DataSnapshot snapshot ) {
      this( snapshot.getKey(), snapshot.getValue() );
    }
    private Response( String id, Object value ) {
      this.id = id;
      this.value = value;
    }

    public final String id;
    public final Object value;
  }
}
