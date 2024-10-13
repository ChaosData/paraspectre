package trust.nccgroup.paraspectre.agent.BluePill.Hooks;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import trust.nccgroup.paraspectre.agent.BluePill.BaseHook;
import trust.nccgroup.paraspectre.agent.PreMain;

import java.lang.instrument.Instrumentation;

public class Class__forName implements BaseHook {

  public static boolean exception;

  @Advice.OnMethodExit(onThrowable = Exception.class)
  public static void exit(@Advice.Thrown(readOnly = false) Throwable throwable,
                          @Advice.BoxedArguments Object[] boxed) {
    String name = (String)boxed[0];

    if (name.startsWith("trust.nccgroup.paraspectre")) {
      //generally speaking, this is a game of cat and mouse
      //an app can actually have such a class and then use this to check if
      //its being hooked

      try {
        throw new Exception();
      } catch (Exception e) {
        StackTraceElement[] ste = e.getStackTrace();
        for (StackTraceElement st : ste) {
          //System.out.println(st.getClassName());
          if (st.getClassName().equals("trust.nccgroup.paraspectre.agent.PreMain")
            || st.getClassName().startsWith("net.bytebuddy.")
            ) {
            return;
          }
        }
      }
      System.out.println("[Class__forName]: " + "Class.forname(...) called on " + name + " !");
      throwable = new ClassNotFoundException(name);
    }
  }

  @Override
  public void hook(Instrumentation inst) {
    try {

      /*
      new AgentBuilder.Default()
          .disableClassFormatChanges()
          .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
          .ignore(ElementMatchers.none())
          .type(ElementMatchers.is(Class.class))
          .transform((builder, _cl, _td) -> builder.visit(this.getVisitor()))
          .installOnByteBuddyAgent();
      */

      new ByteBuddy()
          .with(Implementation.Context.Disabled.Factory.INSTANCE)
          .redefine(Class.class)
          .visit(this.getVisitor())
          .make()
          .load(ClassLoader.getSystemClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  AsmVisitorWrapper getVisitor() {
    return Advice.to(this.getClass()).on(ElementMatchers.named("forName"));
  }
}

