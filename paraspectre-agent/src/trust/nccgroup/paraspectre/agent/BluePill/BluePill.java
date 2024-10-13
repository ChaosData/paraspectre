package trust.nccgroup.paraspectre.agent.BluePill;

import trust.nccgroup.paraspectre.agent.BluePill.Hooks.Class__forName;
import trust.nccgroup.paraspectre.agent.BluePill.Hooks.RuntimeMXBean__getInputArguments;

import java.lang.instrument.Instrumentation;

public class BluePill {

  public static void configure(Instrumentation inst) {

    new RuntimeMXBean__getInputArguments().hook(inst);
    new Class__forName().hook(inst);



  }

}
