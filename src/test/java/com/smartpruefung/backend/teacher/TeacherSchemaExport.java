package com.smartpruefung.backend.teacher;

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
public class TeacherSchemaExport {
  public static void main(String[] args) throws Exception {
    Path output = Paths.get(args.length > 0 ? args[0] : "db/teacher-schema.sql");
    Files.createDirectories(output.toAbsolutePath().getParent());
    Path full = output.resolveSibling("teacher-export.tmp.sql");
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
          "-- Additive teacher tables only. No existing customer, billing or exam table is altered.");
      ddl.add("-- Apply once to an existing MySQL database before running with schema validation.");
      Set<String> tables = Set.of("teacher_profiles","teacher_classes","class_memberships","class_assignments","assignment_students","teacher_seat_orders","teacher_seats","seat_offers","payment_webhook_events","teacher_audit_events");
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
