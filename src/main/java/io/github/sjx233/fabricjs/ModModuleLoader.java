package io.github.sjx233.fabricjs;

import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.apache.commons.io.FilenameUtils.getPrefixLength;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;

import java.io.IOException;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.util.Pair;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class ModModuleLoader extends DefaultESModuleLoader {
  public ModModuleLoader(JSRealm realm) {
    super(realm);
  }

  private static String dropPrefix(String path) {
    int prefix = getPrefixLength(path);
    if (prefix < 0) return null;
    if (prefix >= path.length()) return "";
    return path.substring(prefix);
  }

  private Pair<ModContainer, String> parseSpecifier(String referrer, String specifier) {
    if (specifier == null) throw Errors.createError("Unable to determine import target");
    int index = specifier.indexOf(":");
    ModContainer mod;
    String path;
    if (index == -1) {
      Pair<ModContainer, String> result = parseSpecifier(null, referrer);
      mod = result.getFirst();
      path = concat(getPath(result.getSecond()), specifier);
    } else {
      String modId = specifier.substring(0, index);
      mod = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> Errors.createError("Mod '" + modId + "' not found"));
      path = normalize(dropPrefix(specifier.substring(index + 1)));
    }
    path = separatorsToUnix(path);
    if (path == null || path.startsWith("//")) throw Errors.createError("Invalid path");
    return new Pair<>(mod, path);
  }

  @Override
  public JSModuleRecord resolveImportedModule(ScriptOrModule referrer, String specifier) {
    Pair<ModContainer, String> result = parseSpecifier(referrer.getSource().getName(), specifier);
    ModContainer mod = result.getFirst();
    String path = result.getSecond();
    try {
      String key = result.getFirst().getMetadata().getId() + ':' + result.getSecond();
      JSModuleRecord module = moduleMap.get(key);
      if (module == null) {
        TruffleFile file = realm.getEnv().getPublicTruffleFile(mod.getPath(path).toUri());
        Source source = Source.newBuilder(JavaScriptLanguage.ID, file).name(key).build();
        module = realm.getContext().getEvaluator().parseModule(realm.getContext(), source, this);
        moduleMap.put(key, module);
      }
      return module;
    } catch (IOException | SecurityException e) {
      throw Errors.createErrorFromException(e);
    }
  }
}
