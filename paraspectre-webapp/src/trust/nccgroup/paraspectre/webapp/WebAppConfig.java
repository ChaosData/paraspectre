package trust.nccgroup.paraspectre.webapp;

import org.eclipse.jgit.api.Git;

@SuppressWarnings("WeakerAccess") //used by paraspectre-webapp
public class WebAppConfig {

  public static final String GIT_NAME = "paraspectre-webapp";
  public static final String GIT_EMAIL = "jeff.dileo@paraspectre.local";

  public Git git;
  public String address;
  public int port;
  public String web_root;
  public String edit_root;
  public boolean delete_existing_web_root = true;

  public String api_key;
}
