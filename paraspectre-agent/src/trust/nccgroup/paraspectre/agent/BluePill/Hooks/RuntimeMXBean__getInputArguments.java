package trust.nccgroup.paraspectre.agent.BluePill.Hooks;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import trust.nccgroup.paraspectre.agent.BluePill.BaseHook;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

public class RuntimeMXBean__getInputArguments implements BaseHook {

  private static final String TAG = "PS/Agent/RuntimeMXBean__getInputArguments";

  /*
  @Advice.OnMethodEnter
  static void enter(@Advice.BoxedArguments Object[] boxed) {
    System.out.println("[RuntimeMXBean__getInputArguments]: " + "enter");
  }
  */

  @Advice.OnMethodExit(onThrowable = Exception.class)
  static void exit(@Advice.BoxedReturn (readOnly = false) Object value) {
    System.out.println("[RuntimeMXBean__getInputArguments]: " + "exit");
    List<String> n = new ArrayList<>();
    for (Object o : (List<Object>)value) {
      String s = null;
      if (o instanceof String) {
        s = (String)o;
      } else {
        continue;
      }

      if (s.startsWith("-javaagent:")) {
        System.out.println("[RuntimeMXBean__getInputArguments]: " + "hiding!");
        continue;
      }
      n.add(s);
    }
    value = n;

    /* //breaks due to java.lang.BootstrapMethodError: java.lang.NoClassDefFoundError:
    //value = new ArrayList<String>();
    value = ((List<String>)value).stream()
        .filter((e) -> {
          if ((e instanceof String) && !((String)e).startsWith("-javaagent:")) {
            System.out.println("hiding!");
            return true;
          }
          return false;
        })
        .map((e) -> {
          return (String)e;
        })
        .collect(Collectors.toList());
    */
  }


  //note: doesn't work in > 1.5+
  //      some issue w/ the @Advice.Return that replaces @Advice.BoxedReturn
  //      and @Advice.AllArguments that replaces @Advice.BoxedArguments
  @Override
  public void hook(Instrumentation inst) {

    /*
    ClassPool cp = new ClassPool(true);
    CtClass clz = cp.get("sun.management.RuntimeImpl");

    clz.getDeclaredMethod("getInputArguments", new CtClass[]{}).setBody("{ return new java.util.ArrayList(); }");
    inst.redefineClasses(new ClassDefinition(Class.forName(clz.getName(), false, null), clz.toBytecode()));
    */

    RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();

    //fails in 1.4.33 if using @Advice.Return
    new AgentBuilder.Default()
        .disableClassFormatChanges()
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .ignore(ElementMatchers.none())
        .type(ElementMatchers.is(rmxb.getClass())) //sun.management.RuntimeImpl
        .transform((builder, _cl, _td) -> builder.visit(this.getVisitor()))
        .installOnByteBuddyAgent();


    //throws exception in 1.4.33 if using @Advice.Return
    /*
    ClassLoader rmxbcl = rmxb.getClass().getClassLoader();
    TypeDescription td = TypePool.Default.of(ClassFileLocator.ForClassLoader.of(rmxbcl)).describe("sun.management.RuntimeImpl").resolve();
    try {
      new ByteBuddy()
          .with(Implementation.Context.Disabled.Factory.INSTANCE)
          .redefine(td, ClassFileLocator.ForClassLoader.of(rmxbcl))
          .visit(this.getVisitor())
          .make()
          .load(rmxbcl, ClassReloadingStrategy.fromInstalledAgent());
    } catch (Throwable t) {
      t.printStackTrace();
    }
    */


  }

  AsmVisitorWrapper getVisitor() {
    return Advice.to(this.getClass()).on(ElementMatchers.named("getInputArguments"));
  }
}
