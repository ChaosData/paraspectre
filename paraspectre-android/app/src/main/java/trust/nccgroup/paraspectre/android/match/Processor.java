package trust.nccgroup.paraspectre.android.match;

import android.content.Context;
import android.util.Log;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;

//note: nifty looking but ultimately much slower than single threaded.
//      all of the time is spent in this is in ClassLoader.loadClass which has
//      a thread lock, so can only be done in one thread w/o wasting time on
//      lock contention.

@Deprecated
public class Processor {
  private final static String TAG = "PS/Processor";

  private final ExecutorService executorService = Executors.newFixedThreadPool(2);
  private final List<Match.ClassToHook> toHook = new ArrayList<>();
  private final SettableFuture<Boolean> future = SettableFuture.create();
  private final EventBus eventBus = new EventBus(TAG);
  private int batches_sent = 0;
  //private AtomicInteger batches_received = new AtomicInteger(-1);
  private int batches_received = -1;
  private boolean end_state = false;

  public Processor() {
    eventBus.register(this);
  }

  static class Event {
    final List<Match.ClassToHook> hooks;

    Event(List<Match.ClassToHook> _hooks) {
      this.hooks = _hooks;
    }
  }

  @Subscribe
  public void receiveHooks(Event evt) {
    //batches_received.incrementAndGet();
    synchronized (toHook) {
      batches_received += 1;
    }
    if (evt.hooks == null) {
      return;
    }
    synchronized (toHook) {
      toHook.addAll(evt.hooks);
      if (end_state) {
        //if (batches_received.intValue() == batches_sent) {
        if (batches_received == batches_sent) {
          future.set(true);
        }
      }
    }
  }



  @Subscribe
  public void setEndState(Boolean state) {
    synchronized (toHook) {
      if (state != null) {
        end_state = state;
      }
    }
  }

  public List<Match.ClassToHook> findMatchingClasses(List<ClassMatcher.Full> fullclss, Context ctx, ClassLoader hooked_cl, ClassLoader fallback_cl) {
    String packageCodePath = ctx.getPackageCodePath();
    DexFile df;
    try {
      df = new DexFile(packageCodePath);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to find dex file", ioe);
      return new ArrayList<>();
    }

    List<ClassMatcher.Full> working_fct = new ArrayList<>(fullclss);
    for (ClassMatcher.Full fct : fullclss) {
      if (fct.cls != null) {
        toHook.add(new Match.ClassToHook(fct, fct.cls));
        working_fct.remove(fct);
      }
    }

    if (working_fct.size() == 0) {
      return toHook;
    }

    for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {

      //List<Class<?>> batch = new ArrayList<>(512);
      List<String> batch = new ArrayList<>(8);
      for (int i=0; i<8; i++) {
        if (iter.hasMoreElements()) {
          //String clClassStr = iter.nextElement();
          //Class<?> clClass = Clazz.Full.getClass(clClassStr, hooked_cl, fallback_cl);
          //batch.add(clClass);
          batch.add(iter.nextElement());
        }
      }

      batches_sent += 1;
      executorService.execute(new Scanner(working_fct, batch, hooked_cl, fallback_cl, eventBus));
      //executorService.execute(new Scanner(working_fct, batch, eventBus));

    }

    eventBus.post(Boolean.TRUE);

    //counterbalance -1 to prevent improbable race
    eventBus.post(new Event(new ArrayList<Match.ClassToHook>()));

    while (true) {
      try {
        Boolean b = future.get();
        if (!b) {
          Log.e(TAG, "future was set false?");
        }
        break;
      } catch (ExecutionException | InterruptedException ee) {
        //pass
      }
    }

    return toHook;
  }
}