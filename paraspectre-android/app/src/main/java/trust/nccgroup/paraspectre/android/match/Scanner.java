package trust.nccgroup.paraspectre.android.match;

import com.google.common.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import trust.nccgroup.paraspectre.android.PackageHook;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;

@Deprecated
public class Scanner implements Runnable {
  private static final String TAG = "PS/Scanner";

  private final List<ClassMatcher.Full> working_fct;
  private final List<String> clClassStrs;
  //private final List<Class<?>> clClasses;
  private final ClassLoader hooked_cl;
  private final ClassLoader fallback_cl;
  private final EventBus eventBus;

  Scanner(List<ClassMatcher.Full> working_fct,
          List<String> clClassStrs,
          //List<Class<?>> clClasses,
          ClassLoader hooked_cl,
          ClassLoader fallback_cl,
          EventBus eventBus) {
    this.working_fct = working_fct;
    this.clClassStrs = clClassStrs;
    //this.clClasses = clClasses;
    this.hooked_cl = hooked_cl;
    this.fallback_cl = fallback_cl;
    this.eventBus = eventBus;
  }

  public void run() {

    List<Match.ClassToHook> toHook = null;
    for (String clClassStr : clClassStrs) {
    //for (Class<?> clClass : clClasses) {

      Class<?> clClass = PackageHook.getClass(clClassStr, hooked_cl, fallback_cl);
      if (clClass == null) {
        //Log.e(TAG, "null clClass for " + clClassStr);
        continue;
      } else if (clClass.isInterface()) { //no point in getting interfaces themselves
        continue;
      }

      for (ClassMatcher.Full fct : working_fct) {
        boolean tohook = false;

        if (fct.superCls != null) {
          if (fct.superCls.isAssignableFrom(clClass)) {
            if (fct.superCls == clClass) { //we want subclasses, not the actual parent
              continue;
            }
            tohook = true;
          } else {
            continue;
          }
        }

        if (fct.ifClss != null) {
          for (Class<?> ifClass : fct.ifClss) {
            if (ifClass.isAssignableFrom(clClass)) {
              //no need to check if they're the same, as ifaces are filtered out already
              tohook = true;
            } else {
              tohook = false;
              break;
            }
          }
        }

        if (tohook) {
          if (toHook == null) {
            toHook = new ArrayList<>();
          }
          toHook.add(new Match.ClassToHook(fct, clClass));
        }
      }

    }
    eventBus.post(new Processor.Event(toHook));

  }
}
