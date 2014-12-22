package test.integration.groovy
import com.firebase.vertx.FirebaseVerticle
import org.junit.Test
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

import static org.vertx.groovy.core.impl.ClosureUtil.wrapAsyncResultHandler
import static org.vertx.testtools.VertxAssert.testComplete
/**
 * @author nhudak
 */
class FirebaseTest extends TestVerticle {

    Map testConfig

    @Override
    void start() {
        testConfig = new JsonObject(vertx.fileSystem().readFileSync('testConfig.json').toString()).toMap();
        super.start()
    }

    def prepareFirebase() {
    }

    @Test
    void secretAuthentication(){
        JsonObject config = new JsonObject(ref: testConfig.ref, auth: testConfig.secret)
        container.deployVerticle(FirebaseVerticle.name, config, wrapAsyncResultHandler {
            if( it.failed ){
                throw it.cause
            }
            testComplete();
        })
    }
}
