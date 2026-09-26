package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section_definitions")
@Getter @Setter @NoArgsConstructor
public class SectionDefinition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExamDefinition examDefinition;
    @Column(nullable = false, length = 60)
    private String sectionKey;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private int orderIndex;
    private Integer durationSeconds;
    @Column(length = 60)
    private String timingGroup;
    @OneToMany(mappedBy = "sectionDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<PartDefinition> parts = new ArrayList<>();

    public void addPart(PartDefinition part) {
        parts.add(part);
        part.setSectionDefinition(this);
    }
}
