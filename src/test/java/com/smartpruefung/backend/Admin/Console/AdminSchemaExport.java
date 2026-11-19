package com.smartpruefung.backend.Admin.Console;

import java.nio.file.*;
import java.util.*;
import javax.persistence.Entity;
import org.hibernate.boot.*;
import org.hibernate.boot.registry.*;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/** Exports additive admin DDL offline, without connecting to or altering a database. */
public class AdminSchemaExport {
  public static void main(String[] args) throws Exception {
    Path output = Paths.get(args.length > 0 ? args[0] : "db/admin-schema.sql");
    Files.createDirectories(output.toAbsolutePath().getParent());
    Path full = output.resolveSibling("admin-export.tmp.sql");
    Files.deleteIfExists(full);
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect")
            .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
            .applySetting(
                "hibernate.physical_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy")
            .build();
    try {
      var sources = new MetadataSources(registry);
      var scanner = new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
      for (var bean : scanner.findCandidateComponents("com.smartpruefung.backend"))
        sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
      var metadata = sources.buildMetadata();
      new SchemaExport()
          .setDelimiter(";")
          .setOutputFile(full.toString())
          .setFormat(false)
          .createOnly(EnumSet.of(TargetType.SCRIPT), metadata);
      List<String> ddl = new ArrayList<>();
      ddl.add(
          "-- Additive admin tables only. No existing customer, billing or exam table is altered.");
      ddl.add("-- Apply once to an existing MySQL database before running with schema validation.");
      Set<String> tables =
          Set.of(
              "admin_credentials",
              "admin_user_state",
              "admin_subscription_state",
              "admin_sessions",
              "admin_audit",
              "admin_plan_settings");
      for (String line : Files.readAllLines(full)) {
        String normalized = line.trim().toLowerCase(Locale.ROOT);
        for (String table : tables)
          if (normalized.startsWith("create table " + table + " ")
              || normalized.startsWith("alter table " + table + " ")
              || (normalized.startsWith("create index ")
                  && normalized.contains(" on " + table + " "))) {
            ddl.add(line);
            break;
          }
      }
      Files.write(output, ddl);
      Files.deleteIfExists(full);
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }
}
