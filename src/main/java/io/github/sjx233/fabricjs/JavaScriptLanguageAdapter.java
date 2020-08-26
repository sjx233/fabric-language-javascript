package io.github.sjx233.fabricjs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.launch.common.FabricLauncherBase;

public class JavaScriptLanguageAdapter implements LanguageAdapter {
  private static final MethodHandle MH_JSRealm_setModuleLoader;
  private static final IsCallableNode isCallable = IsCallableNode.create();
  private final Context context;
  private final JSRealm realm;

  public JavaScriptLanguageAdapter() {
    try {
      context = Context
        .newBuilder("js")
        .allowAllAccess(true)
        .allowPolyglotAccess(PolyglotAccess.NONE)
        .option("js.intl-402", "true")
        .option("js.regexp-match-indices", "true")
        .option("js.console", "false")
        .option("js.print", "false")
        .option("js.load", "false")
        .option("js.class-fields", "true")
        .build();
      realm = JavaScriptLanguage.getJSRealm(context);
      MH_JSRealm_setModuleLoader.invokeExact(realm, (JSModuleLoader) new ModModuleLoader(realm));
      context.enter();
      realm.getContext().getEvaluator().evaluate(realm, null, Source.newBuilder(JavaScriptLanguage.ID, JavaScriptLanguageAdapter.class.getResource("/init.js")).build());
      context.leave();
    } catch (Throwable e) {
      throw new RuntimeException("Failed to initialize JavaScript runtime", e);
    }
  }

  @Override
  public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
    String specifier = mod.getMetadata().getId() + ':' + value;
    String name = type.getMethods()[0].getName();
    JSModuleRecord module = evaluateModule(null, specifier);
    Object func = getExport(module, name);
    if (!(func instanceof DynamicObject && isCallable.executeBoolean(func))) throw new LanguageAdapterException("Entrypoint is not a function");
    CallTarget target = JSFunction.getFunctionData((DynamicObject) func).getCallTarget();
    return type.cast(Proxy.newProxyInstance(FabricLauncherBase.getLauncher().getTargetClassLoader(), new Class[] { type }, (proxy, method, args) -> {
      context.enter();
      try {
        return target.call();
      } finally {
        context.leave();
      }
    }));
  }

  private JSModuleRecord evaluateModule(ScriptOrModule referrer, String specifier) {
    Evaluator evaluator = realm.getContext().getEvaluator();
    context.enter();
    try {
      JSModuleRecord module = evaluator.hostResolveImportedModule(realm.getContext(), referrer, specifier);
      evaluator.moduleInstantiation(realm, module);
      evaluator.moduleEvaluation(realm, module);
      if (module.getEvaluationError() != null) throw JSRuntime.rethrow(module.getEvaluationError());
      return module;
    } finally {
      context.leave();
    }
  }

  private Object getExport(JSModuleRecord module, String name) {
    ExportResolution resolution = realm.getContext().getEvaluator().resolveExport(module, name);
    if (resolution.isNull() || resolution.isAmbiguous()) return null;
    String bindingName = resolution.getBindingName();
    FrameSlot frameSlot = module.getFrameDescriptor().findFrameSlot(bindingName);
    return module.getEnvironment().getValue(frameSlot);
  }

  static {
    try {
      MH_JSRealm_setModuleLoader = MethodHandles
        .privateLookupIn(JSRealm.class, MethodHandles.lookup())
        .findSetter(JSRealm.class, "moduleLoader", JSModuleLoader.class);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
}
