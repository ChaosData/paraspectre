package trust.nccgroup.paraspectre.codegen

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
//import org.reflections.*
//import org.reflections.scanners.*

import javax.lang.model.element.Modifier
import java.util.regex.Pattern
import java.util.*
import java.io.*

class FileInterner {

  private final File in_dir;
  private final File out_dir;

  private final String pkg;
  private final String name;

  private final String ext;
  private final String suffix;

  private FileInterner(File _in_dir, File _out_dir, String _pkg, String _name, String _ext) {
    in_dir = _in_dir;
    out_dir = _out_dir;
    if (_pkg.endsWith(".generated")) {
      pkg = _pkg;
    } else {
      pkg = _pkg + ".generated";
    }
    name = _name;
    if (_ext.startsWith(".")) {
      ext = _ext;
    } else {
      ext = "." + _ext;
    }
    suffix = ext.replace(".", "_");
  }

  public class Builder {
    private File in_dir;
    private File out_dir;

    private String pkg;
    private String name;

    private String ext;

    public Builder() { }

    public Builder in_dir(File _in_dir) {
      in_dir = _in_dir;
      return this;
    }

    public Builder out_dir(File _out_dir) {
      out_dir = _out_dir;
      return this;
    }

    public Builder pkg(String _pkg) {
      pkg = _pkg;
      return this;
    }

    public Builder name(String _name) {
      name = _name;
      return this;
    }

    public Builder ext(String _ext) {
      ext = _ext;
      return this;
    }

    public FileInterner build() {
      return new FileInterner(in_dir, out_dir, pkg, name, ext);
    }
  }

  public void generate() {
    if (!in_dir.isDirectory()) {
      throw new IOException('Could not find directory: ' + in_dir);
    }

    File target_dir = new File(out_dir, pkg.replace('.', '/'))
    if (!target_dir.isDirectory() && !target_dir.mkdirs()) {
      throw new IOException('Could not create directory: ' + target_dir)
    }

    Set<String> filePaths = gatherFiles();


    TypeSpec.Builder jsonTemplateBuilder = TypeSpec
      .classBuilder(name)
      .addModifiers(Modifier.PUBLIC);

    for (String path : filePaths) {
      String[] parts = path.split('/');
      String varname = parts[parts.length-1].replace(ext, suffix).toUpperCase()

      String code = readFile('/' + path)

      jsonTemplateBuilder.addField(
        FieldSpec
          .builder(String.class, varname)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer('$S', code)
          .build()
      )

    }

    String jsonTemplateString = JavaFile
      .builder(pkg, jsonTemplateBuilder.build())
      .build()
      .toString();

    File target_file = new File(target_dir, name + '.java')
    PrintWriter out = new PrintWriter(target_file)
    out.write(jsonTemplateString)
    out.close()

  }

  private String readFile(String path) {
    return new Scanner(new FileInputStream(path), "UTF-8")
           .useDelimiter("\\A")
           .next()
  }

  private Set<String> gatherFiles() {
    HashSet<String> ret = new HashSet<>();
    
    String[] fnames = in_dir.list(new FilenameFilter() {
      public boolean accept(File unused, String name) {
        return (name.endsWith(FileInterner.this.ext));
      }
    });

    for (String fname : fnames) {
      ret.add(new File(in_dir, fname).getCanonicalPath());
    }
    return ret;
  }

}
