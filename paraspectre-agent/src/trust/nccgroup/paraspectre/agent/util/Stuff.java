package trust.nccgroup.paraspectre.agent.util;

import javax.management.loading.ClassLoaderRepository;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;

public class Stuff {
  public static void dumpClassLoaders(Instrumentation inst) throws Throwable {
    ClassLoader bcl = ClassLoader.getSystemClassLoader().getParent();

    RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
    ClassLoader rmxbcl = rmxb.getClass().getClassLoader();

    for (Class c : inst.getInitiatedClasses(bcl)) {
      System.out.println("c: " + c.getCanonicalName());
    }

    for (Class c : inst.getInitiatedClasses(ClassLoader.getSystemClassLoader())) {
      System.out.println("cc: " + c.getCanonicalName());
    }

    ClassLoader cl = null;

    ClassLoaderRepository clr = ManagementFactory.getPlatformMBeanServer().getClassLoaderRepository();

    Field real_clr_f = clr.getClass().getDeclaredField("clr");
    real_clr_f.setAccessible(true);
    Object real_clr = real_clr_f.get(clr);

    Field loaders_f = real_clr.getClass().getDeclaredField("loaders");
    loaders_f.setAccessible(true);
    Object[] loaders = (Object[])loaders_f.get(real_clr);

    for (Object loader : loaders) {
      Field name_f = loader.getClass().getDeclaredField("name");
      name_f.setAccessible(true);
      Object name = name_f.get(loader);

      Field real_loader_f = loader.getClass().getDeclaredField("loader");
      real_loader_f.setAccessible(true);
      Object real_loader = real_loader_f.get(loader);

      if (real_loader instanceof ClassLoader) {
        cl = (ClassLoader)real_loader;
      }
    }

    for (Class c : inst.getInitiatedClasses(cl)) {
      System.out.println("ccc: " + c.getCanonicalName());
    }

    for (Class c : inst.getInitiatedClasses(cl.getParent())) {
      System.out.println("cccc: " + c.getCanonicalName());
    }

    for (Class c : inst.getInitiatedClasses(rmxbcl)) {
      System.out.println("ccccc: " + c.getCanonicalName());
    }

  }

}
