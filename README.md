[![](http://jenkins.imagej.net/job/SCIFIO-BF-Compat/lastBuild/badge/icon)](http://jenkins.imagej.net/job/SCIFIO-BF-Compat/)

scifio-bf-compat
================

[SCIFIO](https://github.com/scifio/scifio) plug-in allowing the use
[Bio-Formats](https://github.com/openmicroscopy/bioformats) readers.

Usage
=====

__Short version:__ Add the following dependencies to your project:
* [`io.scif:scifio`](http://maven.imagej.net/index.html#nexus-search;gav~io.scif~scifio~~~~kw,versionexpand)
  - to use SCIFIO
* [`io.scif:scifio-bf-compat`](http://maven.imagej.net/index.html#nexus-search;gav~io.scif~scifio-bf-compat~~~~kw,versionexpand)
  - to enable Bio-Formats support within SCIFIO
* [`ome:formats-bsd`](http://maven.imagej.net/index.html#nexus-search;gav~ome~formats-bsd~~~~kw,versionexpand)
  - optional, for open formats supported by Bio-Formats
* [`ome:formats-gpl`](http://maven.imagej.net/index.html#nexus-search;gav~ome~formats-gpl~~~~kw,versionexpand)
  - optional, for proprietary formats supported by Bio-Formats

To ensure version compatibility, you should import or extend
[`net.imagej:pom-imagej`](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.imagej%22%20AND%20a%3A%22pom-imagej%22),
which manages the [Bill of Materials](http://imagej.net/BOM).
Doing this allows you to declare the above dependencies without
explicitly specifying their respective versions.

Be aware that the first three components above are licensed under
[BSD-2](http://imagej.net/BSD), whereas `ome:formats-gpl` is licensed
under the [GPL](http://imagej.net/GPL), which might [affect your project's
licensing](http://www.cio.com/article/2400153/it-organization/how-open-source-licenses-affect-your-business-and-your-developers.html).

__Details:__ Add the following blocks to your Maven POM:

```xml
<dependencyManagement>
  <dependencies>
    <!-- NB: Inherit the SCIFIO version management. -->
    <dependency>
      <groupId>net.imagej</groupId>
      <artifactId>pom-imagej</artifactId>
      <version>5.12.0</version>
    </dependency>
  </dependencies>
</dependencyManagement>
...
<dependencies>
  ...
  <dependency>
    <groupId>io.scif</groupId>
    <artifactId>scifio</artifactId>
  </dependency>
  <dependency>
    <groupId>io.scif</groupId>
    <artifactId>scifio-bf-compat</artifactId>
    <scope>runtime</scope>
  </dependency>
  <!-- The formats-bsd component adds support for open formats. -->
  <dependency>
    <groupId>ome</groupId>
    <artifactId>formats-bsd</artifactId>
    <scope>runtime</scope>
  </dependency>
  <!-- The formats-gpl component adds support for proprietary formats. -->
  <dependency>
    <groupId>ome</groupId>
    <artifactId>formats-gpl</artifactId>
    <scope>runtime</scope>
  </dependency>
  ...
</dependencies>
...
<repositories>
  <!-- NB: For SCIFIO and Bio-Formats dependencies. -->
  <repository>
    <id>imagej.public</id>
    <url>http://maven.imagej.net/content/groups/public</url>
  </repository>
</repositories>
```

See the [Bio-Formats supported formats
table](http://openmicroscopy.org/info/bio-formats/supported-formats.html) for a
full breakdown of which file formats are supported by `ome:formats-bsd` vs.
`ome:formats-gpl`.
