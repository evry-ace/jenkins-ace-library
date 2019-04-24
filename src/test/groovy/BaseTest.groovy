import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before

class BaseTest extends BasePipelineTest {
  // private static final log = LoggerFactory.getLogger(BaseTest)

  @Before
  void setUp() {
    super.setUp()

    // helper.registerAllowedMethod("echo", [ String ]) { arg ->
    //  log.debug "(echo) $arg"
    // }

    // load all steps from vars directory
    new File('vars').eachFile { File file ->
      String name = file.name.replace('.groovy', '')

      // register step with no args, example: toAlphanumeric()
      helper.registerAllowedMethod(name, []) { ->
        Object loader = loadScript(file.path)
        loader()
      }

      // register step with Map arg, example: toAlphanumeric(text: "a")
      helper.registerAllowedMethod(name, [ Map ]) { opts ->
        Object loader = loadScript(file.path)
        loader(opts)
      }
    }
  }
}
