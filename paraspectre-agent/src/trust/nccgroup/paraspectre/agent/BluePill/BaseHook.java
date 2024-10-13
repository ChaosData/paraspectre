package trust.nccgroup.paraspectre.agent.BluePill;

import net.bytebuddy.asm.AsmVisitorWrapper;

import java.lang.instrument.Instrumentation;

public interface BaseHook {

  public void hook(Instrumentation inst);

}
