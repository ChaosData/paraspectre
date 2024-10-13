package trust.nccgroup.paraspectre.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class InternerPlugin implements Plugin<Project> {
  void apply(Project project) {

    def interner = project.extensions.create("interner", InternerExtension)

    project.task("interner") {

    }

    project.afterEvaluate {
      FileInterner fileInterner = new FileInterner.Builder()
                                  .in_dir(interner.in_dir)
                                  .out_dir(interner.out_dir)
                                  .ext(interner.ext)
                                  .pkg(interner.pkg)
                                  .name(interner.name)
                                  .build()

      if (isJavaProject(project)) {
        project.compileJava.dependsOn {
          fileInterner.generate()
        }
      } else if (isAndroidProject(project)) {
        project.preBuild.dependsOn {
          fileInterner.generate()
        }
      }

    }
  }

  public static boolean isJavaProject(Project project) {
    project.plugins.findPlugin('java')
  }

  public static boolean isAndroidProject(Project project) {
    project.plugins.findPlugin('com.android.application') ||
    project.plugins.findPlugin('com.android.library')
  }

}



