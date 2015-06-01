package nl.ru.crpx.search;

import java.io.File;

/** Index parameters */
class IndexParam {
  // ====================== LOcal fields =======================================
  private File dir;
  private String pidField;
  private boolean mayViewContents;

  // ====================== Class instantiation ================================
  public IndexParam(File dir, String pidField, boolean mayViewContents) {
    super();
    this.dir = dir;
    this.pidField = pidField;
    this.mayViewContents = mayViewContents;
  }

  // ====================== Outside access to this object ======================
  public File getDir()                      { return dir; }
  public String getPidField()               { return pidField; }
  public boolean mayViewContents()          { return mayViewContents;  }
  public void setPidField(String pidField)  { this.pidField = pidField;  }

}