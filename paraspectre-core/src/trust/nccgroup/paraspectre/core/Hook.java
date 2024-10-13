package trust.nccgroup.paraspectre.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import trikita.log.Log;

public class Hook {
  private String TAG = null;
  private ClassLoader jcl = null;
  private Scripts scripts;
  private ConcurrentMap<Long, ContainerUnitHolder> container_cache;


  public static class Scripts {
    String init_script;
    String hook_script;
    String eval_script;

    public Scripts(String init_script, String hook_script, String eval_script) {
      this.init_script = init_script;
      this.hook_script = hook_script;
      this.eval_script = eval_script;
    }
  }

  public static class Input {
    private Object thiz;
    private  Constructor constructor;
    private Method method;
    private Object[] args;
    private String eval;

    public Map<String,Object> vars;

    public Input(Object thiz, Constructor constructor, Method method,
                 Object[] args, Map<String,Object> vars) {
      this.thiz = thiz;
      this.constructor = constructor;
      this.method = method;
      this.args = args;
      this.eval = null;

      if (vars == null) {
        this.vars = new HashMap<>();
      } else {
        this.vars = vars;
      }
    }
  }

  public Hook(String tag, ClassLoader jcl, Scripts scripts,
              ConcurrentMap<Long,ContainerUnitHolder> container_cache) {
    this.TAG = tag;
    this.jcl = jcl;
    this.scripts = scripts;
    this.container_cache = container_cache;
  }

  public Object[] run(Long tid, Input input) throws Throwable {
    input.eval = scripts.eval_script;

    ContainerUnitHolder cuh = container_cache.get(tid);

    if (cuh == null || cuh.container == null) {
      //Log.d(TAG, "setting up jruby container/unit for tid " + tid);
      Object container;
      Object unit;

      try {
        container = ReflectSetup.setupContainer(scripts.init_script, jcl); //13 seconds
        unit = ReflectSetup.setupUnit(container, scripts.hook_script);
        cuh = new ContainerUnitHolder(container, unit);
        container_cache.put(tid, cuh);
      } catch (Throwable t) {
        Log.e(TAG, "failed to setup container/unit", t);
      }
    } else if (cuh.unit == null) {
      try {
        cuh.unit = ReflectSetup.setupUnit(cuh.container, scripts.hook_script);
      } catch (Throwable t) {
        Log.e(TAG, "failed to setup container/unit", t);
      }
    }

    if (cuh == null) {
      return new Object[]{false};
    }

    Object container = cuh.container;

    ReflectSetup.Proxy.put.invoke(container, "@eval", input.eval);
    ReflectSetup.Proxy.put.invoke(container, "@this", input.thiz);
    ReflectSetup.Proxy.put.invoke(container, "@method", input.method);
    ReflectSetup.Proxy.put.invoke(container, "@constructor", input.constructor);
    ReflectSetup.Proxy.put.invoke(container, "@args", input.args);
    if (input.vars != null) {
      for (Map.Entry<String, Object> kv : input.vars.entrySet()) {
        ReflectSetup.Proxy.put.invoke(container, "@" + kv.getKey(), kv.getValue());
      }
    }
    return new Object[]{ true, ReflectSetup.run(container, cuh.unit) };
  }

  //old

  public String script = null;
  public String eval = null;


}
