package io.github.sjx233.fabricjs;

import static io.github.sjx233.fabricjs.Util.escape;
import static io.github.sjx233.fabricjs.Util.findInArray;
import static io.github.sjx233.fabricjs.Util.getFieldValue;
import static io.github.sjx233.fabricjs.Util.setFieldValue;

import java.io.IOException;
import java.lang.reflect.Proxy;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSRealm;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.launch.common.FabricLauncherBase;

public class JavaScriptLanguageAdapter implements LanguageAdapter {
  private static Context context = Context
    .newBuilder("js")
    .allowAllAccess(true)
    .allowPolyglotAccess(PolyglotAccess.NONE)
    .option("js.intl-402", "true")
    .option("js.regexp-match-indices", "true")
    .option("js.timer-resolution", "1")
    .option("js.console", "false")
    .option("js.performance", "true")
    .option("js.print", "false")
    .option("js.load", "false")
    .option("js.class-fields", "true")
    .build();
  private int nextId = 0;

  @Override
  public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
    Value func;
    try {
      String code = String.format("import * as mod from '%s'; mod['%s'];", escape(mod.getMetadata().getId() + ':' + value), escape(type.getMethods()[0].getName()));
      func = context.eval(Source.newBuilder(JavaScriptLanguage.ID, code, "entrypoint-" + nextId++ + ".mjs").build());
    } catch (IOException | PolyglotException e) {
      throw new LanguageAdapterException(e);
    }
    if (func == null || !func.canExecute()) throw new LanguageAdapterException("Entrypoint is not a function");
    return type.cast(Proxy.newProxyInstance(FabricLauncherBase.getLauncher().getTargetClassLoader(), new Class[] { type }, (proxy, method, args) -> func.execute()));
  }

  static {
    try {
      context.eval(Source.newBuilder(JavaScriptLanguage.ID, JavaScriptLanguageAdapter.class.getResource("/init.js")).build());
    } catch (IOException | PolyglotException e) {
      throw new RuntimeException("Failed to initialize globals", e);
    }
    try {
      JSRealm realm = (JSRealm) findInArray((Object[]) getFieldValue(getFieldValue(context, "impl"), "contextImpls"), obj -> obj instanceof JSRealm);
      setFieldValue(realm, "moduleLoader", new ModModuleLoader(realm));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to replace module loader");
    }
  }
}
