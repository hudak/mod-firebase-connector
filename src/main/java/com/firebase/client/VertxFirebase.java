package com.firebase.client;

import com.firebase.vertx.FirebaseVerticle;

/**
 * @author nhudak
 */
public class VertxFirebase extends Firebase {
  public VertxFirebase( FirebaseVerticle owner, String ref ) {
    super( ref, new VertxConfig( owner ) );
  }
}
