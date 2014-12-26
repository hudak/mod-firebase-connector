package com.firebase.client;

import org.vertx.java.platform.Verticle;

/**
 * @author nhudak
 */
public class VertxFirebase extends Firebase {
  public VertxFirebase( Verticle owner, String ref ) {
    super( ref, new VertxConfig( owner ) );
  }
}
